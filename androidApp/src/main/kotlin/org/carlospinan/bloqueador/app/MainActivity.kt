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
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingIntent
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingScreen
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingViewModel
import org.carlospinan.bloqueador.app.permissions.AppPermission
import org.carlospinan.bloqueador.app.permissions.PermissionsOnboardingScreen
import org.carlospinan.bloqueador.app.permissions.permissionChecklist
import org.carlospinan.bloqueador.app.telecom.AutoResponderAudio
import org.carlospinan.bloqueador.app.telecom.RecordingPlayer
import org.carlospinan.bloqueador.app.welcome.WelcomeScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val viewModel: DialerOnboardingViewModel by inject()

    /** Lazily created so a plain app launch (no auto-responder test tapped) never touches TTS/MediaPlayer. */
    private val testAudioLazy = lazy { AutoResponderAudio(this) }
    private val testAudio get() = testAudioLazy.value

    /** Same reasoning: nothing is constructed until a recording is actually played. */
    private val recordingPlayerLazy = lazy { RecordingPlayer() }
    private val recordingPlayer get() = recordingPlayerLazy.value

    private val roleRequestLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            viewModel.onIntent(DialerOnboardingIntent.RequestResult(granted = result.resultCode == RESULT_OK))
        }

    private var audioPickResultCallback: ((String) -> Unit)? = null

    private val audioPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.toString()?.let { audioPickResultCallback?.invoke(it) }
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

    private val micPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ -> refreshPermissionStatus() }

    private var notificationsPermissionGranted by mutableStateOf(true)
    private var fullScreenIntentAllowed by mutableStateOf(true)
    private var callPhonePermissionGranted by mutableStateOf(true)
    private var contactsPermissionGranted by mutableStateOf(true)

    /**
     * Only consulted once the user turns auto-responder recording on. Deliberately never
     * requested at launch: a dialer asking for the microphone on first run, for a feature that
     * is off by default, reads as overreach and gets denied permanently.
     */
    private var micPermissionGranted by mutableStateOf(true)

    /**
     * Launches the system dialog for one row of the onboarding checklist. Microphone is absent
     * on purpose: [AppPermission.MICROPHONE] is marked non-requestable there, so the screen
     * never calls this for it -- it is asked for at the moment recording is switched on.
     */
    private fun requestPermission(permission: AppPermission) {
        when (permission) {
            AppPermission.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

            AppPermission.CONTACTS -> contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            AppPermission.PHONE -> callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            AppPermission.MICROPHONE -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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
        micPermissionGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissionStatus()

        setContent {
            val state by viewModel.state.collectAsState()

            if (!state.welcomeShown) {
                WelcomeScreen(
                    onGetStarted = { viewModel.onIntent(DialerOnboardingIntent.WelcomeShown) },
                )
            } else {
                DialerOnboardingScreen(
                    state = state,
                    onIntent = viewModel::onIntent,
                    onRequestRole = ::launchDefaultDialerRequest,
                    content = {
                        if (!state.permissionsPromptShown) {
                            PermissionsOnboardingScreen(
                                items =
                                    permissionChecklist(
                                        notificationsGranted = notificationsPermissionGranted,
                                        contactsGranted = contactsPermissionGranted,
                                        phoneGranted = callPhonePermissionGranted,
                                        micGranted = micPermissionGranted,
                                        notificationsApplicable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                                    ),
                                onRequest = ::requestPermission,
                                onContinue = { viewModel.onIntent(DialerOnboardingIntent.PermissionsPromptShown) },
                            )
                        } else {
                            App(
                                onPickAudio = { onResult ->
                                    audioPickResultCallback = onResult
                                    audioPickerLauncher.launch("audio/*")
                                },
                                onTestGreeting = { script, audioUri ->
                                    testAudio.play(script, audioUri.ifBlank { null }, onComplete = {})
                                },
                                onRequestContactsPermission = {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                },
                                onShareFile = { json ->
                                    val shareIntent =
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                    startActivity(
                                        Intent.createChooser(shareIntent, getString(R.string.share_backup_chooser_title)),
                                    )
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
                                            Intent(
                                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                                "package:$packageName".toUri(),
                                            ),
                                        )
                                    }
                                },
                                onOpenAppSettings = {
                                    startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()),
                                    )
                                },
                                micPermissionGranted = micPermissionGranted,
                                onRequestMicPermission = {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                onPlayRecording = { path -> recordingPlayer.play(path) },
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onIntent(DialerOnboardingIntent.Refresh)
        refreshPermissionStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (testAudioLazy.isInitialized()) {
            testAudio.release()
        }
        if (recordingPlayerLazy.isInitialized()) {
            recordingPlayer.stop()
        }
    }

    private fun placeCall(number: String) {
        if (number.isBlank()) return
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = "tel:${number.trim()}".toUri()
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
