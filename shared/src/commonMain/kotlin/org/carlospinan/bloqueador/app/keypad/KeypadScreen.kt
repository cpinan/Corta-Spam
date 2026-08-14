package org.carlospinan.bloqueador.app.keypad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.keypad_call
import cortaspam.shared.generated.resources.keypad_contact_row
import cortaspam.shared.generated.resources.keypad_contacts_denied
import cortaspam.shared.generated.resources.keypad_delete
import cortaspam.shared.generated.resources.keypad_hint
import cortaspam.shared.generated.resources.keypad_more_matches
import cortaspam.shared.generated.resources.keypad_no_matches
import cortaspam.shared.generated.resources.keypad_title
import cortaspam.shared.generated.resources.settings_grant_contacts
import org.carlospinan.bloqueador.app.adaptive.ScrollableScreenColumn
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactSearch
import org.jetbrains.compose.resources.stringResource

/**
 * The dial pad, and the app's contact search. This app takes `ROLE_DIALER`, which means the
 * platform dialer stops being the user's way to place a call -- without this screen, becoming the
 * default phone app removed the ability to make one, and without the search half it removed the
 * ability to call anyone whose number you do not know by heart.
 *
 * One field serves both: what is typed is dialled by the Call button and searched for among
 * [contacts] at the same time. Two separate inputs (a search box and a number display) would make
 * the user choose which one they are using before they know which one they need -- and the
 * platform dialer they are replacing does not ask them that either.
 *
 * [dialRequest] is what an `ACTION_DIAL` intent carried. The screen deliberately does *not* place
 * that call itself: `ACTION_DIAL` means "show this number, let the user decide", unlike
 * `ACTION_CALL`. Dialling it automatically would turn every `tel:` link on the web into a call
 * placed without a confirmation.
 */
@Composable
fun KeypadScreen(
    dialRequest: DialRequest? = null,
    onCall: (String) -> Unit = {},
    contacts: List<Contact> = emptyList(),
    contactsPermissionGranted: Boolean = true,
    onRequestContactsPermission: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var typed by rememberSaveable { mutableStateOf("") }

    // Applied at most once per request. Keying the effect on the request alone is not enough:
    // navigating away and back builds a fresh effect, which would retype a number the user had
    // already deleted. Keying on the id it last applied is what makes "again" different from
    // "still the same request".
    var appliedRequestId by rememberSaveable { mutableStateOf(NO_REQUEST_APPLIED) }
    LaunchedEffect(dialRequest) {
        val request = dialRequest ?: return@LaunchedEffect
        if (request.id == appliedRequestId) return@LaunchedEffect
        typed = request.number
        appliedRequestId = request.id
    }

    val matches = remember(contacts, typed) { ContactSearch.match(contacts, typed) }
    val focusManager = LocalFocusManager.current

    MaterialTheme {
        Surface(modifier = modifier.fillMaxWidth()) {
            ScrollableScreenColumn(
                contentPadding = PaddingValues(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.keypad_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = { Text(stringResource(Res.string.keypad_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                )

                ContactMatches(
                    query = typed,
                    matches = matches,
                    contactsPermissionGranted = contactsPermissionGranted,
                    onRequestContactsPermission = onRequestContactsPermission,
                    onPick = { contact ->
                        typed = contact.number
                        // Dismisses the soft keyboard with the focus. Measured on a razr 50 ultra:
                        // with the keyboard up, the results list pushes the Call button off the
                        // bottom of the screen, so picking a contact left the user searching for
                        // the button that places the call.
                        focusManager.clearFocus()
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                KEY_ROWS.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { key ->
                            OutlinedButton(
                                onClick = { typed += key.digit },
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            ) {
                                Text(
                                    text = key.digit,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // '+' sits here rather than behind a long-press on 0: a long-press is
                    // undiscoverable, and without any way to type it no international number
                    // could be dialled from this screen at all.
                    OutlinedButton(
                        onClick = { typed += "+" },
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Text(text = "+", style = MaterialTheme.typography.titleLarge)
                    }

                    val deleteLabel = stringResource(Res.string.keypad_delete)
                    TextButton(
                        onClick = { typed = typed.dropLast(1) },
                        enabled = typed.isNotEmpty(),
                        modifier = Modifier.semantics { contentDescription = deleteLabel },
                    ) {
                        Text(text = "⌫")
                    }

                    Button(
                        onClick = { onCall(typed) },
                        enabled = typed.isNotBlank(),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Text(text = stringResource(Res.string.keypad_call))
                    }
                }
            }
        }
    }
}

/**
 * The search results, between the field and the pad.
 *
 * Nothing at all is shown for an empty query: the pad is what this screen is for, and pushing it
 * below a full address book to reach it would be a regression for the user who came here to type
 * a number. Results appear as soon as there is something to match on.
 *
 * A row's number fills the field rather than dialling, so the number is on screen before the call
 * is placed; the call button on the row is the one that dials. A single tap that both picks a
 * contact and rings them gives a mis-tap no recovery.
 */
@Composable
private fun ContactMatches(
    query: String,
    matches: List<Contact>,
    contactsPermissionGranted: Boolean,
    onRequestContactsPermission: () -> Unit,
    onPick: (Contact) -> Unit,
) {
    if (query.isBlank()) return

    if (!contactsPermissionGranted) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.keypad_contacts_denied),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onRequestContactsPermission) {
            Text(stringResource(Res.string.settings_grant_contacts))
        }
        return
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (matches.isEmpty()) {
        Text(
            text = stringResource(Res.string.keypad_no_matches),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    // This screen is one scrolling Column, not a lazy list -- rendering 500 matching contacts
    // would build 500 rows for a query one more character would have narrowed. The overflow is
    // stated rather than silently dropped, so a missing contact reads as "keep typing" instead
    // of "not in your phone".
    matches.take(MAX_VISIBLE_MATCHES).forEach { contact ->
        ContactRow(contact = contact, onPick = { onPick(contact) })
        HorizontalDivider()
    }
    if (matches.size > MAX_VISIBLE_MATCHES) {
        Text(
            text = stringResource(Res.string.keypad_more_matches),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

/**
 * A result row fills the field instead of dialling straight away.
 *
 * The alternative -- tap a name, phone rings -- puts a call one mis-tap away in a list that
 * reshuffles under the finger on every keystroke, and this screen already has a Call button two
 * centimetres below. Picking a contact is the search result; placing the call stays one
 * deliberate press.
 */
@Composable
private fun ContactRow(
    contact: Contact,
    onPick: () -> Unit,
) {
    val label = stringResource(Res.string.keypad_contact_row, contact.name, contact.number)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onPick)
                .semantics { contentDescription = label }
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = contact.number,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val NO_REQUEST_APPLIED = -1L

/** Enough to recognise the right contact without burying the pad below the fold. */
private const val MAX_VISIBLE_MATCHES = 5

private data class KeypadKey(
    val digit: String,
)

private val KEY_ROWS: List<List<KeypadKey>> =
    listOf(
        listOf(KeypadKey("1"), KeypadKey("2"), KeypadKey("3")),
        listOf(KeypadKey("4"), KeypadKey("5"), KeypadKey("6")),
        listOf(KeypadKey("7"), KeypadKey("8"), KeypadKey("9")),
        listOf(KeypadKey("*"), KeypadKey("0"), KeypadKey("#")),
    )
