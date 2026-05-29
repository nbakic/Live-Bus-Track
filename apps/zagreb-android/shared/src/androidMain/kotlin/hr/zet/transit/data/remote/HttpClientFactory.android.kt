package hr.zet.transit.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

/**
 * Tvornica HttpClienta za Android. Eksplicitni timeouti su nužni — bez njih
 * OkHttp može čekati beskonačno na zaglavljenom socketu (npr. emulator bez
 * mreže). Pozivi idu u viewModelScope (ne na main thread), pa duži timeout
 * ne uzrokuje ANR.
 *
 * Request timeout je 25 s jer prvi dohvat statičkih endpointa (linije,
 * stajališta, vozni red) okida ~16 s hladno parsiranje GTFS-a na backendu;
 * 8 s je bilo prekratko i lista linija bi se prazno učitala. Connect ostaje
 * kratak (5 s) da se brzo detektira nedostupna mreža.
 */
actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        connectTimeoutMillis = 5_000
        requestTimeoutMillis = 25_000
        socketTimeoutMillis = 25_000
    }
}
