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
import cortaspam.shared.generated.resources.autoresponder_pick_audio
import cortaspam.shared.generated.resources.autoresponder_recording
import cortaspam.shared.generated.resources.autoresponder_recording_desc
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
                                }
                            }
                            Switch(
                                checked = state.config.recordingEnabled,
                                onCheckedChange = onSetRecordingEnabled,
                            )
                        }
                    }
                }
            }
        }
    }
}
