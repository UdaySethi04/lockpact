package com.lockpact.pacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockpact.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PactsUiState(
    val pacts: List<Pact> = emptyList(),
    val selectedPact: Pact? = null,
    val selectedMembership: PactMember? = null,
    val selectedMembers: List<PactMember> = emptyList(),
    val selectedUserProfiles: List<UserProfile> = emptyList(),
    val selectedExposedApps: List<MemberExposedApp> = emptyList(),
    val selectedActiveLocks: List<ActiveLock> = emptyList(),
    val selectedActivityEvents: List<PactActivityEvent> = emptyList(),
    // Home screen
    val myActiveLocks: List<ActiveLock> = emptyList(),
    val myProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val createSuccess: Boolean = false,
    val joinSuccess: Boolean = false,
    val lockSuccess: Boolean = false,
    val lockError: String? = null
)

class PactViewModel : ViewModel() {
    private val repository = PactRepository()

    private val _uiState = MutableStateFlow(PactsUiState())
    val uiState: StateFlow<PactsUiState> = _uiState

    init {
        loadPacts()
    }

    fun loadPacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getMyPacts()
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, pacts = result.getOrDefault(emptyList()))
            } else {
                _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val locksResult = repository.getMyActiveLocks()
            val userId = SessionManager.currentUserId()
            val profileResult = if (userId != null) {
                repository.getUserProfiles(listOf(userId))
            } else null

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                myActiveLocks = locksResult.getOrDefault(emptyList()),
                myProfile = profileResult?.getOrDefault(emptyList())?.firstOrNull()
            )
        }
    }

    fun loadPactDetail(pactId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, error = null,
                selectedPact = null, selectedMembership = null,
                selectedMembers = emptyList(), selectedExposedApps = emptyList(),
                selectedActiveLocks = emptyList(), selectedUserProfiles = emptyList(),
                selectedActivityEvents = emptyList()
            )

            val pactResult = repository.getPactById(pactId)
            if (pactResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = pactResult.exceptionOrNull()?.message
                )
                return@launch
            }

            val membershipResult = repository.getMyMembership(pactId)
            val membersResult = repository.getPactMembers(pactId)
            val members = membersResult.getOrDefault(emptyList())
            val userIds = members.map { it.user_id }

            val profilesResult = repository.getUserProfiles(userIds)
            val appsResult = repository.getExposedAppsForUsers(userIds)
            val locksResult = repository.getActiveLocksForPact(pactId)
            val activityResult = repository.getActivityForPact(pactId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedPact = pactResult.getOrNull(),
                selectedMembership = membershipResult.getOrNull(),
                selectedMembers = members,
                selectedUserProfiles = profilesResult.getOrDefault(emptyList()),
                selectedExposedApps = appsResult.getOrDefault(emptyList()),
                selectedActiveLocks = locksResult.getOrDefault(emptyList()),
                selectedActivityEvents = activityResult.getOrDefault(emptyList()),
                error = null
            )
        }
    }

    fun createLock(
        pactId: String,
        targetUserId: String,
        packageName: String,
        appName: String,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(lockError = null, lockSuccess = false)
            val result = repository.createLock(pactId, targetUserId, packageName, appName, durationMinutes)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(lockSuccess = true)
                loadPactDetail(pactId) // refresh
            } else {
                _uiState.value = _uiState.value.copy(
                    lockError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun createPact(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, createSuccess = false)
            val result = repository.createPact(name)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, createSuccess = true, successMessage = "Pact created")
            } else {
                _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun joinPact(inviteCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, joinSuccess = false)
            val result = repository.joinPact(inviteCode)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, joinSuccess = true, successMessage = "Joined pact")
            } else {
                _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null, lockError = null) }
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(
            successMessage = null, createSuccess = false, joinSuccess = false, lockSuccess = false
        )
    }
}
