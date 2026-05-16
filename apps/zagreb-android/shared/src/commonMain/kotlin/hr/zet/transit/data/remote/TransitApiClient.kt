package hr.zet.transit.data.remote

import hr.zet.transit.data.remote.dto.ArrivalDto
import hr.zet.transit.data.remote.dto.FeedResponse
import hr.zet.transit.data.remote.dto.RouteShapeDto
import hr.zet.transit.data.remote.dto.ServiceAlertDto
import hr.zet.transit.data.remote.dto.VehicleDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
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
