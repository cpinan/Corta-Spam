package org.carlospinan.bloqueador.app.calllog

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.rules.BlockReason
import org.carlospinan.bloqueador.app.rules.BlockReasonCodec
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The call log must never render a raw database value to the user.
 *
 * `CallLogEntry.action` stores the strings "BLOCKED" and "ALLOWED". A localized label for each
 * already existed, but two call sites rendered `entry.action` directly instead — so the status
 * column read "BLOCKED" in English in all four shipped locales. It was invisible in review
 * because the default locale's label happens to be the same word as the stored enum; it only
 * showed up when the app was screenshotted in Spanish for the store listing.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CallLogLocalizationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun entry(
        action: String,
        ruleType: String?,
        reason: BlockReason?,
    ) = CallLogEntryData(
        id = 1,
        number = "+34900123456",
        timestamp = 1_754_000_000_000L,
        action = action,
        ruleType = ruleType,
        ruleId = null,
        ruleDetail = reason?.let { BlockReasonCodec.encode(it) },
    )

    @Test
    fun aBlockedRowShowsTheLocalizedLabelAndNotTheStoredEnum() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(entry("BLOCKED", "MANUAL", BlockReason.ManuallyBlocked)),
                onBack = {},
            )
        }

        // The default locale's label is "Blocked" -- title case. The stored value is the
        // screaming-snake "BLOCKED", so an exact match on that means the raw column leaked.
        // A positive assertion, deliberately. The first version of this test asserted that
        // "BLOCKED" does *not* exist, and passed even with the bug reintroduced -- an
        // assertDoesNotExist can pass because the thing is absent for an unrelated reason.
        // Asserting the localized label is *present* can only pass if it is actually rendered.
        composeTestRule.onNodeWithText("+34900123456").assertExists()
        composeTestRule.onNodeWithText("Blocked call", substring = true).assertExists()
    }

    @Test
    fun anAllowedRowShowsTheLocalizedLabelAndNotTheStoredEnum() {
        composeTestRule.setContent {
            CallLogScreen(
                entries = listOf(entry("ALLOWED", null, null)),
                onBack = {},
            )
        }

        composeTestRule.onNodeWithText("+34900123456").assertExists()
        composeTestRule.onNodeWithText("Allowed call", substring = true).assertExists()
    }
}
