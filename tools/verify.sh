#!/usr/bin/env bash
# Standard entry point. The real script lives at scripts/verify.sh and resolves its own
# paths relative to that location, so this wrapper execs it in place rather than
# symlinking — a symlink would make `dirname $0` resolve to tools/ and break it.
#
# Standardised 2026-08-26: every repo on this machine exposes its checks at
# tools/verify.sh, so no session has to search for the path.
set -euo pipefail
cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec bash "scripts/verify.sh" "$@"
