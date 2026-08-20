package org.carlospinan.bloqueador.app.rules

/**
 * The name of an ISO 3166-1 alpha-2 region in the language the user is reading, or null when the
 * platform has no name for it.
 *
 * Implemented per platform because the data is the platform's: Android and the JVM carry CLDR,
 * iOS carries its own, and both know far more languages than this app ships strings for. The
 * alternative — 223 country names times four locales in the resource files — would be a large
 * translation surface that goes stale, and would still leave every locale the app does not ship
 * reading English.
 *
 * Returning null rather than the region code matters: the caller has a better fallback than "MA".
 */
expect fun platformCountryName(regionCode: String): String?

/**
 * Country names for display, resolved at render time rather than read from storage.
 *
 * A `CountryRule` row persists the name it was created with, and so does the `rule_detail` on a
 * blocked call. That was the whole bug: the name was captured once, in whatever language the list
 * happened to be written in — English — so a Spanish call log said "País: Morocco / Western
 * Sahara", and changing the phone's language could never have fixed it because the string was
 * already in the database.
 *
 * Resolving from the dialling code instead means existing rows localize with no migration: the
 * stored name is demoted to a fallback and only surfaces for a region the platform cannot name.
 */
object CountryNames {
    /** " / ", matching how a shared dialling code has always been written in [COUNTRIES]. */
    private const val SEPARATOR = " / "

    private val byCode: Map<String, Country> by lazy { COUNTRIES.associateBy { it.code } }

    /**
     * The localized name for [diallingCode], falling back to [storedName] and then to the bundled
     * English one.
     *
     * [storedName] is what the database recorded when the rule was created. It is preferred over
     * the bundled name because a code this build does not know about can still have been stored by
     * an older one — dropping to the raw code there would lose information the row already had.
     *
     * A code covering more than one region is joined: `+1` reads "United States / Canada" in
     * English and "Estados Unidos / Canadá" in Spanish, from the platform, in both cases.
     */
    fun forDiallingCode(
        diallingCode: String,
        storedName: String? = null,
    ): String {
        val country = byCode[diallingCode]
        val localized =
            country
                ?.regions
                ?.mapNotNull { platformCountryName(it) }
                ?.takeIf { it.size == country.regions.size }
                ?.joinToString(SEPARATOR)
        return localized
            ?: storedName?.takeIf { it.isNotBlank() }
            ?: country?.name
            ?: diallingCode
    }

    /**
     * Whether [country] matches a picker query.
     *
     * Both names are searched, not just the displayed one. Someone reading the app in Spanish may
     * still type "Germany" — the English name is what most of the internet calls it — and the
     * dialling code is matched too because that is what a user copying a number off a screen has.
     */
    fun matches(
        country: Country,
        query: String,
    ): Boolean {
        if (query.isBlank()) return true
        val trimmed = query.trim()
        return forDiallingCode(country.code, storedName = null).contains(trimmed, ignoreCase = true) ||
            country.name.contains(trimmed, ignoreCase = true) ||
            country.code.contains(trimmed)
    }
}
