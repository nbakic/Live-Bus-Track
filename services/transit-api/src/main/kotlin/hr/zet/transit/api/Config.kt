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
        env("ZET_GTFS_RT_URL", "https://zet.hr/gtfs2/gtfs-rt-protobuf")

    /** GTFS static ZIP — linije, stajališta, redovi vožnje. */
    val zetGtfsStaticUrl: String =
        env("ZET_GTFS_STATIC_URL", "https://zet.hr/gtfs2/gtfs-scheduled/latest")

    /** Pass-through cache TTL za RT feed (sekunde) — sekcija 5 plana: ~10 s. */
    val rtCacheTtlSeconds: Long = env("RT_CACHE_TTL", "10").toLong()

    /** HTTP port. */
    val port: Int = env("PORT", "8080").toInt()

    private fun env(key: String, default: String): String =
        System.getenv(key)?.takeIf { it.isNotBlank() } ?: default
}
