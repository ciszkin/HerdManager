package by.ciszkin.herdmanager.presentation.modellist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.domain.model.OllamaModel

@Composable
fun ModelGrid(
    models: List<OllamaModel>,
    onDelete: (String) -> Unit,
    onShowDetails: (OllamaModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(models, key = { it.name }) { model ->
            ModelCard(
                model = model,
                onDelete = onDelete,
                onShowDetails = onShowDetails
            )
        }
    }
}
