#!/bin/bash
# Audit the repo for material that should never have been committed.
#
# Written after the 2026-08-05 audit, which was typed out by hand one grep at a
# time. It found no secrets but two files that .gitignore claimed to exclude and
# git was tracking anyway -- including a 17 MB darwin-arm64 .dylib that had been
# public since July.
#
# Scans ALL history, not just the working tree. A secret deleted in a later
# commit is still published.
#
# Usage:
#   ./scripts/audit_repo_secrets.sh            # working tree + full history
#   ./scripts/audit_repo_secrets.sh --fast     # skip the full-history content grep
#
# Exit codes: 0 clean, 1 findings, 2 could not run.

set -uo pipefail

cd "$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "not a git repo"; exit 2; }

FAST=0
[ "${1:-}" = "--fast" ] && FAST=1

FINDINGS=0
section() { printf '\n\033[1m── %s ──\033[0m\n' "$1"; }
fail()    { printf '  \033[31m✗ %s\033[0m\n' "$1"; FINDINGS=$((FINDINGS+1)); }
pass()    { printf '  \033[32m✓ %s\033[0m\n' "$1"; }

# Excludes node_modules etc from content scans. Keep quoted -- an unquoted glob
# in zsh aborts the whole command with "no matches found", which reads like a
# clean result.
PATHSPEC=(':(exclude)node_modules' ':(exclude)*.lock' ':(exclude)package-lock.json')

section "Repo visibility"
if command -v gh >/dev/null 2>&1 && VIS=$(gh repo view --json visibility -q .visibility 2>/dev/null); then
  if [ "$VIS" = "PUBLIC" ]; then
    printf '  \033[33m! PUBLIC — everything below is world-readable\033[0m\n'
  else
    pass "$VIS"
  fi
else
  echo "  (gh unavailable or no remote; assuming public and continuing)"
fi

section "Signing material / credentials, anywhere in history"
# Filenames that should never appear in any commit, ever.
HITS=$(git log --all --pretty=format: --name-only 2>/dev/null | sort -u | grep -iE \
  '\.(jks|keystore|p12|pfx|pem|key|mobileprovision|p8|kdbx)$|(^|/)\.env|local\.properties|google-services\.json|GoogleService-Info\.plist|play-service-account|(^|/)credentials|(^|/)id_rsa' \
  || true)
if [ -n "$HITS" ]; then
  fail "files that must never be committed appear in history:"
  echo "$HITS" | sed 's/^/      /'
  echo "      A rewrite alone does NOT purge these from a GitHub remote — the"
  echo "      objects stay fetchable by SHA until GitHub GCs. Rotate the secret."
else
  pass "no keystores, .env, local.properties or service accounts in any commit"
fi

section "Secret-shaped strings"
PATTERN='AIza[0-9A-Za-z_-]{20,}|AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9]{20,}|ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}|-----BEGIN [A-Z ]*PRIVATE KEY-----'
if [ "$FAST" = "1" ]; then
  SCOPE="HEAD"; REVS="HEAD"
else
  SCOPE="all history"; REVS=$(git rev-list --all)
fi
# shellcheck disable=SC2086
HITS=$(git grep -nIE "$PATTERN" $REVS -- "${PATHSPEC[@]}" 2>/dev/null | head -40 || true)
if [ -n "$HITS" ]; then
  fail "possible live credential ($SCOPE):"
  echo "$HITS" | sed 's/^/      /'
else
  pass "no API keys, tokens or private keys ($SCOPE)"
fi

section "Ignored but tracked"
# The trap that produced both findings on 2026-08-05: adding a path to
# .gitignore does not untrack what is already in the index, and git says
# nothing. Fix is `git rm -r --cached <path>`, not another .gitignore line.
HITS=$(git ls-files -i -c --exclude-standard 2>/dev/null || true)
if [ -n "$HITS" ]; then
  COUNT=$(echo "$HITS" | wc -l | tr -d ' ')
  fail "$COUNT file(s) are gitignored AND tracked — .gitignore is lying:"
  echo "$HITS" | head -10 | sed 's/^/      /'
  [ "$COUNT" -gt 10 ] && echo "      ... and $((COUNT-10)) more"
  echo "      Fix: git rm -r --cached <path>   (leaves the files on disk)"
else
  pass ".gitignore and the index agree"
fi

section "Oversized blobs in history"
BIG=$(git rev-list --objects --all 2>/dev/null \
  | git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)' 2>/dev/null \
  | awk '$1=="blob" && $3 > 2097152 {printf "%.1f MB  %s\n", $3/1048576, $4}' | sort -rn | head -10 || true)
if [ -n "$BIG" ]; then
  fail "blobs over 2 MB — every clone pays for these forever:"
  echo "$BIG" | sed 's/^/      /'
else
  pass "no blob over 2 MB"
fi

printf '\n'
if [ "$FINDINGS" -eq 0 ]; then
  printf '\033[32m✓ clean\033[0m\n'; exit 0
else
  printf '\033[31m✗ %d finding(s)\033[0m — deleting at HEAD is not enough; the history has to be rewritten and any real secret rotated\n' "$FINDINGS"
  exit 1
fi
