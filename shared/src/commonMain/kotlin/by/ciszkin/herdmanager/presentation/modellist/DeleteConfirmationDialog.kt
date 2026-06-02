package by.ciszkin.herdmanager.presentation.modellist

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import herdmanager.shared.generated.resources.Res
import herdmanager.shared.generated.resources.confirm_delete
import herdmanager.shared.generated.resources.delete
import herdmanager.shared.generated.resources.cancel
import herdmanager.shared.generated.resources.delete_dialog_message
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteConfirmationDialog(
    modelName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.confirm_delete)) },
        text = { Text(stringResource(Res.string.delete_dialog_message, modelName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        modifier = modifier
    )
}
