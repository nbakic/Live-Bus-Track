package hr.zet.transit.di

import hr.zet.transit.data.gtfs.GtfsImporter
import hr.zet.transit.data.local.db.TransitDatabase
import hr.zet.transit.data.remote.TransitApiClient
import hr.zet.transit.data.remote.createPlatformHttpClient
import hr.zet.transit.data.repository.FavoritesRepositoryImpl
import hr.zet.transit.data.repository.JourneyRepositoryImpl
import hr.zet.transit.data.repository.RealtimeRepositoryImpl
import hr.zet.transit.data.repository.RoutineRepositoryImpl
import hr.zet.transit.data.repository.StaticRepositoryImpl
import hr.zet.transit.domain.repository.FavoritesRepository
import hr.zet.transit.domain.repository.JourneyRepository
import hr.zet.transit.domain.repository.RealtimeRepository
import hr.zet.transit.domain.repository.RoutineRepository
import hr.zet.transit.domain.repository.StaticRepository
import hr.zet.transit.domain.usecase.GetNearbyStopsUseCase
import hr.zet.transit.domain.usecase.ObserveAlertsUseCase
import hr.zet.transit.domain.usecase.ObserveArrivalsUseCase
import hr.zet.transit.domain.usecase.ObserveVehiclesUseCase
import hr.zet.transit.domain.usecase.PlanJourneyUseCase
import hr.zet.transit.domain.usecase.SearchUseCase
import hr.zet.transit.domain.usecase.ToggleFavoriteUseCase
import org.koin.dsl.module

/**
 * Koin modul za KMP shared sloj — repozitoriji i use-caseovi.
 * Platform moduli (Android `:app`, iOS) dodaju driver/engine ovisnosti
 * te ViewModele / state holdere.
 */
val sharedModule = module {
    single { TransitDatabase(get()) }

    single<StaticRepository> { StaticRepositoryImpl(db = get(), api = get()) }
    single<RealtimeRepository> { RealtimeRepositoryImpl(get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }
    single<RoutineRepository> { RoutineRepositoryImpl(get()) }
    single<JourneyRepository> { JourneyRepositoryImpl(get()) }

    // GTFS static importer — dijeli vlastiti HttpClient (download ZIP-a s backenda).
    single { createPlatformHttpClient() }
    single { GtfsImporter(httpClient = get(), db = get()) }

    factory { ObserveVehiclesUseCase(get()) }
    factory { ObserveArrivalsUseCase(get()) }
    factory { ObserveAlertsUseCase(get()) }
    factory { GetNearbyStopsUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { SearchUseCase(get()) }
    factory { PlanJourneyUseCase(get()) }
}

/**
 * Pomoćne funkcije za backend URL-ove.
 *
 * Bazni backend URL više nije ovdje konstanta — Android sloj ga daje iz
 * build configa (`BuildConfig.BACKEND_URL`, P4): debug → lokalni backend,
 * release → produkcijski. `transit-api` mora biti EU-hostan (sekcija 14 plana).
 */
object ApiConfig {
    /** Backend ZIP proxy za GTFS static — `GtfsImporter` ga koristi. */
    fun gtfsStaticZipUrl(baseUrl: String): String = "$baseUrl/v1/gtfs/static.zip"
}
