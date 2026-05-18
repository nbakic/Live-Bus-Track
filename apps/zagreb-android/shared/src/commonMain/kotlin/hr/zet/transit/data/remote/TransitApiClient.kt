package hr.zet.transit.data.remote

import hr.zet.transit.data.remote.dto.ArrivalDto
import hr.zet.transit.data.remote.dto.FeedResponse
import hr.zet.transit.data.remote.dto.JourneyPlanDto
import hr.zet.transit.data.remote.dto.RegisterTokenRequest
import hr.zet.transit.data.remote.dto.RouteScheduleDto
import hr.zet.transit.data.remote.dto.RouteShapeDto
import hr.zet.transit.data.remote.dto.ServiceAlertDto
import hr.zet.transit.data.remote.dto.VehicleDto
import hr.zet.transit.data.remote.dto.WalkRouteDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Ktor klijent za `transit-api` backend.
 *
 * Klijent gađa ISKLJUČIVO ovaj backend (`/v1/...`), nikad ZET izravno
 * (sekcija 5 plana — D1). Ugovor je verzioniran; `API_VERSION` se mijenja
 * samo uz breaking change, starije instalacije moraju nastaviti raditi.
 *
 * HttpClient se kreira interno ([buildHttpClient]) pa platform sloj (`:app`,
 * iOS) ne mora ovisiti o Ktoru — konstruira se samo iz `baseUrl`.
 */
class TransitApiClient(
    private val baseUrl: String,
) {
    private val httpClient: HttpClient = buildHttpClient()

    suspend fun getVehicles(): FeedResponse<List<VehicleDto>> =
        httpClient.get(baseUrl) {
            url { appendPathSegments(API_VERSION, "vehicles") }
        }.body()

    suspend fun getArrivals(stopId: String): FeedResponse<List<ArrivalDto>> =
        httpClient.get(baseUrl) {
            url { appendPathSegments(API_VERSION, "stops", stopId, "arrivals") }
        }.body()

    suspend fun getAlerts(): FeedResponse<List<ServiceAlertDto>> =
        httpClient.get(baseUrl) {
            url { appendPathSegments(API_VERSION, "alerts") }
        }.body()

    suspend fun getRouteShape(routeId: String): FeedResponse<RouteShapeDto?> =
        httpClient.get(baseUrl) {
            url { appendPathSegments(API_VERSION, "routes", routeId, "shape") }
        }.body()

    suspend fun getRouteSchedule(routeId: String): FeedResponse<RouteScheduleDto?> =
        httpClient.get(baseUrl) {
            url { appendPathSegments(API_VERSION, "routes", routeId, "schedule") }
        }.body()

    /** A1.6 — pješačka ruta od izvora do odredišta (OSRM foot na backendu). */
    suspend fun getWalkRoute(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
    ): WalkRouteDto = httpClient.get(baseUrl) {
        url { appendPathSegments(API_VERSION, "walk") }
        parameter("from", "$fromLat,$fromLng")
        parameter("to", "$toLat,$toLng")
    }.body()

    /** A2.1 — planiranje rute A→B (GraphHopper pt na backendu). */
    suspend fun getJourneyPlans(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
    ): List<JourneyPlanDto> = httpClient.get(baseUrl) {
        url { appendPathSegments(API_VERSION, "plan") }
        parameter("from", "$fromLat,$fromLng")
        parameter("to", "$toLat,$toLng")
    }.body()

    /** A2.2 — registrira FCM token uređaja za push notifikacije. */
    suspend fun registerNotificationToken(token: String) {
        httpClient.post(baseUrl) {
            url { appendPathSegments(API_VERSION, "notifications", "register") }
            contentType(ContentType.Application.Json)
            setBody(RegisterTokenRequest(token))
        }
    }

    companion object {
        const val API_VERSION = "v1"

        /**
         * Tvornica HttpClienta s JSON content negotiationom.
         * Engine je per-platform ([createPlatformHttpClient]) — dijeli se s iOS-om.
         */
        private fun buildHttpClient(): HttpClient = createPlatformHttpClient().config {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
    }
}
