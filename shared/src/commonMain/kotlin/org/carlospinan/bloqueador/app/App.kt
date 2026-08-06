package org.carlospinan.bloqueador.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import org.carlospinan.bloqueador.app.navigation.AppNavHost

@Composable
fun App(
    onPickAudio: (((String) -> Unit) -> Unit)? = null,
    onTestGreeting: ((script: String, audioUri: String) -> Unit)? = null,
    onRequestContactsPermission: (() -> Unit)? = null,
    onShareFile: ((String) -> Unit)? = null,
    onPickImportFile: (((String) -> Unit) -> Unit)? = null,
    onCallBack: ((String) -> Unit)? = null,
    onCopyNumber: ((String) -> Unit)? = null,
    contactsPermissionGranted: Boolean = false,
    notificationsPermissionGranted: Boolean = true,
    fullScreenIntentAllowed: Boolean = true,
    callPhonePermissionGranted: Boolean = true,
    onOpenNotificationSettings: (() -> Unit)? = null,
    onOpenFullScreenIntentSettings: (() -> Unit)? = null,
    onOpenAppSettings: (() -> Unit)? = null,
    micPermissionGranted: Boolean = true,
    onRequestMicPermission: (() -> Unit)? = null,
    onPlayRecording: ((String) -> Unit)? = null,
) {
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        onPickAudio = onPickAudio,
        onTestGreeting = onTestGreeting,
        onRequestContactsPermission = onRequestContactsPermission,
        onShareFile = onShareFile,
        onPickImportFile = onPickImportFile,
        onCallBack = onCallBack,
        onCopyNumber = onCopyNumber,
        contactsPermissionGranted = contactsPermissionGranted,
        notificationsPermissionGranted = notificationsPermissionGranted,
        fullScreenIntentAllowed = fullScreenIntentAllowed,
        callPhonePermissionGranted = callPhonePermissionGranted,
        onOpenNotificationSettings = onOpenNotificationSettings,
        onOpenFullScreenIntentSettings = onOpenFullScreenIntentSettings,
        onOpenAppSettings = onOpenAppSettings,
        micPermissionGranted = micPermissionGranted,
        onRequestMicPermission = onRequestMicPermission,
        onPlayRecording = onPlayRecording,
    )
}
