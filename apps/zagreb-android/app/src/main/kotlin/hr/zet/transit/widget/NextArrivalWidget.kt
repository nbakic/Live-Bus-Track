package hr.zet.transit.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import hr.zet.transit.R
import hr.zet.transit.domain.repository.FavoritesRepository
import hr.zet.transit.domain.repository.StaticRepository
import hr.zet.transit.domain.usecase.ObserveArrivalsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Home-screen widget (A0.7) — sljedeći dolazak na omiljenom stajalištu.
 *
 * Primarna persona je dnevni putnik: treba informaciju koja ga dočeka, ne
 * app koji mora otvarati (plan sekcija 11). Widget prikazuje prvo omiljeno
 * stajalište; ako ih nema, poziva korisnika da doda omiljeno.
 *
 * Osvježava se periodički (`updatePeriodMillis` u widget-info XML-u) i pri
 * svakom `onUpdate`. Mreža se dohvaća na IO scopeu — `goAsync` nije nužan
 * jer RemoteViews update stigne brzo nakon dohvaćanja.
 */
class NextArrivalWidget : AppWidgetProvider(), KoinComponent {

    private val favoritesRepository: FavoritesRepository by inject()
    private val staticRepository: StaticRepository by inject()
    private val observeArrivals: ObserveArrivalsUseCase by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { widgetId ->
            scope.launch { renderWidget(context, appWidgetManager, widgetId) }
        }
    }

    private suspend fun renderWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_next_arrival)

        val favoriteStopId = favoritesRepository.observeFavoriteStopIds()
            .first()
            .firstOrNull()

        if (favoriteStopId == null) {
            views.setTextViewText(R.id.widget_stop_name, "ZET")
            views.setTextViewText(R.id.widget_route_eta, "Dodaj omiljeno stajalište")
            views.setTextViewText(R.id.widget_updated, "")
            manager.updateAppWidget(widgetId, views)
            return
        }

        val stopName = staticRepository.getStop(favoriteStopId)?.name ?: favoriteStopId
        val arrivals = observeArrivals(favoriteStopId).first().data
        val next = arrivals.minByOrNull { it.predictedTime }

        views.setTextViewText(R.id.widget_stop_name, stopName)
        views.setTextViewText(
            R.id.widget_route_eta,
            if (next != null) {
                "${next.routeShortName} • ${formatWidgetEta(next.predictedTime)}"
            } else {
                "Nema najavljenih dolazaka"
            },
        )
        views.setTextViewText(
            R.id.widget_updated,
            if (next?.isRealtime == true) "uživo" else "po redu vožnje",
        )
        manager.updateAppWidget(widgetId, views)
    }
}

/** Kratak ETA za skučen widget prostor. */
internal fun formatWidgetEta(
    predictedEpochSeconds: Long,
    nowEpochSeconds: Long = System.currentTimeMillis() / 1000,
): String {
    val minutes = (predictedEpochSeconds - nowEpochSeconds) / 60
    return when {
        minutes <= 0 -> "stiže"
        else -> "za $minutes min"
    }
}
