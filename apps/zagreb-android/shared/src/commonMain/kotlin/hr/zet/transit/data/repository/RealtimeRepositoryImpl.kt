package hr.zet.transit.data.repository

import hr.zet.transit.data.remote.TransitApiClient
import hr.zet.transit.data.remote.dto.ArrivalDto
import hr.zet.transit.data.remote.dto.ServiceAlertDto
import hr.zet.transit.data.remote.dto.VehicleDto
import hr.zet.transit.domain.model.Arrival
import hr.zet.transit.domain.model.AlertSeverity
import hr.zet.transit.domain.model.Heading
import hr.zet.transit.domain.model.LatLng
import hr.zet.transit.domain.model.RealtimeFeed
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.ServiceAlert
import hr.zet.transit.domain.model.TransitMode
import hr.zet.transit.domain.model.Vehicle
import hr.zet.transit.domain.repository.RealtimeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * RealtimeRepository nad `transit-api` backendom.
 *
 * Polling je trenutno fiksan ([PollConfig.DEFAULT]); adaptivni polling vezan
 * na zoom level / vidljivost (sekcija 4.6 plana) dodaje se u A0 kad UI sloj
 * može javiti zoom kontekst. Exponential backoff na greške je već ovdje.
 */
class RealtimeRepositoryImpl(
    private val api: TransitApiClient,
) : RealtimeRepository {

    override fun observeVehicles(): Flow<RealtimeFeed<Vehicle>> =
        pollingFlow { api.getVehicles().data.map { it.toDomain() } }

    override fun observeArrivals(stopId: String): Flow<RealtimeFeed<Arrival>> =
        pollingFlow { api.getArrivals(stopId).data.map { it.toDomain() } }

    override fun observeAlerts(): Flow<RealtimeFeed<ServiceAlert>> =
        pollingFlow { api.getAlerts().data.map { it.toDomain() } }

    /**
     * Generički polling s exponential backoffom. Emita uvijek — i pri grešci
     * šalje prethodni snapshot s `isLive=false` (ili praznu listu pri prvom
     * padu) da UI može pokazati degradirano stanje umjesto da viseći čeka.
     */
    private fun <T> pollingFlow(fetch: suspend () -> List<T>): Flow<RealtimeFeed<T>> = flow {
        var backoffMs = PollConfig.MIN_BACKOFF_MS
        var lastData: List<T> = emptyList()
        while (true) {
            try {
                val data = fetch()
                lastData = data
                emit(RealtimeFeed(data = data, isLive = true))
                backoffMs = PollConfig.MIN_BACKOFF_MS
                delay(PollConfig.DEFAULT_INTERVAL_MS)
            } catch (e: Exception) {
                // R1 graceful degradation: greška ne ruši Flow. UI vidi
                // `isLive=false` i pokazuje "podaci uživo nedostupni".
                emit(RealtimeFeed(data = lastData, isLive = false))
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(PollConfig.MAX_BACKOFF_MS)
            }
        }
    }

    object PollConfig {
        const val DEFAULT_INTERVAL_MS = 10_000L
        const val MIN_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 60_000L
    }
}

// --- DTO → domain mapiranje ---

internal fun parseMode(raw: String): TransitMode =
    if (raw.equals("TRAM", ignoreCase = true)) TransitMode.TRAM else TransitMode.BUS

private fun VehicleDto.toDomain() = Vehicle(
    id = id,
    routeId = routeId,
    mode = parseMode(mode),
    position = LatLng(lat, lng),
    heading = bearing?.let { Heading(it) },
    timestamp = timestamp,
)

private fun ArrivalDto.toDomain() = Arrival(
    routeId = routeId,
    routeShortName = routeShortName,
    mode = parseMode(mode),
    headsign = headsign,
    predictedTime = predictedTime,
    delaySeconds = delaySeconds,
    isRealtime = isRealtime,
)

private fun ServiceAlertDto.toDomain() = ServiceAlert(
    id = id,
    headerText = headerText,
    descriptionText = descriptionText,
    affectedRouteIds = affectedRouteIds,
    severity = when (severity.uppercase()) {
        "SEVERE" -> AlertSeverity.SEVERE
        "WARNING" -> AlertSeverity.WARNING
        else -> AlertSeverity.INFO
    },
)
