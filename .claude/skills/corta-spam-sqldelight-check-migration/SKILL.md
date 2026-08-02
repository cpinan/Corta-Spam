---
name: corta-spam-sqldelight-check-migration
description: Use when adding a new value to a CHECK-constrained column in Corta Spam's SQLDelight schema (e.g. a new rule_type tag on CallLogEntry). SQLite can't ALTER a CHECK constraint — this is the rebuild-table migration pattern this project already uses.
metadata:
  type: project-runbook
  version: "1.0.0"
---

# SQLite CHECK-constraint migration — Corta Spam

`CallLogEntry.rule_type` (and possibly other columns) has a `CHECK (rule_type IN (...))` constraint in `shared/src/commonMain/sqldelight/org/carlospinan/bloqueador/app/db/AppDatabase.sq`. SQLite has no `ALTER TABLE ... ALTER CONSTRAINT` — the only way to widen a CHECK is the rebuild-table pattern. This project has done this twice already (`4.sqm` added `SCHEDULE`, `6.sqm` added `REVIEW`, `7.sqm` added `REPEATED_ALLOWED`) — follow the same shape.

## 1. Find the next migration number

```
ls shared/src/commonMain/sqldelight/*.sqm
```

Next file is `(highest + 1).sqm`, e.g. if `7.sqm` is latest, add `8.sqm`.

## 2. Write the migration file

`shared/src/commonMain/sqldelight/N.sqm`, following the exact shape of `6.sqm`/`7.sqm`:

```sql
CREATE TABLE CallLogEntry_new (
    -- ...every existing column, unchanged...
    rule_type TEXT CHECK (rule_type IN ('MANUAL', 'PATTERN', 'COUNTRY', 'SPAM', 'ACTION', 'SCHEDULE', 'REVIEW', 'REPEATED_ALLOWED', 'YOUR_NEW_TAG'))
    -- ...
);

INSERT INTO CallLogEntry_new SELECT * FROM CallLogEntry;

DROP TABLE CallLogEntry;

ALTER TABLE CallLogEntry_new RENAME TO CallLogEntry;
```

Copy the *actual current full column list* from `AppDatabase.sq`'s `CREATE TABLE CallLogEntry` — don't hand-reconstruct it from memory, it will drift.

## 3. Update the base schema to match

`shared/src/commonMain/sqldelight/org/carlospinan/bloqueador/app/db/AppDatabase.sq` — update the same `CHECK (rule_type IN (...))` list in the live `CREATE TABLE CallLogEntry` statement. **Required**, not optional: `shared/build.gradle.kts` has `verifyMigrations.set(true)`, which fails the build if the migration chain's end state and the base schema disagree.

## 4. Known gap in this project — don't let it block you, but don't make it worse silently

There is a *separate* SQLDelight verification task, `verifyCommonMainAppDatabaseMigration`, that replays `.sqm` files on top of a `1.db` snapshot and diffs against `N.db` snapshots — `6.db` and `7.db` are currently missing (only `1.db`-`5.db` exist), so that specific task already fails with `duplicate column name`. It's confirmed `SKIPPED` in the normal build/test graph so it won't block you, but adding `N.sqm` without an `N.db` snapshot means the gap now spans one more version. Either regenerate the missing `.db` snapshots (build a fresh sqlite db via the intended `CREATE TABLE` statements, then `PRAGMA user_version = N;`) or explicitly flag in your summary that the gap grew — don't silently ignore it a third time.

## 5. Add the RuleDecision variant (if this migration is backing a new decision type)

`RuleDecision.kt` — new variant, update `isBlocked`, `blockReason`, `loggedRuleId`, `ruleTypeTag` (this must exactly equal the new CHECK string). `RulePrecedenceResolver.kt` — wire the new decision into the right precedence step; check placement carefully, precedence order is a deliberate security-relevant ordering (spam/manual-block always outrank convenience features like bypass-on-retry).

## 6. Verify

`corta-spam-verify-build`. Also add resolver/use-case test cases for the new decision path (see existing `RulePrecedenceResolverTest.kt`/`EvaluateIncomingCallUseCaseTest.kt` for the pattern: threshold-boundary case, one-below-threshold case, feature-off case, and a precedence-ordering regression case proving a higher-precedence block still wins).
