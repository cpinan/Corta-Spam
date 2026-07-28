package org.carlospinan.bloqueador.app.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.carlospinan.bloqueador.app.db.AppDatabase
import org.carlospinan.bloqueador.app.db.KeyValueSettingsStore

class SqlSettingsRepository(
    db: AppDatabase,
) : SettingsRepository {
    private val store = KeyValueSettingsStore(db)

    private val _blockingEnabled = MutableStateFlow(true)
    override val blockingEnabled: Flow<Boolean> = _blockingEnabled.asStateFlow()

    private val _autoAllowContacts = MutableStateFlow(true)
    override val autoAllowContacts: Flow<Boolean> = _autoAllowContacts.asStateFlow()

    private val _defaultAction = MutableStateFlow(DefaultAction.ALLOW)
    override val defaultAction: Flow<DefaultAction> = _defaultAction.asStateFlow()

    init {
        _blockingEnabled.value = store.readBool("blocking_enabled", true)
        _autoAllowContacts.value = store.readBool("auto_allow_contacts", true)
        _defaultAction.value =
            store.readString("default_action", DefaultAction.ALLOW.key).let { key ->
                DefaultAction.entries.find { it.key == key } ?: DefaultAction.ALLOW
            }
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

    private var _welcomeShown: Boolean = false
    override val welcomeShown: Boolean
        get() = _welcomeShown

    init {
        _welcomeShown = store.readBool("welcome_shown", false)
    }

    override suspend fun setWelcomeShown() {
        store.writeBool("welcome_shown", true)
        _welcomeShown = true
    }
}
