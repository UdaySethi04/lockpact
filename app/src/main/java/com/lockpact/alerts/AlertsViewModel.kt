package com.lockpact.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AlertsUiState(
    val alerts: List<AlertFeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AlertsViewModel : ViewModel() {
    private val repository = AlertsRepository()

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getAlerts()
            _uiState.value = if (result.isSuccess) {
                AlertsUiState(alerts = result.getOrDefault(emptyList()), isLoading = false)
            } else {
                AlertsUiState(
                    alerts = emptyList(),
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Unable to load alerts"
                )
            }
        }
    }
}
