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

## 4. If the change touched the SQLDelight schema or migrations

```
./gradlew :shared:verifySqlDelightMigration
```

Replays every `.sqm` on top of the baseline snapshot `shared/src/commonMain/sqldelight/databases/1.db` and diffs against `AppDatabase.sq`. It is **not** in the normal `build`/`check` graph, so step 1 will not catch a schema mismatch — run it explicitly. It is wired into the `jvm-android` CI job, so a mismatch fails the pipeline.

Never make a failure here disappear by regenerating the baseline (`generateCommonMainAppDatabaseSchema` rewrites it from the current `.sq`, hiding any diff). See `corta-spam-sqldelight-check-migration` for the migration rules.

## 5. Renaming the project

`rootProject.name` is not cosmetic. Compose Multiplatform derives the generated resources package from it, so a rename changes `Res` imports across every screen file. The package is now pinned in `shared/build.gradle.kts` via `compose.resources { packageOfResClass = ... }` — keep it pinned, and change that string only alongside a matching import sweep.
