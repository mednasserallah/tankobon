package eu.kanade.tachiyomi.ui.reader.textdetection.translation

/**
 * A translation engine. Implementations hide their specifics (on-device model vs. remote API) so the
 * rest of the app translates the same way regardless of which engine the user picked.
 *
 * Current implementations: [MlKitTranslator] (on-device Google ML Kit, default) and [DeepLTranslator]
 * (online, user's own API key).
 */
interface Translator {

    /**
     * Prepares the engine before [translate]. ML Kit downloads its language pack the first time
     * (invoking [onDownloading] while it does so the UI can show a distinct "downloading" state);
     * DeepL has nothing to prepare. May throw — the caller treats a thrown exception as a setup
     * failure, same as a failed [translate].
     */
    suspend fun prepare(onDownloading: () -> Unit) {}

    /**
     * Translates [text]. Returns a typed [TranslationResult]; expected failures (bad key, quota,
     * network) come back as result values rather than exceptions so the caller can react per-case.
     */
    suspend fun translate(text: String): TranslationResult

    /** Releases any held resources. No-op for stateless engines. */
    fun close() {}
}
