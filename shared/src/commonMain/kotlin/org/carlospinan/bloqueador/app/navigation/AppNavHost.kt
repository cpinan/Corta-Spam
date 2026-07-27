package org.carlospinan.bloqueador.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.carlospinan.bloqueador.app.blocklist.AllowlistScreen
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
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            val viewModel = koinViewModel<HomeViewModel>()
            val state by viewModel.state.collectAsState()
            HomeScreen(
                state = state,
                onNavigateToCallLog = { navController.navigate(Routes.CALL_LOG) },
                onNavigateToBlockList = { navController.navigate(Routes.BLOCK_LIST) },
                onToggleBlocking = viewModel::toggleBlocking,
            )
        }

        composable(Routes.CALL_LOG) {
            val viewModel = koinViewModel<CallLogViewModel>()
            val entries by viewModel.entries.collectAsState()
            CallLogScreen(
                entries = entries,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.BLOCK_LIST) {
            val viewModel = koinViewModel<BlockListViewModel>()
            val blockedCount by viewModel.blockedCount.collectAsState()
            val allowlistedCount by viewModel.allowlistedCount.collectAsState()
            org.carlospinan.bloqueador.app.blocklist.BlockListHubScreen(
                blockedCount = blockedCount,
                allowlistedCount = allowlistedCount,
                onNavigateToManual = { navController.navigate(Routes.MANUAL_BLOCK_LIST) },
                onNavigateToAllowlist = { navController.navigate(Routes.ALLOWLIST) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.MANUAL_BLOCK_LIST) {
            val viewModel = koinViewModel<BlockListViewModel>()
            val blocked by viewModel.blockedNumbers.collectAsState()
            ManualBlockListScreen(
                numbers = blocked,
                onAdd = viewModel::addBlockedNumber,
                onRemove = viewModel::removeBlockedNumber,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ALLOWLIST) {
            val viewModel = koinViewModel<BlockListViewModel>()
            val allowlisted by viewModel.allowlistedNumbers.collectAsState()
            AllowlistScreen(
                numbers = allowlisted,
                onAdd = viewModel::addAllowlistedNumber,
                onRemove = viewModel::removeAllowlistedNumber,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
