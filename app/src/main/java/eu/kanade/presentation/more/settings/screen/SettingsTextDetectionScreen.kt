package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.textdetection.translation.TranslationEngine
import eu.kanade.tachiyomi.ui.reader.textdetection.translation.deleteArabicTranslationModel
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsTextDetectionScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_text_detection

    @Composable
    override fun getPreferences(): List<Preference> {
        val readerPreferences = remember { Injekt.get<ReaderPreferences>() }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val selectedEngine by readerPreferences.translationEngine.collectAsState()

        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.translationEngine,
                entries = TranslationEngine.entries
                    .associateWith { stringResource(it.titleRes) },
                title = stringResource(MR.strings.pref_translation_engine),
            ),
            // DeepL (online) — the user's own key, better quality.
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.translation_engine_deepl),
                preferenceItems = listOf(
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(MR.strings.pref_deepl_api_key),
                    ) {
                        DeepLApiKeyPreference(selectedEngine = selectedEngine)
                    },
                ),
            ),
            // On-device (Google ML Kit) — the offline language pack these settings manage.
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.translation_engine_mlkit),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPreferences.translationWifiOnly,
                        title = stringResource(MR.strings.pref_translation_wifi_only),
                        subtitle = stringResource(MR.strings.pref_translation_wifi_only_summary),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_delete_translation_model),
                        subtitle = stringResource(MR.strings.pref_delete_translation_model_summary),
                        onClick = {
                            scope.launch {
                                runCatching { deleteArabicTranslationModel() }
                                context.toast(MR.strings.translation_model_deleted)
                            }
                        },
                    ),
                ),
            ),
        )
    }
}
