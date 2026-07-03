package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.toDomainVolume
import eu.kanade.tachiyomi.ui.reader.model.ReaderVolume
import tachiyomi.domain.chapter.service.calculateVolumeGap as domainCalculateChapterGap

fun calculateVolumeGap(higherReaderChapter: ReaderVolume?, lowerReaderChapter: ReaderVolume?): Int {
    return domainCalculateChapterGap(
        higherReaderChapter?.chapter?.toDomainVolume(),
        lowerReaderChapter?.chapter?.toDomainVolume(),
    )
}
