package hr.zet.transit.domain.repository

import hr.zet.transit.domain.model.Arrival
import hr.zet.transit.domain.model.JourneyPlan
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.RouteSchedule
import hr.zet.transit.domain.model.RouteShape
import hr.zet.transit.domain.model.Routine
import hr.zet.transit.domain.model.RoutineKind
import hr.zet.transit.domain.model.ServiceAlert
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.model.Vehicle
import hr.zet.transit.domain.model.WalkRoute
import kotlinx.coroutines.flow.Flow

/**
 * Repository interfaci — domain definira ugovor, data sloj ga implementira.
 *
 * Sve realtime metode su Flow jer UI prati žive promjene; adaptivni polling
 * (sekcija 4.6 plana) događa se u data sloju, ne ovdje.
 *
 * VAŽNO (sekcija 5 plana): implementacije gađaju `transit-api` backend,
 * NIKAD ZET izravno.
 */

/** Statički GTFS — linije i stajališta. */
interface StaticRepository {
    suspend fun getRoutes(): List<Route>
    suspend fun getRoute(routeId: String): Route?
    suspend fun getStops(): List<Stop>
    suspend fun getStop(stopId: String): Stop?
    /** Stajališta unutar zadanog radijusa (metri) od točke. */
    suspend fun getStopsNear(lat: Double, lng: Double, radiusMeters: Int): List<Stop>

    /** Geometrija rute za prikaz na karti (A1.2); null ako linija nema shape. */
    suspend fun getRouteShape(routeId: String): RouteShape?

    /** Kompletan statički vozni red linije (A1.4); null ako ga nema. */
    suspend fun getRouteSchedule(routeId: String): RouteSchedule?

    /** Pješačka ruta od točke do točke (A1.6); null ako put nije nađen. */
    suspend fun getWalkRoute(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
    ): WalkRoute?
}

/**
 * Realtime sloj — opcionalni sloj nad statičkim GTFS-om.
 * Ako RT feed padne (R1), implementacija degradira na statički vozni red.
 */
interface RealtimeRepository {
    /** Žive pozicije vozila. Adaptivni polling iza Flowa. */
    fun observeVehicles(): Flow<List<Vehicle>>

    /** Dolasci na stajalište — RT predikcije ili statički fallback. */
    fun observeArrivals(stopId: String): Flow<List<Arrival>>

    /** Aktivni service alerts. */
    fun observeAlerts(): Flow<List<ServiceAlert>>
}

/** Omiljena stajališta — lokalno, bez backenda (A0.3 plana). */
interface FavoritesRepository {
    fun observeFavoriteStopIds(): Flow<Set<String>>
    suspend fun addFavorite(stopId: String)
    suspend fun removeFavorite(stopId: String)
    suspend fun isFavorite(stopId: String): Boolean
}

/** Rutine Dom/Posao/Škola — lokalno, bez backenda (A1.5 plana). */
interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>
    suspend fun setRoutine(routine: Routine)
    suspend fun clearRoutine(kind: RoutineKind)
}

/** Planiranje rute A→B (A2.1) — backend GraphHopper pt. */
interface JourneyRepository {
    /**
     * Ponuđene varijante putovanja od izvora do odredišta.
     * @return lista planova; prazna ako ruta nije nađena ili je planiranje
     *   nedostupno (GraphHopper nije konfiguriran).
     */
    suspend fun planJourney(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
    ): List<JourneyPlan>
}
