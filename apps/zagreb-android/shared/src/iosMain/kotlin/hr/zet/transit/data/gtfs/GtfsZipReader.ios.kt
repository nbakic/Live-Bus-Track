package hr.zet.transit.data.gtfs

/**
 * iOS implementacija GTFS ZIP čitača — dovršava se u Fazi C.
 *
 * Native iOS treba ZIP dekompresiju (npr. preko `Compression` frameworka ili
 * minizip cinteropa) i `CryptoKit` SHA-256. Skeleton svjesno baca grešku
 * umjesto lažne implementacije — iOS UI/data wiring je Faza C (sekcija 13 plana).
 */
actual class GtfsZipReader actual constructor() {

    actual fun readTextEntries(
        zipBytes: ByteArray,
        entryNames: Set<String>,
    ): Map<String, String> =
        throw NotImplementedError("GtfsZipReader: iOS implementacija dolazi u Fazi C.")

    actual fun sha256(zipBytes: ByteArray): String =
        throw NotImplementedError("GtfsZipReader.sha256: iOS implementacija dolazi u Fazi C.")
}
