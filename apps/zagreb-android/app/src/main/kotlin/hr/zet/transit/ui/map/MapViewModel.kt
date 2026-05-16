package hr.zet.transit.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Vehicle
import hr.zet.transit.domain.usecase.ObserveVehiclesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za kartu (A0.1). MVVM state holder — drži [MapUiState],
 * domain logiku delegira use-caseu. Vidi sekciju 4.1 plana.
 */
class MapViewModel(
    private val observeVehicles: ObserveVehiclesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        observeVehicleStream()
    }

    private fun observeVehicleStream() {
        viewModelScope.launch {
            observeVehicles().collect { vehicles ->
                _uiState.value = _uiState.value.copy(
                    vehicles = vehicles,
                    isLoading = false,
                )
            }
        }
    }
}

/** UI state za kartu — immutable, jedini izvor istine za MapScreen. */
data class MapUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = true,
    /** false kad RT feed nedostaje — UI prikazuje "Podaci uživo nedostupni". */
    val isLive: Boolean = true,
)
