package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Download
import compose.icons.feathericons.Info
import herdmanager.composeapp.generated.resources.Res
import herdmanager.composeapp.generated.resources.get_update
import herdmanager.composeapp.generated.resources.update_available
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateBanner(
    currentVersion: String,
    latestVersion: String,
    onOpenRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = FeatherIcons.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = stringResource(Res.string.update_available, latestVersion, currentVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Button(
                onClick = onOpenRelease
            ) {
                Icon(FeatherIcons.Download, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(stringResource(Res.string.get_update))
            }
        }
    }
}
