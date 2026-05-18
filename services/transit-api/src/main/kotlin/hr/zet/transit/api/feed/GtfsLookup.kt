package hr.zet.transit.api.feed

/**
 * Pretraživanje GTFS static podataka koje GTFS-RT mapper treba za obogaćivanje.
 *
 * RT feed daje samo `route_id` i `trip_id`; ime linije, mode i headsign žive
 * u GTFS static. Ovaj lookup spaja to dvoje (rješava C5 iz docs/TODO.md te
 * placeholdere `routeShortName`/`headsign` u dolascima).
 */
data class GtfsLookup(
    /** route_id → "TRAM" | "BUS" (iz `route_type` u routes.txt). */
    private val modeByRoute: Map<String, String>,
    /** route_id → kratko ime linije (route_short_name). */
    private val shortNameByRoute: Map<String, String>,
    /** trip_id → trip_headsign (smjer/odredište). */
    private val headsignByTrip: Map<String, String>,
) {
    /** Mode linije; "BUS" kao siguran default ako linija nije poznata. */
    fun modeOf(routeId: String?): String =
        routeId?.let { modeByRoute[it] } ?: "BUS"

    /** Kratko ime linije; vraća sam `routeId` ako ime nije poznato. */
    fun shortNameOf(routeId: String?): String =
        routeId?.let { shortNameByRoute[it] ?: it } ?: ""

    /** Headsign tripa; prazan string ako trip nije poznat. */
    fun headsignOf(tripId: String?): String =
        tripId?.let { headsignByTrip[it] } ?: ""

    companion object {
        /** Prazan lookup — fallback dok GTFS static nije učitan. */
        val EMPTY = GtfsLookup(emptyMap(), emptyMap(), emptyMap())
    }
}
