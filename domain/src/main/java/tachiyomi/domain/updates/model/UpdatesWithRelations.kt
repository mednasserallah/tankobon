package tachiyomi.domain.updates.model

import tachiyomi.domain.manga.model.MangaCover

data class UpdatesWithRelations(
    val mangaId: Long,
    val mangaTitle: String,
    val volumeId: Long,
    val volumeName: String,
    val scanlator: String?,
    val volumeUrl: String,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: MangaCover,
)
