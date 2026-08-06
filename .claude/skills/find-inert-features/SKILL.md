---
name: find-inert-features
description: Use when auditing a codebase for bugs, reviewing whether a feature actually works, or when something "should be working" but users report it isn't. Finds code that ships, has passing tests, and never executes — the failure mode component tests are structurally unable to catch.
metadata:
  type: audit-runbook
  version: "1.0.0"
---

# Finding features that ship, pass tests, and never run

A full audit of Corta Spam on 2026-08-05 found eighteen defects. **Six shared one shape**: a
feature with a database table, a repository, a branch in the engine, and green tests — none of
which executed in the shipping app.

This is the most expensive class of bug to carry, because every signal says it is fine. It is
also nearly invisible to the review question "does this code look right?", since each piece
does. Look for it deliberately.

## Why the tests don't help

The bundled spam list had a dedicated test asserting that `lookup("+2348012345678")` returns a
spam verdict. It passed. It called the provider **directly**, and production never does —
production passed a digits-only string with the `+` stripped, which matched nothing.

> A component test proves a component works. It says nothing about whether anything calls it
> correctly. The bug lives in the *seam*, and only a test through the real entry point sees it.

## The five probes

Run these against any feature you are asked to verify.

### 1. Reachability — can a user create the input this code consumes?

```bash
# a table exists; does anything write to it from the UI?
grep -rn "addActionRule" --include=*.kt . | grep -v "Test\|Fake\|Repository\|UseCase"
```

Corta Spam's repeat-caller rules had a table, a repository, a resolver branch, `CallAttempt`
tracking and backup fields since M2. Nothing in the app could create one. The only route in was
hand-editing a backup JSON.

**Probe:** for every table and every enum variant, find the UI that produces it. No producer
means the feature is decoration.

### 2. Precedence shadowing — does an earlier branch always win?

Pattern-scoped action rules resolved their scope against the *enabled* patterns. An enabled
pattern already returns a block two steps earlier in the chain, so the branch could never be
taken, at any threshold.

**Probe:** for any ordered chain (precedence, middleware, interceptors, `when` on a sealed type),
ask of each branch: *what input reaches here without matching something above?* If the answer is
"none", it is dead.

### 3. Normalisation seams — does an earlier step destroy what a later step needs?

```kotlin
val normalized = normalizeForComparison(number)   // strips "+"
val country    = parseCountryCode(normalized)     // needs the "+" to mean anything
val spam       = provider.lookup(normalized)      // list is stored as "+234..."
```

One normalisation was right for the first consumer and destructive for the next two.

**Probe:** trace a value from entry point to each consumer. At every transform ask what
information it removes, and whether anything downstream needed it. Canonicalising early is a
common and reasonable instinct — and this is how it goes wrong.

### 4. Declaration without implementation — what did a manifest or interface promise?

`IN_CALL_SERVICE_RINGING` told Telecom the app rings for itself, so Telecom stopped ringing.
Nothing in the source ever played a sound. `DriverFactory.databaseDispatcher` was declared,
implemented on both platforms, and read by nobody.

**Probe:**
```bash
# declared members with no reader
grep -rn "val someProperty" --include=*.kt . | head
grep -rn "someProperty" --include=*.kt . | grep -v "override\|val someProperty" | head
```
Do the same for every manifest `meta-data`, `<uses-feature>`, and interface member. A declaration
is a promise; find the code that keeps it.

### 5. Uniqueness collisions — can the second one ever land?

`CountryRule` is `UNIQUE(country_code)` inserted with `INSERT OR IGNORE`, and four codes appeared
twice in the source list. The second entry rendered in the picker, did nothing when tapped, and
never reached the list.

**Probe:** for every `UNIQUE` column fed from a static list, assert the list has no duplicates —
in a test, not by eye.

### 6. Prose that promises a feature — what do the docs claim the code does?

The probes above all search code. This one does not, and it is the one that carries legal and
store-review consequence.

On 2026-08-06 Corta Spam had a **"Record caller message"** switch. It persisted its flag,
validated a consent phrase, rendered as a `Switch`, and had a passing repository test asserting
the flag round-tripped. Nothing recorded: no `RECORD_AUDIO` permission, no `MediaRecorder`, no
`AudioRecord` anywhere in the source. That is probe 4, and probe 4 would have found it.

What probe 4 would **not** have found is the rest of the blast radius. Four separate user-facing
surfaces asserted the feature:

| Surface | What it claimed |
|---|---|
| `AutoResponderScreen.kt` | a live toggle |
| `strings.xml` ×4 locales | the toggle, its description, a consent hint |
| `onboarding_never_do_record` | "we will never record any call *unless you separately turn that on*" |
| **`docs/PRIVACY.html`, both languages** | a whole `<h2>Call recording</h2>` section describing it |

The privacy policy is the dangerous one. It is publicly hosted, linked from the Play listing,
legally operative, and it was volunteering *call recording* — among the most scrutinised topics
in app review — for a capability the binary did not have.

**Probe:** take each feature named in your user-facing prose and grep for the API that would have
to exist.

```bash
# every capability the privacy policy / store listing / onboarding claims
grep -oiE "record|location|camera|upload|sync|cloud|backup" docs/PRIVACY.html docs/STORE_LISTING.md | sort -u

# then, per claim, the API that would implement it
grep -rn "MediaRecorder\|AudioRecord\|RECORD_AUDIO" --include=*.kt --include=*.xml androidApp/src shared/src
```

Zero hits against a documented capability is the finding. Run it the other way too — a permission
in the manifest with no prose explaining it is the same defect inverted, and reviewers ask about
exactly that.

**Why this ranks above the code probes:** a dead code path is a bug you fix on your own schedule.
A dead code path that a legal document describes to users is a misrepresentation, and it is
discovered by someone who can pull your listing.

## Confirm before reporting

Do not report any of these from reading alone. Write a throwaway test that calls the **real entry
point** and prints the decision:

```kotlin
val decision = RulePrecedenceResolver.evaluate("+2348012345678", ctx)
println("AUDIT decision = $decision")   // DefaultAllow -> the whole spam list is inert
```

Delete it afterwards; it exists to turn a suspicion into a fact. Roughly a third of what looks
inert on inspection turns out to be reachable by a path you had not noticed.

## Then fix the test, not just the code

Every one of these had a test that passed. Once the bug is fixed, ask what the test was actually
asserting — sometimes it was asserting the bug. Corta Spam had a test named
`patternMatch_caseInsensitive` claiming `"ABC*"` matches `"abc123"`. It passed because `"ABC*"`
matched *every number in existence*: patterns are compared on digits, so a letters-only pattern
has an empty core, and an empty core satisfies `startsWith`. The test was observing a
phone-wide outage and calling it case-insensitivity.

Add the integration-level test that would have caught it. That is the only part of the fix that
prevents the next one.
