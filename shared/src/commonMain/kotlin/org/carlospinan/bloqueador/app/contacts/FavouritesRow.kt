package org.carlospinan.bloqueador.app.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.agenda_favourites
import cortaspam.shared.generated.resources.keypad_contact_row
import org.jetbrains.compose.resources.stringResource

/**
 * The phone's starred contacts, across the top, the way every dialer shows favourites.
 *
 * Shared by the Agenda tab and the keypad rather than written twice: they show the same people
 * from the same platform flag, and two copies would drift into two different ideas of what a
 * favourite looks like -- which is the whole reason the dial pad itself is one component.
 *
 * On the keypad it also earns its place by filling space that was otherwise blank. A 1080x2640
 * phone is about 1000 dp tall; twelve keys cannot fill that at any size that still reads as a dial
 * key, so the choice is a band of nothing above the number field or a band of the people most
 * likely to be called from it.
 */
@Composable
fun FavouritesRow(
    contacts: List<Contact>,
    onPick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    // Passed in rather than fixed, because the keypad draws this same strip of recent callers
    // when nothing is starred, and a row of recents headed "Favourites" would be a lie about
    // where those people came from.
    title: String = stringResource(Res.string.agenda_favourites),
) {
    if (contacts.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            contacts.forEach { contact ->
                val label = stringResource(Res.string.keypad_contact_row, contact.name, contact.number)
                Column(
                    modifier =
                        Modifier
                            .width(72.dp)
                            .clickable(onClick = { onPick(contact) })
                            .semantics(mergeDescendants = true) { contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ContactAvatar(contact.name)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The contact's initial in a circle. A placeholder rather than the contact's photo, deliberately:
 * reading photo blobs means a second provider query per contact, on screens that are scrolled, to
 * decorate a list already identified by name and number.
 */
@Composable
fun ContactAvatar(name: String) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // The first letter *or digit*, not the first character: on the keypad this strip also
            // shows recent callers who are not in the address book, whose "name" is their number
            // -- and the first character of "+34902100200" is a plus sign, which identifies
            // nobody. Every international number would wear the same badge.
            text = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
