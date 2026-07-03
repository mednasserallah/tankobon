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

    // region hyphen line-break handling (collapseHyphenation)

    @Test
    fun `stutter fragment is dropped`() {
        // "PR-" repeats the start of "PREPARE" → drawn-out stammer, drop the fragment.
        TextLineMerger.collapseHyphenation("PR- PREPARE") shouldBe "PREPARE"
    }

    @Test
    fun `word-wrap hyphenation is spliced without a space`() {
        // "ACCOM" is a long fragment, not a prefix-stutter → rejoin the split word.
        TextLineMerger.collapseHyphenation("ACCOM- PLISH") shouldBe "ACCOMPLISH"
    }

    @Test
    fun `repeated stutter collapses fully`() {
        TextLineMerger.collapseHyphenation("P- P- PREPARE") shouldBe "PREPARE"
    }

    @Test
    fun `a trailing hyphen with nothing following is left as-is`() {
        TextLineMerger.collapseHyphenation("GOODBYE-") shouldBe "GOODBYE-"
    }

    @Test
    fun `hyphenation keeps the rest of the line`() {
        TextLineMerger.collapseHyphenation("ACCOM- PLISH THE TASK") shouldBe "ACCOMPLISH THE TASK"
        TextLineMerger.collapseHyphenation("PR- PREPARE FOR IT") shouldBe "PREPARE FOR IT"
    }

    @Test
    fun `hyphenated wrapped lines merge and collapse into one word`() {
        // Two stacked, near-aligned lines "ACCOM-" / "PLISH" → cluster → collapse to one word.
        val lines = listOf(
            line("ACCOM-", left = 100, top = 0, right = 260, bottom = 40),
            line("PLISH", left = 105, top = 48, right = 250, bottom = 88),
        )
        mergedTexts(lines) shouldBe listOf("ACCOMPLISH")
    }

    // endregion
}
