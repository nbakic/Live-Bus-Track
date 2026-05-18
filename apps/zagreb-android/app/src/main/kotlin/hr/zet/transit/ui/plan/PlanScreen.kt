package hr.zet.transit.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.domain.model.JourneyLeg
import hr.zet.transit.domain.model.JourneyLegType
import hr.zet.transit.domain.model.JourneyPlan
import hr.zet.transit.domain.model.Stop
import org.koin.androidx.compose.koinViewModel

/**
 * Planiranje rute A→B (A2.1). Korisnik bira polazište i odredište pretragom
 * stajališta; backend (GraphHopper pt) vraća varijante s presjedanjima.
 */
@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Planiranje rute",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = state.fromQuery,
            onValueChange = { viewModel.onSearchQueryChange(it, PlanField.FROM) },
            label = { Text("Od") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.toQuery,
            onValueChange = { viewModel.onSearchQueryChange(it, PlanField.TO) },
            label = { Text("Do") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.searchResults.isNotEmpty()) {
            SearchResultsList(
                stops = state.searchResults,
                onSelect = viewModel::onStopSelected,
            )
        }

        Button(
            onClick = viewModel::onPlanRequested,
            enabled = state.canPlan && !state.isPlanning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Pronađi rutu")
        }

        when {
            state.isPlanning -> CircularProgressIndicator(
                modifier = Modifier.padding(top = 16.dp),
            )

            state.plans.isEmpty() && state.canPlan -> Text(
                text = "Nema pronađenih ruta za odabrane točke.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            else -> state.plans.forEach { plan ->
                JourneyPlanCard(plan)
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    stops: List<Stop>,
    onSelect: (Stop) -> Unit,
) {
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
        items(stops, key = Stop::id) { stop ->
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(stop) }
                    .padding(vertical = 10.dp),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun JourneyPlanCard(plan: JourneyPlan, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Trajanje: ${plan.totalDurationSeconds / 60} min",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            plan.legs.forEach { leg ->
                JourneyLegRow(leg)
            }
        }
    }
}

@Composable
private fun JourneyLegRow(leg: JourneyLeg, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when (leg.type) {
                JourneyLegType.WALK -> "🚶"
                JourneyLegType.TRANSIT -> "🚌"
            },
            modifier = Modifier.padding(end = 8.dp),
        )
        Column {
            Text(
                text = when (leg.type) {
                    JourneyLegType.WALK -> "Hodaj do ${leg.toName}"
                    JourneyLegType.TRANSIT ->
                        "Linija ${leg.routeName.orEmpty()} → ${leg.headsign.orEmpty()}"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${leg.fromName} → ${leg.toName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
