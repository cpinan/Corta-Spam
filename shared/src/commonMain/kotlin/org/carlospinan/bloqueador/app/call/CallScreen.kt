package org.carlospinan.bloqueador.app.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Android-only in-call UI. M1 scope: answer/decline/hang up, no custom dialer chrome yet. */
enum class CallUiPhase { RINGING, DIALING, ACTIVE, OTHER }

@Composable
fun CallScreen(
    number: String,
    phase: CallUiPhase,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = when (phase) {
                            CallUiPhase.RINGING -> "Incoming call"
                            CallUiPhase.DIALING -> "Calling…"
                            CallUiPhase.ACTIVE -> "In call"
                            CallUiPhase.OTHER -> ""
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = number.ifBlank { "Unknown number" },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }

                when (phase) {
                    CallUiPhase.RINGING -> Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Button(
                            onClick = onDecline,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("Decline") }
                        Button(onClick = onAnswer) { Text("Answer") }
                    }

                    CallUiPhase.ACTIVE, CallUiPhase.DIALING -> Button(
                        onClick = onHangUp,
                        modifier = Modifier.padding(bottom = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Hang up") }

                    CallUiPhase.OTHER -> Unit
                }
            }
        }
    }
}
