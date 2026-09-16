# RELEASE — Corta Spam

`org.carlospinan.cortaspam` · versionCode `10` · versionName `1.6.2` · track: production
_Checked 2026-09-16 against the built artefact and the user's own confirmation of upload._

## Next action

Watch the Play Console review outcome for 10 (1.6.2); re-triage the four recurring advisories
against this build's `mapping.txt` once the Console resolves the obfuscated trace names.

## Stage

production-review

## Done

- [x] Version bumped to 10 (1.6.2) — versionCode confirmed free on the Console before the bump.
- [x] Bundle built: `androidApp/build/outputs/bundle/release/10-1.6.2-release.aab`
      (4,972,097 bytes, signing cert `4CA33D30…46E0`, same upload key as 9).
- [x] Manifest audited via `bundletool dump manifest` — 7 permissions, unchanged from 9, targetSdk
      36.
- [x] Installed and launched on physical hardware (razr 50 ultra, `ZY22JZJDNH`).
- [x] Release notes written in all four locales — `docs/store/RELEASE_NOTES_1.6.2.md`.
- [x] Three on-device verification rounds (DND-while-ringing, call-waiting/hang-up-block,
      full-screen gate) ran and passed 2026-09-16.
- [x] Four recurring Play Console advisories re-triaged against 10 — no new work, see
      `docs/PLAY_ADVISORIES.md`.
- [x] "No debug symbols" upload warning triaged — not fixable, third-party stripped `.so`, see
      `docs/PLAY_ADVISORIES.md`.
- [x] Uploaded to production 2026-09-16 — versionCode 10 is now spent.

## Blocked / waiting

- Play review outcome for 10 (1.6.2), submitted 2026-09-16.

## Left

- [ ] Re-triage the four advisories against 10's actual `mapping.txt` once obfuscated trace names
      are visible in the Console — `docs/PLAY_ADVISORIES.md` checklist.
- [ ] Add pt-BR and hi-IN to the store listing itself so their release notes reach anyone —
      `docs/STORE_LISTING.md:395`.
