@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SVolume
import java.io.Serializable
import tachiyomi.domain.chapter.model.Volume as DomainVolume

interface Volume : SVolume, Serializable {

    var id: Long?

    var manga_id: Long?

    var read: Boolean

    var bookmark: Boolean

    var last_page_read: Int

    var date_fetch: Long

    var source_order: Int

    var last_modified: Long

    var version: Long
}

val Volume.isRecognizedNumber: Boolean
    get() = volume_number >= 0

fun Volume.toDomainVolume(): DomainVolume? {
    if (id == null || manga_id == null) return null
    return DomainVolume(
        id = id!!,
        mangaId = manga_id!!,
        read = read,
        bookmark = bookmark,
        lastPageRead = last_page_read.toLong(),
        dateFetch = date_fetch,
        sourceOrder = source_order.toLong(),
        url = url,
        name = name,
        dateUpload = date_upload,
        volumeNumber = volume_number.toLong(),
        volumeNumberEnd = volume_number_end?.toLong(),
        scanlator = scanlator,
        lastModifiedAt = last_modified,
        version = version,
        memo = memo,
    )
}
