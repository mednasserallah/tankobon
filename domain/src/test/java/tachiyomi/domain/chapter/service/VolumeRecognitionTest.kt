package tachiyomi.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class VolumeRecognitionTest {

    // region parseVolume — happy path

    @Test
    fun `standard volume file`() {
        val info = VolumeRecognition.parseVolume("Ao Haru Ride - Volume 01 (2018)")
        info.number shouldBe 1
        info.numberEnd shouldBe null
        info.year shouldBe 2018
        info.name shouldBe "Volume 01"
    }

    @Test
    fun `zero padding does not affect parsed number`() {
        VolumeRecognition.parseVolume("Series - Volume 01 (2020)").number shouldBe 1
        VolumeRecognition.parseVolume("Series - Volume 9 (2020)").number shouldBe 9
        VolumeRecognition.parseVolume("Series - Volume 10 (2020)").number shouldBe 10
    }

    @Test
    fun `volumes sort numerically not lexically`() {
        val files = listOf(
            "Series - Volume 10 (2020)",
            "Series - Volume 1 (2020)",
            "Series - Volume 2 (2020)",
            "Series - Volume 9 (2020)",
        )
        val sorted = files
            .map { VolumeRecognition.parseVolume(it) }
            .sortedBy { it.number }
            .map { it.number }
        sorted shouldBe listOf(1, 2, 9, 10)
    }

    // endregion

    // region parseVolume — edge cases

    @Test
    fun `one-shot with no volume segment is volume 1`() {
        val info = VolumeRecognition.parseVolume("Boy Meets Maria (2021)")
        info.number shouldBe 1
        info.numberEnd shouldBe null
        info.year shouldBe 2021
        info.name shouldBe "Boy Meets Maria"
    }

    @Test
    fun `one-shot with no year`() {
        val info = VolumeRecognition.parseVolume("Goodbye, Eri")
        info.number shouldBe 1
        info.year shouldBe null
        info.name shouldBe "Goodbye, Eri"
    }

    @Test
    fun `series name parentheses not confused with year`() {
        val info = VolumeRecognition.parseVolume("BLAME! (Master Edition) - Volume 01 (2016)")
        info.number shouldBe 1
        info.year shouldBe 2016
        info.name shouldBe "Volume 01"
    }

    @Test
    fun `edition parenthetical alone is not treated as a year`() {
        // No 4-digit year present; the (Omnibus Edition) group must not leak into year.
        val info = VolumeRecognition.parseVolume("Gantz (Omnibus Edition) - Volume 05")
        info.number shouldBe 5
        info.year shouldBe null
        info.name shouldBe "Volume 05"
    }

    @Test
    fun `inconsistent year per volume is parsed per file`() {
        VolumeRecognition.parseVolume("Ao Haru Ride - Volume 01 (2018)").year shouldBe 2018
        VolumeRecognition.parseVolume("Ao Haru Ride - Volume 02 (2018)").year shouldBe 2018
        VolumeRecognition.parseVolume("Ao Haru Ride - Volume 03 (2019)").year shouldBe 2019
    }

    @Test
    fun `omnibus range captures start and end`() {
        val info = VolumeRecognition.parseVolume("Homunculus (Omnibus Edition) - Volume 01-02 (2023)")
        info.number shouldBe 1
        info.numberEnd shouldBe 2
        info.year shouldBe 2023
        info.name shouldBe "Volume 01-02"
    }

    @Test
    fun `omnibus range sorts by its start number`() {
        val info = VolumeRecognition.parseVolume("Series - Volume 03-05 (2023)")
        info.number shouldBe 3
        info.numberEnd shouldBe 5
    }

    @Test
    fun `range end equal to start is ignored`() {
        // "Volume 02-02" is not a real range; treat as a single volume.
        val info = VolumeRecognition.parseVolume("Series - Volume 02-02 (2023)")
        info.number shouldBe 2
        info.numberEnd shouldBe null
    }

    @Test
    fun `abbreviated vol token`() {
        VolumeRecognition.parseVolume("Series - Vol. 7 (2019)").number shouldBe 7
        VolumeRecognition.parseVolume("Series v08 (2019)").number shouldBe 8
    }

    @Test
    fun `unrecognized name falls back to volume 1 with full basename`() {
        val info = VolumeRecognition.parseVolume("random_scan_group_release")
        info.number shouldBe 1
        info.numberEnd shouldBe null
        info.name shouldBe "random_scan_group_release"
    }

    // endregion

    // region parseSeries

    @Test
    fun `series with no edition`() {
        val info = VolumeRecognition.parseSeries("Boy Meets Maria")
        info.title shouldBe "Boy Meets Maria"
        info.edition shouldBe null
    }

    @Test
    fun `series with master edition tag`() {
        val info = VolumeRecognition.parseSeries("BLAME! (Master Edition)")
        info.title shouldBe "BLAME!"
        info.edition shouldBe "Master Edition"
    }

    @Test
    fun `series with omnibus edition tag`() {
        val info = VolumeRecognition.parseSeries("Gantz (Omnibus Edition)")
        info.title shouldBe "Gantz"
        info.edition shouldBe "Omnibus Edition"
    }

    @Test
    fun `series with multi-word omnibus edition`() {
        val info = VolumeRecognition.parseSeries("Goodnight Punpun (Omnibus Edition)")
        info.title shouldBe "Goodnight Punpun"
        info.edition shouldBe "Omnibus Edition"
    }

    @Test
    fun `series folder with a trailing year is not an edition`() {
        val info = VolumeRecognition.parseSeries("One Shot Wonder (2021)")
        info.edition shouldBe null
        // Year groups are left in the series title (rare for folders); not misread as edition.
        info.title shouldBe "One Shot Wonder (2021)"
    }

    // endregion
}
