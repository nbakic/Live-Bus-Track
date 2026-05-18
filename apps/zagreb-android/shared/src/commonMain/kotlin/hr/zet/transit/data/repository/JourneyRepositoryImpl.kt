package hr.zet.transit.data.repository

import hr.zet.transit.data.remote.TransitApiClient
import hr.zet.transit.data.remote.dto.JourneyLegDto
import hr.zet.transit.data.remote.dto.JourneyPlanDto
import hr.zet.transit.domain.model.JourneyLeg
import hr.zet.transit.domain.model.JourneyLegType
import hr.zet.transit.domain.model.JourneyPlan
import hr.zet.transit.domain.repository.JourneyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Planiranje rute A→B (A2.1) preko `transit-api` backenda (GraphHopper pt).
 *
 * Pri grešci ili nedostupnom planiranju vraća praznu listu — UI to prikazuje
 * kao "nema rezultata", ne kao pad.
 */
class JourneyRepositoryImpl(
    private val api: TransitApiClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : JourneyRepository {

    override suspend fun planJourney(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
    ): List<JourneyPlan> = withContext(ioDispatcher) {
        runCatching {
            api.getJourneyPlans(fromLat, fromLng, toLat, toLng).map { it.toDomain() }
        }.getOrElse { emptyList() }
    }
}

private fun JourneyPlanDto.toDomain() = JourneyPlan(
    totalDurationSeconds = totalDurationSeconds,
    departureTime = departureTime,
    arrivalTime = arrivalTime,
    legs = legs.map { it.toDomain() },
)

private fun JourneyLegDto.toDomain() = JourneyLeg(
    type = if (type.equals("TRANSIT", ignoreCase = true)) {
        JourneyLegType.TRANSIT
    } else {
        JourneyLegType.WALK
    },
    fromName = fromName,
    toName = toName,
    departureTime = departureTime,
    arrivalTime = arrivalTime,
    routeName = routeName,
    headsign = headsign,
)
