package hr.zet.transit.ui.stop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Arrival
import hr.zet.transit.domain.repository.StaticRepository
import hr.zet.transit.domain.usecase.ObserveArrivalsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za stajalište-detalje (A0.2) — prikaz predviđenih dolazaka.
 *
 * Dolasci dolaze kao Flow (adaptivni polling iza repozitorija); statičko
 * ime stajališta dohvaća se jednom pri otvaranju.
 */
class StopDetailViewModel(
    private val observeArrivals: ObserveArrivalsUseCase,
    private val staticRepository: StaticRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopDetailUiState())
    val uiState: StateFlow<StopDetailUiState> = _uiState.asStateFlow()

    /** Poziva se kad ekran dobije stopId iz navigacije. */
    fun load(stopId: String) {
        viewModelScope.launch {
            val stop = staticRepository.getStop(stopId)
            _uiState.value = _uiState.value.copy(stopName = stop?.name ?: stopId)
        }
        viewModelScope.launch {
            observeArrivals(stopId).collect { arrivals ->
                _uiState.value = _uiState.value.copy(
                    arrivals = arrivals.sortedBy { it.predictedTime },
                    isLoading = false,
                )
            }
        }
    }
}

/** UI state za stajalište-detalje — jedini izvor istine za StopDetailScreen. */
data class StopDetailUiState(
    val stopName: String = "",
    val arrivals: List<Arrival> = emptyList(),
    val isLoading: Boolean = true,
)
