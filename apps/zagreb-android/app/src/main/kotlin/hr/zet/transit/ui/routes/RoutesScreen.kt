package hr.zet.transit.ui.routes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.TransitMode
import hr.zet.transit.ui.common.LoadError
import org.koin.androidx.compose.koinViewModel

/**
 * Pregled svih linija (A1.1) — lista tramvaja i autobusa.
 *
 * Svaki redak: oznaka linije u obojenom čipu + smjer/naziv. Boja čipa iz
 * GTFS `route_color`, fallback po modu (tramvaj/autobus).
 */
@Composable
fun RoutesScreen(
    onRouteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error == LoadError.NO_INTERNET -> EmptyState(
                icon = Icons.Filled.WifiOff,
                title = "Nema internetske veze",
                subtitle = "Izgleda da nisi povezan na internet. Provjeri vezu pa pokušaj ponovno.",
                onRetry = viewModel::retry,
            )

            state.error == LoadError.SERVER -> EmptyState(
                icon = Icons.Filled.CloudOff,
                title = "Poslužitelj nije dostupan",
                subtitle = "Naši poslužitelji trenutno ne odgovaraju. Pokušaj ponovno za koji trenutak.",
                onRetry = viewModel::retry,
            )

            state.routes.isEmpty() -> EmptyState(
                icon = Icons.Filled.DirectionsBus,
                title = "Nema linija za prikaz",
                subtitle = "Trenutno nema dostupnih linija.",
                onRetry = viewModel::retry,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(state.routes, key = Route::id) { route ->
                    RouteRow(route, onClick = { onRouteClick(route.id) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/** Ujednačeno prazno/error stanje — ikona + naslov + pojašnjenje + retry. */
@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(onClick = onRetry) { Text("Pokušaj ponovno") }
        }
    }
}

@Composable
private fun RouteRow(
    route: Route,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RouteBadge(route)
        Text(
            text = route.longName.ifBlank { route.shortName },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun RouteBadge(route: Route, modifier: Modifier = Modifier) {
    val badgeColor = route.color?.let(::parseHexColor)
        ?: if (route.mode == TransitMode.TRAM) Color(0xFF0066CC) else Color(0xFFFF8C00)

    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.size(width = 48.dp, height = 32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = route.shortName,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Parsira "#RRGGBB" u Compose Color; null pri neispravnom formatu. */
private fun parseHexColor(hex: String): Color? = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrNull()
