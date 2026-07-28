# Landscape / Tablet Support + Remaining Tasks

## Status before this phase
M0-M10 implemented (rules engine, all rule types, call screening, auto-responder,
stats, bundled spam, backup/restore). Android APK builds. All unit tests pass.
Pending on-device QA. iOS deferred.

---

## Phase A: Adaptive Architecture (Foundation) — ~3 sessions

### A1. Window size detection
- 3-tier enum: `Compact` (<600dp), `Medium` (600–839dp), `Expanded` (>=840dp)
- `rememberWindowSizeClass()` composable using `BoxWithConstraints`
- **New file**: `shared/.../adaptive/WindowSizeClass.kt`

### A2. Adaptive content wrapper
- `AdaptiveContent` composable — caps max-width at 600dp, centers content
- **New file**: `shared/.../adaptive/AdaptiveContent.kt`

### A3. NavigationSuiteScaffold integration
- Replace bare `NavHost` in `AppNavHost` with `NavigationSuiteScaffold`
- 4 navigation items: Home | Call Log | Block Lists | Settings
- `Compact` (<600dp): bottom navigation bar
- `Medium` (600–839dp): navigation rail (narrow, 76px, icons only)
- `Expanded` (>=840dp): navigation rail (wide, 216px, icons + labels)
- Back button on detail screens pops within scaffold
- **Files**: `AppNavHost.kt`, `App.kt`

---

## Phase B: Screen Adaptations — ~3 sessions

### B1. Home screen
- `Compact`: single column stats, 2-col quick grid
- `Medium`/`Expanded`: two-column layout (stats+recent on left, quick grid on right), 4-col quick grid
- **File**: `HomeScreen.kt`

### B2. Call log (list-detail)
- `Compact`/`Medium`: full-screen list + bottom sheet dialog for detail
- `Expanded`: two-pane — list on left (340px), detail on right (persistent)
- Detail shows: number, timestamp, action, rule detail, block/allowlist/copy buttons
- **File**: `CallLogScreen.kt`

### B3. Block List screens
- Hub: `FlowRow` grid — 2 cols Compact, 4 cols Medium/Expanded
- All list screens (Manual, Allowlist, Patterns, Countries, Actions, Schedules):
  wrap in `AdaptiveContent` (capped width, centered)
- **File**: `BlockListScreens.kt`

### B4. Settings screen
- `Compact`/`Medium`: single pane, items navigate to subsections
- `Expanded`: two-pane — settings list (340px) + selected detail pane on right
- **File**: `SettingsScreen.kt`

### B5. Remaining screens
- Stats, Auto-responder, Backup, Welcome, Onboarding: wrap in `AdaptiveContent`
- **Files**: `StatsScreen.kt`, `AutoResponderScreen.kt`, `BackupScreen.kt`,
  `WelcomeScreen.kt`, `OnboardingScreens.kt`

---

## Phase C: Android Config — ~0.5 session

- Add `android:configChanges="orientation|screenSize|screenLayout"` to activities
  in `AndroidManifest.xml` (Compose handles recomposition, avoid activity recreate)
- Verify no hardcoded orientation lock
- **File**: `AndroidManifest.xml`

---

## Phase D: Remaining Tasks — ~2 sessions

### D1. On-device QA
- Run `docs/QA_M2_M8_MANUAL.md` on phone + tablet/emulator
- Verify edge-to-edge on both orientations + all screen sizes
- Fix regressions

### D2. M11 — Store compliance
- Google Play declaration form draft
- Privacy policy doc (spec §3: no telemetry, opt-in network only)
- App Store review notes (iOS placeholder)
- **New file**: `docs/STORE_COMPLIANCE.md`

### D3. Update MILESTONES.md
- Mark M2-M10 complete, adaptive layout phase, M11 status

---

## Execution Order
```
A1 → A2 → A3 → C → B1 → B2 → B3 → B4 → B5 → D1 → D2 → D3
```

## Estimated: 8-9 sessions total

## Key Decisions

| Decision | Choice |
|---|---|
| Size tiers | 3-tier: Compact <600dp, Medium 600–839dp, Expanded >=840dp |
| Navigation pattern | `NavigationSuiteScaffold` (bottom bar / nav rail / wide rail) |
| Medium nav rail | Narrow (76px), icons only |
| Expanded nav rail | Wide (216px), icons + labels |
| Content on wide | Max-width 600dp, centered |
| List-detail | Call log: bottom sheet on Compact/Medium, persistent pane on Expanded |
| Settings on tablet | Single pane on Medium, list-detail split on Expanded |
| Grid layouts | `FlowRow` 2-col Compact, 4-col Medium/Expanded (Home, BlockListHub) |

## Not in scope (deferred)
- iOS `CallDirectoryProvider` extension
- iOS nav gating (hide Android-only screens)
- Export file encryption
- Recorded caller message playback UI
