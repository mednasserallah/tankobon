package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.repository.VolumeRepository

class GetBookmarkedVolumesByMangaId(
    private val chapterRepository: VolumeRepository,
) {

    suspend fun await(mangaId: Long): List<Volume> {
        return try {
            chapterRepository.getBookmarkedVolumesByMangaId(mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
