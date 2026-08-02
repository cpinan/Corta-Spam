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

    /** 0 = disabled. Otherwise, attempts within 24h before an unknown (default-blocked) number is let through. */
    val repeatedCallerBypassCount: Flow<Int>

    suspend fun setBlockingEnabled(enabled: Boolean)

    suspend fun setAutoAllowContacts(enabled: Boolean)

    suspend fun setDefaultAction(action: DefaultAction)

    suspend fun setNotificationsEnabled(enabled: Boolean)

    suspend fun setRepeatedCallerBypassCount(count: Int)

    val welcomeShown: Boolean

    suspend fun setWelcomeShown()
}
