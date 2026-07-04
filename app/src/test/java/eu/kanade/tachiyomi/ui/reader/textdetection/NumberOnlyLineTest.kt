package eu.kanade.tachiyomi.ui.reader.textdetection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class NumberOnlyLineTest {

    @Test
    fun `plain page numbers are number-only`() {
        isNumberOnlyLine("18") shouldBe true
        isNumberOnlyLine("  20  ") shouldBe true
        isNumberOnlyLine("1/219") shouldBe true
        isNumberOnlyLine("3.") shouldBe true
    }

    @Test
    fun `anything containing a letter is kept`() {
        isNumberOnlyLine("Chapter 1") shouldBe false
        isNumberOnlyLine("Pure folly!") shouldBe false
        isNumberOnlyLine("B") shouldBe false
    }

    @Test
    fun `text with no digits is kept`() {
        isNumberOnlyLine("...") shouldBe false
        isNumberOnlyLine("!!") shouldBe false
        isNumberOnlyLine("") shouldBe false
    }
}
