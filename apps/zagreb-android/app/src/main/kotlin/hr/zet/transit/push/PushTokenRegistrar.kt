package hr.zet.transit.push

import hr.zet.transit.data.remote.TransitApiClient

/**
 * Registrira FCM token uređaja na `transit-api` backend (A2.2 plana).
 *
 * **Stanje:** registracijski put (token → backend) je gotov. Što NEDOSTAJE
 * za pravi push: Firebase projekt + `google-services.json` + Firebase plugin
 * i `FirebaseMessagingService` koji dohvaća token i prima poruke.
 *
 * Namjerno NE dodajemo Firebase Gradle plugin bez `google-services.json` —
 * plugin bez te datoteke ruši build. Kad Firebase projekt bude postavljen:
 *  1. dodati `com.google.gms.google-services` plugin + `firebase-messaging`,
 *  2. `FirebaseMessagingService.onNewToken` → [register],
 *  3. obrada dolaznih poruka (podsjetnici, promjene) — A2.2.
 */
class PushTokenRegistrar(
    private val api: TransitApiClient,
) {
    /** Šalje FCM token backendu. Poziva se iz FirebaseMessagingService-a. */
    suspend fun register(token: String) {
        runCatching { api.registerNotificationToken(token) }
        // Greška se proguta — registracija se ponovi pri sljedećem onNewToken.
    }
}
