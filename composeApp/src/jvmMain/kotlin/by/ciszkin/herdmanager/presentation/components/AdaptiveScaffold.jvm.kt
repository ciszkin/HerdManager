package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
actual fun AdaptiveScaffold(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    PermanentNavigationDrawer(
        drawerContent = {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 8.dp)
            ) {
                NavigationItem.entries.forEach { item ->
                    NavigationRailItem(
                        modifier = Modifier.padding(top = 8.dp),
                        selected = selectedRoute == item.route,
                        onClick = { onRouteSelected(item.route) },
                        icon = {
                            Icon(imageVector = item.icon, contentDescription = item.label)
                        },
                        label = {
                            Text(item.label)
                        }
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .padding(start = 0.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}
