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
    override suspend fun contactNumbers(): Set<String> =
        withContext(Dispatchers.IO) {
            val numbers = mutableSetOf<String>()
            val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

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
                    while (it.moveToNext()) {
                        val raw = it.getString(numberIndex)
                        if (!raw.isNullOrBlank()) {
                            val normalized = PhoneNumberParser.normalizeForComparison(raw)
                            if (normalized.isNotEmpty()) {
                                numbers.add(normalized)
                            }
                        }
                    }
                }
            } finally {
                cursor?.close()
            }
            numbers
        }

    override fun hasPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}
