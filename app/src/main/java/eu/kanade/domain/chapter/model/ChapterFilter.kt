package eu.kanade.domain.chapter.model

import eu.kanade.tachiyomi.ui.manga.VolumeList
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.service.getVolumeSort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.applyFilter

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
@JvmName("applyFiltersToChapters")
fun List<Volume>.applyFilters(manga: Manga): List<Volume> {
    val unreadFilter = manga.unreadFilter
    val bookmarkedFilter = manga.bookmarkedFilter

    return filter { chapter -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { chapter -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .sortedWith(getVolumeSort(manga))
}

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
@JvmName("applyFiltersToChapterListItems")
fun List<VolumeList.Item>.applyFilters(manga: Manga): Sequence<VolumeList.Item> {
    val unreadFilter = manga.unreadFilter
    val bookmarkedFilter = manga.bookmarkedFilter
    return asSequence()
        .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .sortedWith { (chapter1), (chapter2) -> getVolumeSort(manga).invoke(chapter1, chapter2) }
}
