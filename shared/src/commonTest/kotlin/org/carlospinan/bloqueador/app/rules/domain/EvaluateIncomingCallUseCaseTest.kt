package org.carlospinan.bloqueador.app.rules.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.backup.ImportResult
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.ActionRule
import org.carlospinan.bloqueador.app.rules.ActionRuleEntry
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.CountryRuleEntry
import org.carlospinan.bloqueador.app.rules.PatternRule
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.rules.ScheduleRule
import org.carlospinan.bloqueador.app.rules.ScheduleRuleEntry
import org.carlospinan.bloqueador.app.settings.DefaultAction
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import org.carlospinan.bloqueador.app.spam.SpamProviderClient
import org.carlospinan.bloqueador.app.spam.SpamProviderRepository
import org.carlospinan.bloqueador.app.spam.SpamResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeRuleRepository : RuleRepository {
    var blockedEntries = emptyList<BlockedNumberEntry>()
    var allowlistedEntries = emptyList<AllowlistedNumberEntry>()
    val recordedAttempts = mutableListOf<String>()

    override fun blockedNumbers() = MutableStateFlow(blockedEntries)

    override fun allowlistedNumbers() = MutableStateFlow(allowlistedEntries)

    override fun patternRules() = MutableStateFlow(emptyList<PatternRuleEntry>())

    override fun countryRules() = MutableStateFlow(emptyList<CountryRuleEntry>())

    override fun actionRules() = MutableStateFlow(emptyList<ActionRuleEntry>())

    override fun scheduleRules() = MutableStateFlow(emptyList<ScheduleRuleEntry>())

    override suspend fun blockedNumberEntries() = blockedEntries

    override suspend fun allowlistedNumberEntries() = allowlistedEntries

    override suspend fun enabledPatterns(): List<PatternRule> = emptyList()

    override suspend fun enabledCountryRules(): List<CountryRuleEntry> = emptyList()

    override suspend fun enabledActionRules(): List<ActionRule> = emptyList()

    override suspend fun enabledScheduleRules(): List<ScheduleRule> = emptyList()

    override suspend fun recordCallAttempt(
        number: String,
        timestampMillis: Long,
    ) {
        recordedAttempts += number
    }

    override suspend fun countRecentAttempts(
        number: String,
        sinceTimestampMillis: Long,
    ) = 0

    override suspend fun deleteExpiredAttempts(beforeTimestampMillis: Long) {}

    override suspend fun addBlockedNumber(
        number: String,
        label: String?,
    ) {}

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

    override suspend fun exportAll() = "{}"

    override suspend fun importAll(json: String) = ImportResult()
}

private class FakeContactsGateway(
    private val granted: Boolean = false,
    private val numbers: Set<String> = emptySet(),
) : ContactsGateway {
    override suspend fun contactNumbers(): Set<String> = numbers

    override fun hasPermission(): Boolean = granted
}

private class FakeSettingsRepository(
    override val blockingEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
    override val autoAllowContacts: MutableStateFlow<Boolean> = MutableStateFlow(false),
    override val defaultAction: MutableStateFlow<DefaultAction> = MutableStateFlow(DefaultAction.ALLOW),
    override val notificationsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true),
) : SettingsRepository {
    override val welcomeShown = true

    override suspend fun setBlockingEnabled(enabled: Boolean) {}

    override suspend fun setAutoAllowContacts(enabled: Boolean) {}

    override suspend fun setDefaultAction(action: DefaultAction) {}

    override suspend fun setNotificationsEnabled(enabled: Boolean) {}

    override suspend fun setWelcomeShown() {}
}

private class FakeSpamProviderRepository(
    enabled: Boolean = false,
) : SpamProviderRepository {
    override val enabled = MutableStateFlow(enabled)

    override suspend fun setEnabled(enabled: Boolean) {}
}

private object NoOpSpamClient : SpamProviderClient {
    override suspend fun lookup(number: String): SpamResult? = null
}

class EvaluateIncomingCallUseCaseTest {
    @Test
    fun blockingDisabled_alwaysAllows() =
        runTest {
            val ruleRepository =
                FakeRuleRepository().apply {
                    blockedEntries = listOf(BlockedNumberEntry(1, "+34600123456", null, 0))
                }
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(blockingEnabled = MutableStateFlow(false)),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun manualBlockMatch_returnsManualBlockDecision() =
        runTest {
            val ruleRepository =
                FakeRuleRepository().apply {
                    blockedEntries = listOf(BlockedNumberEntry(7, "+34600123456", "Spam caller", 0))
                }
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.ManualBlock)
            assertEquals(7, (decision as RuleDecision.ManualBlock).ruleId)
            assertEquals("Spam caller", decision.label)
        }

    @Test
    fun allowlistRuleMatch_carriesRuleIdAndLabel() =
        runTest {
            val ruleRepository =
                FakeRuleRepository().apply {
                    allowlistedEntries = listOf(AllowlistedNumberEntry(3, "+34600123456", "Mom's new number", 0))
                }
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.Allowlist)
            assertEquals(3, (decision as RuleDecision.Allowlist).ruleId)
            assertEquals("Mom's new number", decision.label)
        }

    @Test
    fun contactOnlyMatch_hasNoRuleIdOrLabel() =
        runTest {
            val ruleRepository = FakeRuleRepository()
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(granted = true, numbers = setOf("+34600123456")),
                    FakeSettingsRepository(autoAllowContacts = MutableStateFlow(true)),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.Allowlist)
            assertEquals(null, (decision as RuleDecision.Allowlist).label)
        }

    @Test
    fun blankNumber_skipsAttemptTracking() =
        runTest {
            val ruleRepository = FakeRuleRepository()
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            useCase.evaluate("")

            assertTrue(ruleRepository.recordedAttempts.isEmpty())
        }

    @Test
    fun nonBlankNumber_recordsAttempt() =
        runTest {
            val ruleRepository = FakeRuleRepository()
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            useCase.evaluate("+34600123456")

            assertEquals(listOf("+34600123456"), ruleRepository.recordedAttempts)
        }

    @Test
    fun noMatch_honorsDefaultActionBlock() =
        runTest {
            val ruleRepository = FakeRuleRepository()
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(defaultAction = MutableStateFlow(DefaultAction.BLOCK)),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.DefaultBlock)
        }
}
