package tachiyomi.domain.manga.model

data class MangaWithVolumeCount(
    val manga: Manga,
    val chapterCount: Long,
)
