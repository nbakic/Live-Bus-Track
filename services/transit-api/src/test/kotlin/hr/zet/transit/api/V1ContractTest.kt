package hr.zet.transit.api

import com.google.transit.realtime.GtfsRealtime.FeedHeader
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import hr.zet.transit.api.feed.GtfsRtFeedService
import hr.zet.transit.api.feed.GtfsStaticFeedService
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.testing.testApplication
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Kontraktni testovi `/v1` ugovora (plan sekcija 8).
 *
 * Starije instalacije aplikacije moraju nastaviti raditi kad backend
 * evoluira — ovi testovi fiksiraju oblik `/v1` odgovora. ZET feedovi se
 * simuliraju Ktor MockEngineom; backend ne dira stvarni ZET.
 */
class V1ContractTest {

    /** Prazan ali valjan GTFS-RT FeedMessage (protobuf). */
    private fun emptyRtFeed(): ByteArray =
        FeedMessage.newBuilder()
            .setHeader(FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
            .build()
            .toByteArray()

    /** Minimalan valjan GTFS static ZIP s routes.txt i stops.txt. */
    private fun minimalGtfsZip(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("routes.txt"))
            zip.write("route_id,route_short_name,route_long_name,route_type\n2,2,Crnomerec,0\n".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("stops.txt"))
            zip.write("stop_id,stop_name,stop_lat,stop_lon\nS1,Jelacic,45.81,15.97\n".toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    /** ZET mock — RT endpoint vraća protobuf, static endpoint vraća ZIP. */
    private fun zetMockClient(): HttpClient {
        val engine = MockEngine { request ->
            val body = if (request.url.encodedPath.contains("rt")) {
                emptyRtFeed()
            } else {
                minimalGtfsZip()
            }
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"),
            )
        }
        return HttpClient(engine)
    }

    private fun io.ktor.server.application.Application.testModule() {
        install(ContentNegotiation) { json() }
        install(StatusPages) {
            exception<Throwable> { call, _ ->
                call.respond(HttpStatusCode.BadGateway, mapOf("error" to "upstream"))
            }
        }
        val client = zetMockClient()
        configureRouting(GtfsRtFeedService(client), GtfsStaticFeedService(client))
    }

    @Test
    fun health_returnsOk() = testApplication {
        application { testModule() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("ok"))
    }

    @Test
    fun v1Vehicles_returnsFeedResponseShape() = testApplication {
        application { testModule() }
        val body = client.get("/v1/vehicles").bodyAsText()
        // FeedResponse ugovor: data + fetchedAt + live.
        assertTrue(body.contains("\"data\""))
        assertTrue(body.contains("\"fetchedAt\""))
        assertTrue(body.contains("\"live\""))
    }

    @Test
    fun v1Routes_parsesGtfsStaticZip() = testApplication {
        application { testModule() }
        val body = client.get("/v1/routes").bodyAsText()
        // routes.txt iz mock ZIP-a: linija "2", tip 0 → TRAM.
        assertTrue(body.contains("\"id\":\"2\""))
        assertTrue(body.contains("TRAM"))
    }

    @Test
    fun v1Stops_parsesGtfsStaticZip() = testApplication {
        application { testModule() }
        val body = client.get("/v1/stops").bodyAsText()
        assertTrue(body.contains("Jelacic"))
        assertTrue(body.contains("45.81"))
    }

    @Test
    fun v1StopArrivals_missingStopId_isBadRequest() = testApplication {
        application { testModule() }
        // Prazan stopId segment — ruta traži neprazan parametar.
        val response = client.get("/v1/stops//arrivals")
        assertTrue(
            response.status == HttpStatusCode.BadRequest ||
                response.status == HttpStatusCode.NotFound,
        )
    }
}
