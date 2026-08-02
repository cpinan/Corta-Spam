---
name: corta-spam-add-localized-string
description: Use when adding a new user-facing string to the Corta Spam app. This project has TWO separate resource systems that both need every string, across FOUR locales — easy to update only one and ship an English fallback by accident.
metadata:
  type: project-runbook
  version: "1.0.0"
---

# Add a localized string — Corta Spam

Corta Spam supports 4 locales: **en (default), es, hi, pt**. There are **two separate, unrelated resource systems** — a string used in only one of them will silently fall back to English (or crash, for the native side, if referenced without a default). Always check which system the call site needs before adding anywhere.

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

## System 2 — Native Android resources

Path: `androidApp/src/main/res/values{,-es,-hi,-pt}/strings.xml`

Use for: anything read from non-suspend, non-Composable call sites — `BroadcastReceiver`s, `Call.Callback`, `NotificationCompat` builders, `Service` classes (e.g. `IncomingCallNotifier.kt`, `CallActionReceiver.kt`, `PassthroughInCallService.kt`). These need synchronous `context.getString()`, which Compose resources don't give you outside a coroutine/Composable.

Same rule: add the key to all 4 locale files under `androidApp/src/main/res/`.

## Checklist

1. Identify which system the call site needs (Composable → System 1, Android component/callback → System 2). If unsure, check how a similar existing string near that call site is referenced.
2. Add the key + English string first.
3. Add es/hi/pt translations to the same key in the other 3 locale files of the *same* system — don't skip a locale even if the translation is rough; a missing locale entry falls back to English for that locale's users, which has been the actual bug this project fixes proactively (e.g. `app_name` was only in `values/` for es/hi/pt until this was caught).
4. Run `corta-spam-verify-build`.
