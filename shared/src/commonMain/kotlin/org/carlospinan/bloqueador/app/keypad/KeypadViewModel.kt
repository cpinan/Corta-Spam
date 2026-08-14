package org.carlospinan.bloqueador.app.keypad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactsGateway

data class KeypadUiState(
    val contacts: List<Contact> = emptyList(),
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
) : ViewModel() {
    private val _state = MutableStateFlow(KeypadUiState())
    val state: StateFlow<KeypadUiState> = _state.asStateFlow()

    init {
        loadContacts()
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
        _state.value =
            _state.value.copy(
                contactsPermissionGranted = granted,
                contacts = if (granted) _state.value.contacts else emptyList(),
            )
        if (!granted) return
        viewModelScope.launch {
            _state.value = _state.value.copy(contacts = contactsGateway.contacts())
        }
    }
}
