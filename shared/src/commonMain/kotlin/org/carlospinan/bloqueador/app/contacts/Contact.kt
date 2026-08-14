package org.carlospinan.bloqueador.app.contacts

/**
 * One dialable address-book entry, as the user saved it.
 *
 * [number] keeps the punctuation it was stored with. The rest of this package works in normalized
 * digits, but a search result the user has to recognise at a glance is the one place where
 * "+34 611 99 88 77" must not be flattened to "34611998877" — and it is also the string handed to
 * the dialler, where the '+' is meaningful.
 */
data class Contact(
    val name: String,
    val number: String,
)
