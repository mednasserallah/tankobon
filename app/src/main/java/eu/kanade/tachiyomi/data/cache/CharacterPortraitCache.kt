package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.graphics.Bitmap
import eu.kanade.tachiyomi.util.storage.DiskUtil
import java.io.File

/**
 * Persistent, app-private store for character-notebook portrait thumbnails (Task 9).
 *
 * Portraits are square JPEG crops taken from a page while reading, one per character. They live in
 * `getExternalFilesDir("character_portraits")` (falling back to internal storage) — persistent,
 * uninstall-scoped storage, NOT the OS-evictable cache — so a saved character keeps its face until
 * the character (or the app) is removed. Filenames are the MD5 of the character id, mirroring
 * [CoverCache]'s custom-cover convention.
 */
class CharacterPortraitCache(private val context: Context) {

    private val cacheDir = getCacheDir(PORTRAITS_DIR)

    /** The (possibly not-yet-existing) portrait file for [characterId]. */
    fun getPortraitFile(characterId: Long): File {
        return File(cacheDir, DiskUtil.hashKeyForDisk("character:$characterId"))
    }

    /**
     * Compresses [bitmap] to the portrait file for [characterId] as JPEG and returns its absolute
     * path (to store on the character row). Overwrites any existing portrait for that id.
     */
    fun writePortrait(characterId: Long, bitmap: Bitmap): String {
        val file = getPortraitFile(characterId)
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return file.absolutePath
    }

    /** Deletes the portrait file for [characterId]. Returns true if a file was removed. */
    fun deletePortrait(characterId: Long): Boolean {
        return getPortraitFile(characterId).let { it.exists() && it.delete() }
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }

    companion object {
        private const val PORTRAITS_DIR = "character_portraits"
        private const val JPEG_QUALITY = 90
    }
}
