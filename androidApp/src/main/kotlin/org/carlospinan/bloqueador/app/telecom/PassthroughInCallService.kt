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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.R
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderConfig
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderRepository
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.contacts.contactDisplayName
import org.carlospinan.bloqueador.app.contacts.isKnownContact
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.EmergencyNumbers
import org.carlospinan.bloqueador.app.rules.PhoneNumberParser
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.rules.RuleRepository
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
    private val contactsGateway: ContactsGateway by inject()
    private val autoResponderRepository: AutoResponderRepository by inject()
    private val ruleRepository: RuleRepository by inject()

    /** Lives for the whole service, not one call, so a second call can't cancel the first's work. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val ringer by lazy { CallRinger(this) }

    /**
     * Only built when a call is actually going to be auto-answered. Constructing it binds a
     * text-to-speech engine, which used to happen on every incoming call whether or not the
     * auto-responder was switched on.
     */
    private var autoResponderAudio: AutoResponderAudio? = null

    /**
     * Per call, keyed by [Call]. Only ever touched from [serviceScope], which is confined to the
     * main dispatcher: Telecom delivers its callbacks there, but the auto-responder's completion
     * callbacks arrive on a TTS or MediaPlayer thread, and those used to mutate this map and the
     * recorder field directly.
     */
    private val callStates = mutableMapOf<Call, CallState>()

    /**
     * Whether to post an after-the-fact notification (blocked / missed / repeated caller) for
     * [number]. The ringing notification does not go through here — see [NotificationPolicy].
     *
     * "Is this one of my contacts" is answered by [ContactsGateway], the same source the rule
     * engine and every screen use, and NOT by ContactNameLookup's PhoneLookup query. PhoneLookup
     * matches through the provider's region-derived `data4` column, so it misses a contact saved
     * nationally when the call arrives in E.164 — verified on an emulator, where every contact
     * read as a stranger and a real contact's missed call was silenced.
     *
     * Suspend because that gateway is: it is cached for five minutes, so this costs a map lookup
     * on all but the first call after a change.
     */
    private suspend fun shouldNotifyResult(number: String): Boolean {
        // Asked once and reused: a denied read returns an empty address book, and the gateway
        // would cache that emptiness for its whole TTL, so don't make it query at all.
        val contactsAccessGranted = contactsGateway.hasPermission()
        return NotificationPolicy.shouldNotifyCallResult(
            notificationsEnabled = settingsRepository.notificationsEnabled.value,
            notifyUnknownCallers = settingsRepository.notifyUnknownCallers.value,
            contactsAccessGranted = contactsAccessGranted,
            callerIsInContacts = contactsAccessGranted && isKnownContact(number, contactsGateway.contactNames()),
        )
    }

    private class CallState {
        var blockedByRules = false
        var evaluation: Job? = null

        /**
         * Ends a blocked call the app cannot leave connected: the greeting that never started or
         * never finished, and the rejection Telecom did not act on. Cancelled by whichever of
         * those actually happened.
         */
        var watchdog: Job? = null

        /** Whether the auto-responder greeting has produced sound. See [BlockedCallPolicy]. */
        var greetingStarted = false

        /**
         * Row id from [CallLogRepository.logCall], so a recording finished after the call ends
         * can still find the entry it belongs to. Null until the evaluation coroutine logs.
         */
        var logEntryId: Long? = null

        /** Disconnects the call once the recording hits its cap; cancelled if the caller hangs up first. */
        var recordingTimeout: Job? = null

        /**
         * This call's recorder, owned per call rather than by the service.
         *
         * It used to be a single service field, and `onCallRemoved` finalised it for *whichever*
         * call ended — so with a second call in progress, one ending would stop the other call's
         * recording and file its audio under the wrong call-log row.
         */
        var recorder: AutoResponderRecorder? = null
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
                        // Hops back onto serviceScope's main dispatcher: TTS reports completion
                        // on its own engine thread and MediaPlayer on a playback thread, and the
                        // work below reads and writes `callStates`, which every other path
                        // touches from the main thread only.
                        // Hops onto serviceScope's main dispatcher for the same reason
                        // onComplete does: this arrives on the TTS engine's thread.
                        onStarted = {
                            serviceScope.launch { callStates[call]?.greetingStarted = true }
                        },
                        onComplete = {
                            serviceScope.launch {
                                // The greeting is over, so nothing is left for the watchdog to
                                // rescue -- and the recording path below deliberately keeps the
                                // call up for another minute.
                                callStates[call]?.watchdog?.cancel()
                                if (config.recordingEnabled) {
                                    startRecordingAfterGreeting(call)
                                } else {
                                    call.disconnect()
                                }
                            }
                        },
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

    override fun onCreate() {
        super.onCreate()
        // Mute and the audio route hang off the service, not off a Call, so InCallState needs a
        // handle on it to offer them at all.
        InCallState.attachService(this)
    }

    /**
     * Telecom's report of what the audio session is doing, forwarded straight to the screen.
     *
     * This is the only honest source for the mute and speaker buttons: the route also changes
     * without a tap — the auto-responder forces the loudspeaker, and plugging in a headset moves
     * it back — and a button drawn from what was last requested would show the opposite.
     *
     * `CallAudioState` is deprecated in favour of `CallEndpoint`, which arrived in API 34. minSdk
     * here is 26, so this is the callback that fires on the devices this app actually ships to;
     * the replacement would need both paths carried side by side to gain nothing on any of them.
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        InCallState.onAudioStateChanged(audioState)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val state = CallState()
        callStates[call] = state

        val number = call.handleNumber()

        InCallState.attach(call)
        resolveCallerName(number)
        startActivity(
            Intent(this@PassthroughInCallService, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        call.registerCallback(stateCallback)

        // An InCallService is handed outgoing calls too. They still get the in-call UI above --
        // this app owns it -- but they must never reach the rule engine: it would run the user's
        // own blocklist against a number they just dialled, and a quiet-hours rule would end
        // every outgoing call made inside the window. See CallDirectionPolicy.
        if (!CallDirectionPolicy.isIncoming(call)) {
            // Logged, though: with only incoming calls written, the app that had replaced the
            // phone app showed a history missing every call its owner had placed. No decision is
            // recorded because none was made -- see CallLogRepository.logOutgoingCall.
            if (number.isNotBlank()) {
                serviceScope.launch {
                    try {
                        // Before the log write, and outside its failure handling: if the user has
                        // just dialled 112 the callback window matters more than the history row,
                        // and a database error on the row must not cost them the exemption.
                        if (EmergencyNumbers.isWellKnown(number)) {
                            settingsRepository.recordEmergencyCall(currentTimestamp())
                        }
                        callLogRepository.logOutgoingCall(number, currentTimestamp())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // A history row is not worth taking down a live call for.
                        Log.e(TAG, "Could not log an outgoing call", e)
                    }
                }
            }
            return
        }

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
                    val decision =
                        evaluateIncomingCall.evaluate(
                            number = number,
                            inEmergencyCallbackMode = call.isInEmergencyCallbackMode(),
                        )
                    state.logEntryId =
                        callLogRepository.logCall(
                            number = number,
                            timestamp = currentTimestamp(),
                            decision = decision,
                        )
                    if (decision.isBlocked) {
                        state.blockedByRules = true
                        ringer.stop()
                        IncomingCallNotifier.cancel(this@PassthroughInCallService)
                        if (shouldNotifyResult(number)) {
                            IncomingCallNotifier.notifyCallResult(
                                this@PassthroughInCallService,
                                number,
                                R.string.notification_blocked_call_title,
                                decision.reason?.let { BlockReasonStrings.format(this@PassthroughInCallService, it) },
                                // Only the undo: the number is already blocked, so a Block
                                // button here would be a no-op the user cannot tell from a bug.
                                actions = setOf(IncomingCallNotifier.CallResultAction.ALLOWLIST),
                            )
                        }
                        val autoConfig = autoResponderRepository.config.first()
                        if (autoConfig.enabled && autoConfig.validate() is AutoResponderConfig.ValidationResult.Ok) {
                            autoResponderAudio = AutoResponderAudio(this@PassthroughInCallService).also { it.prepare() }
                            call.registerCallback(autoResponderCallback)
                            call.answer(VideoProfile.STATE_AUDIO_ONLY)
                            armGreetingWatchdog(call, state)
                        } else {
                            endBlockedCall(call, state)
                        }
                    } else if (decision is RuleDecision.AllowedAfterRepeatedAttempts) {
                        // Not blocked -- the call keeps ringing normally. The only extra step is
                        // telling the user why an unrecognized number is getting through. Keyed by
                        // number: with a second call in progress this result would otherwise land
                        // on the other caller's screen.
                        InCallState.setRepeatedCallAttempts(number, decision.attempts)
                        if (shouldNotifyResult(number)) {
                            IncomingCallNotifier.notifyCallResult(
                                this@PassthroughInCallService,
                                number,
                                R.string.notification_repeated_caller_title,
                                decision.reason?.let { BlockReasonStrings.format(this@PassthroughInCallService, it) },
                                // This number just got through on persistence alone; Block is
                                // the decision the notification exists to offer. Call back is
                                // there too because a persistent caller is as often someone who
                                // genuinely needs reaching as it is a spammer.
                                actions =
                                    setOf(
                                        IncomingCallNotifier.CallResultAction.BLOCK,
                                        IncomingCallNotifier.CallResultAction.CALL_BACK,
                                    ),
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

    /**
     * Ends a call the rules blocked, and checks that it actually ended.
     *
     * `reject()` alone was the whole of this, and it does nothing to a call that is no longer
     * ringing: something else can answer between the call arriving and the rules deciding — the
     * ringing screen's Answer button is reachable by a cheek or a pocket, a headset button reaches
     * it too — and the app then posted "Blocked call" over a call that was live on the phone. That
     * is the report this exists for: *it answers itself and I only notice when I hear the call in
     * progress*. See [BlockedCallPolicy].
     */
    private fun endBlockedCall(
        call: Call,
        state: CallState,
    ) {
        when (BlockedCallPolicy.terminationFor(call.state.toCallUiPhase())) {
            BlockedCallPolicy.Termination.REJECT -> call.reject(false, null)
            BlockedCallPolicy.Termination.DISCONNECT -> call.disconnect()
            // Telecom is already tearing it down; a second request would only race that.
            BlockedCallPolicy.Termination.ALREADY_ENDING -> return
        }
        state.watchdog =
            serviceScope.launch {
                delay(BlockedCallPolicy.TERMINATION_TIMEOUT_MILLIS)
                // Still in the map means onCallRemoved never came, so the call is still there.
                if (!callStates.containsKey(call)) return@launch
                if (BlockedCallPolicy.terminationFor(call.state.toCallUiPhase()) ==
                    BlockedCallPolicy.Termination.ALREADY_ENDING
                ) {
                    return@launch
                }
                Log.w(TAG, "A blocked call was still up after being ended; disconnecting it")
                call.disconnect()
            }
    }

    /**
     * Hangs up a call answered for the auto-responder if the greeting does not run.
     *
     * The greeting is the only reason that call was answered, and until it reports completion the
     * caller is connected with the loudspeaker on. Text-to-speech has several ways to report
     * nothing at all — an engine that fails to bind, a missing voice, a `speak()` that returns an
     * error — and each of them used to leave the call open until the caller hung up.
     *
     * Two deadlines, because the two failures deserve different patience: a greeting that never
     * makes a sound is a broken engine and ends the call quickly, while one that started and lost
     * its completion callback is given the full length a greeting can legitimately take.
     */
    private fun armGreetingWatchdog(
        call: Call,
        state: CallState,
    ) {
        state.watchdog =
            serviceScope.launch {
                delay(BlockedCallPolicy.GREETING_START_TIMEOUT_MILLIS)
                if (!state.greetingStarted) {
                    Log.w(TAG, "The auto-responder greeting never started; hanging up")
                    call.disconnect()
                    return@launch
                }
                delay(BlockedCallPolicy.GREETING_MAX_MILLIS - BlockedCallPolicy.GREETING_START_TIMEOUT_MILLIS)
                Log.w(TAG, "The auto-responder greeting never finished; hanging up")
                call.disconnect()
            }
    }

    /**
     * Puts a name on the in-call screen: the contact's, or failing that the label the user gave
     * this number in their own rules.
     *
     * Off the call-setup path on purpose. Both lookups touch a content provider or the database,
     * and the screen has to be up before either finishes -- a call that shows nothing for 200 ms
     * and then gains a name is fine, a call whose UI waits on a 5,000-contact provider query is
     * not. The number is already on screen from [InCallState.attach]; this only replaces it.
     *
     * The rule label is a genuine fallback rather than a decoration: a number worth adding to a
     * blocklist by hand is usually one the user has already identified ("insurance spam"), and
     * that is more use on a ringing screen than the digits are.
     */
    private fun resolveCallerName(number: String) {
        if (number.isBlank()) return
        serviceScope.launch {
            try {
                val name =
                    ContactNameLookup.displayNameFor(this@PassthroughInCallService, number)
                        ?: contactNameFromAddressBook(number)
                        ?: ruleLabelFor(number)
                if (name != null) InCallState.setDisplayName(number, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The number stays on screen. A missing name is cosmetic; a crash here is not.
                Log.e(TAG, "Could not resolve a name for the caller", e)
            }
        }
    }

    /**
     * The address book's own answer, for the numbers `PhoneLookup` cannot match.
     *
     * `PhoneLookup` matches through the provider's normalized `data4` column, which is derived
     * from the device's default region: a contact saved nationally does not match a call in E.164
     * unless the phone's region happens to agree, and with no SIM `data4` is null and *every*
     * contact reads as a stranger. This is the same probe [ContactsGateway] gives the call log and
     * the block lists — see [org.carlospinan.bloqueador.app.contacts.contactDisplayName], which
     * derives the country code from the number itself and so has no region to be wrong about.
     *
     * It is the fallback rather than the first question only because it is the slower one: the
     * gateway caches for five minutes, but the first call after a change pays for a full scan.
     */
    private suspend fun contactNameFromAddressBook(number: String): String? {
        if (!contactsGateway.hasPermission()) return null
        // contactDisplayName hands back the number itself when nobody claims it, which is not a
        // name and must not be shown as one — the screen already falls back to the number.
        return contactDisplayName(number, contactsGateway.contactNames()).takeIf { it != number }
    }

    /**
     * The label on this number's own block or allowlist entry, if it has one.
     *
     * Matched with [PhoneNumberParser.sameNumber] rather than string equality, for the reason the
     * whole app matches numbers that way: the rule may have been saved nationally and the call
     * arrives in E.164, or the other way round.
     */
    private suspend fun ruleLabelFor(number: String): String? {
        val blocked =
            ruleRepository.blockedNumberEntries().firstOrNull { PhoneNumberParser.sameNumber(it.number, number) }?.label
        if (!blocked.isNullOrBlank()) return blocked
        val allowed =
            ruleRepository.allowlistedNumberEntries().firstOrNull { PhoneNumberParser.sameNumber(it.number, number) }?.label
        return allowed?.takeIf { it.isNotBlank() }
    }

    /**
     * Starts recording the caller once the greeting has played, and arms the disconnect.
     *
     * When the microphone cannot be acquired -- no grant, or an OEM that reserves it for the
     * telephony stack -- this falls through to the same immediate disconnect the non-recording
     * path uses. A blocked call must always end; it must never be left open because recording
     * failed. See [AutoResponderRecorder] for why failure is expected on some devices.
     */
    private fun startRecordingAfterGreeting(call: Call) {
        val state = callStates[call]
        val entryId = state?.logEntryId
        if (entryId == null) {
            // Evaluation logged nothing, so there is no row to attach audio to. Recording
            // anyway would orphan the file the moment the call ended.
            call.disconnect()
            return
        }

        val recorder = AutoResponderRecorder(this).also { state.recorder = it }
        if (!recorder.start(entryId)) {
            state.recorder = null
            call.disconnect()
            return
        }

        state.recordingTimeout =
            serviceScope.launch {
                // MediaRecorder's own setMaxDuration stops the capture but leaves the call up;
                // this is what actually hangs up on a caller who never does.
                delay(AutoResponderRecorder.MAX_DURATION_MILLIS.toLong())
                call.disconnect()
            }
    }

    /**
     * Finalises a recording and points its call-log row at the file.
     *
     * Runs on call removal rather than when the recorder hits its cap: the caller can hang up
     * at any point, and either way the file is only complete once `stop()` has returned.
     */
    private fun finishRecording(state: CallState?) {
        val path = state?.recorder?.stop()
        state?.recorder = null
        val entryId = state?.logEntryId
        if (path == null || entryId == null) return
        serviceScope.launch { callLogRepository.attachRecording(entryId, path) }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        // First, before any of the bookkeeping below. This is what takes the call screen down, and
        // it used to be the last line of the method: anything above it that threw -- a recorder
        // that would not stop, a notification the platform rejected -- left the user on a call
        // screen for a call that no longer existed, with Back deliberately declining to leave it.
        InCallState.detach(call)

        val state = callStates.remove(call)
        state?.evaluation?.cancel()
        state?.watchdog?.cancel()
        state?.recordingTimeout?.cancel()
        finishRecording(state)
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

        if (state?.blockedByRules != true && call.details?.disconnectCause?.code == DisconnectCause.MISSED) {
            // Read off the Call before leaving the callback: onCallRemoved is the last moment
            // Telecom guarantees these are readable, and the coroutine outlives it.
            val number = call.handleNumber()
            serviceScope.launch {
                if (shouldNotifyResult(number)) {
                    IncomingCallNotifier.notifyCallResult(
                        this@PassthroughInCallService,
                        number,
                        R.string.notification_missed_call_title,
                        reason = null,
                        // The call came through, so Allow would change nothing. Call back is the
                        // action a missed call is actually about; without it the user had to
                        // leave for another app to return a call this app told them about.
                        actions =
                            setOf(
                                IncomingCallNotifier.CallResultAction.CALL_BACK,
                                IncomingCallNotifier.CallResultAction.BLOCK,
                            ),
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // The Call objects handed to this service are only usable through its binding to Telecom,
        // and InCallState is an object that outlives the service. Leaving them behind left a call
        // screen the user could not leave, whose hang-up button reached a dead adapter and did
        // nothing — and left a dead call in the stack for the next call to promote back on to the
        // screen when it ended.
        InCallState.clear()
        ringer.stop()
        autoResponderAudio?.release()
        autoResponderAudio = null
        // cancel(), not stop(): the service is going away, so nothing is left to write the path
        // into the call log. Keeping the file would leave audio on disk that no row references
        // and no UI can reach or delete.
        callStates.values.forEach { it.recorder?.cancel() }
        callStates.clear()
        serviceScope.cancel()
    }

    /**
     * The platform's own answer to "did this device just place an emergency call", read off the
     * call rather than from telephony state -- `Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE`
     * needs no permission, and `TelephonyManager` would need `READ_PHONE_STATE`, which this app
     * deliberately does not declare.
     */
    private fun Call.isInEmergencyCallbackMode(): Boolean = details?.hasProperty(Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE) == true

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
