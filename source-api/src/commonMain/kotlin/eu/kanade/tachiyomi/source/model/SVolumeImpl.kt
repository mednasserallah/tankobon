@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.source.model

import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY

class SVolumeImpl : SVolume {

    override lateinit var url: String

    override lateinit var name: String

    override var volume_number: Int = -1

    override var volume_number_end: Int? = null

    override var scanlator: String? = null

    override var date_upload: Long = 0

    override var memo: JsonObject = JsonObject.EMPTY
}
