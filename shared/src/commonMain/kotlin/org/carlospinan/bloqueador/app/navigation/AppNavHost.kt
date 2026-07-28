package org.carlospinan.bloqueador.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderScreen
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderViewModel
import org.carlospinan.bloqueador.app.blocklist.ActionRuleScreen
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
import org.carlospinan.bloqueador.app.settings.SettingsScreen
import org.carlospinan.bloqueador.app.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

object Routes {
    const val HOME = "home"
    const val CALL_LOG = "call_log"
    const val BLOCK_LIST = "block_list"
    const val MANUAL_BLOCK_LIST = "manual_block_list"
    const val ALLOWLIST = "allowlist"
    const val PATTERNS = "patterns"
    const val COUNTRIES = "countries"
    const val ACTION_RULES = "action_rules"
    const val SCHEDULES = "schedules"
    const val SETTINGS = "settings"
    const val AUTO_RESPONDER = "auto_responder"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    // Shared ViewModels scoped to the NavHost — survive across route changes
    // and keep state consistent when navigating between hub and detail screens.
    val homeViewModel = koinViewModel<HomeViewModel>()
    val callLogViewModel = koinViewModel<CallLogViewModel>()
    val blockListViewModel = koinViewModel<BlockListViewModel>()
    val settingsViewModel = koinViewModel<SettingsViewModel>()
    val autoResponderViewModel = koinViewModel<AutoResponderViewModel>()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            // homeViewModel is NavHost-scoped and only refreshes once in init. Re-trigger on
            // every ON_RESUME (covers both re-navigating to this route and the app coming back
            // to the foreground after a call was blocked in the background) — otherwise stats
            // go stale until the process restarts.
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { homeViewModel.refresh() }
            val state by homeViewModel.state.collectAsState()
            HomeScreen(
                state = state,
                onNavigateToCallLog = { navController.navigate(Routes.CALL_LOG) },
                onNavigateToBlockList = { navController.navigate(Routes.BLOCK_LIST) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onToggleBlocking = homeViewModel::toggleBlocking,
            )
        }

        composable(Routes.CALL_LOG) {
            val entries by callLogViewModel.entries.collectAsState()
            CallLogScreen(
                entries = entries,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.BLOCK_LIST) {
            val blockedCount by blockListViewModel.blockedCount.collectAsState()
            val allowlistedCount by blockListViewModel.allowlistedCount.collectAsState()
            val patternCount by blockListViewModel.patternCount.collectAsState()
            val countryCount by blockListViewModel.countryCount.collectAsState()
            val actionCount by blockListViewModel.actionCount.collectAsState()
            val scheduleCount by blockListViewModel.scheduleCount.collectAsState()
            BlockListHubScreen(
                blockedCount = blockedCount,
                allowlistedCount = allowlistedCount,
                patternCount = patternCount,
                countryCount = countryCount,
                actionCount = actionCount,
                scheduleCount = scheduleCount,
                onNavigateToManual = { navController.navigate(Routes.MANUAL_BLOCK_LIST) },
                onNavigateToAllowlist = { navController.navigate(Routes.ALLOWLIST) },
                onNavigateToPatterns = { navController.navigate(Routes.PATTERNS) },
                onNavigateToCountries = { navController.navigate(Routes.COUNTRIES) },
                onNavigateToActions = { navController.navigate(Routes.ACTION_RULES) },
                onNavigateToSchedules = { navController.navigate(Routes.SCHEDULES) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.MANUAL_BLOCK_LIST) {
            val blocked by blockListViewModel.blockedNumbers.collectAsState()
            ManualBlockListScreen(
                numbers = blocked,
                onAdd = blockListViewModel::addBlockedNumber,
                onRemove = blockListViewModel::removeBlockedNumber,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ALLOWLIST) {
            val allowlisted by blockListViewModel.allowlistedNumbers.collectAsState()
            AllowlistScreen(
                numbers = allowlisted,
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

        composable(Routes.ACTION_RULES) {
            val actions by blockListViewModel.actionRules.collectAsState()
            ActionRuleScreen(
                rules = actions,
                onAdd = blockListViewModel::addActionRule,
                onToggle = blockListViewModel::toggleActionRule,
                onRemove = blockListViewModel::removeActionRule,
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
                onSetBlockingEnabled = settingsViewModel::setBlockingEnabled,
                onSetAutoAllowContacts = settingsViewModel::setAutoAllowContacts,
                onSetDefaultAction = settingsViewModel::setDefaultAction,
                onSetSpamEnabled = settingsViewModel::setSpamEnabled,
                onNavigateToAutoResponder = { navController.navigate(Routes.AUTO_RESPONDER) },
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
                onPickAudio = { /* Android file picker wired later via platform callback */ },
                onClearAudio = autoResponderViewModel::clearAudioUri,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
