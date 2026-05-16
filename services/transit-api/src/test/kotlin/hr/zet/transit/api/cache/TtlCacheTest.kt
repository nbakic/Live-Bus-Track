package hr.zet.transit.api.cache

import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class TtlCacheTest {

    @Test
    fun servesCachedValueWithinTtl() = runTest {
        val loads = AtomicInteger(0)
        var now = 0L
        val cache = TtlCache(ttlMillis = 1000, clock = { now }) {
            loads.incrementAndGet()
        }

        cache.get()
        now = 500   // unutar TTL-a
        cache.get()

        assertEquals(1, loads.get(), "Unutar TTL-a feed se ne smije ponovno dohvatiti")
    }

    @Test
    fun reloadsAfterTtlExpires() = runTest {
        val loads = AtomicInteger(0)
        var now = 0L
        val cache = TtlCache(ttlMillis = 1000, clock = { now }) {
            loads.incrementAndGet()
        }

        cache.get()
        now = 1500  // TTL istekao
        cache.get()

        assertEquals(2, loads.get(), "Nakon isteka TTL-a feed se mora ponovno dohvatiti")
    }

    @Test
    fun exposesFetchedAtTimestamp() = runTest {
        var now = 42_000L
        val cache = TtlCache(ttlMillis = 1000, clock = { now }) { "data" }

        val result = cache.get()

        assertEquals(42L, result.fetchedAtEpochSeconds)
        assertEquals("data", result.value)
    }
}
