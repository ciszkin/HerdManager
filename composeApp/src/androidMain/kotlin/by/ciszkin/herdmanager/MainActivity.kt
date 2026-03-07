package by.ciszkin.herdmanager

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import by.ciszkin.herdmanager.data.local.applicationContext as localApplicationContext
import by.ciszkin.herdmanager.di.AppModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        localApplicationContext = this

        lifecycleScope.launch {
            val settings = AppModule.observeSettingsUseCase().first()
            setLocale(settings.language)

            setContent {
                App()
            }
        }
    }

    private fun setLocale(language: String) {
        val locale = when (language) {
            "be" -> Locale("be")
            else -> Locale("en")
        }
        Locale.setDefault(locale)

        val context = localApplicationContext
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
