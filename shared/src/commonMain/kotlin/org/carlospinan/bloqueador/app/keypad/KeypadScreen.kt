package org.carlospinan.bloqueador.app.keypad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.keypad_add_contact
import cortaspam.shared.generated.resources.keypad_call
import cortaspam.shared.generated.resources.keypad_contact_row
import cortaspam.shared.generated.resources.keypad_contacts_denied
import cortaspam.shared.generated.resources.keypad_delete
import cortaspam.shared.generated.resources.keypad_delete_all
import cortaspam.shared.generated.resources.keypad_hint
import cortaspam.shared.generated.resources.keypad_more_matches
import cortaspam.shared.generated.resources.keypad_no_matches
import cortaspam.shared.generated.resources.keypad_title
import cortaspam.shared.generated.resources.settings_grant_contacts
import org.carlospinan.bloqueador.app.adaptive.ScrollableScreenColumn
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactSearch
import org.carlospinan.bloqueador.app.theme.CortaSpamTheme
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
    /**
     * Hands the typed number to the platform's own new-contact editor. Taking `ROLE_DIALER` took
     * away the phone app the user used to save a number from, and this screen had no replacement:
     * a number typed or read off a missed call could be dialled and never kept.
     */
    onAddContact: (String) -> Unit = {},
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

    // The query the results were last dismissed for -- by picking a row, or by tapping away.
    // Comparing it with [typed] rather than holding a boolean is what makes the results come back
    // by themselves: the next keystroke changes [typed], which no longer equals this, and there is
    // no reset path to forget.
    var dismissedFor by remember { mutableStateOf<String?>(null) }
    var fieldWidthPx by remember { mutableStateOf(0) }
    var fieldBottomPx by remember { mutableStateOf(0f) }
    var padTopPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // The floating results are capped at the free space between the field and the pad, so they
    // land in the gap the bottom-anchored layout leaves rather than over the keys. A popup is its
    // own window: a touch inside it never reaches what is underneath, so results drawn across the
    // pad would not merely hide the next key, they would swallow the tap on it.
    val gapPx = padTopPx - fieldBottomPx
    val matchesMaxHeight =
        if (gapPx > 0f) {
            with(density) { gapPx.toDp() }.coerceIn(MATCHES_MIN_HEIGHT, MATCHES_MAX_HEIGHT)
        } else {
            MATCHES_MAX_HEIGHT
        }

    CortaSpamTheme {
        Surface(modifier = modifier.fillMaxWidth()) {
            ScrollableScreenColumn(
                contentPadding = PaddingValues(24.dp),
                // The pad is pinned to the bottom of the window and the field to the top, which is
                // what keeps the keys still now that nothing reserves a fixed gap between them:
                // whatever space is left over lands in the middle, where the results float, and no
                // amount of it moves a key. On a window too short for the content the column
                // scrolls instead and the arrangement stops applying, which is the correct
                // fallback -- at a large font scale, reaching the pad matters more than where it
                // sits.
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.keypad_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // The field is the popup's anchor, so it gets a Box of its own: [Popup]
                    // positions itself against the bounds of the layout node it is declared in,
                    // and declaring it straight into the screen's column would anchor it to the
                    // whole column.
                    Box(
                        modifier =
                            Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                                fieldWidthPx = coordinates.size.width
                                fieldBottomPx = coordinates.boundsInWindow().bottom
                            },
                    ) {
                        OutlinedTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            label = { Text(stringResource(Res.string.keypad_hint)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (typed.isNotBlank() && typed != dismissedFor) {
                            ContactMatchesPopup(
                                width = with(density) { fieldWidthPx.toDp() },
                                maxHeight = matchesMaxHeight,
                                matches = matches,
                                contactsPermissionGranted = contactsPermissionGranted,
                                onRequestContactsPermission = onRequestContactsPermission,
                                onPick = { contact ->
                                    typed = contact.number
                                    dismissedFor = contact.number
                                    // Dismisses the soft keyboard with the focus. Measured on a
                                    // razr 50 ultra: with the keyboard up, the results list pushes
                                    // the Call button off the bottom of the screen, so picking a
                                    // contact left the user searching for the button that places
                                    // the call.
                                    focusManager.clearFocus()
                                },
                                onAddContact = {
                                    dismissedFor = typed
                                    focusManager.clearFocus()
                                    onAddContact(typed)
                                },
                                onDismiss = { dismissedFor = typed },
                            )
                        }
                    }
                }

                Column(
                    modifier =
                        Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                            padTopPx = coordinates.boundsInWindow().top
                        },
                ) {
                    DialPad(onKey = { key -> typed += key })

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

                        // A Box with combinedClickable rather than a TextButton, because a
                        // TextButton takes no onLongClick and long-press-to-clear is what every
                        // phone's dialer does. Without it, correcting a mistyped international
                        // number means tapping this thirteen times -- confirmed on a razr 50
                        // ultra, where a long press deleted exactly one digit.
                        val deleteLabel = stringResource(Res.string.keypad_delete)
                        val deleteAllLabel = stringResource(Res.string.keypad_delete_all)
                        val canDelete = typed.isNotEmpty()
                        Box(
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .combinedClickable(
                                        enabled = canDelete,
                                        onClick = { typed = typed.dropLast(1) },
                                        onClickLabel = deleteLabel,
                                        onLongClick = { typed = "" },
                                        onLongClickLabel = deleteAllLabel,
                                    )
                                    // Merged, so the glyph inside does not become a second node
                                    // and the whole control answers to the description a test
                                    // looks up.
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = deleteLabel
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "⌫",
                                color =
                                    if (canDelete) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
                                    },
                            )
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
}

/**
 * The search results, **floating over** the screen in a window of their own rather than sitting
 * in the column between the field and the pad.
 *
 * The layout this replaces reserved 132 dp between the two, always, and that gap is what the
 * user saw: a blank band under the search box that reads as a rendering bug, because on an empty
 * query nothing is ever drawn in it.
 *
 * The gap was a real fix for a real defect, though, and this has to keep the fix. The block used
 * to size itself to its contents, so every keystroke that changed the number of matches moved the
 * dial pad below it. Measured on a Pixel 8 Pro API 36 emulator against the release build: typing a
 * single `1` matched five seeded contacts and pushed the `1` key from y=702 to y=1584 — 882 px,
 * about a third of the screen — so the second digit of a number landed wherever the first
 * keystroke had just moved the pad to. A tap that missed the pad landed in the match list, and a
 * row there replaces the entire typed number with that contact's; the Call button sits in the same
 * reflowing column, so a mis-tap could dial the wrong person outright.
 *
 * A [Popup] is not in the column's layout at all, so it can be exactly as tall as it has content
 * for and still move nothing: no reserved gap, and a pad that cannot be pushed anywhere. Results
 * scroll inside it once past [MATCHES_MAX_HEIGHT].
 *
 * Not focusable, deliberately. A focusable popup takes the focus off the field it is a search for,
 * which closes the soft keyboard after the first character — the user would type one letter per
 * reopening of the keyboard. The cost is that the platform does not report outside taps, so
 * dismissal is driven by the caller (picking a row) rather than by [onDismiss], which only the
 * back gesture reaches.
 */
@Composable
private fun ContactMatchesPopup(
    width: Dp,
    maxHeight: Dp,
    matches: List<Contact>,
    contactsPermissionGranted: Boolean,
    onRequestContactsPermission: () -> Unit,
    onPick: (Contact) -> Unit,
    onAddContact: () -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = remember { BelowAnchorPositionProvider() },
        properties = PopupProperties(focusable = false),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.width(width).heightIn(max = maxHeight),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
        ) {
            MatchesContent(
                matches = matches,
                contactsPermissionGranted = contactsPermissionGranted,
                onRequestContactsPermission = onRequestContactsPermission,
                onPick = onPick,
                onAddContact = onAddContact,
            )
        }
    }
}

