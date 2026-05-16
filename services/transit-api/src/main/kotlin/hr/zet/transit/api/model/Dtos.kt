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
