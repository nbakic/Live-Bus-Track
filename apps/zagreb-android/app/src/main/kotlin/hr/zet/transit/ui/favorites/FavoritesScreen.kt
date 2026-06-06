package hr.zet.transit.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.ui.common.EmptyState
import hr.zet.transit.ui.common.LoadingState
import org.koin.androidx.compose.koinViewModel

/**
 * Popis omiljenih stajališta (A0.3). Tap na stajalište vodi na detalje.
 *
 * Prazno stanje je očekivano dok korisnik ne označi prvo stajalište — tekst
 * objašnjava kako (umjesto pukog praznog ekrana).
 */
@Composable
fun FavoritesScreen(
    onStopClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Omiljena stajališta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        when {
            state.isLoading -> LoadingState()

            state.stops.isEmpty() -> EmptyState(
                icon = Icons.Filled.StarBorder,
                title = "Još nemaš omiljenih stajališta",
                subtitle = "Otvori stajalište i dodirni zvjezdicu da ga spremiš ovdje.",
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.stops, key = Stop::id) { stop ->
                    Text(
                        text = stop.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStopClick(stop.id) }
                            .padding(vertical = 14.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
