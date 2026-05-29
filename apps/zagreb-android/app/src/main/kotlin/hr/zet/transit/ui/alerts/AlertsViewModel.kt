package hr.zet.transit.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.zet.transit.domain.model.AlertSeverity
import hr.zet.transit.domain.model.ServiceAlert
import hr.zet.transit.domain.usecase.ObserveAlertsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel za prometne obavijesti (A0.4).
 *
 * Alerts dolaze kao Flow iz RT feeda; sortirani po ozbiljnosti (najteži prvi)
 * jer korisnika prvo zanima ono što ga najviše pogađa.
 */
class AlertsViewModel(
    private val observeAlerts: ObserveAlertsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAlerts().collect { feed ->
                _uiState.value = AlertsUiState(
                    alerts = feed.data.sortedByDescending { it.severity.ordinal },
                    isLive = feed.isLive,
                    isLoading = false,
                )
            }
        }
    }
}

/** UI state za prometne obavijesti. */
data class AlertsUiState(
    val alerts: List<ServiceAlert> = emptyList(),
    val isLoading: Boolean = true,
    /** False kad backend ne odgovara — UI prikazuje degradiranu poruku. */
    val isLive: Boolean = true,
)
