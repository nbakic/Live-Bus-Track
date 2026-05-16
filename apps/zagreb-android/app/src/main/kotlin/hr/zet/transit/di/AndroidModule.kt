package hr.zet.transit.di

import hr.zet.transit.data.local.DatabaseDriverFactory
import hr.zet.transit.data.remote.TransitApiClient
import hr.zet.transit.ui.alerts.AlertsViewModel
import hr.zet.transit.ui.favorites.FavoritesViewModel
import hr.zet.transit.ui.map.MapViewModel
import hr.zet.transit.ui.routes.RouteDetailViewModel
import hr.zet.transit.ui.routes.RoutesViewModel
import hr.zet.transit.ui.search.SearchViewModel
import hr.zet.transit.ui.stop.StopDetailViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Android-specifične ovisnosti: SQLDelight driver (Android), Ktor OkHttp
 * engine, ViewModeli. Vidi `sharedModule` za platform-neutralni dio.
 */
val androidModule = module {
    single { DatabaseDriverFactory(androidContext()).create() }

    single { TransitApiClient(baseUrl = ApiConfig.LOCAL) }

    viewModel { MapViewModel(observeVehicles = get(), staticRepository = get()) }
    viewModel {
        StopDetailViewModel(
            observeArrivals = get(),
            staticRepository = get(),
            favoritesRepository = get(),
            toggleFavorite = get(),
        )
    }
    viewModel { RoutesViewModel(staticRepository = get()) }
    viewModel { RouteDetailViewModel(staticRepository = get()) }
    viewModel { FavoritesViewModel(favoritesRepository = get(), staticRepository = get()) }
    viewModel { SearchViewModel(search = get()) }
    viewModel { AlertsViewModel(observeAlerts = get()) }
}
