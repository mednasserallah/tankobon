package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.reader.textdetection.translation.DeepLKeyStore
import eu.kanade.tachiyomi.ui.reader.textdetection.translation.DeepLTranslator
import eu.kanade.tachiyomi.ui.reader.textdetection.translation.TranslationEngine
import eu.kanade.tachiyomi.ui.reader.textdetection.translation.TranslationResult
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val DEEPL_SIGNUP_URL = "https://www.deepl.com/pro-api"

/** Result of the last "Validate key" tap: nothing yet, checking, valid, or a specific failure. */
private sealed interface KeyStatus {
    data object Idle : KeyStatus
    data object Checking : KeyStatus
    data object Valid : KeyStatus
    data class Error(val message: StringResource) : KeyStatus
}

/**
 * The DeepL section of the text-translation settings: a masked API-key field with a reveal toggle,
 * Save (persists to encrypted storage), and Validate (checks the key against DeepL's usage endpoint
 * off the main thread with a clear success/failure indicator). Shows a warning when DeepL is the
 * selected engine but no key is stored, rather than silently falling back.
 *
 * Rendered inside a [Preference.PreferenceItem.CustomPreference] so it owns its own layout/state.
 */
@Composable
fun DeepLApiKeyPreference(selectedEngine: TranslationEngine) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val keyStore = remember { Injekt.get<DeepLKeyStore>() }
    val networkHelper = remember { Injekt.get<NetworkHelper>() }
    val json = remember { Injekt.get<Json>() }

    var draft by remember { mutableStateOf(keyStore.getApiKey()) }
    var savedKey by remember { mutableStateOf(keyStore.getApiKey()) }
    var hidden by rememberSaveable { mutableStateOf(true) }
    var status by remember { mutableStateOf<KeyStatus>(KeyStatus.Idle) }

    Column(
        modifier = Modifier.padding(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.small,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Text(
            text = stringResource(MR.strings.pref_deepl_api_key),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(MR.strings.pref_deepl_api_key_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = draft,
            onValueChange = {
                draft = it
                status = KeyStatus.Idle
            },
            label = { Text(stringResource(MR.strings.pref_deepl_api_key)) },
            singleLine = true,
            visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { hidden = !hidden }) {
                    Icon(
                        imageVector = if (hidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = stringResource(
                            if (hidden) MR.strings.action_show_api_key else MR.strings.action_hide_api_key,
                        ),
                    )
                }
            },
            isError = status is KeyStatus.Error,
        )

        StatusLine(status)

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                enabled = status != KeyStatus.Checking,
                onClick = {
                    keyStore.setApiKey(draft)
                    savedKey = draft.trim()
                    status = KeyStatus.Idle
                    context.toast(MR.strings.deepl_key_saved)
                },
            ) {
                Text(stringResource(MR.strings.action_save))
            }

            FilledTonalButton(
                enabled = status != KeyStatus.Checking,
                onClick = {
                    val entered = draft.trim()
                    if (entered.isEmpty()) {
                        status = KeyStatus.Error(MR.strings.deepl_key_missing)
                        return@FilledTonalButton
                    }
                    status = KeyStatus.Checking
                    scope.launch {
                        val result = withIOContext {
                            DeepLTranslator(entered, networkHelper.client, json).validateKey()
                        }
                        status = if (result is TranslationResult.Success) {
                            // A working key — persist it so the user doesn't also have to tap Save.
                            keyStore.setApiKey(entered)
                            savedKey = entered
                            KeyStatus.Valid
                        } else {
                            KeyStatus.Error(validationErrorMessage(result))
                        }
                    }
                },
            ) {
                Text(stringResource(MR.strings.action_validate_key))
            }

            if (savedKey.isNotBlank()) {
                TextButton(
                    enabled = status != KeyStatus.Checking,
                    onClick = {
                        keyStore.clear()
                        savedKey = ""
                        draft = ""
                        status = KeyStatus.Idle
                        context.toast(MR.strings.deepl_key_cleared)
                    },
                ) {
                    Text(stringResource(MR.strings.action_remove))
                }
            }
        }

        TextButton(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            onClick = { uriHandler.openUri(DEEPL_SIGNUP_URL) },
        ) {
            Text(stringResource(MR.strings.pref_deepl_get_key))
        }

        if (selectedEngine == TranslationEngine.DEEPL && savedKey.isBlank()) {
            Text(
                text = stringResource(MR.strings.deepl_engine_no_key_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun StatusLine(status: KeyStatus) {
    when (status) {
        KeyStatus.Idle -> Unit
        KeyStatus.Checking -> Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
        KeyStatus.Valid -> Text(
            text = stringResource(MR.strings.deepl_key_valid),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        is KeyStatus.Error -> Text(
            text = stringResource(status.message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun validationErrorMessage(result: TranslationResult): StringResource = when (result) {
    TranslationResult.MissingApiKey -> MR.strings.deepl_key_missing
    TranslationResult.InvalidApiKey -> MR.strings.deepl_key_invalid
    TranslationResult.QuotaExceeded -> MR.strings.deepl_key_quota
    TranslationResult.RateLimited -> MR.strings.deepl_key_rate_limited
    TranslationResult.NetworkError -> MR.strings.deepl_key_network_error
    else -> MR.strings.deepl_key_error
}
