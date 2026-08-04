package org.carlospinan.bloqueador.app.testing

import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.settings.DefaultAction
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the defaults and the write-through setters of [FakeSettingsRepository].
 * Every ViewModel test that injects it depends on both, so a change here is a
 * change to the contract those tests assume.
 */
class FakeSettingsRepositoryTest {
    @Test
    fun `defaultAction round trip`() =
        runTest {
            val repo = FakeSettingsRepository()
            assertEquals(DefaultAction.ALLOW, repo.defaultAction.value)
            repo.setDefaultAction(DefaultAction.BLOCK)
            assertEquals(DefaultAction.BLOCK, repo.defaultAction.value)
        }

    @Test
    fun `blockingEnabled round trip`() =
        runTest {
            val repo = FakeSettingsRepository()
            assertEquals(true, repo.blockingEnabled.value)
            repo.setBlockingEnabled(false)
            assertEquals(false, repo.blockingEnabled.value)
        }

    @Test
    fun `autoAllowContacts defaults to false`() {
        val repo = FakeSettingsRepository()
        assertEquals(false, repo.autoAllowContacts.value)
    }

    @Test
    fun `repeatedCallerBypassCount defaults to disabled`() {
        val repo = FakeSettingsRepository()
        assertEquals(0, repo.repeatedCallerBypassCount.value)
    }

    @Test
    fun `repeatedCallerBypassCount round trip`() =
        runTest {
            val repo = FakeSettingsRepository()
            repo.setRepeatedCallerBypassCount(3)
            assertEquals(3, repo.repeatedCallerBypassCount.value)
        }

    @Test
    fun `welcomeShown is seeded at construction`() {
        assertEquals(false, FakeSettingsRepository().welcomeShown)
        assertEquals(true, FakeSettingsRepository(welcomeShown = true).welcomeShown)
    }
}
