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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.action_answer
import cortaspam.shared.generated.resources.action_decline
import cortaspam.shared.generated.resources.action_hang_up
import cortaspam.shared.generated.resources.call_repeated_caller_hint
import cortaspam.shared.generated.resources.call_status_active
import cortaspam.shared.generated.resources.call_status_dialing
import cortaspam.shared.generated.resources.call_status_ringing
import cortaspam.shared.generated.resources.call_unknown_number
import cortaspam.shared.generated.resources.ic_brand_app
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// M1: answer/decline/hang up only; no custom dialer chrome yet.
enum class CallUiPhase { RINGING, DIALING, ACTIVE, OTHER }

@Composable
fun CallScreen(
    number: String,
    phase: CallUiPhase,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
    repeatedCallAttempts: Int? = null,
) {
    MaterialTheme {
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
                                CallUiPhase.OTHER -> ""
                            },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = number.ifBlank { stringResource(Res.string.call_unknown_number) },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    if (repeatedCallAttempts != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.call_repeated_caller_hint, repeatedCallAttempts),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_brand_app),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                    )
                }

                when (phase) {
                    CallUiPhase.RINGING ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Button(
                                onClick = onDecline,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) { Text(stringResource(Res.string.action_decline)) }
                            Button(onClick = onAnswer) { Text(stringResource(Res.string.action_answer)) }
                        }

                    CallUiPhase.ACTIVE, CallUiPhase.DIALING ->
                        Button(
                            onClick = onHangUp,
                            modifier = Modifier.padding(bottom = 32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text(stringResource(Res.string.action_hang_up)) }

                    CallUiPhase.OTHER -> Unit
                }
            }
        }
    }
}
