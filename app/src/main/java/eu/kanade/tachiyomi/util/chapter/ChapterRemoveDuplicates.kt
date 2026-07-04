package eu.kanade.tachiyomi.util.chapter

import tachiyomi.domain.chapter.model.Volume

/**
 * Returns a copy of the list with duplicate chapters removed
 */
fun List<Volume>.removeDuplicates(currentChapter: Volume): List<Volume> {
    return groupBy { it.volumeNumber }
        .map { (_, chapters) ->
            chapters.find { it.id == currentChapter.id }
                ?: chapters.find { it.scanlator == currentChapter.scanlator }
                ?: chapters.first()
        }
}
