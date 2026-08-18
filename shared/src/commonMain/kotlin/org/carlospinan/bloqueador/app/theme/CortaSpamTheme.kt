package org.carlospinan.bloqueador.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The app's colours, in both schemes.
 *
 * Until now every screen opened a bare `MaterialTheme { }`, which is Material 3's *baseline*
 * palette — a light purple, in light mode, unconditionally. So the app had no dark mode at all:
 * not a partial one, not a wrong one, none. That matters more here than in most apps, because the
 * one screen this app draws without being asked is the full-screen incoming-call screen, and it
 * arrives at whatever hour someone rings.
 *
 * The values are the ones already designed in `design/mockups.html`, transcribed rather than
 * re-invented: two schemes that were drawn together and checked against each other. The mockups
 * are the reference for the app, not a separate artefact — if one of these ever has to change,
 * change it there first.
 *
 * Green is doing semantic work, not only brand work: [ColorScheme.primary] is the "allowed" colour
 * in the call log, which is why the accent is green in *both* schemes rather than inverting to
 * something else in the dark one.
 */
private object Palette {
    // --- light (mockups.html :root) ---
    val LightBackground = Color(0xFFF4F5F2) // --bg
    val LightSurface = Color(0xFFFFFFFF) // --surface
    val LightSurfaceAlt = Color(0xFFEEF0EA) // --surface-alt
    val LightBorder = Color(0xFFE2E4DD) // --border
    val LightBorderStrong = Color(0xFFCBCFC5) // --border-strong
    val LightTextPrimary = Color(0xFF1A1C1A) // --text-primary
    val LightTextSecondary = Color(0xFF5C6058) // --text-secondary
    val LightAccent = Color(0xFF146B57) // --accent
    val LightAccentBg = Color(0xFFDCEEE7) // --accent-bg
    val LightAccentStrong = Color(0xFF0E5747) // --accent-strong
    val LightDanger = Color(0xFFA3392B) // --danger
    val LightDangerBg = Color(0xFFF8E4DF) // --danger-bg
    val LightWarning = Color(0xFF8A5A0A) // --warning
    val LightWarningBg = Color(0xFFFBEBD2) // --warning-bg

    // --- dark (mockups.html html[data-theme="dark"]) ---
    val DarkBackground = Color(0xFF121312)
    val DarkSurface = Color(0xFF1C1F1D)
    val DarkSurfaceAlt = Color(0xFF242725)
    val DarkBorder = Color(0xFF33362F)
    val DarkBorderStrong = Color(0xFF464A41)
    val DarkTextPrimary = Color(0xFFEDEEE9)
    val DarkTextSecondary = Color(0xFFA6AA9F)
    val DarkAccent = Color(0xFF4FC79E)
    val DarkAccentBg = Color(0xFF153630)
    val DarkAccentStrong = Color(0xFF7FE0BB)
    val DarkDanger = Color(0xFFE48575)
    val DarkDangerBg = Color(0xFF3B241D)
    val DarkWarning = Color(0xFFE3B457)
    val DarkWarningBg = Color(0xFF3A2C12)

    val White = Color(0xFFFFFFFF)
    val Scrim = Color(0xFF000000)
}

/**
 * Deliberately explicit about `surfaceContainer*`. Material 3 derives a Card's background from
 * that family, and leaving them at the baseline defaults would have put a lilac card on a green
 * app — the failure mode is not a crash, it is a screen that looks like two designs.
 */
