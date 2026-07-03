package eu.kanade.tachiyomi.ui.reader.textdetection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class ReadingOrderSorterTest {

    private fun line(text: String, left: Int, top: Int, right: Int, bottom: Int) =
        DetectedTextLine(text, TextBoundingBox(left, top, right, bottom))

    private fun sortedText(lines: List<DetectedTextLine>, rtl: Boolean) =
        ReadingOrderSorter.sort(lines, rtl).map { it.text }

    @Test
    fun `single column reads top to bottom regardless of direction`() {
        // One panel, three vertically-stacked lines with no vertical overlap → three bands.
        val lines = listOf(
            line("C", left = 10, top = 80, right = 200, bottom = 110),
            line("A", left = 10, top = 0, right = 200, bottom = 30),
            line("B", left = 10, top = 40, right = 200, bottom = 70),
        )

        sortedText(lines, rtl = true) shouldBe listOf("A", "B", "C")
        sortedText(lines, rtl = false) shouldBe listOf("A", "B", "C")
    }

    @Test
    fun `two side-by-side panels order right-to-left within each band for RTL`() {
        // Two horizontal bands, each holding a right panel and a left panel.
        val rt = line("RT", left = 210, top = 0, right = 400, bottom = 40)
        val lt = line("LT", left = 10, top = 0, right = 200, bottom = 40)
        val rb = line("RB", left = 210, top = 60, right = 400, bottom = 100)
        val lb = line("LB", left = 10, top = 60, right = 200, bottom = 100)
        val scrambled = listOf(lb, rt, lt, rb)

        // RTL: within each band right before left; bands top before bottom.
        sortedText(scrambled, rtl = true) shouldBe listOf("RT", "LT", "RB", "LB")
        // LTR: mirror image within each band.
        sortedText(scrambled, rtl = false) shouldBe listOf("LT", "RT", "LB", "RB")
    }

    @Test
    fun `staggered descent stays separate bands and reads top to bottom`() {
        // Diagonal stair: consecutive lines overlap by only 25% of their height (< 0.5 threshold),
        // so they must NOT chain into one band — each is its own band, read strictly top-to-bottom.
        val lines = listOf(
            line("S3", left = 80, top = 30, right = 130, bottom = 50),
            line("S1", left = 0, top = 0, right = 50, bottom = 20),
            line("S4", left = 120, top = 45, right = 170, bottom = 65),
            line("S2", left = 40, top = 15, right = 90, bottom = 35),
        )

        sortedText(lines, rtl = true) shouldBe listOf("S1", "S2", "S3", "S4")
        sortedText(lines, rtl = false) shouldBe listOf("S1", "S2", "S3", "S4")
    }

    @Test
    fun `a small vertical offset still groups into one band (tolerance not per-pixel)`() {
        // Same band, right and left boxes offset vertically by 3px (85% overlap) — they must group,
        // proving the clustering is not so strict that a tiny offset creates a new row.
        val right = line("R", left = 100, top = 0, right = 200, bottom = 20)
        val left = line("L", left = 0, top = 3, right = 90, bottom = 23)

        sortedText(listOf(left, right), rtl = true) shouldBe listOf("R", "L")
        sortedText(listOf(left, right), rtl = false) shouldBe listOf("L", "R")
    }

    @Test
    fun `empty and single-line inputs are returned unchanged`() {
        ReadingOrderSorter.sort(emptyList(), rtl = true) shouldBe emptyList()
        val single = listOf(line("only", 0, 0, 10, 10))
        ReadingOrderSorter.sort(single, rtl = true) shouldBe single
    }
}
