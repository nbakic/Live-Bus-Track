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
 * Geometrija rute — niz točaka koje crtaju liniju na karti (GTFS `shapes.txt`).
 * Linija može imati više shapeova (smjerovi, varijante); ovo je jedan.
 */
data class RouteShape(
    val routeId: String,
    val points: List<LatLng>,
)

/** Vozni red linije (A1.4) — polasci grupirani po smjeru. */
data class RouteSchedule(
    val routeId: String,
    val directions: List<DirectionSchedule>,
)

/** Vozni red jednog smjera linije — vremena polazaka kao "HH:MM". */
data class DirectionSchedule(
    val headsign: String,
    val departures: List<String>,
)

/** Tip rutine — imenovano svakodnevno odredište dnevnog putnika (A1.5). */
enum class RoutineKind {
    HOME,
    WORK,
    SCHOOL,
}

/**
 * Rutina (A1.5) — imenovano mjesto vezano na stajalište s kojeg korisnik
 * najčešće kreće. Hrani jutarnji ekran i pojednostavljuje dnevni tok.
 */
data class Routine(
    val kind: RoutineKind,
    val stopId: String,
    /** Naziv stajališta — cacheiran radi prikaza bez dodatnog upita. */
    val stopName: String,
)

/**
 * Pješačka ruta (A1.6) — rezultat OSRM `foot` upita preko backenda.
 * Koristi se za "najbliža stanica preko pješačkih putova" i crtanje puta.
 */
data class WalkRoute(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val geometry: List<LatLng>,
)

/** Tip dionice putovanja — pješačenje ili vožnja linijom. */
enum class JourneyLegType {
    WALK,
    TRANSIT,
}

/** Jedna dionica plana putovanja (A2.1). */
data class JourneyLeg(
    val type: JourneyLegType,
    val fromName: String,
    val toName: String,
    /** Unix sekunde. */
    val departureTime: Long,
    val arrivalTime: Long,
    /** Oznaka i smjer linije — samo za TRANSIT dionice. */
    val routeName: String?,
    val headsign: String?,
)

/** Plan rute A→B (A2.1) — jedna ponuđena varijanta putovanja. */
data class JourneyPlan(
    val totalDurationSeconds: Long,
    val departureTime: Long,
    val arrivalTime: Long,
    val legs: List<JourneyLeg>,
)

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
