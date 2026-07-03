package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.repository.VolumeRepository

class GetVolume(
    private val chapterRepository: VolumeRepository,
) {

    suspend fun await(id: Long): Volume? {
        return try {
            chapterRepository.getVolumeById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun await(url: String, mangaId: Long): Volume? {
        return try {
            chapterRepository.getVolumeByUrlAndMangaId(url, mangaId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }
}
