package org.carlospinan.bloqueador.app.telecom

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPolicyTest {
    @Test
    fun `posts nothing when notifications are off entirely`() {
        assertFalse(
            NotificationPolicy.shouldNotifyCallResult(
                notificationsEnabled = false,
                notifyUnknownCallers = true,
                contactsAccessGranted = true,
                callerIsInContacts = true,
            ),
        )
    }

    @Test
    fun `posts for a stranger while the unknown-caller setting is on`() {
        assertTrue(
            NotificationPolicy.shouldNotifyCallResult(
                notificationsEnabled = true,
                notifyUnknownCallers = true,
                contactsAccessGranted = true,
                callerIsInContacts = false,
            ),
        )
    }

    @Test
    fun `drops a stranger once the unknown-caller setting is off`() {
        assertFalse(
            NotificationPolicy.shouldNotifyCallResult(
                notificationsEnabled = true,
                notifyUnknownCallers = false,
                contactsAccessGranted = true,
                callerIsInContacts = false,
            ),
        )
    }

    @Test
    fun `still posts for a contact once the unknown-caller setting is off`() {
        assertTrue(
            NotificationPolicy.shouldNotifyCallResult(
                notificationsEnabled = true,
                notifyUnknownCallers = false,
                contactsAccessGranted = true,
                callerIsInContacts = true,
            ),
        )
    }

    @Test
    fun `posts for everyone when contacts access is denied`() {
        // The trap this guards: with no READ_CONTACTS the lookup reports every caller as unknown,
        // so filtering on it would silence the channel for the user's whole address book too.
        assertTrue(
            NotificationPolicy.shouldNotifyCallResult(
                notificationsEnabled = true,
                notifyUnknownCallers = false,
                contactsAccessGranted = false,
                callerIsInContacts = false,
            ),
        )
    }
}
