package eu.kanade.tachiyomi.ui.reader.textdetection

import androidx.compose.runtime.Immutable

/**
 * UI state for the on-page text-detection sheet.
 *
 * [Empty] and [Error] are normal, expected outcomes for a best-effort feature — a page may simply
 * have no detectable text, or OCR may fail — not exceptional/crash-adjacent states.
 */
@Immutable
sealed interface TextDetectionState {
    data object Loading : TextDetectionState
    data object Empty : TextDetectionState
    data object Error : TextDetectionState

    @Immutable
    data class Success(val items: List<DetectedLineItem>) : TextDetectionState
}

/** A detected line plus its (optional) translation state. */
@Immutable
data class DetectedLineItem(
    val line: DetectedTextLine,
    val translation: TranslationState = TranslationState.Idle,
)

/**
 * Per-line translation state. [Downloading] covers the one-time language-pack download; all states
 * other than [Done] are transient or expected (not error-styled beyond [Error]).
 */
@Immutable
sealed interface TranslationState {
    data object Idle : TranslationState
    data object Downloading : TranslationState
    data object Translating : TranslationState
    data object Error : TranslationState

    @Immutable
    data class Done(val text: String) : TranslationState
}
