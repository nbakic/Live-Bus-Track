package hr.zet.transit.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.Stop
import hr.zet.transit.domain.repository.StaticRepository
import hr.zet.transit.domain.usecase.GetNearbyStopsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za "najbliža stanica" (A0.5 + A1.6).
 *
 * Kandidate bira zračnom udaljenošću (GetNearbyStopsUseCase), zatim za njih
 * dohvaća PJEŠAČKU udaljenost preko backenda (OSRM foot, A1.6) i sortira po
 * njoj — zračna linija ne uzima u obzir pješačke putove (plan, sekcija 11).
 */
class NearbyViewModel(
    private val getNearbyStops: GetNearbyStopsUseCase,
    private val staticRepository: StaticRepository,
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
            val (lat, lng) = location
            val candidates = getNearbyStops(lat = lat, lng = lng)

            // Pješačka udaljenost po kandidatu — paralelno, jedan backend upit svaki.
            val withWalk = candidates.map { stop ->
                async {
                    val walk = staticRepository.getWalkRoute(
                        fromLat = lat, fromLng = lng,
                        toLat = stop.position.lat, toLng = stop.position.lng,
                    )
                    NearbyStop(
                        stop = stop,
                        walkMeters = walk?.distanceMeters?.toInt(),
                        walkMinutes = walk?.durationSeconds?.let { (it / 60).toInt() },
                    )
                }
            }.awaitAll()

            // Sortiraj po pješačkoj udaljenosti; stajališta bez pješačkog
            // podatka idu na kraj (fallback na redoslijed po zračnoj liniji).
            _uiState.value = NearbyUiState(
                status = NearbyStatus.READY,
                stops = withWalk.sortedBy { it.walkMeters ?: Int.MAX_VALUE },
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

/** Stajalište s pješačkom udaljenošću; walk* je null ako routing nije uspio. */
data class NearbyStop(
    val stop: Stop,
    val walkMeters: Int?,
    val walkMinutes: Int?,
)

/** UI state za "najbliža stanica". */
data class NearbyUiState(
    val status: NearbyStatus = NearbyStatus.LOADING,
    val stops: List<NearbyStop> = emptyList(),
)
