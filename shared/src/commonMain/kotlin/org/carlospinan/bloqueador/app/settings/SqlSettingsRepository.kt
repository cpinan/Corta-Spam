package org.carlospinan.bloqueador.app.settings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.carlospinan.bloqueador.app.db.AppDatabase
import org.carlospinan.bloqueador.app.db.KeyValueSettingsStore

class SqlSettingsRepository(
    db: AppDatabase,
    dispatcher: CoroutineDispatcher,
) : SettingsRepository {
    private val store = KeyValueSettingsStore(db, dispatcher)

    private val _blockingEnabled = MutableStateFlow(true)
    override val blockingEnabled: Flow<Boolean> = _blockingEnabled.asStateFlow()

    private val _autoAllowContacts = MutableStateFlow(true)
    override val autoAllowContacts: Flow<Boolean> = _autoAllowContacts.asStateFlow()

    private val _defaultAction = MutableStateFlow(DefaultAction.ALLOW)
    override val defaultAction: Flow<DefaultAction> = _defaultAction.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    override val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notifyUnknownCallers = MutableStateFlow(true)
    override val notifyUnknownCallers: StateFlow<Boolean> = _notifyUnknownCallers.asStateFlow()

    private val _repeatedCallerBypassCount = MutableStateFlow(0)
    override val repeatedCallerBypassCount: Flow<Int> = _repeatedCallerBypassCount.asStateFlow()

    init {
        _blockingEnabled.value = store.readBool("blocking_enabled", true)
        _autoAllowContacts.value = store.readBool("auto_allow_contacts", true)
        _defaultAction.value =
            store.readString("default_action", DefaultAction.ALLOW.key).let { key ->
                DefaultAction.entries.find { it.key == key } ?: DefaultAction.ALLOW
            }
        _notificationsEnabled.value = store.readBool("notifications_enabled", true)
        _notifyUnknownCallers.value = store.readBool("notify_unknown_callers", true)
        _repeatedCallerBypassCount.value = store.readInt("repeated_caller_bypass_count", 0)
    }

    override suspend fun setBlockingEnabled(enabled: Boolean) {
        store.writeBool("blocking_enabled", enabled)
        _blockingEnabled.value = enabled
    }

    override suspend fun setAutoAllowContacts(enabled: Boolean) {
        store.writeBool("auto_allow_contacts", enabled)
        _autoAllowContacts.value = enabled
    }

    override suspend fun setDefaultAction(action: DefaultAction) {
        store.write("default_action", action.key)
        _defaultAction.value = action
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.writeBool("notifications_enabled", enabled)
        _notificationsEnabled.value = enabled
    }

    override suspend fun setNotifyUnknownCallers(enabled: Boolean) {
        store.writeBool("notify_unknown_callers", enabled)
        _notifyUnknownCallers.value = enabled
    }

    override suspend fun setRepeatedCallerBypassCount(count: Int) {
        store.writeInt("repeated_caller_bypass_count", count)
        _repeatedCallerBypassCount.value = count
    }

    private var _welcomeShown: Boolean = false
    override val welcomeShown: Boolean
        get() = _welcomeShown

    private var _permissionsPromptShown: Boolean = false
    override val permissionsPromptShown: Boolean
        get() = _permissionsPromptShown

    init {
        _welcomeShown = store.readBool("welcome_shown", false)
        _permissionsPromptShown = store.readBool("permissions_prompt_shown", false)
    }

    override suspend fun setWelcomeShown() {
        store.writeBool("welcome_shown", true)
        _welcomeShown = true
    }

    override suspend fun setPermissionsPromptShown() {
        store.writeBool("permissions_prompt_shown", true)
        _permissionsPromptShown = true
    }
}
