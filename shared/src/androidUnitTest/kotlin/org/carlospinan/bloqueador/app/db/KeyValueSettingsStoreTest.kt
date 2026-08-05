package org.carlospinan.bloqueador.app.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [KeyValueSettingsStore] is the single storage primitive behind the settings, spam-provider
 * and auto-responder repositories, so its fallback behaviour is what every one of those
 * repositories' defaults actually rests on.
 */
class KeyValueSettingsStoreTest {
    private fun store() = KeyValueSettingsStore(createTestDatabase(), Dispatchers.Unconfined)

    @Test
    fun missingKeyReturnsDefault() {
        val store = store()

        assertEquals(true, store.readBool("absent", true))
        assertEquals(false, store.readBool("absent", false))
        assertEquals("fallback", store.readString("absent", "fallback"))
        assertEquals(42, store.readInt("absent", 42))
    }

    @Test
    fun boolRoundTrip() =
        runTest {
            val store = store()

            store.writeBool("flag", false)
            assertEquals(false, store.readBool("flag", true))

            store.writeBool("flag", true)
            assertEquals(true, store.readBool("flag", false))
        }

    @Test
    fun intRoundTrip() =
        runTest {
            val store = store()

            store.writeInt("count", 7)
            assertEquals(7, store.readInt("count", 0))
        }

    @Test
    fun stringRoundTrip() =
        runTest {
            val store = store()

            store.write("label", "quiet hours")
            assertEquals("quiet hours", store.readString("label", ""))
        }

    @Test
    fun writeOverwritesPreviousValue() =
        runTest {
            val store = store()

            store.write("key", "first")
            store.write("key", "second")

            assertEquals("second", store.readString("key", ""))
        }

    @Test
    fun unparseableBoolFallsBackToDefault() =
        runTest {
            val store = store()

            store.write("flag", "yes")

            // toBooleanStrictOrNull() only accepts exactly "true"/"false", so anything
            // else reads as the caller's default rather than throwing or guessing.
            assertEquals(true, store.readBool("flag", true))
            assertEquals(false, store.readBool("flag", false))
        }

    @Test
    fun capitalisedBoolIsNotAccepted() =
        runTest {
            val store = store()

            store.write("flag", "TRUE")

            // Strict parsing is case-sensitive — "TRUE" is not "true".
            assertEquals(false, store.readBool("flag", false))
        }

    @Test
    fun unparseableIntFallsBackToDefault() =
        runTest {
            val store = store()

            store.write("count", "many")

            assertEquals(3, store.readInt("count", 3))
        }

    @Test
    fun blankStringFallsBackToDefault() =
        runTest {
            val store = store()

            store.write("label", "")

            // A stored empty string is indistinguishable from "unset" on read. Callers that
            // need to persist an empty value cannot do it through this store.
            assertEquals("fallback", store.readString("label", "fallback"))
        }
}
