package hr.zet.transit.api

import hr.zet.transit.api.cache.CachedValue
import hr.zet.transit.api.feed.GtfsRtFeedService
import hr.zet.transit.api.feed.GtfsStaticFeedService
import hr.zet.transit.api.model.FeedResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * `/v1` endpointi — verzionirani klijent↔backend ugovor (sekcija 5 plana).
 * Breaking promjene idu u novi `/v2` blok; `/v1` ostaje za starije instalacije.
 */
fun Application.configureRouting(
    rtFeed: GtfsRtFeedService,
    staticFeed: GtfsStaticFeedService,
) {
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        route("/v1") {
            // A0.1 — žive pozicije vozila.
            get("/vehicles") {
                call.respond(rtFeed.vehicles().toFeedResponse())
            }

            // A0.2 — dolasci na stajalište.
            get("/stops/{stopId}/arrivals") {
                val stopId = call.parameters["stopId"]
                if (stopId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing stopId"))
                    return@get
                }
                call.respond(rtFeed.arrivals(stopId).toFeedResponse())
            }

            // A0.4 — service alerts.
            get("/alerts") {
                call.respond(rtFeed.alerts().toFeedResponse())
            }

            // A1.1 — sve linije iz GTFS static.
            get("/routes") {
                call.respond(staticFeed.routes().toFeedResponse())
            }

            // A1.2 — geometrija rute iz shapes.txt.
            get("/routes/{routeId}/shape") {
                val routeId = call.parameters["routeId"]
                if (routeId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing routeId"))
                    return@get
                }
                call.respond(staticFeed.routeShape(routeId).toFeedResponse())
            }

            // A0.6 — sva stajališta iz GTFS static.
            get("/stops") {
                call.respond(staticFeed.stops().toFeedResponse())
            }

            // Sirovi GTFS static ZIP — klijent ga importira (GtfsImporter).
            get("/gtfs/static.zip") {
                call.respondBytes(
                    bytes = staticFeed.rawZip(),
                    contentType = ContentType.Application.Zip,
                )
            }
        }
    }
}

/** Cached vrijednost → klijentski FeedResponse, prenosi meta o svježini. */
private fun <T> CachedValue<T>.toFeedResponse(): FeedResponse<T> =
    FeedResponse(
        data = value,
        fetchedAt = fetchedAtEpochSeconds,
        live = true,
    )
