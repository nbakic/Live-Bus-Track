package hr.zet.transit.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.data.ConnectivityChecker
import hr.zet.transit.data.gtfs.GtfsImporter
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.TransitMode
import hr.zet.transit.domain.repository.StaticRepository
import hr.zet.transit.ui.common.LoadError
import hr.zet.transit.ui.common.classifyLoadError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za pregled svih linija (A1.1).
 *
 * Linije žive u lokalnoj bazi koju puni GTFS sync. Ako je baza prazna (npr.
 * prvi start prije nego je periodični sync stigao odraditi), pokušamo
 * jednokratni sync odmah. Pri neuspjehu razlikujemo problem s korisnikovom
 * vezom od problema s našim poslužiteljem, pa UI prikaže pravu poruku.
 */
class RoutesViewModel(
    private val staticRepository: StaticRepository,
    private val importer: GtfsImporter,
    private val connectivity: ConnectivityChecker,
    private val gtfsZipUrl: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = RoutesUiState(isLoading = true)

            var routes = staticRepository.getRoutes()
            var error: LoadError? = null

            if (routes.isEmpty()) {
                if (!connectivity.isOnline()) {
                    error = LoadError.NO_INTERNET
                } else {
                    when (importer.sync(gtfsZipUrl)) {
                        is GtfsImporter.Result.Imported,
                        GtfsImporter.Result.UpToDate -> routes = staticRepository.getRoutes()
                        is GtfsImporter.Result.Failed ->
                            error = classifyLoadError(online = connectivity.isOnline())
                    }
                }
            }

            _uiState.value = RoutesUiState(
                routes = routes.sortedWith(routeOrder),
                isLoading = false,
                error = error,
            )
        }
    }

    private companion object {
        /** Tramvaji prije autobusa; unutar grupe numerički, pa abecedno. */
        val routeOrder = compareBy<Route>(
            { if (it.mode == TransitMode.TRAM) 0 else 1 },
            { it.shortName.toIntOrNull() ?: Int.MAX_VALUE },
            { it.shortName },
        )
    }
}

/** UI state za pregled linija. */
data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val isLoading: Boolean = true,
    val error: LoadError? = null,
)
