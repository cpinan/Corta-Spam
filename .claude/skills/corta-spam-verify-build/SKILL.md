---
name: corta-spam-verify-build
description: Use after any code change in this repo (BloqueaLlamadas / Corta Spam KMP app) before calling work done or committing. Runs the exact Android + iOS verification sequence this project expects — not a general "run the build" reminder.
metadata:
  type: project-runbook
  version: "1.0.0"
---

# Verify build — Corta Spam

Run this after every batch of code changes, before committing. Skipping the iOS step for a `commonMain`/`shared` change has caused real shipped compile breaks in this project (`Clock.System`, `Dispatchers.IO` — Android-only checks passed, iOS didn't).

## 1. Always run (any change)

```
./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin ktlintCheck :shared:testDebugUnitTest
```

All four must pass. `ktlintCheck` catches import-ordering issues often introduced when adding new resource/string imports (e.g. `Res.string.foo_bar` inserted out of alphabetical order) — if it fails, fix the import position, don't disable the rule.

## 2. If the change touched anything under `shared/src/commonMain`

```
./gradlew :shared:compileKotlinIosSimulatorArm64
```

Non-optional. `commonMain` code that only ever ran on Android historically compiled fine there and broke on iOS silently until this step is run (e.g. `kotlinx.datetime.Clock.System` doesn't resolve on iOS the same way; this project's fix pattern is the existing `currentTimeMillis()` expect/actual, not reaching for `Clock.System` again).

## 3. Do NOT install or launch the app

Unless the user has explicitly asked for a device/simulator run in *this* turn, stop at compile/test/lint. This project has a standing "do not install until I say it" instruction — code, build, test, lint only. See project memory `user-preferences` if unsure whether that's still in effect.

## 4. Known pre-existing gap — do not treat as your bug

`./gradlew verifyCommonMainAppDatabaseMigration` (or the broader `verifyMigrations` task) currently fails with `duplicate column name` because `shared/src/commonMain/sqldelight/6.db` and `7.db` snapshot files are missing (only `1.db`-`5.db` exist). This task is `SKIPPED` in the normal `build`/`check`/`test`/`ktlintCheck` graph, so it never blocks the sequence above — confirmed via `--dry-run`. Don't silently "fix" this as a drive-by; it's tracked in project memory. If you add a new `N.sqm` migration, either regenerate the missing snapshots or explicitly flag that the gap now spans one more file.
