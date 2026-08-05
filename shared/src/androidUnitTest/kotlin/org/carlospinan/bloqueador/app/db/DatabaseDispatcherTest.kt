package org.carlospinan.bloqueador.app.db

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.rules.SqlCallLogRepository
import org.carlospinan.bloqueador.app.rules.SqlRuleRepository
import org.carlospinan.bloqueador.app.settings.SqlSettingsRepository
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SQLite calls must run on the dispatcher [DriverFactory] nominates for the platform.
 *
 * `DriverFactory.databaseDispatcher` was declared, implemented on both platforms, and read by
 * nobody: every repository hardcoded `Dispatchers.Default`. That put blocking file I/O on the
 * pool sized to the CPU count — the same pool Compose uses — so a burst of queries on a
 * low-core device could starve rendering. These tests pin that the injected dispatcher is the
 * one actually used, which is the part a reader can't confirm by eye once it compiles either way.
 */
class DatabaseDispatcherTest {
    /**
     * Counts every block routed through it, then runs it inline.
     *
     * Inline rather than delegating to a real pool so `runTest` stays deterministic — and
     * notably *not* delegating to [Dispatchers.Unconfined], whose `dispatch` throws
     * `UnsupportedOperationException` by design (it never dispatches, it resumes in place).
     */
    private class CountingDispatcher : CoroutineDispatcher() {
        val dispatches = AtomicInteger(0)

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatches.incrementAndGet()
            block.run()
        }

        override fun isDispatchNeeded(context: CoroutineContext): Boolean = true
    }

    @Test
    fun ruleRepositoryWritesAndReadsGoThroughTheInjectedDispatcher() =
        runTest {
            val dispatcher = CountingDispatcher()
            val repo = SqlRuleRepository(createTestDatabase(), dispatcher)

            repo.addBlockedNumber("+34600123456", null)
            repo.blockedNumberEntries()

            assertTrue(dispatcher.dispatches.get() >= 2, "both the write and the read must be dispatched")
        }

    @Test
    fun reactiveFlowsAlsoGoThroughTheInjectedDispatcher() =
        runTest {
            val dispatcher = CountingDispatcher()
            val repo = SqlRuleRepository(createTestDatabase(), dispatcher)

            repo.blockedNumbers().first()

            assertTrue(dispatcher.dispatches.get() > 0, "mapToList must use the injected dispatcher, not Dispatchers.Default")
        }

    @Test
    fun callLogRepositoryUsesTheInjectedDispatcher() =
        runTest {
            val dispatcher = CountingDispatcher()
            val repo = SqlCallLogRepository(createTestDatabase(), dispatcher)

            repo.blockedStats()

            assertTrue(dispatcher.dispatches.get() > 0)
        }

    @Test
    fun settingsWritesUseTheInjectedDispatcher() =
        runTest {
            val dispatcher = CountingDispatcher()
            val repo = SqlSettingsRepository(createTestDatabase(), dispatcher)

            repo.setBlockingEnabled(false)

            assertTrue(dispatcher.dispatches.get() > 0)
        }

    @Test
    fun noRepositoryFallsBackToTheDefaultDispatcher() =
        runTest {
            // Guards the specific regression: swapping any withContext(dispatcher) back to
            // withContext(Dispatchers.Default) leaves the counter at zero for that call.
            val dispatcher = CountingDispatcher()
            val db = createTestDatabase()
            val rules = SqlRuleRepository(db, dispatcher)

            rules.addPatternRule("+34900*", null)
            rules.addCountryRule("34", "Spain")
            rules.addScheduleRule("night", 1320, 420)
            rules.addActionRule("repeats", 3, 5, null)
            val before = dispatcher.dispatches.get()

            rules.enabledPatterns()
            rules.allPatterns()
            rules.enabledCountryRules()
            rules.enabledScheduleRules()
            rules.enabledActionRules()

            assertTrue(
                dispatcher.dispatches.get() >= before + 5,
                "every snapshot read must be dispatched; got ${dispatcher.dispatches.get() - before} for 5 reads",
            )
        }
}
