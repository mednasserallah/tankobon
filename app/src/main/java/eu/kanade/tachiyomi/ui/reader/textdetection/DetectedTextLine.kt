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
 * A single line of text detected on a page, with its position and ML Kit [confidence] (0f..1f,
 * or null when the model doesn't report one).
 *
 * "Line" is ML Kit's `Text.Line` granularity — the most natural per-speech-bubble grouping
 * for manga (a `Block` can merge separate bubbles; an `Element` is a single word).
 */
data class DetectedTextLine(
    val text: String,
    val box: TextBoundingBox,
    val confidence: Float? = null,
) {
    /** Whether this detection should be flagged for the user to double-check before acting on it. */
    fun isLowConfidence(threshold: Float = LOW_CONFIDENCE_THRESHOLD): Boolean =
        confidence != null && confidence < threshold
}

/**
 * ML Kit line confidence below which a detection is visually flagged as "maybe edit this". Chosen at
 * 0.5: on real fixtures clean lettering scores well above this (~0.9+), while smudged / small /
 * stylised text drops below it, so 0.5 catches the genuinely-doubtful reads without flagging most
 * good ones. It's a hint, never a block. Tunable — bump up to flag more aggressively.
 */
const val LOW_CONFIDENCE_THRESHOLD = 0.5f
