package org.carlospinan.bloqueador.app.rules.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.EmergencyCallPolicy
import org.carlospinan.bloqueador.app.rules.PatternRuleEntry
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.rules.currentTimeMillis
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

    /**
     * The exemption is checked before every rule, including a manual block. A number on the
     * blocklist ringing minutes after the user dialled 112 is far more likely to be a dispatcher
     * on a shared line than the spammer they blocked -- and being wrong the other way means an
     * ambulance rejected, or answered by the auto-responder and hung up on.
     */
    @Test
    fun emergencyWindow_beatsEvenAManualBlock() =
        runTest {
            val ruleRepository =
                FakeRuleRepository().apply {
                    blockedNumbersFlow.value = listOf(BlockedNumberEntry(1, "+34600123456", null, 0))
                }
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(
                        lastEmergencyCallAtMillis = MutableStateFlow(currentTimeMillis()),
                    ),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.EmergencyExempt)
            assertEquals(false, decision.isBlocked)
        }

    /** Switched off, the blocklist wins again -- the exemption is the user's choice to make. */
    @Test
    fun emergencyWindow_withTheExemptionOff_blocksAsBefore() =
        runTest {
            val ruleRepository =
                FakeRuleRepository().apply {
                    blockedNumbersFlow.value = listOf(BlockedNumberEntry(1, "+34600123456", null, 0))
                }
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(
                        emergencyCallbackExemption = MutableStateFlow(false),
                        lastEmergencyCallAtMillis = MutableStateFlow(currentTimeMillis()),
                    ),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.ManualBlock)
        }

    /** Long after the emergency call, ordinary screening resumes. */
    @Test
    fun anOldEmergencyCall_doesNotExemptForever() =
        runTest {
            val ruleRepository =
                FakeRuleRepository().apply {
                    blockedNumbersFlow.value = listOf(BlockedNumberEntry(1, "+34600123456", null, 0))
                }
            val stale = currentTimeMillis() - EmergencyCallPolicy.CALLBACK_WINDOW_MILLIS - 1
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    FakeSettingsRepository(lastEmergencyCallAtMillis = MutableStateFlow(stale)),
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            val decision = useCase.evaluate("+34600123456")

            assertTrue(decision is RuleDecision.ManualBlock)
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

    /**
     * A platform stuck in emergency callback mode stops exempting once the window has passed.
     *
     * The end-to-end half of `EmergencyCallPolicyTest`: this asserts the use case actually
     * *maintains* the marker the policy reads. First call arrives with callback mode set and
     * nothing recorded, so it starts the window and is exempt; a call thirty minutes later, with
     * the platform still claiming callback mode, is blocked again.
     *
     * Reproduced on a Pixel 8 Pro API 36 emulator that had dialled 112 the previous day and never
     * stopped reporting `PROPERTY_EMERGENCY_CALLBACK_MODE`. Every rule was short-circuited for as
     * long as it did, which is call blocking silently switched off.
     */
    @Test
    fun stuckCallbackMode_stopsExemptingAfterTheWindow() =
        runTest {
            val ruleRepository =
                FakeRuleRepository().apply {
                    blockedNumbersFlow.value = listOf(BlockedNumberEntry(1, "+34600123456", null, 0))
                }
            val settings = FakeSettingsRepository()
            val useCase =
                EvaluateIncomingCallUseCase(
                    ruleRepository,
                    FakeContactsGateway(),
                    settings,
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            // First call in callback mode: the window opens here, so this one is exempt.
            val first = useCase.evaluate("+34600123456", inEmergencyCallbackMode = true)
            assertTrue(first is RuleDecision.EmergencyExempt)

            // Wind the recorded start back past the window, as a stuck flag would look later on.
            settings.emergencyCallbackModeSinceMillis.value =
                currentTimeMillis() - EmergencyCallPolicy.CALLBACK_WINDOW_MILLIS - 1

            val later = useCase.evaluate("+34600123456", inEmergencyCallbackMode = true)
            assertTrue(later is RuleDecision.ManualBlock, "a stuck flag must not exempt forever")
        }

    /** Callback mode clearing resets the window, so a second emergency is protected too. */
    @Test
    fun callbackModeClearing_restartsTheWindow() =
        runTest {
            val settings =
                FakeSettingsRepository(
                    emergencyCallbackModeSinceMillis =
                        MutableStateFlow(currentTimeMillis() - EmergencyCallPolicy.CALLBACK_WINDOW_MILLIS - 1),
                )
            val useCase =
                EvaluateIncomingCallUseCase(
                    FakeRuleRepository().apply {
                        blockedNumbersFlow.value = listOf(BlockedNumberEntry(1, "+34600123456", null, 0))
                    },
                    FakeContactsGateway(),
                    settings,
                    FakeSpamProviderRepository(),
                    NoOpSpamClient,
                )

            // A call with the platform no longer claiming callback mode clears the marker...
            useCase.evaluate("+34600123456", inEmergencyCallbackMode = false)
            assertEquals(EmergencyCallPolicy.NEVER, settings.emergencyCallbackModeSinceMillis.value)

            // ...so when it returns, the window starts again rather than reading as long expired.
            val decision = useCase.evaluate("+34600123456", inEmergencyCallbackMode = true)
            assertTrue(decision is RuleDecision.EmergencyExempt)
        }
}
