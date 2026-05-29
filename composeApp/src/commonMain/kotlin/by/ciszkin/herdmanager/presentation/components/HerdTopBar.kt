package by.ciszkin.herdmanager.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.launch
import by.ciszkin.herdmanager.di.AppModule
import compose.icons.FeatherIcons
import compose.icons.feathericons.RefreshCw
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.refresh
import org.jetbrains.compose.resources.stringResource

private const val REFRESH_ANIMATION_DURATION_MS = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HerdTopBar(
    title: String,
    onRefresh: (() -> Unit)? = null,
    buttonColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    isLoading: Boolean = false,
    additionalActions: @Composable RowScope.() -> Unit = {}
) {
    val connectionState by AppModule.connectionMonitor.state.collectAsState()
    val refreshLabel = stringResource(Res.string.refresh)
    val rotation = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isLoading) {
        if (isLoading && !rotation.isRunning) {
            coroutineScope.launch {
                rotation.snapTo(0f)
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(durationMillis = REFRESH_ANIMATION_DURATION_MS)
                )
            }
        }
    }

    TopAppBar(
        title = { Text(title) },
        actions = {
            ConnectionStatusIcon(state = connectionState)
            if (onRefresh != null) {
                IconButton(onClick = { onRefresh() }) {
                    Icon(
                        imageVector = FeatherIcons.RefreshCw,
                        contentDescription = refreshLabel,
                        tint = buttonColor,
                        modifier = Modifier.rotate(rotation.value)
                    )
                }
            }
            additionalActions()
        }
    )
}
