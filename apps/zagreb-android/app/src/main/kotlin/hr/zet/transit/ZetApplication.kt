package hr.zet.transit

import android.app.Application
import hr.zet.transit.di.androidModule
import hr.zet.transit.di.sharedModule
import hr.zet.transit.work.GtfsSyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ZetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ZetApplication)
            modules(sharedModule, androidModule)
        }
        // Dnevni GTFS static sync (idempotentno — KEEP politika).
        GtfsSyncWorker.schedule(this)
    }
}
