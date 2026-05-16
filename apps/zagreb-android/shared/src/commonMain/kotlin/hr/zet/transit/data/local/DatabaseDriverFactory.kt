package hr.zet.transit.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specifična izrada SQLDelight drivera.
 * `expect` u commonMain; `actual` u androidMain/iosMain — klasičan KMP pattern.
 */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
