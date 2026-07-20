package eu.kanade.tachiyomi.util.chapter

import eu.kanade.domain.chapter.model.applyFilters
import eu.kanade.tachiyomi.ui.manga.VolumeList
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.manga.model.Manga

/**
 * Gets next unread chapter with filters and sorting applied
 */
@JvmName("getNextUnreadFromChapters")
fun List<Volume>.getNextUnread(manga: Manga): Volume? {
    return applyFilters(manga).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.read && !it.isArchived }
        } else {
            chapters.find { !it.read && !it.isArchived }
        }
    }
}

/**
 * Gets next unread chapter with filters and sorting applied
 */
@JvmName("getNextUnreadFromChapterListItems")
fun List<VolumeList.Item>.getNextUnread(manga: Manga): Volume? {
    return applyFilters(manga).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.chapter.read && !it.chapter.isArchived }
        } else {
            chapters.find { !it.chapter.read && !it.chapter.isArchived }
        }
    }?.chapter
}
