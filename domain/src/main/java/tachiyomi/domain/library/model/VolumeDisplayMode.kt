package tachiyomi.domain.library.model

/**
 * How the per-manga volume list is presented: the default text [List], or a [Grid] of per-volume
 * cover thumbnails. Persisted app-wide via `LibraryPreferences.volumeDisplayMode`.
 */
enum class VolumeDisplayMode {
    List,
    Grid,
}
