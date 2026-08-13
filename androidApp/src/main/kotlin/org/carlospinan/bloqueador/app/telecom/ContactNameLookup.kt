package org.carlospinan.bloqueador.app.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Best-effort contact display-name lookup for notification titles. Separate from
 * [org.carlospinan.bloqueador.app.contacts.ContactsGateway] (which only needs a normalized
 * number set for allowlist matching, not names) -- this is Android notification-UI polish, not
 * rule-evaluation logic, so it doesn't need a commonMain interface/iOS actual.
 */
object ContactNameLookup {
    /**
     * Whether a lookup can return anything at all.
     *
     * Callers that treat a null name as "this caller is a stranger" have to ask this first: with
     * the permission denied, every number in the world looks like a stranger.
     */
    fun hasContactsAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun displayNameFor(
        context: Context,
        number: String,
    ): String? {
        if (number.isBlank()) return null
        if (!hasContactsAccess(context)) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            } else {
                null
            }
        }
    }
}
