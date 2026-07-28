package org.carlospinan.bloqueador.app.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.DayStat

data class StatsUiState(
    val dailyStats: List<DayStat> = emptyList(),
    val isLoading: Boolean = true,
)

class StatsViewModel(
    private val callLogRepository: CallLogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = StatsUiState(isLoading = true)
            val daily = callLogRepository.blockedByDay(7)
            _state.value = StatsUiState(dailyStats = daily, isLoading = false)
        }
    }
}
