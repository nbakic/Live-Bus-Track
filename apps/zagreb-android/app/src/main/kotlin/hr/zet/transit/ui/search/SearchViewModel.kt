package hr.zet.transit.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Route
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.usecase.SearchUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * ViewModel za pretragu (A1.3). Upit se debounce-a prije pokretanja
 * pretrage — tipkanje ne okida pretragu na svaki znak.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    private val search: SearchUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow.debounce(DEBOUNCE_MILLIS).collect { query ->
                val results = search(query)
                _uiState.value = _uiState.value.copy(
                    routes = results.routes,
                    stops = results.stops,
                )
            }
        }
    }

    /** Poziva se na svaku promjenu teksta u polju za pretragu. */
    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        queryFlow.value = query
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 250L
    }
}

/** UI state za pretragu. */
data class SearchUiState(
    val query: String = "",
    val routes: List<Route> = emptyList(),
    val stops: List<Stop> = emptyList(),
)
