package hr.zet.transit.api.feed

import com.google.transit.realtime.GtfsRealtime.FeedMessage
import hr.zet.transit.api.Config
import hr.zet.transit.api.cache.CachedValue
import hr.zet.transit.api.cache.TtlCache
import hr.zet.transit.api.model.ArrivalDto
import hr.zet.transit.api.model.ServiceAlertDto
import hr.zet.transit.api.model.VehicleDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes

/**
 * Dohvaća i parsira GTFS-RT protobuf sa ZET-a. Jedina ZET-okrenuta točka
 * (sekcija 5 plana). Protobuf parsiranje radi backend; klijent dobiva JSON.
 *
 * Sva tri feeda dijele isti protobuf endpoint, pa jedan TTL cache nad
 * sirovom FeedMessage; svaki getter projicira svoj dio. Tako se ZET gađa
 * jednom po TTL prozoru bez obzira koliko endpointa klijent zove.
 */
class GtfsRtFeedService(
    private val httpClient: HttpClient,
    /**
     * Daje GTFS static lookup (mode, ime linije, headsign) za obogaćivanje
     * RT feeda — C5. Vrijednost se dohvaća lijeno; ako static još nije
     * učitan, fallback je `GtfsLookup.EMPTY` (vozila/dolasci i dalje rade).
     */
    private val lookupProvider: suspend () -> GtfsLookup = { GtfsLookup.EMPTY },
) {
    private val feedCache = TtlCache(
        ttlMillis = Config.rtCacheTtlSeconds * 1000,
        loader = ::fetchFeed,
    )

    private suspend fun fetchFeed(): FeedMessage {
        val bytes = httpClient.get(Config.zetGtfsRtUrl).readBytes()
        return FeedMessage.parseFrom(bytes)
    }

    private suspend fun lookup(): GtfsLookup =
        runCatching { lookupProvider() }.getOrDefault(GtfsLookup.EMPTY)

    suspend fun vehicles(): CachedValue<List<VehicleDto>> {
        val lookup = lookup()
        return feedCache.get().map { feed ->
            feed.entityList
                .filter { it.hasVehicle() }
                .map { it.vehicle.toDto(lookup) }
        }
    }

    suspend fun arrivals(stopId: String): CachedValue<List<ArrivalDto>> {
        val lookup = lookup()
        return feedCache.get().map { feed ->
            feed.entityList
                .filter { it.hasTripUpdate() }
                .flatMap { entity -> entity.tripUpdate.toArrivalDtos(stopId, lookup) }
        }
    }

    suspend fun alerts(): CachedValue<List<ServiceAlertDto>> =
        feedCache.get().map { feed ->
            feed.entityList
                .filter { it.hasAlert() }
                .map { entity -> entity.alert.toDto(entity.id) }
        }
}

/** Projicira cached vrijednost kroz transformaciju, čuva meta o svježini. */
private fun <T, R> CachedValue<T>.map(transform: (T) -> R): CachedValue<R> =
    CachedValue(value = transform(value), fetchedAtEpochSeconds = fetchedAtEpochSeconds)
