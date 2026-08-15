package org.carlospinan.bloqueador.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compose Multiplatform string resources must not carry Android's `\'` escape.
 *
 * The two string tables in this project look identical and are not. Under `androidApp/src/main/res`
 * aapt2 parses the file and `\'` is the *required* spelling of an apostrophe. Under
 * `shared/src/commonMain/composeResources` the Compose resource loader reads it as plain XML text,
 * where a backslash means nothing — so the escape survives into the UI and the user reads
 * `your phone\'s loudspeaker`.
 *
 * Found by screenshotting the auto-responder screen on a device, which also revealed that
 * `blocklist_duplicate_blocked_body` had been shipping `won\'t override the block` in the
 * duplicate-number dialog. Nothing failed: it compiles, `TranslationCompletenessTest` only counts
 * keys, and Android Lint cannot see this tree at all. Only a human looking at the pixels could
 * catch it, which is why it is a test now.
 *
 * A bare `'` is valid XML and renders correctly, so the fix is always to delete the backslash.
 */
class ComposeStringEscapingTest {
    private fun repoRoot(): File {
        var dir = File(".").absoluteFile
        while (dir.parentFile != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return dir
    }

    @Test
    fun noComposeStringCarriesAnAndroidApostropheEscape() {
        val root = File(repoRoot(), "shared/src/commonMain/composeResources")
        assertTrue(root.isDirectory, "compose resource root not found: $root")

        val offenders =
            root
                .walkTopDown()
                .filter { it.isFile && it.name == "strings.xml" }
                .flatMap { file ->
                    file.readLines().withIndex().mapNotNull { (index, line) ->
                        // Only the apostrophe escape. `\n` is handled by the loader and is used
                        // deliberately (welcome_subtitle), so it must not be reported here.
                        if (line.contains("\\'")) "${file.parentFile.name}/${file.name}:${index + 1}" else null
                    }
                }.toList()

        assertEquals(
            emptyList(),
            offenders,
            "these lines would render a literal backslash to the user; delete the backslash",
        )
    }
}
