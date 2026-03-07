package by.ciszkin.herdmanager.presentation.registry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.domain.model.RegistryModel
import by.ciszkin.herdmanager.presentation.components.InfoLabel
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.pull
import herdmanager.composeapp.generated.resources.pull_count
import herdmanager.composeapp.generated.resources.tags_more
import org.jetbrains.compose.resources.stringResource

@Composable
fun RegistryCard(
    model: RegistryModel,
    onPull: (RegistryModel) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (model.capabilities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    model.capabilities.forEach { category ->
                        InfoLabel(text = category)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.pull_count, formatPullCount(model.pullCount)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (model.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    model.tags.take(3).forEach { tag ->
                        InfoLabel(text = tag)
                    }
                    if (model.tags.size > 3) {
                        Text(
                            text = stringResource(Res.string.tags_more,model.tags.size - 3),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onPull(model) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.pull))
            }
        }
    }
}

fun formatPullCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
        count >= 1_000 -> "%.1fK".format(count / 1_000.0)
        else -> count.toString()
    }
}
