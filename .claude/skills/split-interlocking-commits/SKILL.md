---
name: split-interlocking-commits
description: Use when a large working tree has to become several purpose-scoped commits and the changes overlap across files, so `git add <file>` alone cannot separate them. Rebuilds history from a snapshot, verifying each commit compiles, without fabricating intermediate states.
metadata:
  type: workflow-runbook
  version: "1.0.0"
---

# Splitting an interlocking working tree into buildable commits

The easy case is one purpose per file: stage the files, commit, repeat. This is for the other
case — an audit or refactor where `RulePrecedenceResolver.kt` carries three unrelated fixes and
`strings.xml` carries four, so committing whole files would smear purposes together.

Interactive `git add -p` is unavailable in this environment, and hand-authoring an intermediate
version of every shared file is both slow and a good way to commit something that never compiled.

## The method: snapshot, reset, rebuild forward

```bash
SNAP=/tmp/final-tree
mkdir -p "$SNAP"
rsync -a --exclude '.git/' --exclude 'build/' --exclude '.gradle/' \
      --exclude 'node_modules/' ./ "$SNAP/"

git stash push -u -m "verified-final"   # tree is now at HEAD
```

Then for each commit, copy forward only the files that commit owns:

```bash
take() { for f in "$@"; do mkdir -p "$(dirname "$f")"; cp "$SNAP/$f" "$f"; done; }

take path/to/OwnedFile.kt path/to/ItsTest.kt
<build + test>
git add -A && git commit
```

**The snapshot is the safety net.** When the last commit is in, prove you reproduced it exactly:

```bash
diff -rq --exclude='.git' --exclude='build' --exclude='.gradle' . "$SNAP"
# empty output = the rebuilt history ends at the tree you verified
```

Run that. It is the whole reason this is safe, and it catches a dropped file immediately.

## Handling the genuinely shared files

For a file two commits both need, choose one of:

1. **Hand-write the intermediate** — best when the slice is small and separable, e.g. adding one
   method to an interface implementation:
   ```python
   # take the HEAD version and add only this commit's part
   s = read(path); s = s.replace(anchor, anchor + new_method); write(path, s)
   ```
2. **Slice from the snapshot by region** — best for test files and appended sections:
   ```python
   fin = read(SNAP + path); cur = read(path)
   cur = cur[:cur.index(START)] + fin[fin.index(START):fin.index(END)] + cur[cur.index(END):]
   ```
3. **Let it ride with the dominant commit and say so in the body.** Legitimate when the changes
   are genuinely one thought. Name the rider explicitly — "also switches the service off the
   removed `blockReason`, because this commit is what removes it" — rather than hiding it.

Option 3 is not a failure. Pretending a commit is atomic when it is not is.

## Ordering

Order by **dependency, not importance**. A commit that removes an API must come with or after
everything that stops using it. Practical shortcut: start with the change that alters the most
constructor signatures or interface members — everything else builds on top of it. In this
project's audit that was the dispatcher injection.

If a commit will not compile without a slice of the next one, you have found a real dependency:
either reorder, or accept it as a rider (option 3).

## Verify every commit, not just the last

```bash
./scripts/verify.sh --fast   # before each commit
./scripts/verify.sh          # before the final one, and after the diff -rq check
```

A history where intermediate commits do not build defeats `git bisect`, which is most of why the
history is being split at all. Do not skip this to save time — the compiler finds the missing
slice in seconds, and guessing does not.

## Recovering if it goes wrong

The stash still holds the verified tree:

```bash
git stash list          # verified-final should be there
git stash pop           # or: git checkout stash@{0} -- .
```

Do not `git stash drop` until `diff -rq` comes back empty.

## When not to bother

If the whole change is one thought, ship it as one commit. This costs real time; spend it when
the history will actually be read — a shared repo, a review, or a change likely to be bisected
later. A solo spike does not need it.
