package hr.zet.transit.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.domain.model.Arrival
import hr.zet.transit.domain.model.Routine
import hr.zet.transit.domain.model.RoutineKind
import hr.zet.transit.ui.common.LoadingState
import hr.zet.transit.ui.stop.formatEta
import org.koin.androidx.compose.koinViewModel

/**
 * Jutarnji ekran (A1.5) — rutine Dom/Posao/Škola sa živim dolascima.
 *
 * Primarna persona je dnevni putnik (plan sekcija 11): ne traži stajalište,
 * treba odgovor "kad mi kreće" koji ga dočeka. Tap na rutinu vodi na detalje
 * tog stajališta. Rutine se postavljaju s ekrana stajališta.
 */
@Composable
fun RoutinesScreen(
    onStopClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Moje rutine",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        when {
            state.isLoading -> LoadingState()

            state.routines.isEmpty() -> Text(
                text = "Još nemaš rutina.\n" +
                    "Otvori stajalište i postavi ga kao Dom, Posao ili Školu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )

            else -> state.routines.forEach { routine ->
                RoutineCard(
                    routine = routine,
                    arrivals = state.arrivalsByKind[routine.kind].orEmpty(),
                    // Bez ulaza u mapu = još čekamo prvi snapshot → tretiraj kao živ.
                    isLive = state.liveByKind[routine.kind] ?: true,
                    onClick = { onStopClick(routine.stopId) },
                )
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    arrivals: List<Arrival>,
    isLive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = routineLabel(routine.kind),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = routine.stopName,
                style = MaterialTheme.typography.titleMedium,
            )
            if (arrivals.isEmpty()) {
                // Razlikuj "stvarno nema dolazaka" od "backend ne odgovara" (C8) —
                // inače bi pad poslužitelja izgledao kao mirno jutro bez polazaka.
                Text(
                    text = if (isLive) {
                        "Nema najavljenih dolazaka."
                    } else {
                        "Dolasci trenutno nisu dostupni — provjeri vezu."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                arrivals.forEach { arrival ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${arrival.routeShortName} → ${arrival.headsign}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formatEta(arrival.predictedTime),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun routineLabel(kind: RoutineKind): String = when (kind) {
    RoutineKind.HOME -> "Dom"
    RoutineKind.WORK -> "Posao"
    RoutineKind.SCHOOL -> "Škola"
}
