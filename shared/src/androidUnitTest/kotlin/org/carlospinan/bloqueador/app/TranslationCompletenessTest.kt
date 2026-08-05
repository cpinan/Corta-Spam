package org.carlospinan.bloqueador.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every user-facing string exists in all four shipped locales, in both resource systems.
 *
 * This project has two entirely separate string tables — Compose Multiplatform resources under
 * `shared/src/commonMain/composeResources` for screens, and Android resources under
 * `androidApp/src/main/res` for notifications and other non-Composable call sites — and adding a
 * key to only one locale file is invisible: the build succeeds and those users silently read
 * English. That has already happened once in this repo (`app_name`), which is why it's a test
 * and not a convention.
 *
 * Android Lint's own `MissingTranslation` check is disabled in `androidApp/build.gradle.kts`
 * precisely because it only sees one of the two systems.
 */
class TranslationCompletenessTest {
    private val locales = listOf("values-es", "values-hi", "values-pt")

    private fun repoRoot(): File {
        // Tests run with a module directory as the working directory; walk up to the root.
        var dir = File(".").absoluteFile
        while (dir.parentFile != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return dir
    }

    private fun keysIn(file: File): Set<String> {
        if (!file.exists()) return emptySet()
        val text = file.readText()
        return Regex("""<(?:string|plurals) name="([^"]+)"""")
            .findAll(text)
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun assertComplete(resourceRoot: File) {
        assertTrue(resourceRoot.isDirectory, "resource root not found: $resourceRoot")
        val defaultKeys = keysIn(File(resourceRoot, "values/strings.xml"))
        assertTrue(defaultKeys.isNotEmpty(), "no default-locale strings found under $resourceRoot")

        for (locale in locales) {
            val translated = keysIn(File(resourceRoot, "$locale/strings.xml"))
            val missing = (defaultKeys - translated).sorted()
            assertEquals(
                emptyList(),
                missing,
                "$locale is missing ${missing.size} key(s) from ${resourceRoot.name} and would fall back to English",
            )
            // The reverse direction matters too: a key only present in a translation is dead
            // weight nobody will ever see, usually the residue of a rename.
            val orphaned = (translated - defaultKeys).sorted()
            assertEquals(emptyList(), orphaned, "$locale has key(s) with no default-locale entry")
        }
    }

    @Test
    fun composeMultiplatformResourcesAreCompleteInEveryLocale() {
        assertComplete(File(repoRoot(), "shared/src/commonMain/composeResources"))
    }

    @Test
    fun androidAppResourcesAreCompleteInEveryLocale() {
        assertComplete(File(repoRoot(), "androidApp/src/main/res"))
    }

    @Test
    fun theSharedModulesAndroidResourcesAreCompleteInEveryLocale() {
        assertComplete(File(repoRoot(), "shared/src/androidMain/res"))
    }
}
