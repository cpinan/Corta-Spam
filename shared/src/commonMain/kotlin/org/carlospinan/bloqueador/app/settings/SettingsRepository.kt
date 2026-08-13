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

    /**
     * Whether the after-the-fact call notifications -- blocked, missed, repeated caller -- are
     * posted for numbers the address book does not claim. Defaults to true, which is the
     * behaviour that shipped before this existed.
     *
     * Deliberately scoped to those three and **not** to the ringing full-screen notification.
     * That one is not decoration: as the default dialer this app owns the incoming-call UI, and
     * suppressing its notification would leave a stranger's call with no screen at all over the
     * lock screen. Silencing strangers entirely is what a block rule is for.
     *
     * StateFlow for the same reason as [notificationsEnabled] -- read from Telecom callbacks.
     */
    val notifyUnknownCallers: StateFlow<Boolean>

    /** 0 = disabled. Otherwise, attempts within 24h before an unknown (default-blocked) number is let through. */
    val repeatedCallerBypassCount: Flow<Int>

    suspend fun setBlockingEnabled(enabled: Boolean)

    suspend fun setAutoAllowContacts(enabled: Boolean)

    suspend fun setDefaultAction(action: DefaultAction)

    suspend fun setNotificationsEnabled(enabled: Boolean)

    suspend fun setNotifyUnknownCallers(enabled: Boolean)

    suspend fun setRepeatedCallerBypassCount(count: Int)

    val welcomeShown: Boolean

    suspend fun setWelcomeShown()

    /**
     * True once the user has walked past the permission checklist step of onboarding.
     * Plain `Boolean` for the same reason as [welcomeShown]: it is read once, at the
     * root of the Android UI tree, to decide whether that step still needs showing.
     */
    val permissionsPromptShown: Boolean

    suspend fun setPermissionsPromptShown()
}
