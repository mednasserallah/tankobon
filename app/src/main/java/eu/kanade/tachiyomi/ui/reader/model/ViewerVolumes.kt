package eu.kanade.tachiyomi.ui.reader.model

data class ViewerVolumes(
    val currChapter: ReaderVolume,
    val prevChapter: ReaderVolume?,
    val nextChapter: ReaderVolume?,
) {

    fun ref() {
        currChapter.ref()
        prevChapter?.ref()
        nextChapter?.ref()
    }

    fun unref() {
        currChapter.unref()
        prevChapter?.unref()
        nextChapter?.unref()
    }
}
