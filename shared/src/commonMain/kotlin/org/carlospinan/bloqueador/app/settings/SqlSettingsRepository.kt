package org.carlospinan.bloqueador.app.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.db.AppDatabase

class SqlSettingsRepository(
    private val db: AppDatabase,
) : SettingsRepository {
    private val _blockingEnabled = MutableStateFlow(true)
    override val blockingEnabled: Flow<Boolean> = _blockingEnabled.asStateFlow()

    private val _autoAllowContacts = MutableStateFlow(true)
    override val autoAllowContacts: Flow<Boolean> = _autoAllowContacts.asStateFlow()

    private val _defaultAction = MutableStateFlow(DefaultAction.ALLOW)
    override val defaultAction: Flow<DefaultAction> = _defaultAction.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _blockingEnabled.value = readBool("blocking_enabled", true)
        _autoAllowContacts.value = readBool("auto_allow_contacts", true)
        _defaultAction.value =
            readString("default_action", DefaultAction.ALLOW.key).let { key ->
                DefaultAction.entries.find { it.key == key } ?: DefaultAction.ALLOW
            }
    }

    private fun readBool(
        key: String,
        default: Boolean,
    ): Boolean {
        val raw = db.appDatabaseQueries.selectSetting(key).executeAsOneOrNull() ?: return default
        return raw.toBooleanStrictOrNull() ?: default
    }

    private fun readString(
        key: String,
        default: String,
    ): String {
        val raw = db.appDatabaseQueries.selectSetting(key).executeAsOneOrNull() ?: return default
        return raw.ifBlank { default }
    }

    override suspend fun setBlockingEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            db.appDatabaseQueries.upsertSetting("blocking_enabled", enabled.toString())
        }
        _blockingEnabled.value = enabled
    }

    override suspend fun setAutoAllowContacts(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            db.appDatabaseQueries.upsertSetting("auto_allow_contacts", enabled.toString())
        }
        _autoAllowContacts.value = enabled
    }

    override suspend fun setDefaultAction(action: DefaultAction) {
        withContext(Dispatchers.IO) {
            db.appDatabaseQueries.upsertSetting("default_action", action.key)
        }
        _defaultAction.value = action
    }
}
