package org.carlospinan.bloqueador.app.rules

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localizedStringForCountryCode

/**
 * Foundation's own region names, in the user's current locale.
 *
 * `localizedStringForCountryCode` already returns null for a code it does not know, which is the
 * contract the common side wants; the blank check is for the empty string some regions return.
 */
actual fun platformCountryName(regionCode: String): String? =
    NSLocale.currentLocale
        .localizedStringForCountryCode(regionCode)
        ?.takeIf { it.isNotBlank() }
