package eu.kanade.tachiyomi.ui.reader.character

import android.graphics.Bitmap
import java.io.InputStream

/**
 * Context for the square (1:1) crop tool: the current page decoded for display plus everything
 * needed to re-decode a full-resolution square from it. Held on the reader's dialog state.
 *
 * Not a `data class` — it wraps a [Bitmap] and a stream lambda, which have no meaningful equality.
 *
 * @param displayBitmap the whole page, downscaled for on-screen manipulation.
 * @param sourceWidth/[sourceHeight] the *true* source-image dimensions, so a square in
 *   [displayBitmap] pixels maps to full-resolution source pixels.
 * @param streamProvider re-invokable factory for the page's compressed bytes, used to region-decode
 *   the crop at full resolution on confirm.
 * @param editingCharacterId id of the character being re-cropped, or -1 for a brand-new character.
 */
class CharacterCropTarget(
    val mangaId: Long,
    val editingCharacterId: Long,
    val initialName: String,
    val initialNote: String,
    val displayBitmap: Bitmap,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val streamProvider: () -> InputStream,
)

/**
 * Context for the save form: the confirmed square [portrait] crop plus the fields to seed the name
 * and note inputs. Not a `data class` (wraps a [Bitmap]).
 *
 * @param editingCharacterId id of the character being edited, or -1 for a new one.
 */
class CharacterSaveTarget(
    val mangaId: Long,
    val editingCharacterId: Long,
    val initialName: String,
    val initialNote: String,
    val portrait: Bitmap,
)
