# STATUS — Corta Spam

_Last updated: 2026-08-27 · branch `main` · 2 uncommitted files (a LinkedIn draft + its .docx)_

## Next action

Fill the two `[NOMBRE]` / `[QUÉ HIZO]` placeholders at the end of `docs/LINKEDIN_POST_ES.md` and decide whether that post should carry donation links, then commit it.

## State

- **1.6.0 (versionCode 8) is live in production**, published 2026-08-21. No app code changed since; this session was documentation only.
- **GitHub Sponsors is live and listed everywhere.** Its Stripe Connect payout was approved 2026-08-27, which removes the only thing that had been blocking it. `github: cpinan` is first in `.github/FUNDING.yml`, and the Sponsor row appears in both DONATE files, both READMEs, the CHANGELOG, both privacy copies and the live pages site.
- **Monthly tiers are off on purpose**, matching Ko-fi's memberships — a recurring tier is a promise to deliver something every month and there is nothing here to deliver.
- **Nothing is asked for inside the app.** No billing code, no donation prompt, no nag screen; the entire ask lives in the repo and on the pages site. Do not add one.
- Liberapay and Open Collective remain deliberately out — see "Do not redo".
- The Sponsorships checkbox in repo **Settings → General → Features is ticked** (2026-08-27). Without it GitHub ignores `FUNDING.yml` silently — no button, no error.

## In flight

- `docs/LINKEDIN_POST_ES.md` (86 lines, untracked) + `docs/LINKEDIN_POST_ES.docx` — launch post in Spanish. Two `[NOMBRE]` / `[QUÉ HIZO]` placeholders near the end are unfilled, flagged in its own header. Unlike `docs/LINKEDIN_POST_1_6_0_ES.md` it carries **no donation links at all**; not touched this session because that may be deliberate for a launch post.
- `~/Projects/cpinan.github.io` — unrelated uncommitted work, left exactly as found: `README.md`, `bendiciones-buenos-dias/index.html`, root `index.html`, plus untracked `huellitas-al-dia/index.html`, `assets/covers/bendiciones.jpg`, `assets/icons/huellitas-al-dia.png`. Belongs to other apps on that site, not to Corta Spam.

## Verify

```bash
bash tools/verify.sh
```

Not run this session — no code changed, only Markdown, HTML and YAML.

## Open questions

- **The Sponsor button was never visually confirmed** (2026-08-27). GitHub definitely parsed the file — all four funding URLs appear in the repo page HTML, and Ko-fi/PayPal can only have come from `FUNDING.yml` — but the rendered button and its dropdown were not seen. One glance at https://github.com/cpinan/Corta-Spam settles it; expect four rows with **cpinan** first.
- Should the launch post (`LINKEDIN_POST_ES.md`) carry donation links, or stay clean? The 1.6.0 post carries all three.
- Version codes 6 and 7 are both spent. Treat the next code as unknown until the Play Console states it — 8 is published, so 9 is the expectation, not a fact.

## Do not redo

- **Do not re-litigate GitHub Sponsors' absence.** Resolved 2026-08-27; the "deliberately absent" rationale was deleted from both DONATE files and the CHANGELOG, not left standing. The old blocker was that Sponsors pays out through a Stripe Connect account whose country must match the bank account's. The bank-free workaround — a fiscal host such as Open Source Collective, 10% fee, choosable only at signup — was never needed and should not be revisited.
- **Liberapay stays out.** On Stripe it would suit recurring support, but recurring is declined on purpose, and its other rail is PayPal, which makes the donor confirm every single payment and shows donor and recipient names and emails to each other. Stripe now existing does not revive it.
- **Do not `git commit -am` in `cpinan.github.io`.** It carries unrelated dirty files from other apps; stage by explicit path or they get swallowed.
- **`FUNDING.yml` alone does not render the button** — the Settings → Features checkbox is the other half, and its absence fails silently.
- Adding an entry for an account that does not exist renders a button that 404s, which is worse than no button. Only live accounts go in that file.
