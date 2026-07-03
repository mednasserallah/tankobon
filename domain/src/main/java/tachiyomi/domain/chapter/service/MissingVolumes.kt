package tachiyomi.domain.chapter.service

import tachiyomi.domain.chapter.model.Volume

fun List<Long>.missingVolumesCount(): Int {
    if (this.isEmpty()) {
        return 0
    }

    val volumes = this
        // Ignore unknown volume numbers
        .filterNot { it == -1L }
        // Only keep unique volumes so that -1 or 16 are not counted multiple times
        .distinct()
        .sorted()

    if (volumes.isEmpty()) {
        return 0
    }

    var missingVolumesCount = 0
    var previousVolume = 0L // The actual volume number, not the array index

    // We go from 0 to lastVolume - Make sure to use the current index instead of the value
    for (i in volumes.indices) {
        val currentVolume = volumes[i]
        if (currentVolume > previousVolume + 1) {
            // Add the amount of missing volumes
            missingVolumesCount += (currentVolume - previousVolume - 1).toInt()
        }
        previousVolume = currentVolume
    }

    return missingVolumesCount
}

fun calculateVolumeGap(higherVolume: Volume?, lowerVolume: Volume?): Int {
    if (higherVolume == null || lowerVolume == null) return 0
    if (!higherVolume.isRecognizedNumber || !lowerVolume.isRecognizedNumber) return 0
    return calculateVolumeGap(higherVolume.volumeNumber, lowerVolume.volumeNumber)
}

fun calculateVolumeGap(higherVolumeNumber: Long, lowerVolumeNumber: Long): Int {
    if (higherVolumeNumber < 0L || lowerVolumeNumber < 0L) return 0
    return (higherVolumeNumber - lowerVolumeNumber - 1).toInt()
}
