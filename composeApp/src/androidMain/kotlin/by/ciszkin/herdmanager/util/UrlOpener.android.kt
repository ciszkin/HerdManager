package by.ciszkin.herdmanager.util

import android.content.Intent
import androidx.core.net.toUri
import by.ciszkin.herdmanager.provideApplicationContext

actual fun openUrl(url: String) {
    val context = provideApplicationContext()
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}
