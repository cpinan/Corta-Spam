package org.carlospinan.bloqueador.app.spam

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.carlospinan.bloqueador.app.db.AppDatabase
import org.carlospinan.bloqueador.app.db.KeyValueSettingsStore

class SqlSpamProviderRepository(
    db: AppDatabase,
    dispatcher: CoroutineDispatcher,
) : SpamProviderRepository {
    private val store = KeyValueSettingsStore(db, dispatcher)

    private val _enabled = MutableStateFlow(false)
    override val enabled: Flow<Boolean> = _enabled.asStateFlow()

    init {
        _enabled.value = store.readBool("spam_provider_enabled", false)
    }

    override suspend fun setEnabled(enabled: Boolean) {
        store.writeBool("spam_provider_enabled", enabled)
        _enabled.value = enabled
    }
}
