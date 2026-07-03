package eu.kanade.tachiyomi.ui.reader.model

sealed class VolumeTransition {

    abstract val from: ReaderVolume
    abstract val to: ReaderVolume?

    class Prev(
        override val from: ReaderVolume,
        override val to: ReaderVolume?,
    ) : VolumeTransition()

    class Next(
        override val from: ReaderVolume,
        override val to: ReaderVolume?,
    ) : VolumeTransition()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VolumeTransition) return false
        if (from == other.from && to == other.to) return true
        if (from == other.to && to == other.from) return true
        return false
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + (to?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(from=${from.chapter.url}, to=${to?.chapter?.url})"
    }
}
