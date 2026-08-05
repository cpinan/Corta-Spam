---
name: eod
description: End-of-day harvest. Use when the user types "eod", "/eod", or asks to wrap up a session. Sweeps the session for anything done more than once, done from memory, or learned the hard way, and converts it into a skill, script, hook, memory or settings change so the next session does not rediscover it.
metadata:
  type: project-runbook
  version: "1.0.0"
---

# EOD — turn this session into things that persist

Trigger: the user says **`eod`**, `/eod`, "end of day", or "wrap up".

The goal is not a summary. It is that **nothing learned today has to be learned again**. A
session ends with knowledge in three places — the transcript (disappears), the code (survives
but does not teach), and the tooling (survives *and* teaches). Move as much as possible into the
third.

## 1. Sweep for candidates

Walk the session and ask, in this order:

| Signal | Becomes |
|---|---|
| A command sequence typed more than twice | a **script** in `scripts/` |
| A rule the user corrected you on | a **memory** of type `feedback`, with the why |
| A multi-step procedure with an order that matters | a **skill** |
| A trap that cost real time and would recur | a section in the **relevant existing skill** |
| An existing skill that turned out to be wrong or stale | **fix that skill** — this outranks writing new ones |
| A check that only ran because you remembered | **CI step** or a line in `scripts/verify.sh` |
| A permission prompt hit repeatedly | an allowlist entry in `.claude/settings.json` |
| A fact about the project not derivable from the code | a **memory** of type `project` |
| Something the user asked for twice in different words | a **skill**, because they will ask a third time |

Be honest about the cut-off. A one-off does not become a skill. The test is: *would a future
session get this wrong without it?*

## 2. Fixing beats adding

Check the existing skills **before** writing a new one:

```bash
ls .claude/skills/
```

A skill that gives wrong advice is worse than no skill, because it is trusted. If this session
contradicted something a skill says, correcting it is the highest-value work available — do it
first, and say in the skill what the old advice was and why it was wrong, so nobody restores it.

## 3. Write it so it survives being read by someone in a hurry

- **Lead with the trap, not the happy path.** The reason to read a skill is to avoid a mistake.
- **Name the symptom.** Future-you searches for the error message, not the concept. Quote it.
- **Say what was already tried and rejected**, or it will be tried again.
- **Keep it runnable.** A skill that says "run the usual checks" is a skill that will be ignored;
  point at a script that exists.

## 4. Verify what you wrote

- Scripts: **run them**, including their failure path where cheap.
- Skills: reread against what actually happened this session, not what you meant to do.
- `settings.json`: it is JSON. A trailing comma silently breaks the whole harness — parse it.

```bash
python3 -m json.tool .claude/settings.json > /dev/null && echo "settings.json OK"
```

## 5. Report as a table, not prose

```
CREATED  scripts/verify.sh          the check sequence, previously retyped by hand
UPDATED  corta-spam-...-migration   numbering rule was wrong; cost a bad commit today
MEMORY   component-vs-integration   tests passed while the feature was dead
SKIPPED  <thing>                    one-off, not worth the file
```

Include the SKIPPED rows. They stop the same judgement being re-litigated tomorrow.

## 6. Then finish the session properly

- `./scripts/verify.sh` green
- Working tree committed, or its state explicitly reported
- Memory index (`MEMORY.md`) has a line for every memory file written

## What this is not

Not a changelog — the READMEs already carry that. Not a brain-dump of the session into a memory
file; a memory nobody can act on is noise. The output is *executable or corrective*, or it is
not worth writing.
