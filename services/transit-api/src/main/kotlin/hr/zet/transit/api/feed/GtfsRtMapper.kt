package hr.zet.transit.api.feed

import com.google.transit.realtime.GtfsRealtime.Alert
import com.google.transit.realtime.GtfsRealtime.TripUpdate
import com.google.transit.realtime.GtfsRealtime.VehiclePosition
import hr.zet.transit.api.model.ArrivalDto
import hr.zet.transit.api.model.ServiceAlertDto
import hr.zet.transit.api.model.VehicleDto

/**
 * GTFS-RT protobuf → `/v1` DTO mapiranje.
 *
 * Kritičan kod — plan traži snapshot testove (sekcija 8) jer ZET može
 * promijeniti format feeda. Mapiranje je defenzivno: zapisi bez nužnih
 * polja se preskaču umjesto da ruše cijeli odgovor (R2 — GTFS rupe).
 */

internal fun VehiclePosition.toDto(): VehicleDto {
    val routeId = trip.routeId.takeIf { it.isNotEmpty() }
    return VehicleDto(
        id = vehicle.id.ifEmpty { vehicle.label },
        routeId = routeId,
        mode = inferMode(routeId),
        lat = position.latitude.toDouble(),
        lng = position.longitude.toDouble(),
        bearing = if (position.hasBearing()) position.bearing else null,
        timestamp = if (hasTimestamp()) timestamp else 0L,
    )
}

/** Vraća dolaske iz TripUpdate-a, filtrirane na traženo stajalište. */
internal fun TripUpdate.toArrivalDtos(stopId: String): List<ArrivalDto> {
    val routeId = trip.routeId.takeIf { it.isNotEmpty() } ?: return emptyList()
    return stopTimeUpdateList
        .filter { it.stopId == stopId && it.hasArrival() }
        .map { stu ->
            val delay = if (stu.arrival.hasDelay()) stu.arrival.delay else null
            ArrivalDto(
                routeId = routeId,
                routeShortName = routeId,   // razriješi punim imenom iz GTFS static
                mode = inferMode(routeId),
                headsign = trip.tripId,     // razriješi headsignom iz GTFS static
                predictedTime = stu.arrival.time,
                delaySeconds = delay,
                isRealtime = true,
            )
        }
}

internal fun Alert.toDto(entityId: String): ServiceAlertDto = ServiceAlertDto(
    id = entityId,
    headerText = headerText.translationList.firstOrNull()?.text.orEmpty(),
    descriptionText = descriptionText.translationList.firstOrNull()?.text.orEmpty(),
    affectedRouteIds = informedEntityList.mapNotNull { it.routeId.takeIf(String::isNotEmpty) },
    severity = when (severityLevel) {
        Alert.SeverityLevel.SEVERE -> "SEVERE"
        Alert.SeverityLevel.WARNING -> "WARNING"
        else -> "INFO"
    },
)

/**
 * ZET ne kodira mode u RT feedu izravno. Privremena heuristika: tramvajske
 * linije su 1–2-znamenkasti brojevi ≤ 17. Faza 0 zamjenjuje ovo lookupom
 * u GTFS static `routes.txt` (`route_type`: 0 = tram, 3 = bus).
 */
internal fun inferMode(routeId: String?): String {
    val num = routeId?.toIntOrNull() ?: return "BUS"
    return if (num in 1..17) "TRAM" else "BUS"
}
