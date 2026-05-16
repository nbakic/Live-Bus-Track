package hr.zet.transit.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTO-i za `transit-api` backend (`/v1/...` ugovor — sekcija 5 plana).
 * Backend serijalizira GTFS-RT protobuf u JSON; klijent nikad ne parsira
 * protobuf izravno (to radi backend). Mapiranje DTO → domain u repozitorijima.
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
data class LatLngDto(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class RouteShapeDto(
    val routeId: String,
    val points: List<LatLngDto>,
)

@Serializable
data class RouteScheduleDto(
    val routeId: String,
    val directions: List<DirectionScheduleDto>,
)

@Serializable
data class DirectionScheduleDto(
    val headsign: String,
    val departures: List<String>,
)

/** Omotač odgovora — backend dodaje meta polja (svježina, izvor). */
@Serializable
data class FeedResponse<T>(
    val data: T,
    /** Unix sekunde — kad je backend zadnji put dohvatio podatke od ZET-a. */
    val fetchedAt: Long,
    /** false = backend servira degradiran/cached odgovor (RT feed nedostupan). */
    val live: Boolean = true,
)
