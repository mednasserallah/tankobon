package eu.kanade.tachiyomi.util.chapter

import eu.kanade.domain.chapter.model.applyFilters
import eu.kanade.tachiyomi.ui.manga.ChapterList
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

/**
 * Gets next unread chapter with filters and sorting applied
 */
@JvmName("getNextUnreadFromChapters")
fun List<Chapter>.getNextUnread(manga: Manga): Chapter? {
    return applyFilters(manga).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.read }
        } else {
            chapters.find { !it.read }
        }
    }
}

/**
 * Gets next unread chapter with filters and sorting applied
 */
@JvmName("getNextUnreadFromChapterListItems")
fun List<ChapterList.Item>.getNextUnread(manga: Manga): Chapter? {
    return applyFilters(manga).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.chapter.read }
        } else {
            chapters.find { !it.chapter.read }
        }
    }?.chapter
}
