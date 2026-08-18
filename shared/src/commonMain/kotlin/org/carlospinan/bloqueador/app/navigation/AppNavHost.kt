package org.carlospinan.bloqueador.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.savedstate.read
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.privacy_policy_body
import cortaspam.shared.generated.resources.settings_privacy_title
import cortaspam.shared.generated.resources.settings_terms_title
import cortaspam.shared.generated.resources.terms_conditions_body
import org.carlospinan.bloqueador.app.adaptive.AdaptiveRoutes
import org.carlospinan.bloqueador.app.adaptive.AdaptiveScaffold
import org.carlospinan.bloqueador.app.adaptive.rememberWindowSizeClass
import org.carlospinan.bloqueador.app.adaptive.routeSection
import org.carlospinan.bloqueador.app.adaptive.sectionRoutes
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderIntent
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderScreen
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderViewModel
import org.carlospinan.bloqueador.app.backup.BackupIntent
import org.carlospinan.bloqueador.app.backup.BackupScreen
import org.carlospinan.bloqueador.app.backup.BackupViewModel
import org.carlospinan.bloqueador.app.blocklist.ActionRuleScreen
import org.carlospinan.bloqueador.app.blocklist.AllowlistScreen
import org.carlospinan.bloqueador.app.blocklist.BlockListHubScreen
import org.carlospinan.bloqueador.app.blocklist.BlockListIntent
import org.carlospinan.bloqueador.app.blocklist.BlockListViewModel
import org.carlospinan.bloqueador.app.blocklist.CountryRuleScreen
import org.carlospinan.bloqueador.app.blocklist.ManualBlockListScreen
import org.carlospinan.bloqueador.app.blocklist.PatternRuleScreen
import org.carlospinan.bloqueador.app.blocklist.ScheduleRuleScreen
import org.carlospinan.bloqueador.app.calllog.CallLogIntent
import org.carlospinan.bloqueador.app.calllog.CallLogRequest
import org.carlospinan.bloqueador.app.calllog.CallLogScreen
import org.carlospinan.bloqueador.app.calllog.CallLogViewModel
import org.carlospinan.bloqueador.app.credits.CreditsScreen
import org.carlospinan.bloqueador.app.home.HomeIntent
import org.carlospinan.bloqueador.app.home.HomeScreen
import org.carlospinan.bloqueador.app.home.HomeViewModel
import org.carlospinan.bloqueador.app.keypad.DialRequest
import org.carlospinan.bloqueador.app.keypad.KeypadIntent
import org.carlospinan.bloqueador.app.keypad.KeypadScreen
import org.carlospinan.bloqueador.app.keypad.KeypadViewModel
import org.carlospinan.bloqueador.app.settings.AppVersion
import org.carlospinan.bloqueador.app.settings.InfoScreen
import org.carlospinan.bloqueador.app.settings.SettingsIntent
import org.carlospinan.bloqueador.app.settings.SettingsScreen
import org.carlospinan.bloqueador.app.settings.SettingsViewModel
import org.carlospinan.bloqueador.app.stats.StatsScreen
import org.carlospinan.bloqueador.app.stats.StatsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Route names for the NavHost. Every value is delegated to [AdaptiveRoutes] rather than
 * re-spelled: the adaptive scaffold decides which navigation-bar section is highlighted by
 * looking the current route up in [AdaptiveRoutes]' section sets, so two independent copies of
 * the same strings would silently disagree the moment one of them gained a route the other
 * didn't — the new destination would render with the wrong section selected.
 */
object Routes {
    const val HOME = AdaptiveRoutes.HOME
    const val KEYPAD = AdaptiveRoutes.KEYPAD
    const val CALL_LOG = AdaptiveRoutes.CALL_LOG
    const val STATS = AdaptiveRoutes.STATS
    const val BLOCK_LIST = AdaptiveRoutes.BLOCK_LIST
    const val MANUAL_BLOCK_LIST = AdaptiveRoutes.MANUAL_BLOCK_LIST
    const val ALLOWLIST = AdaptiveRoutes.ALLOWLIST
    const val PATTERNS = AdaptiveRoutes.PATTERNS
    const val COUNTRIES = AdaptiveRoutes.COUNTRIES
    const val SCHEDULES = AdaptiveRoutes.SCHEDULES
    const val ACTION_RULES = AdaptiveRoutes.ACTION_RULES
    const val SETTINGS = AdaptiveRoutes.SETTINGS
    const val AUTO_RESPONDER = AdaptiveRoutes.AUTO_RESPONDER
    const val BACKUP = AdaptiveRoutes.BACKUP
    const val PRIVACY_POLICY = AdaptiveRoutes.PRIVACY_POLICY
    const val TERMS_CONDITIONS = AdaptiveRoutes.TERMS_CONDITIONS
    const val CREDITS = AdaptiveRoutes.CREDITS

