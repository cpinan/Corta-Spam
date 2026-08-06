---
name: corta-spam-sqldelight-check-migration
description: Use when changing Corta Spam's SQLDelight schema — adding a value to a CHECK-constrained column, adding a column, or adding queries against one. Covers migration numbering (silent when wrong), the rebuild-table pattern SQLite forces for a CHECK change, and the generated-API typing that trips up single-column selects.
metadata:
  type: project-runbook
  version: "1.1.0"
---

# SQLite CHECK-constraint migration — Corta Spam

`CallLogEntry.rule_type` (and possibly other columns) has a `CHECK (rule_type IN (...))` constraint in `shared/src/commonMain/sqldelight/org/carlospinan/bloqueador/app/db/AppDatabase.sq`. SQLite has no `ALTER TABLE ... ALTER CONSTRAINT` — the only way to widen a CHECK is the rebuild-table pattern shown below.

## 1. Find the next migration number — and get it right, because nothing will tell you

```
ls shared/src/commonMain/sqldelight/org/carlospinan/bloqueador/app/db/*.sqm
ls shared/src/commonMain/sqldelight/databases/
```

Note the path: the `.sqm` files sit **next to `AppDatabase.sq`**, in the package directory — not
at the `sqldelight/` root. (An earlier version of this skill had the wrong glob.)

`N.sqm` migrates schema version **N → N+1**, and `databases/N.db` is the snapshot of version N.
The baseline is `databases/1.db` (version 1), so:

> **next number = (number of existing `.sqm` files) + 1**, and `Schema.version` ends up one
> higher than that.

Currently `1.sqm` exists, so the next is `2.sqm` and the schema will be at version 3.

### ⚠ Getting this wrong is silent

On 2026-08-05 a migration was added as `2.sqm` when it should have been `1.sqm`. Everything
passed: unit tests, `verifySqlDelightMigration`, lint, both platforms, and the migration
*actually ran on a device*, because the generated guard is `oldVersion <= 2 && newVersion > 2`
and a version-1 database satisfies it. What it produced was `Schema.version = 3` with nothing
implementing the 1 → 2 step, and a hole the next person adding `2.sqm` would have collided with.

The only thing that caught it was reading `PRAGMA user_version` off a real phone. Which is now
automated:

```
./scripts/device_check.sh --device <serial>
```

That asserts `user_version == (number of .sqm files) + 1` and that every expected index exists.
Run it after any schema change.

**If the schema version goes *down*** (you renumbered a migration, as above), a device carrying
the higher version will refuse to open the database — `AndroidSqliteDriver` treats it as a
downgrade. Clear app data first: `./scripts/device_check.sh --clear --device <serial>`.

## 2. Write the migration file

`shared/src/commonMain/sqldelight/org/carlospinan/bloqueador/app/db/N.sqm`, using this shape:

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
- **`verifySqlDelightMigration` proves the chain reaches the right schema. It does not prove the
  version numbering is sane** — it will happily accept a mis-numbered file whose migration still
  runs. Only `device_check.sh` (or a manual `PRAGMA user_version`) covers that.
- **Do not regenerate the baseline to make a failure go away.** `./gradlew :shared:generateCommonMainAppDatabaseSchema` rewrites `databases/1.db` from the *current* `.sq`, which makes any diff vanish without fixing anything. Only regenerate when deliberately cutting a new squashed baseline.

## 4b. Adding a plain column — and the query typing that will not compile

A nullable `ADD COLUMN` with no CHECK, no default and no foreign key needs none of the
rebuild-table dance above. `2.sqm` (`recording_path` on `CallLogEntry`, 2026-08-06) is one line:

```sql
ALTER TABLE CallLogEntry ADD COLUMN recording_path TEXT;
```

Existing rows get `NULL`, which is what you want — they predate the feature.

**The part that costs compile cycles is the generated API for single-column selects, which is not
consistent.** Two queries against the same nullable column produce two different Kotlin types:

```sql
-- narrowed by the predicate -> Query<String>, the value itself, non-null
selectAllRecordingPaths:
SELECT recording_path FROM CallLogEntry WHERE recording_path IS NOT NULL;

-- no predicate -> Query<SelectRecordingPathById>, a wrapper with a nullable field
selectRecordingPathById:
SELECT recording_path FROM CallLogEntry WHERE id = ?;
```

So:

```kotlin
queries.selectAllRecordingPaths().executeAsList().forEach { RecordingStore.delete(it) }   // String
queries.selectRecordingPathById(id).executeAsOneOrNull()?.recording_path                  // wrapper
```

Symptoms if you guess: `Unresolved reference 'recording_path'` on the narrowed one, and
`Argument type mismatch: actual type is 'SelectRecordingPathById', but 'String' was expected` on
the other. Do not guess — read the generated signature:

```bash
find shared/build -name AppDatabaseQueries.kt | head -1 | xargs grep -n "yourQueryName"
```

**If the column points at a file, deletion is your problem.** `DELETE FROM` drops the row and the
path with it, leaving the file orphaned — unreachable by the UI and undeletable by the user.
Delete files *before* the rows, and see the `RecordingStore` expect/actual: `commonMain` cannot
touch the filesystem, so this needs a platform actual, not a helper function.

## 5. Add the RuleDecision variant (if this migration is backing a new decision type)

`RuleDecision.kt` — new variant, update `isBlocked`, `reason`, `loggedRuleId`, `ruleTypeTag` (this must exactly equal the new CHECK string). Note `reason` returns a structured `BlockReason`, not a string: the old `blockReason: String?` was removed because it was written into `rule_detail` in English and froze the call log in whatever language it was created in. A new variant needs a `BlockReason` case, a `@SerialName`, and a rendering in **both** `BlockReasonText.kt` (Compose) and `BlockReasonStrings.kt` (Android notifications). `RulePrecedenceResolver.kt` — wire the new decision into the right precedence step; check placement carefully, precedence order is a deliberate security-relevant ordering (spam/manual-block always outrank convenience features like bypass-on-retry).

## 6. Verify

`./scripts/verify.sh` (or `corta-spam-verify-build`), then `./scripts/device_check.sh`. Also add resolver/use-case test cases for the new decision path (see existing `RulePrecedenceResolverTest.kt`/`EvaluateIncomingCallUseCaseTest.kt` for the pattern: threshold-boundary case, one-below-threshold case, feature-off case, and a precedence-ordering regression case proving a higher-precedence block still wins).
