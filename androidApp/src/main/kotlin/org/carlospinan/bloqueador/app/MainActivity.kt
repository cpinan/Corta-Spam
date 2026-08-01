package org.carlospinan.bloqueador.app

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderViewModel
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingScreen
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingViewModel
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import org.carlospinan.bloqueador.app.welcome.WelcomeScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val viewModel: DialerOnboardingViewModel by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val autoResponderViewModel: AutoResponderViewModel by inject()

    private val roleRequestLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            viewModel.onRequestResult(granted = result.resultCode == RESULT_OK)
        }

    private val audioPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.toString()?.let {
                scope.launch { autoResponderViewModel.setAudioUri(it) }
            }
        }

    private val contactsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ -> refreshPermissionStatus() }

    private val notificationsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ -> refreshPermissionStatus() }

    private var pendingCallBackNumber: String? = null

    private val callPhonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            refreshPermissionStatus()
            val number = pendingCallBackNumber
            pendingCallBackNumber = null
            if (granted && number != null) {
                placeCall(number)
            }
        }

    private var importResultCallback: ((String) -> Unit)? = null

    private val importFileLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let {
                contentResolver.openInputStream(it)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    importResultCallback?.invoke(text)
                }
            }
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var notificationsPermissionGranted by mutableStateOf(true)
    private var fullScreenIntentAllowed by mutableStateOf(true)
    private var callPhonePermissionGranted by mutableStateOf(true)
    private var contactsPermissionGranted by mutableStateOf(true)

    private fun refreshPermissionStatus() {
        contactsPermissionGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        notificationsPermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        fullScreenIntentAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        callPhonePermissionGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissionStatus()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsPermissionGranted) {
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val showWelcome = !settingsRepository.welcomeShown

        setContent {
            var welcomeDone by remember { mutableStateOf(!showWelcome) }
            val scopeCompose = rememberCoroutineScope()

            if (!welcomeDone) {
                WelcomeScreen(
                    onGetStarted = {
                        scopeCompose.launch { settingsRepository.setWelcomeShown() }
                        welcomeDone = true
                    },
                )
            } else {
                DialerOnboardingScreen(
                    viewModel = viewModel,
                    onRequestRole = ::launchDefaultDialerRequest,
                    content = {
                        App(
                            onPickAudio = { audioPickerLauncher.launch("audio/*") },
                            onRequestContactsPermission = {
                                contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                            },
                            onShareFile = { json ->
                                val shareIntent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                startActivity(Intent.createChooser(shareIntent, "Share backup"))
                            },
                            onPickImportFile = { onResult ->
                                importResultCallback = onResult
                                importFileLauncher.launch("application/json")
                            },
                            onCallBack = { number ->
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        Manifest.permission.CALL_PHONE,
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    placeCall(number)
                                } else {
                                    pendingCallBackNumber = number
                                    callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            },
                            onCopyNumber = { number ->
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("phone_number", number))
                            },
                            contactsPermissionGranted = contactsPermissionGranted,
                            notificationsPermissionGranted = notificationsPermissionGranted,
                            fullScreenIntentAllowed = fullScreenIntentAllowed,
                            callPhonePermissionGranted = callPhonePermissionGranted,
                            onOpenNotificationSettings = {
                                startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
                                )
                            },
                            onOpenFullScreenIntentSettings = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    startActivity(
                                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")),
                                    )
                                }
                            },
                            onOpenAppSettings = {
                                startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
                                )
                            },
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        refreshPermissionStatus()
    }

    private fun placeCall(number: String) {
        if (number.isBlank()) return
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:${number.trim()}")
        try {
            startActivity(callIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No activity to handle call intent for number", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "CALL_PHONE permission denied at call time", e)
        }
    }

    private fun launchDefaultDialerRequest() {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
        roleRequestLauncher.launch(intent)
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
