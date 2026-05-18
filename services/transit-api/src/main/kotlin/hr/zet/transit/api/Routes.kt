package hr.zet.transit.api

import hr.zet.transit.api.cache.CachedValue
import hr.zet.transit.api.feed.GtfsRtFeedService
import hr.zet.transit.api.feed.GtfsStaticFeedService
import hr.zet.transit.api.feed.JourneyPlanningService
import hr.zet.transit.api.feed.WalkRoutingService
import hr.zet.transit.api.model.FeedResponse
import hr.zet.transit.api.notify.NotificationService
import hr.zet.transit.api.notify.RegisterTokenRequest
import io.ktor.server.request.receive
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * `/v1` endpointi — verzionirani klijent↔backend ugovor (sekcija 5 plana).
 * Breaking promjene idu u novi `/v2` blok; `/v1` ostaje za starije instalacije.
 */
fun Application.configureRouting(
    rtFeed: GtfsRtFeedService,
    staticFeed: GtfsStaticFeedService,
    walkRouting: WalkRoutingService,
    journeyPlanning: JourneyPlanningService,
    notifications: NotificationService,
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

            // A1.4 — kompletan statički vozni red linije.
            get("/routes/{routeId}/schedule") {
                val routeId = call.parameters["routeId"]
                if (routeId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing routeId"))
                    return@get
                }
                call.respond(staticFeed.routeSchedule(routeId).toFeedResponse())
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

            // A1.6 — pješački routing (OSRM foot). Parametri: from, to "lat,lng".
            get("/walk") {
                val from = call.parameters["from"]?.let(::parseLatLng)
                val to = call.parameters["to"]?.let(::parseLatLng)
                if (from == null || to == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "from/to traže format 'lat,lng'"),
                    )
                    return@get
                }
                val route = walkRouting.walkRoute(
                    fromLat = from.first, fromLng = from.second,
                    toLat = to.first, toLng = to.second,
                )
                call.respond(route ?: HttpStatusCode.NotFound)
            }

            // A2.1 — planiranje rute A→B (GraphHopper pt). Parametri: from, to,
            // departureTime (Unix sek, opcionalno — default sada).
            get("/plan") {
                val from = call.parameters["from"]?.let(::parseLatLng)
                val to = call.parameters["to"]?.let(::parseLatLng)
                if (from == null || to == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "from/to traže format 'lat,lng'"),
                    )
                    return@get
                }
                if (!journeyPlanning.isConfigured) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        mapOf("error" to "Planiranje rute trenutno nije dostupno."),
                    )
                    return@get
                }
                val departure = call.parameters["departureTime"]?.toLongOrNull()
                    ?: (System.currentTimeMillis() / 1000)
                val plans = journeyPlanning.plan(
                    fromLat = from.first, fromLng = from.second,
                    toLat = to.first, toLng = to.second,
                    departureEpochSeconds = departure,
                )
                call.respond(plans)
            }

            // A2.2 — registracija FCM tokena za push notifikacije.
            post("/notifications/register") {
                val request = call.receive<RegisterTokenRequest>()
                notifications.registerToken(request.token)
                call.respond(HttpStatusCode.NoContent)
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

/** Parsira "lat,lng" query parametar; null pri neispravnom formatu. */
private fun parseLatLng(raw: String): Pair<Double, Double>? {
    val parts = raw.split(",")
    if (parts.size != 2) return null
    val lat = parts[0].trim().toDoubleOrNull() ?: return null
    val lng = parts[1].trim().toDoubleOrNull() ?: return null
    return lat to lng
}
