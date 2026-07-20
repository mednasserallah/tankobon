package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.SVolume
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import mihon.core.archive.archiveReader
import mihon.core.archive.epubReader
import nl.adaptivity.xmlutil.core.AndroidXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.VolumeCoverSelector
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.copyFromComicInfo
import tachiyomi.core.metadata.comicinfo.getComicInfo
import tachiyomi.core.metadata.tachiyomi.MangaDetails
import tachiyomi.domain.chapter.service.VolumeRecognition
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.Archive
import tachiyomi.source.local.io.Format
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.metadata.fillMetadata
import uy.kohesive.injekt.injectLazy
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.days
import tachiyomi.domain.source.model.Source as DomainSource

actual class LocalSource(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
    private val coverManager: LocalCoverManager,
) : Source, UnmeteredSource {

    private val json: Json by injectLazy()
    private val xml: XML by injectLazy()

    @Suppress("PrivatePropertyName")
    private val PopularFilters = FilterList(OrderBy.Popular(context))

    @Suppress("PrivatePropertyName")
    private val LatestFilters = FilterList(OrderBy.Latest(context))

    override val name: String = context.stringResource(MR.strings.local_source)

    override val id: Long = ID

    override val lang: String = "other"

    override fun toString() = name

    override val supportsLatest: Boolean = true

    // Browse related
    override suspend fun getPopularManga(page: Int) = getSearchManga(page, "", PopularFilters)

    override suspend fun getLatestUpdates(page: Int) = getSearchManga(page, "", LatestFilters)

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = withIOContext {
        val lastModifiedLimit = if (filters === LatestFilters) {
            System.currentTimeMillis() - LATEST_THRESHOLD
        } else {
            0L
        }

        var mangaDirs = fileSystem.getFilesInBaseDirectory()
            // Filter out files that are hidden and is not a folder
            .filter { it.isDirectory && !it.name.orEmpty().startsWith('.') }
            .distinctBy { it.name }
            .filter {
                if (lastModifiedLimit == 0L && query.isBlank()) {
                    true
                } else if (lastModifiedLimit == 0L) {
                    it.name.orEmpty().contains(query, ignoreCase = true)
                } else {
                    it.lastModified() >= lastModifiedLimit
                }
            }

        filters.forEach { filter ->
            when (filter) {
                is OrderBy.Popular -> {
                    mangaDirs = if (filter.state!!.ascending) {
                        mangaDirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    } else {
                        mangaDirs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    }
                }
                is OrderBy.Latest -> {
                    mangaDirs = if (filter.state!!.ascending) {
                        mangaDirs.sortedBy(UniFile::lastModified)
                    } else {
                        mangaDirs.sortedByDescending(UniFile::lastModified)
                    }
                }
                else -> {
                    /* Do nothing */
                }
            }
        }

        val mangas = mangaDirs
            .map { mangaDir ->
                async {
                    val dirName = mangaDir.name.orEmpty()
                    val seriesInfo = VolumeRecognition.parseSeries(dirName)
                    SManga.create().apply {
                        // The folder name is the stable key/url; the displayed title has the
                        // edition tag stripped and the edition captured as its own field.
                        title = seriesInfo.title
                        edition = seriesInfo.edition
                        url = dirName

                        // Try to find the cover
                        coverManager.find(dirName)?.let {
                            thumbnail_url = it.uri.toString()
                        }
                    }
                }
            }
            .awaitAll()

        MangasPage(mangas, false)
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        volumes: List<SVolume>,
        fetchDetails: Boolean,
        fetchVolumes: Boolean,
    ): SMangaUpdate = supervisorScope {
        val asyncManga = if (fetchDetails) async { getMangaDetails(manga) } else null
        val asyncVolumes = if (fetchVolumes) async { getVolumeList(manga) } else null
        SMangaUpdate(asyncManga?.await() ?: manga, asyncVolumes?.await() ?: volumes)
    }

    // Manga details related
    private suspend fun getMangaDetails(manga: SManga): SManga = withIOContext {
        // The edition tag is a folder-level property; always re-derive it from the folder name
        // so rescans of existing library entries pick it up.
        manga.edition = VolumeRecognition.parseSeries(manga.url).edition

        coverManager.find(manga.url)?.let {
            manga.thumbnail_url = it.uri.toString()
        }

        // Augment manga details based on metadata files
        try {
            val mangaDir = fileSystem.getMangaDirectory(manga.url) ?: error("${manga.url} is not a valid directory")
            val mangaDirFiles = mangaDir.listFiles().orEmpty()

            val comicInfoFile = mangaDirFiles
                .firstOrNull { it.name == COMIC_INFO_FILE }
            val noXmlFile = mangaDirFiles
                .firstOrNull { it.name == ".noxml" }
            val legacyJsonDetailsFile = mangaDirFiles
                .firstOrNull { it.extension == "json" }

            when {
                // Top level ComicInfo.xml
                comicInfoFile != null -> {
                    noXmlFile?.delete()
                    setMangaDetailsFromComicInfoFile(comicInfoFile.openInputStream(), manga)
                }

                // Old custom JSON format
                // TODO: remove support for this entirely after a while
                legacyJsonDetailsFile != null -> {
                    json.decodeFromStream<MangaDetails>(legacyJsonDetailsFile.openInputStream()).run {
                        title?.let { manga.title = it }
                        author?.let { manga.author = it }
                        artist?.let { manga.artist = it }
                        description?.let { manga.description = it }
                        genre?.let { manga.genre = it.joinToString() }
                        status?.let { manga.status = it }
                    }
                    // Replace with ComicInfo.xml file
                    val comicInfo = manga.getComicInfo()
                    mangaDir
                        .createFile(COMIC_INFO_FILE)
                        ?.openOutputStream()
                        ?.use {
                            val comicInfoString = xml.encodeToString(ComicInfo.serializer(), comicInfo)
                            it.write(comicInfoString.toByteArray())
                            legacyJsonDetailsFile.delete()
                        }
                }

                // Copy ComicInfo.xml from chapter archive to top level if found
                noXmlFile == null -> {
                    val chapterArchives = mangaDirFiles.filter(Archive::isSupported)

                    val copiedFile = copyComicInfoFileFromChapters(chapterArchives, mangaDir)
                    if (copiedFile != null) {
                        setMangaDetailsFromComicInfoFile(copiedFile.openInputStream(), manga)
                    } else {
                        // Avoid re-scanning
                        mangaDir.createFile(".noxml")
                    }
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Error setting manga details from local metadata for ${manga.title}" }
        }

        return@withIOContext manga
    }

    private fun <T> getComicInfoForChapter(chapter: UniFile, block: (InputStream) -> T): T? {
        return if (chapter.isDirectory) {
            chapter.findFile(COMIC_INFO_FILE)?.openInputStream()?.use(block)
        } else {
            chapter.archiveReader(context).use { reader ->
                reader.getInputStream(COMIC_INFO_FILE)?.use(block)
            }
        }
    }

    private fun copyComicInfoFileFromChapters(chapterArchives: List<UniFile>, folder: UniFile): UniFile? {
        for (chapter in chapterArchives) {
            val file = getComicInfoForChapter(chapter) f@{ stream ->
                return@f copyComicInfoFile(stream, folder)
            }
            if (file != null) return file
        }
        return null
    }

    private fun copyComicInfoFile(comicInfoFileStream: InputStream, folder: UniFile): UniFile? {
        return folder.createFile(COMIC_INFO_FILE)?.apply {
            openOutputStream().use { outputStream ->
                comicInfoFileStream.use { it.copyTo(outputStream) }
            }
        }
    }

    private fun parseComicInfo(stream: InputStream): ComicInfo {
        return AndroidXmlReader(stream, StandardCharsets.UTF_8.name()).use {
            xml.decodeFromReader<ComicInfo>(it)
        }
    }

    private fun setMangaDetailsFromComicInfoFile(stream: InputStream, manga: SManga) {
        manga.copyFromComicInfo(parseComicInfo(stream))
    }

    private fun setVolumeDetailsFromComicInfoFile(stream: InputStream, volume: SVolume) {
        val comicInfo = parseComicInfo(stream)

        // The volume number/name come from the filename convention; only augment the scanlator
        // (translator) from ComicInfo so metadata cannot mislabel "Volume 01" as a chapter title.
        comicInfo.translator?.let { volume.scanlator = it.value }
    }

    // Volumes
    private suspend fun getVolumeList(manga: SManga): List<SVolume> = withIOContext {
        val volumes = fileSystem.getFilesInMangaDirectory(manga.url)
            // Skip hidden files and any non-volume file. Sidecars (cover.jpg, details.json,
            // ComicInfo.xml, .noxml, …) are not archives/epubs/directories, so they drop out here.
            .filterNot { it.name.orEmpty().startsWith('.') }
            .filter { it.isDirectory || Archive.isSupported(it) || it.extension.equals("epub", true) }
            .map { volumeFile ->
                val baseName = if (volumeFile.isDirectory) {
                    volumeFile.name
                } else {
                    volumeFile.nameWithoutExtension
                }.orEmpty()
                val parsed = VolumeRecognition.parseVolume(baseName)

                SVolume.create().apply {
                    url = "${manga.url}/${volumeFile.name}"
                    name = parsed.name
                    volume_number = parsed.number
                    volume_number_end = parsed.numberEnd
                    date_upload = volumeFile.lastModified()

                    val format = Format.valueOf(volumeFile)
                    if (format is Format.Epub) {
                        format.file.epubReader(context).use { epub ->
                            epub.fillMetadata(manga, this)
                        }
                    } else {
                        getComicInfoForChapter(volumeFile) { stream ->
                            setVolumeDetailsFromComicInfoFile(stream, this)
                        }
                    }
                }
            }
            // Sort numerically by volume number (1, 2, … 9, 10), not lexically; tie-break by name.
            .sortedWith(
                compareByDescending<SVolume> { it.volume_number }
                    .thenByDescending { it.name.lowercase() },
            )

        // Copy the cover from the first volume (lowest number) if not available.
        if (manga.thumbnail_url.isNullOrBlank()) {
            volumes.lastOrNull()?.let { volume ->
                updateCover(volume, manga)
            }
        }

        volumes
    }

    // Filters
    override fun getFilterList() = FilterList(OrderBy.Popular(context))

    // Unused stuff
    override suspend fun getPageList(volume: SVolume): List<Page> = throw UnsupportedOperationException("Unused")

    fun getFormat(chapter: SVolume): Format {
        try {
            val (mangaDirName, volumeName) = chapter.url.split('/', limit = 2)
            return fileSystem.getBaseDirectory()
                ?.findFile(mangaDirName)
                ?.findFile(volumeName)
                ?.let(Format.Companion::valueOf)
                ?: throw Exception(context.stringResource(MR.strings.chapter_not_found))
        } catch (e: Format.UnknownFormatException) {
            throw Exception(context.stringResource(MR.strings.local_invalid_format))
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Reads the first (natural-sorted) image inside a single volume as raw bytes, for use as that
     * volume's cover thumbnail. Only the chosen entry is decoded — entry names are listed first
     * (cheap) and just that one entry's bytes are read. The whole image is read into memory (a
     * single page) so the result is independent of the archive handle's lifetime.
     *
     * @param volumeUrl the volume's url (`mangaDirName/volumeName`), i.e. [SVolume.url].
     * @return the cover image bytes, or null if the volume has no images or can't be read.
     */
    fun getVolumeCoverBytes(volumeUrl: String): ByteArray? {
        return try {
            when (val format = getFormat(SVolume.create().apply { url = volumeUrl })) {
                is Format.Directory -> {
                    val files = format.file.listFiles()?.filterNot { it.isDirectory }.orEmpty()
                    VolumeCoverSelector.selectCover(files.mapNotNull { it.name })
                        ?.let { name -> files.firstOrNull { it.name == name } }
                        ?.openInputStream()
                        ?.use { it.readBytes() }
                }
                is Format.Archive -> {
                    format.file.archiveReader(context).use { reader ->
                        reader.useEntries { entries ->
                            val names = entries.filter { it.isFile }.map { it.name }.toList()
                            VolumeCoverSelector.selectCover(names)
                                ?.let { name -> reader.getInputStream(name)?.use { it.readBytes() } }
                        }
                    }
                }
                is Format.Epub -> {
                    format.file.epubReader(context).use { epub ->
                        epub.getImagesFromPages().firstOrNull()
                            ?.let { entry -> epub.getInputStream(entry)?.use { it.readBytes() } }
                    }
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Error extracting volume cover for $volumeUrl" }
            null
        }
    }

    /**
     * Deletes a single volume's file (archive) or folder from disk, freeing space. Used by the
     * "shelve" feature: the volume's DB row and cached cover are kept, only the on-disk file goes.
     * Folder-based volumes are removed recursively.
     *
     * @param volumeUrl the volume's url (`mangaDirName/volumeName`), i.e. [SVolume.url].
     * @return true if the file was deleted (or was already gone), false if deletion failed.
     */
    fun deleteVolume(volumeUrl: String): Boolean {
        return try {
            val (mangaDirName, volumeName) = volumeUrl.split('/', limit = 2)
            val file = fileSystem.getBaseDirectory()
                ?.findFile(mangaDirName)
                ?.findFile(volumeName)
                ?: return true // already gone
            deleteRecursively(file)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Error deleting volume $volumeUrl" }
            false
        }
    }

    private fun deleteRecursively(file: UniFile): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        return file.delete()
    }

    private fun updateCover(chapter: SVolume, manga: SManga): UniFile? {
        return try {
            when (val format = getFormat(chapter)) {
                is Format.Directory -> {
                    val entry = format.file.listFiles()
                        ?.sortedWith { f1, f2 ->
                            f1.name.orEmpty().compareToCaseInsensitiveNaturalOrder(
                                f2.name.orEmpty(),
                            )
                        }
                        ?.find {
                            !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() }
                        }

                    entry?.let { coverManager.update(manga, it.openInputStream()) }
                }
                is Format.Archive -> {
                    format.file.archiveReader(context).use { reader ->
                        val entry = reader.useEntries { entries ->
                            entries
                                .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                                .find { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
                        }

                        entry?.let { coverManager.update(manga, reader.getInputStream(it.name)!!) }
                    }
                }
                is Format.Epub -> {
                    format.file.epubReader(context).use { epub ->
                        val entry = epub.getImagesFromPages().firstOrNull()

                        entry?.let { coverManager.update(manga, epub.getInputStream(it)!!) }
                    }
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Error updating cover for ${manga.title}" }
            null
        }
    }

    companion object {
        const val ID = 0L
        const val HELP_URL = "https://mihon.app/docs/guides/local-source/"

        private val LATEST_THRESHOLD = 7.days.inWholeMilliseconds
    }
}

fun Manga.isLocal(): Boolean = source == LocalSource.ID

fun Source.isLocal(): Boolean = id == LocalSource.ID

fun DomainSource.isLocal(): Boolean = id == LocalSource.ID
