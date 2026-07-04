package eu.kanade.tachiyomi.data.coil

import coil3.key.Keyer
import coil3.request.Options
import tachiyomi.domain.chapter.model.VolumeCover

/**
 * Coil memory-cache key for a [VolumeCover]. Includes the volume file's last-modified time so a
 * replaced file busts the in-memory cache the same way it busts the disk thumbnail.
 */
class VolumeCoverKeyer : Keyer<VolumeCover> {
    override fun key(data: VolumeCover, options: Options): String {
        return "${data.url};${data.lastModified}"
    }
}
