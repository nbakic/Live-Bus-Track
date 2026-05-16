package hr.zet.transit.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.usecase.GetNearbyStopsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za "najbliža stanica" (A0.5).
 *
 * Koristi zračnu udaljenost (GetNearbyStopsUseCase) — plan izričito kaže da
 * pravi pješački routing dolazi tek u A1.6, i da se pješački put ne smije
 * obećavati u A0 UI-u.
 */
class NearbyViewModel(
    private val getNearbyStops: GetNearbyStopsUseCase,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    /** Poziva se kad korisnik dodijeli lokacijsku dozvolu i otvori ekran. */
    fun loadNearby() {
        _uiState.value = NearbyUiState(status = NearbyStatus.LOADING)
        viewModelScope.launch {
            val location = locationProvider.lastKnownLocation()
            if (location == null) {
                _uiState.value = NearbyUiState(status = NearbyStatus.NO_LOCATION)
                return@launch
            }
            val stops = getNearbyStops(lat = location.first, lng = location.second)
            _uiState.value = NearbyUiState(
                status = NearbyStatus.READY,
                stops = stops,
            )
        }
    }

    /** Poziva se kad korisnik odbije lokacijsku dozvolu. */
    fun onPermissionDenied() {
        _uiState.value = NearbyUiState(status = NearbyStatus.PERMISSION_DENIED)
    }
}

enum class NearbyStatus {
    LOADING,
    READY,
    NO_LOCATION,
    PERMISSION_DENIED,
}

/** UI state za "najbliža stanica". */
data class NearbyUiState(
    val status: NearbyStatus = NearbyStatus.LOADING,
    val stops: List<Stop> = emptyList(),
)
