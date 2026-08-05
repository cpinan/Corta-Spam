package org.carlospinan.bloqueador.app.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.R
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderConfig
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderRepository
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.rules.domain.EvaluateIncomingCallUseCase
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Telecom entry point for incoming calls. Rule evaluation lives in
 * [EvaluateIncomingCallUseCase]; this handles only the Android/Telecom orchestration around the
 * result — ringing, notifications, answering/rejecting, auto-responder playback.
 *
 * State is tracked **per call** in [callStates]. It used to live in single fields that a second
 * `onCallAdded` overwrote, and the service cancelled its whole coroutine scope on every new
 * call: a call arriving while another was in progress silently killed the first call's in-flight
 * evaluation, so that call was never blocked and never logged.
 */
class PassthroughInCallService :
    InCallService(),
    KoinComponent {
    private val evaluateIncomingCall: EvaluateIncomingCallUseCase by inject()
    private val callLogRepository: CallLogRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val autoResponderRepository: AutoResponderRepository by inject()

    /** Lives for the whole service, not one call, so a second call can't cancel the first's work. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val ringer by lazy { CallRinger(this) }

    /**
     * Only built when a call is actually going to be auto-answered. Constructing it binds a
     * text-to-speech engine, which used to happen on every incoming call whether or not the
     * auto-responder was switched on.
     */
    private var autoResponderAudio: AutoResponderAudio? = null

    private val callStates = mutableMapOf<Call, CallState>()

    private class CallState {
        var blockedByRules = false
        var evaluation: Job? = null
    }

    private val autoResponderCallback =
        object : Call.Callback() {
            override fun onStateChanged(
                call: Call,
                state: Int,
            ) {
                if (state != Call.STATE_ACTIVE) return
                // The greeting reaches the caller only through the handset's own microphone, so
                // it has to come out of the speaker; on the earpiece the mic picks up nothing
                // and the caller hears silence.
                setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                serviceScope.launch {
                    val config = autoResponderRepository.config.first()
                    autoResponderAudio?.play(
                        script = config.script,
                        audioUri = config.audioUri.ifBlank { null },
                        onComplete = { call.disconnect() },
                    )
                }
            }
        }

    // Drives the ringtone and the notifications off the real call state, whichever UI (in-app,
    // notification action, or the system) caused the change.
    private val stateCallback =
        object : Call.Callback() {
            override fun onStateChanged(
                call: Call,
                state: Int,
            ) {
                if (state != Call.STATE_RINGING) {
                    ringer.stop()
                    IncomingCallNotifier.cancel(this@PassthroughInCallService)
                }
                if (state == Call.STATE_ACTIVE) {
                    // A call answered only to play the auto-responder greeting isn't a call the
                    // user is on; offering them a "return to call" notification for it is noise.
                    if (callStates[call]?.blockedByRules != true && settingsRepository.notificationsEnabled.value) {
                        IncomingCallNotifier.notifyOngoingCall(this@PassthroughInCallService, call.handleNumber())
                    }
                } else {
                    IncomingCallNotifier.cancelOngoing(this@PassthroughInCallService)
                }
            }
        }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val state = CallState()
        callStates[call] = state

        val number = call.handleNumber()

        InCallState.attach(call)
        startActivity(
            Intent(this@PassthroughInCallService, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        call.registerCallback(stateCallback)
        if (call.state == Call.STATE_RINGING) {
            // Ring first, decide second. Evaluation touches the database and the contacts
            // provider, and a caller must never be dropped into silence because that was slow --
            // a blocked call is silenced a few hundred milliseconds later by ringer.stop().
            ringer.start()
            if (settingsRepository.notificationsEnabled.value) {
                IncomingCallNotifier.notifyIncomingCall(this, number)
            }
        }

        state.evaluation =
            serviceScope.launch {
                try {
                    val decision = evaluateIncomingCall.evaluate(number)
                    callLogRepository.logCall(
                        number = number,
                        timestamp = currentTimestamp(),
                        decision = decision,
                    )
                    if (decision.isBlocked) {
                        state.blockedByRules = true
                        ringer.stop()
                        IncomingCallNotifier.cancel(this@PassthroughInCallService)
                        if (settingsRepository.notificationsEnabled.value) {
                            IncomingCallNotifier.notifyCallResult(
                                this@PassthroughInCallService,
                                number,
                                R.string.notification_blocked_call_title,
                                decision.reason?.let { BlockReasonStrings.format(this@PassthroughInCallService, it) },
                            )
                        }
                        val autoConfig = autoResponderRepository.config.first()
                        if (autoConfig.enabled && autoConfig.validate() is AutoResponderConfig.ValidationResult.Ok) {
                            autoResponderAudio = AutoResponderAudio(this@PassthroughInCallService).also { it.prepare() }
                            call.registerCallback(autoResponderCallback)
                            call.answer(VideoProfile.STATE_AUDIO_ONLY)
                        } else {
                            call.reject(false, null)
                        }
                    } else if (decision is RuleDecision.AllowedAfterRepeatedAttempts) {
                        // Not blocked -- the call keeps ringing normally. The only extra step is
                        // telling the user why an unrecognized number is getting through.
                        InCallState.setRepeatedCallAttempts(decision.attempts)
                        if (settingsRepository.notificationsEnabled.value) {
                            IncomingCallNotifier.notifyCallResult(
                                this@PassthroughInCallService,
                                number,
                                R.string.notification_repeated_caller_title,
                                decision.reason?.let { BlockReasonStrings.format(this@PassthroughInCallService, it) },
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Fail open: an unblocked spam call is a nuisance, a silently dropped real
                    // one is a missed emergency. The call is already ringing and stays ringing.
                    Log.e(TAG, "Call evaluation failed, failing open", e)
                }
            }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        val state = callStates.remove(call)
        state?.evaluation?.cancel()
        call.unregisterCallback(autoResponderCallback)
        call.unregisterCallback(stateCallback)

        // Only silence the ringer if nothing else is still ringing; with call waiting, one call
        // ending must not mute the other.
        if (callStates.keys.none { it.state == Call.STATE_RINGING }) {
            ringer.stop()
            IncomingCallNotifier.cancel(this)
        }
        if (callStates.isEmpty()) {
            IncomingCallNotifier.cancelOngoing(this)
            autoResponderAudio?.release()
            autoResponderAudio = null
        }

        if (state?.blockedByRules != true &&
            call.details?.disconnectCause?.code == DisconnectCause.MISSED &&
            settingsRepository.notificationsEnabled.value
        ) {
            IncomingCallNotifier.notifyCallResult(
                this,
                call.handleNumber(),
                R.string.notification_missed_call_title,
                reason = null,
            )
        }
        InCallState.detach(call)
    }

    override fun onDestroy() {
        super.onDestroy()
        ringer.stop()
        autoResponderAudio?.release()
        autoResponderAudio = null
        callStates.clear()
        serviceScope.cancel()
    }

    private fun Call.handleNumber(): String =
        details
            ?.handle
            ?.schemeSpecificPart
            .orEmpty()

    private fun currentTimestamp(): Long = System.currentTimeMillis()

    private companion object {
        const val TAG = "PassthroughInCallService"
    }
}
