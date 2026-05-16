package hr.zet.transit.data.gtfs

import java.security.MessageDigest
import java.util.zip.ZipInputStream

actual class GtfsZipReader actual constructor() {

    actual fun readTextEntries(
        zipBytes: ByteArray,
        entryNames: Set<String>,
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name in entryNames) {
                    result[entry.name] = zip.readBytes().decodeToString()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }

    actual fun sha256(zipBytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(zipBytes)
            .joinToString("") { "%02x".format(it) }
}
