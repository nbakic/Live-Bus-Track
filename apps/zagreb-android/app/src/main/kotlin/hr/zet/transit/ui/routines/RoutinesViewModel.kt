package hr.zet.transit.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Arrival
import hr.zet.transit.domain.model.Routine
import hr.zet.transit.domain.model.RoutineKind
import hr.zet.transit.domain.repository.RoutineRepository
import hr.zet.transit.domain.usecase.ObserveArrivalsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za rutine i jutarnji ekran (A1.5).
 *
 * Prati rutine iz baze; za svaku rutinu drži live dolaske s njezina
 * stajališta. Dnevni putnik tako na jednom mjestu vidi "kad mi kreće
 * sljedeći s Doma / Posla / Škole" — informacija koja ga dočeka.
 */
class RoutinesViewModel(
    private val routineRepository: RoutineRepository,
    private val observeArrivals: ObserveArrivalsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutinesUiState())
    val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

    /** Aktivni collectori dolazaka po rutini — gase se kad rutina nestane. */
    private val arrivalJobs = mutableMapOf<RoutineKind, Job>()

    init {
        viewModelScope.launch {
            routineRepository.observeRoutines().collect { routines ->
                _uiState.value = _uiState.value.copy(
                    routines = routines,
                    isLoading = false,
                )
                refreshArrivalSubscriptions(routines)
            }
        }
    }

    /** Pretplaća se na dolaske za stajališta aktualnih rutina. */
    private fun refreshArrivalSubscriptions(routines: List<Routine>) {
        val activeKinds = routines.map { it.kind }.toSet()
        // Otkaži collectore za rutine kojih više nema.
        arrivalJobs.keys.filterNot { it in activeKinds }.forEach { kind ->
            arrivalJobs.remove(kind)?.cancel()
            _uiState.value = _uiState.value.copy(
                arrivalsByKind = _uiState.value.arrivalsByKind - kind,
                liveByKind = _uiState.value.liveByKind - kind,
            )
        }
        // Pokreni collectore za nove rutine.
        routines.filter { it.kind !in arrivalJobs }.forEach { routine ->
            arrivalJobs[routine.kind] = viewModelScope.launch {
                observeArrivals(routine.stopId).collect { feed ->
                    _uiState.value = _uiState.value.copy(
                        arrivalsByKind = _uiState.value.arrivalsByKind +
                            (routine.kind to feed.data.sortedBy { it.predictedTime }.take(3)),
                        liveByKind = _uiState.value.liveByKind +
                            (routine.kind to feed.isLive),
                    )
                }
            }
        }
    }

    fun setRoutine(routine: Routine) {
        viewModelScope.launch { routineRepository.setRoutine(routine) }
    }

    fun clearRoutine(kind: RoutineKind) {
        viewModelScope.launch { routineRepository.clearRoutine(kind) }
    }
}

/** UI state za rutine i jutarnji ekran. */
data class RoutinesUiState(
    val routines: List<Routine> = emptyList(),
    /** Sljedeći dolasci po vrsti rutine — najviše 3 po rutini. */
    val arrivalsByKind: Map<RoutineKind, List<Arrival>> = emptyMap(),
    /**
     * Je li RT feed te rutine živ. False kad backend ne odgovara — kartica tad
     * razlikuje "nema dolazaka" od "ne mogu dohvatiti dolaske" (C8). Nedostatak
     * ključa = još nije stigao prvi snapshot (tretiramo kao živo/učitavanje).
     */
    val liveByKind: Map<RoutineKind, Boolean> = emptyMap(),
    val isLoading: Boolean = true,
)
