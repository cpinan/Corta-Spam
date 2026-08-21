package org.carlospinan.bloqueador.app.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.action_cancel
import cortaspam.shared.generated.resources.action_view
import cortaspam.shared.generated.resources.autoresponder_experimental_badge
import cortaspam.shared.generated.resources.ic_autoresponder
import cortaspam.shared.generated.resources.ic_backup
import cortaspam.shared.generated.resources.ic_blocking
import cortaspam.shared.generated.resources.ic_brand_app
import cortaspam.shared.generated.resources.ic_contacts
import cortaspam.shared.generated.resources.ic_default_action
import cortaspam.shared.generated.resources.ic_keypad
import cortaspam.shared.generated.resources.ic_privacy
import cortaspam.shared.generated.resources.ic_settings
import cortaspam.shared.generated.resources.ic_unknown_call
import cortaspam.shared.generated.resources.settings_auto_allow_contacts
import cortaspam.shared.generated.resources.settings_auto_allow_contacts_desc
import cortaspam.shared.generated.resources.settings_autoresponder
import cortaspam.shared.generated.resources.settings_autoresponder_desc
import cortaspam.shared.generated.resources.settings_backup
import cortaspam.shared.generated.resources.settings_backup_desc
import cortaspam.shared.generated.resources.settings_blocking_enabled
import cortaspam.shared.generated.resources.settings_blocking_enabled_desc
import cortaspam.shared.generated.resources.settings_credits_desc
import cortaspam.shared.generated.resources.settings_credits_title
import cortaspam.shared.generated.resources.settings_default_action
import cortaspam.shared.generated.resources.settings_default_action_allow
import cortaspam.shared.generated.resources.settings_default_action_ask
import cortaspam.shared.generated.resources.settings_default_action_block
import cortaspam.shared.generated.resources.settings_default_action_desc
import cortaspam.shared.generated.resources.settings_emergency_exemption
import cortaspam.shared.generated.resources.settings_emergency_exemption_desc
import cortaspam.shared.generated.resources.settings_grant_contacts
import cortaspam.shared.generated.resources.settings_notify_unknown_callers
import cortaspam.shared.generated.resources.settings_notify_unknown_callers_desc
import cortaspam.shared.generated.resources.settings_privacy_desc
import cortaspam.shared.generated.resources.settings_privacy_title
import cortaspam.shared.generated.resources.settings_recent_on_keypad
import cortaspam.shared.generated.resources.settings_recent_on_keypad_desc
import cortaspam.shared.generated.resources.settings_repeated_caller_bypass
import cortaspam.shared.generated.resources.settings_repeated_caller_bypass_count_label
import cortaspam.shared.generated.resources.settings_repeated_caller_bypass_desc
import cortaspam.shared.generated.resources.settings_section_about
import cortaspam.shared.generated.resources.settings_section_blocking
import cortaspam.shared.generated.resources.settings_section_contacts
import cortaspam.shared.generated.resources.settings_section_notifications
import cortaspam.shared.generated.resources.settings_show_notifications
import cortaspam.shared.generated.resources.settings_show_notifications_desc
import cortaspam.shared.generated.resources.settings_terms_desc
import cortaspam.shared.generated.resources.settings_terms_title
import cortaspam.shared.generated.resources.settings_title
import cortaspam.shared.generated.resources.settings_version_desc
import cortaspam.shared.generated.resources.settings_version_title
import cortaspam.shared.generated.resources.settings_version_value
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.permissions.PermissionWarnings
import org.carlospinan.bloqueador.app.theme.CortaSpamTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val MIN_REPEATED_CALLER_BYPASS_COUNT = 2
private const val MAX_REPEATED_CALLER_BYPASS_COUNT = 10
private const val DEFAULT_REPEATED_CALLER_BYPASS_COUNT = 3

/**
 * Sections of the Expanded (tablet) list-detail layout. Compact and Medium ignore this and
 * render every setting in one scrolling column, in the order they have always been in.
 */
