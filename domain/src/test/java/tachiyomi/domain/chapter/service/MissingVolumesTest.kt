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
        emptyList<Long>().missingVolumesCount() shouldBe 0
    }

    @Test
    fun `missingVolumesCount returns 0 when all unknown volume numbers`() {
        listOf(-1L, -1L, -1L).missingVolumesCount() shouldBe 0
    }

    @Test
    fun `missingVolumesCount ignores repeated volume numbers`() {
        listOf(1L, 1L, 2L, 2L, 3L).missingVolumesCount() shouldBe 0
    }

    @Test
    fun `missingVolumesCount returns number of missing volumes`() {
        listOf(-1L, 1L, 2L, 4L, 6L, 10L, 11L).missingVolumesCount() shouldBe 5
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
    fun `calculateVolumeGap returns 0 if either are not valid volume numbers`() {
        calculateVolumeGap(volume(-1), volume(10)) shouldBe 0
        calculateVolumeGap(volume(99), volume(-1)) shouldBe 0

        calculateVolumeGap(-1, 10) shouldBe 0
        calculateVolumeGap(99, -1) shouldBe 0
    }

    private fun volume(number: Long) = Volume.create().copy(
        volumeNumber = number,
    )
}
