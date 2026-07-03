package tachiyomi.domain.chapter.service

/**
 * Parses Tankobon's opinionated local naming convention into structured volume data.
 *
 * A series lives in a folder; each archive/folder inside it is one **volume** (the atomic
 * reading unit). The expected shape is:
 *
 * ```
 * Series Name/
 *   ├─ Series Name - Volume 01 (Year).cbz
 *   ├─ Series Name - Volume 02 (Year).cbz
 *   ├─ cover.jpg      (sidecar — never a volume)
 *   └─ details.json   (sidecar — never a volume)
 * ```
 *
 * Rules (see CLAUDE.md "Naming-convention parser rules" for the canonical spec):
 * - **Year** is the *last* parenthetical group that is exactly 4 digits, so a trailing
 *   `(2016)` is never confused with an edition tag like `(Master Edition)`. Parsed per file.
 * - **Edition** (a folder-level property) is any parenthetical group in the *series folder name*
 *   that is not a 4-digit year, captured verbatim (`Gantz (Omnibus Edition)` → `Omnibus Edition`).
 * - **Volume number** comes from `Volume NN` (optionally a `NN-MM` omnibus range). Whole numbers
 *   only — volumes are never fractional. The range end is captured separately; sorting keys off
 *   the start.
 * - **Single-volume / one-shot** titles with no `Volume NN` segment are treated as volume `1`.
 * - Unrecognized names fall back to volume `1` with the whole basename as the display name.
 */
object VolumeRecognition {

    private val YEAR = Regex("""^\d{4}$""")

    /** Matches every `(...)` group, capturing its inner content. */
    private val PARENS = Regex("""\(([^)]*)\)""")

    /**
     * `Volume 01`, `Vol. 3`, `v04`, `Volume 01-02` (omnibus range). Case-insensitive.
     * Group 1 = start number, group 2 = optional range end.
     */
    private val VOLUME = Regex("""(?:volume|vol|v)\.?\s*(\d+)\s*(?:-\s*(\d+))?""", RegexOption.IGNORE_CASE)

    data class SeriesInfo(
        val title: String,
        val edition: String?,
    )

    data class VolumeInfo(
        /** Whole-number start of the volume. `1` for one-shots / unrecognized names. */
        val number: Int,
        /** End of an omnibus range (`Volume 01-02` → 2); null for single-volume files. */
        val numberEnd: Int?,
        /** 4-digit publication year if present in the filename, else null. */
        val year: Int?,
        /** Human-facing display name (`Volume 01`, `Volume 01-02`, or a one-shot title). */
        val name: String,
    )

    /**
     * Parses a **series folder name** into its base title and optional edition tag.
     *
     * `BLAME! (Master Edition)` → title `BLAME!`, edition `Master Edition`.
     * `Boy Meets Maria` → title `Boy Meets Maria`, edition null.
     */
    fun parseSeries(folderName: String): SeriesInfo {
        val name = folderName.trim()
        // The edition is the last parenthetical group that is not a 4-digit year.
        val editionMatch = PARENS.findAll(name)
            .lastOrNull { !YEAR.matches(it.groupValues[1].trim()) }

        if (editionMatch == null) {
            return SeriesInfo(title = name, edition = null)
        }

        val edition = editionMatch.groupValues[1].trim().ifBlank { null }
        val title = name.removeRange(editionMatch.range).cleanupTitle()
        return SeriesInfo(title = title.ifBlank { name }, edition = edition)
    }

    /**
     * Parses a **volume file name** (basename, extension already stripped) into structured data.
     *
     * `BLAME! (Master Edition) - Volume 01 (2016)` → number 1, end null, year 2016, name `Volume 01`.
     * `Homunculus (Omnibus Edition) - Volume 01-02 (2023)` → number 1, end 2, name `Volume 01-02`.
     * `Boy Meets Maria (2021)` → number 1, end null, year 2021, name `Boy Meets Maria`.
     */
    fun parseVolume(fileName: String): VolumeInfo {
        val name = fileName.trim()

        val year = PARENS.findAll(name)
            .map { it.groupValues[1].trim() }
            .lastOrNull { YEAR.matches(it) }
            ?.toIntOrNull()

        val volumeMatch = VOLUME.find(name)
        if (volumeMatch != null) {
            val number = volumeMatch.groupValues[1].toIntOrNull() ?: 1
            val numberEnd = volumeMatch.groupValues[2].toIntOrNull()?.takeIf { it > number }
            val display = if (numberEnd != null) {
                "Volume %02d-%02d".format(number, numberEnd)
            } else {
                "Volume %02d".format(number)
            }
            return VolumeInfo(number = number, numberEnd = numberEnd, year = year, name = display)
        }

        // One-shot / single-volume title with no "Volume NN" segment: strip the trailing year
        // group and use the remaining title text as the display name; it is volume 1.
        val display = stripTrailingYear(name).cleanupTitle().ifBlank { name }
        return VolumeInfo(number = 1, numberEnd = null, year = year, name = display)
    }

    /** Removes only a trailing `(YYYY)` group, leaving inner parentheticals (e.g. editions) intact. */
    private fun stripTrailingYear(name: String): String {
        val trailing = Regex("""\s*\(\d{4}\)\s*$""")
        return name.replace(trailing, "")
    }

    /** Trims leftover separators/whitespace after removing a parenthetical group from a title. */
    private fun String.cleanupTitle(): String {
        return trim()
            .removeSuffix("-")
            .trim()
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }
}
