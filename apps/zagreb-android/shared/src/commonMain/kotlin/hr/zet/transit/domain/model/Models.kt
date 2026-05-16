package hr.zet.transit.domain.model

/**
 * Domain modeli — čista Kotlin logika, neovisna o GTFS formatu ili transportu.
 * Mapiranje GTFS → domain događa se u data sloju (gtfs/).
 *
 * Vidi sekciju 3 plana: A0 funkcije rade nad ovim modelima.
 */

/** Geografska točka (WGS84). */
data class LatLng(
    val lat: Double,
    val lng: Double,
)

/** Tip prijevoznog sredstva — ZET ima tramvaje i autobuse. */
enum class TransitMode {
    TRAM,
    BUS,
}

/** Linija (GTFS route). */
data class Route(
    val id: String,
    val shortName: String,
    val longName: String,
    val mode: TransitMode,
    /** HEX boja linije iz GTFS routes.txt, npr. "#E30613". */
    val color: String?,
)

/** Stajalište (GTFS stop). */
data class Stop(
    val id: String,
    val name: String,
    val position: LatLng,
)

/** Smjer vožnje vozila — izračunat iz uzastopnih pozicija ili GTFS-RT bearing. */
data class Heading(val degrees: Float)

/**
 * Živa pozicija vozila iz GTFS-RT VehiclePosition.
 * RT je opcionalni sloj nad statičkim GTFS-om (Plan B za R1, sekcija 9).
 */
data class Vehicle(
    val id: String,
    val routeId: String?,
    val mode: TransitMode,
    val position: LatLng,
    val heading: Heading?,
    /** Unix sekunde — vrijeme zadnjeg ažuriranja iz feeda. */
    val timestamp: Long,
)

/** Predviđeni dolazak na stajalište (GTFS-RT TripUpdate). */
data class Arrival(
    val routeId: String,
    val routeShortName: String,
    val mode: TransitMode,
    val headsign: String,
    /** Unix sekunde — predviđeno vrijeme dolaska. */
    val predictedTime: Long,
    /** Kašnjenje u sekundama (+ kasni, − rani). null = nema RT podatka. */
    val delaySeconds: Int?,
    /** true = predikcija iz RT feeda; false = statički vozni red (Plan B). */
    val isRealtime: Boolean,
)

/** Service alert (GTFS-RT Alert). */
data class ServiceAlert(
    val id: String,
    val headerText: String,
    val descriptionText: String,
    val affectedRouteIds: List<String>,
    val severity: AlertSeverity,
)

enum class AlertSeverity {
    INFO,
    WARNING,
    SEVERE,
}
