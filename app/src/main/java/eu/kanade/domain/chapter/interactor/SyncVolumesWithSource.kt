package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.chapter.model.copyFromSVolume
import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SVolume
import tachiyomi.data.chapter.VolumeSanitizer
import tachiyomi.domain.chapter.interactor.GetVolumesByMangaId
import tachiyomi.domain.chapter.interactor.ShouldUpdateDbVolume
import tachiyomi.domain.chapter.interactor.UpdateVolume
import tachiyomi.domain.chapter.model.NoVolumesException
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.model.toVolumeUpdate
import tachiyomi.domain.chapter.repository.VolumeRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.isLocal
import java.lang.Long.max
import java.time.ZonedDateTime
import java.util.TreeSet

class SyncVolumesWithSource(
    private val chapterRepository: VolumeRepository,
    private val shouldUpdateDbChapter: ShouldUpdateDbVolume,
    private val updateManga: UpdateManga,
    private val updateChapter: UpdateVolume,
    private val getVolumesByMangaId: GetVolumesByMangaId,
    private val getExcludedScanlators: GetExcludedScanlators,
    private val libraryPreferences: LibraryPreferences,
) {

    /**
     * Method to synchronize db chapters with source ones
     *
     * @param rawSourceChapters the chapters from the source.
     * @param manga the manga the chapters belong to.
     * @param source the source the manga belongs to.
     * @return Newly added chapters
     */
    suspend fun await(
        rawSourceChapters: List<SVolume>,
        manga: Manga,
        source: Source,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): List<Volume> {
        if (rawSourceChapters.isEmpty() && !source.isLocal()) {
            throw NoVolumesException()
        }

        val now = ZonedDateTime.now()
        val nowMillis = now.toInstant().toEpochMilli()

        val sourceChapters = rawSourceChapters
            .distinctBy { it.url }
            .mapIndexed { i, sChapter ->
                Volume.create()
                    .copyFromSVolume(sChapter)
                    // Sanitizing strips the series title from the volume name; for one-shots the
                    // volume name equals the title, so fall back to the raw name to avoid a blank.
                    .copy(
                        name = with(VolumeSanitizer) { sChapter.name.sanitize(manga.title) }
                            .ifBlank { sChapter.name },
                    )
                    .copy(mangaId = manga.id, sourceOrder = i.toLong())
            }

        val dbChapters = getVolumesByMangaId.await(manga.id)

        val newChapters = mutableListOf<Volume>()
        val updatedChapters = mutableListOf<Volume>()
        val removedChapters = dbChapters.filterNot { dbChapter ->
            // Shelved volumes are kept even though their file is gone from disk — the user
            // deleted the file to free space but wants the row/metadata/cover preserved.
            dbChapter.isArchived ||
                sourceChapters.any { sourceChapter ->
                    dbChapter.url == sourceChapter.url
                }
        }

        // Used to not set upload date of older chapters
        // to a higher value than newer chapters
        var maxSeenUploadDate = 0L

        for (sourceChapter in sourceChapters) {
            // The volume number is already parsed from the filename by VolumeRecognition in the
            // local source, so no further number recognition is needed here.
            val chapter = sourceChapter

            val dbChapter = dbChapters.find { it.url == chapter.url }

            if (dbChapter == null) {
                val toAddChapter = if (chapter.dateUpload == 0L) {
                    val altDateUpload = if (maxSeenUploadDate == 0L) nowMillis else maxSeenUploadDate
                    chapter.copy(dateUpload = altDateUpload)
                } else {
                    maxSeenUploadDate = max(maxSeenUploadDate, sourceChapter.dateUpload)
                    chapter
                }
                newChapters.add(toAddChapter)
            } else {
                // A file reappeared for a previously shelved volume: un-shelve it so it opens again.
                val unshelve = dbChapter.isArchived
                if (unshelve || shouldUpdateDbChapter.await(dbChapter, chapter)) {
                    var toChangeChapter = dbChapter.copy(
                        name = chapter.name,
                        volumeNumber = chapter.volumeNumber,
                        scanlator = chapter.scanlator,
                        sourceOrder = chapter.sourceOrder,
                        isArchived = false,
                    )

                    if (chapter.dateUpload != 0L) {
                        toChangeChapter = toChangeChapter.copy(dateUpload = chapter.dateUpload)
                    }
                    updatedChapters.add(toChangeChapter)
                }
            }
        }

        // Return if there's nothing to add, delete, or update to avoid unnecessary db transactions.
        if (newChapters.isEmpty() && removedChapters.isEmpty() && updatedChapters.isEmpty()) {
            if (manualFetch || manga.fetchInterval == 0 || manga.nextUpdate < fetchWindow.first) {
                updateManga.awaitUpdateFetchInterval(
                    manga,
                    now,
                    fetchWindow,
                )
            }
            return emptyList()
        }

        val changedOrDuplicateReadUrls = mutableSetOf<String>()

        val deletedChapterNumbers = TreeSet<Long>()
        val deletedReadChapterNumbers = TreeSet<Long>()
        val deletedBookmarkedChapterNumbers = TreeSet<Long>()

        val readChapterNumbers = dbChapters
            .asSequence()
            .filter { it.read && it.isRecognizedNumber }
            .map { it.volumeNumber }
            .toSet()

        removedChapters.forEach { chapter ->
            if (chapter.read) deletedReadChapterNumbers.add(chapter.volumeNumber)
            if (chapter.bookmark) deletedBookmarkedChapterNumbers.add(chapter.volumeNumber)
            deletedChapterNumbers.add(chapter.volumeNumber)
        }

        val deletedChapterNumberDateFetchMap = removedChapters.sortedByDescending { it.dateFetch }
            .associate { it.volumeNumber to it.dateFetch }

        val markDuplicateAsRead = libraryPreferences.markDuplicateReadChapterAsRead.get()
            .contains(LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_NEW)

        // Date fetch is set in such a way that the upper ones will have bigger value than the lower ones
        // Sources MUST return the chapters from most to less recent, which is common.
        var itemCount = newChapters.size
        var updatedToAdd = newChapters.map { toAddItem ->
            var chapter = toAddItem.copy(dateFetch = nowMillis + itemCount--)

            if (chapter.volumeNumber in readChapterNumbers && markDuplicateAsRead) {
                changedOrDuplicateReadUrls.add(chapter.url)
                chapter = chapter.copy(read = true)
            }

            if (!chapter.isRecognizedNumber || chapter.volumeNumber !in deletedChapterNumbers) return@map chapter

            chapter = chapter.copy(
                read = chapter.volumeNumber in deletedReadChapterNumbers,
                bookmark = chapter.volumeNumber in deletedBookmarkedChapterNumbers,
            )

            // Try to to use the fetch date of the original entry to not pollute 'Updates' tab
            deletedChapterNumberDateFetchMap[chapter.volumeNumber]?.let {
                chapter = chapter.copy(dateFetch = it)
            }

            changedOrDuplicateReadUrls.add(chapter.url)

            chapter
        }

        if (removedChapters.isNotEmpty()) {
            val toDeleteIds = removedChapters.map { it.id }
            chapterRepository.removeVolumesWithIds(toDeleteIds)
        }

        if (updatedToAdd.isNotEmpty()) {
            updatedToAdd = chapterRepository.addAll(updatedToAdd)
        }

        if (updatedChapters.isNotEmpty()) {
            val chapterUpdates = updatedChapters.map { it.toVolumeUpdate() }
            updateChapter.awaitAll(chapterUpdates)
        }
        updateManga.awaitUpdateFetchInterval(manga, now, fetchWindow)

        // Set this manga as updated since chapters were changed
        // Note that last_update actually represents last time the chapter list changed at all
        updateManga.awaitUpdateLastUpdate(manga.id)

        val excludedScanlators = getExcludedScanlators.await(manga.id).toHashSet()

        return updatedToAdd.filterNot { it.url in changedOrDuplicateReadUrls || it.scanlator in excludedScanlators }
    }
}
