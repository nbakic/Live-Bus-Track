package hr.zet.transit.api.model

import kotlinx.serialization.Serializable

/**
 * `/v1` JSON ugovor. MORA ostati kompatibilan sa zagreb-android
 * `data/remote/dto/Dtos.kt` — to je verzionirani klijent↔backend ugovor
 * (sekcija 5 plana). Breaking promjene idu u `/v2`, ne mijenjaju `/v1`.
 */

@Serializable
data class VehicleDto(
    val id: String,
    val routeId: String? = null,
    val mode: String,          // "TRAM" | "BUS"
    val lat: Double,
    val lng: Double,
    val bearing: Float? = null,
    val timestamp: Long,
)

@Serializable
data class ArrivalDto(
    val routeId: String,
    val routeShortName: String,
    val mode: String,
    val headsign: String,
    val predictedTime: Long,
    val delaySeconds: Int? = null,
    val isRealtime: Boolean,
)

@Serializable
data class ServiceAlertDto(
    val id: String,
    val headerText: String,
    val descriptionText: String,
    val affectedRouteIds: List<String> = emptyList(),
    val severity: String,      // "INFO" | "WARNING" | "SEVERE"
)

@Serializable
data class RouteDto(
    val id: String,
    val shortName: String,
    val longName: String,
    val mode: String,          // "TRAM" | "BUS"
    val color: String? = null,
)

@Serializable
data class StopDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
)

@Serializable
data class LatLngDto(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class RouteShapeDto(
    val routeId: String,
    val points: List<LatLngDto>,
)

/**
 * Vozni red linije (A1.4) — vremena polazaka grupirana po smjeru.
 * `departures` su "HH:MM" stringovi (GTFS dopušta i sate >24 za noćne).
 */
@Serializable
data class RouteScheduleDto(
    val routeId: String,
    val directions: List<DirectionScheduleDto>,
)

@Serializable
data class DirectionScheduleDto(
    /** Naziv odredišta smjera (GTFS trip_headsign). */
    val headsign: String,
    val departures: List<String>,
)

/**
 * Pješačka ruta (A1.6) — rezultat OSRM `foot` upita.
 * `geometry` su točke pješačkog puta za crtanje na karti.
 */
@Serializable
data class WalkRouteDto(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val geometry: List<LatLngDto>,
)

/**
 * Plan rute A→B (A2.1) — jedna ponuđena varijanta putovanja.
 * Sastoji se od dionica: pješačenje ili vožnja linijom.
 */
@Serializable
data class JourneyPlanDto(
    val totalDurationSeconds: Long,
    /** Unix sekunde — vrijeme polaska/dolaska cijelog putovanja. */
    val departureTime: Long,
    val arrivalTime: Long,
    val legs: List<JourneyLegDto>,
)

@Serializable
data class JourneyLegDto(
    /** "WALK" ili "TRANSIT". */
    val type: String,
    val fromName: String,
    val toName: String,
    val departureTime: Long,
    val arrivalTime: Long,
    /** Oznaka linije — samo za TRANSIT dionice. */
    val routeName: String? = null,
    /** Smjer/odredište linije — samo za TRANSIT dionice. */
    val headsign: String? = null,
)

@Serializable
data class FeedResponse<T>(
    val data: T,
    val fetchedAt: Long,
    val live: Boolean = true,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)
