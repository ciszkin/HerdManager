package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VerticalScrollbarWithStyle(
    gridState: LazyGridState,
    modifier: Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(gridState),
        modifier = modifier,
        style = LocalScrollbarStyle.current.copy(
            hoverColor = MaterialTheme.colorScheme.onSurface,
            unhoverColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
