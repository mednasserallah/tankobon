package eu.kanade.domain.base

import android.content.Context
import eu.kanade.tachiyomi.util.system.GLUtil
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class BasePreferences(
    val context: Context,
    preferenceStore: PreferenceStore,
) {

    val incognitoMode: Preference<Boolean> = preferenceStore.getBoolean(Preference.appStateKey("incognito_mode"), false)

    val shownOnboardingFlow: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey("onboarding_complete"),
        false,
    )

    val displayProfile: Preference<String> = preferenceStore.getString("pref_display_profile_key", "")

    val hardwareBitmapThreshold: Preference<Int> = preferenceStore.getInt(
        "pref_hardware_bitmap_threshold",
        GLUtil.SAFE_TEXTURE_LIMIT,
    )

    val alwaysDecodeLongStripWithSSIV: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_always_decode_long_strip_with_ssiv",
        false,
    )

    val installationId: Preference<String> = preferenceStore.getString(Preference.appStateKey("installation_id"), "")
}
