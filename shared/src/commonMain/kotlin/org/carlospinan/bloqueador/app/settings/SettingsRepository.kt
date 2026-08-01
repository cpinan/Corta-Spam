package org.carlospinan.bloqueador.app.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class DefaultAction(
    val key: String,
) {
    ALLOW("ALLOW"),
    BLOCK("BLOCK"),
    ASK("ASK"),
}

interface SettingsRepository {
    val blockingEnabled: Flow<Boolean>
    val autoAllowContacts: Flow<Boolean>
    val defaultAction: Flow<DefaultAction>

    // StateFlow so Android call-handling code (Telecom callbacks, not always inside a
    // coroutine) can read the current value synchronously via `.value` instead of `.first()`.
    val notificationsEnabled: StateFlow<Boolean>

    suspend fun setBlockingEnabled(enabled: Boolean)

    suspend fun setAutoAllowContacts(enabled: Boolean)

    suspend fun setDefaultAction(action: DefaultAction)

    suspend fun setNotificationsEnabled(enabled: Boolean)

    val welcomeShown: Boolean

    suspend fun setWelcomeShown()
}
