package eu.kanade.tachiyomi.ui.reader.textdetection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class TextLineMergerTest {

    private fun line(text: String, left: Int, top: Int, right: Int, bottom: Int) =
        DetectedTextLine(text, TextBoundingBox(left, top, right, bottom))

    private fun mergedTexts(lines: List<DetectedTextLine>) =
        TextLineMerger.merge(lines).map { it.text }

    @Test
    fun `wrapped lines in one bubble merge into a single entry`() {
        // Two stacked lines, ~40px tall, 8px leading (0.2 of height), near-aligned horizontally.
        val lines = listOf(
            line("WHO IS", left = 100, top = 0, right = 260, bottom = 40),
            line("THAT?!", left = 105, top = 48, right = 255, bottom = 88),
        )
        mergedTexts(lines) shouldBe listOf("WHO IS THAT?!")
    }

    @Test
    fun `three wrapped lines merge in top-to-bottom order`() {
        val lines = listOf(
            line("YOU CAN", left = 100, top = 96, right = 300, bottom = 136),
            line("THE BLACK", left = 100, top = 0, right = 300, bottom = 40),
            line("SWORDSMAN", left = 100, top = 48, right = 300, bottom = 88),
        )
        mergedTexts(lines) shouldBe listOf("THE BLACK SWORDSMAN YOU CAN")
    }

    @Test
    fun `two side-by-side bubbles at the same height stay separate`() {
        // Same vertical band but disjoint horizontally → different bubbles, must not merge.
        val lines = listOf(
            line("RIGHT", left = 300, top = 0, right = 460, bottom = 40),
            line("LEFT", left = 0, top = 0, right = 160, bottom = 40),
        )
        mergedTexts(lines).sorted() shouldBe listOf("LEFT", "RIGHT")
    }

    @Test
    fun `vertically separate bubbles in the same column stay separate`() {
        // Same column, but a large vertical gap (2x line height) = bubble-to-bubble, not wrapping.
        val lines = listOf(
            line("FIRST", left = 100, top = 0, right = 300, bottom = 40),
            line("SECOND", left = 100, top = 120, right = 300, bottom = 160),
        )
        mergedTexts(lines).sorted() shouldBe listOf("FIRST", "SECOND")
    }

    @Test
    fun `a single-line bubble is returned unchanged`() {
        val lines = listOf(line("EH?", left = 0, top = 0, right = 80, bottom = 40))
        mergedTexts(lines) shouldBe listOf("EH?")
    }

    @Test
    fun `empty input returns empty`() {
        TextLineMerger.merge(emptyList()) shouldBe emptyList()
    }
}
