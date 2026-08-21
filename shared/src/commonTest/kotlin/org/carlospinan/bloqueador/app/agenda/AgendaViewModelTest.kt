package org.carlospinan.bloqueador.app.agenda

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
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.testing.FakeRuleRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {
    private class FakeContactsGateway(
        var granted: Boolean = true,
        var book: List<Contact> = emptyList(),
    ) : ContactsGateway {
        var refreshCalls = 0

        override suspend fun contactNumbers(): Set<String> = emptySet()

        override suspend fun contactNames(): Map<String, String> = emptyMap()

        override suspend fun contacts(): List<Contact> = book

        override suspend fun refresh() {
            refreshCalls++
        }

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

            val state = AgendaViewModel(gateway, FakeRuleRepository()).state.first()

            assertTrue(state.contacts.isEmpty())
            assertFalse(state.contactsPermissionGranted)
        }

    @Test
    fun `the address book is loaded when the permission is held`() =
        runTest {
            val gateway = FakeContactsGateway(book = listOf(Contact("Ana", "600111222")))

            val state = AgendaViewModel(gateway, FakeRuleRepository()).state.first { it.contacts.isNotEmpty() }

            assertEquals(listOf(Contact("Ana", "600111222")), state.contacts)
            assertTrue(state.contactsPermissionGranted)
        }

    /**
     * The gateway caches the address book for five minutes, because the allowlist reads it while
     * the phone is ringing. A pull gesture that served that cache would answer a deliberate "look
     * again" with the same stale list -- silently, and convincingly.
     */
    @Test
    fun `Refresh drops the gateway cache before reading`() =
        runTest {
            val gateway = FakeContactsGateway()
            val viewModel = AgendaViewModel(gateway, FakeRuleRepository())
            viewModel.state.first()

            viewModel.onIntent(AgendaIntent.Refresh)
            viewModel.state.first { !it.refreshing }

            assertEquals(1, gateway.refreshCalls)
        }

    /** The construction-time load is not a refresh; nothing is cached yet to be wrong. */
    @Test
    fun `the initial load does not drop the cache`() =
        runTest {
            val gateway = FakeContactsGateway()

            AgendaViewModel(gateway, FakeRuleRepository()).state.first()

            assertEquals(0, gateway.refreshCalls)
        }

    @Test
    fun `the permission granted after construction is picked up by Refresh`() =
        runTest {
            val gateway = FakeContactsGateway(granted = false)
            val viewModel = AgendaViewModel(gateway, FakeRuleRepository())
            assertTrue(
                viewModel.state
                    .first()
                    .contacts
                    .isEmpty(),
            )

            gateway.granted = true
            gateway.book = listOf(Contact("Ana", "600111222"))
            viewModel.onIntent(AgendaIntent.Refresh)

            assertEquals(
                1,
                viewModel.state
                    .first { it.contacts.isNotEmpty() }
                    .contacts.size,
            )
        }

    /**
     * A list left over from before a revocation is address-book data still on screen after the
     * user said no.
     */
    @Test
    fun `a revoked permission clears the contacts already loaded`() =
        runTest {
            val gateway = FakeContactsGateway(book = listOf(Contact("Ana", "600111222")))
            val viewModel = AgendaViewModel(gateway, FakeRuleRepository())
            viewModel.state.first { it.contacts.isNotEmpty() }

            gateway.granted = false
            viewModel.onIntent(AgendaIntent.Refresh)

            val state = viewModel.state.first { !it.contactsPermissionGranted }
            assertTrue(state.contacts.isEmpty())
        }

    @Test
    fun `the rule lists reach the state so a row can show what applies to it`() =
        runTest {
            val rules = FakeRuleRepository()
            rules.blockedNumbersFlow.value =
                listOf(BlockedNumberEntry(id = 7, number = "600111222", label = null, createdAt = 0))

            val state = AgendaViewModel(FakeContactsGateway(), rules).state.first { it.blockedNumbers.isNotEmpty() }

            assertEquals(7, state.blockedNumbers.single().id)
        }

    @Test
    fun `blocking a contact writes a block rule`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val rules = FakeRuleRepository()
            val viewModel = AgendaViewModel(FakeContactsGateway(), rules)

            viewModel.onIntent(AgendaIntent.BlockNumber("600111222"))
            advanceUntilIdle()

            assertEquals(1, rules.addBlockedNumberCalls.size)
            assertEquals("600111222" to null, rules.addBlockedNumberCalls[0])
        }
}
