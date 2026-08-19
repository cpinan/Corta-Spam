package org.carlospinan.bloqueador.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.adaptive.AdaptiveScaffold
import org.carlospinan.bloqueador.app.adaptive.WindowSizeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The app had no dark mode at all: every screen opened a bare `MaterialTheme { }`, which is
 * Material 3's baseline light palette regardless of the system setting. Not a partial dark mode,
 * not a wrong one — none. The screen that suffers most is the full-screen incoming-call screen,
 * which the app draws without being asked, at whatever hour someone rings.
 *
 * Two different things are checked here, and the split is the point:
 *
 * - The **schemes** are what [CortaSpamTheme] resolves, asserted directly.
 * - The **wiring** — that screens actually use it — is asserted by reading the source, because
 *   the obvious runtime test does not work and the plausible one is a lie. `captureToImage()`
 *   times out under Robolectric (`PixelCopy` never redraws), and the first version of this test
 *   read a `CortaSpamTheme { }` block placed *next to* `CallScreen`; it passed against a
 *   deliberately reverted `CallScreen`, because it was asking the theme what the theme says.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CortaSpamThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun schemeUnder(darkTheme: Boolean): Pair<Color, Color> {
        var background = Color.Unspecified
        var onBackground = Color.Unspecified
        composeTestRule.setContent {
            CortaSpamTheme(darkTheme = darkTheme) {
                background = MaterialTheme.colorScheme.background
                onBackground = MaterialTheme.colorScheme.onBackground
                Surface { Text("probe") }
            }
        }
        composeTestRule.waitForIdle()
        return background to onBackground
    }

    @Test
    fun `the light scheme is the palette the mockups define`() {
        val (background, onBackground) = schemeUnder(darkTheme = false)

        assertEquals(Color(0xFFF4F5F2), background)
        assertEquals(Color(0xFF1A1C1A), onBackground)
    }

    @Test
    fun `the dark scheme is actually dark`() {
        val (background, onBackground) = schemeUnder(darkTheme = true)

        assertEquals(Color(0xFF121312), background)
        // A positive assertion, not "differs from light": a scheme rebuilt wrongly would also
        // differ from light while being unreadable.
        assertEquals(Color(0xFFEDEEE9), onBackground)
        assertTrue(background.luminance() < onBackground.luminance())
    }

    /**
     * Every screen must open [CortaSpamTheme], never `MaterialTheme { }`.
     *
     * A theme applied once at the root of the navigation graph would not be enough, which is why
     * this is checked per file rather than in one place: `InCallActivity` draws `CallScreen`
     * outside the nav host entirely, and every Robolectric screen test renders one screen with no
     * host at all. A screen that themes itself is correct in all three settings.
     *
     * `MaterialTheme.` (colours, typography) is untouched by this and stays everywhere.
     */
    @Test
    fun `no screen opens the Material baseline theme`() {
        val root = File(repoRoot(), "shared/src/commonMain/kotlin")
        assertTrue(root.isDirectory, "common source root not found: $root")

        val offenders =
            root
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    file.readLines().withIndex().mapNotNull { (index, line) ->
                        // Comments are prose about the rule, not breaches of it: the theme's own
                        // KDoc names the thing it replaced, and AdaptiveScaffold explains why
                        // being absent from this list did not make it correct. Both are skipped,
                        // or the check reports the sentence describing the bug as the bug.
                        val trimmed = line.trimStart()
                        if (trimmed.startsWith("*") || trimmed.startsWith("//")) return@mapNotNull null
                        if (line.contains("MaterialTheme {")) "${file.name}:${index + 1}" else null
                    }
                }.toList()

        assertEquals(
            emptyList(),
            offenders,
            "these screens render on Material 3's baseline palette and have no dark mode; " +
                "use CortaSpamTheme { }",
        )
    }

    private fun repoRoot(): File {
        var dir = File(".").absoluteFile
        while (dir.parentFile != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return dir
    }

    private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

    /**
     * The chrome around the screens has to be themed too, and the source scan above cannot say so.
     *
     * [AdaptiveScaffold] draws the navigation bar and rail. It opened *no* theme at all, so it was
     * never a `MaterialTheme { }` offender and the scan passed it — while it inherited Material 3's
     * baseline light palette from the composition root. On a Pixel 8 Pro API 36 emulator with the
     * system in dark mode, the release build drew dark content above a white navigation bar.
     *
     * This is a runtime assertion rather than another source scan, and it works because the
     * scaffold composes `content()` *inside* whatever theme it opened: probing the colour scheme
     * from the content slot reports what the bar beside it is drawn with. A scaffold that opens no
     * theme reports the light background here.
     */
    @Test
    @Config(sdk = [34], qualifiers = "night")
    fun `the scaffold themes the navigation chrome it draws`() {
        var background = Color.Unspecified
        composeTestRule.setContent {
            AdaptiveScaffold(
                windowSizeClass = WindowSizeClass.Compact,
                selectedIndex = 0,
                onNavigate = {},
            ) {
                background = MaterialTheme.colorScheme.background
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(Color(0xFF121312), background)
    }
}
