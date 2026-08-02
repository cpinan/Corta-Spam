package org.carlospinan.bloqueador.app.blocklist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.backup.ImportResult
import org.carlospinan.bloqueador.app.rules.ActionRuleEntry
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.CountryRuleEntry
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.rules.ScheduleRuleEntry
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BlockListViewModelTest {
    private class FakeRuleRepository : RuleRepository {
        val blockedNumbersFlow = MutableStateFlow(emptyList<BlockedNumberEntry>())
        val allowlistedNumbersFlow = MutableStateFlow(emptyList<AllowlistedNumberEntry>())
        val patternRulesFlow = MutableStateFlow(emptyList<PatternRuleEntry>())
        val countryRulesFlow = MutableStateFlow(emptyList<CountryRuleEntry>())
        val scheduleRulesFlow = MutableStateFlow(emptyList<ScheduleRuleEntry>())

        override fun blockedNumbers() = blockedNumbersFlow

        override fun allowlistedNumbers() = allowlistedNumbersFlow

        override fun patternRules() = patternRulesFlow

        override fun countryRules() = countryRulesFlow

        override fun actionRules() = MutableStateFlow(emptyList<ActionRuleEntry>())

        override fun scheduleRules() = scheduleRulesFlow

        override suspend fun blockedNumberEntries() = blockedNumbersFlow.value

        override suspend fun allowlistedNumberEntries() = allowlistedNumbersFlow.value

        override suspend fun enabledPatterns() = emptyList<org.carlospinan.bloqueador.app.rules.PatternRule>()

        override suspend fun enabledCountryRules() = emptyList<CountryRuleEntry>()

        override suspend fun enabledActionRules() = emptyList<org.carlospinan.bloqueador.app.rules.ActionRule>()

        override suspend fun enabledScheduleRules() = emptyList<org.carlospinan.bloqueador.app.rules.ScheduleRule>()

        override suspend fun recordCallAttempt(
            number: String,
            timestampMillis: Long,
        ) {}

        override suspend fun countRecentAttempts(
            number: String,
            sinceTimestampMillis: Long,
        ) = 0

        override suspend fun deleteExpiredAttempts(beforeTimestampMillis: Long) {}

        val addBlockedNumberCalls = mutableListOf<Pair<String, String?>>()

        override suspend fun addBlockedNumber(
            number: String,
            label: String?,
        ) {
            addBlockedNumberCalls.add(number to label)
        }

        override suspend fun removeBlockedNumber(id: Long) {}

        override suspend fun addAllowlistedNumber(
            number: String,
            label: String?,
        ) {}

        override suspend fun removeAllowlistedNumber(id: Long) {}

        override suspend fun addPatternRule(
            pattern: String,
            label: String?,
        ) {}

        override suspend fun togglePatternRule(
            id: Long,
            enabled: Boolean,
        ) {}

        override suspend fun removePatternRule(id: Long) {}

        override suspend fun addCountryRule(
            countryCode: String,
            countryName: String,
        ) {}

        override suspend fun toggleCountryRule(
            id: Long,
            enabled: Boolean,
        ) {}

        override suspend fun removeCountryRule(id: Long) {}

        override suspend fun addActionRule(
            label: String?,
            attempts: Int,
            windowMinutes: Int,
            patternId: Long?,
        ) {}

        override suspend fun toggleActionRule(
            id: Long,
            enabled: Boolean,
        ) {}

        override suspend fun removeActionRule(id: Long) {}

        override suspend fun addScheduleRule(
            label: String?,
            startMinute: Int,
            endMinute: Int,
        ) {}

        override suspend fun toggleScheduleRule(
            id: Long,
            enabled: Boolean,
        ) {}

        override suspend fun removeScheduleRule(id: Long) {}

        override suspend fun exportAll(): String = "{}"

        override suspend fun importAll(json: String): ImportResult = ImportResult()
    }

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
