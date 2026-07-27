package org.carlospinan.bloqueador.app.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderRepository
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.ResolveContext
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.rules.RulePrecedenceResolver
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import org.carlospinan.bloqueador.app.spam.SpamProviderClient
import org.carlospinan.bloqueador.app.spam.SpamProviderRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Evaluates incoming calls against the rule set.
 * Blocked calls are rejected, or answered + greeted when auto-responder is on.
 */
class PassthroughInCallService :
    InCallService(),
    KoinComponent {
    private val ruleRepository: RuleRepository by inject()
    private val callLogRepository: CallLogRepository by inject()
    private val contactsGateway: ContactsGateway by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val spamProviderRepository: SpamProviderRepository by inject()
    private val spamProvider: SpamProviderClient by inject()
    private val autoResponderRepository: AutoResponderRepository by inject()
    private var serviceScope: CoroutineScope? = null
    private var autoResponderAudio: AutoResponderAudio? = null
    private var activeCall: Call? = null

    private val callCallback =
        object : Call.Callback() {
            override fun onStateChanged(
                call: Call,
                state: Int,
            ) {
                if (state == Call.STATE_ACTIVE) {
                    serviceScope?.launch {
                        val config = autoResponderRepository.config.first()
                        autoResponderAudio?.play(
                            script = config.script,
                            audioUri = config.audioUri.ifBlank { null },
                            onComplete = {
                                call.disconnect()
                            },
                        )
                    }
                }
            }
        }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        autoResponderAudio = AutoResponderAudio(this).also { it.prepare() }
        activeCall = call

        val number =
            call.details
                ?.handle
                ?.schemeSpecificPart
                .orEmpty()

        serviceScope?.launch {
            val decision = evaluateCall(number)
            callLogRepository.logCall(
                number = number,
                timestamp = currentTimestamp(),
                decision = decision,
            )
            if (decision.isBlocked) {
                val autoConfig = autoResponderRepository.config.first()
                if (autoConfig.enabled &&
                    autoConfig.validate() is org.carlospinan.bloqueador.app.autoresponder.AutoResponderConfig.ValidationResult.Ok
                ) {
                    call.registerCallback(callCallback)
                    call.answer(VideoProfile.STATE_AUDIO_ONLY)
                } else {
                    call.reject(false, null)
                }
            } else {
                InCallState.attach(call)
                startActivity(
                    Intent(this@PassthroughInCallService, InCallActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        InCallState.detach(call)
        if (activeCall == call) {
            activeCall = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoResponderAudio?.release()
        autoResponderAudio = null
        serviceScope?.cancel()
        serviceScope = null
    }

    private suspend fun evaluateCall(number: String): RuleDecision {
        if (!settingsRepository.blockingEnabled.first()) {
            return RuleDecision.DefaultAllow
        }
        val now = currentTimestamp()
        val contactNumbers =
            if (contactsGateway.hasPermission()) {
                contactsGateway.contactNumbers()
            } else {
                emptySet()
            }
        val spamEnabled = spamProviderRepository.enabled.first()
        val actionRules = ruleRepository.enabledActionRules()

        ruleRepository.recordCallAttempt(number, now)
        ruleRepository.deleteExpiredAttempts(now - 24L * 60L * 60L * 1000L)

        val attemptCountsByWindow =
            actionRules
                .map { it.windowMinutes }
                .distinct()
                .associateWith { windowMinutes ->
                    val since = now - windowMinutes * 60L * 1000L
                    ruleRepository.countRecentAttempts(number, since)
                }

        val context =
            ResolveContext(
                allowlistedNumbers = ruleRepository.allowlistedNumberSet(),
                contactNumbers = contactNumbers,
                blockedNumbers = ruleRepository.blockedNumberSet(),
                enabledPatterns = ruleRepository.enabledPatterns(),
                enabledCountryCodes = ruleRepository.enabledCountryCodeSet(),
                spamProvider = spamProvider,
                spamEnabled = spamEnabled,
                enabledActionRules = actionRules,
                attemptCountsByWindowMinutes = attemptCountsByWindow,
            )
        return RulePrecedenceResolver.evaluate(number, context)
    }

    private fun currentTimestamp(): Long = System.currentTimeMillis()
}