/**
 * Directly under the anchor, or above it when the window has no room below — which is the normal
 * case once the soft keyboard is up and the field has been pushed towards the middle of the
 * screen. Without the flip the results would render off-screen behind the keyboard exactly when
 * the user is typing into them.
 */
private class BelowAnchorPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val below = anchorBounds.bottom
        val fitsBelow = below + popupContentSize.height <= windowSize.height
        val y =
            if (fitsBelow) {
                below
            } else {
                (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
            }
        val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}

/**
 * What is inside the floating results.
 *
 * "Create new contact" is the last row rather than a button on the screen below, and that is a
 * move from where this file's history insisted it belonged. The old argument was that anything
 * placed above the pad pushes the Call button down — true of a column packed from the top, and no
 * longer true of a pad pinned to the bottom. Ending the list with it is also where the user is
 * already looking at the moment they need it: the row above says no contact matches what they
 * typed.
 */
@Composable
private fun MatchesContent(
    matches: List<Contact>,
    contactsPermissionGranted: Boolean,
    onRequestContactsPermission: () -> Unit,
    onPick: (Contact) -> Unit,
    onAddContact: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        when {
            !contactsPermissionGranted -> {
                Text(
                    text = stringResource(Res.string.keypad_contacts_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onRequestContactsPermission) {
                    Text(stringResource(Res.string.settings_grant_contacts))
                }
            }

            matches.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.keypad_no_matches),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            else -> {
                // Still capped, and not a lazy list: rendering 500 matching contacts would build
                // 500 rows for a query one more character would have narrowed. The overflow is
                // stated rather than silently dropped, so a missing contact reads as "keep typing"
                // instead of "not in your phone".
                matches.take(MAX_VISIBLE_MATCHES).forEachIndexed { index, contact ->
                    if (index > 0) HorizontalDivider()
                    ContactRow(contact = contact, onPick = { onPick(contact) })
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
        }

        HorizontalDivider()
        TextButton(
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(Res.string.keypad_add_contact))
        }
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

/** Material's disabled-content alpha, for the delete glyph when there is nothing to delete. */
private const val DISABLED_ALPHA = 0.38f

/**
 * How tall the floating results may grow before they scroll. A ceiling rather than a fixed height:
 * the popup is out of the screen's layout, so a shorter list simply draws shorter and covers less
 * of the pad -- see [ContactMatchesPopup].
 */
private val MATCHES_MAX_HEIGHT = 280.dp

/**
 * The floor under that cap. A window short enough to leave less room than this between the field
 * and the pad gets results that overlap the pad's first row, on the grounds that a 40 dp sliver
 * showing half a name is not a search result at all.
 */
private val MATCHES_MIN_HEIGHT = 140.dp

/**
 * How many results are built at all. Not about burying the pad, which the popup settles whatever
 * this is: it is a cap on work, so a one-character query does not build a row per contact in the
 * address book. Anything past it scrolls inside the popup.
 */
private const val MAX_VISIBLE_MATCHES = 5
