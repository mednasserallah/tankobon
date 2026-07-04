package eu.kanade.presentation.manga.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.formatVolumeNumber
import tachiyomi.domain.chapter.model.asVolumeCover
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import eu.kanade.tachiyomi.ui.manga.VolumeList as VolumeListItemHost

/**
 * A single cell in the per-manga volume cover grid: the volume's own cover thumbnail (extracted
 * from inside its archive) with the volume label beneath. Mirrors the list item's click/selection
 * behaviour so both views feel the same.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaVolumeCoverGridItem(
    manga: Manga,
    item: VolumeListItemHost.Item,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val volume = item.chapter
    val label = if (manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER) {
        stringResource(MR.strings.display_mode_chapter, formatVolumeNumber(volume))
    } else {
        volume.name
    }
    // Show how far into a volume the user is, so partially-read volumes are obvious in the grid.
    val readProgress = volume.lastPageRead
        .takeIf { !volume.read && it > 0L }
        ?.let { stringResource(MR.strings.chapter_progress, it + 1) }

    val shape = MaterialTheme.shapes.small

    Column(
        modifier = modifier
            .padding(4.dp)
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(
                if (item.selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MangaCover.Book.ratio),
        ) {
            MangaCover.Book(
                data = volume.asVolumeCover(),
                modifier = Modifier
                    .fillMaxWidth()
                    // Dim the cover for read volumes so progress is legible at a glance.
                    .alpha(if (volume.read) READ_ALPHA else 1f),
                contentDescription = label,
                shape = shape,
            )

            if (volume.bookmark) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = stringResource(MR.strings.action_filter_bookmarked),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(18.dp),
                )
            }

            if (item.selected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(MangaCover.Book.ratio)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .alpha(if (volume.read) READ_ALPHA else 1f),
            style = MaterialTheme.typography.bodySmall,
            color = if (volume.read) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = READ_ALPHA)
            } else {
                Color.Unspecified
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (readProgress != null) {
            Text(
                text = readProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val READ_ALPHA = 0.4f
