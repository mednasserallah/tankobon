package eu.kanade.presentation.reader

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.reader.textdetection.DetectedLineItem
import eu.kanade.tachiyomi.ui.reader.textdetection.TextDetectionState
import eu.kanade.tachiyomi.ui.reader.textdetection.TranslationState
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Bottom sheet listing the English text detected on the current page, in manga reading order, with
 * a per-line copy action and a "Translate all" action that shows the Arabic translation under each
 * line. Loading / empty / error are shown as calm, expected states.
 *
 * Per Google's ML Kit / Cloud Translation attribution requirements, a "Translation powered by
 * Google Translate" line is shown adjacent to the translation results.
 */
@Composable
fun TextDetectionDialog(
    state: TextDetectionState,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
    onTranslateAll: () -> Unit,
    onTranslateLine: (Int) -> Unit,
    onEditLine: (Int, String) -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.medium),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.text_detection_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (state is TextDetectionState.Success) {
                    TextButton(onClick = onTranslateAll) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(MaterialTheme.padding.extraSmall))
                        Text(stringResource(MR.strings.action_translate_all))
                    }
                }
            }
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
                is TextDetectionState.Success -> DetectedLines(state.items, onTranslateLine, onEditLine)
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
private fun DetectedLines(
    items: List<DetectedLineItem>,
    onTranslateLine: (Int) -> Unit,
    onEditLine: (Int, String) -> Unit,
) {
    // Attribution must credit the engine that actually produced each translation. After a DeepL→ML Kit
    // fallback the two can be mixed, so show every engine present among the finished lines.
    val attributions = items
        .mapNotNull { (it.translation as? TranslationState.Done)?.engine }
        .distinct()

    Column {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
        ) {
            itemsIndexed(items) { index, item ->
                DetectedLineRow(
                    item = item,
                    onTranslate = { onTranslateLine(index) },
                    onEditCommit = { onEditLine(index, it) },
                )
                if (index < items.lastIndex) {
                    HorizontalDivider()
                }
            }
        }

        attributions.forEach { engine ->
            Text(
                text = stringResource(engine.attributionRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.medium,
                    top = MaterialTheme.padding.small,
                ),
            )
        }
    }
}

@Composable
private fun DetectedLineRow(
    item: DetectedLineItem,
    onTranslate: () -> Unit,
    onEditCommit: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    // Re-seed the draft whenever the committed text changes (e.g. after an edit is applied).
    var draft by remember(item.text) { mutableStateOf(item.text) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.small,
                top = MaterialTheme.padding.small,
                bottom = MaterialTheme.padding.small,
            ),
    ) {
        if (editing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onEditCommit(draft)
                            editing = false
                        },
                    ),
                )
                IconButton(
                    onClick = {
                        onEditCommit(draft)
                        editing = false
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = stringResource(MR.strings.action_ok),
                    )
                }
            }
        } else {
            // Flag only un-edited low-confidence lines (via dimmed text); once corrected, the hint clears.
            val flagged = item.text == item.line.text && item.line.isLowConfidence()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.text,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            draft = item.text
                            editing = true
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (flagged) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                IconButton(
                    onClick = onTranslate,
                    enabled = item.translation !is TranslationState.Downloading &&
                        item.translation !is TranslationState.Translating,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Translate,
                        contentDescription = stringResource(MR.strings.action_translate),
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            val clip = ClipData.newPlainText(item.text, item.text).toClipEntry()
                            clipboard.setClipEntry(clip)
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
        }
        TranslationRow(item.translation, onRetry = onTranslate)
    }
}

@Composable
private fun TranslationRow(state: TranslationState, onRetry: () -> Unit) {
    when (state) {
        TranslationState.Idle -> {}
        TranslationState.Downloading -> LabelWithSpinner(stringResource(MR.strings.translation_downloading))
        TranslationState.Translating -> LabelWithSpinner(stringResource(MR.strings.translation_in_progress))
        is TranslationState.Done -> {
            // Arabic reads right-to-left.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = state.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.padding.extraSmall),
                )
            }
        }
        TranslationState.Error -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(MR.strings.translation_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(MR.strings.action_retry))
                }
            }
        }
    }
}

@Composable
private fun LabelWithSpinner(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        modifier = Modifier.padding(top = MaterialTheme.padding.extraSmall),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
