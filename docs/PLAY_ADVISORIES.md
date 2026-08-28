# Play Console Advisories

The Console's **App quality → Recommended actions** panel raises advisories against a release the
moment its bundle is processed, before and independently of policy review. They are static analysis
over the uploaded artifact plus the target SDK, not review findings, and **nothing here blocks a
release**: an advisory left unactioned has never delayed a rollout of this app.

They recur. The same four came back on release 9 (1.6.1) that will come back on 10, because three of
them are properties of the SDK level and of a library this app is *told* to use. This file exists so
they are triaged once, with the evidence written down, instead of re-derived per release.

Advisories are per release name, so each new upload restates the ones it still qualifies for. A
verdict below stays valid until the thing it rests on changes — the code, the dependency version, or
the advisory text.

---

## How to check a trace before believing it

Two of the four advisories name code locations. On a minified build those arrive obfuscated
(`c.w.b`, `a4.b.t`) and read as if they were ours. **They usually are not.** R8's mapping file
answers it exactly, and the answer takes a minute:

```sh
# The mapping for the build that was uploaded. Check the mtime against the upload date first —
# a later local rebuild renames every class, and then the lookup silently answers about a
# different binary.
ls -l androidApp/build/outputs/mapping/release/mapping.txt

# `c.w.b` means class `c.w`, method `b`. Resolve the classes:
grep -nE "^[a-zA-Z0-9_.$]+ -> (c\.w|c\.y|a4\.b):$" androidApp/build/outputs/mapping/release/mapping.txt

# Then read the member lines under that class to find the method, and the original
# source file and line numbers come with them.
sed -n '<line>,<line+40>p' androidApp/build/outputs/mapping/release/mapping.txt
```

The mapping is ~44 MB, so `grep -n` then `sed` a window; do not open it.

Two traps in that lookup. **The mapping must belong to the uploaded bundle** — R8 names are not
stable across builds, so a mapping from a rebuild will resolve `c.w` to some unrelated class and the
answer will look authoritative. Check the file's timestamp against the upload date. And **an R8
synthetic outline is named after whichever class it was first outlined from, not the class that
calls it**: `a4.b` below carries the name `androidx.emoji2.text.ConcurrencyHelpers$…` while the call
site is in `androidx.activity`. The class name in a synthetic is not evidence of the caller.

Uploading the mapping to the Console (Play does accept a deobfuscation file) would make Play print
these names itself. Not done today; the recipe above is what stands in for it.

---

## Release 9 (1.6.1) — triaged 2026-08-28

| # | Advisory | Verdict |
|---|---|---|
| 1 | Edge-to-edge may not display for all users | **Already done.** No change. |
| 2 | Deprecated APIs/parameters for edge-to-edge | **Library-internal.** Not ours to fix. |
| 3 | Implement picture-in-picture | **Not applicable.** Dismiss. |
| 4 | Improve R8 with AGP 9.0+ | **Real, deferred.** Its own migration. |

### 1. Edge-to-edge may not display for all users

> From Android 15, apps targeting SDK 35 will display edge-to-edge by default. […] Alternatively,
> call `enableEdgeToEdge()` for Kotlin.

Already called, in both activities the manifest declares — there are only two:

- [`MainActivity.kt:250`](../androidApp/src/main/kotlin/org/carlospinan/bloqueador/app/MainActivity.kt)
- [`InCallActivity.kt:49`](../androidApp/src/main/kotlin/org/carlospinan/bloqueador/app/telecom/InCallActivity.kt)

And the insets are consumed rather than assumed, which is the half of edge-to-edge that actually
breaks screens:

- `safeDrawingPadding()` — `CallScreen`, `PermissionsOnboardingScreen`, `ScrollableScreen`
- `WindowInsets.safeDrawing.only(…)` — `AdaptiveScaffold`, three call sites
- `WindowInsets.safeDrawing.asPaddingValues()` — `KeypadScreen:182`

This advisory fires on `targetSdk` alone. It is not evidence that anything is wrong.