private enum class SettingsSection {
    Blocking,
    Contacts,
    Notifications,
    About,
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    showGrantContacts: Boolean,
    dialerRoleHeld: Boolean = true,
    notificationsPermissionGranted: Boolean = true,
    fullScreenIntentAllowed: Boolean = true,
    callPhonePermissionGranted: Boolean = true,
    onRequestDialerRole: () -> Unit = {},
    onSetBlockingEnabled: (Boolean) -> Unit,
    onSetAutoAllowContacts: (Boolean) -> Unit,
    onSetShowRecentCallersOnKeypad: (Boolean) -> Unit,
    onSetDefaultAction: (DefaultAction) -> Unit,
    onSetSpamEnabled: (Boolean) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit = {},
    onSetNotifyUnknownCallers: (Boolean) -> Unit = {},
    onSetRepeatedCallerBypassCount: (Int) -> Unit = {},
    onSetEmergencyCallbackExemption: (Boolean) -> Unit = {},
    onRequestContactsPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit = {},
    onOpenFullScreenIntentSettings: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onNavigateToAutoResponder: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToCredits: () -> Unit = {},
    /** Shown at the end of About. Null only in tests that do not care which build they render. */
    appVersion: AppVersion? = null,
    onBack: () -> Unit,
    // Injectable so a test can exercise the Expanded layout without a wide container,
    // the same shape AdaptiveScaffold already uses.
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass(),
) {
    var showDefaultActionDialog by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(SettingsSection.Blocking) }

    CortaSpamTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (windowSizeClass == WindowSizeClass.Expanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SettingsSectionPane(
                        selected = selectedSection,
                        onSelect = { selectedSection = it },
                        onNavigateToAutoResponder = onNavigateToAutoResponder,
                        onNavigateToBackup = onNavigateToBackup,
                        modifier = Modifier.width(340.dp).fillMaxHeight(),
                    )
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                    ) {
                        // Permission warnings render on every section, not just the matching one:
                        // they are app-level failures, and filing the notification warning under
                        // "Notifications" would make it findable only by accident.
                        PermissionWarnings(
                            dialerRoleHeld = dialerRoleHeld,
                            notificationsPermissionGranted = notificationsPermissionGranted,
                            fullScreenIntentAllowed = fullScreenIntentAllowed,
                            callPhonePermissionGranted = callPhonePermissionGranted,
                            onRequestDialerRole = onRequestDialerRole,
                            onOpenNotificationSettings = onOpenNotificationSettings,
                            onOpenFullScreenIntentSettings = onOpenFullScreenIntentSettings,
                            onOpenAppSettings = onOpenAppSettings,
                        )

                        when (selectedSection) {
                            SettingsSection.Blocking -> {
                                BlockingToggleItem(state.blockingEnabled, onSetBlockingEnabled)
                                Spacer(modifier = Modifier.height(12.dp))
                                RepeatedCallerBypassItem(state.repeatedCallerBypassCount, onSetRepeatedCallerBypassCount)
                                Spacer(modifier = Modifier.height(12.dp))
                                DefaultActionItem(state.defaultAction) { showDefaultActionDialog = true }
                                Spacer(modifier = Modifier.height(12.dp))
                                EmergencyExemptionItem(state.emergencyCallbackExemption, onSetEmergencyCallbackExemption)
                            }

                            SettingsSection.Contacts -> {
                                ContactsToggleItem(
                                    checked = state.autoAllowContacts,
                                    onCheckedChange = onSetAutoAllowContacts,
                                    showGrantContacts = showGrantContacts,
                                    onRequestContactsPermission = onRequestContactsPermission,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                RecentCallersToggleItem(
                                    checked = state.showRecentCallersOnKeypad,
                                    onCheckedChange = onSetShowRecentCallersOnKeypad,
                                )
                            }

                            SettingsSection.Notifications -> {
                                NotificationsToggleItem(state.notificationsEnabled, onSetNotificationsEnabled)
                                if (state.notificationsEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    UnknownCallerToggleItem(state.notifyUnknownCallers, onSetNotifyUnknownCallers)
                                }
                            }

                            SettingsSection.About -> {
                                PrivacyItem(onNavigateToPrivacy)
                                Spacer(modifier = Modifier.height(12.dp))
                                TermsItem(onNavigateToTerms)
                                Spacer(modifier = Modifier.height(12.dp))
                                CreditsItem(onNavigateToCredits)
                                if (appVersion != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    VersionItem(appVersion)
                                }
                            }
                        }
                    }
                }
            } else {
                AdaptiveContent(
                    windowSizeClass = windowSizeClass,
                    contentModifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PermissionWarnings(
                        dialerRoleHeld = dialerRoleHeld,
                        notificationsPermissionGranted = notificationsPermissionGranted,
                        fullScreenIntentAllowed = fullScreenIntentAllowed,
                        callPhonePermissionGranted = callPhonePermissionGranted,
                        onRequestDialerRole = onRequestDialerRole,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onOpenFullScreenIntentSettings = onOpenFullScreenIntentSettings,
                        onOpenAppSettings = onOpenAppSettings,
                    )

                    BlockingToggleItem(state.blockingEnabled, onSetBlockingEnabled)

                    Spacer(modifier = Modifier.height(12.dp))

                    NotificationsToggleItem(state.notificationsEnabled, onSetNotificationsEnabled)

                    // Hidden rather than disabled while the parent is off: it reads as a
                    // promise the app cannot keep when nothing is being posted at all.
                    if (state.notificationsEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        UnknownCallerToggleItem(state.notifyUnknownCallers, onSetNotifyUnknownCallers)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ContactsToggleItem(
                        checked = state.autoAllowContacts,
                        onCheckedChange = onSetAutoAllowContacts,
                        showGrantContacts = showGrantContacts,
                        onRequestContactsPermission = onRequestContactsPermission,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    RecentCallersToggleItem(
                        checked = state.showRecentCallersOnKeypad,
                        onCheckedChange = onSetShowRecentCallersOnKeypad,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    RepeatedCallerBypassItem(state.repeatedCallerBypassCount, onSetRepeatedCallerBypassCount)

                    Spacer(modifier = Modifier.height(12.dp))

                    DefaultActionItem(state.defaultAction) { showDefaultActionDialog = true }

                    Spacer(modifier = Modifier.height(12.dp))

                    EmergencyExemptionItem(state.emergencyCallbackExemption, onSetEmergencyCallbackExemption)

                    Spacer(modifier = Modifier.height(12.dp))

                    AutoResponderItem(onNavigateToAutoResponder)

                    Spacer(modifier = Modifier.height(12.dp))

                    BackupItem(onNavigateToBackup)

                    Spacer(modifier = Modifier.height(12.dp))

                    PrivacyItem(onNavigateToPrivacy)

                    Spacer(modifier = Modifier.height(12.dp))

                    TermsItem(onNavigateToTerms)

                    Spacer(modifier = Modifier.height(12.dp))

                    CreditsItem(onNavigateToCredits)

                    // Last, and on every layout: the first thing a bug report has to establish is
                    // which build it is about, and "scroll to the bottom of Settings" is the
                    // instruction every other phone app has already taught the user.
                    if (appVersion != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        VersionItem(appVersion)
                    }
                }
            }
        }
    }

    if (showDefaultActionDialog) {
        DefaultActionDialog(
            current = state.defaultAction,
            onSelect = { action ->
                onSetDefaultAction(action)
                showDefaultActionDialog = false
            },
            onDismiss = { showDefaultActionDialog = false },
        )
    }
}

