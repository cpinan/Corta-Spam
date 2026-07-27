package org.carlospinan.bloqueador.app.calllog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.CallLogRepository

class CallLogViewModel(
    callLogRepository: CallLogRepository,
) : ViewModel() {
    val entries: StateFlow<List<CallLogEntryData>> =
        callLogRepository
            .allEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
