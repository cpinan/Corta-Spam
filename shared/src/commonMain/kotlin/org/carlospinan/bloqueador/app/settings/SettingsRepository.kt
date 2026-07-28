package org.carlospinan.bloqueador.app.settings

import kotlinx.coroutines.flow.Flow

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

    suspend fun setBlockingEnabled(enabled: Boolean)

    suspend fun setAutoAllowContacts(enabled: Boolean)

    suspend fun setDefaultAction(action: DefaultAction)

    val welcomeShown: Boolean

    suspend fun setWelcomeShown()
}
