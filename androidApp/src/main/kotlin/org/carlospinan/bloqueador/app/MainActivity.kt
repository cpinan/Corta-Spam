package org.carlospinan.bloqueador.app

import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
        ) { _ -> }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                                Toast.makeText(this@MainActivity, "Dialing $number", Toast.LENGTH_SHORT).show()
                                val dialIntent = Intent(Intent.ACTION_DIAL)
                                dialIntent.data = Uri.parse("tel:${number.trim()}")
                                startActivity(dialIntent)
                            },
                            onCopyNumber = { number ->
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("phone_number", number))
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
}
