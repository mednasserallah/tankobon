package eu.kanade.presentation.util

import tachiyomi.domain.chapter.model.Volume

/**
 * Formats a whole-number volume for display, e.g. `1` → "1".
 */
fun formatVolumeNumber(volumeNumber: Long): String {
    return volumeNumber.toString()
}

/**
 * Formats a volume's number for display, rendering an omnibus range as `start-end`
 * (e.g. `Volume 01-02` → "1-2") and a single volume as just its number.
 */
fun formatVolumeNumber(volume: Volume): String {
    return if (volume.isRange) {
        "${volume.volumeNumber}-${volume.volumeNumberEnd}"
    } else {
        volume.volumeNumber.toString()
    }
}
