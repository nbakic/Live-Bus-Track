package hr.zet.transit.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.maps.MapLibreMap

/**
 * Karta vozila (A0.1) — MapLibre karta sa živim slojem vozila.
 *
 * Karta drži vlastiti [MapLibreMap]; kad stigne novi `uiState.vehicles`,
 * [VehicleLayer.update] samo zamijeni GeoJSON podatke (bez rekreiranja sloja).
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val vehicleLayer = remember { VehicleLayer() }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Karta vozila ZET-a" },
    ) {
        MapLibreView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { readyMap, style ->
                vehicleLayer.attach(style)
                map = readyMap
            },
        )

        // Sloj vozila se osvježi na svaku promjenu stanja kad je karta spremna.
        map?.let { vehicleLayer.update(it, state.vehicles) }

        StatusOverlay(
            vehicleCount = state.vehicles.size,
            isLive = state.isLive,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        )
    }
}

/** Tanka traka na vrhu karte — broj vozila + degradacija kad RT padne. */
@Composable
private fun StatusOverlay(
    vehicleCount: Int,
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isLive) {
                "$vehicleCount vozila uživo"
            } else {
                "Podaci uživo trenutno nedostupni — prikazan red vožnje."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}
