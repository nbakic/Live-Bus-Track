package hr.zet.transit.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.JourneyPlan
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.usecase.PlanJourneyUseCase
import hr.zet.transit.domain.usecase.SearchUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za planiranje rute A→B (A2.1).
 *
 * Korisnik bira polazno i odredišno stajalište pretragom; plan se računa na
 * backendu (GraphHopper pt) i vraća varijante s presjedanjima.
 */
class PlanViewModel(
    private val planJourney: PlanJourneyUseCase,
    private val search: SearchUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    /** Pretražuje stajališta za odabir polazišta/odredišta. */
    fun onSearchQueryChange(query: String, target: PlanField) {
        _uiState.value = when (target) {
            PlanField.FROM -> _uiState.value.copy(fromQuery = query)
            PlanField.TO -> _uiState.value.copy(toQuery = query)
        }
        viewModelScope.launch {
            val results = search(query).stops
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                activeField = target,
            )
        }
    }

    /** Korisnik je odabrao stajalište iz rezultata pretrage. */
    fun onStopSelected(stop: Stop) {
        _uiState.value = when (_uiState.value.activeField) {
            PlanField.FROM -> _uiState.value.copy(
                fromStop = stop,
                fromQuery = stop.name,
                searchResults = emptyList(),
            )
            PlanField.TO, null -> _uiState.value.copy(
                toStop = stop,
                toQuery = stop.name,
                searchResults = emptyList(),
            )
        }
    }

    /** Pokreće planiranje kad su oba stajališta odabrana. */
    fun onPlanRequested() {
        val from = _uiState.value.fromStop ?: return
        val to = _uiState.value.toStop ?: return
        _uiState.value = _uiState.value.copy(isPlanning = true, plans = emptyList())
        viewModelScope.launch {
            val plans = planJourney(
                fromLat = from.position.lat, fromLng = from.position.lng,
                toLat = to.position.lat, toLng = to.position.lng,
            )
            _uiState.value = _uiState.value.copy(isPlanning = false, plans = plans)
        }
    }
}

/** Polje koje se trenutno uređuje — polazište ili odredište. */
enum class PlanField { FROM, TO }

/** UI state za planiranje rute. */
data class PlanUiState(
    val fromQuery: String = "",
    val toQuery: String = "",
    val fromStop: Stop? = null,
    val toStop: Stop? = null,
    val activeField: PlanField? = null,
    val searchResults: List<Stop> = emptyList(),
    val isPlanning: Boolean = false,
    val plans: List<JourneyPlan> = emptyList(),
) {
    /** Plan je moguć tek kad su oba stajališta odabrana. */
    val canPlan: Boolean get() = fromStop != null && toStop != null
}
