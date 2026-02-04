package by.ciszkin.herdmanager.presentation.modellist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.utils.formatDate
import by.ciszkin.herdmanager.utils.formatSize

@Composable
fun ModelDetailsDialog(
    model: OllamaModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Model Details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Name", model.name)
                DetailRow("Model", model.model)
                DetailRow("Size", formatSize(model.size))
                DetailRow("Modified", formatDate(model.modifiedAt))
                DetailRow("Digest", model.digest)

                model.details?.let { details ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Additional Details",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Format", details.format)
                    DetailRow("Family", details.family)
                    DetailRow("Families", details.families.joinToString(", "))
                    DetailRow("Parameter Size", details.parameterSize)
                    DetailRow("Quantization Level", details.quantizationLevel)
                    DetailRow("Parent Model", details.parentModel)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            fontWeight = FontWeight.Bold,
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
