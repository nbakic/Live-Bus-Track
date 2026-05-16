package hr.zet.transit.ui.stop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.domain.model.Arrival
import org.koin.androidx.compose.koinViewModel

/**
 * Stajalište-detalji (A0.2) — lista predviđenih dolazaka.
 *
 * Svaki redak: linija, smjer, "za X min". RT predikcije i statički
 * fallback razlikuju se oznakom (plan: ne lažirati "live").
 */
@Composable
fun StopDetailScreen(
    stopId: String,
    modifier: Modifier = Modifier,
    viewModel: StopDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(stopId) { viewModel.load(stopId) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = state.stopName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.arrivals.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nema najavljenih dolazaka.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.arrivals, key = { it.routeId + it.predictedTime }) { arrival ->
                    ArrivalRow(arrival)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ArrivalRow(arrival: Arrival, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = arrival.routeShortName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = arrival.headsign,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatEta(arrival.predictedTime),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                // Plan: jasno razlikovati potvrđeno (RT) od estimacije (statika).
                text = if (arrival.isRealtime) "uživo" else "po redu vožnje",
                style = MaterialTheme.typography.labelSmall,
                color = if (arrival.isRealtime) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
