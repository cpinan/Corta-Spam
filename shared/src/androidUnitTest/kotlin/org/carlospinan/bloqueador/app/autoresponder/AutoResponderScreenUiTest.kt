package org.carlospinan.bloqueador.app.autoresponder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

/** The first switch on the screen; the second is the recording one below it. */
private const val AUTO_RESPONDER_SWITCH = 0

private const val CONSENTING_SCRIPT = "Hello. This call may be recorded. Please leave a message."

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AutoResponderScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        config: AutoResponderConfig,
        micPermissionGranted: Boolean = true,
        onRequestMicPermission: () -> Unit = {},
        onSetEnabled: (Boolean) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AutoResponderScreen(
                state = AutoResponderUiState(config = config),
                onSetEnabled = onSetEnabled,
                onSetScript = {},
                onSetRecordingEnabled = {},
                onPickAudio = {},
                onClearAudio = {},
                onBack = {},
                micPermissionGranted = micPermissionGranted,
                onRequestMicPermission = onRequestMicPermission,
            )
        }
    }

    /**
     * The limits are the feature. A phone whose telephony stack refuses the microphone records
     * nothing, and without this card that is indistinguishable from an app that is broken —
     * which is how it was reported.
     */
    @Test
    fun `the screen explains that recording captures the microphone and may capture nothing`() {
        setScreen(AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT))

        // The card is at the bottom of a scrolling column, below the fold on a phone-sized
        // window: scroll to it, then assert it is really on screen rather than merely present.
        composeTestRule.onNodeWithText("How this works").performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Recording captures the microphone", substring = true)
            .assertExists()
    }

    @Test
    fun `the screen explains that only blocked calls are answered`() {
        setScreen(AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT))

        composeTestRule
            .onNodeWithText("Only blocked calls are answered", substring = true)
            .assertExists()
    }

    @Test
    fun `turning recording on while the auto-responder is off says why nothing will be recorded`() {
        setScreen(
            AutoResponderConfig(enabled = false, script = CONSENTING_SCRIPT, recordingEnabled = true),
        )

        composeTestRule
            .onNodeWithText("the auto-responder above is off", substring = true)
            .assertExists()
    }

    @Test
    fun `a greeting without the consent sentence blocks recording and says so`() {
        setScreen(
            AutoResponderConfig(enabled = true, script = "No consent here.", recordingEnabled = true),
        )

        composeTestRule
            .onNodeWithText("until the greeting above is valid", substring = true)
            .assertExists()
    }

    @Test
    fun `a missing microphone permission offers the grant button`() {
        var requested = 0
        setScreen(
            config = AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT, recordingEnabled = true),
            micPermissionGranted = false,
            onRequestMicPermission = { requested++ },
        )

        composeTestRule.onNodeWithText("Grant microphone access").performScrollTo().performClick()

        assertEquals(1, requested)
    }

    /** Nothing to fix, so nothing to warn about — the card must not nag a correct setup. */
    @Test
    fun `a fully configured recorder shows no blocker`() {
        setScreen(
            config = AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT, recordingEnabled = true),
            micPermissionGranted = true,
        )

        composeTestRule
            .onNodeWithText("the auto-responder above is off", substring = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("Grant microphone access").assertDoesNotExist()
    }

    /**
     * The microphone warning is gated on recording being switched on: warning about a permission
     * the user has not asked to use is the nag the contacts button used to be.
     */
    @Test
    fun `no microphone warning while recording is switched off`() {
        setScreen(
            config = AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT, recordingEnabled = false),
            micPermissionGranted = false,
        )

        composeTestRule.onNodeWithText("Grant microphone access").assertDoesNotExist()
    }

    /**
     * The reported surprise: the app answering a call by itself. Only an enabled auto-responder
     * does that, so enabling it is confirmed rather than toggled — and nothing is persisted until
     * the user says yes, or the switch would have already done the thing it is warning about.
     */
    @Test
    fun `turning the auto-responder on asks before it is switched on`() {
        val enabledCalls = mutableListOf<Boolean>()
        setScreen(AutoResponderConfig(enabled = false), onSetEnabled = { enabledCalls += it })

        composeTestRule.onAllNodes(isToggleable())[AUTO_RESPONDER_SWITCH].performClick()

        composeTestRule
            .onNodeWithText("your phone shows a call in progress", substring = true)
            .assertExists()
        assertEquals(emptyList(), enabledCalls)
    }

    @Test
    fun `confirming the warning switches the auto-responder on`() {
        val enabledCalls = mutableListOf<Boolean>()
        setScreen(AutoResponderConfig(enabled = false), onSetEnabled = { enabledCalls += it })

        composeTestRule.onAllNodes(isToggleable())[AUTO_RESPONDER_SWITCH].performClick()
        composeTestRule.onNodeWithText("Continue").performClick()

        assertEquals(listOf(true), enabledCalls)
    }

    /** Cancelling leaves it off, and leaves the screen alone. */
    @Test
    fun `dismissing the warning leaves the auto-responder off`() {
        val enabledCalls = mutableListOf<Boolean>()
        setScreen(AutoResponderConfig(enabled = false), onSetEnabled = { enabledCalls += it })

        composeTestRule.onAllNodes(isToggleable())[AUTO_RESPONDER_SWITCH].performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList(), enabledCalls)
        composeTestRule
            .onNodeWithText("your phone shows a call in progress", substring = true)
            .assertDoesNotExist()
    }

    /**
     * Off is the safe direction and is never questioned: a user reaching for this switch has
     * usually just been surprised by the feature and wants it to stop now.
     */
    @Test
    fun `turning the auto-responder off does not ask`() {
        val enabledCalls = mutableListOf<Boolean>()
        setScreen(
            AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT),
            onSetEnabled = { enabledCalls += it },
        )

        composeTestRule.onAllNodes(isToggleable())[AUTO_RESPONDER_SWITCH].performClick()

        assertEquals(listOf(false), enabledCalls)
    }
}
