package hr.zet.transit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Design tokeni za ZET transit app.
 *
 * Koherentna Material 3 paleta izvedena iz ZET crvene (#E30613) kao brand
 * seeda, s neutralnim (ne-lavanda) površinama i plavim akcentom za tranzit.
 * Cilj (sekcija 6 plana): "lijep dizajn ne smije biti slučajan ishod
 * Material 3 defaulta" — zato je definiran pun set tokena, ne samo `primary`.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFFC5161D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF775653),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF1A5FB4),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E3FF),
    onTertiaryContainer = Color(0xFF001B3D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7E0DF),
    onSurfaceVariant = Color(0xFF52443F),
    outline = Color(0xFF85736F),
    outlineVariant = Color(0xFFD8C2BD),
    surfaceTint = Color(0xFFC5161D),
    inverseSurface = Color(0xFF2F3133),
    inverseOnSurface = Color(0xFFF1F0F3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3AD),
    onPrimary = Color(0xFF680010),
    primaryContainer = Color(0xFF930017),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFA9C7FF),
    onTertiary = Color(0xFF00305F),
    tertiaryContainer = Color(0xFF004788),
    onTertiaryContainer = Color(0xFFD6E3FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E5),
    surfaceVariant = Color(0xFF52443F),
    onSurfaceVariant = Color(0xFFD8C2BD),
    outline = Color(0xFFA08C88),
    outlineVariant = Color(0xFF52443F),
    surfaceTint = Color(0xFFFFB3AD),
    inverseSurface = Color(0xFFE3E2E5),
    inverseOnSurface = Color(0xFF2F3133),
)

@Composable
fun ZetTransitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
