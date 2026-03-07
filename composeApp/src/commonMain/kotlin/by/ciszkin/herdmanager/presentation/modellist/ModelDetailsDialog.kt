package by.ciszkin.herdmanager.presentation.modellist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.domain.model.OllamaModel
import by.ciszkin.herdmanager.presentation.components.DetailRow
import by.ciszkin.herdmanager.utils.formatDate
import by.ciszkin.herdmanager.utils.formatSize
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.additional_details
import herdmanager.composeapp.generated.resources.model_details
import herdmanager.composeapp.generated.resources.cancel
import herdmanager.composeapp.generated.resources.digest
import herdmanager.composeapp.generated.resources.families
import herdmanager.composeapp.generated.resources.family
import herdmanager.composeapp.generated.resources.format
import herdmanager.composeapp.generated.resources.model
import herdmanager.composeapp.generated.resources.modified
import herdmanager.composeapp.generated.resources.name
import herdmanager.composeapp.generated.resources.parameter_size
import herdmanager.composeapp.generated.resources.parent_model
import herdmanager.composeapp.generated.resources.quantization
import herdmanager.composeapp.generated.resources.size
import org.jetbrains.compose.resources.stringResource

@Composable
fun ModelDetailsDialog(
    model: OllamaModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.model_details)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow(stringResource(Res.string.name), model.name)
                DetailRow(stringResource(Res.string.model), model.model)
                DetailRow(stringResource(Res.string.size), formatSize(model.size))
                DetailRow(stringResource(Res.string.modified), formatDate(model.modifiedAt))
                DetailRow(stringResource(Res.string.digest), model.digest)

                model.details?.let { details ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.additional_details),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(stringResource(Res.string.format), details.format)
                    DetailRow(stringResource(Res.string.family), details.family)
                    DetailRow(stringResource(Res.string.families), details.families.joinToString(", "))
                    DetailRow(stringResource(Res.string.parameter_size), details.parameterSize)
                    DetailRow(stringResource(Res.string.quantization), details.quantizationLevel)
                    DetailRow(stringResource(Res.string.parent_model), details.parentModel)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

