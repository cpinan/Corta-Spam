package org.carlospinan.bloqueador.app.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.R
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderRepository
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.domain.EvaluateIncomingCallUseCase
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Telecom entry point for incoming calls. Delegates the actual rule evaluation to
 * [EvaluateIncomingCallUseCase] and only handles Android/Telecom-specific orchestration
 * (notifications, answering/rejecting the call, auto-responder playback) around the result.
 */
class PassthroughInCallService :
    InCallService(),
    KoinComponent {
    private val evaluateIncomingCall: EvaluateIncomingCallUseCase by inject()
    private val callLogRepository: CallLogRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val autoResponderRepository: AutoResponderRepository by inject()
    private var serviceScope: CoroutineScope? = null
    private var autoResponderAudio: AutoResponderAudio? = null
    private var activeCall: Call? = null
    private var callWasBlockedByRules = false

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

    // Clears the full-screen ringing notification the moment the call stops ringing --
    // answered, declined, or hung up by the other side -- regardless of which UI (in-app,
    // notification action, or the system) drove the state change. Also drives the
    // "return to call" notification while active, since InCallActivity leaves nothing behind
    // once dismissed (excludeFromRecents=true).
    private val notificationCallback =
        object : Call.Callback() {
            override fun onStateChanged(
                call: Call,
                state: Int,
            ) {
                if (state != Call.STATE_RINGING) {
                    IncomingCallNotifier.cancel(this@PassthroughInCallService)
                }
                if (state == Call.STATE_ACTIVE) {
                    val number =
                        call.details
                            ?.handle
                            ?.schemeSpecificPart
                            .orEmpty()
                    if (settingsRepository.notificationsEnabled.value) {
                        IncomingCallNotifier.notifyOngoingCall(this@PassthroughInCallService, number)
                    }
                } else {
                    IncomingCallNotifier.cancelOngoing(this@PassthroughInCallService)
                }
            }
        }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        serviceScope?.cancel()
        autoResponderAudio?.release()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        autoResponderAudio = AutoResponderAudio(this).also { it.prepare() }
        activeCall = call
        callWasBlockedByRules = false

        val number =
            call.details
                ?.handle
                ?.schemeSpecificPart
                .orEmpty()

        InCallState.attach(call)
        startActivity(
            Intent(this@PassthroughInCallService, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        call.registerCallback(notificationCallback)
        if (call.state == Call.STATE_RINGING && settingsRepository.notificationsEnabled.value) {
            IncomingCallNotifier.notifyIncomingCall(this, number)
        }

        serviceScope?.launch {
            try {
                val decision = evaluateIncomingCall.evaluate(number)
                callLogRepository.logCall(
                    number = number,
                    timestamp = currentTimestamp(),
                    decision = decision,
                )
                if (decision.isBlocked) {
                    callWasBlockedByRules = true
                    if (settingsRepository.notificationsEnabled.value) {
                        IncomingCallNotifier.notifyCallResult(
                            this@PassthroughInCallService,
                            number,
                            R.string.notification_blocked_call_title,
                            decision.blockReason,
                        )
                    }
                    val autoConfig = autoResponderRepository.config.first()
                    if (autoConfig.enabled &&
                        autoConfig.validate() is org.carlospinan.bloqueador.app.autoresponder.AutoResponderConfig.ValidationResult.Ok
                    ) {
                        call.registerCallback(callCallback)
                        call.answer(VideoProfile.STATE_AUDIO_ONLY)
                    } else {
                        call.reject(false, null)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Call evaluation failed, failing open", e)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        call.unregisterCallback(notificationCallback)
        IncomingCallNotifier.cancel(this)
        IncomingCallNotifier.cancelOngoing(this)
        if (!callWasBlockedByRules &&
            call.details?.disconnectCause?.code == DisconnectCause.MISSED &&
            settingsRepository.notificationsEnabled.value
        ) {
            val number =
                call.details
                    ?.handle
                    ?.schemeSpecificPart
                    .orEmpty()
            IncomingCallNotifier.notifyCallResult(this, number, R.string.notification_missed_call_title, reason = null)
        }
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

    private fun currentTimestamp(): Long = System.currentTimeMillis()

    private companion object {
        const val TAG = "PassthroughInCallService"
    }
}
