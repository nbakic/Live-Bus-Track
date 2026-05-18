package hr.zet.transit.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.graphics.RectF
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.maps.MapLibreMap

/**
 * Karta vozila (A0.1) — MapLibre karta sa živim slojem vozila i statičkim
 * slojem stajališta. Tap na stajalište navigira na stajalište-detalje.
 */
@Composable
fun MapScreen(
    onStopClick: (String) -> Unit,
    onRoutesClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onRoutinesClick: () -> Unit,
    onNearbyClick: () -> Unit,
    onPlanClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val vehicleLayer = remember { VehicleLayer() }
    val stopLayer = remember { StopLayer() }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Karta vozila ZET-a" },
    ) {
        MapLibreView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { readyMap, style ->
                // Stajališta prvo (ispod), vozila iznad.
                stopLayer.attach(style)
                vehicleLayer.attach(style)
                readyMap.addOnMapClickListener { latLng ->
                    val tappedStopId = readyMap.findStopAt(latLng)
                    if (tappedStopId != null) {
                        onStopClick(tappedStopId)
                        true
                    } else {
                        false
                    }
                }
                map = readyMap
            },
        )

        // Slojevi se osvježe na svaku promjenu stanja kad je karta spremna.
        map?.let { readyMap ->
            stopLayer.setStops(readyMap, state.stops)
            vehicleLayer.update(readyMap, state.vehicles)
        }

        StatusOverlay(
            vehicleCount = state.vehicles.size,
            isLive = state.isLive,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapAction("Ruta", onPlanClick)
                MapAction("Traži", onSearchClick)
                MapAction("U blizini", onNearbyClick)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapAction("Rutine", onRoutinesClick)
                MapAction("Omiljeni", onFavoritesClick)
                MapAction("Linije", onRoutesClick)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapAction("Obavijesti", onAlertsClick)
            }
        }
    }
}

@Composable
private fun MapAction(label: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        text = { Text(label) },
        icon = {},
        onClick = onClick,
    )
}

/** Vraća `stopId` stajališta pod tapom, ili null ako ondje nema stajališta. */
private fun MapLibreMap.findStopAt(latLng: org.maplibre.android.geometry.LatLng): String? {
    val screenPoint = projection.toScreenLocation(latLng)
    // Tap-tolerancija ±12 px — prst nije precizan kao kursor.
    val touchArea = RectF(
        screenPoint.x - TAP_TOLERANCE_PX,
        screenPoint.y - TAP_TOLERANCE_PX,
        screenPoint.x + TAP_TOLERANCE_PX,
        screenPoint.y + TAP_TOLERANCE_PX,
    )
    return queryRenderedFeatures(touchArea, StopLayer.LAYER_ID)
        .firstOrNull()
        ?.getStringProperty(StopLayer.PROP_STOP_ID)
}

private const val TAP_TOLERANCE_PX = 12f

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
