package hr.zet.transit.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Ujednačeno prazno/error stanje — ikona u mekom tonalnom krugu + naslov +
 * pojašnjenje + opcionalni "Pokušaj ponovno". Dijele ga svi ekrani da poruke
 * i izgled budu dosljedni.
 */
@Composable
fun EmptyState(
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
        // Ikona unutar mekog kruga — daje praznom stanju namjeran, dizajniran
        // izgled umjesto "gole" ikone na bjelini.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            // Jedina akcija na error ekranu — zaslužuje naglašen, brendiran CTA.
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text("Pokušaj ponovno")
            }
        }
    }
}

/**
 * Prazno stanje za neuspjelo učitavanje — jedinstveni izvor poruka koje
 * korisniku jasno kažu je li problem do njegove veze ili do našeg poslužitelja.
 */
@Composable
fun LoadErrorState(
    error: LoadError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) = when (error) {
    LoadError.NO_INTERNET -> EmptyState(
        icon = Icons.Filled.WifiOff,
        title = "Nema internetske veze",
        subtitle = "Izgleda da nisi povezan na internet. Provjeri vezu pa pokušaj ponovno.",
        modifier = modifier,
        onRetry = onRetry,
    )

    LoadError.SERVER -> EmptyState(
        icon = Icons.Filled.CloudOff,
        title = "Poslužitelj nije dostupan",
        subtitle = "Naši poslužitelji trenutno ne odgovaraju. Pokušaj ponovno za koji trenutak.",
        modifier = modifier,
        onRetry = onRetry,
    )
}

/** Kratka poruka (za trake/pilule) — bez ikone i gumba. */
fun LoadError.shortMessage(): String = when (this) {
    LoadError.NO_INTERNET -> "Nema internetske veze"
    LoadError.SERVER -> "Poslužitelj nije dostupan"
}
