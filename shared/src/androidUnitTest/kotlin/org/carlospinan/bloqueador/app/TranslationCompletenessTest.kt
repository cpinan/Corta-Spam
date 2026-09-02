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

    /**
     * Every Compose Multiplatform string that takes an argument spells it positionally.
     *
     * Compose Multiplatform does not format these strings; it substitutes them, with one regex —
     * `%(\d+)\$[ds]`, verified in the shipped `classes.dex` of 1.6.1. A bare `%d` or `%s` matches
     * nothing, so it is not an argument at all: it is printed to the user, literally, as `%d`.
     * That shipped for five releases in `call_repeated_caller_hint`, in all four locales — the
     * one live caller of the whole mechanism.
     *
     * Nothing else catches it. The key is present, the specifier parity check above accepts both
     * spellings by design (it is comparing a translation against its source, and both sides had
     * the same wrong one), the build succeeds, and the string only appears on a screen you need a
     * repeat spam caller to reach.
     *
     * Android resources are deliberately not checked here: `Context.getString` is a real
     * formatter and `%d` works correctly there.
     */
    @Test
    fun composeMultiplatformFormatArgumentsArePositional() {
        val root = File(repoRoot(), "shared/src/commonMain/composeResources")
        val offenders = mutableListOf<String>()

        for (locale in listOf("values") + locales) {
            val file = File(root, "$locale/strings.xml")
            for ((key, body) in bodiesOf(file, "string") + bodiesOf(file, "plurals")) {
                // A doubled %% is a literal percent sign, so blank those out before looking for
                // a specifier -- "50%%" must not read as an implicit one.
                val implicit = Regex("""%[sdf]""").findAll(body.replace("%%", "")).map { it.value }.toList()
                if (implicit.isNotEmpty()) {
                    offenders += "$locale/$key uses ${implicit.joinToString()} — write %1\$d / %1\$s instead"
                }
            }
        }

        assertEquals(emptyList(), offenders.sorted(), "implicit format specifiers reach the user verbatim")
    }

    @Test
    fun theSharedModulesAndroidResourcesAreCompleteInEveryLocale() {
        assertComplete(File(repoRoot(), "shared/src/androidMain/res"))
    }

    /**
     * Format specifiers in a translation, in the order a formatter would consume them.
     *
     * Positional (`%1$s`) and implicit (`%s`) are both collected because both appear in this
     * repo, and `%%` is deliberately excluded — it is a literal percent sign, not an argument.
     */
    private fun specifiersIn(value: String): List<String> = Regex("""%(?:\d+\$)?[sdf]""").findAll(value).map { it.value }.toList()

    private fun bodiesOf(
        file: File,
        tag: String,
    ): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return Regex("""<$tag name="([^"]+)"[^>]*>(.*?)</$tag>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun stringsIn(file: File) = bodiesOf(file, "string")

    /** All quantity forms of a plural, concatenated — see [assertSpecifiersMatch] for why. */
    private fun pluralsIn(file: File) = bodiesOf(file, "plurals")

    /**
     * A translation must take the same arguments as the string it translates.
     *
     * Key parity above says a translator answered; this says they answered the same question.
     * A specifier the source does not have throws `MissingFormatArgumentException` at format time,
     * and one the translation drops loses the value silently. Neither is visible in anything else
     * this project runs: the build succeeds, the key is present, and Lint's own check is disabled
     * here for the reason in the class doc.
     *
     * **Plain strings are compared strictly**, as sorted multisets — a legitimate reordering like
     * `%1$s (%2$s)` to `%2$s de %1$s` still passes, a dropped or invented argument does not.
     *
     * **Plurals are compared as distinct sets across all their quantity forms**, and that
     * looseness is deliberate rather than a weaker test. Which forms carry the count is a property
     * of the language, not of the message: English says "Called once" for `one` and uses `%1$d`
     * only in `other`, while Portuguese ("Ligou %1$d vez") and Hindi both use it in every form.
     * Comparing per-form, or as multisets, fails all three of those correct translations — the
     * first version of this test did exactly that. What must still hold is that no locale
     * introduces an argument the source never had, and none loses the only one it did.
     */
    private fun assertSpecifiersMatch(resourceRoot: File) {
        val defaultStrings = stringsIn(File(resourceRoot, "values/strings.xml"))
        val defaultPlurals = pluralsIn(File(resourceRoot, "values/strings.xml"))
        val mismatches = mutableListOf<String>()

        for (locale in locales) {
            val localeFile = File(resourceRoot, "$locale/strings.xml")
            val translatedStrings = stringsIn(localeFile)
            val translatedPlurals = pluralsIn(localeFile)

            for ((key, english) in defaultStrings) {
                val other = translatedStrings[key] ?: continue // key parity is asserted above
                val expected = specifiersIn(english).sorted()
                val actual = specifiersIn(other).sorted()
                if (expected != actual) {
                    mismatches += "$locale/$key: expected $expected, found $actual"
                }
            }

            for ((key, english) in defaultPlurals) {
                val other = translatedPlurals[key] ?: continue
                val expected = specifiersIn(english).toSortedSet()
                val actual = specifiersIn(other).toSortedSet()
                if (expected != actual) {
                    mismatches += "$locale/$key (plural): expected $expected, found $actual"
                }
            }
        }

        assertEquals(emptyList(), mismatches.sorted(), "format specifiers differ from the default locale")
    }

    @Test
    fun composeResourcesTakeTheSameFormatArgumentsInEveryLocale() {
        assertSpecifiersMatch(File(repoRoot(), "shared/src/commonMain/composeResources"))
    }

    @Test
    fun androidAppResourcesTakeTheSameFormatArgumentsInEveryLocale() {
        assertSpecifiersMatch(File(repoRoot(), "androidApp/src/main/res"))
    }
}
