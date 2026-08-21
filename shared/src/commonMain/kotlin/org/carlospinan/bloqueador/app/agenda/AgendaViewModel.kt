package org.carlospinan.bloqueador.app.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.RuleRepository

data class AgendaUiState(
    val contacts: List<Contact> = emptyList(),
    /**
     * Whether the address book could be read at all. Needed separately from an empty [contacts]:
     * "you have no contacts" and "this app cannot see your contacts" look identical in a list and
     * need opposite things from the user.
     */
    val contactsPermissionGranted: Boolean = false,
    val blockedNumbers: List<BlockedNumberEntry> = emptyList(),
    val allowlistedNumbers: List<AllowlistedNumberEntry> = emptyList(),
    /** Drives the pull-to-refresh indicator, and nothing else. */
    val refreshing: Boolean = false,
)

sealed interface AgendaIntent {
    /**
     * Re-read the address book from the provider, past the gateway's cache.
     *
     * Both the pull gesture and the resume hook land here. The cache exists because the allowlist
     * asks the address book on the ringing-call path, and five minutes of it is invisible
     * everywhere else in the app -- but a user who has just added a contact in another app and
     * pulled this list down to see it would be told, silently and convincingly, that it is not
     * there.
     */
    data object Refresh : AgendaIntent

    data class BlockNumber(
        val number: String,
    ) : AgendaIntent

    data class AllowlistNumber(
        val number: String,
    ) : AgendaIntent

    /** Takes the rule's id, not the number: unblocking has to remove the row that matched. */
    data class UnblockNumber(
        val id: Long,
    ) : AgendaIntent

    data class RemoveFromAllowlist(
        val id: Long,
    ) : AgendaIntent
}

/**
 * The Agenda tab: the phone's address book, read through this app's rules.
 *
 * It holds the rule lists as well as the contacts because every row can be acted on -- blocking
 * the person you are looking at is the whole reason a call blocker shows an address book -- and
 * because two of the four filters are questions about the rules rather than about the contacts.
 */
class AgendaViewModel(
    private val contactsGateway: ContactsGateway,
    private val ruleRepository: RuleRepository,
) : ViewModel() {
    private val contactsFlow = MutableStateFlow<List<Contact>>(emptyList())
    private val permissionFlow = MutableStateFlow(false)
    private val refreshingFlow = MutableStateFlow(false)

    val state: StateFlow<AgendaUiState> =
        combine(
            contactsFlow,
            permissionFlow,
            ruleRepository.blockedNumbers(),
            ruleRepository.allowlistedNumbers(),
            refreshingFlow,
        ) { contacts, granted, blocked, allowlisted, refreshing ->
            AgendaUiState(
                contacts = contacts,
                contactsPermissionGranted = granted,
                blockedNumbers = blocked,
                allowlistedNumbers = allowlisted,
                refreshing = refreshing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AgendaUiState())

    init {
        loadContacts(force = false)
    }

    fun onIntent(intent: AgendaIntent) {
        when (intent) {
            is AgendaIntent.Refresh -> loadContacts(force = true)
            is AgendaIntent.BlockNumber ->
                viewModelScope.launch { ruleRepository.addBlockedNumber(intent.number) }
            is AgendaIntent.AllowlistNumber ->
                viewModelScope.launch { ruleRepository.addAllowlistedNumber(intent.number) }
            is AgendaIntent.UnblockNumber ->
                viewModelScope.launch { ruleRepository.removeBlockedNumber(intent.id) }
            is AgendaIntent.RemoveFromAllowlist ->
                viewModelScope.launch { ruleRepository.removeAllowlistedNumber(intent.id) }
        }
    }

    private fun loadContacts(force: Boolean) {
        val granted = contactsGateway.hasPermission()
        permissionFlow.value = granted
        if (!granted) {
            // Cleared as well as flagged: the permission can be revoked from system settings
            // while this screen is alive, and a list left over from before the revocation is
            // address-book data still on screen after the user said no.
            contactsFlow.value = emptyList()
            refreshingFlow.value = false
            return
        }
        refreshingFlow.value = true
        viewModelScope.launch {
            try {
                if (force) contactsGateway.refresh()
                contactsFlow.value = contactsGateway.contacts()
            } finally {
                // In `finally` so a provider that throws cannot leave the pull indicator spinning
                // forever over a list that has stopped loading.
                refreshingFlow.value = false
            }
        }
    }
}
