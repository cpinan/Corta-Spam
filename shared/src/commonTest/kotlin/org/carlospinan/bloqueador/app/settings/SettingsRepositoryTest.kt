package org.carlospinan.bloqueador.app.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    private class FakeSettingsRepository(
        val blockingEnabledFlow: MutableStateFlow<Boolean> = MutableStateFlow(true),
        val defaultActionFlow: MutableStateFlow<DefaultAction> = MutableStateFlow(DefaultAction.ALLOW),
    ) : SettingsRepository {
        override val blockingEnabled = blockingEnabledFlow
        override val autoAllowContacts = MutableStateFlow(false)
        override val defaultAction = defaultActionFlow
        override val notificationsEnabled = MutableStateFlow(true)

        override suspend fun setBlockingEnabled(enabled: Boolean) {
            blockingEnabledFlow.value = enabled
        }

        override suspend fun setAutoAllowContacts(enabled: Boolean) {}

        override suspend fun setDefaultAction(action: DefaultAction) {
            defaultActionFlow.value = action
        }

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            notificationsEnabled.value = enabled
        }

        override val welcomeShown: Boolean = true

        override suspend fun setWelcomeShown() {}
    }

    @Test
    fun `defaultAction round trip`() {
        val repo = FakeSettingsRepository()
        assertEquals(DefaultAction.ALLOW, repo.defaultAction.value)
        repo.defaultActionFlow.value = DefaultAction.BLOCK
        assertEquals(DefaultAction.BLOCK, repo.defaultAction.value)
    }

    @Test
    fun `blockingEnabled round trip`() {
        val repo = FakeSettingsRepository()
        assertEquals(true, repo.blockingEnabled.value)
        repo.blockingEnabledFlow.value = false
        assertEquals(false, repo.blockingEnabled.value)
    }

    @Test
    fun `autoAllowContacts defaults to false`() {
        val repo = FakeSettingsRepository()
        assertEquals(false, repo.autoAllowContacts.value)
    }
}
