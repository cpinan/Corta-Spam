package org.carlospinan.bloqueador.app.calllog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.currentTimeMillis

data class CallLogUiState(
    val entries: List<CallLogEntryData> = emptyList(),
    val contactNames: Map<String, String> = emptyMap(),
)

sealed interface CallLogIntent {
    data class SetFilter(
        val filter: String,
    ) : CallLogIntent
}

class CallLogViewModel(
    callLogRepository: CallLogRepository,
    private val contactsGateway: ContactsGateway,
) : ViewModel() {
    private val filterFlow = MutableStateFlow("all")
    private val contactNamesFlow = MutableStateFlow<Map<String, String>>(emptyMap())

    val state: StateFlow<CallLogUiState> =
        combine(
            callLogRepository.allEntries(),
            filterFlow,
            contactNamesFlow,
        ) { entries, filter, contactNames ->
            CallLogUiState(entries = applyFilter(entries, filter), contactNames = contactNames)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallLogUiState())

    init {
        if (contactsGateway.hasPermission()) {
            viewModelScope.launch {
                contactNamesFlow.value = contactsGateway.contactNames()
            }
        }
    }

    fun onIntent(intent: CallLogIntent) {
        when (intent) {
            is CallLogIntent.SetFilter -> filterFlow.value = intent.filter
        }
    }
}

private fun applyFilter(
    entries: List<CallLogEntryData>,
    filter: String,
): List<CallLogEntryData> {
    if (filter == "all") return entries
    if (filter == "review") return entries.filter { it.ruleType == "REVIEW" }
    val now =
        Instant
            .fromEpochMilliseconds(currentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
    val todayStart = LocalDateTime(now.year, now.monthNumber, now.dayOfMonth, 0, 0, 0)
    val todayStartEpoch = todayStart.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    val cutoff =
        when (filter) {
            "today" -> todayStartEpoch
            "week" -> {
                val daysBack = now.dayOfWeek.ordinal
                todayStartEpoch - daysBack * 86_400_000L
            }
            "month" -> {
                val monthStart = LocalDateTime(now.year, now.monthNumber, 1, 0, 0, 0)
                monthStart.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
            else -> return entries
        }
    return entries.filter { it.timestamp >= cutoff }
}
