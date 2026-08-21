package org.carlospinan.bloqueador.app.adaptive

object AdaptiveRoutes {
    const val HOME = "home"
    const val KEYPAD = "keypad"
    const val AGENDA = "agenda"
    const val CALL_LOG = "call_log/{filter}"
    const val STATS = "stats"
    const val BLOCK_LIST = "block_list"
    const val MANUAL_BLOCK_LIST = "manual_block_list"
    const val ALLOWLIST = "allowlist"
    const val PATTERNS = "patterns"
    const val COUNTRIES = "countries"
    const val SCHEDULES = "schedules"
    const val ACTION_RULES = "action_rules"
    const val SETTINGS = "settings"
    const val AUTO_RESPONDER = "auto_responder"
    const val BACKUP = "backup"
    const val PRIVACY_POLICY = "privacy_policy"
    const val TERMS_CONDITIONS = "terms_conditions"
    const val CREDITS = "credits"

    fun callLogRoute(filter: String = "all"): String = "call_log/$filter"
}

val homeSectionRoutes =
    setOf(AdaptiveRoutes.HOME, AdaptiveRoutes.STATS)
val keypadSectionRoutes =
    setOf(AdaptiveRoutes.KEYPAD)
val agendaSectionRoutes =
    setOf(AdaptiveRoutes.AGENDA)
val callLogSectionRoutes =
    setOf(AdaptiveRoutes.CALL_LOG)
val blockListSectionRoutes =
    setOf(
        AdaptiveRoutes.BLOCK_LIST,
        AdaptiveRoutes.MANUAL_BLOCK_LIST,
        AdaptiveRoutes.ALLOWLIST,
        AdaptiveRoutes.PATTERNS,
        AdaptiveRoutes.COUNTRIES,
        AdaptiveRoutes.SCHEDULES,
        AdaptiveRoutes.ACTION_RULES,
    )
val settingsSectionRoutes =
    setOf(
        AdaptiveRoutes.SETTINGS,
        AdaptiveRoutes.AUTO_RESPONDER,
        AdaptiveRoutes.BACKUP,
        AdaptiveRoutes.PRIVACY_POLICY,
        AdaptiveRoutes.TERMS_CONDITIONS,
        AdaptiveRoutes.CREDITS,
    )

/**
 * The five destinations the navigation bar offers, in order.
 *
 * Settings is deliberately not among them. Material's bar tops out at five, the Agenda tab is the
 * address book of an app that replaced the phone's dialer -- something reached constantly -- and
 * Settings is reached once, from the card Home already has for it. Five items that are used beats
 * six that are cramped, and on a 360 dp phone six labels truncate to initials.
 */
val sectionRoutes =
    listOf(
        AdaptiveRoutes.HOME,
        AdaptiveRoutes.KEYPAD,
        AdaptiveRoutes.AGENDA,
        AdaptiveRoutes.callLogRoute(),
        AdaptiveRoutes.BLOCK_LIST,
    )

/**
 * No tab is highlighted. Settings and its sub-screens are reached from Home rather than from the
 * bar, and lighting up Home while the user is reading the privacy policy would claim they are
 * somewhere they are not.
 */
const val NO_SECTION = -1

fun routeSection(route: String?): Int =
    when {
        route == null -> 0
        route in homeSectionRoutes -> 0
        route in keypadSectionRoutes -> 1
        route in agendaSectionRoutes -> 2
        route.startsWith("call_log") -> 3
        route in blockListSectionRoutes -> 4
        route in settingsSectionRoutes -> NO_SECTION
        else -> 0
    }
