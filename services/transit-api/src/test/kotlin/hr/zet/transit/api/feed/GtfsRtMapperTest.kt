package hr.zet.transit.api.feed

import com.google.transit.realtime.GtfsRealtime.Alert
import com.google.transit.realtime.GtfsRealtime.EntitySelector
import com.google.transit.realtime.GtfsRealtime.Position
import com.google.transit.realtime.GtfsRealtime.TranslatedString
import com.google.transit.realtime.GtfsRealtime.TripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripUpdate
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor
import com.google.transit.realtime.GtfsRealtime.VehiclePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testovi GTFS-RT protobuf → DTO mapiranja.
 *
 * Plan (sekcija 8): RT parser je kritičan i podložan promjeni feeda.
 * Umjesto binarnog fixturea gradimo protobuf poruke programski — pokriva
 * istu logiku mapiranja i ne ovisi o sačuvanom ZET payloadu.
 */
class GtfsRtMapperTest {

    @Test
    fun vehiclePosition_mapsCoreFields() {
        val vp = VehiclePosition.newBuilder()
            .setVehicle(VehicleDescriptor.newBuilder().setId("V123"))
            .setTrip(TripDescriptor.newBuilder().setRouteId("2"))
            .setPosition(
                Position.newBuilder()
                    .setLatitude(45.81f)
                    .setLongitude(15.98f)
                    .setBearing(90f),
            )
            .setTimestamp(1_700_000_000L)
            .build()

        val dto = vp.toDto()

        assertEquals("V123", dto.id)
        assertEquals("2", dto.routeId)
        assertEquals("TRAM", dto.mode, "linija 2 je ≤17 → tramvaj")
        assertEquals(90f, dto.bearing)
        assertEquals(1_700_000_000L, dto.timestamp)
    }

    @Test
    fun vehiclePosition_withoutBearing_hasNullBearing() {
        val vp = VehiclePosition.newBuilder()
            .setVehicle(VehicleDescriptor.newBuilder().setId("V1"))
            .setPosition(Position.newBuilder().setLatitude(45.8f).setLongitude(16.0f))
            .build()

        assertNull(vp.toDto().bearing)
    }

    @Test
    fun inferMode_busForHighRouteNumbers() {
        assertEquals("BUS", inferMode("109"))
        assertEquals("TRAM", inferMode("17"))
        assertEquals("BUS", inferMode(null))
    }

    @Test
    fun tripUpdate_filtersToRequestedStop() {
        val tu = TripUpdate.newBuilder()
            .setTrip(TripDescriptor.newBuilder().setRouteId("4").setTripId("trip-7"))
            .addStopTimeUpdate(
                TripUpdate.StopTimeUpdate.newBuilder()
                    .setStopId("STOP_A")
                    .setArrival(TripUpdate.StopTimeEvent.newBuilder().setTime(1_700_000_100L).setDelay(60)),
            )
            .addStopTimeUpdate(
                TripUpdate.StopTimeUpdate.newBuilder()
                    .setStopId("STOP_B")
                    .setArrival(TripUpdate.StopTimeEvent.newBuilder().setTime(1_700_000_200L)),
            )
            .build()

        val arrivals = tu.toArrivalDtos("STOP_A")

        assertEquals(1, arrivals.size, "samo dolazak za traženo stajalište")
        assertEquals("4", arrivals.single().routeId)
        assertEquals(60, arrivals.single().delaySeconds)
        assertTrue(arrivals.single().isRealtime)
    }

    @Test
    fun alert_mapsSeverityAndText() {
        val alert = Alert.newBuilder()
            .setSeverityLevel(Alert.SeverityLevel.WARNING)
            .setHeaderText(
                TranslatedString.newBuilder().addTranslation(
                    TranslatedString.Translation.newBuilder().setText("Zatvorena Ilica"),
                ),
            )
            .setDescriptionText(
                TranslatedString.newBuilder().addTranslation(
                    TranslatedString.Translation.newBuilder().setText("Radovi do petka"),
                ),
            )
            .addInformedEntity(EntitySelector.newBuilder().setRouteId("6"))
            .build()

        val dto = alert.toDto("alert-1")

        assertEquals("alert-1", dto.id)
        assertEquals("WARNING", dto.severity)
        assertEquals("Zatvorena Ilica", dto.headerText)
        assertEquals("Radovi do petka", dto.descriptionText)
        assertEquals(listOf("6"), dto.affectedRouteIds)
    }
}
