package eu.kanade.tachiyomi.ui.reader.textdetection.translation

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Encrypted-at-rest storage for the DeepL API key — a real user credential.
 *
 * Backed by Jetpack Security [EncryptedSharedPreferences] (AES-256), a separate mechanism from the
 * app's ordinary [tachiyomi.core.common.preference.PreferenceStore], which keeps values in plaintext
 * SharedPreferences. The key never leaves this store except when a request is made, and is never
 * logged. This file lives outside the normal preferences, so it is not swept into backups.
 */
class DeepLKeyStore(context: Context) {

    private val prefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.onFailure {
            logcat(LogPriority.ERROR, it) { "Failed to open encrypted DeepL key store" }
        }.getOrNull()
    }

    /** The stored key, or empty string if none / the store couldn't be opened. */
    fun getApiKey(): String = prefs?.getString(KEY_API_KEY, "").orEmpty()

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    fun setApiKey(key: String) {
        prefs?.edit()?.putString(KEY_API_KEY, key.trim())?.apply()
    }

    fun clear() {
        prefs?.edit()?.remove(KEY_API_KEY)?.apply()
    }

    private companion object {
        const val FILE_NAME = "deepl_secure_prefs"
        const val KEY_API_KEY = "deepl_api_key"
    }
}
