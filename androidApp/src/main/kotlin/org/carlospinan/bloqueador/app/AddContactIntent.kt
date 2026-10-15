package org.carlospinan.bloqueador.app

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract

private const val GOOGLE_ACCOUNT_TYPE = "com.google"

/**
 * The (name, type) of the Google account that already holds most of the address book, or null when none does.
 *
 * Picked from the contacts themselves rather than from `AccountManager`, which needs
 * `GET_ACCOUNTS`: this app already holds `READ_CONTACTS` and asking for another permission to
 * choose a default account would be a poor trade. Most rows wins, so a stray second account with
 * three contacts does not capture new ones.
 */
internal fun pickSyncAccount(rows: List<Pair<String, String>>): Pair<String, String>? =
    rows
        .filter { (name, type) -> type == GOOGLE_ACCOUNT_TYPE && name.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

private fun Context.readContactAccounts(): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    val projection = arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE)
    runCatching {
        contentResolver
            .query(
                ContactsContract.RawContacts.CONTENT_URI,
                projection,
                "${ContactsContract.RawContacts.DELETED} = 0",
                null,
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    val type = cursor.getString(1)
                    if (name != null && type != null) rows.add(name to type)
                }
            }
    } // No READ_CONTACTS (or no provider): fall back to whatever the editor defaults to.
    return rows
}

/**
 * Intent for the platform's new-contact editor with [number] filled in.
 *
 * The editor saves to whichever account it defaults to, and on many phones that is the
 * device-only account -- a contact saved there never syncs, so it is gone on the next phone and
 * invisible to every other app signed in with the user's Google account. Preselecting the
 * account the address book already lives in keeps new contacts with the rest.
 */
internal fun Context.addContactIntent(number: String): Intent =
    Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        putExtra(ContactsContract.Intents.Insert.PHONE, number.trim())
        pickSyncAccount(readContactAccounts())?.let { (name, type) ->
            putExtra(ContactsContract.Intents.Insert.EXTRA_ACCOUNT, Account(name, type))
        }
    }
