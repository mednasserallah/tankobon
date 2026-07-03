package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.repository.VolumeRepository

class GetVolumeByUrlAndMangaId(
    private val chapterRepository: VolumeRepository,
) {

    suspend fun await(url: String, sourceId: Long): Volume? {
        return try {
            chapterRepository.getVolumeByUrlAndMangaId(url, sourceId)
        } catch (e: Exception) {
            null
        }
    }
}
