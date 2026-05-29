package hr.zet.transit.api

/**
 * Konfiguracija backenda. ZET base URL-ovi i cache TTL.
 *
 * Vrijednosti se čitaju iz env varijabli s defaultima — produkcijski
 * deploy ih override-a. ZET endpointi prema sekciji 2 plana.
 */
object Config {
    /** GTFS-RT protobuf feed — žive pozicije, kašnjenja, alerts. */
    val zetGtfsRtUrl: String =
        env("ZET_GTFS_RT_URL", "https://www.zet.hr/gtfs-rt-protobuf")

    /** GTFS static ZIP — linije, stajališta, redovi vožnje. */
    val zetGtfsStaticUrl: String =
        env("ZET_GTFS_STATIC_URL", "https://www.zet.hr/gtfs-scheduled/latest")

    /** Pass-through cache TTL za RT feed (sekunde) — sekcija 5 plana: ~10 s. */
    val rtCacheTtlSeconds: Long = env("RT_CACHE_TTL", "10").toLong()

    /**
     * OSRM bazni URL s `foot` profilom — pješački routing (A1.6 plana).
     * Default je javni demo server; produkcija koristi self-hosted OSRM
     * (lagan, ~stane u A0/A1 hosting tier — sekcija 14 plana).
     */
    val osrmFootUrl: String =
        env("OSRM_FOOT_URL", "https://routing.openstreetmap.de/routed-foot/route/v1/foot")

    /**
     * GraphHopper Routing API — transit planiranje rute A→B (A2.1 plana).
     * Plan (sekcija 3.2): GraphHopper je izbor ako trošak hostinga dominira.
     * Koristi se hostani GraphHopper API s `pt` profilom — treba API ključ.
     */
    val graphHopperUrl: String =
        env("GRAPHHOPPER_URL", "https://graphhopper.com/api/1/route")

    /** GraphHopper API ključ; prazan = A2.1 endpoint vraća 503. */
    val graphHopperApiKey: String = env("GRAPHHOPPER_API_KEY", "")

    /** HTTP port. */
    val port: Int = env("PORT", "8080").toInt()

    private fun env(key: String, default: String): String =
        System.getenv(key)?.takeIf { it.isNotBlank() } ?: default
}
