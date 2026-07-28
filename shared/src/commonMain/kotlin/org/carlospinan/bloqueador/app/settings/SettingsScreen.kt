package org.carlospinan.bloqueador.app.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.action_cancel
import bloqueallamadas.shared.generated.resources.ic_autoresponder
import bloqueallamadas.shared.generated.resources.ic_backup
import bloqueallamadas.shared.generated.resources.ic_blocking
import bloqueallamadas.shared.generated.resources.ic_contacts
import bloqueallamadas.shared.generated.resources.ic_default_action
import bloqueallamadas.shared.generated.resources.ic_privacy
import bloqueallamadas.shared.generated.resources.ic_spam_provider
import bloqueallamadas.shared.generated.resources.settings_auto_allow_contacts
import bloqueallamadas.shared.generated.resources.settings_auto_allow_contacts_desc
import bloqueallamadas.shared.generated.resources.settings_autoresponder
import bloqueallamadas.shared.generated.resources.settings_autoresponder_desc
import bloqueallamadas.shared.generated.resources.settings_backup
import bloqueallamadas.shared.generated.resources.settings_backup_desc
import bloqueallamadas.shared.generated.resources.settings_blocking_enabled
import bloqueallamadas.shared.generated.resources.settings_blocking_enabled_desc
import bloqueallamadas.shared.generated.resources.settings_default_action
import bloqueallamadas.shared.generated.resources.settings_default_action_allow
import bloqueallamadas.shared.generated.resources.settings_default_action_ask
import bloqueallamadas.shared.generated.resources.settings_default_action_block
import bloqueallamadas.shared.generated.resources.settings_default_action_desc
import bloqueallamadas.shared.generated.resources.settings_grant_contacts
import bloqueallamadas.shared.generated.resources.settings_privacy_desc
import bloqueallamadas.shared.generated.resources.settings_privacy_title
import bloqueallamadas.shared.generated.resources.settings_spam_provider
import bloqueallamadas.shared.generated.resources.settings_spam_provider_desc
import bloqueallamadas.shared.generated.resources.settings_terms_desc
import bloqueallamadas.shared.generated.resources.settings_terms_title
import bloqueallamadas.shared.generated.resources.settings_title
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    blockingEnabled: Boolean,
    autoAllowContacts: Boolean,
    defaultAction: DefaultAction,
    spamEnabled: Boolean,
    showGrantContacts: Boolean,
    onSetBlockingEnabled: (Boolean) -> Unit,
    onSetAutoAllowContacts: (Boolean) -> Unit,
    onSetDefaultAction: (DefaultAction) -> Unit,
    onSetSpamEnabled: (Boolean) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onNavigateToAutoResponder: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onBack: () -> Unit,
) {
    var showDefaultActionDialog by remember { mutableStateOf(false) }
    val windowSizeClass = rememberWindowSizeClass()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdaptiveContent(
                windowSizeClass = windowSizeClass,
                contentModifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(Res.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingToggle(
                    title = stringResource(Res.string.settings_blocking_enabled),
                    description = stringResource(Res.string.settings_blocking_enabled_desc),
                    icon = Res.drawable.ic_blocking,
                    checked = blockingEnabled,
                    onCheckedChange = onSetBlockingEnabled,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggle(
                    title = stringResource(Res.string.settings_auto_allow_contacts),
                    description = stringResource(Res.string.settings_auto_allow_contacts_desc),
                    icon = Res.drawable.ic_contacts,
                    checked = autoAllowContacts,
                    onCheckedChange = onSetAutoAllowContacts,
                )

                if (showGrantContacts && autoAllowContacts) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onRequestContactsPermission) {
                        Text(stringResource(Res.string.settings_grant_contacts))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingDropdown(
                    title = stringResource(Res.string.settings_default_action),
                    description = stringResource(Res.string.settings_default_action_desc),
                    icon = Res.drawable.ic_default_action,
                    value =
                        when (defaultAction) {
                            DefaultAction.ALLOW -> stringResource(Res.string.settings_default_action_allow)
                            DefaultAction.BLOCK -> stringResource(Res.string.settings_default_action_block)
                            DefaultAction.ASK -> stringResource(Res.string.settings_default_action_ask)
                        },
                    onClick = { showDefaultActionDialog = true },
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggle(
                    title = stringResource(Res.string.settings_spam_provider),
                    description = stringResource(Res.string.settings_spam_provider_desc),
                    icon = Res.drawable.ic_spam_provider,
                    checked = spamEnabled,
                    onCheckedChange = onSetSpamEnabled,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingDropdown(
                    title = stringResource(Res.string.settings_autoresponder),
                    description = stringResource(Res.string.settings_autoresponder_desc),
                    icon = Res.drawable.ic_autoresponder,
                    value = stringResource(Res.string.settings_autoresponder),
                    onClick = onNavigateToAutoResponder,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingDropdown(
                    title = stringResource(Res.string.settings_backup),
                    description = stringResource(Res.string.settings_backup_desc),
                    icon = Res.drawable.ic_backup,
                    value = stringResource(Res.string.settings_backup),
                    onClick = onNavigateToBackup,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingDropdown(
                    title = stringResource(Res.string.settings_privacy_title),
                    description = stringResource(Res.string.settings_privacy_desc),
                    icon = Res.drawable.ic_privacy,
                    value = "View",
                    onClick = onNavigateToPrivacy,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingDropdown(
                    title = stringResource(Res.string.settings_terms_title),
                    description = stringResource(Res.string.settings_terms_desc),
                    icon = Res.drawable.ic_privacy,
                    value = "View",
                    onClick = onNavigateToTerms,
                )
            }
        }
    }

    if (showDefaultActionDialog) {
        DefaultActionDialog(
            current = defaultAction,
            onSelect = { action ->
                onSetDefaultAction(action)
                showDefaultActionDialog = false
            },
            onDismiss = { showDefaultActionDialog = false },
        )
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    icon: DrawableResource,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun SettingDropdown(
    title: String,
    description: String,
    icon: DrawableResource,
    value: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DefaultActionDialog(
    current: DefaultAction,
    onSelect: (DefaultAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_default_action)) },
        text = {
            Column {
                DefaultAction.entries.forEach { action ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(action) }
                                .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = action == current,
                            onClick = { onSelect(action) },
                        )
                        Text(
                            text =
                                when (action) {
                                    DefaultAction.ALLOW -> stringResource(Res.string.settings_default_action_allow)
                                    DefaultAction.BLOCK -> stringResource(Res.string.settings_default_action_block)
                                    DefaultAction.ASK -> stringResource(Res.string.settings_default_action_ask)
                                },
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
