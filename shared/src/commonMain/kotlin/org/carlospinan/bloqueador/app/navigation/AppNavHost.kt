package org.carlospinan.bloqueador.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.carlospinan.bloqueador.app.blocklist.AllowlistScreen
import org.carlospinan.bloqueador.app.blocklist.BlockListHubScreen
import org.carlospinan.bloqueador.app.blocklist.BlockListViewModel
import org.carlospinan.bloqueador.app.blocklist.CountryRuleScreen
import org.carlospinan.bloqueador.app.blocklist.ManualBlockListScreen
import org.carlospinan.bloqueador.app.blocklist.PatternRuleScreen
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
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    // Shared ViewModels scoped to the NavHost — survive across route changes
    // and keep state consistent when navigating between hub and detail screens.
    val homeViewModel = koinViewModel<HomeViewModel>()
    val callLogViewModel = koinViewModel<CallLogViewModel>()
    val blockListViewModel = koinViewModel<BlockListViewModel>()
    val settingsViewModel = koinViewModel<SettingsViewModel>()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
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
            BlockListHubScreen(
                blockedCount = blockedCount,
                allowlistedCount = allowlistedCount,
                patternCount = patternCount,
                countryCount = countryCount,
                onNavigateToManual = { navController.navigate(Routes.MANUAL_BLOCK_LIST) },
                onNavigateToAllowlist = { navController.navigate(Routes.ALLOWLIST) },
                onNavigateToPatterns = { navController.navigate(Routes.PATTERNS) },
                onNavigateToCountries = { navController.navigate(Routes.COUNTRIES) },
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
                onBack = { navController.popBackStack() },
            )
        }
    }
}
