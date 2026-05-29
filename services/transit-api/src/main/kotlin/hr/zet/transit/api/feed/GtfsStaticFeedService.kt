package hr.zet.transit.api.feed

import hr.zet.transit.api.Config
import hr.zet.transit.api.cache.CachedValue
import hr.zet.transit.api.cache.TtlCache
import hr.zet.transit.api.model.DirectionScheduleDto
import hr.zet.transit.api.model.LatLngDto
import hr.zet.transit.api.model.RouteDto
import hr.zet.transit.api.model.RouteScheduleDto
import hr.zet.transit.api.model.RouteShapeDto
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
        /** routeId → geometrija rute (spojeni shapeovi te linije). */
        val shapesByRoute: Map<String, RouteShapeDto>,
        /** routeId → vozni red (polasci po smjerovima). */
        val schedulesByRoute: Map<String, RouteScheduleDto>,
        /** Lookup za obogaćivanje GTFS-RT feeda (mode, ime, headsign). */
        val lookup: GtfsLookup,
    )

    private val cache = TtlCache(
        ttlMillis = STATIC_CACHE_TTL_MILLIS,
        loader = ::fetchAndParse,
    )

    private suspend fun fetchAndParse(): StaticData {
        val zipBytes = httpClient.get(Config.zetGtfsStaticUrl).readBytes()
        val files = readZipEntries(
            zipBytes,
            // stop_times.txt (≈120 MB raspakirano) NE učitavamo u memoriju —
            // streamamo ga zasebno; inače CsvParser materijalizira milijune
            // Map redova po datoteci i ruši JVM (OOM).
            setOf("routes.txt", "stops.txt", "shapes.txt", "trips.txt"),
        )
        val routes = parseRoutes(files["routes.txt"].orEmpty())
        return StaticData(
            zipBytes = zipBytes,
            routes = routes,
            stops = parseStops(files["stops.txt"].orEmpty()),
            shapesByRoute = parseShapesByRoute(
                shapesTxt = files["shapes.txt"].orEmpty(),
                tripsTxt = files["trips.txt"].orEmpty(),
            ),
            schedulesByRoute = parseSchedulesByRoute(
                tripsTxt = files["trips.txt"].orEmpty(),
                tripDeparture = streamTripDepartures(zipBytes),
            ),
            lookup = parseLookup(
                routes = routes,
                tripsTxt = files["trips.txt"].orEmpty(),
            ),
        )
    }

    /** GTFS-RT obogaćivanje (C5) — vraća lookup; prazan ako static nije zreo. */
    suspend fun lookup(): GtfsLookup = cache.get().value.lookup

    /** Spaja routes.txt + trips.txt u GtfsLookup za RT mapper. */
    private fun parseLookup(routes: List<RouteDto>, tripsTxt: String): GtfsLookup {
        val headsignByTrip = CsvParser.parse(tripsTxt)
            .mapNotNull { row ->
                val tripId = row["trip_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                tripId to row["trip_headsign"].orEmpty()
            }
            .toMap()
        return GtfsLookup(
            modeByRoute = routes.associate { it.id to it.mode },
            shortNameByRoute = routes.associate { it.id to it.shortName },
            headsignByTrip = headsignByTrip,
        )
    }

    suspend fun routes(): CachedValue<List<RouteDto>> =
        cache.get().let { CachedValue(it.value.routes, it.fetchedAtEpochSeconds) }

    suspend fun stops(): CachedValue<List<StopDto>> =
        cache.get().let { CachedValue(it.value.stops, it.fetchedAtEpochSeconds) }

    /** Geometrija rute za danu liniju; null ako linija nema shape. */
    suspend fun routeShape(routeId: String): CachedValue<RouteShapeDto?> =
        cache.get().let { CachedValue(it.value.shapesByRoute[routeId], it.fetchedAtEpochSeconds) }

    /** Vozni red linije; null ako linija nema voznog reda. */
    suspend fun routeSchedule(routeId: String): CachedValue<RouteScheduleDto?> =
        cache.get().let { CachedValue(it.value.schedulesByRoute[routeId], it.fetchedAtEpochSeconds) }

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

    /**
     * Streamira `stop_times.txt` (≈120 MB raspakirano) red po red i vadi samo
     * polazno vrijeme s prvog stajališta (najmanji `stop_sequence`) po tripu.
     *
     * Cijela datoteka se NIKAD ne drži u memoriji — ni kao String ni kao
     * parsirani redovi. Zadržava se samo trip_id → (min seq, vrijeme), reda
     * veličine MB. (`CsvParser.parse` na cijeloj datoteci gradi milijune
     * Map-ova i ruši JVM — OOM na ovom feedu.)
     */
    private fun streamTripDepartures(zipBytes: ByteArray): Map<String, String> {
        val best = HashMap<String, Pair<Int, String>>()
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "stop_times.txt") {
                    // Reader NE zatvaramo — zatvorio bi cijeli ZipInputStream.
                    val reader = zip.bufferedReader()
                    val header = reader.readLine()?.removePrefix("﻿")?.let(::splitCsvLine)
                    if (header != null) {
                        val tripIdx = header.indexOf("trip_id")
                        val seqIdx = header.indexOf("stop_sequence")
                        val depIdx = header.indexOf("departure_time")
                        if (tripIdx >= 0 && seqIdx >= 0 && depIdx >= 0) {
                            val maxIdx = maxOf(tripIdx, seqIdx, depIdx)
                            var line = reader.readLine()
                            while (line != null) {
                                val cols = splitCsvLine(line)
                                if (cols.size > maxIdx) {
                                    val tripId = cols[tripIdx]
                                    val seq = cols[seqIdx].toIntOrNull()
                                    val dep = cols[depIdx]
                                    if (tripId.isNotEmpty() && dep.isNotEmpty() && seq != null) {
                                        val cur = best[tripId]
                                        if (cur == null || seq < cur.first) best[tripId] = seq to dep
                                    }
                                }
                                line = reader.readLine()
                            }
                        }
                    }
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return best.mapValues { it.value.second }
    }

    /** Jednoredni RFC 4180 split (quoted polja smiju sadržavati zareze); trimano. */
    private fun splitCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && line.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> { out.add(field.toString().trim()); field.setLength(0) }
                else -> field.append(c)
            }
            i++
        }
        out.add(field.toString().trim())
        return out
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

    /**
     * Spaja `shapes.txt` + `trips.txt` u routeId → geometrija.
     *
     * `shapes.txt`: shape_id, lat, lon, sequence. `trips.txt` linki route_id
     * → shape_id. Linija ima više shapeova (smjerovi/varijante) — uzimamo
     * onaj s najviše točaka (reprezentativan za prikaz na karti).
     */
    private fun parseShapesByRoute(shapesTxt: String, tripsTxt: String): Map<String, RouteShapeDto> {
        // shape_id → točke sortirane po sequence
        val shapePoints: Map<String, List<LatLngDto>> = CsvParser.parse(shapesTxt)
            .mapNotNull { row ->
                val shapeId = row["shape_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val lat = row["shape_pt_lat"]?.toDoubleOrNull() ?: return@mapNotNull null
                val lng = row["shape_pt_lon"]?.toDoubleOrNull() ?: return@mapNotNull null
                val seq = row["shape_pt_sequence"]?.toIntOrNull() ?: 0
                Triple(shapeId, seq, LatLngDto(lat, lng))
            }
            .groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, pts) -> pts.sortedBy { it.first }.map { it.second } }

        // route_id → kandidatski shape_id-ovi iz trips.txt
        return CsvParser.parse(tripsTxt)
            .mapNotNull { row ->
                val routeId = row["route_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val shapeId = row["shape_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                routeId to shapeId
            }
            .groupBy({ it.first }, { it.second })
            .mapNotNull { (routeId, shapeIds) ->
                val bestShape = shapeIds.distinct()
                    .mapNotNull { shapePoints[it] }
                    .maxByOrNull { it.size }
                    ?: return@mapNotNull null
                routeId to RouteShapeDto(routeId = routeId, points = bestShape)
            }
            .toMap()
    }

    /**
     * Spaja `trips.txt` + `stop_times.txt` u routeId → vozni red.
     *
     * Vozni red linije = vremena polazaka s prvog stajališta svakog tripa
     * (min `stop_sequence`), grupirano po `trip_headsign` (smjer). GTFS
     * dopušta vremena >24 h (noćni polasci) — normaliziramo na "HH:MM".
     */
    private fun parseSchedulesByRoute(
        tripsTxt: String,
        tripDeparture: Map<String, String>,
    ): Map<String, RouteScheduleDto> {
        // trip_id → (route_id, headsign)
        val tripInfo: Map<String, Pair<String, String>> = CsvParser.parse(tripsTxt)
            .mapNotNull { row ->
                val tripId = row["trip_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val routeId = row["route_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                tripId to (routeId to row["trip_headsign"].orEmpty())
            }
            .toMap()

        // (routeId, headsign) → sortirana lista polazaka
        return tripDeparture.entries
            .mapNotNull { (tripId, time) ->
                val info = tripInfo[tripId] ?: return@mapNotNull null
                Triple(info.first, info.second, normalizeTime(time))
            }
            .groupBy { it.first }
            .mapValues { (routeId, rows) ->
                val directions = rows
                    .groupBy { it.second }
                    .map { (headsign, deps) ->
                        DirectionScheduleDto(
                            headsign = headsign,
                            departures = deps.map { it.third }.distinct().sorted(),
                        )
                    }
                    .sortedBy { it.headsign }
                RouteScheduleDto(routeId = routeId, directions = directions)
            }
    }

    /** GTFS "HH:MM:SS" (sati mogu biti >24) → "HH:MM". */
    private fun normalizeTime(gtfsTime: String): String {
        val parts = gtfsTime.split(":")
        if (parts.size < 2) return gtfsTime
        val hh = parts[0].trim().padStart(2, '0')
        val mm = parts[1].trim().padStart(2, '0')
        return "$hh:$mm"
    }

    private companion object {
        /** Static GTFS se mijenja rijetko — 6 h TTL je dovoljno čest. */
        const val STATIC_CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L
    }
}
