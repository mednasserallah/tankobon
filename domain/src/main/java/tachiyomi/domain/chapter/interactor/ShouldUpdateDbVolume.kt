package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.model.Volume

class ShouldUpdateDbVolume {

    fun await(dbChapter: Volume, sourceChapter: Volume): Boolean {
        return dbChapter.scanlator != sourceChapter.scanlator ||
            dbChapter.name != sourceChapter.name ||
            dbChapter.dateUpload != sourceChapter.dateUpload ||
            dbChapter.volumeNumber != sourceChapter.volumeNumber ||
            dbChapter.sourceOrder != sourceChapter.sourceOrder ||
            dbChapter.memo != sourceChapter.memo
    }
}
