package eu.kanade.domain.chapter.model

import eu.kanade.tachiyomi.ui.manga.ChapterList
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.applyFilter

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
@JvmName("applyFiltersToChapters")
fun List<Chapter>.applyFilters(manga: Manga): List<Chapter> {
    val unreadFilter = manga.unreadFilter
    val bookmarkedFilter = manga.bookmarkedFilter

    return filter { chapter -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { chapter -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .sortedWith(getChapterSort(manga))
}

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
@JvmName("applyFiltersToChapterListItems")
fun List<ChapterList.Item>.applyFilters(manga: Manga): Sequence<ChapterList.Item> {
    val unreadFilter = manga.unreadFilter
    val bookmarkedFilter = manga.bookmarkedFilter
    return asSequence()
        .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .sortedWith { (chapter1), (chapter2) -> getChapterSort(manga).invoke(chapter1, chapter2) }
}
