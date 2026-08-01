package org.carlospinan.bloqueador.app.calllog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.CallLogRepository

class CallLogViewModel(
    callLogRepository: CallLogRepository,
    private val contactsGateway: ContactsGateway,
) : ViewModel() {
    val entries: StateFlow<List<CallLogEntryData>> =
        callLogRepository
            .allEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contactNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val contactNames: StateFlow<Map<String, String>> = _contactNames.asStateFlow()

    init {
        if (contactsGateway.hasPermission()) {
            viewModelScope.launch {
                _contactNames.value = contactsGateway.contactNames()
            }
        }
    }
}
