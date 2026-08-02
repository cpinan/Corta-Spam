package org.carlospinan.bloqueador.app

import android.app.Application
import org.carlospinan.bloqueador.app.di.initKoin
import org.carlospinan.bloqueador.app.telecom.IncomingCallNotifier
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
    }
}
