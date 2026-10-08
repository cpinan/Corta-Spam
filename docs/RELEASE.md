# RELEASE — Corta Spam

`org.carlospinan.cortaspam` · versionCode `11` · versionName `1.7.0` · track: production (not yet uploaded)
_Checked 2026-10-08 against the built artefact, not the source manifest. 10 (1.6.2) was uploaded
2026-09-16 and its review outcome is not known to this repo._

## Next action

Upload `androidApp/build/outputs/bundle/release/11-1.7.0-release.aab` to production, paste the
release name and notes from `docs/store/RELEASE_NOTES_1.7.0.md`, and replace the listing
screenshots with `docs/store/play/` (en-US) and `docs/store/play/es_*` (es-419, the default).

## Stage

built-not-uploaded

## Done

- [x] versionCode 11 confirmed free by the maintainer on the Console, 2026-10-08 — asked, not inferred.
- [x] Bundle built and signed with the same upload key as 10 (SHA-256 `4CA33D30…46E0`), 4,981,685 bytes.
- [x] Manifest audited with `bundletool dump manifest` on the artifact: identical to 10 except the
      version fields.
- [x] `tools/verify.sh` green, 787 tests; iOS compile included.
- [x] Release (R8) build run on the API 36 emulator from a fresh install: first-run tutorial shown, no crash.
- [x] Release notes in all four locales — `docs/store/RELEASE_NOTES_1.7.0.md`.
- [x] Listing screenshots retaken, en-US and es-419, padded to 9:16.

## Blocked / waiting

- 10 (1.6.2) review outcome — unknown since 2026-09-18.

## Left

- [ ] Upload, paste notes, swap screenshots — `play-release-run` / `play-store-assets`.
- [ ] pt-BR and hi-IN still not added to the store listing itself (`docs/STORE_LISTING.md:395`).
- [ ] Re-triage the four Play advisories against this build's `mapping.txt` once visible.
- [ ] Not run on a physical device; no call-handling code changed since 1.6.2.
