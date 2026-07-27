package org.carlospinan.bloqueador.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.CallLogRepository

data class HomeUiState(
    val blockedToday: Int = 0,
    val blockedThisWeek: Int = 0,
    val blockedThisMonth: Int = 0,
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val callLogRepository: CallLogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

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
                    isLoading = false,
                )
        }
    }

    fun toggleBlocking(enabled: Boolean) {
        // M2 scope: toggle is visual only; actual blocking state managed at service level.
    }
}
