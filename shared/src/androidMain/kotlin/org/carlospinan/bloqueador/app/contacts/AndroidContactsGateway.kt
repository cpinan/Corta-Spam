package org.carlospinan.bloqueador.app.contacts

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.rules.PhoneNumberParser

class AndroidContactsGateway(
    private val context: Context,
) : ContactsGateway {
    override suspend fun contactNumbers(): Set<String> = queryContacts().numbers

    override suspend fun contactNames(): Map<String, String> = queryContacts().names

    /** One bulk ContentResolver scan producing both the number set and the number->name map. */
    private suspend fun queryContacts(): ContactsSnapshot =
        withContext(Dispatchers.IO) {
            val numbers = mutableSetOf<String>()
            val names = mutableMapOf<String, String>()
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
                        val normalized = PhoneNumberParser.normalizeForComparison(raw)
                        if (normalized.isEmpty()) continue
                        numbers.add(normalized)
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            names[normalized] = name
                        }
                    }
                }
            } finally {
                cursor?.close()
            }
            ContactsSnapshot(numbers, names)
        }

    override fun hasPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private data class ContactsSnapshot(
        val numbers: Set<String>,
        val names: Map<String, String>,
    )
}
