package eu.kanade.tachiyomi.ui.reader.textdetection.translation

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.mlkit.nl.translate.Translator as MlKitClient

/**
 * On-device translation via Google ML Kit. Fully offline once the target language pack (~30 MB) has
 * been downloaded once. The default engine — free, no key, no network after the first download.
 *
 * Scoped to EN→AR for now; source/target are the only hardcoded values, so a future multi-language
 * version only needs to parameterise them. "Powered by Google Translate" attribution is required
 * wherever results are shown (handled in the UI via [TranslationEngine.attributionRes]).
 *
 * @param requireWifi resolved lazily per [prepare] so it always reflects the current preference.
 */
class MlKitTranslator(
    private val requireWifi: () -> Boolean,
    private val source: String = TranslateLanguage.ENGLISH,
    private val target: String = TranslateLanguage.ARABIC,
) : Translator {

    private val client: MlKitClient = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build(),
    )

    override suspend fun prepare(onDownloading: () -> Unit) {
        val model = TranslateRemoteModel.Builder(target).build()
        val missing = !RemoteModelManager.getInstance().isModelDownloaded(model).await()
        if (missing) {
            onDownloading()
            val wifiOnly = requireWifi()
            val conditions = DownloadConditions.Builder()
                .apply { if (wifiOnly) requireWifi() }
                .build()
            client.downloadModelIfNeeded(conditions).await()
        }
    }

    override suspend fun translate(text: String): TranslationResult = try {
        TranslationResult.Success(client.translate(text).await())
    } catch (e: Exception) {
        // ML Kit surfaces a missing model / IO issue as an exception; from the user's point of view
        // it's a "couldn't translate right now" — network-style failure.
        TranslationResult.NetworkError
    }

    override fun close() {
        client.close()
    }
}

/** Deletes the downloaded Arabic language pack to reclaim storage. */
suspend fun deleteArabicTranslationModel() {
    val model = TranslateRemoteModel.Builder(TranslateLanguage.ARABIC).build()
    RemoteModelManager.getInstance().deleteDownloadedModel(model).await()
}

internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
