package org.carlospinan.bloqueador.app.spam

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.db.AppDatabase

class SqlSpamProviderRepository(
    private val db: AppDatabase,
) : SpamProviderRepository {
    private val _enabled = MutableStateFlow(false)
    override val enabled: Flow<Boolean> = _enabled.asStateFlow()

    init {
        _enabled.value =
            db.appDatabaseQueries
                .selectSetting("spam_provider_enabled")
                .executeAsOneOrNull()
                ?.toBooleanStrictOrNull() ?: false
    }

    override suspend fun setEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            db.appDatabaseQueries.upsertSetting("spam_provider_enabled", enabled.toString())
        }
        _enabled.value = enabled
    }
}
