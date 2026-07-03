package eu.kanade.tachiyomi.ui.reader.textdetection

/**
 * Merges ML Kit `Line` results that belong to the same speech bubble / paragraph before
 * reading-order clustering runs.
 *
 * ML Kit splits wrapped text inside one bubble into separate `Line`s ("WHO IS" / "THAT?!"). We
 * avoid `Block` granularity because it can merge genuinely separate bubbles. Instead we do a
 * conservative vertical merge: two lines join only when the vertical gap between them is small
 * relative to their height (wrapped-line leading, not bubble-to-bubble spacing) **and** their
 * horizontal ranges substantially overlap (same column of text, not two side-by-side bubbles).
 *
 * Bias is deliberately toward under-merging: leaving a bubble split across two entries is a minor
 * annoyance, whereas blending two unrelated bubbles produces nonsense (and mistranslates).
 *
 * Pure Kotlin (operates on [TextBoundingBox]) so it is unit-testable on the JVM.
 */
object TextLineMerger {

    /**
     * Max vertical gap between two stacked lines, as a fraction of their average height, to still
     * treat them as wrapped text in one bubble. Wrapped-line leading is typically well under one
     * line height; bubble-to-bubble gaps are larger. 0.6 keeps this conservative.
     */
    private const val DEFAULT_VERTICAL_GAP_RATIO = 0.6f

    /**
     * Min horizontal overlap between two lines, as a fraction of the narrower line's width, to be
     * considered the same column of text. Wrapped lines in a bubble are near-aligned (high overlap);
     * two side-by-side bubbles barely overlap.
     */
    private const val DEFAULT_HORIZONTAL_OVERLAP_RATIO = 0.35f

    fun merge(
        lines: List<DetectedTextLine>,
        verticalGapRatio: Float = DEFAULT_VERTICAL_GAP_RATIO,
        horizontalOverlapRatio: Float = DEFAULT_HORIZONTAL_OVERLAP_RATIO,
    ): List<DetectedTextLine> {
        if (lines.size <= 1) return lines

        val clusters = mutableListOf<MutableList<DetectedTextLine>>()
        for (line in lines.sortedBy { it.box.top }) {
            // Append to the cluster whose current bottom-most line this line continues.
            val cluster = clusters.firstOrNull { cluster ->
                val last = cluster.last()
                verticallyContinues(last.box, line.box, verticalGapRatio) &&
                    horizontallyOverlaps(last.box, line.box, horizontalOverlapRatio)
            }
            if (cluster != null) cluster.add(line) else clusters.add(mutableListOf(line))
        }

        return clusters.map { mergeCluster(it) }
    }

    /** True if [below] directly continues [above] as the next wrapped line (small vertical gap). */
    private fun verticallyContinues(above: TextBoundingBox, below: TextBoundingBox, gapRatio: Float): Boolean {
        val averageHeight = (above.height + below.height) / 2f
        if (averageHeight <= 0f) return false
        val gap = below.top - above.bottom
        // Allow a slightly overlapping gap (tight leading) up to gapRatio * height; reject a large gap.
        return gap <= gapRatio * averageHeight && gap >= -averageHeight
    }

    private fun horizontallyOverlaps(a: TextBoundingBox, b: TextBoundingBox, overlapRatio: Float): Boolean {
        val overlap = minOf(a.right, b.right) - maxOf(a.left, b.left)
        if (overlap <= 0) return false
        val narrowerWidth = minOf(a.right - a.left, b.right - b.left)
        if (narrowerWidth <= 0) return false
        return overlap.toFloat() / narrowerWidth.toFloat() >= overlapRatio
    }

    private fun mergeCluster(cluster: List<DetectedTextLine>): DetectedTextLine {
        if (cluster.size == 1) return cluster.first()
        val ordered = cluster.sortedBy { it.box.top }
        val text = ordered.joinToString(" ") { it.text }
        val box = TextBoundingBox(
            left = ordered.minOf { it.box.left },
            top = ordered.minOf { it.box.top },
            right = ordered.maxOf { it.box.right },
            bottom = ordered.maxOf { it.box.bottom },
        )
        return DetectedTextLine(text = text, box = box)
    }
}
