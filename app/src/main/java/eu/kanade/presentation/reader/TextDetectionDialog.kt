package eu.kanade.presentation.reader

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.reader.textdetection.DetectedTextLine
import eu.kanade.tachiyomi.ui.reader.textdetection.TextDetectionState
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Bottom sheet listing the English text detected on the current page, in manga reading order,
 * with a per-line copy action. Loading / empty / error are shown as calm, expected states.
 */
@Composable
fun TextDetectionDialog(
    state: TextDetectionState,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(MR.strings.text_detection_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            )
            Spacer(Modifier.height(MaterialTheme.padding.small))

            when (state) {
                TextDetectionState.Loading -> CenteredMessage { CircularProgressIndicator() }
                TextDetectionState.Empty -> CenteredMessage {
                    Text(
                        text = stringResource(MR.strings.text_detection_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                TextDetectionState.Error -> CenteredMessage {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(MR.strings.text_detection_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = onRetry) {
                            Text(stringResource(MR.strings.action_retry))
                        }
                    }
                }
                is TextDetectionState.Success -> DetectedLines(state.lines)
            }
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun DetectedLines(lines: List<DetectedTextLine>) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
    ) {
        itemsIndexed(lines) { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MaterialTheme.padding.medium,
                        end = MaterialTheme.padding.small,
                        top = MaterialTheme.padding.small,
                        bottom = MaterialTheme.padding.small,
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = line.text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            val clipEntry = ClipData.newPlainText(line.text, line.text).toClipEntry()
                            clipboard.setClipEntry(clipEntry)
                        }
                        context.toast(MR.strings.copied_to_clipboard_plain)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(MR.strings.action_copy_to_clipboard),
                    )
                }
            }
            if (index < lines.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
