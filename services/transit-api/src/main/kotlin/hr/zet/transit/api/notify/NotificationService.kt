package hr.zet.transit.api.notify

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Push notifikacije (A2.2 plana) — registracija FCM tokena i slanje.
 *
 * **Stanje:** registracija tokena radi i perzistentna je (C4 iz docs/TODO.md);
 * stvarno slanje preko FCM-a traži Firebase projekt i Admin SDK credential.
 * `sendArrivalReminder` je zato namjerno skeleton.
 *
 * Plan (A2.2): backend prati RT feed i šalje FCM podsjetnike. Praćenje po
 * korisniku i RT→FCM pipeline grade se nad ovom osnovom u Fazi A2.
 *
 * Tokeni se drže u memoriji za brz pristup i perzistiraju u datoteku (jedan
 * token po retku) da prežive restart backenda. Datoteka je dovoljna za rani
 * volumen; pri rastu se zamjenjuje pravom bazom.
 */
class NotificationService(
    private val storePath: Path = Path.of(
        System.getenv("FCM_TOKEN_STORE")?.takeIf { it.isNotBlank() } ?: "fcm-tokens.txt",
    ),
) {
    private val log = LoggerFactory.getLogger("notifications")

    /** Registrirani FCM tokeni — u memoriji, sinkronizirani s datotekom. */
    private val tokens = ConcurrentHashMap.newKeySet<String>()

    init {
        loadFromDisk()
    }

    /** Sprema FCM token uređaja za buduće push notifikacije. */
    fun registerToken(token: String) {
        if (token.isBlank()) return
        if (tokens.add(token)) {
            persistToDisk()
            log.info("FCM token registriran (ukupno: ${tokens.size})")
        }
    }

    /** Učitava tokene iz datoteke pri pokretanju; prazno ako datoteke nema. */
    private fun loadFromDisk() {
        runCatching {
            if (Files.exists(storePath)) {
                Files.readAllLines(storePath)
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .forEach(tokens::add)
                log.info("Učitano ${tokens.size} FCM tokena iz ${storePath}")
            }
        }.onFailure { log.error("Neuspjelo učitavanje FCM tokena", it) }
    }

    /** Atomarno zapisuje sve tokene u datoteku. */
    @Synchronized
    private fun persistToDisk() {
        runCatching {
            val tmp = storePath.resolveSibling("${storePath.fileName}.tmp")
            Files.write(tmp, tokens.toList())
            Files.move(tmp, storePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }.onFailure { log.error("Neuspjelo spremanje FCM tokena", it) }
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