    fun callLogRoute(filter: String = "all"): String = AdaptiveRoutes.callLogRoute(filter)
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    onPickAudio: (((String) -> Unit) -> Unit)? = null,
    onTestGreeting: ((script: String, audioUri: String) -> Unit)? = null,
    onRequestContactsPermission: (() -> Unit)? = null,
    onShareFile: ((String) -> Unit)? = null,
    onPickImportFile: (((String) -> Unit) -> Unit)? = null,
    onCallBack: ((String) -> Unit)? = null,
    onCopyNumber: ((String) -> Unit)? = null,
    contactsPermissionGranted: Boolean = false,
    dialerRoleHeld: Boolean = true,
    notificationsPermissionGranted: Boolean = true,
    fullScreenIntentAllowed: Boolean = true,
    callPhonePermissionGranted: Boolean = true,
    onRequestDialerRole: (() -> Unit)? = null,
    onOpenNotificationSettings: (() -> Unit)? = null,
    onOpenFullScreenIntentSettings: (() -> Unit)? = null,
    onOpenAppSettings: (() -> Unit)? = null,
    micPermissionGranted: Boolean = true,
    onRequestMicPermission: (() -> Unit)? = null,
    onPlayRecording: ((String) -> Unit)? = null,
    dialRequest: DialRequest? = null,
    callLogRequest: CallLogRequest? = null,
) {
    val windowSizeClass = rememberWindowSizeClass()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedSection = routeSection(currentRoute)

    // An ACTION_DIAL intent has to reach the keypad wherever the user happens to be. Without this
    // the number arrived and the app simply showed whatever screen was already open, which is the
    // bug the manifest's DIAL filters had shipped with since they were added for ROLE_DIALER.
    LaunchedEffect(dialRequest) {
        if (dialRequest == null) return@LaunchedEffect
        navController.navigate(Routes.KEYPAD) {
            popUpTo(Routes.HOME)
            launchSingleTop = true
        }
    }

    // Same shape for a tapped call notification: the destination is the call log, and the screen
    // itself opens the actions for the caller once it gets there.
    LaunchedEffect(callLogRequest) {
        if (callLogRequest == null) return@LaunchedEffect
        navController.navigate(Routes.callLogRoute()) {
            popUpTo(Routes.HOME)
            launchSingleTop = true
        }
    }

    AdaptiveScaffold(
        windowSizeClass = windowSizeClass,
        selectedIndex = selectedSection,
        onNavigate = { index ->
            val destination = sectionRoutes.getOrElse(index) { Routes.HOME }
            val isRootDestination =
                currentRoute in sectionRoutes || (currentRoute?.startsWith("call_log") == true && destination.startsWith("call_log"))
            if (index == selectedSection && isRootDestination) return@AdaptiveScaffold
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
                val homeViewModel = koinViewModel<HomeViewModel>()
                val state by homeViewModel.state.collectAsState()
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    homeViewModel.onIntent(HomeIntent.Refresh)
                }
                HomeScreen(
                    state = state,
                    onNavigateToCallLog = { navController.navigate(Routes.callLogRoute()) { launchSingleTop = true } },
                    onNavigateToCallLogToday = { navController.navigate(Routes.callLogRoute("today")) { launchSingleTop = true } },
                    onNavigateToCallLogThisWeek = { navController.navigate(Routes.callLogRoute("week")) { launchSingleTop = true } },
                    onNavigateToCallLogThisMonth = { navController.navigate(Routes.callLogRoute("month")) { launchSingleTop = true } },
                    onNavigateToCallLogReview = { navController.navigate(Routes.callLogRoute("review")) { launchSingleTop = true } },
                    onNavigateToBlockList = { navController.navigate(Routes.BLOCK_LIST) { launchSingleTop = true } },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    onNavigateToStats = { navController.navigate(Routes.STATS) { launchSingleTop = true } },
                    onToggleBlocking = { homeViewModel.onIntent(HomeIntent.ToggleBlocking(it)) },
                    dialerRoleHeld = dialerRoleHeld,
                    notificationsPermissionGranted = notificationsPermissionGranted,
                    fullScreenIntentAllowed = fullScreenIntentAllowed,
                    callPhonePermissionGranted = callPhonePermissionGranted,
                    onRequestDialerRole = onRequestDialerRole ?: {},
                    onOpenNotificationSettings = onOpenNotificationSettings ?: {},
                    onOpenFullScreenIntentSettings = onOpenFullScreenIntentSettings ?: {},
                    onOpenAppSettings = onOpenAppSettings ?: {},
                )
            }

            composable(Routes.KEYPAD) {
                val keypadViewModel = koinViewModel<KeypadViewModel>()
                val keypadState by keypadViewModel.state.collectAsState()
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    keypadViewModel.onIntent(KeypadIntent.RefreshContacts)
                }
                KeypadScreen(
                    dialRequest = dialRequest,
                    onCall = onCallBack ?: {},
                    contacts = keypadState.contacts,
                    contactsPermissionGranted = keypadState.contactsPermissionGranted,
                    onRequestContactsPermission = onRequestContactsPermission ?: {},
                )
            }

            composable(
                route = Routes.CALL_LOG,
                arguments = listOf(navArgument("filter") { type = NavType.StringType }),
            ) { backStackEntry ->
                val callLogViewModel = koinViewModel<CallLogViewModel>()
                val blockListViewModel = koinViewModel<BlockListViewModel>()
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    callLogViewModel.onIntent(CallLogIntent.RefreshContactNames)
                }

                // Read through SavedState, not by casting to Map. `arguments` has never been a
                // Map -- the compiler said so ("this cast can never succeed") and the `as?` then
                // swallowed it into null, so every entry point fell through to "all". Home's
                // "Blocked today", "This week", "This month" and "Pending review" tiles all
                // navigated to a filtered route and all landed on the unfiltered list.
                val filter = backStackEntry.arguments?.read { getStringOrNull("filter") } ?: "all"
                LaunchedEffect(filter) {
                    callLogViewModel.onIntent(CallLogIntent.SetFilter(filter))
                }
                val state by callLogViewModel.state.collectAsState()
                // The live rule set, so a row can say whether the caller is on a list right now
                // and offer the action that is actually left to take. Comes from
                // BlockListViewModel, which already owns every rule read and write this screen
                // needs -- the alternative was a second RuleRepository dependency in
                // CallLogViewModel and two independent copies of the same flows.
                val blockListState by blockListViewModel.state.collectAsState()
                CallLogScreen(
                    entries = state.entries,
                    blockedNumbers = blockListState.blockedNumbers,
                    allowlistedNumbers = blockListState.allowlistedNumbers,
                    onUnblockNumber = { id -> blockListViewModel.onIntent(BlockListIntent.RemoveBlockedNumber(id)) },
                    onRemoveFromAllowlist = { id -> blockListViewModel.onIntent(BlockListIntent.RemoveAllowlistedNumber(id)) },
                    // The ViewModel's filter, not the route argument: the screen's own date chips
                    // change it, and the route argument would stay at whatever the user arrived
                    // with, leaving the wrong chip selected from the first tap onwards.
                    filter = state.filter,
                    onSelectTimeFilter = { callLogViewModel.onIntent(CallLogIntent.SetFilter(it)) },
                    contactNames = state.contactNames,
                    onBlockNumber = { number -> blockListViewModel.onIntent(BlockListIntent.AddBlockedNumber(number)) },
                    onAllowlistNumber = { number -> blockListViewModel.onIntent(BlockListIntent.AddAllowlistedNumber(number)) },
                    onCopyNumber = onCopyNumber ?: {},
                    onCallBack = onCallBack ?: {},
                    onBack = { navController.popBackStack() },
                    onPlayRecording = onPlayRecording ?: {},
                    onDeleteRecording = { entryId -> callLogViewModel.onIntent(CallLogIntent.DeleteRecording(entryId)) },
                    callLogRequest = callLogRequest,
                )
            }

            composable(Routes.STATS) {
                val statsViewModel = koinViewModel<StatsViewModel>()
                val state by statsViewModel.state.collectAsState()
                StatsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.BLOCK_LIST) {
                val blockListViewModel = koinViewModel<BlockListViewModel>()
                val state by blockListViewModel.state.collectAsState()
                BlockListHubScreen(
                    blockedCount = state.blockedCount,
                    allowlistedCount = state.allowlistedCount,
                    patternCount = state.patternCount,
                    countryCount = state.countryCount,
                    scheduleCount = state.scheduleCount,
                    actionCount = state.actionCount,
                    onNavigateToManual = { navController.navigate(Routes.MANUAL_BLOCK_LIST) { launchSingleTop = true } },
                    onNavigateToAllowlist = { navController.navigate(Routes.ALLOWLIST) { launchSingleTop = true } },
                    onNavigateToPatterns = { navController.navigate(Routes.PATTERNS) { launchSingleTop = true } },
                    onNavigateToCountries = { navController.navigate(Routes.COUNTRIES) { launchSingleTop = true } },
                    onNavigateToSchedules = { navController.navigate(Routes.SCHEDULES) { launchSingleTop = true } },
                    onNavigateToActionRules = { navController.navigate(Routes.ACTION_RULES) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.MANUAL_BLOCK_LIST) {
                val blockListViewModel = koinViewModel<BlockListViewModel>()
                val state by blockListViewModel.state.collectAsState()
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    blockListViewModel.onIntent(BlockListIntent.RefreshContactNames)
                }
                ManualBlockListScreen(
                    numbers = state.blockedNumbers,
                    allowlistedNumbers = state.allowlistedNumbers.map { it.number }.toSet(),
                    contactNames = state.contactNames,
                    onAdd = { number, label -> blockListViewModel.onIntent(BlockListIntent.AddBlockedNumber(number, label)) },
                    onRemove = { id -> blockListViewModel.onIntent(BlockListIntent.RemoveBlockedNumber(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ALLOWLIST) {
                val blockListViewModel = koinViewModel<BlockListViewModel>()
                val state by blockListViewModel.state.collectAsState()
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    blockListViewModel.onIntent(BlockListIntent.RefreshContactNames)
                }
                AllowlistScreen(
                    numbers = state.allowlistedNumbers,
                    blockedNumbers = state.blockedNumbers.map { it.number }.toSet(),
                    contactNames = state.contactNames,
                    onAdd = { number, label -> blockListViewModel.onIntent(BlockListIntent.AddAllowlistedNumber(number, label)) },
                    onRemove = { id -> blockListViewModel.onIntent(BlockListIntent.RemoveAllowlistedNumber(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PATTERNS) {
                val blockListViewModel = koinViewModel<BlockListViewModel>()
                val state by blockListViewModel.state.collectAsState()
                PatternRuleScreen(
                    patterns = state.patternRules,
                    onAdd = { pattern, label -> blockListViewModel.onIntent(BlockListIntent.AddPatternRule(pattern, label)) },
                    onToggle = { id, enabled -> blockListViewModel.onIntent(BlockListIntent.TogglePatternRule(id, enabled)) },
                    onRemove = { id -> blockListViewModel.onIntent(BlockListIntent.RemovePatternRule(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.COUNTRIES) {
                val blockListViewModel = koinViewModel<BlockListViewModel>()
                val state by blockListViewModel.state.collectAsState()
                CountryRuleScreen(
                    countries = state.countryRules,
                    onAdd = { code, name -> blockListViewModel.onIntent(BlockListIntent.AddCountryRule(code, name)) },
                    onToggle = { id, enabled -> blockListViewModel.onIntent(BlockListIntent.ToggleCountryRule(id, enabled)) },
                    onRemove = { id -> blockListViewModel.onIntent(BlockListIntent.RemoveCountryRule(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SCHEDULES) {
                val blockListViewModel = koinViewModel<BlockListViewModel>()
                val state by blockListViewModel.state.collectAsState()
                ScheduleRuleScreen(
                    rules = state.scheduleRules,
                    onAdd = { label, start, end -> blockListViewModel.onIntent(BlockListIntent.AddScheduleRule(label, start, end)) },
                    onToggle = { id, enabled -> blockListViewModel.onIntent(BlockListIntent.ToggleScheduleRule(id, enabled)) },
                    onRemove = { id -> blockListViewModel.onIntent(BlockListIntent.RemoveScheduleRule(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ACTION_RULES) {
                val blockListViewModel = koinViewModel<BlockListViewModel>()
                val state by blockListViewModel.state.collectAsState()
                ActionRuleScreen(
                    rules = state.actionRules,
                    patterns = state.patternRules,
                    onAdd = { label, attempts, window, patternId ->
                        blockListViewModel.onIntent(BlockListIntent.AddActionRule(label, attempts, window, patternId))
                    },
                    onToggle = { id, enabled -> blockListViewModel.onIntent(BlockListIntent.ToggleActionRule(id, enabled)) },
                    onRemove = { id -> blockListViewModel.onIntent(BlockListIntent.RemoveActionRule(id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                val settingsViewModel = koinViewModel<SettingsViewModel>()
                val settingsUiState by settingsViewModel.state.collectAsState()
                SettingsScreen(
                    state = settingsUiState,
                    showGrantContacts = onRequestContactsPermission != null && !contactsPermissionGranted,
                    dialerRoleHeld = dialerRoleHeld,
                    notificationsPermissionGranted = notificationsPermissionGranted,
                    fullScreenIntentAllowed = fullScreenIntentAllowed,
                    callPhonePermissionGranted = callPhonePermissionGranted,
                    onRequestDialerRole = onRequestDialerRole ?: {},
                    onSetBlockingEnabled = { settingsViewModel.onIntent(SettingsIntent.SetBlockingEnabled(it)) },
                    onSetAutoAllowContacts = { settingsViewModel.onIntent(SettingsIntent.SetAutoAllowContacts(it)) },
                    onSetDefaultAction = { settingsViewModel.onIntent(SettingsIntent.SetDefaultAction(it)) },
                    onSetSpamEnabled = { settingsViewModel.onIntent(SettingsIntent.SetSpamEnabled(it)) },
                    onSetNotificationsEnabled = { settingsViewModel.onIntent(SettingsIntent.SetNotificationsEnabled(it)) },
                    onSetNotifyUnknownCallers = { settingsViewModel.onIntent(SettingsIntent.SetNotifyUnknownCallers(it)) },
                    onSetRepeatedCallerBypassCount = { settingsViewModel.onIntent(SettingsIntent.SetRepeatedCallerBypassCount(it)) },
                    onSetEmergencyCallbackExemption = {
                        settingsViewModel.onIntent(SettingsIntent.SetEmergencyCallbackExemption(it))
                    },
                    onRequestContactsPermission = onRequestContactsPermission ?: {},
                    onOpenNotificationSettings = onOpenNotificationSettings ?: {},
                    onOpenFullScreenIntentSettings = onOpenFullScreenIntentSettings ?: {},
                    onOpenAppSettings = onOpenAppSettings ?: {},
                    onNavigateToAutoResponder = { navController.navigate(Routes.AUTO_RESPONDER) { launchSingleTop = true } },
                    onNavigateToBackup = { navController.navigate(Routes.BACKUP) { launchSingleTop = true } },
                    onNavigateToPrivacy = { navController.navigate(Routes.PRIVACY_POLICY) { launchSingleTop = true } },
                    onNavigateToTerms = { navController.navigate(Routes.TERMS_CONDITIONS) { launchSingleTop = true } },
                    onNavigateToCredits = { navController.navigate(Routes.CREDITS) { launchSingleTop = true } },
                    appVersion = koinInject<AppVersion>(),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.AUTO_RESPONDER) {
                val autoResponderViewModel = koinViewModel<AutoResponderViewModel>()
                val autoResponderUiState by autoResponderViewModel.state.collectAsState()
                AutoResponderScreen(
                    state = autoResponderUiState,
                    onSetEnabled = { autoResponderViewModel.onIntent(AutoResponderIntent.SetEnabled(it)) },
                    onSetScript = { autoResponderViewModel.onIntent(AutoResponderIntent.SetScript(it)) },
                    onSetRecordingEnabled = { autoResponderViewModel.onIntent(AutoResponderIntent.SetRecordingEnabled(it)) },
                    onPickAudio = {
                        onPickAudio?.invoke { uri -> autoResponderViewModel.onIntent(AutoResponderIntent.SetAudioUri(uri)) }
                    },
                    onClearAudio = { autoResponderViewModel.onIntent(AutoResponderIntent.ClearAudioUri) },
                    onTest = {
                        onTestGreeting?.invoke(autoResponderUiState.config.script, autoResponderUiState.config.audioUri)
                    },
                    onBack = { navController.popBackStack() },
                    micPermissionGranted = micPermissionGranted,
                    onRequestMicPermission = onRequestMicPermission ?: {},
                )
            }

            composable(Routes.BACKUP) {
                val backupViewModel = koinViewModel<BackupViewModel>()
                BackupScreen(
                    effect = backupViewModel.effect,
                    onExport = { backupViewModel.onIntent(BackupIntent.Export) },
                    onImport = {
                        onPickImportFile?.invoke { content ->
                            backupViewModel.onIntent(BackupIntent.Import(content))
                        }
                    },
                    onExportReady = { json -> onShareFile?.invoke(json) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PRIVACY_POLICY) {
                InfoScreen(
                    title = stringResource(Res.string.settings_privacy_title),
                    body = stringResource(Res.string.privacy_policy_body),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.TERMS_CONDITIONS) {
                InfoScreen(
                    title = stringResource(Res.string.settings_terms_title),
                    body = stringResource(Res.string.terms_conditions_body),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.CREDITS) {
                CreditsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
