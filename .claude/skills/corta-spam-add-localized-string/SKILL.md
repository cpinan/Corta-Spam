---
name: corta-spam-add-localized-string
description: Use when adding a new user-facing string, plural, or count to the Corta Spam app. This project has THREE separate resource trees that each need every string, across FOUR locales, plus locale-specific plural rules that fail the build. Easy to update only one and ship an English fallback by accident.
metadata:
  type: project-runbook
  version: "1.0.0"
---

# Add a localized string — Corta Spam

Corta Spam supports 4 locales: **en (default), es, hi, pt**, across **three separate resource
trees**. A string added to only one of them silently falls back to English for that locale's
users. Always check which the call site needs before adding anywhere.

**`TranslationCompletenessTest` now enforces this** (`shared/src/androidUnitTest/.../TranslationCompletenessTest.kt`).
It walks all three trees in both directions — missing translations *and* keys that exist only in
a translation — and fails the build. On the day it was written it found eleven real gaps,
including the in-app privacy policy and terms, plus eight keys nothing referenced and one that
existed only in Spanish. Do not delete or weaken it to make an addition pass.

Android Lint's own `MissingTranslation` check is deliberately **disabled** in
`androidApp/build.gradle.kts`: it can only see one of the three trees, so a green lint run was
actively misleading.

## System 1 — Compose Multiplatform resources

Path: `shared/src/commonMain/composeResources/values{,-es,-hi,-pt}/strings.xml`

Use for: anything rendered from a `@Composable` (screens, dialogs) via `stringResource(Res.string.foo)`. This is what almost all UI strings use.

Add the same `<string name="...">` key to **all 4** files:
- `values/strings.xml` (English — required, this is the fallback)
- `values-es/strings.xml`
- `values-hi/strings.xml`
- `values-pt/strings.xml`

For strings with placeholders (e.g. a count), use `%1$d`/`%1$s` and call `stringResource(Res.string.foo, value)` — keep formatting in the Composable, not pre-formatted in ViewModel/domain code (this project's convention: presentation-edge formatting only).

After adding, the generated `Res.string.foo` accessor needs a rebuild (`ktlintCheck`/compile will show unresolved-reference errors if the key is missing from even one locale file — SQLDelight-style codegen requires all locale files to at least have the default-locale keys present via fallback, but ktlint import-ordering will flag it if you import the resource before it exists).

## System 3 — The shared module's Android resources

Path: `shared/src/androidMain/res/values{,-es,-hi,-pt}/strings.xml`

Use for: strings the **shared module** needs synchronously from a non-Composable, non-suspend
context. There is exactly one today — `auto_responder_default_script`, read from
`SqlAutoResponderRepository`'s constructor via `AutoResponderDefaults`, which cannot use the
Compose resource system because `getString` there is `suspend`.

Reach it with `import org.carlospinan.bloqueador.app.shared.R` (the *shared* module's R class,
not the app's). Supply the iOS side a constant in `PlatformModule.ios.kt`.

## System 2 — Native Android resources

Path: `androidApp/src/main/res/values{,-es,-hi,-pt}/strings.xml`

Use for: anything read from non-suspend, non-Composable call sites — `BroadcastReceiver`s, `Call.Callback`, `NotificationCompat` builders, `Service` classes (e.g. `IncomingCallNotifier.kt`, `CallActionReceiver.kt`, `PassthroughInCallService.kt`). These need synchronous `context.getString()`, which Compose resources don't give you outside a coroutine/Composable.

Same rule: add the key to all 4 locale files under `androidApp/src/main/res/`.

## Plurals

A count in a string is a `<plurals>`, not `%1$d` in a `<string>`. "Called 1 times" is wrong in
every locale this app ships, and Android Lint fails the build on it (`PluralsCandidate`, then
`ImpliedQuantity`).

Two traps that cost a build each:

- **In Hindi and Brazilian Portuguese the `one` category covers both 0 and 1**, so the `one` form
  must still contain the number: `%1$d बार कॉल किया`, not `एक बार कॉल किया`. Lint's message is
  `The quantity 'one' matches more than one specific number in this locale (0, 1)`.
- Add the plural to **both** the Compose tree and the Android tree if both render it — read with
  `pluralStringResource(Res.plurals.x, count, count)` and
  `resources.getQuantityString(R.plurals.x, count, count)` respectively. Note the count is passed
  twice: once to select the form, once to fill the placeholder.

## Never build sentences outside the UI

A string assembled in a ViewModel, repository or domain class cannot be localized — those layers
have no access to string resources. This project got that wrong three times over and had to undo
it: block reasons (which were also *persisted* in English, freezing the call log in whatever
language it was written in), statistics day labels, and backup result messages.

The rule: **the data layer emits data; the Composable chooses words.** If you find yourself
writing an English sentence anywhere under `commonMain` that is not a log message, stop.

Inside a `LaunchedEffect` or other coroutine, `stringResource` is unavailable — use the suspend
`org.jetbrains.compose.resources.getString(Res.string.x, args...)` instead of resolving format
strings outside the collector and substituting by hand.

## Checklist

1. Identify which system the call site needs (Composable → System 1; Android component/callback in `androidApp` → System 2; shared-module synchronous read → System 3). If unsure, check how a similar existing string near that call site is referenced.
2. Add the key + English string first.
3. Add es/hi/pt translations to the same key in the other 3 locale files of the *same* system — don't skip a locale even if the translation is rough; a missing locale entry falls back to English for that locale's users, which has been the actual bug this project fixes proactively (e.g. `app_name` was only in `values/` for es/hi/pt until this was caught).
4. Run `./scripts/verify.sh` — `TranslationCompletenessTest` and Android Lint both gate this.
