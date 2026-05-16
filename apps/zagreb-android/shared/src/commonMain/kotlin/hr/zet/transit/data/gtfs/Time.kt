package hr.zet.transit.data.gtfs

/** Trenutno vrijeme u Unix sekundama — platform-specifičan izvor sata. */
internal expect fun nowEpochSeconds(): Long
