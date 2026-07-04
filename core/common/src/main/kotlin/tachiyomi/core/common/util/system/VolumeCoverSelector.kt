package tachiyomi.core.common.util.system

import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder

/**
 * Picks the cover image out of a volume archive/folder's list of entry names.
 *
 * The cover is the **alphanumerically-first image using natural sort** — so `page2` precedes
 * `page10` and `p000` < `p001` < `p010` < `p100`. There is deliberately no `[Cover]`-tag,
 * `p000`-marker or `cover.*`-filename special-casing: across real scan-group naming conventions
 * the natural-sort-first image is already the front cover, and tag conventions vary too much to
 * rely on.
 *
 * This only inspects file *names* (extension + natural order); it never opens or decodes an entry,
 * so callers can list an archive's headers cheaply and only read the single chosen entry.
 */
object VolumeCoverSelector {

    /**
     * Common raster image extensions. Case-insensitive; a superset of [ImageUtil]'s canonical set
     * (adds `jpeg`/`heic`/`bmp`) so name-only detection doesn't miss real files where a magic-byte
     * fallback isn't available.
     */
    private val IMAGE_EXTENSIONS = setOf(
        "avif", "bmp", "gif", "heic", "heif", "jpeg", "jpg", "jxl", "png", "webp",
    )

    /**
     * @param entryNames the names of the file entries in the volume (archive entries or directory
     * file names). Non-file/directory entries should be filtered out by the caller.
     * @return the name of the entry to use as the cover, or `null` if none of the entries are
     * images (e.g. an empty or corrupt archive).
     */
    fun selectCover(entryNames: Iterable<String>): String? {
        return entryNames
            .filter(::isImageName)
            .minWithOrNull { a, b -> a.compareToCaseInsensitiveNaturalOrder(b) }
    }

    private fun isImageName(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension.isNotEmpty() && extension in IMAGE_EXTENSIONS
    }
}
