package org.carlospinan.bloqueador.app.agenda

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.action_cancel
import cortaspam.shared.generated.resources.agenda_action_call
import cortaspam.shared.generated.resources.agenda_contacts_denied
import cortaspam.shared.generated.resources.agenda_empty
import cortaspam.shared.generated.resources.agenda_filter_all
import cortaspam.shared.generated.resources.agenda_filter_allowed
import cortaspam.shared.generated.resources.agenda_filter_blocked
import cortaspam.shared.generated.resources.agenda_filter_starred
import cortaspam.shared.generated.resources.agenda_no_matches
import cortaspam.shared.generated.resources.agenda_title
import cortaspam.shared.generated.resources.call_log_action_allowlist
import cortaspam.shared.generated.resources.call_log_action_block
import cortaspam.shared.generated.resources.call_log_action_copy
import cortaspam.shared.generated.resources.call_log_action_remove_allowlist
import cortaspam.shared.generated.resources.call_log_action_unblock
import cortaspam.shared.generated.resources.call_log_number_allowlisted
import cortaspam.shared.generated.resources.call_log_number_blocked
import cortaspam.shared.generated.resources.call_log_search_hint
import cortaspam.shared.generated.resources.keypad_contact_row
import cortaspam.shared.generated.resources.settings_grant_contacts
import org.carlospinan.bloqueador.app.adaptive.AdaptiveContent
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.calllog.NumberRuleState
import org.carlospinan.bloqueador.app.calllog.numberRuleStates
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactAvatar
import org.carlospinan.bloqueador.app.contacts.FavouritesRow
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.theme.CortaSpamTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The address book, as a call blocker sees it.
 *
 * Taking `ROLE_DIALER` replaced the phone app, and a phone app has an address book. Without this
 * screen the only way to reach a contact was to remember enough of their name to type it into the
 * keypad's search field -- there was no screen in the app that would simply list the people in the
 * phone, and none at all that could answer "which of my contacts have I blocked", which is the one
 * question this app is in a position to answer and no other app is.
 *
 * Rows are shown with the rule state that applies to them, and tapping one opens the same
 * Block/Allow/Call/Copy actions the call log offers, for the same reason: acting on a person is
 * what the list is for, and a row that only dialled would put a call one mis-tap away in a list
 * the user is scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    contacts: List<Contact> = emptyList(),
    contactsPermissionGranted: Boolean = true,
    blockedNumbers: List<BlockedNumberEntry> = emptyList(),
    allowlistedNumbers: List<AllowlistedNumberEntry> = emptyList(),
    refreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onRequestContactsPermission: () -> Unit = {},
    onCallNumber: (String) -> Unit = {},
    onCopyNumber: (String) -> Unit = {},
    onBlockNumber: (String) -> Unit = {},
    onAllowlistNumber: (String) -> Unit = {},
    onUnblockNumber: (Long) -> Unit = {},
    onRemoveFromAllowlist: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf(AgendaFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Contact?>(null) }
    val windowSizeClass = rememberWindowSizeClass()

    val visible =
        remember(contacts, filter, query, blockedNumbers, allowlistedNumbers) {
            filterAgenda(contacts, filter, query, blockedNumbers, allowlistedNumbers)
        }

    // Computed once for the whole list rather than per row: each lookup is a sameNumber scan of
    // both rule lists, and doing it inside the row composable ran rows x rules of them on every
    // keystroke in the search box.
    val ruleStates =
        remember(visible, blockedNumbers, allowlistedNumbers) {
            numberRuleStates(visible.map { it.number }, blockedNumbers, allowlistedNumbers)
        }

    // Only where they belong: a favourites strip above a list the user has already narrowed is
    // showing them people the filter said to leave out.
    val favourites =
        if (filter == AgendaFilter.ALL && query.isBlank()) contacts.filter { it.starred } else emptyList()

    CortaSpamTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            AdaptiveContent(windowSizeClass = windowSizeClass) {
                Text(
                    text = stringResource(Res.string.agenda_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(Res.string.call_log_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrolls horizontally, because the labels are translated and a row of four chips
                // that fits in English does not fit in Spanish.
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AGENDA_CHIPS.forEach { (value, label) ->
                        FilterChip(
                            selected = filter == value,
                            onClick = { filter = value },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The pull gesture wraps the list rather than the screen, and the list is always a
                // LazyColumn -- including when it has one line of text in it. A message rendered
                // outside the scrollable would be a state the user cannot pull to leave, which is
                // exactly the state they most want to: "no contacts" right after granting the
                // permission is the case the gesture exists for.
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (favourites.isNotEmpty()) {
                            item(key = FAVOURITES_KEY) {
                                FavouritesRow(
                                    favourites = favourites,
                                    onPick = { selected = it },
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                            }
                        }

                        when {
                            !contactsPermissionGranted ->
                                item(key = DENIED_KEY) {
                                    ContactsDenied(onRequestContactsPermission)
                                }

                            contacts.isEmpty() ->
                                item(key = EMPTY_KEY) {
                                    Hint(stringResource(Res.string.agenda_empty))
                                }

                            visible.isEmpty() ->
                                // An empty address book and an empty filter result are different
                                // problems, and the advice for one is nonsense for the other.
                                item(key = NO_MATCHES_KEY) {
                                    Hint(stringResource(Res.string.agenda_no_matches))
                                }

                            else ->
                                items(visible, key = { "${it.name}|${it.number}" }) { contact ->
                                    ContactListRow(
                                        contact = contact,
                                        ruleState = ruleStates[contact.number] ?: NumberRuleState.None,
                                        onTap = { selected = contact },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }

    selected?.let { contact ->
        ContactActionsDialog(
            contact = contact,
            ruleState = ruleStates[contact.number] ?: NumberRuleState.None,
            onDismiss = { selected = null },
            onCallNumber = onCallNumber,
            onCopyNumber = onCopyNumber,
            onBlockNumber = onBlockNumber,
            onAllowlistNumber = onAllowlistNumber,
            onUnblockNumber = onUnblockNumber,
            onRemoveFromAllowlist = onRemoveFromAllowlist,
        )
    }
}

@Composable
private fun ContactListRow(
    contact: Contact,
    ruleState: NumberRuleState,
    onTap: () -> Unit,
) {
    val label = stringResource(Res.string.keypad_contact_row, contact.name, contact.number)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .semantics(mergeDescendants = true) { contentDescription = label }
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(contact.name)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = contact.number,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Stated on the row rather than only inside the dialog: a list of people this app is
            // silencing is worth being able to read at a glance.
            RuleStateLine(ruleState)
        }
    }
}

@Composable
private fun RuleStateLine(ruleState: NumberRuleState) {
    val text =
        when {
            ruleState.isBlocked -> stringResource(Res.string.call_log_number_blocked)
            ruleState.isAllowlisted -> stringResource(Res.string.call_log_number_allowlisted)
            else -> return
        }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color =
            if (ruleState.isBlocked) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
    )
}

@Composable
private fun ContactsDenied(onRequestContactsPermission: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Hint(stringResource(Res.string.agenda_contacts_denied))
        TextButton(onClick = onRequestContactsPermission) {
            Text(stringResource(Res.string.settings_grant_contacts))
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The same actions the call log offers a caller, and one button per meaning: offering "Block this
 * number" for a number that is already blocked was a real bug there -- the tap did nothing visible
 * and nothing in the dialog said the number was already on the list.
 */
@Composable
private fun ContactActionsDialog(
    contact: Contact,
    ruleState: NumberRuleState,
    onDismiss: () -> Unit,
    onCallNumber: (String) -> Unit,
    onCopyNumber: (String) -> Unit,
    onBlockNumber: (String) -> Unit,
    onAllowlistNumber: (String) -> Unit,
    onUnblockNumber: (Long) -> Unit,
    onRemoveFromAllowlist: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contact.name) },
        text = {
            Column {
                Text(
                    text = contact.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RuleStateLine(ruleState)
                TextButton(
                    onClick = {
                        onCallNumber(contact.number)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.agenda_action_call))
                }
                TextButton(
                    onClick = {
                        val blockedRuleId = ruleState.blockedRuleId
                        if (blockedRuleId != null) onUnblockNumber(blockedRuleId) else onBlockNumber(contact.number)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (ruleState.isBlocked) {
                                Res.string.call_log_action_unblock
                            } else {
                                Res.string.call_log_action_block
                            },
                        ),
                    )
                }
                TextButton(
                    onClick = {
                        val allowlistedRuleId = ruleState.allowlistedRuleId
                        if (allowlistedRuleId != null) {
                            onRemoveFromAllowlist(allowlistedRuleId)
                        } else {
                            onAllowlistNumber(contact.number)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (ruleState.isAllowlisted) {
                                Res.string.call_log_action_remove_allowlist
                            } else {
                                Res.string.call_log_action_allowlist
                            },
                        ),
                    )
                }
                TextButton(
                    onClick = {
                        onCopyNumber(contact.number)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.call_log_action_copy))
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

private val AGENDA_CHIPS: List<Pair<AgendaFilter, StringResource>> =
    listOf(
        AgendaFilter.ALL to Res.string.agenda_filter_all,
        AgendaFilter.STARRED to Res.string.agenda_filter_starred,
        AgendaFilter.BLOCKED to Res.string.agenda_filter_blocked,
        AgendaFilter.ALLOWED to Res.string.agenda_filter_allowed,
    )

// Stable keys for the single-item states, so a LazyColumn swapping one message for another does
// not reuse the slot and animate a hint into a contact row.
private const val FAVOURITES_KEY = "favourites"
private const val DENIED_KEY = "denied"
private const val EMPTY_KEY = "empty"
private const val NO_MATCHES_KEY = "no_matches"
