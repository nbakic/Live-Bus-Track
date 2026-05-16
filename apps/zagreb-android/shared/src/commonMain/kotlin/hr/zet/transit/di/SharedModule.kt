package hr.zet.transit.di

import hr.zet.transit.data.gtfs.GtfsImporter
import hr.zet.transit.data.local.db.TransitDatabase
import hr.zet.transit.data.remote.TransitApiClient
import hr.zet.transit.data.remote.createPlatformHttpClient
import hr.zet.transit.data.repository.FavoritesRepositoryImpl
import hr.zet.transit.data.repository.RealtimeRepositoryImpl
import hr.zet.transit.data.repository.StaticRepositoryImpl
import hr.zet.transit.domain.repository.FavoritesRepository
import hr.zet.transit.domain.repository.RealtimeRepository
import hr.zet.transit.domain.repository.StaticRepository
import hr.zet.transit.domain.usecase.GetNearbyStopsUseCase
import hr.zet.transit.domain.usecase.ObserveArrivalsUseCase
import hr.zet.transit.domain.usecase.ObserveVehiclesUseCase
import hr.zet.transit.domain.usecase.ToggleFavoriteUseCase
import org.koin.dsl.module

/**
 * Koin modul za KMP shared sloj — repozitoriji i use-caseovi.
 * Platform moduli (Android `:app`, iOS) dodaju driver/engine ovisnosti
 * te ViewModele / state holdere.
 */
val sharedModule = module {
    single { TransitDatabase(get()) }

    single<StaticRepository> { StaticRepositoryImpl(get()) }
    single<RealtimeRepository> { RealtimeRepositoryImpl(get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }

    // GTFS static importer — dijeli vlastiti HttpClient (download ZIP-a s backenda).
    single { createPlatformHttpClient() }
    single { GtfsImporter(httpClient = get(), db = get()) }

    factory { ObserveVehiclesUseCase(get()) }
    factory { ObserveArrivalsUseCase(get()) }
    factory { GetNearbyStopsUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
}

/**
 * Konfiguracija backenda — base URL `transit-api`-ja po environmentu.
 * `transit-api` mora biti EU-hostan (sekcija 14 plana — regija backenda).
 */
object ApiConfig {
    /** Lokalni dev backend (services/transit-api na portu 8080). */
    const val LOCAL = "http://10.0.2.2:8080"
    /** Placeholder — produkcijski URL postavlja se u Fazi 0. */
    const val PRODUCTION = "https://api.zet-transit.example"
}
