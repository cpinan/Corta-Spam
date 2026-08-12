package org.carlospinan.bloqueador.app.permissions

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PermissionsOnboardingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `every permission is named and explained before any system dialog can appear`() {
        composeTestRule.setContent {
            PermissionsOnboardingScreen(
                items = permissionChecklist(false, false, false, false, false),
                onRequest = {},
                onContinue = {},
            )
        }

        composeTestRule.onNodeWithText("Notifications").assertExists()
        composeTestRule.onNodeWithText("Full-screen calls").assertExists()
        composeTestRule.onNodeWithText("Contacts").assertExists()
        composeTestRule.onNodeWithText("Phone").assertExists()
        composeTestRule.onNodeWithText("Microphone (optional)").assertExists()

        // The "why", not just the name -- a bare list of permission names is the thing this
        // screen was added to replace.
        composeTestRule.onNodeWithText("Show you who is calling", substring = true).assertExists()
        composeTestRule.onNodeWithText("over your lock screen", substring = true).assertExists()
        composeTestRule.onNodeWithText("show their name instead of a bare number", substring = true).assertExists()
        composeTestRule.onNodeWithText("before your phone rings", substring = true).assertExists()
        composeTestRule.onNodeWithText("recording for the auto-responder", substring = true).assertExists()
    }

    @Test
    fun `tapping allow requests that permission and no other`() {
        val requested = mutableListOf<AppPermission>()

        composeTestRule.setContent {
            PermissionsOnboardingScreen(
                items = permissionChecklist(false, false, false, false, false),
                onRequest = { requested += it },
                onContinue = {},
            )
        }

        // Index 1: notifications, contacts and phone carry an "Allow" button. Full-screen intent
        // sits between notifications and contacts but is not one of them -- its button reads
        // "Open settings", which is the whole point of the distinction.
        //
        // performScrollTo is not decoration: adding the full-screen row pushed contacts below the
        // fold, and a performClick on an off-screen node registers nothing at all -- the callback
        // simply never fires, which reads as "the wrong permission was requested".
        composeTestRule.onAllNodesWithText("Allow")[1].performScrollTo().performClick()

        assertEquals(listOf(AppPermission.CONTACTS), requested)
    }

    /**
     * With pre-grant declined in the Play Console, this row is ungranted on every Android 14+
     * install -- so it is the one row a fresh user is guaranteed to meet.
     */
    @Test
    fun `full-screen intent offers a settings route rather than a dialog it cannot open`() {
        val requested = mutableListOf<AppPermission>()

        composeTestRule.setContent {
            PermissionsOnboardingScreen(
                items = permissionChecklist(false, false, false, false, false),
                onRequest = { requested += it },
                onContinue = {},
            )
        }

        composeTestRule.onNodeWithText("Open settings").performScrollTo().performClick()

        assertEquals(listOf(AppPermission.FULL_SCREEN_INTENT), requested)
    }

    @Test
    fun `a granted permission shows its state instead of an allow button`() {
        composeTestRule.setContent {
            PermissionsOnboardingScreen(
                items =
                    permissionChecklist(
                        notificationsGranted = true,
                        contactsGranted = true,
                        phoneGranted = true,
                        micGranted = false,
                        fullScreenIntentGranted = true,
                    ),
                onRequest = {},
                onContinue = {},
            )
        }

        composeTestRule.onNodeWithText("Allow").assertDoesNotExist()
        composeTestRule.onNodeWithText("Open settings").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Granted").assertCountEquals(4)
    }

    @Test
    fun `microphone offers no allow button because it is asked for at recording time`() {
        composeTestRule.setContent {
            PermissionsOnboardingScreen(
                items = listOf(PermissionUiItem(AppPermission.MICROPHONE, granted = false, requestable = false)),
                onRequest = { error("microphone must not be requestable from onboarding") },
                onContinue = {},
            )
        }

        composeTestRule.onNodeWithText("Allow").assertDoesNotExist()
        composeTestRule.onNodeWithText("Asked only if you turn recording on").assertExists()
    }

    @Test
    fun `the user is never trapped -- continue works with everything denied`() {
        var continued = false

        composeTestRule.setContent {
            PermissionsOnboardingScreen(
                items = permissionChecklist(false, false, false, false, false),
                onRequest = {},
                onContinue = { continued = true },
            )
        }

        composeTestRule.onNodeWithText("Continue anyway").performScrollTo().performClick()

        assertTrue(continued)
    }

    @Test
    fun `continue drops the anyway wording once nothing is outstanding`() {
        composeTestRule.setContent {
            PermissionsOnboardingScreen(
                items = permissionChecklist(true, true, true, micGranted = false, fullScreenIntentGranted = true),
                onRequest = {},
                onContinue = {},
            )
        }

        composeTestRule.onNodeWithText("Continue").assertExists()
        composeTestRule.onNodeWithText("Continue anyway").assertDoesNotExist()
    }
}
