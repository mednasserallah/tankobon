package eu.kanade.tachiyomi.ui.reader.textdetection

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps Google ML Kit's on-device (Latin-script) text recognizer for detecting English text
 * on a manga page. Fully offline, free, no API key.
 *
 * All work runs off the main thread. Detection is best-effort: an empty result (no text on the
 * page) is a normal, expected outcome — not an error.
 *
 * The recognizer is scoped to Latin script only (see the `text-recognition` Latin bundle in the
 * build file), which keeps the app small; the CJK/Devanagari bundles are intentionally not shipped
 * because this feature targets English source text.
 */
class TextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Runs OCR on [bitmap] and returns each detected line with its bounding box, in ML Kit's raw
     * (unsorted) order. Manga reading-order sorting is applied separately by [ReadingOrderSorter].
     *
     * Blank lines and lines with no bounding box are dropped.
     */
    suspend fun recognize(bitmap: Bitmap): List<DetectedTextLine> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).await()
        text.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                val content = line.text.trim()
                if (content.isEmpty()) return@mapNotNull null
                DetectedTextLine(
                    text = content,
                    box = TextBoundingBox(box.left, box.top, box.right, box.bottom),
                    confidence = (line.confidence as Float?)?.takeUnless { it.isNaN() },
                )
            }
    }

    fun close() {
        recognizer.close()
    }
}

/**
 * Awaits a Google Play Services [Task] as a coroutine, without pulling in
 * kotlinx-coroutines-play-services just for this one call site.
 */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
