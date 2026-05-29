package hr.zet.transit.ui.alerts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.domain.model.AlertSeverity
import hr.zet.transit.domain.model.ServiceAlert
import org.koin.androidx.compose.koinViewModel

/**
 * Prometne obavijesti (A0.4) — lista aktivnih service alertsa.
 *
 * Plan (sekcija 11): obavijesti su kod konkurenta "zid teksta". Ovdje su
 * jasno odvojene karticom po ozbiljnosti; B4 dodaje "što to znači za mene".
 */
@Composable
fun AlertsScreen(
    modifier: Modifier = Modifier,
    viewModel: AlertsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Prometne obavijesti",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            !state.isLive && state.alerts.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nije moguće dohvatiti obavijesti — provjeri vezu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            state.alerts.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Trenutno nema prometnih obavijesti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
            ) {
                items(state.alerts, key = ServiceAlert::id) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: ServiceAlert, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = severityColor(alert.severity)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = alert.headerText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (alert.descriptionText.isNotBlank()) {
                Text(
                    text = alert.descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (alert.affectedRouteIds.isNotEmpty()) {
                Text(
                    text = "Linije: ${alert.affectedRouteIds.joinToString(", ")}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Blaga pozadinska boja po ozbiljnosti — ne vrišti, ali se razlikuje. */
private fun severityColor(severity: AlertSeverity): Color = when (severity) {
    AlertSeverity.SEVERE -> Color(0xFFFFDAD6)
    AlertSeverity.WARNING -> Color(0xFFFFF1C2)
    AlertSeverity.INFO -> Color(0xFFE3F0FF)
}
