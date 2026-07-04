package eu.kanade.tachiyomi.ui.reader.textdetection.translation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class DeepLTranslatorTest {

    private val json = Json { ignoreUnknownKeys = true }

    // region Free vs. Pro endpoint routing

    @Test
    fun `free keys route to the free host`() {
        DeepLTranslator.baseUrlFor("279a2e9d-83b3-c416-7e2d-f721593e42a0:fx") shouldBe
            "https://api-free.deepl.com"
    }

    @Test
    fun `pro keys route to the pro host`() {
        DeepLTranslator.baseUrlFor("279a2e9d-83b3-c416-7e2d-f721593e42a0") shouldBe
            "https://api.deepl.com"
    }

    @Test
    fun `surrounding whitespace does not change routing`() {
        DeepLTranslator.baseUrlFor("  abc:fx  ") shouldBe "https://api-free.deepl.com"
        DeepLTranslator.baseUrlFor("  abc  ") shouldBe "https://api.deepl.com"
    }

    @Test
    fun `fx appearing mid-key is not treated as free`() {
        // Only a trailing :fx marks a free key.
        DeepLTranslator.baseUrlFor("ab:fx-cd") shouldBe "https://api.deepl.com"
    }

    // endregion

    // region Successful response parsing

    @Test
    fun `200 with a translation returns Success with the translated text`() {
        val body = """{"translations":[{"detected_source_language":"EN","text":"مرحبا"}]}"""
        val result = DeepLTranslator.mapResponse(200, body, json)
        result.shouldBeInstanceOf<TranslationResult.Success>()
        result.text shouldBe "مرحبا"
    }

    @Test
    fun `200 uses the first translation when several are returned`() {
        val body = """{"translations":[{"text":"الأول"},{"text":"الثاني"}]}"""
        val result = DeepLTranslator.mapResponse(200, body, json)
        result.shouldBeInstanceOf<TranslationResult.Success>()
        result.text shouldBe "الأول"
    }

    @Test
    fun `200 with an empty translations array is a failure, not an empty success`() {
        val result = DeepLTranslator.mapResponse(200, """{"translations":[]}""", json)
        result.shouldBeInstanceOf<TranslationResult.Failed>()
    }

    @Test
    fun `200 with malformed body is a failure, not a crash`() {
        val result = DeepLTranslator.mapResponse(200, "not json", json)
        result.shouldBeInstanceOf<TranslationResult.Failed>()
    }

    // endregion

    // region Error status mapping

    @Test
    fun `403 maps to InvalidApiKey`() {
        DeepLTranslator.mapResponse(403, "", json) shouldBe TranslationResult.InvalidApiKey
        DeepLTranslator.mapError(403) shouldBe TranslationResult.InvalidApiKey
    }

    @Test
    fun `456 maps to QuotaExceeded`() {
        DeepLTranslator.mapError(456) shouldBe TranslationResult.QuotaExceeded
    }

    @Test
    fun `429 maps to RateLimited`() {
        DeepLTranslator.mapError(429) shouldBe TranslationResult.RateLimited
    }

    @Test
    fun `5xx maps to NetworkError`() {
        DeepLTranslator.mapError(500) shouldBe TranslationResult.NetworkError
        DeepLTranslator.mapError(503) shouldBe TranslationResult.NetworkError
    }

    @Test
    fun `other 4xx codes map to a generic Failed carrying the code`() {
        val result = DeepLTranslator.mapError(400)
        result.shouldBeInstanceOf<TranslationResult.Failed>()
        result.detail!!.shouldContain("400")
    }

    // endregion
}
