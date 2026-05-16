package hr.zet.transit.api.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Jednostavan in-memory TTL cache s single-flight semantikom.
 *
 * Single-flight: kad TTL istekne, samo JEDAN coroutine osvježava feed dok
 * ostali čekaju isti rezultat. Time backend troši ZET rate-limit kao jedan
 * potrošač, neovisno o broju klijenata (ključno za D1, sekcija 5 plana).
 */
class TtlCache<T>(
    private val ttlMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
    private val loader: suspend () -> T,
) {
    private data class Entry<T>(val value: T, val loadedAt: Long)

    private val mutex = Mutex()
    private var entry: Entry<T>? = null

    /** Vraća cached vrijednost ili je osvježava ako je TTL istekao. */
    suspend fun get(): CachedValue<T> {
        entry?.let { if (!isStale(it)) return it.toCachedValue() }
        return mutex.withLock {
            // Drugi coroutine je možda već osvježio dok smo čekali lock.
            entry?.let { if (!isStale(it)) return it.toCachedValue() }
            val fresh = Entry(loader(), clock())
            entry = fresh
            fresh.toCachedValue()
        }
    }

    private fun isStale(e: Entry<T>): Boolean = clock() - e.loadedAt >= ttlMillis

    private fun Entry<T>.toCachedValue() =
        CachedValue(value = value, fetchedAtEpochSeconds = loadedAt / 1000)
}

/** Cached vrijednost + meta o svježini — backend ga pretvara u FeedResponse. */
data class CachedValue<T>(
    val value: T,
    val fetchedAtEpochSeconds: Long,
)
