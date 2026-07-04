package eu.kanade.tachiyomi.ui.reader.textdetection.translation

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * DeepL online translation using the user's own API key.
 *
 * Free keys end in `:fx` and must hit `api-free.deepl.com`; Pro keys hit `api.deepl.com` — routing
 * is decided from the key format ([baseUrlFor]). The key is sent as an `Authorization: DeepL-Auth-Key`
 * header and is never logged.
 *
 * Errors map to distinct [TranslationResult]s (invalid key / quota / rate-limit / network) so the UI
 * can explain what went wrong and the caller can fall back to [MlKitTranslator]. "Powered by DeepL"
 * attribution is required wherever results are shown (handled via [TranslationEngine.attributionRes]).
 */
class DeepLTranslator(
    private val apiKey: String,
    private val client: OkHttpClient,
    private val json: Json,
    private val source: String = SOURCE_LANG,
    private val target: String = TARGET_LANG,
) : Translator {

    override suspend fun translate(text: String): TranslationResult {
        val key = apiKey.trim()
        if (key.isEmpty()) return TranslationResult.MissingApiKey
        if (text.isBlank()) return TranslationResult.Success(text)

        return try {
            val payload = json.encodeToString(
                DeepLRequest(text = listOf(text), targetLang = target, sourceLang = source),
            )
            val request = POST(
                url = "${baseUrlFor(key)}/v2/translate",
                headers = authHeaders(key),
                body = payload.toRequestBody(JSON_MEDIA_TYPE),
                cache = CacheControl.FORCE_NETWORK,
            )
            client.newCall(request).await().use { response ->
                mapResponse(response.code, response.body.string(), json)
            }
        } catch (e: IOException) {
            TranslationResult.NetworkError
        } catch (e: Exception) {
            TranslationResult.Failed(e.message)
        }
    }

    /**
     * Validates the key against the usage endpoint (cheap, doesn't consume the translation quota).
     * Returns [TranslationResult.Success] on a working key, otherwise a typed failure.
     */
    suspend fun validateKey(): TranslationResult {
        val key = apiKey.trim()
        if (key.isEmpty()) return TranslationResult.MissingApiKey
        return try {
            val request = GET(
                url = "${baseUrlFor(key)}/v2/usage",
                headers = authHeaders(key),
                cache = CacheControl.FORCE_NETWORK,
            )
            client.newCall(request).await().use { response ->
                if (response.isSuccessful) TranslationResult.Success("") else mapError(response.code)
            }
        } catch (e: IOException) {
            TranslationResult.NetworkError
        } catch (e: Exception) {
            TranslationResult.Failed(e.message)
        }
    }

    override fun close() {}

    companion object {
        const val SOURCE_LANG = "EN"
        const val TARGET_LANG = "AR"

        private const val FREE_SUFFIX = ":fx"
        private const val FREE_API_URL = "https://api-free.deepl.com"
        private const val PRO_API_URL = "https://api.deepl.com"

        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /** Free keys (`…:fx`) use the free host; everything else is treated as Pro. */
        fun baseUrlFor(apiKey: String): String =
            if (apiKey.trim().endsWith(FREE_SUFFIX)) FREE_API_URL else PRO_API_URL

        private fun authHeaders(key: String): Headers =
            Headers.headersOf("Authorization", "DeepL-Auth-Key $key")

        /**
         * Pure status + body → result mapping, unit-tested without touching the network.
         */
        fun mapResponse(code: Int, body: String, json: Json): TranslationResult {
            if (code == 200) {
                val translated = runCatching {
                    json.decodeFromString<DeepLResponse>(body)
                }.getOrNull()?.translations?.firstOrNull()?.text
                return translated?.let { TranslationResult.Success(it) }
                    ?: TranslationResult.Failed("Empty translations array")
            }
            return mapError(code)
        }

        /** Maps a DeepL error status code to a distinct result. */
        fun mapError(code: Int): TranslationResult = when (code) {
            403 -> TranslationResult.InvalidApiKey
            456 -> TranslationResult.QuotaExceeded
            429 -> TranslationResult.RateLimited
            in 500..599 -> TranslationResult.NetworkError
            else -> TranslationResult.Failed("HTTP $code")
        }
    }
}

@Serializable
private data class DeepLRequest(
    val text: List<String>,
    @SerialName("target_lang") val targetLang: String,
    @SerialName("source_lang") val sourceLang: String? = null,
)

@Serializable
private data class DeepLResponse(
    val translations: List<DeepLTranslation> = emptyList(),
)

@Serializable
private data class DeepLTranslation(
    val text: String,
)
