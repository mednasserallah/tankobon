package eu.kanade.tachiyomi.ui.reader.textdetection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class SentenceCaseTest {

    @Test
    fun `a simple all-caps sentence becomes sentence case`() {
        SentenceCase.normalize("WHO IS THAT?!") shouldBe "Who is that?!"
    }

    @Test
    fun `each sentence in a merged line is capitalized`() {
        SentenceCase.normalize("THE BLACK SWORDSMAN HAS COME. THAT'S ALL.") shouldBe
            "The black swordsman has come. That's all."
    }

    @Test
    fun `capitalizes after exclamation and question marks`() {
        SentenceCase.normalize("REALLY?! YOU CAME. AMAZING! WHY?") shouldBe
            "Really?! You came. Amazing! Why?"
    }

    @Test
    fun `standalone i is uppercased everywhere`() {
        SentenceCase.normalize("YOU AND I WENT, BUT I STAYED") shouldBe "You and I went, but I stayed"
    }

    @Test
    fun `standalone i does not affect words containing i`() {
        SentenceCase.normalize("IT IS HIS SHIP") shouldBe "It is his ship"
    }

    @Test
    fun `trailing ellipsis and punctuation are preserved`() {
        SentenceCase.normalize("WHA-WHAT MESSAGE...?") shouldBe "Wha-what message...?"
    }

    @Test
    fun `contraction with i is uppercased`() {
        SentenceCase.normalize("I'M FINE") shouldBe "I'm fine"
    }

    @Test
    fun `blank input is returned unchanged`() {
        SentenceCase.normalize("") shouldBe ""
        SentenceCase.normalize("   ") shouldBe "   "
    }
}
