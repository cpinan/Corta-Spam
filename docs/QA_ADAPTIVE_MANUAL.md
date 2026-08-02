# Adaptive Layout QA — Manual Verification

Covers A1-A3 (window size detection, adaptive scaffold, content wrapper)
and B1-B5 (screen adaptations). Run on real hardware + emulator.

## Setup

- Phone device (width <600dp in portrait): run all Compact steps
- Phone device rotated to landscape (600-839dp): run all Medium steps
- Tablet emulator or device (>=840dp width): run all Expanded steps
- `adb devices` for screenshots/logcat

### Build & install

```sh
cd ~/Projects/BloqueaLlamadas
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n org.carlospinan.bloqueador.app/.MainActivity
```

---

## 1. Compact — Phone Portrait (<600dp)

### 1a. Navigation
| Step | Expect |
|---|---|
| Bottom nav bar visible | 4 items: Home (house icon), Log (list icon), Lists (lock icon), Settings (gear icon) |
| Tap Home | Home screen loads, Home item highlighted |
| Tap Log | Call log screen loads, Log item highlighted |
| Tap Lists | Block list hub loads, Lists item highlighted |
| Tap Settings | Settings screen loads, Settings item highlighted |
| Tap Home again | Returns to Home, no duplicate entries in back stack |

### 1b. Home screen
| Step | Expect |
|---|---|
| Title "Corta Spam" + blocking toggle | Visible at top |
| Stats card "Blocked today" with count | Full width, primary container color |
| Week + Month stats cards | Side by side, equal widths |
| Quick access: Call Log, Stats, Block List, Settings | 4 TextButtons stacked vertically |
| Tap "View call log" | Navigates to call log |

### 1c. Block List Hub
| Step | Expect |
|---|---|
| 6 hub cards visible | Manual, Allowlist, Patterns, Countries, Actions, Schedules |
| Cards in 2-column grid | FlowRow with 2 items per row |
| Count badges visible | Each card shows its count number |
| Tap Manual | Navigates to ManualBlockList screen |

### 1d. Call Log
| Step | Expect |
|---|---|
| List of call entries visible | Each shows number, rule/subtitle, action badge (green/red) |
| Tap a blocked entry | AlertDialog shows with Block/Allowlist/Copy buttons |
| Tap Cancel | Dialog dismisses |

### 1e. Settings
| Step | Expect |
|---|---|
| Blocking rules, Auto-responder, Spam, Backup items visible | Each as a card with title + description |
| Tap Auto-responder | Navigates to AutoResponder screen |
| Back button | Pops to Settings |

---

## 2. Medium — Phone Landscape (600–839dp)

### 2a. Navigation
| Step | Expect |
|---|---|
| Nav rail on left side, icons with small labels | Bottom bar replaced by vertical rail |
| Rail icons: Home, Log, Lists, Settings | Same icons as Compact |
| Content area fills remaining width | No letterboxing |

### 2b. Home screen: two-column layout
| Step | Expect |
|---|---|
| Left column: title + toggle + stats cards | Stats cards full-width in left column |
| Right column (220dp): quick access grid | 4 cards in 2x2 FlowRow |
| "View call log", "View stats", "Manage block lists", "Settings" | Each visible as clickable card |

### 2c. Block List Hub
| Step | Expect |
|---|---|
| 6 hub cards in 4-column grid | FlowRow with 4 items per row |

### 2d. Call Log
| Step | Expect |
|---|---|
| Full list visible | Same as Compact — no permanent detail pane |
| Tap entry → AlertDialog | Same dialog behavior |
| Content capped at 600dp, centered | Comfortable reading width |

### 2e. Settings
| Step | Expect |
|---|---|
| Single pane list, centered, capped at 600dp | Same as Compact but centered |

---

## 3. Expanded — Tablet Landscape (>=840dp)

### 3a. Navigation
| Step | Expect |
|---|---|
| Nav rail on left side, wider with labels next to icons | Icons + labels visible side by side |
| Content area fills remaining width | Full content width available |

### 3b. Home screen: two-column layout
| Step | Expect |
|---|---|
| Same two-column layout as Medium | Left: stats, Right: quick grid |
| Content capped at 600dp, centered in available space | Not full-width, centered |

### 3c. Call Log: list-detail two-pane
| Step | Expect |
|---|---|
| Left pane (~340dp): call list | Scrollable list of entries |
| Right pane: "Select a call to see details" | Placeholder text when nothing selected |
| Tap a blocked entry in list | Left: entry highlighted (primary container bg), Right: detail shows number, action tag, rule detail, action buttons |
| Tap "Allow this number" | Detail clears, toast/feedback shown |
| Tap "Copy number" | Number copied to clipboard |
| Tap a different entry | Right pane updates with new entry details |
| Tapping empty area in right pane | No crash |

### 3d. Block List Hub
| Step | Expect |
|---|---|
| 6 hub cards in 4-column FlowRow grid | Same as Medium |

### 3e. Settings
| Step | Expect |
|---|---|
| Single pane list, centered, capped at 600dp | No list-detail split (deferred) |
| Tap Auto-responder → sub-screen | Navigates inline |

### 3f. Detail screens (Stats, AutoResponder, Backup)
| Step | Expect |
|---|---|
| Content capped at 600dp, centered | Comfortable reading width, not stretched full-width |
| Back button visible on detail screens | Pops within nav, nav rail stays |

---

## 4. Rotation & Configuration Changes

| Step | Expect |
|---|---|
| On Home screen, rotate phone | No activity recreate, screen recomposes with new layout |
| Nav should match new width class | Bottom bar ↔ nav rail depending on orientation |
| On Call Log Expanded detail pane, rotate to portrait | Detail pane gone, list takes full width, dialog shows on tap |
| On detail screen (ManualBlockList), rotate | Screen stays, back button still works, nav rail updates |
| Force-stop, relaunch while in landscape | App starts in correct orientation, nav rail visible |

---

## 5. Edge-to-Edge Verification

| Step | Check |
|---|---|
| Status bar: content title not obscured | "Corta Spam" title fully visible below status bar |
| Navigation bar (3-button devices): bottom content not cut off | Nav bar items fully visible above OS nav buttons |
| Gesture nav: bottom content has safe insets | No overlap with gesture bar area |
| Notch/cutout: horizontal content not obscured | Row content clear of display cutouts |

---

## 6. Regression — Core Functionality

| Step | Expect |
|---|---|
| Blocking toggle on Home | Toggle persists across restarts |
| Add number to block list, receive call | Call blocked, call log entry created |
| Onboarding flow (fresh install) | Explainer screen → OS dialog → main app with nav |
| Auto-responder TTS | Works on blocked call (M6 check) |
| Stats screen | Loads daily breakdown |
| Backup export/import | Round-trip successful |
| App background/foreground | Home stats refresh on resume |

---

## Known Limitations

- **Settings list-detail:** deferred. Settings uses AdaptiveContent centering on all widths.
- **Home recent calls:** not implemented on dashboard. Call log serves that purpose.
- **DefaultAction.ASK** behaves identically to ALLOW (no review UI exists — known gap from M2-M8 QA).
- **Spam provider (M7):** no-op stub, toggle persists but no network behavior.
