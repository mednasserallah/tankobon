package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.data.database.models.toDomainVolume
import eu.kanade.tachiyomi.ui.reader.model.ReaderVolume
import eu.kanade.tachiyomi.ui.reader.model.VolumeTransition
import tachiyomi.domain.chapter.model.Volume
import tachiyomi.domain.chapter.service.calculateVolumeGap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun VolumeTransition(
    transition: VolumeTransition,
) {
    val currChapter = transition.from.chapter.toDomainVolume()
    val goingToChapter = transition.to?.chapter?.toDomainVolume()

    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        when (transition) {
            is VolumeTransition.Prev -> {
                TransitionText(
                    topLabel = stringResource(MR.strings.transition_previous),
                    topChapter = goingToChapter,
                    bottomLabel = stringResource(MR.strings.transition_current),
                    bottomChapter = currChapter,
                    fallbackLabel = stringResource(MR.strings.transition_no_previous),
                    chapterGap = calculateVolumeGap(currChapter, goingToChapter),
                )
            }
            is VolumeTransition.Next -> {
                TransitionText(
                    topLabel = stringResource(MR.strings.transition_finished),
                    topChapter = currChapter,
                    bottomLabel = stringResource(MR.strings.transition_next),
                    bottomChapter = goingToChapter,
                    fallbackLabel = stringResource(MR.strings.transition_no_next),
                    chapterGap = calculateVolumeGap(goingToChapter, currChapter),
                )
            }
        }
    }
}

@Composable
private fun TransitionText(
    topLabel: String,
    topChapter: Volume?,
    bottomLabel: String,
    bottomChapter: Volume?,
    fallbackLabel: String,
    chapterGap: Int,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth(),
    ) {
        if (topChapter != null) {
            VolumeText(
                header = topLabel,
                name = topChapter.name,
                scanlator = topChapter.scanlator,
            )

            Spacer(Modifier.height(VerticalSpacerSize))
        } else {
            NoVolumeNotification(
                text = fallbackLabel,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        if (bottomChapter != null) {
            if (chapterGap > 0) {
                VolumeGapWarning(
                    gapCount = chapterGap,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            Spacer(Modifier.height(VerticalSpacerSize))

            VolumeText(
                header = bottomLabel,
                name = bottomChapter.name,
                scanlator = bottomChapter.scanlator,
            )
        } else {
            NoVolumeNotification(
                text = fallbackLabel,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun NoVolumeNotification(
    text: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardColor,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun VolumeGapWarning(
    gapCount: Int,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = null,
            )

            Text(
                text = pluralStringResource(MR.plurals.missing_chapters_warning, count = gapCount, gapCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun VolumeHeaderText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun VolumeText(
    header: String,
    name: String,
    scanlator: String?,
) {
    Column {
        VolumeHeaderText(
            text = header,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Text(
            text = name,
            fontSize = 20.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
        )

        scanlator?.let {
            Text(
                text = it,
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private val CardColor: CardColors
    @Composable
    get() = CardDefaults.outlinedCardColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

private val VerticalSpacerSize = 24.dp

private fun previewChapter(name: String, scanlator: String, volumeNumber: Long) = Volume.create().copy(
    id = 0L,
    mangaId = 0L,
    url = "",
    name = name,
    scanlator = scanlator,
    volumeNumber = volumeNumber,
)
private val FakeVolume = previewChapter(
    name = "Vol.1, Ch.1 - Fake Volume Title",
    scanlator = "Scanlator Name",
    volumeNumber = 1L,
)
private val FakeGapVolume = previewChapter(
    name = "Vol.5, Ch.44 - Fake Gap Volume Title",
    scanlator = "Scanlator Name",
    volumeNumber = 44L,
)
private val FakeVolumeLongTitle = previewChapter(
    name = "Vol.1, Ch.0 - The Mundane Musings of a Metafictional Manga: A Volume About a Volume, Featuring" +
        " an Absurdly Long Title and a Surprisingly Normal Day in the Lives of Our Heroes, as They Grapple with the " +
        "Daily Challenges of Existence, from Paying Rent to Finding Love, All While Navigating the Strange World of " +
        "Fictional Realities and Reality-Bending Fiction, Where the Fourth Wall is Always in Danger of Being Broken " +
        "and the Line Between Author and Character is Forever Blurred.",
    scanlator = "Long Long Funny Scanlator Sniper Group Name Reborn",
    volumeNumber = 1L,
)

@PreviewLightDark
@Composable
private fun TransitionTextPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            VolumeTransition(
                transition = VolumeTransition.Next(ReaderVolume(FakeVolume), ReaderVolume(FakeVolume)),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransitionTextLongTitlePreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            VolumeTransition(
                transition = VolumeTransition.Next(ReaderVolume(FakeVolumeLongTitle), ReaderVolume(FakeVolume)),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransitionTextWithGapPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            VolumeTransition(
                transition = VolumeTransition.Next(ReaderVolume(FakeVolume), ReaderVolume(FakeGapVolume)),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransitionTextNoNextPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            VolumeTransition(
                transition = VolumeTransition.Next(ReaderVolume(FakeVolume), null),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransitionTextNoPreviousPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            VolumeTransition(
                transition = VolumeTransition.Prev(ReaderVolume(FakeVolume), null),
            )
        }
    }
}
