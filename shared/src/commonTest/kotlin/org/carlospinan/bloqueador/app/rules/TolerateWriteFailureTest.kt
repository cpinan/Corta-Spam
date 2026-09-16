package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TolerateWriteFailureTest {
    @Test
    fun aSuccessfulWriteReturnsItsValue() =
        runTest {
            var failureSeen = false
            val result = tolerateWriteFailure(onFailure = { failureSeen = true }) { 42L }
            assertEquals(42L, result)
            assertTrue(!failureSeen)
        }

    @Test
    fun aFailingWriteReturnsNullAndReportsTheFailure() =
        runTest {
            var reported: Exception? = null
            val error = IllegalStateException("db is gone")
            val result =
                tolerateWriteFailure(onFailure = { reported = it }) {
                    throw error
                }
            assertNull(result)
            assertEquals(error, reported)
        }

    /** The one exception this must never swallow: it would mark a cancelled coroutine as failed. */
    @Test
    fun cancellationIsRethrownRatherThanTreatedAsAFailure() =
        runTest {
            var failureSeen = false
            assertFailsWith<CancellationException> {
                tolerateWriteFailure(onFailure = { failureSeen = true }) {
                    throw CancellationException("cancelled")
                }
            }
            assertTrue(!failureSeen)
        }
}
