package tachiyomi.domain.chapter.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.model.VolumeUpdate

interface VolumeRepository {

    suspend fun addAll(chapters: List<Volume>): List<Volume>

    suspend fun update(chapterUpdate: VolumeUpdate)

    suspend fun updateAll(chapterUpdates: List<VolumeUpdate>)

    suspend fun removeVolumesWithIds(chapterIds: List<Long>)

    suspend fun getChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Volume>

    suspend fun getScanlatorsByMangaId(mangaId: Long): List<String>

    fun getScanlatorsByMangaIdAsFlow(mangaId: Long): Flow<List<String>>

    suspend fun getBookmarkedVolumesByMangaId(mangaId: Long): List<Volume>

    suspend fun getVolumeById(id: Long): Volume?

    suspend fun getChapterByMangaIdAsFlow(mangaId: Long, applyScanlatorFilter: Boolean = false): Flow<List<Volume>>

    suspend fun getVolumeByUrlAndMangaId(url: String, mangaId: Long): Volume?
}
