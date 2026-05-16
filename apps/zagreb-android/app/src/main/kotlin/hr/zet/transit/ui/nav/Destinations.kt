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

    /** Stajalište-detalji (A0.2) — argument: stopId. */
    const val STOP_DETAIL = "stop/{stopId}"

    /** Gradi konkretnu rutu za stajalište-detalje. */
    fun stopDetail(stopId: String): String = "stop/$stopId"

    const val ARG_STOP_ID = "stopId"
}