/** Left pane of the Expanded layout: section picker plus the two rows that navigate away. */
@Composable
private fun SettingsSectionPane(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    onNavigateToAutoResponder: () -> Unit,
    onNavigateToBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection.entries.forEach { section ->
            SectionRow(
                title =
                    when (section) {
                        SettingsSection.Blocking -> stringResource(Res.string.settings_section_blocking)
                        SettingsSection.Contacts -> stringResource(Res.string.settings_section_contacts)
                        SettingsSection.Notifications -> stringResource(Res.string.settings_section_notifications)
                        SettingsSection.About -> stringResource(Res.string.settings_section_about)
                    },
                icon =
                    when (section) {
                        SettingsSection.Blocking -> Res.drawable.ic_blocking
                        SettingsSection.Contacts -> Res.drawable.ic_contacts
                        SettingsSection.Notifications -> Res.drawable.ic_settings
                        SettingsSection.About -> Res.drawable.ic_privacy
                    },
                selected = section == selected,
                onClick = { onSelect(section) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Auto-responder and Backup are whole screens, not settings panels, so they keep
        // navigating away instead of becoming detail-pane sections.
        AutoResponderItem(onNavigateToAutoResponder)

        Spacer(modifier = Modifier.height(12.dp))

        BackupItem(onNavigateToBackup)
    }
}

@Composable
private fun SectionRow(
    title: String,
    icon: DrawableResource,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors =
            if (selected) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            } else {
                CardDefaults.cardColors()
            },
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun BlockingToggleItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingToggle(
        title = stringResource(Res.string.settings_blocking_enabled),
        description = stringResource(Res.string.settings_blocking_enabled_desc),
        icon = Res.drawable.ic_blocking,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun NotificationsToggleItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingToggle(
        title = stringResource(Res.string.settings_show_notifications),
        description = stringResource(Res.string.settings_show_notifications_desc),
        icon = Res.drawable.ic_settings,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

/**
 * Sub-toggle of [NotificationsToggleItem]: whether the blocked / missed / repeated-caller
 * notifications are posted for numbers the address book does not claim. It does not touch the
 * ringing call screen — see SettingsRepository.notifyUnknownCallers.
 */
@Composable
private fun UnknownCallerToggleItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingToggle(
        title = stringResource(Res.string.settings_notify_unknown_callers),
        description = stringResource(Res.string.settings_notify_unknown_callers_desc),
        icon = Res.drawable.ic_unknown_call,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun ContactsToggleItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showGrantContacts: Boolean,
    onRequestContactsPermission: () -> Unit,
) {
    SettingToggle(
        title = stringResource(Res.string.settings_auto_allow_contacts),
        description = stringResource(Res.string.settings_auto_allow_contacts_desc),
        icon = Res.drawable.ic_contacts,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )

    if (showGrantContacts && checked) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRequestContactsPermission) {
            Text(stringResource(Res.string.settings_grant_contacts))
        }
    }
}

/**
 * The keypad's strip of who rang recently. Starred contacts are not covered by it: those are a
 * choice the user made and every dialer shows them, while this is call history appearing on a
 * screen anyone holding the unlocked phone can open.
 */
@Composable
private fun RecentCallersToggleItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingToggle(
        title = stringResource(Res.string.settings_recent_on_keypad),
        description = stringResource(Res.string.settings_recent_on_keypad_desc),
        icon = Res.drawable.ic_keypad,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun RepeatedCallerBypassItem(
    count: Int,
    onCountChange: (Int) -> Unit,
) {
    SettingToggle(
        title = stringResource(Res.string.settings_repeated_caller_bypass),
        description = stringResource(Res.string.settings_repeated_caller_bypass_desc),
        icon = Res.drawable.ic_unknown_call,
        checked = count > 0,
        onCheckedChange = { checked ->
            onCountChange(if (checked) DEFAULT_REPEATED_CALLER_BYPASS_COUNT else 0)
        },
    )

    if (count > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_repeated_caller_bypass_count_label),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = { onCountChange((count - 1).coerceAtLeast(MIN_REPEATED_CALLER_BYPASS_COUNT)) },
            ) { Text("\u2212") }
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                onClick = { onCountChange((count + 1).coerceAtMost(MAX_REPEATED_CALLER_BYPASS_COUNT)) },
            ) { Text("+") }
        }
    }
}

