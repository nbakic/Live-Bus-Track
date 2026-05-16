package hr.zet.transit.data.repository

import kotlin.test.Test
import kotlin.test.assertTrue

class GeoTest {

    @Test
    fun haversine_samePoint_isZero() {
        val d = haversineMeters(45.8150, 15.9819, 45.8150, 15.9819)
        assertTrue(d < 0.001, "Ista točka mora dati ~0 m, dobiveno $d")
    }

    @Test
    fun haversine_zagrebLandmarks_isPlausible() {
        // Trg bana Jelačića → Glavni kolodvor ≈ 850 m zračne linije.
        val jelacic = 45.8131 to 15.9775
        val kolodvor = 45.8047 to 15.9786
        val d = haversineMeters(jelacic.first, jelacic.second, kolodvor.first, kolodvor.second)
        assertTrue(d in 700.0..1000.0, "Očekivano ~850 m, dobiveno $d")
    }
}
