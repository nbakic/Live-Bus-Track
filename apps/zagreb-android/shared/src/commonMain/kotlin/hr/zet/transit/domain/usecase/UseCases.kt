package hr.zet.transit.domain.usecase

import hr.zet.transit.domain.model.Arrival
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.model.Vehicle
import hr.zet.transit.domain.repository.FavoritesRepository
import hr.zet.transit.domain.repository.RealtimeRepository
import hr.zet.transit.domain.repository.StaticRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-caseovi — jedna jedinica domain logike po klasi. ViewModeli ih zovu;
 * use-caseovi nikad ne znaju za Android/iOS. Testabilni bez okvira.
 *
 * Pokriva A0 funkcije; A1/A2 use-caseovi dolaze u svojim fazama.
 */

/** A0.1 — žive pozicije vozila za kartu. */
class ObserveVehiclesUseCase(
    private val realtime: RealtimeRepository,
) {
    operator fun invoke(): Flow<List<Vehicle>> = realtime.observeVehicles()
}

/** A0.2 — dolasci na stajalište (RT predikcije ili statički fallback). */
class ObserveArrivalsUseCase(
    private val realtime: RealtimeRepository,
) {
    operator fun invoke(stopId: String): Flow<List<Arrival>> =
        realtime.observeArrivals(stopId)
}

/**
 * A0.5 — najbliža stajališta. U A0 koristi zračnu udaljenost (vidi napomenu
 * uz A0.5 u sekciji 3.2 plana). Pravi pješački routing dolazi u A1.6.
 */
class GetNearbyStopsUseCase(
    private val static: StaticRepository,
) {
    suspend operator fun invoke(
        lat: Double,
        lng: Double,
        radiusMeters: Int = DEFAULT_RADIUS_METERS,
    ): List<Stop> = static.getStopsNear(lat, lng, radiusMeters)

    companion object {
        const val DEFAULT_RADIUS_METERS = 500
    }
}

/** A0.3 — toggle omiljenog stajališta. */
class ToggleFavoriteUseCase(
    private val favorites: FavoritesRepository,
) {
    suspend operator fun invoke(stopId: String) {
        if (favorites.isFavorite(stopId)) {
            favorites.removeFavorite(stopId)
        } else {
            favorites.addFavorite(stopId)
        }
    }
}
