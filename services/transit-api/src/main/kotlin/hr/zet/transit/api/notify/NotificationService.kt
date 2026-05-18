package hr.zet.transit.api.notify

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Push notifikacije (A2.2 plana) — registracija FCM tokena i slanje.
 *
 * **Stanje:** registracija tokena radi (in-memory); stvarno slanje preko FCM-a
 * traži Firebase projekt i Admin SDK credential. `sendArrivalReminder` je
 * zato namjerno skeleton — implementira se kad Firebase projekt postoji.
 *
 * Plan (A2.2): backend prati RT feed i šalje FCM podsjetnike. Praćenje po
 * korisniku i RT→FCM pipeline grade se nad ovom osnovom u Fazi A2.
 *
 * In-memory store je privremen — produkcija treba perzistentnu pohranu
 * (tokeni moraju preživjeti restart backenda).
 */
class NotificationService {

    private val log = LoggerFactory.getLogger("notifications")

    /** Registrirani FCM tokeni. Privremeno in-memory (vidi napomenu iznad). */
    private val tokens = ConcurrentHashMap.newKeySet<String>()

    /** Sprema FCM token uređaja za buduće push notifikacije. */
    fun registerToken(token: String) {
        if (token.isBlank()) return
        tokens.add(token)
        log.info("FCM token registriran (ukupno: ${tokens.size})")
    }

    /**
     * Šalje podsjetnik o dolasku na uređaj.
     *
     * TODO(A2.2): implementirati preko Firebase Admin SDK-a kad Firebase
     * projekt bude postavljen. Trenutno samo logira — namjerno nije lažni
     * push koji tiho ne radi.
     */
    fun sendArrivalReminder(token: String, message: String) {
        log.warn(
            "sendArrivalReminder pozvan, ali FCM slanje još nije konfigurirano " +
                "(token=${token.take(8)}…, message=$message)",
        )
    }
}

/** Tijelo zahtjeva za /v1/notifications/register. */
@Serializable
data class RegisterTokenRequest(
    val token: String,
)
