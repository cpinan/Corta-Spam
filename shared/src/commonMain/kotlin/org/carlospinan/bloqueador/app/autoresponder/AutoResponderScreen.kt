package org.carlospinan.bloqueador.app.autoresponder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.autoresponder_audio_custom
import cortaspam.shared.generated.resources.autoresponder_audio_tts
import cortaspam.shared.generated.resources.autoresponder_clear_audio
import cortaspam.shared.generated.resources.autoresponder_consent_hint
import cortaspam.shared.generated.resources.autoresponder_enabled
import cortaspam.shared.generated.resources.autoresponder_enabled_desc
import cortaspam.shared.generated.resources.autoresponder_error_consent
import cortaspam.shared.generated.resources.autoresponder_error_empty
import cortaspam.shared.generated.resources.autoresponder_error_too_long
import cortaspam.shared.generated.resources.autoresponder_experimental_badge
import cortaspam.shared.generated.resources.autoresponder_grant_mic
import cortaspam.shared.generated.resources.autoresponder_how_blocked_only
import cortaspam.shared.generated.resources.autoresponder_how_it_works
import cortaspam.shared.generated.resources.autoresponder_how_recording_mic
import cortaspam.shared.generated.resources.autoresponder_how_speaker
import cortaspam.shared.generated.resources.autoresponder_how_storage
import cortaspam.shared.generated.resources.autoresponder_mic_permission_required
import cortaspam.shared.generated.resources.autoresponder_pick_audio
import cortaspam.shared.generated.resources.autoresponder_recording
import cortaspam.shared.generated.resources.autoresponder_recording_desc
import cortaspam.shared.generated.resources.autoresponder_recording_inactive_greeting
import cortaspam.shared.generated.resources.autoresponder_recording_inactive_responder_off
import cortaspam.shared.generated.resources.autoresponder_script_hint
import cortaspam.shared.generated.resources.autoresponder_test
import cortaspam.shared.generated.resources.autoresponder_title
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.jetbrains.compose.resources.stringResource

@Composable
fun AutoResponderScreen(
    state: AutoResponderUiState,
    onSetEnabled: (Boolean) -> Unit,
    onSetScript: (String) -> Unit,
    onSetRecordingEnabled: (Boolean) -> Unit,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onTest: () -> Unit = {},
    onBack: () -> Unit,
    // Defaults to true so the warning is invisible on platforms with no microphone permission
    // to grant (iOS builds this screen too) rather than showing a fix button that does nothing.
    micPermissionGranted: Boolean = true,
    onRequestMicPermission: () -> Unit = {},
) {
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(Res.string.autoresponder_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(Res.string.autoresponder_experimental_badge),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.autoresponder_enabled),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = stringResource(Res.string.autoresponder_enabled_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = state.config.enabled,
                                onCheckedChange = onSetEnabled,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.config.script,
                        onValueChange = onSetScript,
                        label = { Text(stringResource(Res.string.autoresponder_script_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8,
                        isError = state.validationError != null,
                        supportingText = {
                            if (state.validationError != null) {
                                Text(
                                    text =
                                        when (state.validationError) {
                                            AutoResponderConfig.ErrorCode.EMPTY_SCRIPT ->
                                                stringResource(Res.string.autoresponder_error_empty)
                                            AutoResponderConfig.ErrorCode.SCRIPT_TOO_LONG ->
                                                stringResource(Res.string.autoresponder_error_too_long)
                                            AutoResponderConfig.ErrorCode.MISSING_CONSENT ->
                                                stringResource(Res.string.autoresponder_error_consent)
                                        },
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.autoresponder_audio_tts),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = state.config.usesTts,
                                    onClick = onClearAudio,
                                )
                                Text(stringResource(Res.string.autoresponder_audio_tts))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = !state.config.usesTts,
                                    onClick = onPickAudio,
                                )
                                Text(stringResource(Res.string.autoresponder_audio_custom))
                            }
                            if (!state.config.usesTts) {
                                Text(
                                    text = state.config.audioUri,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = onClearAudio) {
                                    Text(stringResource(Res.string.autoresponder_clear_audio))
                                }
                            } else {
                                TextButton(onClick = onPickAudio) {
                                    Text(stringResource(Res.string.autoresponder_pick_audio))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onTest,
                        enabled = state.config.script.isNotBlank() || state.config.audioUri.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.autoresponder_test))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.autoresponder_recording),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = stringResource(Res.string.autoresponder_recording_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (state.config.recordingEnabled) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(Res.string.autoresponder_consent_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    // Everything that stops the switch from producing a recording,
                                    // said out loud. A switch left on while the auto-responder was
                                    // off recorded nothing and explained nothing, which reads as a
                                    // feature that does not work -- and is how it was reported.
                                    // Only shown once recording is actually on: warning about a
                                    // microphone the user has not asked to use would be the same
                                    // permanent nag the contacts button used to be.
                                    when (recordingReadiness(state.config, micPermissionGranted)) {
                                        RecordingReadiness.AutoResponderOff ->
                                            RecordingBlockedNote(
                                                stringResource(Res.string.autoresponder_recording_inactive_responder_off),
                                            )

                                        RecordingReadiness.GreetingInvalid ->
                                            RecordingBlockedNote(
                                                stringResource(Res.string.autoresponder_recording_inactive_greeting),
                                            )

                                        RecordingReadiness.MicPermissionMissing -> {
                                            RecordingBlockedNote(
                                                stringResource(Res.string.autoresponder_mic_permission_required),
                                            )
                                            TextButton(onClick = onRequestMicPermission) {
                                                Text(stringResource(Res.string.autoresponder_grant_mic))
                                            }
                                        }

                                        RecordingReadiness.Ready, RecordingReadiness.Off -> Unit
                                    }
                                }
                            }
                            Switch(
                                checked = state.config.recordingEnabled,
                                onCheckedChange = onSetRecordingEnabled,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HowItWorksCard()
                }
            }
        }
    }
}

/**
 * What the feature does, and — more usefully — what it cannot do.
 *
 * Both halves of this screen depend on acoustic coupling through the handset, which is the only
 * route Android leaves a third-party app: the real call-audio streams need `CAPTURE_AUDIO_OUTPUT`,
 * a signature|privileged permission, and holding the dialer role does not grant it. That makes
 * the greeting and the recording best-effort *by construction*, and several manufacturers refuse
 * the microphone outright during a call. Saying so here is the difference between a limitation
 * and a bug report: without it, a phone that records nothing looks like an app that is broken.
 */
@Composable
private fun HowItWorksCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.autoresponder_how_it_works),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                Res.string.autoresponder_how_blocked_only,
                Res.string.autoresponder_how_speaker,
                Res.string.autoresponder_how_recording_mic,
                Res.string.autoresponder_how_storage,
            ).forEach { line ->
                Text(
                    text = stringResource(line),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/** One reason the recording switch will not do anything yet. */
@Composable
private fun RecordingBlockedNote(text: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}
