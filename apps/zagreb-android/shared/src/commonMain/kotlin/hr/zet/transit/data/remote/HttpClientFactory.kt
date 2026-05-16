package hr.zet.transit.data.remote

import io.ktor.client.HttpClient

/**
 * Platform-specifična izrada Ktor HttpClienta. `expect` u commonMain;
 * `actual` bira engine per-platform (OkHttp na Androidu, Darwin na iOS-u).
 *
 * Time `:app` ne mora ovisiti o Ktoru izravno — engine izbor ostaje u shared.
 */
expect fun createPlatformHttpClient(): HttpClient
