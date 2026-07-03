package tachiyomi.domain.chapter.model

import kotlinx.serialization.json.JsonObject

data class VolumeUpdate(
    val id: Long,
    val mangaId: Long? = null,
    val read: Boolean? = null,
    val bookmark: Boolean? = null,
    val lastPageRead: Long? = null,
    val dateFetch: Long? = null,
    val sourceOrder: Long? = null,
    val url: String? = null,
    val name: String? = null,
    val dateUpload: Long? = null,
    val volumeNumber: Long? = null,
    val volumeNumberEnd: Long? = null,
    val scanlator: String? = null,
    val version: Long? = null,
    val memo: JsonObject? = null,
)

fun Volume.toVolumeUpdate(): VolumeUpdate {
    return VolumeUpdate(
        id,
        mangaId,
        read,
        bookmark,
        lastPageRead,
        dateFetch,
        sourceOrder,
        url,
        name,
        dateUpload,
        volumeNumber,
        volumeNumberEnd,
        scanlator,
        version,
        memo,
    )
}
