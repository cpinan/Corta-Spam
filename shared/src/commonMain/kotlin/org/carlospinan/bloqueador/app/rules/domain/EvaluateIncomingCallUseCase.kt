package org.carlospinan.bloqueador.app.rules.domain

import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.EmergencyCallPolicy
import org.carlospinan.bloqueador.app.rules.ResolveContext
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.rules.RulePrecedenceResolver
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.rules.currentTimeMillis
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import org.carlospinan.bloqueador.app.spam.SpamProviderClient
import org.carlospinan.bloqueador.app.spam.SpamProviderRepository

/**
 * Evaluates an incoming number against the full rule set. Extracted out of the Android
 * Telecom entry point (PassthroughInCallService) so the app's core call-blocking decision
 * is testable in commonTest with fake repositories, the same way every other feature is --
 * this was previously the only untested piece of non-trivial orchestration in the app.
 */
class EvaluateIncomingCallUseCase(
    private val ruleRepository: RuleRepository,
    private val contactsGateway: ContactsGateway,
    private val settingsRepository: SettingsRepository,
    private val spamProviderRepository: SpamProviderRepository,
    private val spamProvider: SpamProviderClient,
) {
    /**
     * [inEmergencyCallbackMode] is the platform's own `PROPERTY_EMERGENCY_CALLBACK_MODE` on the
     * incoming call. Defaulted so every existing caller and test is unaffected; only the Telecom
     * entry point has one to pass.
     */
    suspend fun evaluate(
        number: String,
        inEmergencyCallbackMode: Boolean = false,
    ): RuleDecision {
        if (!settingsRepository.blockingEnabled.first()) {
            return RuleDecision.DefaultAllow
        }
        val now = currentTimeMillis()

        // Track how long the platform has been claiming callback mode, so the exemption below can
        // be time-boxed on that signal the same way it already is on our own timestamp. Recorded
        // on the first call that reports it and cleared on the first that does not, so a genuine
        // second emergency restarts the window while a platform that never clears the flag cannot
        // hold the exemption open forever. See EmergencyCallPolicy.isExempt.
        val callbackModeSince = settingsRepository.emergencyCallbackModeSinceMillis.value
        val effectiveCallbackModeSince =
            when {
                inEmergencyCallbackMode && callbackModeSince <= EmergencyCallPolicy.NEVER -> {
                    settingsRepository.setEmergencyCallbackModeSince(now)
                    now
                }
                !inEmergencyCallbackMode && callbackModeSince > EmergencyCallPolicy.NEVER -> {
                    settingsRepository.setEmergencyCallbackModeSince(EmergencyCallPolicy.NEVER)
                    EmergencyCallPolicy.NEVER
                }
                else -> callbackModeSince
            }

        // Before every rule, including a manual block. A number on the blocklist ringing inside
        // the callback window is far more likely to be a dispatcher on a shared line than the
        // spammer the user blocked -- and the cost of being wrong the other way is an ambulance
        // rejected, or answered by the auto-responder and hung up on. See EmergencyCallPolicy.
        if (EmergencyCallPolicy.isExempt(
                exemptionEnabled = settingsRepository.emergencyCallbackExemption.value,
                inEmergencyCallbackMode = inEmergencyCallbackMode,
                nowMillis = now,
                lastEmergencyCallAtMillis = settingsRepository.lastEmergencyCallAtMillis.value,
                callbackModeSinceMillis = effectiveCallbackModeSince,
            )
        ) {
            return RuleDecision.EmergencyExempt
        }
        val contactNumbers =
            if (settingsRepository.autoAllowContacts.first() && contactsGateway.hasPermission()) {
                contactsGateway.contactNumbers()
            } else {
                emptySet()
            }
        val spamEnabled = spamProviderRepository.enabled.first()
        val defaultAction = settingsRepository.defaultAction.first()
        val actionRules = ruleRepository.enabledActionRules()

        // Private/restricted callers report an empty number — skip attempt tracking for them so
        // unrelated withheld-number calls don't share a bucket and trip action rules together.
        if (number.isNotBlank()) {
            ruleRepository.recordCallAttempt(number, now)
        }
        ruleRepository.deleteExpiredAttempts(now - 24L * 60L * 60L * 1000L)

        val repeatedAttemptsBypassCount = settingsRepository.repeatedCallerBypassCount.first()
        // Reuses the same 24h retention deleteExpiredAttempts already enforces above -- no
        // separate window setting needed. Includes the attempt just recorded, so the Nth call
        // is the one that gets through, matching "if this number calls me N times" literally.
        val recentAttemptsForNumber =
            if (number.isBlank()) {
                0
            } else {
                ruleRepository.countRecentAttempts(number, now - 24L * 60L * 60L * 1000L)
            }

        val attemptCountsByWindow =
            if (number.isBlank()) {
                emptyMap()
            } else {
                actionRules
                    .map { it.windowMinutes }
                    .distinct()
                    .associateWith { windowMinutes ->
                        val since = now - windowMinutes * 60L * 1000L
                        ruleRepository.countRecentAttempts(number, since)
                    }
            }

        val blockedEntries = ruleRepository.blockedNumberEntries()
        val allowlistedEntries = ruleRepository.allowlistedNumberEntries()
        val countryEntries = ruleRepository.enabledCountryRules()
        val context =
            ResolveContext(
                allowlistedNumbers = allowlistedEntries.map { it.number }.toSet(),
                allowlistedNumberDetails = allowlistedEntries.associateBy { it.number },
                contactNumbers = contactNumbers,
                blockedNumbers = blockedEntries.map { it.number }.toSet(),
                blockedNumberDetails = blockedEntries.associateBy { it.number },
                enabledPatterns = ruleRepository.enabledPatterns(),
                allPatterns = ruleRepository.allPatterns(),
                enabledCountryCodes = countryEntries.map { it.countryCode }.toSet(),
                countryRuleDetails = countryEntries.associateBy { it.countryCode },
                spamProvider = spamProvider,
                spamEnabled = spamEnabled,
                enabledActionRules = actionRules,
                attemptCountsByWindowMinutes = attemptCountsByWindow,
                enabledScheduleRules = ruleRepository.enabledScheduleRules(),
                currentLocalMinuteOfDay = currentLocalMinuteOfDay(now),
                defaultAction = defaultAction,
                repeatedAttemptsBypassCount = repeatedAttemptsBypassCount,
                recentAttemptsForNumber = recentAttemptsForNumber,
            )
        return RulePrecedenceResolver.evaluate(number, context)
    }

    // Quiet hours are a wall-clock concept, so this must use the local calendar day/time
    // (DST-aware via kotlinx-datetime), not a UTC epoch computation.
    private fun currentLocalMinuteOfDay(epochMillis: Long): Int {
        val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        return local.hour * 60 + local.minute
    }
}
