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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import bloqueallamadas.shared.generated.resources.settings_auto_allow_contacts
import bloqueallamadas.shared.generated.resources.settings_auto_allow_contacts_desc
import bloqueallamadas.shared.generated.resources.settings_blocking_enabled
import bloqueallamadas.shared.generated.resources.settings_blocking_enabled_desc
import bloqueallamadas.shared.generated.resources.settings_default_action
import bloqueallamadas.shared.generated.resources.settings_default_action_allow
import bloqueallamadas.shared.generated.resources.settings_default_action_ask
import bloqueallamadas.shared.generated.resources.settings_default_action_block
import bloqueallamadas.shared.generated.resources.settings_default_action_desc
import bloqueallamadas.shared.generated.resources.settings_spam_provider
import bloqueallamadas.shared.generated.resources.settings_spam_provider_desc
import bloqueallamadas.shared.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    blockingEnabled: Boolean,
    autoAllowContacts: Boolean,
    defaultAction: DefaultAction,
    spamEnabled: Boolean,
    onSetBlockingEnabled: (Boolean) -> Unit,
    onSetAutoAllowContacts: (Boolean) -> Unit,
    onSetDefaultAction: (DefaultAction) -> Unit,
    onSetSpamEnabled: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var showDefaultActionDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingToggle(
                    title = stringResource(Res.string.settings_blocking_enabled),
                    description = stringResource(Res.string.settings_blocking_enabled_desc),
                    checked = blockingEnabled,
                    onCheckedChange = onSetBlockingEnabled,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggle(
                    title = stringResource(Res.string.settings_auto_allow_contacts),
                    description = stringResource(Res.string.settings_auto_allow_contacts_desc),
                    checked = autoAllowContacts,
                    onCheckedChange = onSetAutoAllowContacts,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingDropdown(
                    title = stringResource(Res.string.settings_default_action),
                    description = stringResource(Res.string.settings_default_action_desc),
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
                    checked = spamEnabled,
                    onCheckedChange = onSetSpamEnabled,
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
    value: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
