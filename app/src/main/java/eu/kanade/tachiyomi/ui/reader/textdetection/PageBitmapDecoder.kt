package eu.kanade.tachiyomi.ui.reader.textdetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

/**
 * Decodes a reader page into a Bitmap suitable for ML Kit OCR, downscaled so the longest side is
 * at most [maxDimension] pixels. Manga pages can be very large (multi-MB, 2000px+ tall); bounding
 * the decode keeps memory in check while leaving enough resolution for recognition.
 *
 * [streamProvider] is invoked twice (a bounds pass then the real decode), which is why the reader's
 * re-invokable `ReaderPage.stream` factory is used rather than a one-shot stream. Returns null if
 * the image can't be decoded.
 */
fun decodePageBitmap(streamProvider: () -> InputStream, maxDimension: Int = 2048): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    streamProvider().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    val longestSide = maxOf(bounds.outWidth, bounds.outHeight)
    while (longestSide / sampleSize > maxDimension) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return streamProvider().use { BitmapFactory.decodeStream(it, null, options) }
}
