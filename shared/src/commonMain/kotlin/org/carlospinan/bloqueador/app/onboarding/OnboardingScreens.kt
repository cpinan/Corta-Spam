package org.carlospinan.bloqueador.app.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Top-level Android default-dialer onboarding flow. Renders [content] once the
 * role is held (granted this session or already held); otherwise walks the
 * user through the explainer -> OS request -> result states.
 *
 * "Not now" is a UI-only skip -- it does not change [DialerOnboardingViewModel]'s
 * telecom state, it just lets the user past onboarding without granting the role.
 */
@Composable
fun DialerOnboardingScreen(
    viewModel: DialerOnboardingViewModel,
    onRequestRole: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var skipped by remember { mutableStateOf(false) }

    val isPastOnboarding = skipped ||
        state == DialerOnboardingState.GRANTED ||
        state == DialerOnboardingState.ALREADY_DEFAULT

    if (isPastOnboarding) {
        content()
        return
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (state) {
                DialerOnboardingState.REQUESTING -> RequestingIndicatorScreen()

                DialerOnboardingState.DENIED -> DeniedScreen(
                    onRetry = {
                        viewModel.onRequestStarted()
                        onRequestRole()
                    },
                    onContinueWithoutDefault = { skipped = true },
                )

                DialerOnboardingState.NOT_REQUESTED -> PermissionExplainerScreen(
                    onContinue = {
                        viewModel.onRequestStarted()
                        onRequestRole()
                    },
                    onNotNow = { skipped = true },
                )

                DialerOnboardingState.GRANTED, DialerOnboardingState.ALREADY_DEFAULT -> Unit
            }
        }
    }
}

@Composable
fun PermissionExplainerScreen(
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp, bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconCircle()
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "We need to become your default phone app",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This is the only way Android lets an app see who's calling before " +
                    "you answer, and speak a greeting to callers you haven't approved yet.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        ReasonRow(
            title = "Screen calls silently.",
            body = "We check the number against your rules before your phone ever rings.",
        )
        ReasonRow(
            title = "Power the optional auto-responder.",
            body = "Only used if you turn it on yourself in Settings.",
        )
        ReasonRow(
            title = null,
            body = "You can switch back to your phone's original dialer at any time in Android Settings.",
        )

        Spacer(modifier = Modifier.height(8.dp))

        NeverDoBox()

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) {
            Text("Not now")
        }
    }
}

@Composable
private fun IconCircle() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("☎", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ReasonRow(title: String?, body: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 10.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Column {
            if (title != null) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NeverDoBox() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "WHAT WE WILL NEVER DO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                "Record any call unless you separately turn that on, per call type.",
                "Send your call data, contacts, or numbers off this device.",
                "Show ads or share information with third parties.",
            ).forEach { line ->
                Text(
                    text = "✓  $line",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "This app is open source — view the code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun RequestingIndicatorScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Waiting for the system dialog…",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DeniedScreen(
    onRetry: () -> Unit,
    onContinueWithoutDefault: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Not set as your default phone app",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Without this, call screening and the auto-responder can't run. " +
                "You can turn it on later from Settings.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Try again")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onContinueWithoutDefault, modifier = Modifier.fillMaxWidth()) {
            Text("Continue without it")
        }
    }
}
