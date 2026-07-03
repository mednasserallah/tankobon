package tachiyomi.domain.chapter.service

import tachiyomi.domain.chapter.model.Volume

/**
 * Counts the volumes missing between the lowest and highest owned volume.
 *
 * Range files (omnibus, `Volume 01-02`) cover every volume from `volumeNumber` to
 * `volumeNumberEnd`, so they are expanded before counting — an `01-02` plus an `03-04` file
 * leaves nothing missing.
 */
fun List<Volume>.missingVolumesCount(): Int {
    val present = this
        .asSequence()
        .filter { it.isRecognizedNumber }
        .flatMap { it.volumeNumber..(it.volumeNumberEnd ?: it.volumeNumber) }
        .toSortedSet()

    if (present.isEmpty()) return 0

    // Every whole number between the first and last owned volume that isn't covered.
    return (present.last() - present.first() + 1 - present.size).toInt()
}

fun calculateVolumeGap(higherVolume: Volume?, lowerVolume: Volume?): Int {
    if (higherVolume == null || lowerVolume == null) return 0
    if (!higherVolume.isRecognizedNumber || !lowerVolume.isRecognizedNumber) return 0
    // The lower unit may itself be a range; measure the gap from the end of what it covers.
    return calculateVolumeGap(
        higherVolume.volumeNumber,
        lowerVolume.volumeNumberEnd ?: lowerVolume.volumeNumber,
    )
}

fun calculateVolumeGap(higherVolumeNumber: Long, lowerVolumeNumber: Long): Int {
    if (higherVolumeNumber < 0L || lowerVolumeNumber < 0L) return 0
    return (higherVolumeNumber - lowerVolumeNumber - 1).toInt().coerceAtLeast(0)
}
