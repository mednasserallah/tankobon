package tachiyomi.domain.history.interactor

import tachiyomi.domain.chapter.interactor.GetVolumesByMangaId
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.service.getVolumeSort
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.manga.interactor.GetManga
import kotlin.math.max

class GetNextVolumes(
    private val getVolumesByMangaId: GetVolumesByMangaId,
    private val getManga: GetManga,
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(onlyUnread: Boolean = true): List<Volume> {
        val history = historyRepository.getLastHistory() ?: return emptyList()
        return await(history.mangaId, history.volumeId, onlyUnread)
    }

    suspend fun await(mangaId: Long, onlyUnread: Boolean = true): List<Volume> {
        val manga = getManga.await(mangaId) ?: return emptyList()
        val chapters = getVolumesByMangaId.await(mangaId, applyScanlatorFilter = true)
            .sortedWith(getVolumeSort(manga, sortDescending = false))

        return if (onlyUnread) {
            chapters.filterNot { it.read }
        } else {
            chapters
        }
    }

    suspend fun await(
        mangaId: Long,
        fromChapterId: Long,
        onlyUnread: Boolean = true,
    ): List<Volume> {
        val chapters = await(mangaId, onlyUnread)
        val currChapterIndex = chapters.indexOfFirst { it.id == fromChapterId }
        val nextChapters = chapters.subList(max(0, currChapterIndex), chapters.size)

        if (onlyUnread) {
            return nextChapters
        }

        // The "next chapter" is either:
        // - The current chapter if it isn't completely read
        // - The chapters after the current chapter if the current one is completely read
        val fromChapter = chapters.getOrNull(currChapterIndex)
        return if (fromChapter != null && !fromChapter.read) {
            nextChapters
        } else {
            nextChapters.drop(1)
        }
    }
}
