package by.ciszkin.herdmanager.presentation.registry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.domain.model.PullResult
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.pull
import herdmanager.composeapp.generated.resources.cancel
import herdmanager.composeapp.generated.resources.close
import herdmanager.composeapp.generated.resources.digest
import herdmanager.composeapp.generated.resources.pull_complete
import herdmanager.composeapp.generated.resources.pull_failed
import herdmanager.composeapp.generated.resources.pulling
import herdmanager.composeapp.generated.resources.ready_to_pull
import herdmanager.composeapp.generated.resources.select_size
import herdmanager.composeapp.generated.resources.size
import herdmanager.composeapp.generated.resources.starting
import herdmanager.composeapp.generated.resources.successfully_pulled
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullModelDialog(
    modelName: String,
    selectedTag: String?,
    availableTags: List<String>,
    pullResult: PullResult?,
    onTagSelected: (String) -> Unit,
    onPull: () -> Unit,
    onDismiss: () -> Unit
) {
    val isPulling = pullResult is PullResult.Starting || pullResult is PullResult.Progress
    val progress = (pullResult as? PullResult.Progress)?.progress
    val hasTags = availableTags.isNotEmpty()

    AlertDialog(
        onDismissRequest = { if (!isPulling) onDismiss() },
        title = {
            Text(
                text = when (pullResult) {
                    null -> stringResource(Res.string.pull)
                    PullResult.Starting -> stringResource(Res.string.pulling, modelName)
                    is PullResult.Progress -> stringResource(Res.string.pulling, modelName)
                    PullResult.Completed -> stringResource(Res.string.pull_complete)
                    is PullResult.Error -> stringResource(Res.string.pull_failed)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.select_size),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isPulling) expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedTag ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.size)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isPulling
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    onTagSelected(tag)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                when (pullResult) {
                    null -> {
                        if (selectedTag != null) {
                            Text(
                                text = stringResource(Res.string.ready_to_pull, modelName, selectedTag),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    PullResult.Starting -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.starting),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is PullResult.Progress -> {
                        progress?.let { currentProgress ->
                            val progressValue = if (currentProgress.total != null && currentProgress.completed != null) {
                                currentProgress.completed.toFloat() / currentProgress.total
                            } else {
                                null
                            }
                            
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (progressValue != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(progressValue * 100).toInt()}%",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${formatBytes(currentProgress.completed ?: 0)} / ${formatBytes(currentProgress.total ?: 0)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progressValue },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                currentProgress.status?.let { status ->
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                currentProgress.digest?.let { digest ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(Res.string.digest) + digest.take(16) + "...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    PullResult.Completed -> {
                        Text(
                            text = stringResource(Res.string.successfully_pulled, modelName, selectedTag ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is PullResult.Error -> {
                        Text(
                            text = pullResult.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (pullResult) {
                null -> {
                    Button(
                        onClick = onPull,
                        enabled = selectedTag != null || !hasTags
                    ) {
                        Text(stringResource(Res.string.pull))
                    }
                }
                PullResult.Starting, is PullResult.Progress -> {
                    Button(onClick = onDismiss) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
                else -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.close))
                    }
                }
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.2f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024L -> "%.2f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}