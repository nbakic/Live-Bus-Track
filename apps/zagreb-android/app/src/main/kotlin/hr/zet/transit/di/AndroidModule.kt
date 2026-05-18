package hr.zet.transit.di

import hr.zet.transit.BuildConfig
import hr.zet.transit.data.local.DatabaseDriverFactory
import hr.zet.transit.data.remote.TransitApiClient
import hr.zet.transit.ui.alerts.AlertsViewModel
import hr.zet.transit.ui.favorites.FavoritesViewModel
import hr.zet.transit.ui.map.MapViewModel
import hr.zet.transit.push.PushTokenRegistrar
import hr.zet.transit.ui.nearby.LocationProvider
import hr.zet.transit.ui.nearby.NearbyViewModel
import hr.zet.transit.ui.plan.PlanViewModel
import hr.zet.transit.ui.routes.RouteDetailViewModel
import hr.zet.transit.ui.routes.RoutesViewModel
import hr.zet.transit.ui.routines.RoutinesViewModel
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

    // Backend URL dolazi iz build configa (P4) — debug: lokalni, release: prod.
    single { TransitApiClient(baseUrl = BuildConfig.BACKEND_URL) }

    single { LocationProvider(androidContext()) }
    single { PushTokenRegistrar(api = get()) }

    viewModel { MapViewModel(observeVehicles = get(), staticRepository = get()) }
    viewModel {
        StopDetailViewModel(
            observeArrivals = get(),
            staticRepository = get(),
            favoritesRepository = get(),
            toggleFavorite = get(),
            routineRepository = get(),
        )
    }
    viewModel { RoutesViewModel(staticRepository = get()) }
    viewModel { RouteDetailViewModel(staticRepository = get()) }
    viewModel { FavoritesViewModel(favoritesRepository = get(), staticRepository = get()) }
    viewModel { SearchViewModel(search = get()) }
    viewModel { AlertsViewModel(observeAlerts = get()) }
    viewModel { RoutinesViewModel(routineRepository = get(), observeArrivals = get()) }
    viewModel { PlanViewModel(planJourney = get(), search = get()) }
    viewModel {
        NearbyViewModel(
            getNearbyStops = get(),
            staticRepository = get(),
            locationProvider = get(),
        )
    }
}
