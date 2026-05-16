package hr.zet.transit.data.gtfs

import hr.zet.transit.data.local.db.TransitDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Preuzima GTFS static ZIP, provjerava svježinu i atomarno učitava u bazu.
 *
 * Mehanizam svježine (sekcija 2 plana):
 *  1. preuzmi ZIP s backenda (`transit-api`, nikad ZET izravno),
 *  2. **primarno: SHA-256** ZIP-a — ako je isti kao zadnji import, preskoči,
 *  3. parsiraj `routes.txt`/`stops.txt`, potvrdi `feed_version` iz `feed_info.txt`,
 *  4. atomarni staging-swap: brisanje + insert u jednoj transakciji.
 */
class GtfsImporter(
    private val httpClient: HttpClient,
    private val db: TransitDatabase,
    private val zipReader: GtfsZipReader = GtfsZipReader(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /** Ishod importa — UI/WorkManager logiraju koji je slučaj nastupio. */
    sealed interface Result {
        /** ZIP nepromijenjen (SHA-256 isti) — baza ostaje kakva jest. */
        data object UpToDate : Result
        /** Novi GTFS uspješno učitan. */
        data class Imported(val routeCount: Int, val stopCount: Int) : Result
        /** Import nije uspio — baza ostaje na zadnjem ispravnom stanju (R2). */
        data class Failed(val cause: Throwable) : Result
    }

    /**
     * Sinkronizira GTFS static iz danog URL-a (backend `/v1` GTFS endpoint).
     * @return ishod; pri grešci baza ostaje netaknuta (fallback na zadnji ZIP).
     */
    suspend fun sync(gtfsZipUrl: String): Result = withContext(ioDispatcher) {
        runCatching {
            val zipBytes = httpClient.get(gtfsZipUrl).readBytes()
            val sha = zipReader.sha256(zipBytes)

            val current = db.transitQueries.selectFeedMeta().executeAsOneOrNull()
            if (current?.sha256 == sha) return@runCatching Result.UpToDate

            val files = zipReader.readTextEntries(
                zipBytes,
                setOf("routes.txt", "stops.txt", "feed_info.txt"),
            )
            val routes = GtfsStaticParser.parseRoutes(files["routes.txt"].orEmpty())
            val stops = GtfsStaticParser.parseStops(files["stops.txt"].orEmpty())
            val feedVersion = files["feed_info.txt"]
                ?.let(GtfsStaticParser::parseFeedVersion)

            require(routes.isNotEmpty() && stops.isNotEmpty()) {
                "GTFS ZIP ne sadrži valjane linije/stajališta — import odbijen."
            }

            // Atomarni staging-swap: sve u jednoj transakciji.
            db.transitQueries.transaction {
                db.transitQueries.deleteAllRoutes()
                routes.forEach {
                    db.transitQueries.insertRoute(
                        it.id, it.shortName, it.longName, it.mode.name, it.color,
                    )
                }
                db.transitQueries.deleteAllStops()
                stops.forEach {
                    db.transitQueries.insertStop(
                        it.id, it.name, it.position.lat, it.position.lng,
                    )
                }
                db.transitQueries.upsertFeedMeta(sha, feedVersion, nowEpochSeconds())
            }
            Result.Imported(routeCount = routes.size, stopCount = stops.size)
        }.getOrElse { Result.Failed(it) }
    }
}
