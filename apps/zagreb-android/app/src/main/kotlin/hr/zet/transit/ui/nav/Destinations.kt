package hr.zet.transit.ui.nav

/**
 * Navigacijska odredišta. Rute su stringovi s argument-placeholderima
 * (Navigation Compose); `route` je obrazac, `build*` gradi konkretan put.
 */
object Destinations {
    /** Karta vozila (A0.1) — početni ekran. */
    const val MAP = "map"

    /** Pregled svih linija (A1.1). */
    const val ROUTES = "routes"

    /** Omiljena stajališta (A0.3). */
    const val FAVORITES = "favorites"

    /** Pretraga (A1.3). */
    const val SEARCH = "search"

    /** Stajalište-detalji (A0.2) — argument: stopId. */
    const val STOP_DETAIL = "stop/{stopId}"

    /** Detalji linije (A1.1) — argument: routeId. */
    const val ROUTE_DETAIL = "route/{routeId}"

    /** Gradi konkretnu rutu za stajalište-detalje. */
    fun stopDetail(stopId: String): String = "stop/$stopId"

    /** Gradi konkretnu rutu za detalje linije. */
    fun routeDetail(routeId: String): String = "route/$routeId"

    const val ARG_STOP_ID = "stopId"
    const val ARG_ROUTE_ID = "routeId"
}
