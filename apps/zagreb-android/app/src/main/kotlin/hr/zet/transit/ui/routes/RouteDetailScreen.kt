package hr.zet.transit.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import hr.zet.transit.domain.model.TransitMode
import org.koin.androidx.compose.koinViewModel

/**
 * Detalji linije (A1.1) — osnovni podaci linije.
 *
 * Geometrija rute na karti (A1.2) i vozni red (A1.4) dolaze u Fazi A1.
 */
@Composable
fun RouteDetailScreen(
    routeId: String,
    modifier: Modifier = Modifier,
    viewModel: RouteDetailViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(routeId) { viewModel.load(routeId) }

    when {
        state.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        state.route == null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Linija nije pronađena.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        else -> {
            val route = state.route!!
            Column(
                modifier = modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = route.shortName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = route.longName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when (route.mode) {
                        TransitMode.TRAM -> "Tramvajska linija"
                        TransitMode.BUS -> "Autobusna linija"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
