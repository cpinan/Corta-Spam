package org.carlospinan.bloqueador.app.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

/**
 * Reads the version out of the installed package, which is the one copy that cannot disagree with
 * the APK the user is running.
 *
 * `PackageInfoCompat.getLongVersionCode` rather than the deprecated `versionCode` field: from
 * API 28 the code is a long, and reading the int field truncates it.
 *
 * A package manager that cannot find the app's own package is not a state worth crashing the
 * settings screen over — it happens while an install is being replaced — so it degrades to
 * [AppVersion.Unknown].
 */
fun Context.readAppVersion(): AppVersion =
    try {
        val info = packageManager.getPackageInfo(packageName, 0)
        AppVersion(
            name = info.versionName.orEmpty().ifBlank { AppVersion.Unknown.name },
            code = PackageInfoCompat.getLongVersionCode(info),
        )
    } catch (_: PackageManager.NameNotFoundException) {
        AppVersion.Unknown
    }
