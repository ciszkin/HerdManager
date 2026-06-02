package by.ciszkin.herdmanager.util

import android.content.Intent
import androidx.core.net.toUri
import by.ciszkin.herdmanager.di.provideApplicationContext

actual fun openUrl(url: String) {
    try {
        val context = provideApplicationContext()
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
