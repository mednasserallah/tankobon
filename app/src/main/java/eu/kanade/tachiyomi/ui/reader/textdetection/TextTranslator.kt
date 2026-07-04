package eu.kanade.tachiyomi.ui.reader.textdetection

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps Google ML Kit's on-device translation for English → Arabic. Fully offline once the Arabic
 * language pack (~30 MB) has been downloaded once.
 *
 * Scoped to EN→AR for now, but source/target are the only hardcoded values — swapping or
 * parameterising them is all that a future multi-language version needs. "Powered by Google
 * Translate" attribution is required wherever results are shown (handled in the UI).
 */
class TextTranslator(
    private val source: String = TranslateLanguage.ENGLISH,
    private val target: String = TranslateLanguage.ARABIC,
) {

    private val translator: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build(),
    )

    /** True if the target language pack still needs downloading before the first translation. */
    suspend fun isModelMissing(): Boolean {
        val model = TranslateRemoteModel.Builder(target).build()
        return !RemoteModelManager.getInstance().isModelDownloaded(model).await()
    }

    /** Downloads the target language pack if it isn't present yet. */
    suspend fun ensureModelDownloaded(requireWifi: Boolean) {
        val conditions = DownloadConditions.Builder()
            .apply { if (requireWifi) requireWifi() }
            .build()
        translator.downloadModelIfNeeded(conditions).await()
    }

    /** Translates [text]; the model must already be downloaded (see [ensureModelDownloaded]). */
    suspend fun translate(text: String): String = translator.translate(text).await()

    fun close() {
        translator.close()
    }
}

/** Deletes the downloaded Arabic language pack to reclaim storage. */
suspend fun deleteArabicTranslationModel() {
    val model = TranslateRemoteModel.Builder(TranslateLanguage.ARABIC).build()
    RemoteModelManager.getInstance().deleteDownloadedModel(model).await()
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
