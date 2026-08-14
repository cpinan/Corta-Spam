package org.carlospinan.bloqueador.app.blocklist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.rules.ActionRuleEntry
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
    private class FakeContactsGateway(
        var granted: Boolean = false,
        var names: Map<String, String> = emptyMap(),
    ) : org.carlospinan.bloqueador.app.contacts.ContactsGateway {
        override suspend fun contactNumbers(): Set<String> = emptySet()

        override suspend fun contactNames(): Map<String, String> = names

        override suspend fun contacts(): List<org.carlospinan.bloqueador.app.contacts.Contact> = emptyList()

        override fun hasPermission(): Boolean = granted
    }

    @Test
    fun `RefreshContactNames picks up a grant that landed after construction`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val contacts = FakeContactsGateway(granted = false)
            val vm = BlockListViewModel(FakeRuleRepository(), contacts)
            advanceUntilIdle()
            assertEquals(emptyMap(), vm.state.first().contactNames)

            contacts.granted = true
            contacts.names = mapOf("611998877" to "Ana")
            vm.onIntent(BlockListIntent.RefreshContactNames)

            val names = vm.state.first { it.contactNames.isNotEmpty() }.contactNames
            assertEquals("Ana", names["611998877"])
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

    // ---- patterns ----

    @Test
    fun `onIntent AddPatternRule rejects a pattern that would match every number`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            val vm = BlockListViewModel(repo, FakeContactsGateway())

            // Matching compares digits only, so "*" has an empty core and matches everything.
            // Persisting one would block the user's whole phone with no visible cause.
            vm.onIntent(BlockListIntent.AddPatternRule("*", null))
            vm.onIntent(BlockListIntent.AddPatternRule("*abc*", null))
            advanceUntilIdle()

            assertEquals(0, repo.addPatternRuleCalls.size)
        }

    @Test
    fun `onIntent AddPatternRule accepts a pattern with digits`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            val vm = BlockListViewModel(repo, FakeContactsGateway())

            vm.onIntent(BlockListIntent.AddPatternRule("+34900*", "Spanish 900s"))
            advanceUntilIdle()

            assertEquals(1, repo.addPatternRuleCalls.size)
        }

    // ---- action rules ----
    //
    // These had no UI at all until now: the schema, repository, resolver branch and backup
    // fields all existed, but nothing in the app could create one.

    @Test
    fun `actionRules reflects repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            val vm = BlockListViewModel(repo, FakeContactsGateway())
            advanceUntilIdle()
            assertEquals(0, vm.state.first().actionCount)

            repo.actionRulesFlow.value =
                listOf(
                    ActionRuleEntry(
                        id = 1,
                        label = "repeats",
                        attempts = 3,
                        windowMinutes = 5,
                        patternId = null,
                        enabled = true,
                        createdAt = 0L,
                    ),
                )
            advanceUntilIdle()

            assertEquals(1, vm.state.first { it.actionCount == 1 }.actionCount)
        }

    @Test
    fun `onIntent AddActionRule delegates to repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            val vm = BlockListViewModel(repo, FakeContactsGateway())

            vm.onIntent(BlockListIntent.AddActionRule("repeats", attempts = 3, windowMinutes = 5, patternId = 7L))
            advanceUntilIdle()

            assertEquals(
                listOf(FakeRuleRepository.AddActionRuleCall("repeats", 3, 5, 7L)),
                repo.addActionRuleCalls,
            )
        }

    @Test
    fun `onIntent AddActionRule rejects thresholds that would block every caller`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository()
            val vm = BlockListViewModel(repo, FakeContactsGateway())

            // attempts < 1 makes "count >= attempts" true for everyone; a 0-minute window can
            // never contain an attempt, so that rule is inert clutter.
            vm.onIntent(BlockListIntent.AddActionRule(null, attempts = 0, windowMinutes = 5))
            vm.onIntent(BlockListIntent.AddActionRule(null, attempts = -1, windowMinutes = 5))
            vm.onIntent(BlockListIntent.AddActionRule(null, attempts = 3, windowMinutes = 0))
            advanceUntilIdle()

            assertEquals(0, repo.addActionRuleCalls.size)
        }
}
