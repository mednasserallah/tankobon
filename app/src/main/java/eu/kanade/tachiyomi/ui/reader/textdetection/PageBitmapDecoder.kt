package eu.kanade.tachiyomi.ui.reader.textdetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import java.io.InputStream

/**
 * Decodes a reader page into a Bitmap suitable for ML Kit OCR, downscaled so the longest side is at
 * most [maxDimension] pixels. Manga pages can be very large (multi-MB, 2000px+ tall); bounding the
 * decode keeps memory in check while leaving enough resolution for recognition.
 *
 * If [region] is non-null (the user is zoomed in), only that region of the original image — in
 * source-image pixel coordinates — is decoded via [BitmapRegionDecoder], so detection scopes to
 * what's on screen. Because a smaller region fills more of the [maxDimension] budget, small text is
 * effectively higher-resolution to the recognizer, which also tends to improve accuracy.
 *
 * The page's compressed bytes are read fully into memory first, then decoded from that byte array.
 * This deliberately avoids `BitmapFactory.decodeStream` on the archive stream: the local
 * `ArchiveInputStream.read(b, off, len)` mis-handles a partial read (its wrapped `ByteBuffer` is
 * `clear()`ed back to full capacity, so it can return more than `len` bytes), which corrupts Skia's
 * incremental peek/rewind decode and crashes natively. Reading the whole entry uses the
 * off=0/len=capacity path, where that quirk is harmless. Returns null if the image can't be decoded.
 */
fun decodePageBitmap(
    streamProvider: () -> InputStream,
    maxDimension: Int = 2048,
    region: Rect? = null,
): Bitmap? {
    val bytes = streamProvider().use { it.readBytes() }
    if (bytes.isEmpty()) return null
    return if (region == null) {
        decodeFullBitmap(bytes, maxDimension)
    } else {
        decodeRegionBitmap(bytes, region, maxDimension)
    }
}

private fun decodeFullBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), maxDimension)
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun decodeRegionBitmap(bytes: ByteArray, region: Rect, maxDimension: Int): Bitmap? {
    @Suppress("DEPRECATION")
    val decoder = BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false) ?: return null
    return try {
        val clamped = Rect(
            region.left.coerceIn(0, decoder.width),
            region.top.coerceIn(0, decoder.height),
            region.right.coerceIn(0, decoder.width),
            region.bottom.coerceIn(0, decoder.height),
        )
        if (clamped.width() <= 0 || clamped.height() <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(maxOf(clamped.width(), clamped.height()), maxDimension)
        }
        decoder.decodeRegion(clamped, options)
    } finally {
        decoder.recycle()
    }
}

private fun sampleSizeFor(longestSide: Int, maxDimension: Int): Int {
    var sampleSize = 1
    while (longestSide / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}
