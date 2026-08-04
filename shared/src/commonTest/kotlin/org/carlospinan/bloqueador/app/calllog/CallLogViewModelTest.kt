package org.carlospinan.bloqueador.app.calllog

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.testing.FakeCallLogRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CallLogViewModelTest {
    private class FakeContactsGateway : ContactsGateway {
        override suspend fun contactNumbers(): Set<String> = emptySet()

        override suspend fun contactNames(): Map<String, String> = emptyMap()

        override fun hasPermission(): Boolean = false
    }

    @Test
    fun `entries flow reflects repository data`() =
        runTest {
            val entry =
                CallLogEntryData(
                    id = 1L,
                    number = "+34611223344",
                    timestamp = 1234567890L,
                    action = "BLOCKED",
                    ruleType = "MANUAL",
                    ruleId = 1L,
                    ruleDetail = "Manually blocked",
                )
            val flow = MutableStateFlow(listOf(entry))
            val repo = FakeCallLogRepository(entriesFlow = flow)
            val vm = CallLogViewModel(callLogRepository = repo, contactsGateway = FakeContactsGateway())

            val entries = vm.state.first { it.entries.isNotEmpty() }.entries
            assertEquals(1, entries.size)
            assertEquals("+34611223344", entries[0].number)
            assertEquals("BLOCKED", entries[0].action)
            assertEquals("Manually blocked", entries[0].ruleDetail)
        }

    @Test
    fun `entries starts empty`() =
        runTest {
            val repo = FakeCallLogRepository()
            val vm = CallLogViewModel(callLogRepository = repo, contactsGateway = FakeContactsGateway())

            val entries = vm.state.first().entries
            assertTrue(entries.isEmpty())
        }

    @Test
    fun `SetFilter review keeps only REVIEW-tagged entries`() =
        runTest {
            val reviewEntry =
                CallLogEntryData(
                    id = 1L,
                    number = "+34611223344",
                    timestamp = 1L,
                    action = "ALLOWED",
                    ruleType = "REVIEW",
                    ruleId = null,
                    ruleDetail = "Pending review",
                )
            val manualEntry =
                CallLogEntryData(
                    id = 2L,
                    number = "+34699887766",
                    timestamp = 2L,
                    action = "BLOCKED",
                    ruleType = "MANUAL",
                    ruleId = 5L,
                    ruleDetail = "Manually blocked",
                )
            val flow = MutableStateFlow(listOf(reviewEntry, manualEntry))
            val repo = FakeCallLogRepository(entriesFlow = flow)
            val vm = CallLogViewModel(callLogRepository = repo, contactsGateway = FakeContactsGateway())

            vm.onIntent(CallLogIntent.SetFilter("review"))

            val entries = vm.state.first { it.entries.size == 1 }.entries
            assertEquals("REVIEW", entries[0].ruleType)
        }

    @Test
    fun `SetFilter all keeps every entry`() =
        runTest {
            val entryA =
                CallLogEntryData(
                    id = 1L,
                    number = "+34611223344",
                    timestamp = 1L,
                    action = "ALLOWED",
                    ruleType = "REVIEW",
                    ruleId = null,
                    ruleDetail = "Pending review",
                )
            val entryB =
                CallLogEntryData(
                    id = 2L,
                    number = "+34699887766",
                    timestamp = 2L,
                    action = "BLOCKED",
                    ruleType = "MANUAL",
                    ruleId = 5L,
                    ruleDetail = "Manually blocked",
                )
            val flow = MutableStateFlow(listOf(entryA, entryB))
            val repo = FakeCallLogRepository(entriesFlow = flow)
            val vm = CallLogViewModel(callLogRepository = repo, contactsGateway = FakeContactsGateway())

            vm.onIntent(CallLogIntent.SetFilter("all"))

            val entries = vm.state.first { it.entries.size == 2 }.entries
            assertEquals(2, entries.size)
        }
}
