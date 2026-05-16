package hr.zet.transit.ui.stop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Arrival
import hr.zet.transit.domain.model.Routine
import hr.zet.transit.domain.model.RoutineKind
import hr.zet.transit.domain.repository.FavoritesRepository
import hr.zet.transit.domain.repository.RoutineRepository
import hr.zet.transit.domain.repository.StaticRepository
import hr.zet.transit.domain.usecase.ObserveArrivalsUseCase
import hr.zet.transit.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za stajalište-detalje (A0.2 + A0.3).
 *
 * Dolasci dolaze kao Flow (adaptivni polling iza repozitorija); statičko
 * ime stajališta dohvaća se jednom; favorite status prati Flow iz baze.
 */
class StopDetailViewModel(
    private val observeArrivals: ObserveArrivalsUseCase,
    private val staticRepository: StaticRepository,
    private val favoritesRepository: FavoritesRepository,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val routineRepository: RoutineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopDetailUiState())
    val uiState: StateFlow<StopDetailUiState> = _uiState.asStateFlow()

    private var stopId: String = ""
    private var stopName: String = ""

    /** Poziva se kad ekran dobije stopId iz navigacije. */
    fun load(stopId: String) {
        this.stopId = stopId
        viewModelScope.launch {
            val stop = staticRepository.getStop(stopId)
            stopName = stop?.name ?: stopId
            _uiState.value = _uiState.value.copy(stopName = stopName)
        }
        viewModelScope.launch {
            observeArrivals(stopId).collect { arrivals ->
                _uiState.value = _uiState.value.copy(
                    arrivals = arrivals.sortedBy { it.predictedTime },
                    isLoading = false,
                )
            }
        }
        viewModelScope.launch {
            favoritesRepository.observeFavoriteStopIds().collect { favorites ->
                _uiState.value = _uiState.value.copy(isFavorite = stopId in favorites)
            }
        }
    }

    /** Toggle omiljenog statusa trenutnog stajališta (A0.3). */
    fun onToggleFavorite() {
        if (stopId.isBlank()) return
        viewModelScope.launch { toggleFavorite(stopId) }
    }

    /** Postavlja trenutno stajalište kao rutinu zadane vrste (A1.5). */
    fun onSetRoutine(kind: RoutineKind) {
        if (stopId.isBlank()) return
        viewModelScope.launch {
            routineRepository.setRoutine(
                Routine(kind = kind, stopId = stopId, stopName = stopName),
            )
        }
    }
}

/** UI state za stajalište-detalje — jedini izvor istine za StopDetailScreen. */
data class StopDetailUiState(
    val stopName: String = "",
    val arrivals: List<Arrival> = emptyList(),
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
)
