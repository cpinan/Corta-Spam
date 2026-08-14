package org.carlospinan.bloqueador.app.contacts

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.SystemClock
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.rules.PhoneNumberParser
import java.text.Collator

class AndroidContactsGateway(
    private val context: Context,
) : ContactsGateway {
    private val cacheMutex = Mutex()
    private var cached: ContactsSnapshot? = null
    private var cachedAtMillis = 0L

    override suspend fun contactNumbers(): Set<String> = snapshot().numbers

    override suspend fun contactNames(): Map<String, String> = snapshot().names

    override suspend fun contacts(): List<Contact> = snapshot().contacts

    /**
     * Cached for [CACHE_TTL_MILLIS], because this runs on the ringing-call path.
     *
     * Every incoming call asks the allowlist "is this one of my contacts?", which used to mean a
     * full ContentResolver scan of every phone number on the device while the phone was ringing
     * — on a 5000-contact address book that is not free, and it happened again immediately for
     * the name lookup. A short TTL keeps a contact added minutes ago from being missed while
     * removing the per-call cost of a list that changes very rarely.
     */
    private suspend fun snapshot(): ContactsSnapshot =
        cacheMutex.withLock {
            val now = SystemClock.elapsedRealtime()
            val current = cached
            if (current != null && now - cachedAtMillis < CACHE_TTL_MILLIS) return@withLock current
            queryContacts().also {
                cached = it
                cachedAtMillis = now
            }
        }

    /** One bulk ContentResolver scan producing both the number set and the number->name map. */
    private suspend fun queryContacts(): ContactsSnapshot =
        withContext(Dispatchers.IO) {
            val numbers = mutableSetOf<String>()
            val names = mutableMapOf<String, String>()
            val contacts = mutableListOf<Contact>()
            // One row per phone entry, and the same number arrives twice whenever a card is
            // synced from two accounts. Deduplicated on (name, digits) rather than on the number
            // alone: a household landline saved under two people is two search results, and
            // dropping one of them hides a contact the user went looking for.
            val seen = mutableSetOf<Pair<String, String>>()
            val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection =
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                )

            var cursor: Cursor? = null
            try {
                cursor =
                    context.contentResolver.query(
                        uri,
                        projection,
                        null,
                        null,
                        null,
                    )
                cursor?.let {
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    while (it.moveToNext()) {
                        val raw = it.getString(numberIndex)
                        if (raw.isNullOrBlank()) continue
                        // Every form this contact may be recognised by, not just its digits: a
                        // card saved "611 99 88 77" has to be found when "+34611998877" calls.
                        val keys = PhoneNumberParser.comparisonKeys(raw)
                        if (keys.isEmpty()) continue
                        numbers.add(PhoneNumberParser.normalizeForComparison(raw))
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            // putIfAbsent semantics: the first contact to claim a key keeps it, so
                            // a later card sharing a national number cannot rename an exact match.
                            keys.forEach { key -> names.getOrPut(key) { name } }
                            if (seen.add(name to PhoneNumberParser.normalizeForComparison(raw))) {
                                contacts.add(Contact(name = name, number = raw.trim()))
                            }
                        }
                    }
                }
            } finally {
                cursor?.close()
            }
            // Sorted here rather than by the cursor: ordering by DISPLAY_NAME in SQLite is
            // bytewise, which files every accented name after Z -- and "Ángela" belongs with the
            // A's in all four locales this app ships in. Collator is the locale-aware comparison.
            val byName = compareBy<Contact, String>(Collator.getInstance()) { it.name }
            ContactsSnapshot(numbers, names, contacts.sortedWith(byName))
        }

    override fun hasPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private data class ContactsSnapshot(
        val numbers: Set<String>,
        val names: Map<String, String>,
        val contacts: List<Contact>,
    )

    private companion object {
        /** Elapsed-realtime, so it isn't skewed by a wall-clock adjustment. */
        const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
    }
}
