package eu.kanade.domain.chapter.model

import eu.kanade.tachiyomi.data.database.models.VolumeImpl
import eu.kanade.tachiyomi.source.model.SVolume
import tachiyomi.domain.chapter.model.Volume
import eu.kanade.tachiyomi.data.database.models.Volume as DbVolume

// TODO: Remove when all deps are migrated
fun Volume.toSVolume(): SVolume {
    return SVolume.create().also {
        it.url = url
        it.name = name
        it.date_upload = dateUpload
        it.volume_number = volumeNumber.toInt()
        it.volume_number_end = volumeNumberEnd?.toInt()
        it.scanlator = scanlator
        it.memo = memo
    }
}

fun Volume.copyFromSVolume(sVolume: SVolume): Volume {
    return this.copy(
        name = sVolume.name,
        url = sVolume.url,
        dateUpload = sVolume.date_upload,
        volumeNumber = sVolume.volume_number.toLong(),
        volumeNumberEnd = sVolume.volume_number_end?.toLong(),
        scanlator = sVolume.scanlator?.ifBlank { null }?.trim(),
        memo = sVolume.memo,
    )
}

fun Volume.toDbVolume(): DbVolume = VolumeImpl().also {
    it.id = id
    it.manga_id = mangaId
    it.url = url
    it.name = name
    it.scanlator = scanlator
    it.read = read
    it.bookmark = bookmark
    it.last_page_read = lastPageRead.toInt()
    it.date_fetch = dateFetch
    it.date_upload = dateUpload
    it.volume_number = volumeNumber.toInt()
    it.volume_number_end = volumeNumberEnd?.toInt()
    it.source_order = sourceOrder.toInt()
    it.memo = memo
}
