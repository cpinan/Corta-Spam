package org.carlospinan.bloqueador.app.permissions

/**
 * The permissions this app asks a user for, in the order the onboarding checklist shows them.
 *
 * Deliberately *not* a mirror of AndroidManifest.xml. `VIBRATE` is a normal permission with
 * nothing to grant, and `CALL_PHONE` is shown as plain "Phone" because the permission name means
 * nothing to a user.
 *
 * `FULL_SCREEN_INTENT` was excluded from here on the reasoning that an app-op reachable only from
 * system Settings belongs in the warning banners rather than in a checklist of things to tap
 * Allow on. That reasoning rested on the permission being pre-granted at install, and it no
 * longer is: the Play Console full-screen-intent declaration answers *no* to pre-grant, so on
 * Android 14+ every install now starts with the ringing screen switched off. The warning banner
 * ranks it third, behind the two warnings a fresh install always shows, and Home renders only the
 * first -- so the one permission that is now always missing was the one hardest to find.
 */
enum class AppPermission {
    NOTIFICATIONS,
    FULL_SCREEN_INTENT,
    CONTACTS,
    PHONE,
    MICROPHONE,
}

/**
 * @param requestable whether this screen offers a control for it at all. Microphone is listed so
 *   the user knows it exists and what it is for, but is only ever requested at the moment call
 *   recording is switched on: a dialer asking for the mic during onboarding, for a feature that
 *   ships off, reads as overreach and gets denied permanently.
 * @param opensSettings whether that control leaves the app instead of showing a system dialog.
 *   `USE_FULL_SCREEN_INTENT` has no runtime-permission dialog -- it is an app-op, and the only way
 *   to grant it is the system settings screen -- so labelling its button "Allow" would promise a
 *   dialog that never appears. The grant is picked up by `onResume`, on the way back.
 */
data class PermissionUiItem(
    val permission: AppPermission,
    val granted: Boolean,
    val requestable: Boolean,
    val opensSettings: Boolean = false,
)

/**
 * Builds the onboarding checklist. [notificationsApplicable] is false below API 33, where
 * POST_NOTIFICATIONS does not exist and showing a row with an Allow button that opens nothing
 * would be a dead control. [fullScreenIntentApplicable] is the same story below API 34, where the
 * app-op does not exist and the platform shows the ringing screen without being asked.
 *
 * The full-screen row sits directly under notifications because they are one feature between
 * them: the notification is what carries the ringing call, and the app-op is what lets it take
 * over the screen. Splitting them across the list would read as two unrelated asks.
 */
fun permissionChecklist(
    notificationsGranted: Boolean,
    contactsGranted: Boolean,
    phoneGranted: Boolean,
    micGranted: Boolean,
    fullScreenIntentGranted: Boolean,
    notificationsApplicable: Boolean = true,
    fullScreenIntentApplicable: Boolean = true,
): List<PermissionUiItem> =
    buildList {
        if (notificationsApplicable) {
            add(PermissionUiItem(AppPermission.NOTIFICATIONS, notificationsGranted, requestable = true))
        }
        if (fullScreenIntentApplicable) {
            add(
                PermissionUiItem(
                    AppPermission.FULL_SCREEN_INTENT,
                    fullScreenIntentGranted,
                    requestable = true,
                    opensSettings = true,
                ),
            )
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
