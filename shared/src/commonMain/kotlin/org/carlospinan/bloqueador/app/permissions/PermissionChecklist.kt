package org.carlospinan.bloqueador.app.permissions

/**
 * The permissions this app asks a user for, in the order the onboarding checklist shows them.
 *
 * Deliberately *not* a mirror of AndroidManifest.xml. `VIBRATE` is a normal permission with
 * nothing to grant, `READ_PHONE_STATE` and `CALL_PHONE` are one thing from the user's point of
 * view ("Phone"), and `USE_FULL_SCREEN_INTENT` is an app-op the user can only reach from system
 * Settings -- it belongs in the warning banners, not in a checklist of things to tap Allow on.
 */
enum class AppPermission {
    NOTIFICATIONS,
    CONTACTS,
    PHONE,
    MICROPHONE,
}

/**
 * @param requestable whether this screen may launch the system dialog for it. Microphone is
 *   listed so the user knows it exists and what it is for, but is only ever requested at the
 *   moment call recording is switched on: a dialer asking for the mic during onboarding, for a
 *   feature that ships off, reads as overreach and gets denied permanently.
 */
data class PermissionUiItem(
    val permission: AppPermission,
    val granted: Boolean,
    val requestable: Boolean,
)

/**
 * Builds the onboarding checklist. [notificationsApplicable] is false below API 33, where
 * POST_NOTIFICATIONS does not exist and showing a row with an Allow button that opens nothing
 * would be a dead control.
 */
fun permissionChecklist(
    notificationsGranted: Boolean,
    contactsGranted: Boolean,
    phoneGranted: Boolean,
    micGranted: Boolean,
    notificationsApplicable: Boolean = true,
): List<PermissionUiItem> =
    buildList {
        if (notificationsApplicable) {
            add(PermissionUiItem(AppPermission.NOTIFICATIONS, notificationsGranted, requestable = true))
        }
        add(PermissionUiItem(AppPermission.CONTACTS, contactsGranted, requestable = true))
        add(PermissionUiItem(AppPermission.PHONE, phoneGranted, requestable = true))
        add(PermissionUiItem(AppPermission.MICROPHONE, micGranted, requestable = false))
    }

/**
 * True when nothing on the checklist is still waiting on the user. Drives the continue button's
 * label only -- the user is never trapped on this screen, every permission here is refusable.
 */
fun allRequestablePermissionsGranted(items: List<PermissionUiItem>): Boolean = items.none { it.requestable && !it.granted }
