package org.carlospinan.bloqueador.app.blocklist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.CountryRuleEntry
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.testing.FakeRuleRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BlockListViewModelTest {
    private class FakeContactsGateway : org.carlospinan.bloqueador.app.contacts.ContactsGateway {
        override suspend fun contactNumbers(): Set<String> = emptySet()

        override suspend fun contactNames(): Map<String, String> = emptyMap()

        override fun hasPermission(): Boolean = false
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blockedNumbers reflects repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            repo.blockedNumbersFlow.value =
                listOf(BlockedNumberEntry(id = 1, number = "+34", label = null, createdAt = 0L))
            val vm = BlockListViewModel(repo, FakeContactsGateway())
            advanceUntilIdle()
            assertEquals(
                1,
                vm.state
                    .first { it.blockedNumbers.isNotEmpty() }
                    .blockedNumbers.size,
            )
        }

    @Test
    fun `allowlistedNumbers reflects repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            repo.allowlistedNumbersFlow.value =
                listOf(AllowlistedNumberEntry(id = 1, number = "+34", label = null, createdAt = 0L))
            val vm = BlockListViewModel(repo, FakeContactsGateway())
            advanceUntilIdle()
            assertEquals(
                1,
                vm.state
                    .first { it.allowlistedNumbers.isNotEmpty() }
                    .allowlistedNumbers.size,
            )
        }

    @Test
    fun `patternRules reflects repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            repo.patternRulesFlow.value =
                listOf(PatternRuleEntry(id = 1, pattern = "*900*", label = null, enabled = true, createdAt = 0L))
            val vm = BlockListViewModel(repo, FakeContactsGateway())
            advanceUntilIdle()
            assertEquals(
                1,
                vm.state
                    .first { it.patternRules.isNotEmpty() }
                    .patternRules.size,
            )
        }

    @Test
    fun `countryRules reflects repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            repo.countryRulesFlow.value =
                listOf(CountryRuleEntry(id = 1, countryCode = "34", countryName = "Spain", enabled = true, createdAt = 0L))
            val vm = BlockListViewModel(repo, FakeContactsGateway())
            advanceUntilIdle()
            assertEquals(
                1,
                vm.state
                    .first { it.countryRules.isNotEmpty() }
                    .countryRules.size,
            )
        }

    @Test
    fun `counts update with flow changes`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            val vm = BlockListViewModel(repo, FakeContactsGateway())
            advanceUntilIdle()
            assertEquals(0, vm.state.first().blockedCount)

            repo.blockedNumbersFlow.value =
                listOf(BlockedNumberEntry(id = 1, number = "+34", label = null, createdAt = 0L))
            advanceUntilIdle()
            assertEquals(1, vm.state.first { it.blockedCount == 1 }.blockedCount)
        }

    @Test
    fun `onIntent AddBlockedNumber delegates to repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            val vm = BlockListViewModel(repo, FakeContactsGateway())

            vm.onIntent(BlockListIntent.AddBlockedNumber("+34611223344", "Spam"))
            advanceUntilIdle()

            assertEquals(1, repo.addBlockedNumberCalls.size)
            assertEquals("+34611223344" to "Spam", repo.addBlockedNumberCalls[0])
        }
}