val CortaSpamLightColors: ColorScheme =
    lightColorScheme(
        primary = Palette.LightAccent,
        onPrimary = Palette.White,
        primaryContainer = Palette.LightAccentBg,
        onPrimaryContainer = Palette.LightAccentStrong,
        secondary = Palette.LightAccentStrong,
        onSecondary = Palette.White,
        secondaryContainer = Palette.LightAccentBg,
        onSecondaryContainer = Palette.LightAccentStrong,
        tertiary = Palette.LightWarning,
        onTertiary = Palette.White,
        tertiaryContainer = Palette.LightWarningBg,
        onTertiaryContainer = Palette.LightWarning,
        error = Palette.LightDanger,
        onError = Palette.White,
        errorContainer = Palette.LightDangerBg,
        onErrorContainer = Palette.LightDanger,
        background = Palette.LightBackground,
        onBackground = Palette.LightTextPrimary,
        surface = Palette.LightSurface,
        onSurface = Palette.LightTextPrimary,
        surfaceVariant = Palette.LightSurfaceAlt,
        onSurfaceVariant = Palette.LightTextSecondary,
        surfaceContainerLowest = Palette.White,
        surfaceContainerLow = Palette.White,
        surfaceContainer = Palette.LightSurfaceAlt,
        surfaceContainerHigh = Palette.LightSurfaceAlt,
        surfaceContainerHighest = Palette.LightBorder,
        outline = Palette.LightBorderStrong,
        outlineVariant = Palette.LightBorder,
        inverseSurface = Palette.DarkSurface,
        inverseOnSurface = Palette.DarkTextPrimary,
        inversePrimary = Palette.DarkAccent,
        scrim = Palette.Scrim,
    )

val CortaSpamDarkColors: ColorScheme =
    darkColorScheme(
        primary = Palette.DarkAccent,
        // The dark accent is a bright mint; text on top of it has to be near-black, and the
        // palette's own dark accent background is exactly that colour.
        onPrimary = Palette.DarkAccentBg,
        primaryContainer = Palette.DarkAccentBg,
        onPrimaryContainer = Palette.DarkAccentStrong,
        secondary = Palette.DarkAccentStrong,
        onSecondary = Palette.DarkAccentBg,
        secondaryContainer = Palette.DarkSurfaceAlt,
        onSecondaryContainer = Palette.DarkTextPrimary,
        tertiary = Palette.DarkWarning,
        onTertiary = Palette.DarkWarningBg,
        tertiaryContainer = Palette.DarkWarningBg,
        onTertiaryContainer = Palette.DarkWarning,
        error = Palette.DarkDanger,
        onError = Palette.DarkDangerBg,
        errorContainer = Palette.DarkDangerBg,
        onErrorContainer = Palette.DarkDanger,
        background = Palette.DarkBackground,
        onBackground = Palette.DarkTextPrimary,
        surface = Palette.DarkSurface,
        onSurface = Palette.DarkTextPrimary,
        surfaceVariant = Palette.DarkSurfaceAlt,
        onSurfaceVariant = Palette.DarkTextSecondary,
        surfaceContainerLowest = Palette.DarkBackground,
        surfaceContainerLow = Palette.DarkSurface,
        surfaceContainer = Palette.DarkSurfaceAlt,
        surfaceContainerHigh = Palette.DarkSurfaceAlt,
        surfaceContainerHighest = Palette.DarkBorder,
        outline = Palette.DarkBorderStrong,
        outlineVariant = Palette.DarkBorder,
        inverseSurface = Palette.LightTextPrimary,
        inverseOnSurface = Palette.LightSurface,
        inversePrimary = Palette.LightAccent,
        scrim = Palette.Scrim,
    )

/**
 * Wraps a screen in the app's colours. Every screen calls this instead of `MaterialTheme { }`.
 *
 * Each screen themes *itself* rather than relying on one wrapper at the top of the navigation
 * graph, and that is not redundancy: `InCallActivity` draws `CallScreen` outside the nav host
 * entirely, and every Robolectric test renders a single screen with no host at all. A theme
 * applied only at the root would have left exactly the screen that most needs a dark mode — the
 * one that opens by itself while the phone rings — on the baseline palette.
 *
 * [darkTheme] follows the system by default. There is no in-app light/dark switch: Android has
 * had a system-wide one since 10, and a second control that can disagree with it is a support
 * question, not a feature.
 */
@Composable
fun CortaSpamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) CortaSpamDarkColors else CortaSpamLightColors,
        content = content,
    )
}
