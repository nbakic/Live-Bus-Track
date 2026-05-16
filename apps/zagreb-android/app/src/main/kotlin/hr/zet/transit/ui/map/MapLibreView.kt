package hr.zet.transit.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * MapLibre karta kao Compose komponenta.
 *
 * MapLibre Android SDK je View-based, pa ga omatamo u [AndroidView] i ručno
 * prosljeđujemo lifecycle evente — `MapView` ih treba ili curi resurse.
 *
 * Stil pločica: zasad MapLibre demo stil. Plaćeni tile provider i vlastiti
 * vizualni stil odlučuju se u Fazi 0 (plan sekcija 4.5 / rizik R3).
 */
@Composable
fun MapLibreView(
    modifier: Modifier = Modifier,
    /** Poziva se kad su karta i stil spremni — sloj vozila se tu attacha. */
    onMapReady: (MapLibreMap, Style) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapLibre se mora inicijalizirati prije instanciranja MapViewa.
    remember { MapLibre.getInstance(context) }

    val mapView = remember { MapView(context) }

    // Most između Compose lifecyclea i MapView lifecycle metoda.
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map ->
                map.setStyle(DEMO_STYLE_URL) { style ->
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(ZAGREB_LAT, ZAGREB_LNG))
                        .zoom(ZAGREB_DEFAULT_ZOOM)
                        .build()
                    onMapReady(map, style)
                }
            }
        },
    )
}

/** Trg bana Jelačića — prirodni centar Zagreba za početni prikaz. */
private const val ZAGREB_LAT = 45.8131
private const val ZAGREB_LNG = 15.9775
private const val ZAGREB_DEFAULT_ZOOM = 12.5

/** Privremeni demo stil; zamjenjuje se vlastitim tile stilom u Fazi 0 (R3). */
private const val DEMO_STYLE_URL = "https://demotiles.maplibre.org/style.json"