/**
 * Sits directly under the default action, which is the setting it exists to override: with the
 * default set to BLOCK, an ambulance ringing back from a number that is not in the address book
 * was blocked -- and with the auto-responder on, answered and hung up on.
 */
@Composable
private fun EmergencyExemptionItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingToggle(
        title = stringResource(Res.string.settings_emergency_exemption),
        description = stringResource(Res.string.settings_emergency_exemption_desc),
        icon = Res.drawable.ic_unknown_call,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun DefaultActionItem(
    current: DefaultAction,
    onClick: () -> Unit,
) {
    SettingDropdown(
        title = stringResource(Res.string.settings_default_action),
        description = stringResource(Res.string.settings_default_action_desc),
        icon = Res.drawable.ic_default_action,
        value =
            when (current) {
                DefaultAction.ALLOW -> stringResource(Res.string.settings_default_action_allow)
                DefaultAction.BLOCK -> stringResource(Res.string.settings_default_action_block)
                DefaultAction.ASK -> stringResource(Res.string.settings_default_action_ask)
            },
        onClick = onClick,
    )
}

@Composable
private fun AutoResponderItem(onClick: () -> Unit) {
    SettingDropdown(
        title = stringResource(Res.string.settings_autoresponder),
        description = stringResource(Res.string.settings_autoresponder_desc),
        icon = Res.drawable.ic_autoresponder,
        value = stringResource(Res.string.autoresponder_experimental_badge),
        onClick = onClick,
    )
}

@Composable
private fun BackupItem(onClick: () -> Unit) {
    SettingDropdown(
        title = stringResource(Res.string.settings_backup),
        description = stringResource(Res.string.settings_backup_desc),
        icon = Res.drawable.ic_backup,
        value = stringResource(Res.string.settings_backup),
        onClick = onClick,
    )
}

@Composable
private fun PrivacyItem(onClick: () -> Unit) {
    SettingDropdown(
        title = stringResource(Res.string.settings_privacy_title),
        description = stringResource(Res.string.settings_privacy_desc),
        icon = Res.drawable.ic_privacy,
        value = stringResource(Res.string.action_view),
        onClick = onClick,
    )
}

@Composable
private fun TermsItem(onClick: () -> Unit) {
    SettingDropdown(
        title = stringResource(Res.string.settings_terms_title),
        description = stringResource(Res.string.settings_terms_desc),
        icon = Res.drawable.ic_privacy,
        value = stringResource(Res.string.action_view),
        onClick = onClick,
    )
}

@Composable
private fun CreditsItem(onClick: () -> Unit) {
    SettingDropdown(
        title = stringResource(Res.string.settings_credits_title),
        description = stringResource(Res.string.settings_credits_desc),
        icon = Res.drawable.ic_contacts,
        value = stringResource(Res.string.action_view),
        onClick = onClick,
    )
}

/**
 * The build the user is on. Not clickable — there is nowhere for it to go, and a row that looks
 * tappable and is not is worse than a plain one.
 */
@Composable
private fun VersionItem(appVersion: AppVersion) {
    SettingInfo(
        title = stringResource(Res.string.settings_version_title),
        description = stringResource(Res.string.settings_version_desc),
        icon = Res.drawable.ic_brand_app,
        value =
            stringResource(
                Res.string.settings_version_value,
                appVersion.name,
                // As a string, not an Int: the code is a Long, and the argument goes through the
                // multiplatform resource formatter rather than Android's String.format.
                appVersion.code.toString(),
            ),
    )
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
    SettingInfo(
        title = title,
        description = description,
        icon = icon,
        value = value,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** A settings row that states something instead of changing it: icon, title, description, value. */
@Composable
private fun SettingInfo(
    title: String,
    description: String,
    icon: DrawableResource,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
