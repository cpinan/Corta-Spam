---
name: corta-spam-add-setting
description: Use when adding a new persisted user setting (a new field on SettingsRepository) in the Corta Spam app. Lists the exact fallout of files that must change — every implementer/fake breaks otherwise.
metadata:
  type: project-runbook
  version: "1.1.0"
---

# Add a new SettingsRepository field — Corta Spam

Adding a field to `SettingsRepository` (`shared/src/commonMain/kotlin/org/carlospinan/bloqueador/app/settings/SettingsRepository.kt`) is an interface change — every implementer must be updated in the same pass or the build breaks. This project uses hand-written fakes (no MockK), but there is now exactly one of them per repository interface, under `shared/src/commonTest/kotlin/.../testing/`.

## 1. Storage primitive (only if the value type is new)

`db/KeyValueSettingsStore.kt` currently has `readBool`/`writeBool` and `readInt`/`writeInt`. Reuse these; only add a new `read*`/`write*` pair if the setting isn't a bool/int/string.

## 2. The interface + real implementation

- `shared/.../settings/SettingsRepository.kt` — add the `Flow<T>` (or `StateFlow<T>` if Android Telecom/BroadcastReceiver code needs synchronous `.value` access outside a coroutine — check whether the setting is read from `PassthroughInCallService`/`CallActionReceiver` first) and the `suspend fun set...(...)`.
- `shared/.../settings/SqlSettingsRepository.kt` — back it with a new string key, a sane default, same `MutableStateFlow` + init-hydration pattern as the existing fields.

## 3. Fallout — every one of these needs the new member added or the build fails

Confirmed via `grep -rl "SettingsRepository" shared androidApp --include="*.kt"` — re-run that grep to catch any new implementer added since this was written:

- `shared/src/commonMain/kotlin/.../settings/SettingsViewModel.kt` — thread into `SettingsUiState`. **If `combine()` is already at 5 flows** (kotlinx.coroutines' typed overload ceiling), don't force a 6-arg vararg combine — chain a second `.combine(newFlow) { partial, value -> ... }` on top of the existing combine's result, like `repeatedCallerBypassCount` did.
- `shared/src/commonMain/kotlin/.../home/HomeViewModel.kt` (if it reads settings)
- `shared/src/commonMain/kotlin/.../rules/domain/EvaluateIncomingCallUseCase.kt` (if the rule engine needs it)
- `shared/src/commonMain/kotlin/.../onboarding/DialerOnboardingViewModel.kt`
- `androidApp/.../telecom/PassthroughInCallService.kt`
- **One** test fake: `shared/src/commonTest/kotlin/.../testing/FakeSettingsRepository.kt`. Add the flow as a constructor parameter with a default (so no existing call site breaks) and a write-through setter, matching the other fields. `testing/FakeSettingsRepositoryTest.kt` pins that contract — extend it if the new default matters. Every ViewModel and screen test shares this one fake; there are no per-test copies left.

## 4. UI

`SettingsScreen.kt` — new toggle/control, gated visibility pattern already used for `autoAllowContacts`'s contacts-permission gate. New Compose strings needed — use `corta-spam-add-localized-string` skill for that part (all 4 locales).

## 5. If the setting feeds a `RuleDecision`/precedence change

Also touches `RuleDecision.kt` (new variant or field) and `RulePrecedenceResolver.kt`'s `ResolveContext` — that likely means a new `rule_type` CHECK value too. Use `corta-spam-sqldelight-check-migration` skill for that part.

## 6. Verify

Run `corta-spam-verify-build` (Android trio + iOS compile since this always touches `commonMain`).
