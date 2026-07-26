package org.carlospinan.bloqueador.app

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingScreen
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val viewModel: DialerOnboardingViewModel by inject()

    private val roleRequestLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            viewModel.onRequestResult(granted = result.resultCode == RESULT_OK)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DialerOnboardingScreen(
                viewModel = viewModel,
                onRequestRole = ::launchDefaultDialerRequest,
                content = { App() },
            )
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
