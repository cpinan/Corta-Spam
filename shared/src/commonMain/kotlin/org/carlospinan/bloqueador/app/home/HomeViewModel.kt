package org.carlospinan.bloqueador.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.settings.SettingsRepository

data class HomeUiState(
    val blockedToday: Int = 0,
    val blockedThisWeek: Int = 0,
    val blockedThisMonth: Int = 0,
    val pendingReview: Int = 0,
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val callLogRepository: CallLogRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val blockingEnabled: StateFlow<Boolean> =
        settingsRepository.blockingEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val stats = callLogRepository.blockedStats()
            _state.value =
                HomeUiState(
                    blockedToday = stats.today,
                    blockedThisWeek = stats.thisWeek,
                    blockedThisMonth = stats.thisMonth,
                    pendingReview = stats.pendingReview,
                    isLoading = false,
                )
        }
    }

    fun toggleBlocking(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockingEnabled(enabled)
        }
    }
}
