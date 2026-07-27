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
import org.carlospinan.bloqueador.app.blocklist.ManualBlockListScreen
import org.carlospinan.bloqueador.app.calllog.CallLogScreen
import org.carlospinan.bloqueador.app.calllog.CallLogViewModel
import org.carlospinan.bloqueador.app.home.HomeScreen
import org.carlospinan.bloqueador.app.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

object Routes {
    const val HOME = "home"
    const val CALL_LOG = "call_log"
    const val BLOCK_LIST = "block_list"
    const val MANUAL_BLOCK_LIST = "manual_block_list"
    const val ALLOWLIST = "allowlist"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    // Shared ViewModels scoped to the NavHost — survive across route changes
    // and keep state consistent when navigating between hub and detail screens.
    val homeViewModel = koinViewModel<HomeViewModel>()
    val callLogViewModel = koinViewModel<CallLogViewModel>()
    val blockListViewModel = koinViewModel<BlockListViewModel>()

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
            BlockListHubScreen(
                blockedCount = blockedCount,
                allowlistedCount = allowlistedCount,
                onNavigateToManual = { navController.navigate(Routes.MANUAL_BLOCK_LIST) },
                onNavigateToAllowlist = { navController.navigate(Routes.ALLOWLIST) },
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
    }
}
