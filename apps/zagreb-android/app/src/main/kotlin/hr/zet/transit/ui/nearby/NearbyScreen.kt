package hr.zet.transit.ui.nearby

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.ui.common.EmptyState
import hr.zet.transit.ui.common.LoadError
import hr.zet.transit.ui.common.LoadErrorState
import hr.zet.transit.ui.common.LoadingState
import org.koin.androidx.compose.koinViewModel

/**
 * "Najbliža stanica" (A0.5) — stajališta blizu korisnika.
 *
 * Lokacijska dozvola se traži pri otvaranju (foreground-only opt-in, R5).
 * Udaljenost je zračna; UI ne obećava pješački put — to dolazi u A1.6.
 */
@Composable
fun NearbyScreen(
    onStopClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NearbyViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.loadNearby() else viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Najbliža stajališta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        when (state.status) {
            NearbyStatus.LOADING -> LoadingState(label = "Tražim stajališta u blizini…")

            NearbyStatus.PERMISSION_DENIED -> EmptyState(
                icon = Icons.Filled.LocationOff,
                title = "Pristup lokaciji je isključen",
                subtitle = "Za prikaz najbližih stajališta dopusti aplikaciji pristup lokaciji.",
                onRetry = {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
            )

            NearbyStatus.NO_LOCATION -> EmptyState(
                icon = Icons.Filled.LocationSearching,
                title = "Lokacija nije dostupna",
                subtitle = "Uključi GPS i provjeri da imaš signal, pa pokušaj ponovno.",
                onRetry = viewModel::loadNearby,
            )

            // Lokalna baza prazna i ne možemo je napuniti — razlikuj korisnikovu
            // vezu od našeg poslužitelja (C8), umjesto lažne "nema stajališta".
            NearbyStatus.LOAD_ERROR -> LoadErrorState(
                error = state.error ?: LoadError.SERVER,
                onRetry = viewModel::loadNearby,
            )

            NearbyStatus.READY -> {
                if (state.stops.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Place,
                        title = "Nema stajališta u blizini",
                        subtitle = "U tvojoj blizini nema poznatih ZET stajališta.",
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(state.stops, key = { it.stop.id }) { nearby ->
                            NearbyRow(nearby, onClick = { onStopClick(nearby.stop.id) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyRow(nearby: NearbyStop, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = nearby.stop.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 12.dp),
        )
        // Pješačka udaljenost (A1.6); "—" kad routing nije uspio.
        Text(
            text = nearby.walkMinutes?.let { min ->
                "${min} min hoda · ${nearby.walkMeters} m"
            } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
