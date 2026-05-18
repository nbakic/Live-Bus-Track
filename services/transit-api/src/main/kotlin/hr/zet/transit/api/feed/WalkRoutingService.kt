package hr.zet.transit.api.feed

import hr.zet.transit.api.Config
import hr.zet.transit.api.model.LatLngDto
import hr.zet.transit.api.model.WalkRouteDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pješački routing (A1.6 plana) preko OSRM `foot` profila.
 *
 * Plan izričito kaže: A1.6 koristi lagani pješački graf (OSRM foot), NE puni
 * transit engine (OTP) — puni engine bi podigao operativni trošak na razinu
 * A2. Ovaj servis je samo proxy nad OSRM-om; backend ne hosta vlastiti graf.
 */
class WalkRoutingService(
    private val httpClient: HttpClient,
) {
    /**
     * Pješačka ruta od izvora do odredišta.
     * @return ruta s udaljenošću, trajanjem i geometrijom; null ako OSRM
     *   ne nađe put (npr. točka izvan grafa).
     */
    suspend fun walkRoute(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
    ): WalkRouteDto? {
        // OSRM očekuje "lng,lat;lng,lat" u putanji.
        val coords = "$fromLng,$fromLat;$toLng,$toLat"
        val raw: OsrmResponse = httpClient.get("${Config.osrmFootUrl}/$coords") {
            parameter("overview", "full")
            parameter("geometries", "geojson")
        }.body()

        if (raw.code != "Ok") return null
        val route = raw.routes.firstOrNull() ?: return null

        return WalkRouteDto(
            distanceMeters = route.distance,
            durationSeconds = route.duration,
            geometry = route.geometry.coordinates.map { it.toLatLng() },
        )
    }

    @Serializable
    private data class OsrmResponse(
        val code: String,
        val routes: List<OsrmRoute> = emptyList(),
    )

    @Serializable
    private data class OsrmRoute(
        val distance: Double,
        val duration: Double,
        val geometry: OsrmGeometry,
    )

    /** GeoJSON LineString — coordinates su [lng, lat] parovi. */
    @Serializable
    private data class OsrmGeometry(
        @SerialName("coordinates")
        val coordinates: List<List<Double>> = emptyList(),
    )

    /** OSRM coordinate [lng, lat] → naš LatLng DTO. */
    private fun List<Double>.toLatLng(): LatLngDto =
        LatLngDto(lat = this[1], lng = this[0])
}
