package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import eu.kanade.tachiyomi.data.cache.VolumeCoverCache
import logcat.LogPriority
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.VolumeCover
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.injectLazy
import java.io.File
import kotlin.math.max

/**
 * A [Fetcher] for per-volume cover thumbnails ([VolumeCover]).
 *
 * On a cache miss it asks [LocalSource] for the raw bytes of the volume's first image (only that
 * one entry is read from the archive), downscales them to a thumbnail, and persists the result in
 * [VolumeCoverCache]. Subsequent loads read the cached thumbnail directly. Formats [BitmapFactory]
 * can't decode (e.g. JPEG XL) fall back to caching the raw bytes, which Coil's own decoder handles.
 */
class VolumeCoverFetcher(
    private val url: String,
    private val options: Options,
    private val cacheFileLazy: Lazy<File>,
    private val diskCacheKeyLazy: Lazy<String>,
    private val localSourceLazy: Lazy<LocalSource?>,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val cacheFile = cacheFileLazy.value
        if (cacheFile.exists() && options.diskCachePolicy.readEnabled) {
            return fileLoader(cacheFile)
        }

        val localSource = localSourceLazy.value ?: error("Local source unavailable")
        val bytes = localSource.getVolumeCoverBytes(url) ?: error("No cover image in volume: $url")

        // Downscale before caching so we never persist a full-resolution page as a thumbnail.
        // If BitmapFactory can't decode the format, cache the raw bytes for Coil's decoder instead.
        val thumbnail = downscaleToJpeg(bytes, THUMBNAIL_MAX_SIZE) ?: bytes

        if (options.diskCachePolicy.writeEnabled) {
            writeToCache(cacheFile, thumbnail)
        }

        return if (cacheFile.exists()) {
            fileLoader(cacheFile)
        } else {
            SourceFetchResult(
                source = ImageSource(source = Buffer().apply { write(thumbnail) }, fileSystem = FileSystem.SYSTEM),
                mimeType = "image/*",
                dataSource = DataSource.DISK,
            )
        }
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                file = file.toOkioPath(),
                fileSystem = FileSystem.SYSTEM,
                diskCacheKey = diskCacheKeyLazy.value,
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private fun writeToCache(file: File, data: ByteArray) {
        try {
            file.parentFile?.mkdirs()
            // Write to a temp file then rename so a concurrent read never sees a partial thumbnail.
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeBytes(data)
            if (!tmp.renameTo(file)) {
                tmp.delete()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to write volume cover thumbnail ${file.name}" }
        }
    }

    private fun downscaleToJpeg(bytes: ByteArray, maxSize: Int): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
        return try {
            Buffer().let { buffer ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, buffer.outputStream())
                buffer.readByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    /** Largest power-of-two subsample that keeps the longer edge >= [maxSize]. */
    private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        var longEdge = max(width, height)
        while (longEdge / 2 >= maxSize) {
            longEdge /= 2
            sample *= 2
        }
        return sample
    }

    class Factory : Fetcher.Factory<VolumeCover> {

        private val volumeCoverCache: VolumeCoverCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: VolumeCover, options: Options, imageLoader: ImageLoader): Fetcher {
            return VolumeCoverFetcher(
                url = data.url,
                options = options,
                cacheFileLazy = lazy { volumeCoverCache.getCoverFile(data.url, data.lastModified) },
                diskCacheKeyLazy = lazy { imageLoader.components.key(data, options)!! },
                localSourceLazy = lazy { sourceManager.get(LocalSource.ID) as? LocalSource },
            )
        }
    }

    companion object {
        private const val THUMBNAIL_MAX_SIZE = 640
        private const val JPEG_QUALITY = 90
    }
}
