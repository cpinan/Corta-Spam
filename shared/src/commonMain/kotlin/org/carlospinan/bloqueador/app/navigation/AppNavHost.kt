package org.carlospinan.bloqueador.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import bloqueallamadas.shared.generated.resources.Res
import bloqueallamadas.shared.generated.resources.settings_privacy_title
import bloqueallamadas.shared.generated.resources.settings_terms_title
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.carlospinan.bloqueador.app.adaptive.AdaptiveScaffold
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.adaptive.routeSection
import org.carlospinan.bloqueador.app.adaptive.sectionRoutes
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderScreen
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderViewModel
import org.carlospinan.bloqueador.app.backup.BackupScreen
import org.carlospinan.bloqueador.app.backup.BackupViewModel
import org.carlospinan.bloqueador.app.blocklist.AllowlistScreen
import org.carlospinan.bloqueador.app.blocklist.BlockListHubScreen
import org.carlospinan.bloqueador.app.blocklist.BlockListViewModel
import org.carlospinan.bloqueador.app.blocklist.CountryRuleScreen
import org.carlospinan.bloqueador.app.blocklist.ManualBlockListScreen
import org.carlospinan.bloqueador.app.blocklist.PatternRuleScreen
import org.carlospinan.bloqueador.app.blocklist.ScheduleRuleScreen
import org.carlospinan.bloqueador.app.calllog.CallLogScreen
import org.carlospinan.bloqueador.app.calllog.CallLogViewModel
import org.carlospinan.bloqueador.app.home.HomeScreen
import org.carlospinan.bloqueador.app.home.HomeViewModel
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.settings.InfoScreen
import org.carlospinan.bloqueador.app.settings.SettingsScreen
import org.carlospinan.bloqueador.app.settings.SettingsViewModel
import org.carlospinan.bloqueador.app.stats.StatsScreen
import org.carlospinan.bloqueador.app.stats.StatsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

object Routes {
    const val HOME = "home"
    const val CALL_LOG = "call_log/{filter}"
    const val STATS = "stats"
    const val BLOCK_LIST = "block_list"
    const val MANUAL_BLOCK_LIST = "manual_block_list"
    const val ALLOWLIST = "allowlist"
    const val PATTERNS = "patterns"
    const val COUNTRIES = "countries"
    const val SCHEDULES = "schedules"
    const val SETTINGS = "settings"
    const val AUTO_RESPONDER = "auto_responder"
    const val BACKUP = "backup"
    const val PRIVACY_POLICY = "privacy_policy"
    const val TERMS_CONDITIONS = "terms_conditions"

