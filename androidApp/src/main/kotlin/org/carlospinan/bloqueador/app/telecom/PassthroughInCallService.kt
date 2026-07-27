package org.carlospinan.bloqueador.app.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.ResolveContext
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.rules.RulePrecedenceResolver
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * M2: Evaluates incoming calls against the rule set.
 * Allowed calls pass through to the UI; blocked calls are rejected immediately
 * and logged with the firing rule's reason.
 */
class PassthroughInCallService :
    InCallService(),
    KoinComponent {
    private val ruleRepository: RuleRepository by inject()
    private val callLogRepository: CallLogRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val number =
            call.details
                ?.handle
                ?.schemeSpecificPart
                .orEmpty()

        serviceScope.launch {
            val decision = evaluateCall(number)
            if (decision.isBlocked) {
                call.reject(false, null)
                callLogRepository.logCall(
                    number = number,
                    timestamp = currentTimestamp(),
                    decision = decision,
                )
            } else {
                InCallState.attach(call)
                callLogRepository.logCall(
                    number = number,
                    timestamp = currentTimestamp(),
                    decision = decision,
                )
                startActivity(
                    Intent(this@PassthroughInCallService, InCallActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        InCallState.detach(call)
    }

    private suspend fun evaluateCall(number: String): RuleDecision {
        val context =
            ResolveContext(
                allowlistedNumbers = ruleRepository.allowlistedNumberSet(),
                blockedNumbers = ruleRepository.blockedNumberSet(),
                enabledPatterns = ruleRepository.enabledPatterns(),
                enabledCountryCodes = ruleRepository.enabledCountryCodeSet(),
            )
        return RulePrecedenceResolver.evaluate(number, context)
    }

    private fun currentTimestamp(): Long = System.currentTimeMillis()
}
