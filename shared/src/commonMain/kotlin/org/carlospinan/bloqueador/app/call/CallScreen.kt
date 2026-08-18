package org.carlospinan.bloqueador.app.call

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.action_answer
import cortaspam.shared.generated.resources.action_close
import cortaspam.shared.generated.resources.action_decline
import cortaspam.shared.generated.resources.action_hang_up
import cortaspam.shared.generated.resources.action_resume
import cortaspam.shared.generated.resources.call_keypad_hide
import cortaspam.shared.generated.resources.call_keypad_show
import cortaspam.shared.generated.resources.call_other_call_active
import cortaspam.shared.generated.resources.call_repeated_caller_hint
import cortaspam.shared.generated.resources.call_status_active
import cortaspam.shared.generated.resources.call_status_dialing
import cortaspam.shared.generated.resources.call_status_disconnecting
import cortaspam.shared.generated.resources.call_status_holding
import cortaspam.shared.generated.resources.call_status_other
import cortaspam.shared.generated.resources.call_status_ringing
import cortaspam.shared.generated.resources.call_unknown_number
import cortaspam.shared.generated.resources.ic_brand_app
import org.carlospinan.bloqueador.app.keypad.DialPad
import org.carlospinan.bloqueador.app.theme.CortaSpamTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * What the call is doing, as far as this screen is concerned.
 *
 * [HOLDING], [DISCONNECTING] and [OTHER] exist because they used to be one value that rendered no
 * buttons at all. A held call, a call being torn down, a dual-SIM call waiting for an account to
 * be picked and Telecom's simulated ringing all landed on it, and the user got a screen with a
 * name on it and no way to end the call.
 */
enum class CallUiPhase {
    RINGING,
    DIALING,
    ACTIVE,

    /** Parked, usually because the user answered a second call. Resumable. */
    HOLDING,

    /** Already ending. Nothing to offer: the button it would show is the thing happening. */
    DISCONNECTING,

    /**
     * Everything Telecom can report that this screen has no specific handling for. Still gets a
     * hang-up button — whatever the state turns out to be, ending the call is the escape hatch,
     * and having none is what made this a bug.
     */
    OTHER,
}

/**
 * The in-call UI. This app holds `ROLE_DIALER`, so this screen *is* the phone app's call screen —
 * whatever it cannot do, the user cannot do while on a call.
 *
 * [dtmfDigits] and [onDtmf] are that principle applied to automated menus: without a keypad
 * during the call there is no way to press 1 for anything, so every bank, airline and doctor's
 * surgery becomes unreachable. The digits already sent are shown because DTMF is fire-and-forget —
 * nothing echoes them back, and a user who has lost count of a card number has no way to check.
 */
@Composable
fun CallScreen(
    number: String,
    phase: CallUiPhase,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
    /** Takes a held call off hold. Only reachable from [CallUiPhase.HOLDING]. */
    onResume: () -> Unit = {},
    repeatedCallAttempts: Int? = null,
    /** Contact name, or the user's own label for this number. Null when it is neither. */
    displayName: String? = null,
    /** The tones already sent on this call, in order. */
    dtmfDigits: String = "",
    onDtmf: (Char) -> Unit = {},
    /**
     * Calls Telecom is holding besides this one. Said out loud rather than left implicit: showing
     * one of two calls as though it were the only one is how a user hangs up on the wrong person.
     */
    otherCallCount: Int = 0,
    /**
     * Leaves the call screen without touching the call. Only reachable from
     * [CallUiPhase.DISCONNECTING], where there is no call left to act on and the screen would
     * otherwise be a dead end — Back deliberately backgrounds the task rather than leaving.
     */
    onDismiss: () -> Unit = {},
) {
    var keypadRequested by rememberSaveable { mutableStateOf(false) }
    // Tones only mean anything on a connected call: Telecom drops playDtmfTone on a call that is
    // still ringing or dialling, so offering the pad there would be a button that does nothing.
    val keypadVisible = keypadRequested && phase == CallUiPhase.ACTIVE

    CortaSpamTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                            when (phase) {
                                CallUiPhase.RINGING -> stringResource(Res.string.call_status_ringing)
                                CallUiPhase.DIALING -> stringResource(Res.string.call_status_dialing)
                                CallUiPhase.ACTIVE -> stringResource(Res.string.call_status_active)
                                CallUiPhase.HOLDING -> stringResource(Res.string.call_status_holding)
                                CallUiPhase.DISCONNECTING -> stringResource(Res.string.call_status_disconnecting)
                                CallUiPhase.OTHER -> stringResource(Res.string.call_status_other)
                            },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // The name is what identifies the caller; the number stays underneath rather
                    // than being replaced, because "who is calling" and "which of their numbers"
                    // are different questions and a call screen is where both get asked.
                    Text(
                        text = displayName ?: number.ifBlank { stringResource(Res.string.call_unknown_number) },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    if (displayName != null && number.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (repeatedCallAttempts != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.call_repeated_caller_hint, repeatedCallAttempts),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (otherCallCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.call_other_call_active),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (dtmfDigits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dtmfDigits,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (keypadVisible) {
                        // Scrolls because it has to: in landscape the pad is taller than the
                        // space between the caller's name and the hang-up button, and a pad that
                        // pushed the hang-up button off screen would be worse than no pad.
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            DialPad(onKey = onDtmf)
                        }
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.ic_brand_app),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (phase == CallUiPhase.ACTIVE) {
                        TextButton(onClick = { keypadRequested = !keypadRequested }) {
                            Text(
                                stringResource(
                                    if (keypadVisible) Res.string.call_keypad_hide else Res.string.call_keypad_show,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    when (phase) {
                        CallUiPhase.RINGING ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                HangUpButton(onClick = onDecline, label = stringResource(Res.string.action_decline))
                                Button(onClick = onAnswer) { Text(stringResource(Res.string.action_answer)) }
                            }

                        CallUiPhase.HOLDING ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                HangUpButton(onClick = onHangUp, label = stringResource(Res.string.action_hang_up))
                                Button(onClick = onResume) { Text(stringResource(Res.string.action_resume)) }
                            }

                        // OTHER is in here on purpose. Whatever Telecom is reporting, the user
                        // must be able to end the call; a screen with no button was the bug.
                        CallUiPhase.ACTIVE, CallUiPhase.DIALING, CallUiPhase.OTHER ->
                            HangUpButton(onClick = onHangUp, label = stringResource(Res.string.action_hang_up))

                        // The call is already ending, so a hang-up button here would be a control
                        // for something the platform is doing anyway — but the screen still needs
                        // a door. Telecom can sit on a disconnected call for seconds before it
                        // removes it, and Back backgrounds the task rather than leaving, so this
                        // state used to be a screen with nothing on it and no way off it. Not
                        // offered while another call is live: leaving then would take away the
                        // only UI for the call still on the line.
                        CallUiPhase.DISCONNECTING ->
                            if (otherCallCount == 0) {
                                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
                            }
                    }
                }
            }
        }
    }
}

/**
 * The destructive action, in both places it appears.
 *
 * `contentColor` is passed explicitly: `ButtonDefaults.buttonColors(containerColor = …)` leaves
 * the content at `onPrimary`, which was white-on-red by luck in the light scheme and a dark green
 * on light coral in the dark one.
 */
@Composable
private fun HangUpButton(
    onClick: () -> Unit,
    label: String,
) {
    Button(
        onClick = onClick,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
    ) { Text(label) }
}
