package com.lockpact.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MyAppsUiState(
    val installedApps: List<InstalledApp> = emptyList(),
    val exposedPackageNames: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MyAppsViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = InstalledAppsScanner(application)
    private val repository = MyAppsRepository()

    private val _uiState = MutableStateFlow(MyAppsUiState(isLoading = true))
    val uiState: StateFlow<MyAppsUiState> = _uiState

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val installedApps = cachedInstalledApps ?: withContext(Dispatchers.Default) {
                scanner.scanLaunchableApps()
            }.also { cachedInstalledApps = it }
            val exposedResult = withContext(Dispatchers.IO) {
                repository.getExposedApps()
            }

            _uiState.value = if (exposedResult.isSuccess) {
                MyAppsUiState(
                    installedApps = installedApps,
                    exposedPackageNames = exposedResult.getOrDefault(emptySet()),
                    isLoading = false
                )
            } else {
                MyAppsUiState(
                    installedApps = installedApps,
                    isLoading = false,
                    error = exposedResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun toggleApp(app: InstalledApp) {
        val isExposed = app.packageName in _uiState.value.exposedPackageNames
        _uiState.value = _uiState.value.copy(
            exposedPackageNames = if (isExposed) {
                _uiState.value.exposedPackageNames - app.packageName
            } else {
                _uiState.value.exposedPackageNames + app.packageName
            },
            error = null
        )

        viewModelScope.launch {
            val result = if (isExposed) {
                repository.hideApp(app.packageName)
            } else {
                repository.exposeApp(app)
            }

            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    exposedPackageNames = if (isExposed) {
                        _uiState.value.exposedPackageNames + app.packageName
                    } else {
                        _uiState.value.exposedPackageNames - app.packageName
                    },
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    companion object {
        private var cachedInstalledApps: List<InstalledApp>? = null
    }
}
