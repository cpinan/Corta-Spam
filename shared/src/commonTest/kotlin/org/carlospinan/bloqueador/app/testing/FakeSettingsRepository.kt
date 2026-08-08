package org.carlospinan.bloqueador.app.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.carlospinan.bloqueador.app.settings.DefaultAction
import org.carlospinan.bloqueador.app.settings.SettingsRepository

/**
 * In-memory [SettingsRepository] for tests.
 *
 * Every flow is a constructor parameter so a test can both seed a starting value and
 * keep a handle on it. Setters write through to those flows, so a test may assert on
 * either the flow or the setter's effect.
 *
 * [welcomeShown] and [permissionsPromptShown] are plain `Boolean`s in the interface, so
 * [setWelcomeShown]/[setPermissionsPromptShown] cannot flip them; pass the value the test
 * needs at construction.
 */
internal class FakeSettingsRepository(
    override val blockingEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    override val autoAllowContacts: MutableStateFlow<Boolean> = MutableStateFlow(false),
    override val defaultAction: MutableStateFlow<DefaultAction> = MutableStateFlow(DefaultAction.ALLOW),
    override val notificationsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    override val repeatedCallerBypassCount: MutableStateFlow<Int> = MutableStateFlow(0),
    override val welcomeShown: Boolean = false,
    override val permissionsPromptShown: Boolean = false,
) : SettingsRepository {
    override suspend fun setBlockingEnabled(enabled: Boolean) {
        blockingEnabled.value = enabled
    }

    override suspend fun setAutoAllowContacts(enabled: Boolean) {
        autoAllowContacts.value = enabled
    }

    override suspend fun setDefaultAction(action: DefaultAction) {
        defaultAction.value = action
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
    }

    override suspend fun setRepeatedCallerBypassCount(count: Int) {
        repeatedCallerBypassCount.value = count
    }

    override suspend fun setWelcomeShown() {}

    override suspend fun setPermissionsPromptShown() {}
}
