package hr.zet.transit.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import hr.zet.transit.BuildConfig
import hr.zet.transit.data.gtfs.GtfsImporter
import hr.zet.transit.di.ApiConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Periodički osvježava GTFS static iz backenda (sekcija 2 plana).
 *
 * SHA-256 freshness check je u `GtfsImporter` — Worker se okida svakih 24 h,
 * ali stvaran import se dogodi samo ako se ZIP promijenio. Mrežno ograničenje
 * (CONNECTED) i retry pri grešci dolaze iz WorkManagera.
 */
class GtfsSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val importer: GtfsImporter by inject()

    override suspend fun doWork(): Result =
        when (val outcome = importer.sync(ApiConfig.gtfsStaticZipUrl(BuildConfig.BACKEND_URL))) {
            is GtfsImporter.Result.Imported -> Result.success()
            GtfsImporter.Result.UpToDate -> Result.success()
            // Greška: WorkManager pokuša ponovno; baza ostaje na zadnjem ZIP-u (R2).
            is GtfsImporter.Result.Failed -> Result.retry()
        }

    companion object {
        private const val WORK_NAME = "gtfs-static-sync"

        /** Registrira dnevni sync — idempotentno, poziva se pri startu appa. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GtfsSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
