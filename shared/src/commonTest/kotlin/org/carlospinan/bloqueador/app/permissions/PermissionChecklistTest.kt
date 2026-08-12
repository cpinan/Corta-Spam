package org.carlospinan.bloqueador.app.permissions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionChecklistTest {
    @Test
    fun `every permission the app asks for is on the checklist`() {
        val items = permissionChecklist(false, false, false, false, false)

        assertEquals(
            listOf(
                AppPermission.NOTIFICATIONS,
                AppPermission.FULL_SCREEN_INTENT,
                AppPermission.CONTACTS,
                AppPermission.PHONE,
                AppPermission.MICROPHONE,
            ),
            items.map { it.permission },
        )
    }

    @Test
    fun `notifications row is absent where the permission does not exist`() {
        val items = permissionChecklist(false, false, false, false, false, notificationsApplicable = false)

        assertFalse(items.any { it.permission == AppPermission.NOTIFICATIONS })
        assertEquals(4, items.size)
    }

    @Test
    fun `full-screen row is absent where the app-op does not exist`() {
        val items = permissionChecklist(false, false, false, false, false, fullScreenIntentApplicable = false)

        assertFalse(items.any { it.permission == AppPermission.FULL_SCREEN_INTENT })
        assertEquals(4, items.size)
    }

    @Test
    fun `microphone is explained but never requested from onboarding`() {
        val mic = permissionChecklist(false, false, false, false, false).single { it.permission == AppPermission.MICROPHONE }

        assertFalse(mic.requestable)
    }

    /**
     * The distinction the row's button label depends on. Every other requestable permission has a
     * system dialog; this one has only a settings screen, and a button reading "Allow" would
     * promise a dialog that never appears.
     */
    @Test
    fun `full-screen intent is the only row that leaves the app to be granted`() {
        val items = permissionChecklist(false, false, false, false, false)

        assertEquals(
            listOf(AppPermission.FULL_SCREEN_INTENT),
            items.filter { it.opensSettings }.map { it.permission },
        )
    }

    @Test
    fun `grant state is carried through per permission`() {
        val items =
            permissionChecklist(
                notificationsGranted = true,
                contactsGranted = false,
                phoneGranted = true,
                micGranted = false,
                fullScreenIntentGranted = true,
            ).associate { it.permission to it.granted }

        assertEquals(
            mapOf(
                AppPermission.NOTIFICATIONS to true,
                AppPermission.FULL_SCREEN_INTENT to true,
                AppPermission.CONTACTS to false,
                AppPermission.PHONE to true,
                AppPermission.MICROPHONE to false,
            ),
            items,
        )
    }

    @Test
    fun `an ungranted microphone does not count as outstanding work`() {
        val items = permissionChecklist(true, true, true, micGranted = false, fullScreenIntentGranted = true)

        assertTrue(allRequestablePermissionsGranted(items))
    }

    @Test
    fun `any ungranted requestable permission counts as outstanding work`() {
        assertFalse(allRequestablePermissionsGranted(permissionChecklist(false, true, true, true, true)))
        assertFalse(allRequestablePermissionsGranted(permissionChecklist(true, false, true, true, true)))
        assertFalse(allRequestablePermissionsGranted(permissionChecklist(true, true, false, true, true)))
        assertFalse(allRequestablePermissionsGranted(permissionChecklist(true, true, true, true, false)))
    }
}
