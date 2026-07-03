package eu.kanade.tachiyomi.ui.reader.textdetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

/**
 * Decodes a reader page into a Bitmap suitable for ML Kit OCR, downscaled so the longest side is
 * at most [maxDimension] pixels. Manga pages can be very large (multi-MB, 2000px+ tall); bounding
 * the decode keeps memory in check while leaving enough resolution for recognition.
 *
 * The page's compressed bytes are read fully into memory first, then decoded from that byte array
 * (a bounds pass, then the real decode). This deliberately avoids `BitmapFactory.decodeStream` on
 * the archive stream: the local `ArchiveInputStream.read(b, off, len)` mis-handles a partial read
 * (its wrapped `ByteBuffer` is `clear()`ed back to full capacity, so it can return more than `len`
 * bytes), which corrupts Skia's incremental peek/rewind decode and crashes natively. Reading the
 * whole entry uses the off=0/len=capacity path, where that quirk is harmless. Returns null if the
 * image can't be decoded.
 */
fun decodePageBitmap(streamProvider: () -> InputStream, maxDimension: Int = 2048): Bitmap? {
    val bytes = streamProvider().use { it.readBytes() }
    if (bytes.isEmpty()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    val longestSide = maxOf(bounds.outWidth, bounds.outHeight)
    while (longestSide / sampleSize > maxDimension) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
