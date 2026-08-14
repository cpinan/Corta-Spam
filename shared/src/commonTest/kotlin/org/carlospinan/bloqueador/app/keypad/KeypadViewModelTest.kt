package org.carlospinan.bloqueador.app.keypad

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KeypadViewModelTest {
    private class FakeContactsGateway(
        var granted: Boolean = false,
        var book: List<Contact> = emptyList(),
    ) : ContactsGateway {
        override suspend fun contactNumbers(): Set<String> = emptySet()

        override suspend fun contactNames(): Map<String, String> = emptyMap()

        override suspend fun contacts(): List<Contact> = book

        override fun hasPermission(): Boolean = granted
    }

    @Test
    fun `contacts are not read without the permission`() =
        runTest {
            val gateway = FakeContactsGateway(granted = false, book = listOf(Contact("Ana", "600111222")))

            val state = KeypadViewModel(gateway).state.first()

            assertTrue(state.contacts.isEmpty())
            assertFalse(state.contactsPermissionGranted)
        }

    /**
     * The permission is granted from this screen's own button, so init has already run and
     * skipped the load by the time the grant lands. Without the resume refresh, search stayed
     * empty until the process was restarted.
     */
    @Test
    fun `RefreshContacts picks up a grant that landed after construction`() =
        runTest {
            val gateway = FakeContactsGateway(granted = false)
            val viewModel = KeypadViewModel(gateway)
            assertTrue(
                viewModel.state
                    .first()
                    .contacts
                    .isEmpty(),
            )

            gateway.granted = true
            gateway.book = listOf(Contact("Ana", "600111222"))
            viewModel.onIntent(KeypadIntent.RefreshContacts)

            val state = viewModel.state.first { it.contacts.isNotEmpty() }
            assertEquals("Ana", state.contacts.single().name)
            assertTrue(state.contactsPermissionGranted)
        }

    /** Revoked from system settings while the app is alive: the list has to go with the grant. */
    @Test
    fun `a revoked permission clears the contacts already loaded`() =
        runTest {
            val gateway = FakeContactsGateway(granted = true, book = listOf(Contact("Ana", "600111222")))
            val viewModel = KeypadViewModel(gateway)
            val loaded = viewModel.state.first { it.contacts.isNotEmpty() }
            assertEquals("Ana", loaded.contacts.single().name)

            gateway.granted = false
            viewModel.onIntent(KeypadIntent.RefreshContacts)

            val state = viewModel.state.first()
            assertTrue(state.contacts.isEmpty())
            assertFalse(state.contactsPermissionGranted)
        }
}
