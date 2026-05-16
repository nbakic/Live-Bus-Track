package hr.zet.transit.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import hr.zet.transit.data.local.db.TransitDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(TransitDatabase.Schema, "transit.db")
}
