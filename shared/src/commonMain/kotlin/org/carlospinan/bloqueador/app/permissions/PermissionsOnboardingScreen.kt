package org.carlospinan.bloqueador.app.permissions

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.action_open_settings
import cortaspam.shared.generated.resources.permissions_allow
import cortaspam.shared.generated.resources.permissions_asked_later
import cortaspam.shared.generated.resources.permissions_change_later
import cortaspam.shared.generated.resources.permissions_contacts_body
import cortaspam.shared.generated.resources.permissions_contacts_title
import cortaspam.shared.generated.resources.permissions_continue
import cortaspam.shared.generated.resources.permissions_continue_anyway
import cortaspam.shared.generated.resources.permissions_fullscreen_body
import cortaspam.shared.generated.resources.permissions_fullscreen_title
import cortaspam.shared.generated.resources.permissions_granted
import cortaspam.shared.generated.resources.permissions_microphone_body
import cortaspam.shared.generated.resources.permissions_microphone_title
import cortaspam.shared.generated.resources.permissions_notifications_body
import cortaspam.shared.generated.resources.permissions_notifications_title
import cortaspam.shared.generated.resources.permissions_phone_body
import cortaspam.shared.generated.resources.permissions_phone_title
import cortaspam.shared.generated.resources.permissions_subtitle
import cortaspam.shared.generated.resources.permissions_title
import org.carlospinan.bloqueador.app.theme.CortaSpamTheme
import org.jetbrains.compose.resources.stringResource

/**
 * The permission checklist step of onboarding, shown once, after the default-dialer explainer.
 *
 * It exists because the app used to fire the POST_NOTIFICATIONS system dialog from
 * `MainActivity.onCreate`, on top of a welcome screen that never mentioned permissions -- a
 * first-run experience indistinguishable from an app grabbing whatever it can. Every row here
 * names the one thing the permission is used for before its system dialog can appear.
 *
 * Nothing on this screen is mandatory. The continue button is always enabled; it only changes
 * label to make clear that something was left ungranted.
 */
@Composable
fun PermissionsOnboardingScreen(
    items: List<PermissionUiItem>,
    onRequest: (AppPermission) -> Unit,
    onContinue: () -> Unit,
) {
    val allGranted = allRequestablePermissionsGranted(items)

    CortaSpamTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 28.dp, bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.permissions_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.permissions_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                items.forEach { item ->
                    PermissionRow(item = item, onRequest = { onRequest(item.permission) })
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = stringResource(Res.string.permissions_change_later),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(
                            if (allGranted) Res.string.permissions_continue else Res.string.permissions_continue_anyway,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    item: PermissionUiItem,
    onRequest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // A glyph rather than a drawable: there is no notification/microphone icon in the
            // icon set, and borrowing an unrelated one ("scheduled blocking" for notifications)
            // would say something false about what the permission does.
            Text(
                text = if (item.granted) "✓" else "•",
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (item.granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.permission.titleRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(item.permission.bodyRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                when {
                    item.granted ->
                        Text(
                            text = stringResource(Res.string.permissions_granted),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )

                    item.requestable ->
                        TextButton(onClick = onRequest) {
                            Text(
                                stringResource(
                                    if (item.opensSettings) Res.string.action_open_settings else Res.string.permissions_allow,
                                ),
                            )
                        }

                    else ->
                        Text(
                            text = stringResource(Res.string.permissions_asked_later),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
        }
    }
}

private fun AppPermission.titleRes() =
    when (this) {
        AppPermission.NOTIFICATIONS -> Res.string.permissions_notifications_title
        AppPermission.FULL_SCREEN_INTENT -> Res.string.permissions_fullscreen_title
        AppPermission.CONTACTS -> Res.string.permissions_contacts_title
        AppPermission.PHONE -> Res.string.permissions_phone_title
        AppPermission.MICROPHONE -> Res.string.permissions_microphone_title
    }

private fun AppPermission.bodyRes() =
    when (this) {
        AppPermission.NOTIFICATIONS -> Res.string.permissions_notifications_body
        AppPermission.FULL_SCREEN_INTENT -> Res.string.permissions_fullscreen_body
        AppPermission.CONTACTS -> Res.string.permissions_contacts_body
        AppPermission.PHONE -> Res.string.permissions_phone_body
        AppPermission.MICROPHONE -> Res.string.permissions_microphone_body
    }
