package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VerticalScrollbarWithStyle(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
)
