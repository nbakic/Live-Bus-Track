package hr.zet.transit.api

import hr.zet.transit.api.feed.GtfsRtFeedService
import hr.zet.transit.api.feed.GtfsStaticFeedService
import hr.zet.transit.api.feed.JourneyPlanningService
import hr.zet.transit.api.feed.WalkRoutingService
import hr.zet.transit.api.model.ErrorResponse
import hr.zet.transit.api.notify.NotificationService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, port = Config.port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val log = LoggerFactory.getLogger("transit-api")

    install(ContentNegotiation) {
        json(Json { prettyPrint = false; encodeDefaults = true })
    }
    install(CallLogging)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            // R1 graceful degradation: ako ZET feed padne, ne rušimo proces —
            // vraćamo 502 s jasnom porukom; klijent backoffa i prikazuje fallback.
            log.error("Neuhvaćena greška pri obradi zahtjeva", cause)
            call.respond(
                HttpStatusCode.BadGateway,
                ErrorResponse(
                    error = "upstream_unavailable",
                    message = "Feed trenutno nedostupan.",
                ),
            )
        }
    }

    val zetHttpClient = HttpClient(CIO)
    // Static prvo — RT feed ga koristi za obogaćivanje (mode, ime, headsign — C5).
    val staticFeed = GtfsStaticFeedService(zetHttpClient)
    val rtFeed = GtfsRtFeedService(
        httpClient = zetHttpClient,
        lookupProvider = { staticFeed.lookup() },
    )

    // Zaseban klijent s JSON negotiationom — OSRM i GraphHopper vraćaju JSON
    // (ZET feedovi su protobuf/zip). Dijeli ga pješački i transit routing.
    val jsonHttpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    val walkRouting = WalkRoutingService(jsonHttpClient)
    val journeyPlanning = JourneyPlanningService(jsonHttpClient)
    val notifications = NotificationService()

    configureRouting(rtFeed, staticFeed, walkRouting, journeyPlanning, notifications)
    log.info("transit-api pokrenut na portu ${Config.port}")
}
