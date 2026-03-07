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
import by.ciszkin.herdmanager.presentation.components.DetailRow
import by.ciszkin.herdmanager.utils.formatDate
import by.ciszkin.herdmanager.utils.formatSize
import compose.icons.FeatherIcons
import compose.icons.feathericons.Info
import compose.icons.feathericons.Trash
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.delete_model
import herdmanager.composeapp.generated.resources.modified
import herdmanager.composeapp.generated.resources.show_details
import herdmanager.composeapp.generated.resources.size
import org.jetbrains.compose.resources.stringResource

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
                        Icon(FeatherIcons.Info, contentDescription = stringResource(Res.string.show_details))
                    }
                    IconButton(onClick = { onDelete(model.name) }) {
                        Icon(FeatherIcons.Trash, contentDescription = stringResource(Res.string.delete_model))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(stringResource(Res.string.size), formatSize(model.size))
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow(stringResource(Res.string.modified), formatDate(model.modifiedAt))
        }
    }
}
