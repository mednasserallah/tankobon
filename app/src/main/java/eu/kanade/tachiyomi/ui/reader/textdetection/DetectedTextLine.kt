package eu.kanade.tachiyomi.ui.reader.textdetection

/**
 * A framework-agnostic bounding box (in source-image pixels) for a piece of detected text.
 *
 * Kept independent of `android.graphics.Rect` on purpose: the reading-order sorter that
 * consumes it stays pure Kotlin and can be unit-tested on the JVM without Android stubs.
 */
data class TextBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val height: Int get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * A single line of text detected on a page, with its position on the page.
 *
 * "Line" is ML Kit's `Text.Line` granularity — the most natural per-speech-bubble grouping
 * for manga (a `Block` can merge separate bubbles; an `Element` is a single word).
 */
data class DetectedTextLine(
    val text: String,
    val box: TextBoundingBox,
)
