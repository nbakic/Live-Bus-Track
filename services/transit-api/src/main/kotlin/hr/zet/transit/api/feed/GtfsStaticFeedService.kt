package hr.zet.transit.api.feed

import hr.zet.transit.api.Config
import hr.zet.transit.api.cache.CachedValue
import hr.zet.transit.api.cache.TtlCache
import hr.zet.transit.api.model.RouteDto
import hr.zet.transit.api.model.StopDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import java.util.zip.ZipInputStream

/**
 * Dohvaća i parsira ZET GTFS static ZIP, servira linije/stajališta kao JSON.
 *
 * Static GTFS se mijenja rijetko (dani/tjedni), pa cache TTL je dug — za
 * razliku od RT feeda. Klijent može birati: JSON preko ovih endpointa ili
 * sirovi ZIP preko `GtfsImporter`-a (sekcija 2 plana). JSON je lakši za
 * povremeni dohvat, ZIP za puni offline import.
 */
class GtfsStaticFeedService(
    private val httpClient: HttpClient,
) {
    /** Cijeli GTFS — sirovi ZIP + parsirane projekcije, dijeli jedan dohvat. */
    private class StaticData(
        val zipBytes: ByteArray,
        val routes: List<RouteDto>,
        val stops: List<StopDto>,
    )

    private val cache = TtlCache(
        ttlMillis = STATIC_CACHE_TTL_MILLIS,
        loader = ::fetchAndParse,
    )

    private suspend fun fetchAndParse(): StaticData {
        val zipBytes = httpClient.get(Config.zetGtfsStaticUrl).readBytes()
        val files = readZipEntries(zipBytes, setOf("routes.txt", "stops.txt"))
        return StaticData(
            zipBytes = zipBytes,
            routes = parseRoutes(files["routes.txt"].orEmpty()),
            stops = parseStops(files["stops.txt"].orEmpty()),
        )
    }

    suspend fun routes(): CachedValue<List<RouteDto>> =
        cache.get().let { CachedValue(it.value.routes, it.fetchedAtEpochSeconds) }

    suspend fun stops(): CachedValue<List<StopDto>> =
        cache.get().let { CachedValue(it.value.stops, it.fetchedAtEpochSeconds) }

    /** Sirovi GTFS ZIP — klijent ga importira preko `GtfsImporter`-a. */
    suspend fun rawZip(): ByteArray = cache.get().value.zipBytes

    private fun readZipEntries(zipBytes: ByteArray, names: Set<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name in names) result[entry.name] = zip.readBytes().decodeToString()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }

    /** `route_type`: 0 = tram, 3 = bus (GTFS spec). Defenzivno (R2). */
    private fun parseRoutes(csv: String): List<RouteDto> =
        CsvParser.parse(csv).mapNotNull { row ->
            val id = row["route_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            RouteDto(
                id = id,
                shortName = row["route_short_name"].orEmpty(),
                longName = row["route_long_name"].orEmpty(),
                mode = if (row["route_type"]?.trim() == "0") "TRAM" else "BUS",
                color = row["route_color"]
                    ?.takeIf(String::isNotBlank)
                    ?.let { if (it.startsWith("#")) it else "#$it" },
            )
        }

    private fun parseStops(csv: String): List<StopDto> =
        CsvParser.parse(csv).mapNotNull { row ->
            val id = row["stop_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val lat = row["stop_lat"]?.toDoubleOrNull() ?: return@mapNotNull null
            val lng = row["stop_lon"]?.toDoubleOrNull() ?: return@mapNotNull null
            StopDto(id = id, name = row["stop_name"].orEmpty(), lat = lat, lng = lng)
        }

    private companion object {
        /** Static GTFS se mijenja rijetko — 6 h TTL je dovoljno čest. */
        const val STATIC_CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L
    }
}
