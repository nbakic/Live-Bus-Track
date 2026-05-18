package hr.zet.transit.api.feed

import hr.zet.transit.api.Config
import hr.zet.transit.api.model.JourneyLegDto
import hr.zet.transit.api.model.JourneyPlanDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Planiranje rute A→B (A2.1 plana) preko GraphHopper Routing API-ja.
 *
 * Plan (sekcija 3.2): GraphHopper je izabran kao transit engine — lakši za
 * hosting od OTP-a. Koristi se hostani GraphHopper API s `pt` profilom; ovaj
 * servis je proxy koji GraphHopper odgovor prevodi u naš `/v1` ugovor.
 *
 * GraphHopper rješava presjedanja i vremensku optimizaciju — ono što
 * jednostavni plan iz GTFS-a ne bi mogao (zato je odabran pravi engine).
 */
class JourneyPlanningService(
    private val httpClient: HttpClient,
) {
    /** true ako je GraphHopper konfiguriran (API ključ postavljen). */
    val isConfigured: Boolean get() = Config.graphHopperApiKey.isNotBlank()

    /**
     * Planira putovanja A→B s polaskom u zadano vrijeme.
     * @return lista ponuđenih varijanti; prazna ako ruta nije nađena.
     * @throws IllegalStateException ako GraphHopper nije konfiguriran.
     */
    suspend fun plan(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
        departureEpochSeconds: Long,
    ): List<JourneyPlanDto> {
        check(isConfigured) { "GraphHopper API ključ nije postavljen." }

        val departureIso = DateTimeFormatter.ISO_INSTANT
            .format(Instant.ofEpochSecond(departureEpochSeconds))

        val response: GhResponse = httpClient.get(Config.graphHopperUrl) {
            parameter("point", "$fromLat,$fromLng")
            parameter("point", "$toLat,$toLng")
            parameter("profile", "pt")
            parameter("pt.earliest_departure_time", departureIso)
            parameter("locale", "hr")
            parameter("key", Config.graphHopperApiKey)
        }.body()

        return response.paths.map { it.toJourneyPlan() }
    }

    // --- GraphHopper odgovor → /v1 DTO ---

    private fun GhPath.toJourneyPlan(): JourneyPlanDto {
        val mappedLegs = legs.map { it.toLeg() }
        return JourneyPlanDto(
            totalDurationSeconds = time / 1000,
            departureTime = mappedLegs.firstOrNull()?.departureTime ?: 0,
            arrivalTime = mappedLegs.lastOrNull()?.arrivalTime ?: 0,
            legs = mappedLegs,
        )
    }

    private fun GhLeg.toLeg(): JourneyLegDto {
        val isTransit = type.equals("pt", ignoreCase = true)
        return JourneyLegDto(
            type = if (isTransit) "TRANSIT" else "WALK",
            fromName = departureLocation ?: "",
            toName = arrivalLocation ?: "",
            departureTime = parseIsoToEpoch(departureTime),
            arrivalTime = parseIsoToEpoch(arrivalTime),
            routeName = if (isTransit) routeId else null,
            headsign = if (isTransit) tripHeadsign else null,
        )
    }

    /** ISO-8601 vrijeme iz GraphHoppera → Unix sekunde; 0 pri grešci. */
    private fun parseIsoToEpoch(iso: String?): Long =
        iso?.let { runCatching { OffsetDateTime.parse(it).toEpochSecond() }.getOrNull() } ?: 0

    @Serializable
    private data class GhResponse(
        val paths: List<GhPath> = emptyList(),
    )

    @Serializable
    private data class GhPath(
        /** Ukupno trajanje u milisekundama. */
        val time: Long = 0,
        val legs: List<GhLeg> = emptyList(),
    )

    @Serializable
    private data class GhLeg(
        val type: String = "",
        @SerialName("departure_location")
        val departureLocation: String? = null,
        @SerialName("arrival_location")
        val arrivalLocation: String? = null,
        @SerialName("departure_time")
        val departureTime: String? = null,
        @SerialName("arrival_time")
        val arrivalTime: String? = null,
        @SerialName("route_id")
        val routeId: String? = null,
        @SerialName("trip_headsign")
        val tripHeadsign: String? = null,
    )
}
