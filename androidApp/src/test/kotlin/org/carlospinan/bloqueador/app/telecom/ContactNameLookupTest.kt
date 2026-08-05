package org.carlospinan.bloqueador.app.telecom

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [ContactNameLookup] decides what name a call notification shows. Its failure modes are all
 * silent -- a denied permission, a blank number, or a contact that isn't there all return null
 * and the notification falls back to the raw number -- so the only way to know which branch ran
 * is to exercise them.
 *
 * `application = Application::class` is required, not cosmetic: Robolectric otherwise instantiates
 * the real `CortaSpamApp`, whose `onCreate()` calls `startKoin()`. The first test in the JVM
 * passes and every one after it dies with `KoinApplicationAlreadyStartedException`, since Koin's
 * global context outlives the per-test Application. Nothing here needs the DI graph.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class ContactNameLookupTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Stands in for the system contacts provider; returns whatever rows the test hands it. */
    class FakeContactsProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? {
            val name = namesByNumber[uri.lastPathSegment?.let { Uri.decode(it) }]
            val cursor = MatrixCursor(arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME))
            if (name != null) cursor.addRow(arrayOf(name))
            return cursor
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ) = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ) = 0

        companion object {
            val namesByNumber = mutableMapOf<String, String>()
        }
    }

    private fun installProvider() {
        FakeContactsProvider.namesByNumber.clear()
        Robolectric
            .buildContentProvider(FakeContactsProvider::class.java)
            .create(ContactsContract.AUTHORITY)
    }

    private fun grantContactsPermission() {
        Shadows
            .shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .grantPermissions(Manifest.permission.READ_CONTACTS)
    }

    @Test
    fun `a blank number is rejected before anything else`() {
        // Checked ahead of the permission test, so this holds even with contacts denied.
        assertNull(ContactNameLookup.displayNameFor(context, ""))
        assertNull(ContactNameLookup.displayNameFor(context, "   "))
    }

    @Test
    fun `without the contacts permission it returns null instead of querying`() {
        installProvider()
        FakeContactsProvider.namesByNumber["+34600123456"] = "Mom"

        // Permission is not granted in this test: a name exists, but must not be read.
        assertNull(ContactNameLookup.displayNameFor(context, "+34600123456"))
    }

    @Test
    fun `a matching contact resolves to its display name`() {
        installProvider()
        grantContactsPermission()
        FakeContactsProvider.namesByNumber["+34600123456"] = "Mom"

        assertEquals("Mom", ContactNameLookup.displayNameFor(context, "+34600123456"))
    }

    @Test
    fun `an unknown number resolves to null rather than an empty string`() {
        installProvider()
        grantContactsPermission()

        // The notification uses null to mean "show the raw number"; "" would render blank.
        assertNull(ContactNameLookup.displayNameFor(context, "+34699999999"))
    }

    @Test
    fun `the number is url-encoded into the lookup uri`() {
        installProvider()
        grantContactsPermission()
        // '+' is significant in a URI path; the provider only sees it if it was encoded.
        FakeContactsProvider.namesByNumber["+34 600 123 456"] = "Spaced Contact"

        assertEquals("Spaced Contact", ContactNameLookup.displayNameFor(context, "+34 600 123 456"))
    }
}
