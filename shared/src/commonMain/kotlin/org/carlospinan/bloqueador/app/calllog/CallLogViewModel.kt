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
    /**
     * The active date window. Exposed rather than read back from the navigation route, because
     * the screen's own chips change it too — a route argument would keep reporting whatever the
     * user navigated in with, and the selected chip would be wrong from the first tap.
     */
    val filter: String = "all",
    /** Drives the pull-to-refresh indicator, and nothing else. */
    val refreshing: Boolean = false,
)

sealed interface CallLogIntent {
    data class SetFilter(
        val filter: String,
    ) : CallLogIntent

    /** Destroy the auto-responder recording attached to a call-log entry. */
    data class DeleteRecording(
        val entryId: Long,
    ) : CallLogIntent

    /**
     * Re-read the address book. Dispatched on resume, because the permission that gates it can be
     * granted while this screen is already on top -- from Settings, or from the onboarding
     * checklist -- and loading names only in `init` left the log showing bare numbers until the
     * process died.
     */
    data object RefreshContactNames : CallLogIntent

    /**
     * The same reload, asked for by hand, past the contacts gateway's five-minute cache and with
     * an indicator on screen while it runs.
     *
     * The calls themselves arrive as a database flow and need no refreshing -- but the *names*
     * beside them do not, and a log full of bare numbers after saving a contact in another app is
     * exactly the moment a user reaches for a pull gesture.
     */
    data object Refresh : CallLogIntent
}

class CallLogViewModel(
    private val callLogRepository: CallLogRepository,
    private val contactsGateway: ContactsGateway,
) : ViewModel() {
    private val filterFlow = MutableStateFlow("all")
    private val contactNamesFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    private val refreshingFlow = MutableStateFlow(false)

    val state: StateFlow<CallLogUiState> =
        combine(
            callLogRepository.allEntries(),
            filterFlow,
            contactNamesFlow,
            refreshingFlow,
        ) { entries, filter, contactNames, refreshing ->
            CallLogUiState(
                entries = applyFilter(entries, filter),
                contactNames = contactNames,
                filter = filter,
                refreshing = refreshing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallLogUiState())

    init {
        loadContactNames()
    }

    fun onIntent(intent: CallLogIntent) {
        when (intent) {
            is CallLogIntent.SetFilter -> filterFlow.value = intent.filter
            is CallLogIntent.DeleteRecording ->
                viewModelScope.launch { callLogRepository.deleteRecording(intent.entryId) }
            is CallLogIntent.RefreshContactNames -> loadContactNames()
            is CallLogIntent.Refresh -> loadContactNames(force = true)
        }
    }

    private fun loadContactNames(force: Boolean = false) {
        if (!contactsGateway.hasPermission()) {
            // Still cleared, so the indicator cannot be left spinning by a refresh asked for on a
            // screen whose permission was revoked while it was open.
            refreshingFlow.value = false
            return
        }
        if (force) refreshingFlow.value = true
        viewModelScope.launch {
            try {
                if (force) contactsGateway.refresh()
                contactNamesFlow.value = contactsGateway.contactNames()
            } finally {
                refreshingFlow.value = false
            }
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
