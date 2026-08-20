package org.carlospinan.bloqueador.app.rules

import java.util.Locale

/**
 * CLDR, through `java.util.Locale`.
 *
 * `getDisplayCountry` returns the region code back when it has no name for it, so that case is
 * turned into null — the caller has a better fallback than "MA", and a screen reading "ZZ" would
 * be worse than reading the English name the database already had.
 *
 * `Locale.getDefault()` rather than a captured locale: this is called during composition, and on
 * Android 13+ a per-app language change restarts the activity, so the default is current by the
 * time anything re-reads it.
 */
actual fun platformCountryName(regionCode: String): String? {
    val locale = Locale.Builder().setRegion(regionCode).build()
    val name = locale.getDisplayCountry(Locale.getDefault())
    return name.takeIf { it.isNotBlank() && !it.equals(regionCode, ignoreCase = true) }
}
