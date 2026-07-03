package tachiyomi.domain.chapter.model

/**
 * A request for the per-volume cover thumbnail: the image extracted from *inside* a volume's
 * archive/folder (distinct from the series-level `cover.jpg`).
 *
 * [lastModified] is the volume file's mtime (carried on [Volume.dateUpload]); it is part of the
 * cache key so replacing the file on disk transparently invalidates the cached thumbnail.
 */
data class VolumeCover(
    val volumeId: Long,
    val url: String,
    val lastModified: Long,
)

fun Volume.asVolumeCover(): VolumeCover {
    return VolumeCover(
        volumeId = id,
        url = url,
        lastModified = dateUpload,
    )
}
