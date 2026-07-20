package tachiyomi.data.chapter

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import logcat.LogPriority
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.MemoColumnAdapter.encode
import tachiyomi.data.subscribeToList
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.model.VolumeUpdate
import tachiyomi.domain.chapter.repository.VolumeRepository

class VolumeRepositoryImpl(
    private val database: Database,
) : VolumeRepository {

    override suspend fun addAll(chapters: List<Volume>): List<Volume> {
        return try {
            database.transactionWithResult {
                chapters.map { chapter ->
                    val volumeId = database.volumesQueries.insertReturningId(
                        chapter.mangaId,
                        chapter.url,
                        chapter.name,
                        chapter.scanlator,
                        chapter.read,
                        chapter.bookmark,
                        chapter.lastPageRead,
                        chapter.volumeNumber,
                        chapter.volumeNumberEnd,
                        chapter.sourceOrder,
                        chapter.dateFetch,
                        chapter.dateUpload,
                        chapter.version,
                        chapter.memo,
                        chapter.isArchived,
                    )
                        .awaitAsOne()
                    chapter.copy(id = volumeId)
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    override suspend fun update(chapterUpdate: VolumeUpdate) {
        partialUpdate(chapterUpdate)
    }

    override suspend fun updateAll(chapterUpdates: List<VolumeUpdate>) {
        partialUpdate(*chapterUpdates.toTypedArray())
    }

    private suspend fun partialUpdate(vararg chapterUpdates: VolumeUpdate) {
        database.transaction {
            chapterUpdates.forEach { chapterUpdate ->
                database.volumesQueries.update(
                    mangaId = chapterUpdate.mangaId,
                    url = chapterUpdate.url,
                    name = chapterUpdate.name,
                    scanlator = chapterUpdate.scanlator,
                    read = chapterUpdate.read,
                    bookmark = chapterUpdate.bookmark,
                    lastPageRead = chapterUpdate.lastPageRead,
                    volumeNumber = chapterUpdate.volumeNumber,
                    volumeNumberEnd = chapterUpdate.volumeNumberEnd,
                    sourceOrder = chapterUpdate.sourceOrder,
                    dateFetch = chapterUpdate.dateFetch,
                    dateUpload = chapterUpdate.dateUpload,
                    volumeId = chapterUpdate.id,
                    version = chapterUpdate.version,
                    isSyncing = 0,
                    memo = chapterUpdate.memo?.let(MemoColumnAdapter::encode),
                    isArchived = chapterUpdate.isArchived,
                )
            }
        }
    }

    override suspend fun removeVolumesWithIds(chapterIds: List<Long>) {
        try {
            database.volumesQueries.removeVolumesWithIds(chapterIds)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun getChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean): List<Volume> {
        return database.volumesQueries
            .getVolumesByMangaId(mangaId, applyScanlatorFilter.toLong(), ::mapVolume)
            .awaitAsList()
    }

    override suspend fun getScanlatorsByMangaId(mangaId: Long): List<String> {
        return database.volumesQueries
            .getScanlatorsByMangaId(mangaId) { it.orEmpty() }
            .awaitAsList()
    }

    override fun getScanlatorsByMangaIdAsFlow(mangaId: Long): Flow<List<String>> {
        return database.volumesQueries
            .getScanlatorsByMangaId(mangaId) { it.orEmpty() }
            .subscribeToList()
    }

    override suspend fun getBookmarkedVolumesByMangaId(mangaId: Long): List<Volume> {
        return database.volumesQueries
            .getBookmarkedVolumesByMangaId(mangaId, ::mapVolume)
            .awaitAsList()
    }

    override suspend fun getVolumeById(id: Long): Volume? {
        return database.volumesQueries
            .getVolumeById(id, ::mapVolume)
            .awaitAsOneOrNull()
    }

    override suspend fun getChapterByMangaIdAsFlow(mangaId: Long, applyScanlatorFilter: Boolean): Flow<List<Volume>> {
        return database.volumesQueries
            .getVolumesByMangaId(mangaId, applyScanlatorFilter.toLong(), ::mapVolume)
            .subscribeToList()
    }

    override suspend fun getVolumeByUrlAndMangaId(url: String, mangaId: Long): Volume? {
        return database.volumesQueries
            .getVolumeByUrlAndMangaId(url, mangaId, ::mapVolume)
            .awaitAsOneOrNull()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun mapVolume(
        id: Long,
        mangaId: Long,
        url: String,
        name: String,
        scanlator: String?,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        volumeNumber: Long,
        volumeNumberEnd: Long?,
        sourceOrder: Long,
        dateFetch: Long,
        dateUpload: Long,
        lastModifiedAt: Long,
        version: Long,
        isSyncing: Long,
        memo: JsonObject,
        isArchived: Boolean,
    ): Volume = Volume(
        id = id,
        mangaId = mangaId,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        dateFetch = dateFetch,
        sourceOrder = sourceOrder,
        url = url,
        name = name,
        dateUpload = dateUpload,
        volumeNumber = volumeNumber,
        volumeNumberEnd = volumeNumberEnd,
        scanlator = scanlator,
        lastModifiedAt = lastModifiedAt,
        version = version,
        memo = memo,
        isArchived = isArchived,
    )
}
