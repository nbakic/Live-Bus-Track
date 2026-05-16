package hr.zet.transit.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.repository.FavoritesRepository
import hr.zet.transit.domain.repository.StaticRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za popis omiljenih stajališta (A0.3).
 *
 * Prati Flow ID-jeva iz baze; za svaki ID dohvaća puni Stop iz statičke baze.
 * Stajališta čije podatke statička baza ne zna (još nije importan GTFS) se
 * preskaču — popis prikazuje samo ono što se može smisleno prikazati.
 */
class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val staticRepository: StaticRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.observeFavoriteStopIds().collect { ids ->
                val stops = ids.mapNotNull { staticRepository.getStop(it) }
                    .sortedBy { it.name }
                _uiState.value = FavoritesUiState(stops = stops, isLoading = false)
            }
        }
    }
}

/** UI state za popis omiljenih. */
data class FavoritesUiState(
    val stops: List<Stop> = emptyList(),
    val isLoading: Boolean = true,
)
