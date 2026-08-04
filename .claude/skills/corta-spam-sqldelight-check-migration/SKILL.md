---
name: corta-spam-sqldelight-check-migration
description: Use when adding a new value to a CHECK-constrained column in Corta Spam's SQLDelight schema (e.g. a new rule_type tag on CallLogEntry). SQLite can't ALTER a CHECK constraint — this is the rebuild-table migration pattern this project already uses.
metadata:
  type: project-runbook
  version: "1.0.0"
---

# SQLite CHECK-constraint migration — Corta Spam

`CallLogEntry.rule_type` (and possibly other columns) has a `CHECK (rule_type IN (...))` constraint in `shared/src/commonMain/sqldelight/org/carlospinan/bloqueador/app/db/AppDatabase.sq`. SQLite has no `ALTER TABLE ... ALTER CONSTRAINT` — the only way to widen a CHECK is the rebuild-table pattern shown below.

## 1. Find the next migration number

```
ls shared/src/commonMain/sqldelight/*.sqm
ls shared/src/commonMain/sqldelight/databases/
```

`N.sqm` migrates schema version **N → N+1**, and `databases/N.db` is the snapshot of version N.

Migration history was squashed when the database file was renamed to `cortaspam.db` pre-release: the only artifact is the baseline `databases/1.db` and there are currently **no** `.sqm` files. So the next migration to add is `1.sqm` (v1 → v2), then `2.sqm`, and so on.

## 2. Write the migration file

`shared/src/commonMain/sqldelight/N.sqm`, using this shape:

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

## 4. Run the migration check — it is real now, and it gates CI

```
./gradlew :shared:verifySqlDelightMigration
```

This replays every `.sqm` on top of `databases/1.db` and diffs the result against `AppDatabase.sq`. It is wired into the `jvm-android` job in `.github/workflows/ci.yml`, so a mismatch fails the build. Run it locally before pushing — it takes about a second.

Two rules that matter:

- **Never use `ALTER TABLE ... ADD COLUMN` for a column that carries a `REFERENCES` clause.** SQLite cannot add a foreign key that way. The column will exist but the FK will not, so upgraded databases silently diverge from fresh installs. Use the rebuild-table pattern above for those too. (This exact bug shipped once in the old `5.sqm` and went unnoticed because the check was not in CI.)
- **Do not regenerate the baseline to make a failure go away.** `./gradlew :shared:generateCommonMainAppDatabaseSchema` rewrites `databases/1.db` from the *current* `.sq`, which makes any diff vanish without fixing anything. Only regenerate when deliberately cutting a new squashed baseline.

## 5. Add the RuleDecision variant (if this migration is backing a new decision type)

`RuleDecision.kt` — new variant, update `isBlocked`, `blockReason`, `loggedRuleId`, `ruleTypeTag` (this must exactly equal the new CHECK string). `RulePrecedenceResolver.kt` — wire the new decision into the right precedence step; check placement carefully, precedence order is a deliberate security-relevant ordering (spam/manual-block always outrank convenience features like bypass-on-retry).

## 6. Verify

`corta-spam-verify-build`. Also add resolver/use-case test cases for the new decision path (see existing `RulePrecedenceResolverTest.kt`/`EvaluateIncomingCallUseCaseTest.kt` for the pattern: threshold-boundary case, one-below-threshold case, feature-off case, and a precedence-ordering regression case proving a higher-precedence block still wins).
