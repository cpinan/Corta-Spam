package org.carlospinan.bloqueador.app.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thin wrapper over the AppSettings key-value table, shared by settings/spam/autoresponder repositories. */
class KeyValueSettingsStore(
    private val db: AppDatabase,
) {
    fun readBool(
        key: String,
        default: Boolean,
    ): Boolean {
        val raw = db.appDatabaseQueries.selectSetting(key).executeAsOneOrNull() ?: return default
        return raw.toBooleanStrictOrNull() ?: default
    }

    fun readString(
        key: String,
        default: String,
    ): String {
        val raw = db.appDatabaseQueries.selectSetting(key).executeAsOneOrNull() ?: return default
        return raw.ifBlank { default }
    }

    suspend fun write(
        key: String,
        value: String,
    ) {
        withContext(Dispatchers.IO) {
            db.appDatabaseQueries.upsertSetting(key, value)
        }
    }

    suspend fun writeBool(
        key: String,
        value: Boolean,
    ) = write(key, value.toString())
}
