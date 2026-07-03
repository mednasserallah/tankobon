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
    data class Success(val lines: List<DetectedTextLine>) : TextDetectionState
}
