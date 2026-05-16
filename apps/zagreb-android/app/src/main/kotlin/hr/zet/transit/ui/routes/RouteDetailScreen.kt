package hr.zet.transit.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.domain.model.DirectionSchedule
import hr.zet.transit.domain.model.TransitMode
import hr.zet.transit.ui.map.MapLibreView
import hr.zet.transit.ui.map.RouteShapeLayer
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.maps.MapLibreMap

/**
 * Detalji linije — osnovni podaci (A1.1), geometrija na karti (A1.2),
 * kompletan statički vozni red (A1.4).
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
            var map by remember { mutableStateOf<MapLibreMap?>(null) }
            val shapeLayer = remember { RouteShapeLayer() }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
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

                // Geometrija rute na karti (A1.2).
                MapLibreView(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    onMapReady = { readyMap, style ->
                        shapeLayer.attach(style)
                        map = readyMap
                    },
                )
                map?.let { shapeLayer.setShape(it, state.shape) }

                // Vozni red (A1.4).
                ScheduleSection(directions = state.schedule?.directions.orEmpty())
            }
        }
    }
}

@Composable
private fun ScheduleSection(
    directions: List<DirectionSchedule>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        Text(
            text = "Vozni red",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (directions.isEmpty()) {
            Text(
                text = "Vozni red nije dostupan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            directions.forEach { direction ->
                DirectionSchedule(direction)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectionSchedule(
    direction: DirectionSchedule,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 12.dp)) {
        Text(
            text = "Smjer: ${direction.headsign}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            direction.departures.forEach { time ->
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
