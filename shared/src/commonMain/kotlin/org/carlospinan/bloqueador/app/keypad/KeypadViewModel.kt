package org.carlospinan.bloqueador.app.keypad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.CallLogRepository

data class KeypadUiState(
    val contacts: List<Contact> = emptyList(),
    /**
     * The people this phone dealt with most recently, for the strip above the number field when
     * nothing is starred. Read from this app's own call log rather than the platform's: the
     * platform log needs a permission this app does not hold, and the rows here are the ones the
     * user can already see on the Log tab.
     */
    val recent: List<Contact> = emptyList(),
    /**
     * Whether the address book could be read at all. The screen needs this separately from an
     * empty [contacts] list: "you have no contacts" and "this app cannot see your contacts" look
     * identical in the results area and need opposite things from the user.
     */
    val contactsPermissionGranted: Boolean = false,
)

sealed interface KeypadIntent {
    /**
     * Re-read the address book. Dispatched on resume: the permission that gates it can be granted
     * while this screen is already on top -- from its own button, from Settings, or from the
     * onboarding checklist -- and loading only in `init` would leave search dead until the
     * process was restarted.
     */
    data object RefreshContacts : KeypadIntent
}

class KeypadViewModel(
    private val contactsGateway: ContactsGateway,
    private val callLogRepository: CallLogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(KeypadUiState())
    val state: StateFlow<KeypadUiState> = _state.asStateFlow()

    init {
        loadContacts()
        observeRecentCalls()
    }

    /**
     * Names are resolved here rather than in the composable because the strip shows one label per
     * caller and the address book is the only thing that can turn a number into it. A number with
     * no contact behind it keeps the number, which is a perfectly good answer to "who rang me".
     */
    private fun observeRecentCalls() {
        viewModelScope.launch {
            callLogRepository.allEntries().collect { entries ->
                val names = if (contactsGateway.hasPermission()) contactsGateway.contactNames() else emptyMap()
                _state.update { it.copy(recent = recentContacts(entries, names)) }
            }
        }
    }

    fun onIntent(intent: KeypadIntent) {
        when (intent) {
            is KeypadIntent.RefreshContacts -> loadContacts()
        }
    }

    private fun loadContacts() {
        val granted = contactsGateway.hasPermission()
        // Cleared as well as flagged when the permission is gone: it can be revoked from system
        // settings while the app is alive, and a list left over from before the revocation is
        // address-book data still on screen after the user said no.
        // update, not `value =`. Two coroutines write this state -- the address-book load and the
        // call-log collector -- and a read-modify-write between them loses whichever wrote first:
        // the permission flag came back false under a list of contacts that had just been loaded
        // with it true. update() retries on a concurrent change instead.
        _state.update {
            it.copy(
                contactsPermissionGranted = granted,
                contacts = if (granted) it.contacts else emptyList(),
            )
        }
        if (!granted) return
        viewModelScope.launch {
            val loaded = contactsGateway.contacts()
            _state.update { it.copy(contacts = loaded) }
        }
    }
}
