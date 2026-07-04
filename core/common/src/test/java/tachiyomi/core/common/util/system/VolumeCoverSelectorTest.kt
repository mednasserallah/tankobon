package tachiyomi.core.common.util.system

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VolumeCoverSelectorTest {

    @Test
    fun `returns null when there are no entries`() {
        assertNull(VolumeCoverSelector.selectCover(emptyList()))
    }

    @Test
    fun `returns null when no entry is an image`() {
        assertNull(
            VolumeCoverSelector.selectCover(
                listOf("ComicInfo.xml", "details.json", "notes.txt", "readme"),
            ),
        )
    }

    @Test
    fun `ignores non-image entries and picks the first image`() {
        assertEquals(
            "page1.jpg",
            VolumeCoverSelector.selectCover(
                listOf("ComicInfo.xml", "page1.jpg", "page2.jpg", "cover.txt"),
            ),
        )
    }

    @Test
    fun `natural sort puts page2 before page10`() {
        assertEquals(
            "page2.jpg",
            VolumeCoverSelector.selectCover(
                listOf("page10.jpg", "page2.jpg", "page20.jpg"),
            ),
        )
    }

    @Test
    fun `natural sort handles zero-padded numbers across padding boundary`() {
        // p000 < p001 < p010 < p099 < p100 numerically, not lexically.
        assertEquals(
            "p000.jpg",
            VolumeCoverSelector.selectCover(
                listOf("p100.jpg", "p010.jpg", "p099.jpg", "p001.jpg", "p000.jpg"),
            ),
        )
    }

    @Test
    fun `natural sort is stable when padding differs between entries`() {
        // Mixed padding: p0, p00, p000 all mean page 0 and must sort before p1.
        assertEquals(
            "p1.jpg",
            VolumeCoverSelector.selectCover(
                listOf("p10.jpg", "p2.jpg", "p1.jpg"),
            ),
        )
    }

    @Test
    fun `picks the tagged p000 cover for the real Homunculus naming pattern`() {
        val entries = listOf(
            "Homunculus - c001 (v01) - p001 [dig] [Seven Seas] [LuCaZ] {HQ}.jpg",
            "Homunculus - c001 (v01) - p000 [Cover] [dig] [Seven Seas] [LuCaZ] {HQ}.jpg",
            "Homunculus - c001 (v01) - p002 [dig] [Seven Seas] [LuCaZ] {HQ}.jpg",
            "Homunculus - c001 (v01) - p010 [dig] [Seven Seas] [LuCaZ] {HQ}.jpg",
        )
        assertEquals(
            "Homunculus - c001 (v01) - p000 [Cover] [dig] [Seven Seas] [LuCaZ] {HQ}.jpg",
            VolumeCoverSelector.selectCover(entries),
        )
    }

    @Test
    fun `picks the first page for the real Berserk naming pattern`() {
        val entries = listOf(
            "Berserk - 001 (v01) - p002 [Digital-HD] [danke-Empire].jpg",
            "Berserk - 001 (v01) - p000 [Digital-HD] [danke-Empire].jpg",
            "Berserk - 001 (v01) - p001 [Digital-HD] [danke-Empire].jpg",
            "Berserk - 001 (v01) - p100 [Digital-HD] [danke-Empire].jpg",
        )
        assertEquals(
            "Berserk - 001 (v01) - p000 [Digital-HD] [danke-Empire].jpg",
            VolumeCoverSelector.selectCover(entries),
        )
    }

    @Test
    fun `image detection is case-insensitive on the extension`() {
        assertEquals(
            "PAGE01.JPG",
            VolumeCoverSelector.selectCover(
                listOf("PAGE02.PNG", "PAGE01.JPG"),
            ),
        )
    }

    @Test
    fun `recognizes jpeg and webp extensions`() {
        assertEquals(
            "001.jpeg",
            VolumeCoverSelector.selectCover(
                listOf("002.webp", "001.jpeg"),
            ),
        )
    }
}
