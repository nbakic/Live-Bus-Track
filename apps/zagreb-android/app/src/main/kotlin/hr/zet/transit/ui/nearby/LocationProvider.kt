package hr.zet.transit.ui.nearby

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Dohvaća zadnju poznatu lokaciju uređaja preko Android `LocationManager`-a.
 *
 * Plan (R5): lokacija je foreground-only, eksplicitan opt-in, bez background
 * trackinga. Koristi se zadnja poznata fiksacija — dovoljno za "najbliža
 * stanica", bez kontinuiranog praćenja koje troši bateriju.
 *
 * Namjerno bez Google Play Services (FusedLocation) — `LocationManager` je u
 * SDK-u, bez dodatne ovisnosti; za A0.5 zračnu udaljenost je dovoljno točan.
 */
class LocationProvider(private val context: Context) {

    /** (lat, lng) zadnje poznate lokacije, ili null ako nije dostupna. */
    @SuppressLint("MissingPermission")
    suspend fun lastKnownLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            val manager = context.getSystemService<LocationManager>()
            if (manager == null) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            // Pozivatelj je dužan provjeriti dozvolu prije ove metode.
            val location = runCatching {
                listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                    .mapNotNull { provider ->
                        if (manager.isProviderEnabled(provider)) {
                            manager.getLastKnownLocation(provider)
                        } else {
                            null
                        }
                    }
                    .maxByOrNull { it.time }
            }.getOrNull()

            cont.resume(location?.let { it.latitude to it.longitude })
        }
}
