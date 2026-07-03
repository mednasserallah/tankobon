package eu.kanade.tachiyomi.ui.webview

import android.content.Context
import androidx.core.net.toUri
import cafe.adriel.voyager.core.model.StateScreenModel
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class WebViewScreenModel(
    val sourceId: Long?,
    private val network: NetworkHelper = Injekt.get(),
) : StateScreenModel<StatsScreenState>(StatsScreenState.Loading) {

    var headers = emptyMap<String, String>()

    fun shareWebpage(context: Context, url: String) {
        try {
            context.startActivity(url.toUri().toShareIntent(context, type = "text/plain"))
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        context.openInBrowser(url, forceDefaultBrowser = true)
    }

    fun clearCookies(url: String) {
        url.toHttpUrlOrNull()?.let {
            val cleared = network.cookieJar.remove(it)
            logcat { "Cleared $cleared cookies for: $url" }
        }
    }
}
