package org.carlospinan.bloqueador.app.adaptive

object AdaptiveRoutes {
    const val HOME = "home"
    const val KEYPAD = "keypad"
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
val sectionRoutes =
    listOf(
        AdaptiveRoutes.HOME,
        AdaptiveRoutes.KEYPAD,
        AdaptiveRoutes.callLogRoute(),
        AdaptiveRoutes.BLOCK_LIST,
        AdaptiveRoutes.SETTINGS,
    )

fun routeSection(route: String?): Int =
    when {
        route == null -> 0
        route in homeSectionRoutes -> 0
        route in keypadSectionRoutes -> 1
        route.startsWith("call_log") -> 2
        route in blockListSectionRoutes -> 3
        route in settingsSectionRoutes -> 4
        else -> 0
    }
