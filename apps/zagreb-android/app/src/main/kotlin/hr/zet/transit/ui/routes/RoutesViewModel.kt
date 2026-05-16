package hr.zet.transit.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.TransitMode
import hr.zet.transit.domain.repository.StaticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za pregled svih linija (A1.1).
 *
 * Linije se učitavaju jednom iz statičke baze i sortiraju: tramvaji prije
 * autobusa, unutar grupe numerički po oznaci linije.
 */
class RoutesViewModel(
    private val staticRepository: StaticRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val routes = staticRepository.getRoutes().sortedWith(routeOrder)
            _uiState.value = RoutesUiState(routes = routes, isLoading = false)
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
)
