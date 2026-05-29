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
        // Odmah napuni bazu na prvom pokretanju (jednokratno), pa registriraj
        // dnevni sync za svježinu (oba idempotentna — KEEP politika).
        GtfsSyncWorker.syncNow(this)
        GtfsSyncWorker.schedule(this)
    }
}
