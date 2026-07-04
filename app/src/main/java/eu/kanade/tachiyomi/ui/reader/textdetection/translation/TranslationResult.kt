package eu.kanade.tachiyomi.ui.reader.textdetection.translation

/**
 * Outcome of a single translation (or key-validation) attempt.
 *
 * The failures are kept distinct so the UI can show an engine-appropriate message and the caller can
 * decide whether to fall back to another engine, rather than collapsing everything into one generic
 * "translation failed". Anything that isn't [Success] is a failure.
 */
sealed interface TranslationResult {
    data class Success(val text: String) : TranslationResult

    /** DeepL selected but no API key stored. */
    data object MissingApiKey : TranslationResult

    /** DeepL rejected the key (HTTP 403). */
    data object InvalidApiKey : TranslationResult

    /** DeepL character quota reached (HTTP 456). */
    data object QuotaExceeded : TranslationResult

    /** Too many requests in a short window (HTTP 429). */
    data object RateLimited : TranslationResult

    /** Network unreachable / timeout / server (5xx) error. */
    data object NetworkError : TranslationResult

    /** Any other failure; [detail] is for logs only, never shown verbatim. */
    data class Failed(val detail: String? = null) : TranslationResult
}
