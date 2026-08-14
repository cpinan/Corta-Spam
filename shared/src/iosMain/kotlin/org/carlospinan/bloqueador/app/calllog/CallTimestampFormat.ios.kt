package org.carlospinan.bloqueador.app.calllog

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

/**
 * A fresh formatter per call, and `currentLocale` read each time, for the reason the Android
 * actual gives: the locale can change while the app is running, and a cached formatter would keep
 * printing the previous one. `NSDateFormatter` construction is not cheap, but this runs once per
 * visible row rather than per frame.
 */
actual fun formatCallTimestamp(epochMillis: Long): String {
    val formatter =
        NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            timeStyle = NSDateFormatterShortStyle
            locale = NSLocale.currentLocale
        }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}
