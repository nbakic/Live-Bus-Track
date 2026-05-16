package hr.zet.transit.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextNormalizerTest {

    @Test
    fun matches_isDiacriticInsensitive() {
        // Ključni slučaj iz plana: "Jelacica" mora naći "Jelačića".
        assertTrue(TextNormalizer.matches("Trg bana Jelačića", "jelacica"))
        assertTrue(TextNormalizer.matches("Črnomerec", "crnomerec"))
        assertTrue(TextNormalizer.matches("Žitnjak", "zitnjak"))
    }

    @Test
    fun matches_isCaseInsensitive() {
        assertTrue(TextNormalizer.matches("Glavni Kolodvor", "KOLODVOR"))
    }

    @Test
    fun matches_worksOnPartialSubstring() {
        assertTrue(TextNormalizer.matches("Savski most", "savs"))
    }

    @Test
    fun matches_returnsFalseForNonMatch() {
        assertFalse(TextNormalizer.matches("Dubrava", "maksimir"))
    }
}
