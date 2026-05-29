package hr.zet.transit.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.graphics.RectF
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.maps.MapLibreMap

/**
 * Karta vozila (A0.1) — MapLibre karta sa živim slojem vozila i statičkim
 * slojem stajališta. Tap na stajalište navigira na stajalište-detalje.
 *
 * Overlay: plutajuća tražilica na vrhu + horizontalno klizni redak akcijskih
 * čipova (Google Maps / Citymapper obrazac), te diskretan "live" indikator.
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

        TopControls(
            onSearchClick = onSearchClick,
            onRoutesClick = onRoutesClick,
            onNearbyClick = onNearbyClick,
            onPlanClick = onPlanClick,
            onRoutinesClick = onRoutinesClick,
            onFavoritesClick = onFavoritesClick,
            onAlertsClick = onAlertsClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        LiveStatusPill(
            vehicleCount = state.vehicles.size,
            isLive = state.isLive,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun TopControls(
    onSearchClick: () -> Unit,
    onRoutesClick: () -> Unit,
    onNearbyClick: () -> Unit,
    onPlanClick: () -> Unit,
    onRoutinesClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onAlertsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        Action("Linije", Icons.Filled.DirectionsBus, onRoutesClick),
        Action("U blizini", Icons.Filled.NearMe, onNearbyClick),
        Action("Ruta", Icons.Filled.Route, onPlanClick),
        Action("Rutine", Icons.Filled.Schedule, onRoutinesClick),
        Action("Omiljeni", Icons.Filled.Star, onFavoritesClick),
        Action("Obavijesti", Icons.Filled.Notifications, onAlertsClick),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchBarPill(onClick = onSearchClick)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { action ->
                ElevatedAssistChip(
                    onClick = action.onClick,
                    label = { Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

private data class Action(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Plutajuća tražilica — vodeća ulazna točka, vodi na pretragu. */
@Composable
private fun SearchBarPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Pretraži stanice i linije" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Pretraži stanice i linije",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Diskretan "live" indikator — broj vozila uživo ili degradacija kad RT padne. */
@Composable
private fun LiveStatusPill(
    vehicleCount: Int,
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isLive) LiveGreen else MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isLive) "$vehicleCount vozila uživo" else "Nema podataka uživo",
                style = MaterialTheme.typography.labelLarge,
                color = if (isLive) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

private val LiveGreen = Color(0xFF2E7D32)

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
