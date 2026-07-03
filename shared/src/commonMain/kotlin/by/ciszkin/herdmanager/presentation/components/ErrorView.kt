package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.domain.error.AppError
import by.ciszkin.herdmanager.presentation.error.toUserMessage
import herdmanager.shared.generated.resources.Res
import herdmanager.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

/**
 * Displays an error message with a retry button.
 *
 * @param error The AppError to display, or null to hide the view
 * @param onRetry Callback when the user taps the retry button
 */
@Composable
fun ErrorView(error: AppError?, onRetry: () -> Unit) {
    error ?: return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = error.toUserMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.retry))
            }
        }
    }
}
