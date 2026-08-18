package org.carlospinan.bloqueador.app.settings

import platform.Foundation.NSBundle

/**
 * The iOS equivalent of the Android package read: `CFBundleShortVersionString` is the marketing
 * version a user quotes, `CFBundleVersion` the build number App Store Connect increments.
 *
 * `CFBundleVersion` is a string on iOS and only conventionally a number, so a bundle that uses
 * the dotted form ("1.4.0") has no integer to report and falls back to zero rather than throwing
 * on a settings screen.
 */
fun readAppVersion(): AppVersion {
    val bundle = NSBundle.mainBundle
    val name = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    val code = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
    return AppVersion(
        name = name?.ifBlank { null } ?: AppVersion.Unknown.name,
        code = code?.toLongOrNull() ?: AppVersion.Unknown.code,
    )
}
