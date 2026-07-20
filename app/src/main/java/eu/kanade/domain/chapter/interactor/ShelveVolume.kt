package eu.kanade.domain.chapter.interactor

import android.content.Context
import coil3.imageLoader
import coil3.request.ImageRequest
import eu.kanade.tachiyomi.data.cache.VolumeCoverCache
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.UpdateVolume
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.model.VolumeUpdate
import tachiyomi.domain.chapter.model.asVolumeCover
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource

/**
 * Shelves volumes: deletes each volume's file from disk to free space while keeping its DB row,
 * metadata and cover. The cover thumbnail is force-cached *before* the file is deleted (otherwise
 * it could never be re-extracted), then the file is removed and the row flagged as archived.
 *
 * The library sync ([eu.kanade.domain.chapter.interactor.SyncVolumesWithSource]) keeps archived
 * rows even though their file is gone, and un-shelves them automatically if the file reappears.
 */
class ShelveVolume(
    private val context: Context,
    private val sourceManager: SourceManager,
    private val volumeCoverCache: VolumeCoverCache,
    private val updateVolume: UpdateVolume,
) {

    /**
     * @param volumes the volumes to shelve; already-archived volumes are skipped.
     * @return the number of volumes successfully shelved.
     */
    suspend fun await(volumes: List<Volume>): Int = withIOContext {
        val localSource = sourceManager.get(LocalSource.ID) as? LocalSource
            ?: return@withIOContext 0

        val updates = mutableListOf<VolumeUpdate>()
        for (volume in volumes) {
            if (volume.isArchived) continue
            try {
                ensureCoverCached(volume)
                localSource.deleteVolume(volume.url)
                updates.add(VolumeUpdate(id = volume.id, isArchived = true))
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to shelve volume ${volume.url}" }
            }
        }
        if (updates.isNotEmpty()) {
            updateVolume.awaitAll(updates)
        }
        updates.size
    }

    /**
     * Ensures the volume's cover thumbnail is written to [VolumeCoverCache] while the file still
     * exists. Reuses the normal Coil fetch path so the exact extraction/downscale/write logic runs.
     * If the cover is already cached, or extraction fails, this is a no-op — a missing cover simply
     * falls back to the placeholder, exactly like any coverless volume.
     */
    private suspend fun ensureCoverCached(volume: Volume) {
        val cover = volume.asVolumeCover()
        if (volumeCoverCache.getCoverFile(cover.url, cover.lastModified).exists()) return

        val request = ImageRequest.Builder(context)
            .data(cover)
            .build()
        context.imageLoader.execute(request)
    }
}
