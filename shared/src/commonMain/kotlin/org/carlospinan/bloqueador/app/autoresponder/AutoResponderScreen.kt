package org.carlospinan.bloqueador.app.autoresponder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.autoresponder_audio_custom
import bloqueallamadas.shared.generated.resources.autoresponder_audio_tts
import bloqueallamadas.shared.generated.resources.autoresponder_clear_audio
import bloqueallamadas.shared.generated.resources.autoresponder_consent_hint
import bloqueallamadas.shared.generated.resources.autoresponder_enabled
import bloqueallamadas.shared.generated.resources.autoresponder_enabled_desc
import bloqueallamadas.shared.generated.resources.autoresponder_error_consent
import bloqueallamadas.shared.generated.resources.autoresponder_error_empty
import bloqueallamadas.shared.generated.resources.autoresponder_error_too_long
import bloqueallamadas.shared.generated.resources.autoresponder_pick_audio
import bloqueallamadas.shared.generated.resources.autoresponder_recording
import bloqueallamadas.shared.generated.resources.autoresponder_recording_desc
import bloqueallamadas.shared.generated.resources.autoresponder_script_hint
import bloqueallamadas.shared.generated.resources.autoresponder_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun AutoResponderScreen(
    config: AutoResponderConfig,
    validationError: AutoResponderConfig.ErrorCode?,
    onSetEnabled: (Boolean) -> Unit,
    onSetScript: (String) -> Unit,
    onSetRecordingEnabled: (Boolean) -> Unit,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onBack: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(Res.string.autoresponder_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                            checked = config.enabled,
                            onCheckedChange = onSetEnabled,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = config.script,
                    onValueChange = onSetScript,
                    label = { Text(stringResource(Res.string.autoresponder_script_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    isError = validationError != null,
                    supportingText = {
                        if (validationError != null) {
                            Text(
                                text =
                                    when (validationError) {
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
                                selected = config.usesTts,
                                onClick = onClearAudio,
                            )
                            Text(stringResource(Res.string.autoresponder_audio_tts))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !config.usesTts,
                                onClick = onPickAudio,
                            )
                            Text(stringResource(Res.string.autoresponder_audio_custom))
                        }
                        if (!config.usesTts) {
                            Text(
                                text = config.audioUri,
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
                            if (config.recordingEnabled) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.autoresponder_consent_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Switch(
                            checked = config.recordingEnabled,
                            onCheckedChange = onSetRecordingEnabled,
                        )
                    }
                }
            }
        }
    }
}
