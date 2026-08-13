package org.carlospinan.bloqueador.app.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.action_continue
import cortaspam.shared.generated.resources.action_continue_without_default
import cortaspam.shared.generated.resources.action_not_now
import cortaspam.shared.generated.resources.action_try_again
import cortaspam.shared.generated.resources.ic_brand_app
import cortaspam.shared.generated.resources.onboarding_denied_body
import cortaspam.shared.generated.resources.onboarding_denied_title
import cortaspam.shared.generated.resources.onboarding_never_do_ads
import cortaspam.shared.generated.resources.onboarding_never_do_header
import cortaspam.shared.generated.resources.onboarding_never_do_record
import cortaspam.shared.generated.resources.onboarding_never_do_send_data
import cortaspam.shared.generated.resources.onboarding_open_source
import cortaspam.shared.generated.resources.onboarding_reason_autoresponder_body
import cortaspam.shared.generated.resources.onboarding_reason_autoresponder_title
import cortaspam.shared.generated.resources.onboarding_reason_revert_body
import cortaspam.shared.generated.resources.onboarding_reason_screen_body
import cortaspam.shared.generated.resources.onboarding_reason_screen_title
import cortaspam.shared.generated.resources.onboarding_requesting_indicator
import cortaspam.shared.generated.resources.onboarding_subtitle
import cortaspam.shared.generated.resources.onboarding_title
import org.carlospinan.bloqueador.app.adaptive.ScrollableScreenColumn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
    state: DialerOnboardingUiState,
    onIntent: (DialerOnboardingIntent) -> Unit,
    onRequestRole: () -> Unit,
    content: @Composable () -> Unit,
) {
    var skipped by remember { mutableStateOf(false) }

    val isPastOnboarding =
        skipped ||
            state.dialerState == DialerOnboardingState.GRANTED ||
            state.dialerState == DialerOnboardingState.ALREADY_DEFAULT

    if (isPastOnboarding) {
        content()
        return
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (state.dialerState) {
                DialerOnboardingState.REQUESTING -> RequestingIndicatorScreen()

                DialerOnboardingState.DENIED ->
                    DeniedScreen(
                        onRetry = {
                            onIntent(DialerOnboardingIntent.RequestStarted)
                            onRequestRole()
                        },
                        onContinueWithoutDefault = { skipped = true },
                    )

                DialerOnboardingState.NOT_REQUESTED ->
                    PermissionExplainerScreen(
                        onContinue = {
                            onIntent(DialerOnboardingIntent.RequestStarted)
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
    // Scrollable, not just padded: at font scale 1.8 on a 1344x2992 screen this column's own
    // Continue and Not now buttons fell off the bottom and could not be reached, which made
    // first-run onboarding impossible to complete.
    ScrollableScreenColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconCircle()
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(Res.string.onboarding_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        ReasonRow(
            title = stringResource(Res.string.onboarding_reason_screen_title),
            body = stringResource(Res.string.onboarding_reason_screen_body),
        )
        ReasonRow(
            title = stringResource(Res.string.onboarding_reason_autoresponder_title),
            body = stringResource(Res.string.onboarding_reason_autoresponder_body),
        )
        ReasonRow(
            title = null,
            body = stringResource(Res.string.onboarding_reason_revert_body),
        )

        Spacer(modifier = Modifier.height(8.dp))

        NeverDoBox()

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_continue))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_not_now))
        }
    }
}

@Composable
private fun IconCircle() {
    Image(
        painter = painterResource(Res.drawable.ic_brand_app),
        contentDescription = null,
        modifier = Modifier.size(56.dp),
    )
}

@Composable
private fun ReasonRow(
    title: String?,
    body: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier =
                Modifier
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
                text = stringResource(Res.string.onboarding_never_do_header),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                stringResource(Res.string.onboarding_never_do_record),
                stringResource(Res.string.onboarding_never_do_send_data),
                stringResource(Res.string.onboarding_never_do_ads),
            ).forEach { line ->
                Text(
                    text = "✓  $line",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.onboarding_open_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun RequestingIndicatorScreen() {
    ScrollableScreenColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.onboarding_requesting_indicator),
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
    ScrollableScreenColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.onboarding_denied_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.onboarding_denied_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_try_again))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onContinueWithoutDefault, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_continue_without_default))
        }
    }
}
