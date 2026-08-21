package org.carlospinan.bloqueador.app.contacts

/**
 * One dialable address-book entry, as the user saved it.
 *
 * [number] keeps the punctuation it was stored with. The rest of this package works in normalized
 * digits, but a search result the user has to recognise at a glance is the one place where
 * "+34 611 99 88 77" must not be flattened to "34611998877" — and it is also the string handed to
 * the dialler, where the '+' is meaningful.
 *
 * [starred] is the platform's own favourite flag, not a preference this app keeps: the user
 * starred these people in whatever address-book app they use, and a dialer that shows a different
 * set of favourites from every other app on the phone is showing its own opinion instead of
 * theirs. Android exposes it as `ContactsContract`'s `STARRED`; iOS has no equivalent public API,
 * so it is false there and the favourites strip simply has nothing to draw.
 */
data class Contact(
    val name: String,
    val number: String,
    val starred: Boolean = false,
)
