package hr.zet.transit.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hr.zet.transit.domain.model.TransitMode

/**
 * Obojeni čip s oznakom linije — jedinstven prikaz broja linije kroz cijelu
 * aplikaciju (Linije, pretraga, dolasci). Boja iz GTFS `route_color`, a kad
 * je ona neupotrebljiva (ZET šalje `#ffffff` za sve linije → bijelo na bijelom),
 * pada na konvencionalnu boju po modu: tramvaj plav, autobus narančast.
 *
 * Boja teksta se bira po luminanciji pozadine da oznaka uvijek bude čitka.
 */
@Composable
fun RouteBadge(
    shortName: String,
    mode: TransitMode,
    colorHex: String?,
    modifier: Modifier = Modifier,
) {
    val background = resolveBadgeColor(colorHex, mode)
    val content = if (background.luminance() > 0.6f) Color(0xFF1A1C1E) else Color.White

    Surface(
        color = background,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.sizeIn(minWidth = 46.dp, minHeight = 30.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = shortName,
                color = content,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

/**
 * Razrješava boju oznake: koristi GTFS boju samo ako je dovoljno tamna da na
 * njoj bude čitka (odbacuje bijelu/gotovo bijelu); inače boja po modu.
 */
private fun resolveBadgeColor(colorHex: String?, mode: TransitMode): Color {
    val parsed = colorHex
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    if (parsed != null && parsed.luminance() < 0.85f) return parsed
    return when (mode) {
        TransitMode.TRAM -> Color(0xFF0F62B0) // ZET tramvaj — plava
        TransitMode.BUS -> Color(0xFFE06C00)  // ZET autobus — narančasta
    }
}
