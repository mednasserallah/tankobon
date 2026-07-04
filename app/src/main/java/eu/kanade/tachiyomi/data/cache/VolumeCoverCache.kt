package eu.kanade.tachiyomi.data.cache

import android.content.Context
import eu.kanade.tachiyomi.util.storage.DiskUtil
import java.io.File

/**
 * Disk cache for **per-volume** cover thumbnails (the downscaled first image extracted from inside
 * each volume archive/folder). Separate from [CoverCache], which caches series-level covers.
 *
 * A thumbnail's filename is the MD5 of `"$url;$lastModified"`, so a volume file replaced on disk
 * (new mtime) maps to a new cache file and is transparently re-extracted; the stale file is simply
 * orphaned (reclaimed by [clear]).
 */
class VolumeCoverCache(private val context: Context) {

    private val cacheDir = getCacheDir(VOLUME_COVERS_DIR)

    /**
     * Returns the cache file for a volume cover, keyed by its url and last-modified time. The file
     * may not exist yet — callers should check [File.exists].
     */
    fun getCoverFile(url: String, lastModified: Long): File {
        return File(cacheDir, DiskUtil.hashKeyForDisk("$url;$lastModified"))
    }

    /**
     * Deletes every cached volume thumbnail.
     *
     * @return the number of files deleted.
     */
    fun clear(): Int {
        return cacheDir.listFiles()?.count { it.isFile && it.delete() } ?: 0
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }

    companion object {
        private const val VOLUME_COVERS_DIR = "volume_covers"
    }
}
