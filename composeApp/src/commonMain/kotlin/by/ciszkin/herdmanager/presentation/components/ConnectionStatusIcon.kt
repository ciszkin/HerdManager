package by.ciszkin.herdmanager.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import by.ciszkin.herdmanager.domain.model.ConnectionState

private val StatusConnectedColor = Color(0xFF4CAF50)
private val StatusDotSize = 10.dp
private val BadgeFontSize = 9.sp

@Composable
fun ConnectionStatusIcon(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        is ConnectionState.Idle, is ConnectionState.Running -> StatusConnectedColor
        is ConnectionState.Disconnected -> MaterialTheme.colorScheme.error
        ConnectionState.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(end = 4.dp)
    ) {
        Canvas(modifier = Modifier.size(StatusDotSize)) {
            drawCircle(color = color, radius = size.minDimension / 2)
        }
        if (state is ConnectionState.Running) {
            Text(
                text = state.count.toString(),
                fontSize = BadgeFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
