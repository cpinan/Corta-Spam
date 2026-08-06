---
name: git-history-rewrite
description: Use when something must be purged from git history rather than just deleted at HEAD — a committed secret, a large binary, an accidentally tracked tree. Covers the backup, the proof that only the intended path moved, the deny rule that blocks the push, and the SHA references that break everywhere afterwards.
metadata:
  type: workflow-runbook
  version: "1.0.0"
---

# Rewriting git history

Ran on 2026-08-05 to purge a 17 MB `node_modules` tree (a darwin-arm64 `.dylib`) from all 111
commits of a public repo. Worked, but three things were only discovered mid-flight. They are
first here, because they are what a future run gets wrong.

## Read these before starting

**1. The push is denied by this repo's own config.**

`.claude/settings.json` denies `Bash(git push --force*)` and eleven sibling patterns. Deny
outranks allow, cannot be prompted past, and `.claude/settings.local.json` allowing
`Bash(git push*)` does not help. The agent will get:

```
Permission to use Bash with command git push --force-with-lease=... has been denied.
```

That is the rule working, not a bug. **Hand the command to the user** to run with a `!` prefix.
Do not rephrase it into `git push origin +main` or `-f` to slip past the prefix match — the
patterns were widened on 2026-08-05 precisely so that trick stops working, and routing around a
guardrail the user set is not the agent's call.

**2. `git filter-repo` deletes your remote.** It prints `NOTICE: Removing 'origin' remote` and
means it. Re-add it before pushing or the push fails with "does not appear to be a git repository".

**3. Every short SHA written down anywhere dies.** This is the expensive one. After the 2026-08-05
run, 58 stale references were sitting in memory files, a skill, and `docs/PLAY_RELEASE_PLAN.md`.
Save the commit map immediately and remap them — step 7. References inside *commit messages*
cannot be fixed without another rewrite; accept those.

## Procedure

### 1. Establish it is worth it

```bash
gh repo view --json forkCount,stargazerCount,visibility
```

Blast radius is one clone per fork plus every collaborator. Zero forks and a solo repo makes this
cheap; a repo others have cloned makes it a coordination problem, not a git problem.

For a **binary or bloat**, a rewrite fully solves it. For a **leaked credential**, a rewrite does
NOT: GitHub keeps unreachable objects fetchable by direct SHA until it garbage-collects, which it
does on its own schedule. Rotate the credential. The rewrite is cleanup, never containment.

### 2. Fix HEAD first, as ordinary commits

Untrack at the tip and commit normally *before* rewriting. This keeps the rewrite a pure
path-deletion whose correctness you can prove in step 5.

```bash
git rm -r --cached <path>          # leaves files on disk
```

Adding the path to `.gitignore` does **not** untrack it. That is how the 2026-08-05 material got
there in the first place: `node_modules/` and `.codex/` were both ignored *and* tracked for weeks.
`./scripts/audit_repo_secrets.sh` now checks for exactly this.

### 3. Back up, and verify the backup

```bash
git bundle create ~/<repo>-pre-rewrite-backup.bundle --all
git bundle verify ~/<repo>-pre-rewrite-backup.bundle     # must say "complete history"
```

Put it outside the repo and outside any temp directory that gets swept. Restore is
`git clone <bundle>`.

### 4. Confirm the remote is not ahead

```bash
git fetch origin
git log --oneline origin/main ^main     # MUST be empty
```

Non-empty means a force push destroys commits you do not have. Stop.

Record the remote tip — it becomes the lease in step 6:

```bash
git rev-parse origin/main
```

### 5. Rewrite, then prove only the intended path moved

```bash
git rev-parse HEAD^{tree}                                  # record BEFORE
git filter-repo --path <path> --invert-paths --force
git rev-parse HEAD^{tree}                                  # MUST be identical
```

**The tree hash is the whole proof.** Because step 2 already removed the path at HEAD, the
resulting HEAD tree must be byte-identical. If it differs, the filter caught something else and
the rewrite is wrong — restore from the bundle. Also check the commit count is unchanged
(`git rev-list --all --count`); filter-repo prunes commits that become empty, which is usually
fine but should be a decision, not a surprise.

### 6. Push

```bash
git remote add origin <url>          # filter-repo removed it
git fetch origin
git push --force-with-lease=main:<sha-from-step-4> origin main
```

Pin the lease to the SHA explicitly. Bare `--force-with-lease` compares against a
remote-tracking ref that filter-repo just deleted, so it can pass vacuously. And per trap 1,
this line is for the **user** to run.

### 7. Repair the references you just broke

```bash
cp .git/filter-repo/commit-map ~/<repo>-sha-map-old-to-new.txt
```

Do this before anything else touches `.git/filter-repo/`. Then rewrite short SHAs across the
places that cite them — for this project that is `~/.claude/projects/*/memory/*.md`,
`.claude/skills/**/SKILL.md`, `docs/`, and the READMEs. Match on unique prefix:

```python
hits = [new for old, new in commit_map.items() if old.startswith(short_sha)]
if len(hits) == 1: replace with hits[0][:len(short_sha)]
```

Then verify every rewritten SHA resolves, and spot-check that `git log -1 --format=%s <new>`
still names the commit the surrounding prose describes. On 2026-08-05 eleven leftover hex strings
did not resolve — they were GitHub Actions run IDs, not SHAs, and correctly left alone. Expect
that class of false positive and check rather than force it.

### 8. Confirm

```bash
git log --all --pretty=format: --name-only | sort -u | grep <path>   # empty
du -sh .git                                                          # 27M -> 4.3M in the real run
./scripts/audit_repo_secrets.sh
```

Note that GitHub's reported `diskUsage` will not drop right away. That is their GC, not your
rewrite failing.
