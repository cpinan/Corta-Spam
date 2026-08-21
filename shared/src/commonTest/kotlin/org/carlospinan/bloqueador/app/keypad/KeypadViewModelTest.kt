package org.carlospinan.bloqueador.app.keypad

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.testing.FakeCallLogRepository
import org.carlospinan.bloqueador.app.testing.FakeSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KeypadViewModelTest {
    private class FakeContactsGateway(
        var granted: Boolean = false,
        var book: List<Contact> = emptyList(),
        var names: Map<String, String> = emptyMap(),
    ) : ContactsGateway {
        override suspend fun contactNumbers(): Set<String> = emptySet()

        override suspend fun contactNames(): Map<String, String> = names

        override suspend fun contacts(): List<Contact> = book

        override fun hasPermission(): Boolean = granted
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `contacts are not read without the permission`() =
        runTest {
            val gateway = FakeContactsGateway(granted = false, book = listOf(Contact("Ana", "600111222")))

            val state = KeypadViewModel(gateway, FakeCallLogRepository(), FakeSettingsRepository()).state.first()

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
            val viewModel = KeypadViewModel(gateway, FakeCallLogRepository(), FakeSettingsRepository())
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
            val viewModel = KeypadViewModel(gateway, FakeCallLogRepository(), FakeSettingsRepository())
            val loaded = viewModel.state.first { it.contacts.isNotEmpty() }
            assertEquals("Ana", loaded.contacts.single().name)

            gateway.granted = false
            viewModel.onIntent(KeypadIntent.RefreshContacts)

            val state = viewModel.state.first()
            assertTrue(state.contacts.isEmpty())
            assertFalse(state.contactsPermissionGranted)
        }

    /**
     * The strip's labels come from the address book, and the address book needs a permission the
     * user may grant from this very screen. Resolving them only when the call log emits left the
     * strip showing bare numbers until somebody rang -- the third time this project has shipped
     * work done once at construction and a permission that arrives afterwards.
     */
    @Test
    fun `RefreshContacts relabels the recent callers with names`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val log = FakeCallLogRepository()
            log.entriesFlow.value =
                listOf(
                    CallLogEntryData(
                        id = 1,
                        number = "+34611998877",
                        timestamp = 1,
                        action = "ALLOWED",
                        ruleType = null,
                        ruleId = null,
                        ruleDetail = null,
                    ),
                )
            val gateway = FakeContactsGateway(granted = false)
            val viewModel = KeypadViewModel(gateway, log, FakeSettingsRepository())
            advanceUntilIdle()
            assertEquals(
                "+34611998877",
                viewModel.state.value.recent
                    .single()
                    .name,
            )

            gateway.granted = true
            gateway.names = mapOf("34611998877" to "Ana Torres")
            viewModel.onIntent(KeypadIntent.RefreshContacts)
            advanceUntilIdle()

            assertEquals(
                "Ana Torres",
                viewModel.state.value.recent
                    .single()
                    .name,
            )
        }

    /**
     * Dropped from the state rather than hidden by the screen: state the UI is told to ignore is
     * state that leaks the next time something else reads it.
     */
    @Test
    fun `switching the setting off empties the recent callers`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val log = FakeCallLogRepository()
            log.entriesFlow.value =
                listOf(
                    CallLogEntryData(
                        id = 1,
                        number = "600111222",
                        timestamp = 1,
                        action = "ALLOWED",
                        ruleType = null,
                        ruleId = null,
                        ruleDetail = null,
                    ),
                )
            val settings = FakeSettingsRepository()
            val viewModel = KeypadViewModel(FakeContactsGateway(granted = true), log, settings)
            advanceUntilIdle()
            assertEquals(1, viewModel.state.value.recent.size)

            settings.showRecentCallersOnKeypad.value = false
            advanceUntilIdle()

            assertTrue(
                viewModel.state.value.recent
                    .isEmpty(),
            )
            assertFalse(viewModel.state.value.showRecentCallers)
        }
}
