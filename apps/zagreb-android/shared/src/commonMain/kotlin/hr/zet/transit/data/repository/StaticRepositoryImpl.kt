package hr.zet.transit.data.repository

import hr.zet.transit.data.local.db.RouteEntity
import hr.zet.transit.data.local.db.StopEntity
import hr.zet.transit.data.local.db.TransitDatabase
import hr.zet.transit.data.remote.TransitApiClient
import hr.zet.transit.domain.model.DirectionSchedule
import hr.zet.transit.domain.model.LatLng
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.RouteSchedule
import hr.zet.transit.domain.model.RouteShape
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.model.TransitMode
import hr.zet.transit.domain.repository.StaticRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos

/**
 * Statički GTFS iz lokalne SQLDelight baze. Bazu puni GtfsImporter
 * iz preuzetog ZIP-a (sekcija 2 plana — GTFS static freshness).
 */
class StaticRepositoryImpl(
    private val db: TransitDatabase,
    private val api: TransitApiClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StaticRepository {

    private val queries get() = db.transitQueries

    override suspend fun getRoutes(): List<Route> = withContext(ioDispatcher) {
        queries.selectAllRoutes().executeAsList().map { it.toDomain() }
    }

    override suspend fun getRoute(routeId: String): Route? = withContext(ioDispatcher) {
        queries.selectRouteById(routeId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getStops(): List<Stop> = withContext(ioDispatcher) {
        queries.selectAllStops().executeAsList().map { it.toDomain() }
    }

    override suspend fun getStop(stopId: String): Stop? = withContext(ioDispatcher) {
        queries.selectStopById(stopId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getStopsNear(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
    ): List<Stop> = withContext(ioDispatcher) {
        // Gruba bounding-box predfiltracija u SQL-u, točan radijus u Kotlinu.
        val latDelta = radiusMeters / METERS_PER_DEGREE_LAT
        val lngDelta = radiusMeters / (METERS_PER_DEGREE_LAT * cos(lat * PI / 180.0))
        queries.selectStopsInBox(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLng = lng - lngDelta,
            maxLng = lng + lngDelta,
        ).executeAsList()
            .map { it.toDomain() }
            .filter { haversineMeters(lat, lng, it.position.lat, it.position.lng) <= radiusMeters }
            .sortedBy { haversineMeters(lat, lng, it.position.lat, it.position.lng) }
    }

    override suspend fun getRouteShape(routeId: String): RouteShape? =
        withContext(ioDispatcher) {
            // Geometrija dolazi s backenda (shapes.txt); nije u lokalnoj bazi.
            val dto = api.getRouteShape(routeId).data ?: return@withContext null
            RouteShape(
                routeId = dto.routeId,
                points = dto.points.map { LatLng(it.lat, it.lng) },
            )
        }

    override suspend fun getRouteSchedule(routeId: String): RouteSchedule? =
        withContext(ioDispatcher) {
            // Vozni red dolazi s backenda (stop_times.txt); nije u lokalnoj bazi.
            val dto = api.getRouteSchedule(routeId).data ?: return@withContext null
            RouteSchedule(
                routeId = dto.routeId,
                directions = dto.directions.map {
                    DirectionSchedule(headsign = it.headsign, departures = it.departures)
                },
            )
        }

    private companion object {
        const val METERS_PER_DEGREE_LAT = 111_320.0
    }
}

private fun RouteEntity.toDomain() = Route(
    id = id,
    shortName = shortName,
    longName = longName,
    mode = if (mode == "TRAM") TransitMode.TRAM else TransitMode.BUS,
    color = color,
)

private fun StopEntity.toDomain() = Stop(
    id = id,
    name = name,
    position = LatLng(lat, lng),
)
