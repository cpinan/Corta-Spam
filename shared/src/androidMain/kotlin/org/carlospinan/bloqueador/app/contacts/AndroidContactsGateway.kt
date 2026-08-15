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

    /**
     * One bulk ContentResolver scan. The rows are handed straight to [buildContactsSnapshot],
     * which owns every decision about number forms and is unit-tested without a provider.
     */
    private suspend fun queryContacts(): ContactsSnapshot =
        withContext(Dispatchers.IO) {
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
                val rows = cursor?.let { readRows(it) } ?: emptyList()
                buildContactsSnapshot(rows.asSequence())
            } finally {
                cursor?.close()
            }
        }

    private fun readRows(cursor: Cursor): List<ContactRow> {
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val rows = mutableListOf<ContactRow>()
        while (cursor.moveToNext()) {
            rows.add(
                ContactRow(
                    number = cursor.getString(numberIndex),
                    displayName = cursor.getString(nameIndex),
                ),
            )
        }
        return rows
    }

    override fun hasPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private companion object {
        /** Elapsed-realtime, so it isn't skewed by a wall-clock adjustment. */
        const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
    }
}
