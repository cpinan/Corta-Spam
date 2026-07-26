package org.carlospinan.bloqueador.app.onboarding

import android.content.Context
import android.telecom.TelecomManager

class AndroidDefaultDialerGateway(
    private val context: Context,
) : DefaultDialerGateway {
    override fun isDefaultDialer(): Boolean {
        val telecomManager =
            context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                ?: return false
        return telecomManager.defaultDialerPackage == context.packageName
    }
}
