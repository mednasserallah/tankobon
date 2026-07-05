package eu.kanade.tachiyomi.ui.reader.character

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The save/edit form shown after a portrait is cropped: a preview of the square portrait, a name
 * field (required) and an optional multi-line note. [onSave] receives the trimmed name and the note
 * (null when blank).
 */
@Composable
fun CharacterSaveDialog(
    target: CharacterSaveTarget,
    onDismissRequest: () -> Unit,
    onSave: (name: String, note: String?) -> Unit,
) {
    val portrait = remember(target) { target.portrait.asImageBitmap() }
    var name by remember(target) { mutableStateOf(target.initialName) }
    var note by remember(target) { mutableStateOf(target.initialNote) }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(
                    if (target.editingCharacterId >= 0) {
                        MR.strings.character_edit_title
                    } else {
                        MR.strings.character_save_title
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            Image(
                bitmap = portrait,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = MaterialTheme.padding.medium)
                    .align(Alignment.CenterHorizontally)
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(MR.strings.character_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.padding.medium),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(MR.strings.character_note_hint)) },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.padding.small),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(MR.strings.action_cancel))
                }
                TextButton(
                    onClick = { onSave(name.trim(), note.trim().ifBlank { null }) },
                    enabled = name.isNotBlank(),
                ) {
                    Text(stringResource(MR.strings.action_save))
                }
            }
        }
    }
}
