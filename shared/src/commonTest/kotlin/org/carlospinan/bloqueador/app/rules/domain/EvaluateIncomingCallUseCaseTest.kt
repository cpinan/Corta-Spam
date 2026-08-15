package org.carlospinan.bloqueador.app.rules.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.settings.DefaultAction
import org.carlospinan.bloqueador.app.spam.SpamProviderClient
import org.carlospinan.bloqueador.app.spam.SpamResult
import org.carlospinan.bloqueador.app.testing.FakeRuleRepository
import org.carlospinan.bloqueador.app.testing.FakeSettingsRepository
import org.carlospinan.bloqueador.app.testing.FakeSpamProviderRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeContactsGateway(
    private val granted: Boolean = false,
    private val numbers: Set<String> = emptySet(),
) : ContactsGateway {
    override suspend fun contactNumbers(): Set<String> = numbers

    override suspend fun contactNames(): Map<String, String> = emptyMap()

    override suspend fun contacts(): List<Contact> = emptyList()

    override fun hasPermission(): Boolean = granted
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
                    blockedNumbersFlow.value = listOf(BlockedNumberEntry(1, "+34600123456", null, 0))
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
                    blockedNumbersFlow.value = listOf(BlockedNumberEntry(7, "+34600123456", "Spam caller", 0))
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
                    allowlistedNumbersFlow.value = listOf(AllowlistedNumberEntry(3, "+34600123456", "Mom's new number", 0))
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

    /**
     * The reported bug: a number in the address book was blocked anyway, by a pattern rule.
     *
     * Both spellings of the same subscriber have to survive step 2, because neither side is
     * under the app's control — the card is written by the user and the handle is written by the
     * network, and a domestic call is routinely delivered without a country code. A contact that
     * fails to match here falls through to the pattern rule below, which matches on digits alone
     * and has no idea it is looking at somebody's mother.
     */
    @Test
    fun contactIsAllowlistedWhicheverSideStatesItsCountry() =
        runTest {
            val savedForms = listOf("+34611998877", "611 99 88 77")
            val incomingForms = listOf("+34611998877", "611998877")

            for (saved in savedForms) {
                for (incoming in incomingForms) {
                    val ruleRepository =
                        FakeRuleRepository().apply {
                            // Matches every 9-digit Spanish mobile starting 611, contact or not.
                            patternRulesFlow.value = listOf(PatternRuleEntry(1, "611*", "Mobile spam", true, 0))
                        }
                    val useCase =
                        EvaluateIncomingCallUseCase(
                            ruleRepository,
                            FakeContactsGateway(granted = true, numbers = setOf(saved)),
                            FakeSettingsRepository(autoAllowContacts = MutableStateFlow(true)),
                            FakeSpamProviderRepository(),
                            NoOpSpamClient,
                        )

                    val decision = useCase.evaluate(incoming)

                    assertTrue(
                        decision is RuleDecision.Allowlist,
                        "contact saved \"$saved\" called from \"$incoming\" was $decision",
                    )
                }
            }
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

    @Test
    fun repeatedUnknownCaller_allowedAfterBypassThreshold() =
        runTest {
            val ruleRepository = FakeRuleRepository().apply { recentAttemptsForNumber = 3 }
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(
                        defaultAction = MutableStateFlow(DefaultAction.BLOCK),
                        repeatedCallerBypassCount = MutableStateFlow(3),
                    ),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.AllowedAfterRepeatedAttempts)
            assertEquals(3, (decision as RuleDecision.AllowedAfterRepeatedAttempts).attempts)
        }
}
