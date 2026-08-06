---
name: corta-spam-commit-and-docs
description: Use when the user says "commit" (with or without "update the README"/"update the course") in the Corta Spam project. This project expects README + README_ES + a real training-course module, not just a commit message, and one commit per logical purpose.
metadata:
  type: project-runbook
  version: "1.0.0"
---

# Commit + docs — Corta Spam

This user's standing expectations (see project memory `user-preferences`), condensed for reuse without re-deriving them each time.

## Before committing

1. Run `corta-spam-verify-build` — never commit on a red or unverified build.
2. `git status` — confirm nothing extraneous is staged and nothing intended is left out. If a prior batch's edits (e.g. a doc fixed after an earlier commit in the same turn) are still sitting uncommitted, check whether they belong in this commit or a separate one.

## Not every commit is a feature commit

The rules below (verify-build, README parity, a course module) are written for changes that ship
**app behaviour**. Applying them to a commit that touches no compiled source wastes a build cycle
and pads the course with modules about nothing.

A commit qualifies for the short path when `git diff --cached --name-only` contains no
`.kt`, `.kts`, `.swift`, `.sq`, `.sqm`, `.xml` or manifest file — i.e. `.gitignore`,
`.claude/**`, `scripts/**`, `package.json`, or a docs-only change.

Short path: skip `verify-build` (state in the report that you skipped it and why), skip the
README changelog, skip the course. **Keep** one-purpose-per-commit and keep the why-focused
message. Confirm the build genuinely cannot be affected rather than assuming — on 2026-08-05,
before untracking `node_modules`, this proved it:

```bash
git grep -nI 'node_modules' -- '*.kts' '*.gradle' '*.yml' '*.sh'   # no hits => Gradle unaffected
```

Five commits that day (untracking `node_modules` and `.codex`, gitignore entries, the permission
deny-list widening, a new script) correctly took the short path. Do not retro-add course modules
for them.

## Splitting commits

**One logical purpose per commit.** If a turn produced both a feature and an unrelated fix noticed along the way (e.g. a branding typo spotted while building a feature), commit them separately with distinct messages — don't bundle. This has been the norm across 15+ commits in this project without correction.

## README

Update **both** `README.md` and `README_ES.md` (this project maintains Spanish parity — don't update only the English one). This project uses a changelog-style section (dated bullets, newest prepended) plus a "Features" bullet list that gains an entry per shipped feature. Bump any counters that drifted (test count, e.g. "166+"→"173+" — check the actual current count rather than incrementing by guess).

## Training course

`course/corta_spam_course.html` — when asked to "update the course," this means a **real new module**, not a mention:
- A `<section>` with Learning Objectives, Beginner/Intermediate/Advanced badged subsections
- At least one real code snippet pulled from the actual change (not paraphrased)
- If the change involved a bug/gap/interesting failure, a "Case Study" callout describing it honestly (this project has done this for the missing-migration-snapshot gap, the `combine()` 5-flow ceiling, etc — these land better as case studies than as buried asides)
- A quiz section with reveal answers, consistent with existing modules' format
- Update every place the module/quiz count is echoed: `<span id="progress-text">` header, the "N Modules, One Production App" hero text, the conclusion table (add a row), the footer test count, and the JS `var total=N`. Grep for the current total first: `grep -c '<div class="quiz-item">' course/corta_spam_course.html` to sanity-check the new quiz count after editing.

## Commit message

End every commit with the trailer for **the model actually writing the commit**, which the
harness states in its environment block — not a name hardcoded here. All 21 commits of the
2026-08-05 audit read:

```
Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
```

This skill previously pinned "Claude Sonnet 5", which was wrong for any other model and made the
history claim authorship that did not happen. Check the environment, don't copy a literal.

Body should explain **why**, not just what — this project's commit messages consistently name the actual bug/user pain being addressed and, when something adjacent was deliberately left alone, say so and why (see commit `1578c0a` for the pattern: what changed, plus an explicit "left unchanged: X, because Y" paragraph).

## After committing

Don't push unless explicitly asked. Don't install/launch the app unless explicitly asked (separate standing constraint — see `user-preferences` memory).
