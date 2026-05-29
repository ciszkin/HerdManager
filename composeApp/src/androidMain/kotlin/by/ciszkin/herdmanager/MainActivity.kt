package by.ciszkin.herdmanager

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import by.ciszkin.herdmanager.di.AppModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

lateinit var localApplicationContext: Context

fun provideApplicationContext(): Context =
    if (::localApplicationContext.isInitialized) localApplicationContext
    else throw IllegalStateException("Application context not initialized. Make sure MainActivity is created first.")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        localApplicationContext = applicationContext

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

        val context = provideApplicationContext()
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
