package hr.zet.transit.di

import hr.zet.transit.data.local.DatabaseDriverFactory
import hr.zet.transit.data.remote.TransitApiClient
import hr.zet.transit.ui.map.MapViewModel
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

    viewModel { MapViewModel(get()) }
}
