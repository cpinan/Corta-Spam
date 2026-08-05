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
./scripts/verify.sh
```

That is the whole sequence, in one place, so it cannot drift between this skill, CI and whoever
is typing. It runs: both compiles, `ktlintCheck`, both test modules, `verifySqlDelightMigration`,
**both Android Lint tasks**, and the iOS compile.

`--fast` skips iOS and release for a tight edit loop; `--release` adds `assembleRelease`. Neither
is acceptable as the final check before a commit that touches `commonMain`.

Everything must pass. `ktlintCheck` catches import-ordering issues often introduced when adding new resource/string imports (e.g. `Res.string.foo_bar` inserted out of alphabetical order) — if it fails, fix the import position, don't disable the rule. `./gradlew ktlintFormat` fixes ordering automatically.

**Android Lint is in the sequence and is not optional.** It could not run at all until 2026-08-05
— AGP 8.7.3's bundled UAST frontend reads Kotlin 2.0 metadata and died on this project's 2.2.20
output with `Module was compiled with an incompatible version of Kotlin`. That is a toolchain
mismatch, **not** something to suppress: if it ever reappears after a Kotlin bump, raise AGP
rather than disabling lint. On its first working run it found three missing permission guards, a
dead pre-API-26 branch, a `StateFlow.value` read inside composition, and plural forms that are
wrong specifically in Hindi and Portuguese — none of which ktlint or the tests look for.

## 2. If the change touched anything under `shared/src/commonMain`

```
./gradlew :shared:compileKotlinIosSimulatorArm64
```

Non-optional. `commonMain` code that only ever ran on Android historically compiled fine there and broke on iOS silently until this step is run (e.g. `kotlinx.datetime.Clock.System` doesn't resolve on iOS the same way; this project's fix pattern is the existing `currentTimeMillis()` expect/actual, not reaching for `Clock.System` again).

## 3. Do NOT install or launch the app

Unless the user has explicitly asked for a device/simulator run in *this* turn, stop at compile/test/lint. This project has a standing "do not install until I say it" instruction — code, build, test, lint only. See project memory `user-preferences` if unsure whether that's still in effect.

When the user *has* asked, use `./scripts/device_check.sh` rather than a bare install: it also
pulls the database back off the phone and asserts the schema version and indexes. A green test
suite says the code is self-consistent; it does not say the database on a real phone is the shape
you think it is. That distinction has already cost this project one bad commit — see
`corta-spam-sqldelight-check-migration`.

## 4. If the change touched the SQLDelight schema or migrations

`./scripts/verify.sh` already includes it, precisely because it is **not** in the normal
`build`/`check` graph and nothing else triggers it. To run it alone:

```
./gradlew :shared:verifySqlDelightMigration
```

Replays every `.sqm` on top of the baseline snapshot `shared/src/commonMain/sqldelight/databases/1.db` and diffs against `AppDatabase.sq`.

It proves the migration chain reaches the right *schema*. It does **not** prove the migration
files are numbered correctly — a mis-numbered `.sqm` whose migration still runs passes this
check. `./scripts/device_check.sh` is what covers that. It is wired into the `jvm-android` CI job, so a mismatch fails the pipeline.

Never make a failure here disappear by regenerating the baseline (`generateCommonMainAppDatabaseSchema` rewrites it from the current `.sq`, hiding any diff). See `corta-spam-sqldelight-check-migration` for the migration rules.

## 5. Renaming the project

`rootProject.name` is not cosmetic. Compose Multiplatform derives the generated resources package from it, so a rename changes `Res` imports across every screen file. The package is now pinned in `shared/build.gradle.kts` via `compose.resources { packageOfResClass = ... }` — keep it pinned, and change that string only alongside a matching import sweep.
