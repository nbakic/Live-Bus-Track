package hr.zet.transit.data.gtfs

/**
 * Čita unutarnje datoteke iz GTFS static ZIP-a i računa njegov SHA-256.
 *
 * ZIP dekompresija i hashiranje su platform-specifični (`java.util.zip` na
 * Androidu, native na iOS-u) — otud `expect`/`actual`.
 */
expect class GtfsZipReader() {

    /**
     * Iz ZIP bajtova izvuče tražene tekstualne datoteke.
     * @param zipBytes sirovi sadržaj `.zip`
     * @param entryNames imena datoteka unutar ZIP-a (npr. `"routes.txt"`)
     * @return mapa ime → UTF-8 sadržaj; datoteke koje ne postoje se izostave
     */
    fun readTextEntries(zipBytes: ByteArray, entryNames: Set<String>): Map<String, String>

    /** SHA-256 ZIP-a kao hex string — primarna provjera svježine (sekcija 2). */
    fun sha256(zipBytes: ByteArray): String
}
