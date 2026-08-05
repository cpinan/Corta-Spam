package org.carlospinan.bloqueador.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.settings.SettingsRepository

data class HomeUiState(
    val blockedToday: Int = 0,
    val blockedThisWeek: Int = 0,
    val blockedThisMonth: Int = 0,
    val pendingReview: Int = 0,
    val isLoading: Boolean = true,
    val blockingEnabled: Boolean = true,
)

sealed interface HomeIntent {
    data object Refresh : HomeIntent

    data class ToggleBlocking(
        val enabled: Boolean,
    ) : HomeIntent
}

class HomeViewModel(
    private val callLogRepository: CallLogRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // changes() emits immediately, so this covers the initial load as well as every later
        // call. Home used to read the counters once and then go stale: a call blocked while the
        // dashboard was on screen didn't show up until the screen was recreated.
        viewModelScope.launch {
            callLogRepository.changes().collect { refresh() }
        }
        viewModelScope.launch {
            settingsRepository.blockingEnabled.collect { enabled ->
                _state.value = _state.value.copy(blockingEnabled = enabled)
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refresh()
            is HomeIntent.ToggleBlocking -> toggleBlocking(intent.enabled)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val stats = callLogRepository.blockedStats()
            _state.value =
                _state.value.copy(
                    blockedToday = stats.today,
                    blockedThisWeek = stats.thisWeek,
                    blockedThisMonth = stats.thisMonth,
                    pendingReview = stats.pendingReview,
                    isLoading = false,
                )
        }
    }

    private fun toggleBlocking(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockingEnabled(enabled)
        }
    }
}
