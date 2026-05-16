package hr.zet.transit.api

import hr.zet.transit.api.cache.CachedValue
import hr.zet.transit.api.feed.GtfsRtFeedService
import hr.zet.transit.api.model.FeedResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * `/v1` endpointi — verzionirani klijent↔backend ugovor (sekcija 5 plana).
 * Breaking promjene idu u novi `/v2` blok; `/v1` ostaje za starije instalacije.
 */
fun Application.configureRouting(feedService: GtfsRtFeedService) {
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        route("/v1") {
            // A0.1 — žive pozicije vozila.
            get("/vehicles") {
                call.respond(feedService.vehicles().toFeedResponse())
            }

            // A0.2 — dolasci na stajalište.
            get("/stops/{stopId}/arrivals") {
                val stopId = call.parameters["stopId"]
                if (stopId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing stopId"))
                    return@get
                }
                call.respond(feedService.arrivals(stopId).toFeedResponse())
            }

            // A0.4 — service alerts.
            get("/alerts") {
                call.respond(feedService.alerts().toFeedResponse())
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
