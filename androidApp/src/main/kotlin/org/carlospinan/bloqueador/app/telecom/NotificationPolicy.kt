package org.carlospinan.bloqueador.app.telecom

/**
 * Whether an after-the-fact call notification — blocked, missed, repeated caller — should be
 * posted.
 *
 * A pure function on purpose: [PassthroughInCallService] is a Telecom-bound service that no unit
 * test can construct, and every notification decision this app has shipped so far was only
 * provable by placing a real call. This one is provable at the desk.
 *
 * Note what is *not* routed through here: the ringing full-screen notification. As the default
 * dialer this app owns the incoming-call UI, so suppressing that notification does not hide a
 * banner — it leaves a stranger's call with no screen at all over the lock screen.
 */
internal object NotificationPolicy {
    fun shouldNotifyCallResult(
        notificationsEnabled: Boolean,
        notifyUnknownCallers: Boolean,
        contactsAccessGranted: Boolean,
        callerIsInContacts: Boolean,
    ): Boolean {
        if (!notificationsEnabled) return false
        if (notifyUnknownCallers) return true
        // Without READ_CONTACTS the PhoneLookup returns nothing for everyone, so filtering on it
        // would silence the channel outright rather than the strangers the setting names — the
        // user would switch off notifications for unknown callers and lose them for their mother
        // too. Notify, and let the existing permission warning be what asks for the grant.
        if (!contactsAccessGranted) return true
        return callerIsInContacts
    }
}
