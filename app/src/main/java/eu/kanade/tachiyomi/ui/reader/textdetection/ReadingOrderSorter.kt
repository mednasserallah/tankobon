package eu.kanade.tachiyomi.ui.reader.textdetection

/**
 * Orders detected text lines into manga reading order.
 *
 * Manga pages are laid out as horizontal bands ("rows"), each band holding one or more panels /
 * bubbles side by side. Reading order is therefore: bands top-to-bottom, and within a band the
 * panels run **right-to-left** for RTL series (the common manga case) or left-to-right otherwise.
 * Lines stacked vertically inside a single bubble land in separate bands and so read top-to-bottom.
 * For a panel-per-band grid (e.g. a 2×2 layout of single-bubble panels) this yields the correct
 * manga order (top-right, top-left, bottom-right, bottom-left in RTL).
 *
 * This is heuristic and best-effort. It has no notion of speech-bubble boundaries — ML Kit yields
 * lines, not bubbles — so two side-by-side *multi-line* bubbles at the same height will interleave
 * line-by-line rather than being read one bubble fully then the next. That is an accepted
 * limitation for a convenience feature and is documented in CLAUDE.md.
 *
 * Pure Kotlin (operates on [TextBoundingBox], not `android.graphics.Rect`) so it is unit-testable
 * on the JVM.
 */
object ReadingOrderSorter {

    /**
     * Fraction of the shorter box's height that two lines must vertically share to be placed in
     * the same band. 0.5 means "overlap by at least half the shorter line" — loose enough to keep
     * a slightly uneven side-by-side pair together, strict enough that a staggered/diagonal
     * descent is not chained into one giant band.
     */
    private const val DEFAULT_ROW_OVERLAP_THRESHOLD = 0.5f

    fun sort(
        lines: List<DetectedTextLine>,
        rtl: Boolean,
        rowOverlapThreshold: Float = DEFAULT_ROW_OVERLAP_THRESHOLD,
    ): List<DetectedTextLine> {
        if (lines.size <= 1) return lines

        // Cluster into horizontal bands. Process top-to-bottom and compare each line against the
        // band's anchor (its topmost member) rather than the band's growing union, so a diagonal
        // descent does not chain every line into a single band. A line joins the band it overlaps
        // most (ties broken toward the higher band).
        val bands = mutableListOf<MutableList<DetectedTextLine>>()
        for (line in lines.sortedBy { it.box.top }) {
            val target = bands
                .map { band -> band to verticalOverlapRatio(line.box, band.first().box) }
                .filter { it.second >= rowOverlapThreshold }
                .maxByOrNull { it.second }
                ?.first
            if (target != null) target.add(line) else bands.add(mutableListOf(line))
        }

        // Bands top-to-bottom; within a band, ordered by reading direction.
        return bands
            .sortedBy { band -> band.minOf { it.box.top } }
            .flatMap { band ->
                val leftToRight = band.sortedBy { it.box.centerX }
                if (rtl) leftToRight.asReversed() else leftToRight
            }
    }

    /** Vertical overlap of two boxes as a fraction of the shorter box's height (0f..1f). */
    private fun verticalOverlapRatio(a: TextBoundingBox, b: TextBoundingBox): Float {
        val overlap = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
        if (overlap <= 0) return 0f
        val shorter = minOf(a.height, b.height)
        if (shorter <= 0) return 0f
        return overlap.toFloat() / shorter.toFloat()
    }
}
