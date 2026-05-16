package hr.zet.transit.data.gtfs

import hr.zet.transit.domain.model.TransitMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GtfsStaticParserTest {

    @Test
    fun parsesRoutes_andMapsRouteType() {
        val csv = """
            route_id,route_short_name,route_long_name,route_type,route_color
            1,1,Zapadni kolodvor - Borongaj,0,E30613
            109,109,Črnomerec - Šestine,3,
        """.trimIndent()

        val routes = GtfsStaticParser.parseRoutes(csv)

        assertEquals(2, routes.size)
        assertEquals(TransitMode.TRAM, routes[0].mode)
        assertEquals("#E30613", routes[0].color, "route_color dobiva # prefiks")
        assertEquals(TransitMode.BUS, routes[1].mode)
        assertNull(routes[1].color, "prazan route_color je null")
    }

    @Test
    fun parseStops_skipsRowsWithInvalidCoordinates() {
        val csv = """
            stop_id,stop_name,stop_lat,stop_lon
            S1,Trg bana Jelačića,45.8131,15.9775
            S2,Pokvareno stajalište,,15.99
            S3,Glavni kolodvor,45.8047,15.9786
        """.trimIndent()

        val stops = GtfsStaticParser.parseStops(csv)

        assertEquals(2, stops.size, "stajalište bez koordinata se preskače (R2)")
        assertTrue(stops.none { it.id == "S2" })
    }

    @Test
    fun csvParser_handlesQuotedFieldsWithCommas() {
        val csv = "stop_id,stop_name,stop_lat,stop_lon\n" +
            "S1,\"Ulica grada Vukovara, 50\",45.80,15.97"

        val stops = GtfsStaticParser.parseStops(csv)

        assertEquals("Ulica grada Vukovara, 50", stops.single().name)
    }

    @Test
    fun parseFeedVersion_extractsVersion() {
        val csv = """
            feed_publisher_name,feed_version
            ZET,2026-05-16-001
        """.trimIndent()

        assertEquals("2026-05-16-001", GtfsStaticParser.parseFeedVersion(csv))
    }
}
