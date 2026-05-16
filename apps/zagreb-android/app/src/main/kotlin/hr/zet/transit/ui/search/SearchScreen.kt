package hr.zet.transit.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.Stop
import org.koin.androidx.compose.koinViewModel

/**
 * Pretraga stajališta i linija (A1.3) — dijakritika-neutralna.
 *
 * Rezultati grupirani: linije pa stajališta. Tap na liniju ili stajalište
 * navigira na odgovarajući detalj-ekran.
 */
@Composable
fun SearchScreen(
    onStopClick: (String) -> Unit,
    onRouteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Traži stajalište ili liniju") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            if (state.routes.isNotEmpty()) {
                item { SectionHeader("Linije") }
                items(state.routes, key = { "route-" + it.id }) { route ->
                    ResultRow(
                        text = "${route.shortName} — ${route.longName}",
                        onClick = { onRouteClick(route.id) },
                    )
                    HorizontalDivider()
                }
            }
            if (state.stops.isNotEmpty()) {
                item { SectionHeader("Stajališta") }
                items(state.stops, key = { "stop-" + it.id }) { stop ->
                    ResultRow(
                        text = stop.name,
                        onClick = { onStopClick(stop.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun ResultRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
