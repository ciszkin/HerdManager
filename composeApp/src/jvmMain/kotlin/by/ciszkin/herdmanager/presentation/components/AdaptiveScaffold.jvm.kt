package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun AdaptiveScaffold(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
    language: String,
    hasUpdateBadge: Boolean,
    content: @Composable (Modifier) -> Unit
) {
    PermanentNavigationDrawer(
        drawerContent = {
            key(language) {
                Column(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    NavigationItem.entries.forEach { item ->
                        val showBadge = hasUpdateBadge && item.route == NavigationItem.Settings.route

                        NavigationRailItem(
                            modifier = Modifier.padding(top = 8.dp),
                            selected = selectedRoute == item.route,
                            onClick = { onRouteSelected(item.route) },
                            icon = {
                                if (showBadge) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text("!")
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = item.icon, contentDescription = item.getLabel())
                                    }
                                } else {
                                    Icon(imageVector = item.icon, contentDescription = item.getLabel())
                                }
                            },
                            label = {
                                Text(item.getLabel())
                            }
                        )
                    }
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
