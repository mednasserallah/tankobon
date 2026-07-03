package tachiyomi.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.chapter.model.Volume

@Execution(ExecutionMode.CONCURRENT)
class MissingVolumesTest {

    @Test
    fun `missingVolumesCount returns 0 when empty list`() {
        emptyList<Volume>().missingVolumesCount() shouldBe 0
    }

    @Test
    fun `missingVolumesCount returns 0 when all unknown volume numbers`() {
        listOf(volume(-1), volume(-1), volume(-1)).missingVolumesCount() shouldBe 0
    }

    @Test
    fun `missingVolumesCount ignores repeated volume numbers`() {
        listOf(volume(1), volume(1), volume(2), volume(2), volume(3)).missingVolumesCount() shouldBe 0
    }

    @Test
    fun `missingVolumesCount returns number of missing volumes`() {
        listOf(volume(-1), volume(1), volume(2), volume(4), volume(6), volume(10), volume(11))
            .missingVolumesCount() shouldBe 5
    }

    @Test
    fun `missingVolumesCount expands omnibus ranges so covered volumes are not counted missing`() {
        // Volume 01-02 + Volume 03-04 covers 1..4 with no gaps.
        listOf(volume(1, 2), volume(3, 4)).missingVolumesCount() shouldBe 0
        // Volume 01-02 then Volume 05-06 is missing 3 and 4.
        listOf(volume(1, 2), volume(5, 6)).missingVolumesCount() shouldBe 2
    }

    @Test
    fun `calculateVolumeGap returns difference`() {
        calculateVolumeGap(volume(10), volume(9)) shouldBe 0
        calculateVolumeGap(volume(10), volume(8)) shouldBe 1
        calculateVolumeGap(volume(10), volume(1)) shouldBe 8

        calculateVolumeGap(10, 9) shouldBe 0
        calculateVolumeGap(10, 8) shouldBe 1
        calculateVolumeGap(10, 1) shouldBe 8
    }

    @Test
    fun `calculateVolumeGap accounts for a range as the lower unit`() {
        // Volume 03-04 following Volume 01-02: no gap (end of lower is 2).
        calculateVolumeGap(volume(3, 4), volume(1, 2)) shouldBe 0
        // Volume 05 following Volume 01-02: volumes 3 and 4 are missing.
        calculateVolumeGap(volume(5), volume(1, 2)) shouldBe 2
    }

    @Test
    fun `calculateVolumeGap returns 0 if either are not valid volume numbers`() {
        calculateVolumeGap(volume(-1), volume(10)) shouldBe 0
        calculateVolumeGap(volume(99), volume(-1)) shouldBe 0

        calculateVolumeGap(-1, 10) shouldBe 0
        calculateVolumeGap(99, -1) shouldBe 0
    }

    private fun volume(number: Long, end: Long? = null) = Volume.create().copy(
        volumeNumber = number,
        volumeNumberEnd = end,
    )
}
