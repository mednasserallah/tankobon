package tachiyomi.domain.chapter.model

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

data class Volume(
    val id: Long,
    val mangaId: Long,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val dateFetch: Long,
    val sourceOrder: Long,
    val url: String,
    val name: String,
    val dateUpload: Long,
    val volumeNumber: Long,
    val volumeNumberEnd: Long?,
    val scanlator: String?,
    val lastModifiedAt: Long,
    val version: Long,
    val memo: JsonObject,
) {
    val isRecognizedNumber: Boolean
        get() = volumeNumber >= 0

    /** True when this reading unit spans more than one volume (an omnibus range). */
    val isRange: Boolean
        get() = volumeNumberEnd != null && volumeNumberEnd > volumeNumber

    fun copyFrom(other: Volume): Volume {
        return copy(
            name = other.name,
            url = other.url,
            dateUpload = other.dateUpload,
            volumeNumber = other.volumeNumber,
            volumeNumberEnd = other.volumeNumberEnd,
            scanlator = other.scanlator?.ifBlank { null },
        )
    }

    companion object {
        fun create() = Volume(
            id = -1,
            mangaId = -1,
            read = false,
            bookmark = false,
            lastPageRead = 0,
            dateFetch = 0,
            sourceOrder = 0,
            url = "",
            name = "",
            dateUpload = -1,
            volumeNumber = -1,
            volumeNumberEnd = null,
            scanlator = null,
            lastModifiedAt = 0,
            version = 1,
            memo = JsonObject.EMPTY,
        )
    }
}
