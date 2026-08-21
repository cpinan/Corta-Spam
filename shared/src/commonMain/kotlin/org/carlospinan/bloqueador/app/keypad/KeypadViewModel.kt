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
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.settings.SettingsRepository

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
     * Whether recent callers may be shown at all. The screen needs it separately from an empty
     * [recent]: "nobody has rung yet" and "you turned this off" want different words in the band.
     */
    val showRecentCallers: Boolean = true,
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
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(KeypadUiState())
    val state: StateFlow<KeypadUiState> = _state.asStateFlow()

    /** The log as it was last seen, so a permission grant can relabel it without a new call. */
    private var lastEntries: List<CallLogEntryData> = emptyList()

    init {
        loadContacts()
        observeRecentCalls()
        observeRecentCallerSetting()
    }

    /**
     * The strip is call history on a screen anyone holding the unlocked phone can open, so it is a
     * setting. Switched off, the recents are dropped from the state rather than merely hidden by
     * the screen: state the UI is told to ignore is state that leaks the next time someone reads
     * it for another purpose.
     */
    private fun observeRecentCallerSetting() {
        viewModelScope.launch {
            settingsRepository.showRecentCallersOnKeypad.collect { allowed ->
                _state.update { it.copy(showRecentCallers = allowed) }
                relabelRecentCalls()
            }
        }
    }

    /**
     * Names are resolved here rather than in the composable because the strip shows one label per
     * caller and the address book is the only thing that can turn a number into it. A number with
     * no contact behind it keeps the number, which is a perfectly good answer to "who rang me".
     */
    private fun observeRecentCalls() {
        viewModelScope.launch {
            // recentEntries, not allEntries: the strip needs four callers and the log is unbounded.
            // Collecting every row meant sorting the whole call log -- and computing comparison
            // keys for each of its numbers -- on the main dispatcher every time a call was logged.
            // The limit is well above four because the same caller occupies several rows and they
            // collapse into one entry here.
            callLogRepository.recentEntries(RECENT_SCAN_LIMIT).collect { entries ->
                lastEntries = entries
                relabelRecentCalls()
            }
        }
    }

    /**
     * Rebuilds the strip from the log already seen.
     *
     * Called on a permission change as well as on a new call, because names are resolved here:
     * without it, granting contacts from this screen's own button left the strip showing bare
     * numbers until someone rang. That is the same shape of bug this project has now fixed three
     * times -- work done once at construction, and a permission that arrives afterwards.
     */
    private fun relabelRecentCalls() {
        viewModelScope.launch {
            if (!_state.value.showRecentCallers) {
                _state.update { it.copy(recent = emptyList()) }
                return@launch
            }
            val names = if (contactsGateway.hasPermission()) contactsGateway.contactNames() else emptyMap()
            val recent = recentContacts(lastEntries, names)
            _state.update { it.copy(recent = recent) }
        }
    }

    fun onIntent(intent: KeypadIntent) {
        when (intent) {
            is KeypadIntent.RefreshContacts -> {
                loadContacts()
                relabelRecentCalls()
            }
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

/**
 * How many log rows are scanned for the strip. Four callers are shown; the same caller holds
 * several rows -- a blocked number that tried five times is five of them -- so the scan has to be
 * wider than the strip or a persistent caller fills it alone.
 */
private const val RECENT_SCAN_LIMIT = 50
