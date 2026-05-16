package hr.zet.transit.domain

/**
 * Normalizira tekst za dijakritika-neutralnu pretragu (A1.3 plana).
 *
 * "Jelačića" i "Jelacica" moraju se podudarati. Pokriva hrvatske znakove
 * (č ć š ž đ) plus uobičajene s drugih jezika — dovoljno za GTFS imena
 * stajališta i pretragu korisnika.
 */
object TextNormalizer {

    private val diacriticMap = mapOf(
        'č' to 'c', 'ć' to 'c', 'š' to 's', 'ž' to 'z', 'đ' to 'd',
        'á' to 'a', 'à' to 'a', 'â' to 'a', 'ä' to 'a',
        'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
        'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'ö' to 'o',
        'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
        'ñ' to 'n',
    )

    /** Lowercase + uklonjena dijakritika — kanonski oblik za usporedbu. */
    fun normalize(text: String): String =
        text.lowercase()
            .map { diacriticMap[it] ?: it }
            .joinToString("")

    /** true ako normalizirani [query] dio normaliziranog [text]. */
    fun matches(text: String, query: String): Boolean =
        normalize(text).contains(normalize(query))
}
