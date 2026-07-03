@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.source.model

import kotlinx.serialization.json.JsonObject
import java.io.Serializable

interface SVolume : Serializable {

    var url: String

    var name: String

    /** Whole-number start of the volume. */
    var volume_number: Int

    /** End of an omnibus range (`Volume 01-02` → 2); null for single-volume files. */
    var volume_number_end: Int?

    var scanlator: String?

    var date_upload: Long

    /**
     * Extra metadata associated with the volume.
     *
     * The JSON object is not visible to users and intended for internal or source-specific
     * purposes. Apps may define their own namespaced keys (e.g., `"mihon.*"`) for sources to populate.
     *
     * This allows apps to attach and ask for custom information without affecting the visible
     * volume data.
     *
     * @since tachiyomix 1.6
     */
    var memo: JsonObject

    fun copyFrom(other: SVolume) {
        name = other.name
        url = other.url
        date_upload = other.date_upload
        volume_number = other.volume_number
        volume_number_end = other.volume_number_end
        scanlator = other.scanlator
        memo = other.memo
    }

    companion object {
        fun create(): SVolume {
            return SVolumeImpl()
        }
    }
}