**What would change this verdict:** a new Activity that does not call `enableEdgeToEdge`, or a
screen that draws content without a `safeDrawing`-derived padding. Both are visible on a device — a
status bar overlapping content on a phone with a cutout is what the advisory is warning about.

### 2. Deprecated APIs or parameters for edge-to-edge

> `android.view.Window.setStatusBarColor`, `android.view.Window.setNavigationBarColor`,
> `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` […] start in `c.w.b`, `c.y.b`, `a4.b.t`.

Resolved against the 1.6.1 mapping (`mapping.txt`, stamped 2026-08-27 12:30, the bundle that was
uploaded that day):

| Play trace | Real code |
| --- | --- |
| `c.w.b` | `androidx.activity.EdgeToEdgeApi26.setUp(…)` — `EdgeToEdge.kt:281-286` |
| `c.y.b` | `androidx.activity.EdgeToEdgeApi29.setUp(…)` — `EdgeToEdge.kt:314-321` |
| `a4.b.t` | R8 outline of `void m(WindowManager.LayoutParams)`, called from `EdgeToEdgeApi28.adjustLayoutInDisplayCutoutMode` — `EdgeToEdge.kt:296` |

All three are **inside `enableEdgeToEdge()` itself**: its own API-26/28/29 back-compat path, which
sets the bar colours and the cutout mode on the versions of Android that have no other way to do it.
The call Play recommends in advisory 1 is the call that produces advisory 2.

Nothing in `androidApp/` or `shared/` touches these APIs. A grep over every `.kt` and `.xml` in both
source trees for `setStatusBarColor`, `setNavigationBarColor`, `DisplayCutout`, `cutout`,
`statusBarColor` and `navigationBarColor` returns nothing — including the two `themes.xml` files.

Unfixable at this layer without dropping `enableEdgeToEdge()`, which would be trading a cosmetic
advisory for a real one. It clears when androidx.activity stops using the deprecated calls, not
before. Currently `androidx-activityCompose = "1.13.0"` in `gradle/libs.versions.toml`.

**What would change this verdict:** a trace that resolves to a class under
`org.carlospinan.bloqueador`. Re-run the mapping lookup on each release rather than assuming the
traces are the same three — the obfuscated names *will* differ build to build even when the cause
does not.

### 3. Implement picture-in-picture

A call blocker with no video surface. PiP shows a video in a corner while the user does something
else; there is nothing here to put in it. The in-call screen's equivalent already exists and is not
PiP — an ongoing-call notification, plus `InCallState` so returning to the app lands back on the
live call (see `InCallActivity`'s `BackHandler`, which backgrounds rather than finishes).

Not applicable. It is offered to every app, with a "Peers MAU" figure attached, and being in the
minority here is correct.

### 4. R8 optimisation — upgrade AGP to 9.0+

The only advisory with real work behind it. Current: `agp = "8.13.2"` in
[`gradle/libs.versions.toml`](../gradle/libs.versions.toml).

Deferred deliberately. AGP 9 is a migration rather than a version bump — built-in Kotlin support
removed, KMP plugin handling changed — and this build is a KMP one on Kotlin 2.2.20, Compose
Multiplatform 1.11.1 and SQLDelight 2.3.2, each of which has to be known-good against AGP 9 before
the bump means anything. Doing it on top of a release already in review buys nothing: the payoff is
memory and startup, not correctness, and R8 is already on (`isMinifyEnabled`, `isShrinkResources`,
`proguard-android-optimize.txt`) in `androidApp/build.gradle.kts`.

**When to do it:** as its own change, on its own commit, with `./scripts/verify.sh` green and a
release build installed on hardware afterwards — R8 changes behaviour around reflection,
serialization and Telecom callbacks, which is exactly this app's load-bearing surface. Not the day
before an upload.

---

## Re-triage checklist for the next release

1. Read the Recommended actions panel for the new release name.
2. Anything matching 1–4 above: no work, unless its "what would change this verdict" line now holds.
3. Anything new that names a code location: resolve it against that build's `mapping.txt` **before**
   deciding it is ours.
4. Record the verdict here, dated, with the evidence — not in a commit message, which nobody reads
   at the next upload.
