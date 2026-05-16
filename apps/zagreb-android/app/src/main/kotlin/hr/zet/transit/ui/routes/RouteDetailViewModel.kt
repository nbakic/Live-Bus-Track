package hr.zet.transit.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.RouteSchedule
import hr.zet.transit.domain.model.RouteShape
import hr.zet.transit.domain.repository.StaticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za detalje linije (A1.1).
 *
 * Trenutno prikazuje osnovne podatke linije iz GTFS static. Geometrija rute
 * (`shapes.txt`, A1.2) i kompletan vozni red (A1.4) dodaju se u Fazi A1.
 */
class RouteDetailViewModel(
    private val staticRepository: StaticRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteDetailUiState())
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    /** Poziva se kad ekran dobije routeId iz navigacije. */
    fun load(routeId: String) {
        viewModelScope.launch {
            val route = staticRepository.getRoute(routeId)
            _uiState.value = _uiState.value.copy(route = route, isLoading = false)
        }
        viewModelScope.launch {
            // Geometrija ide s backenda — može potrajati, učitava se odvojeno.
            val shape = runCatching { staticRepository.getRouteShape(routeId) }.getOrNull()
            _uiState.value = _uiState.value.copy(shape = shape)
        }
        viewModelScope.launch {
            // Vozni red također s backenda, odvojeno od osnovnih podataka.
            val schedule = runCatching { staticRepository.getRouteSchedule(routeId) }.getOrNull()
            _uiState.value = _uiState.value.copy(schedule = schedule)
        }
    }
}

/** UI state za detalje linije. */
data class RouteDetailUiState(
    val route: Route? = null,
    val shape: RouteShape? = null,
    val schedule: RouteSchedule? = null,
    val isLoading: Boolean = true,
)
