package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.core.common.extensions.EMPTY
import mihon.core.common.extensions.JsonObjectEmptyBytes
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.chapter.model.Volume

@Serializable
class BackupVolume(
    // in 1.x some of these values have different names
    // url is called key in 1.x
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var scanlator: String? = null,
    @ProtoNumber(4) var read: Boolean = false,
    @ProtoNumber(5) var bookmark: Boolean = false,
    // lastPageRead is called progress in 1.x
    @ProtoNumber(6) var lastPageRead: Long = 0,
    @ProtoNumber(7) var dateFetch: Long = 0,
    @ProtoNumber(8) var dateUpload: Long = 0,
    // volumeNumber is called number in 1.x. Kept as Float at proto field 9 for backward
    // compatibility with existing Mihon/Tachiyomi backups (volumes are whole numbers, so the
    // value is always integral); it is rounded to a whole volume number on restore.
    @ProtoNumber(9) var volumeNumber: Float = 0F,
    @ProtoNumber(10) var sourceOrder: Long = 0,
    @ProtoNumber(11) var lastModifiedAt: Long = 0,
    @ProtoNumber(12) var version: Long = 0,
    @ProtoNumber(13) var memo: ByteArray = JsonObjectEmptyBytes,
    // New in Tankobon: end of an omnibus volume range (null for single-volume files).
    @ProtoNumber(14) var volumeNumberEnd: Int? = null,
) {
    fun toVolumeImpl(): Volume {
        return Volume.create().copy(
            url = this@BackupVolume.url,
            name = this@BackupVolume.name,
            volumeNumber = this@BackupVolume.volumeNumber.toLong(),
            volumeNumberEnd = this@BackupVolume.volumeNumberEnd?.toLong(),
            scanlator = this@BackupVolume.scanlator,
            read = this@BackupVolume.read,
            bookmark = this@BackupVolume.bookmark,
            lastPageRead = this@BackupVolume.lastPageRead,
            dateFetch = this@BackupVolume.dateFetch,
            dateUpload = this@BackupVolume.dateUpload,
            sourceOrder = this@BackupVolume.sourceOrder,
            lastModifiedAt = this@BackupVolume.lastModifiedAt,
            version = this@BackupVolume.version,
            memo = MemoColumnAdapter.decode(this@BackupVolume.memo),
        )
    }
}

val backupChapterMapper = {
        _: Long,
        _: Long,
        url: String,
        name: String,
        scanlator: String?,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        volumeNumber: Long,
        volumeNumberEnd: Long?,
        sourceOrder: Long,
        dateFetch: Long,
        dateUpload: Long,
        lastModifiedAt: Long,
        version: Long,
        _: Long,
        memo: JsonObject,
    ->
    BackupVolume(
        url = url,
        name = name,
        volumeNumber = volumeNumber.toFloat(),
        volumeNumberEnd = volumeNumberEnd?.toInt(),
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        sourceOrder = sourceOrder,
        lastModifiedAt = lastModifiedAt,
        version = version,
        memo = MemoColumnAdapter.encode(memo),
    )
}
