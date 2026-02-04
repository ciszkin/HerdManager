package by.ciszkin.herdmanager.presentation.modellist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.utils.formatDate
import by.ciszkin.herdmanager.utils.formatSize
import compose.icons.FeatherIcons
import compose.icons.feathericons.Info
import compose.icons.feathericons.Trash

@Composable
fun ModelCard(
    model: OllamaModel,
    onDelete: (String) -> Unit,
    onShowDetails: (OllamaModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = { onShowDetails(model) }) {
                        Icon(FeatherIcons.Info, contentDescription = "Show Details")
                    }
                    IconButton(onClick = { onDelete(model.name) }) {
                        Icon(FeatherIcons.Trash, contentDescription = "Delete Model")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Size: ${formatSize(model.size)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Modified: ${formatDate(model.modifiedAt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
