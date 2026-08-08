package org.carlospinan.bloqueador.app.permissions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.action_open_settings
import cortaspam.shared.generated.resources.permissions_dialer_role_missing
import cortaspam.shared.generated.resources.permissions_dialer_role_missing_desc
import cortaspam.shared.generated.resources.permissions_set_as_default
import cortaspam.shared.generated.resources.settings_call_permission_disabled
import cortaspam.shared.generated.resources.settings_call_permission_disabled_desc
import cortaspam.shared.generated.resources.settings_fullscreen_disabled
import cortaspam.shared.generated.resources.settings_fullscreen_disabled_desc
import cortaspam.shared.generated.resources.settings_notifications_disabled
import cortaspam.shared.generated.resources.settings_notifications_disabled_desc
import org.jetbrains.compose.resources.stringResource

/**
 * Warnings for the permissions the app cannot function normally without.
 *
 * Rendered on Home *and* Settings on purpose. These used to live only in Settings, which is a
 * screen a user who never opens it never sees -- so an app that had silently stopped screening
 * calls looked identical to one that was working. Home is where the user notices.
 *
 * Contacts and microphone are deliberately absent: both are optional, both are explained where
 * the feature that needs them lives, and a banner for a permission the user has not asked to
 * use is a nag rather than a warning.
 *
 * Every parameter defaults to the non-warning value so iOS -- which builds these screens too but
 * has none of these concepts -- renders nothing instead of a fix button that does nothing.
 */
@Composable
fun PermissionWarnings(
    modifier: Modifier = Modifier,
    dialerRoleHeld: Boolean = true,
    notificationsPermissionGranted: Boolean = true,
    fullScreenIntentAllowed: Boolean = true,
    callPhonePermissionGranted: Boolean = true,
    onRequestDialerRole: () -> Unit = {},
    onOpenNotificationSettings: () -> Unit = {},
    onOpenFullScreenIntentSettings: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
) {
    Column(modifier = modifier) {
        if (!dialerRoleHeld) {
            PermissionWarningCard(
                title = stringResource(Res.string.permissions_dialer_role_missing),
                description = stringResource(Res.string.permissions_dialer_role_missing_desc),
                actionLabel = stringResource(Res.string.permissions_set_as_default),
                onFix = onRequestDialerRole,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!notificationsPermissionGranted) {
            PermissionWarningCard(
                title = stringResource(Res.string.settings_notifications_disabled),
                description = stringResource(Res.string.settings_notifications_disabled_desc),
                onFix = onOpenNotificationSettings,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!fullScreenIntentAllowed) {
            PermissionWarningCard(
                title = stringResource(Res.string.settings_fullscreen_disabled),
                description = stringResource(Res.string.settings_fullscreen_disabled_desc),
                onFix = onOpenFullScreenIntentSettings,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!callPhonePermissionGranted) {
            PermissionWarningCard(
                title = stringResource(Res.string.settings_call_permission_disabled),
                description = stringResource(Res.string.settings_call_permission_disabled_desc),
                onFix = onOpenAppSettings,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PermissionWarningCard(
    title: String,
    description: String,
    onFix: () -> Unit,
    actionLabel: String = stringResource(Res.string.action_open_settings),
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onFix) {
                Text(actionLabel)
            }
        }
    }
}
