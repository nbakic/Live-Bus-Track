package hr.zet.transit.data.gtfs

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun nowEpochSeconds(): Long = NSDate().timeIntervalSince1970.toLong()
