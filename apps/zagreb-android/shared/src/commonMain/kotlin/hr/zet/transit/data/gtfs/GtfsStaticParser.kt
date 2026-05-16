package hr.zet.transit.data.gtfs

import hr.zet.transit.domain.model.LatLng
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.model.TransitMode

/**
 * Parsira GTFS static CSV datoteke (`routes.txt`, `stops.txt`) u domain modele.
 *
 * Defenzivno (R2 — GTFS rupe): zapisi bez nužnih polja se preskaču, ne ruše
 * cijeli import. Parsiranje vraća samo valjane zapise.
 */
object GtfsStaticParser {

    /** Parsira `routes.txt`. `route_type`: 0 = tram, 3 = bus (GTFS spec). */
    fun parseRoutes(routesTxt: String): List<Route> =
        CsvParser.parse(routesTxt).mapNotNull { row ->
            val id = row["route_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            Route(
                id = id,
                shortName = row["route_short_name"].orEmpty(),
                longName = row["route_long_name"].orEmpty(),
                mode = when (row["route_type"]?.trim()) {
                    "0" -> TransitMode.TRAM
                    else -> TransitMode.BUS
                },
                color = row["route_color"]
                    ?.takeIf(String::isNotBlank)
                    ?.let { if (it.startsWith("#")) it else "#$it" },
            )
        }

    /** Parsira `stops.txt`. Preskače zapise bez valjanih koordinata. */
    fun parseStops(stopsTxt: String): List<Stop> =
        CsvParser.parse(stopsTxt).mapNotNull { row ->
            val id = row["stop_id"]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val lat = row["stop_lat"]?.toDoubleOrNull() ?: return@mapNotNull null
            val lng = row["stop_lon"]?.toDoubleOrNull() ?: return@mapNotNull null
            Stop(
                id = id,
                name = row["stop_name"].orEmpty(),
                position = LatLng(lat, lng),
            )
        }

    /** Iz `feed_info.txt` izvuče `feed_version` (potvrda svježine, sekcija 2). */
    fun parseFeedVersion(feedInfoTxt: String): String? =
        CsvParser.parse(feedInfoTxt).firstOrNull()
            ?.get("feed_version")
            ?.takeIf(String::isNotBlank)
}
