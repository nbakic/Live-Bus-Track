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

    /** Lookup s poznatim linijama 2 (tram) i 4 (bus) te tripom trip-7. */
    private val lookup = GtfsLookup(
        modeByRoute = mapOf("2" to "TRAM", "4" to "BUS"),
        shortNameByRoute = mapOf("2" to "2", "4" to "4"),
        headsignByTrip = mapOf("trip-7" to "Dubec"),
    )

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

        val dto = vp.toDto(lookup)

        assertEquals("V123", dto.id)
        assertEquals("2", dto.routeId)
        assertEquals("TRAM", dto.mode, "linija 2 je tram u lookupu")
        assertEquals(90f, dto.bearing)
        assertEquals(1_700_000_000L, dto.timestamp)
    }

    @Test
    fun vehiclePosition_withoutBearing_hasNullBearing() {
        val vp = VehiclePosition.newBuilder()
            .setVehicle(VehicleDescriptor.newBuilder().setId("V1"))
            .setPosition(Position.newBuilder().setLatitude(45.8f).setLongitude(16.0f))
            .build()

        assertNull(vp.toDto(lookup).bearing)
    }

    @Test
    fun lookup_unknownRoute_fallsBackToBus() {
        // Linija koje nema u lookupu — siguran default je BUS.
        assertEquals("BUS", lookup.modeOf("999"))
        assertEquals("BUS", lookup.modeOf(null))
        // Nepoznata linija: shortName fallback je sam routeId.
        assertEquals("999", lookup.shortNameOf("999"))
    }

    @Test
    fun tripUpdate_filtersToRequestedStop_andEnrichesFromLookup() {
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

        val arrivals = tu.toArrivalDtos("STOP_A", lookup)

        assertEquals(1, arrivals.size, "samo dolazak za traženo stajalište")
        val arrival = arrivals.single()
        assertEquals("4", arrival.routeId)
        assertEquals(60, arrival.delaySeconds)
        assertTrue(arrival.isRealtime)
        // C5: ime i headsign dolaze iz lookupa, ne placeholderi.
        assertEquals("4", arrival.routeShortName)
        assertEquals("Dubec", arrival.headsign)
        assertEquals("BUS", arrival.mode)
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