    fun callLogRoute(filter: String = "all"): String = "call_log/$filter"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    onPickAudio: (() -> Unit)? = null,
    onRequestContactsPermission: (() -> Unit)? = null,
    onShareFile: ((String) -> Unit)? = null,
    onPickImportFile: (((String) -> Unit) -> Unit)? = null,
    onCallBack: ((String) -> Unit)? = null,
    onCopyNumber: ((String) -> Unit)? = null,
) {
    val windowSizeClass = rememberWindowSizeClass()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedSection = routeSection(currentRoute)

    val homeViewModel = koinViewModel<HomeViewModel>()
    val callLogViewModel = koinViewModel<CallLogViewModel>()
    val blockListViewModel = koinViewModel<BlockListViewModel>()
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val autoResponderViewModel = koinViewModel<AutoResponderViewModel>()
    val statsViewModel = koinViewModel<StatsViewModel>()
    val backupViewModel = koinViewModel<BackupViewModel>()

    AdaptiveScaffold(
        windowSizeClass = windowSizeClass,
        selectedIndex = selectedSection,
        onNavigate = { index ->
            val destination = sectionRoutes.getOrElse(index) { Routes.HOME }
            if (currentRoute == destination) return@AdaptiveScaffold
            navController.navigate(destination) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
        ) {
            composable(Routes.HOME) {
                val state by homeViewModel.state.collectAsState()
                val blockingEnabled by homeViewModel.blockingEnabled.collectAsState()
                HomeScreen(
                    state = state,
                    blockingEnabled = blockingEnabled,
                    onNavigateToCallLog = { navController.navigate(Routes.callLogRoute()) { launchSingleTop = true } },
                    onNavigateToCallLogToday = { navController.navigate(Routes.callLogRoute("today")) { launchSingleTop = true } },
                    onNavigateToCallLogThisWeek = { navController.navigate(Routes.callLogRoute("week")) { launchSingleTop = true } },
                    onNavigateToCallLogThisMonth = { navController.navigate(Routes.callLogRoute("month")) { launchSingleTop = true } },
                    onNavigateToBlockList = { navController.navigate(Routes.BLOCK_LIST) { launchSingleTop = true } },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    onNavigateToStats = { navController.navigate(Routes.STATS) { launchSingleTop = true } },
                    onToggleBlocking = homeViewModel::toggleBlocking,
                )
            }

            composable(
                route = Routes.CALL_LOG,
                arguments = listOf(navArgument("filter") { type = NavType.StringType }),
            ) { backStackEntry ->
                val filter = backStackEntry.arguments?.getString("filter") ?: "all"
                val allEntries by callLogViewModel.entries.collectAsState()
                val filtered = filterEntries(allEntries, filter)
                CallLogScreen(
                    entries = filtered,
                    filter = filter,
                    onBlockNumber = blockListViewModel::addBlockedNumber,
                    onAllowlistNumber = blockListViewModel::addAllowlistedNumber,
                    onCopyNumber = onCopyNumber ?: {},
                    onCallBack = onCallBack ?: {},
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.STATS) {
                val state by statsViewModel.state.collectAsState()
                StatsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.BLOCK_LIST) {
                val blockedCount by blockListViewModel.blockedCount.collectAsState()
                val allowlistedCount by blockListViewModel.allowlistedCount.collectAsState()
                val patternCount by blockListViewModel.patternCount.collectAsState()
                val countryCount by blockListViewModel.countryCount.collectAsState()
                val scheduleCount by blockListViewModel.scheduleCount.collectAsState()
                BlockListHubScreen(
                    blockedCount = blockedCount,
                    allowlistedCount = allowlistedCount,
                    patternCount = patternCount,
                    countryCount = countryCount,
                    scheduleCount = scheduleCount,
                    onNavigateToManual = { navController.navigate(Routes.MANUAL_BLOCK_LIST) { launchSingleTop = true } },
                    onNavigateToAllowlist = { navController.navigate(Routes.ALLOWLIST) { launchSingleTop = true } },
                    onNavigateToPatterns = { navController.navigate(Routes.PATTERNS) { launchSingleTop = true } },
                    onNavigateToCountries = { navController.navigate(Routes.COUNTRIES) { launchSingleTop = true } },
                    onNavigateToSchedules = { navController.navigate(Routes.SCHEDULES) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.MANUAL_BLOCK_LIST) {
                val blocked by blockListViewModel.blockedNumbers.collectAsState()
                val allowlisted by blockListViewModel.allowlistedNumbers.collectAsState()
                ManualBlockListScreen(
                    numbers = blocked,
                    allowlistedNumbers = allowlisted.map { it.number }.toSet(),
                    onAdd = blockListViewModel::addBlockedNumber,
                    onRemove = blockListViewModel::removeBlockedNumber,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ALLOWLIST) {
                val allowlisted by blockListViewModel.allowlistedNumbers.collectAsState()
                val blocked by blockListViewModel.blockedNumbers.collectAsState()
                AllowlistScreen(
                    numbers = allowlisted,
                    blockedNumbers = blocked.map { it.number }.toSet(),
                    onAdd = blockListViewModel::addAllowlistedNumber,
                    onRemove = blockListViewModel::removeAllowlistedNumber,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PATTERNS) {
                val patterns by blockListViewModel.patternRules.collectAsState()
                PatternRuleScreen(
                    patterns = patterns,
                    onAdd = blockListViewModel::addPatternRule,
                    onToggle = blockListViewModel::togglePatternRule,
                    onRemove = blockListViewModel::removePatternRule,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.COUNTRIES) {
                val countries by blockListViewModel.countryRules.collectAsState()
                CountryRuleScreen(
                    countries = countries,
                    onAdd = blockListViewModel::addCountryRule,
                    onToggle = blockListViewModel::toggleCountryRule,
                    onRemove = blockListViewModel::removeCountryRule,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SCHEDULES) {
                val schedules by blockListViewModel.scheduleRules.collectAsState()
                ScheduleRuleScreen(
                    rules = schedules,
                    onAdd = blockListViewModel::addScheduleRule,
                    onToggle = blockListViewModel::toggleScheduleRule,
                    onRemove = blockListViewModel::removeScheduleRule,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                val blockingEnabled by settingsViewModel.blockingEnabled.collectAsState()
                val autoAllowContacts by settingsViewModel.autoAllowContacts.collectAsState()
                val defaultAction by settingsViewModel.defaultAction.collectAsState()
                val spamEnabled by settingsViewModel.spamEnabled.collectAsState()
                SettingsScreen(
                    blockingEnabled = blockingEnabled,
                    autoAllowContacts = autoAllowContacts,
                    defaultAction = defaultAction,
                    spamEnabled = spamEnabled,
                    showGrantContacts = onRequestContactsPermission != null,
                    onSetBlockingEnabled = settingsViewModel::setBlockingEnabled,
                    onSetAutoAllowContacts = settingsViewModel::setAutoAllowContacts,
                    onSetDefaultAction = settingsViewModel::setDefaultAction,
                    onSetSpamEnabled = settingsViewModel::setSpamEnabled,
                    onRequestContactsPermission = onRequestContactsPermission ?: {},
                    onNavigateToAutoResponder = { navController.navigate(Routes.AUTO_RESPONDER) { launchSingleTop = true } },
                    onNavigateToBackup = { navController.navigate(Routes.BACKUP) { launchSingleTop = true } },
                    onNavigateToPrivacy = { navController.navigate(Routes.PRIVACY_POLICY) { launchSingleTop = true } },
                    onNavigateToTerms = { navController.navigate(Routes.TERMS_CONDITIONS) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.AUTO_RESPONDER) {
                val config by autoResponderViewModel.config.collectAsState()
                val validationError by autoResponderViewModel.validationError.collectAsState()
                AutoResponderScreen(
                    config = config,
                    validationError = validationError,
                    onSetEnabled = autoResponderViewModel::setEnabled,
                    onSetScript = autoResponderViewModel::setScript,
                    onSetRecordingEnabled = autoResponderViewModel::setRecordingEnabled,
                    onPickAudio = onPickAudio ?: {},
                    onClearAudio = autoResponderViewModel::clearAudioUri,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.BACKUP) {
                val state by backupViewModel.state.collectAsState()
                BackupScreen(
                    state = state,
                    onExport = {
                        backupViewModel.exportJson { json ->
                            onShareFile?.invoke(json)
                        }
                    },
                    onImport = {
                        onPickImportFile?.invoke { content ->
                            backupViewModel.importJson(content)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PRIVACY_POLICY) {
                InfoScreen(
                    title = stringResource(Res.string.settings_privacy_title),
                    body =
                        "Corta Spam does not collect, transmit, or share any personal data. " +
                            "No analytics, no telemetry, no advertising. No data leaves your device " +
                            "unless you explicitly enable the optional spam provider.\n\n" +
                            "All rules, settings, and call logs are stored locally in a SQLite database. " +
                            "You can delete them at any time by clearing the app data or uninstalling the app.\n\n" +
                            "Network access is limited to the optional spam provider check, which sends " +
                            "only the incoming caller's number to a public spam database. No contact data, " +
                            "device identifiers, or call audio is ever transmitted.\n\n" +
                            "The app is fully open source. Every line of code can be audited independently " +
                            "at its public repository.",
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.TERMS_CONDITIONS) {
                InfoScreen(
                    title = stringResource(Res.string.settings_terms_title),
                    body =
                        "Corta Spam is open-source software provided under the MIT License.\n\n" +
                            "Permission is hereby granted, free of charge, to any person obtaining a copy " +
                            "of this software and associated documentation files (the \"Software\"), to deal " +
                            "in the Software without restriction, including without limitation the rights " +
                            "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
                            "copies of the Software, and to permit persons to whom the Software is " +
                            "furnished to do so, subject to the following conditions:\n\n" +
                            "The above copyright notice and this permission notice shall be included in " +
                            "all copies or substantial portions of the Software.\n\n" +
                            "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR " +
                            "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
                            "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
                            "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER " +
                            "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
                            "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN " +
                            "THE SOFTWARE.",
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun filterEntries(
    entries: List<CallLogEntryData>,
    filter: String,
): List<CallLogEntryData> {
    if (filter == "all") return entries
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val todayStart =
        kotlinx.datetime.LocalDateTime(now.year, now.monthNumber, now.dayOfMonth, 0, 0, 0)
    val todayStartEpoch =
        todayStart.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    val cutoff =
        when (filter) {
            "today" -> todayStartEpoch
            "week" -> {
                val daysBack = now.dayOfWeek.ordinal
                todayStartEpoch - daysBack * 86_400_000L
            }
            "month" -> {
                val monthStart =
                    kotlinx.datetime.LocalDateTime(now.year, now.monthNumber, 1, 0, 0, 0)
                monthStart.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
            else -> return entries
        }
    return entries.filter { it.timestamp >= cutoff }
}
