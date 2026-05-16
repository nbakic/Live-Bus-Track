package hr.zet.transit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Design tokeni — placeholder paleta. Finalni tokeni dolaze iz Figma
 * design procesa u Fazi 0 (sekcija 6 plana): "lijep dizajn ne smije biti
 * slučajan ishod Material 3 defaulta".
 *
 * ZET crvena (#E30613) kao bazni brand seed dok ne stigne prava paleta.
 */
private val ZetRed = Color(0xFFE30613)

private val LightColors = lightColorScheme(
    primary = ZetRed,
)

private val DarkColors = darkColorScheme(
    primary = ZetRed,
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
