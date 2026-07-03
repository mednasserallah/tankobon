package mihon.domain.source.models

import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.manga.model.Manga

data class RemoteMangaUpdate(
    val manga: Manga,
    val newChapters: List<Volume>,
)
