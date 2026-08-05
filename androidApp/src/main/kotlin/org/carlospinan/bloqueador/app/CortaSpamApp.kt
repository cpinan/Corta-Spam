package org.carlospinan.bloqueador.app

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.di.initKoin
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import org.carlospinan.bloqueador.app.telecom.IncomingCallNotifier
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class CortaSpamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@CortaSpamApp)
        }
        IncomingCallNotifier.createChannel(this)

        // SqlSettingsRepository reads its values from SQLite in its constructor, and Koin builds
        // singletons lazily on first injection. Without this the first thing to touch it is
        // PassthroughInCallService.onCallAdded, on the main thread, while a call is ringing.
        // Building it here moves that disk read to process start, off the latency-critical path.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { get<SettingsRepository>() }
                .onFailure { Log.w(TAG, "Could not warm the settings repository", it) }
        }
    }

    private companion object {
        const val TAG = "CortaSpamApp"
    }
}
