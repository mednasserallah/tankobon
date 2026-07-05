package eu.kanade.tachiyomi.ui.reader.character

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.domain.character.model.Character
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.io.File

private const val SEARCH_THRESHOLD = 4

/**
 * Bottom sheet listing the characters saved for the current series. Each row shows the portrait,
 * name and a note snippet; tapping expands it to the full note with Re-crop / Edit / Delete actions.
 * When there are more than [SEARCH_THRESHOLD] characters a name search field is shown.
 */
@Composable
fun CharacterListDialog(
    characters: List<Character>,
    onDismissRequest: () -> Unit,
    onDelete: (Character) -> Unit,
    onUpdate: (Character, String, String?) -> Unit,
    onRecrop: (Character) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var expandedId by remember { mutableLongStateOf(-1L) }
    var editingId by remember { mutableLongStateOf(-1L) }

    val filtered = remember(characters, query) {
        if (query.isBlank()) {
            characters
        } else {
            characters.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
    }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(MR.strings.character_notebook_title),
                style = MaterialTheme.typography.titleLarge,
            )

            if (characters.isEmpty()) {
                Text(
                    text = stringResource(MR.strings.character_notebook_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = MaterialTheme.padding.medium),
                )
                return@Column
            }

            if (characters.size > SEARCH_THRESHOLD) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(MR.strings.character_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.padding.small),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
            ) {
                items(filtered, key = { it.id }) { character ->
                    CharacterRow(
                        character = character,
                        expanded = expandedId == character.id,
                        editing = editingId == character.id,
                        onToggle = {
                            editingId = -1L
                            expandedId = if (expandedId == character.id) -1L else character.id
                        },
                        onStartEdit = { editingId = character.id },
                        onCancelEdit = { editingId = -1L },
                        onSaveEdit = { name, note ->
                            onUpdate(character, name, note)
                            editingId = -1L
                        },
                        onDelete = { onDelete(character) },
                        onRecrop = { onRecrop(character) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CharacterRow(
    character: Character,
    expanded: Boolean,
    editing: Boolean,
    onToggle: () -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (String, String?) -> Unit,
    onDelete: () -> Unit,
    onRecrop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = MaterialTheme.padding.small),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CharacterPortrait(character.portraitPath, 80.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = MaterialTheme.padding.small),
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!character.note.isNullOrBlank()) {
                    Text(
                        text = character.note!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (editing) {
            var name by remember(character) { mutableStateOf(character.name) }
            var note by remember(character) { mutableStateOf(character.note.orEmpty()) }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(MR.strings.character_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.padding.small),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(MR.strings.character_note_hint)) },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.padding.small),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancelEdit) {
                    Text(stringResource(MR.strings.action_cancel))
                }
                TextButton(
                    onClick = { onSaveEdit(name.trim(), note.trim().ifBlank { null }) },
                    enabled = name.isNotBlank(),
                ) {
                    Text(stringResource(MR.strings.action_save))
                }
            }
        } else if (expanded) {
            CharacterPortrait(
                path = character.portraitPath,
                size = 220.dp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = MaterialTheme.padding.small),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            ) {
                TextButton(onClick = onRecrop) {
                    Text(stringResource(MR.strings.action_recrop_portrait))
                }
                TextButton(onClick = onStartEdit) {
                    Text(stringResource(MR.strings.action_edit))
                }
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(MR.strings.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterPortrait(path: String?, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Key the Coil request on the file's mtime so a re-cropped portrait (same path) isn't served
    // stale from cache.
    val model = remember(path) {
        path?.let {
            val file = File(it)
            val key = "$it:${file.lastModified()}"
            ImageRequest.Builder(context)
                .data(file)
                .memoryCacheKey(key)
                .diskCacheKey(key)
                .build()
        }
    }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(PortraitPlaceholderColor),
    )
}

private val PortraitPlaceholderColor = Color(0x1F888888)
