#!/usr/bin/env bash
# device_check.sh — install on a real device and assert what only a device can tell you.
#
# Written because the migration numbering bug of 2026-08-05 passed every automated check in the
# project — unit tests, verifySqlDelightMigration, lint, both platforms — and was visible only as
# PRAGMA user_version reporting 3 for a schema with two states. A green suite says the code is
# self-consistent; it does not say the database on someone's phone is the shape you think.
#
# Usage:
#   ./scripts/device_check.sh                    # first connected device
#   ./scripts/device_check.sh --device ZY22JZ...  # a specific one
#   ./scripts/device_check.sh --clear            # wipe app data first (needed after a schema
#                                                # version goes DOWN, which the driver refuses)
set -euo pipefail
cd "$(dirname "$0")/.."

# applicationId, which is what adb/run-as key off. Not the same as the code namespace.
PKG=org.carlospinan.cortaspam
DB=cortaspam.db
DEVICE=""
CLEAR=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --clear)  CLEAR=true; shift ;;
    --help|-h) sed -n '2,14p' "$0"; exit 0 ;;
    *) echo "unknown flag: $1" >&2; exit 2 ;;
  esac
done

if [ -z "$DEVICE" ]; then
  DEVICE=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')
  [ -z "$DEVICE" ] && { echo "No device connected (adb devices)." >&2; exit 1; }
fi
ADB="adb -s $DEVICE"
echo "==> device: $DEVICE"

WAIT_DB=$(mktemp -t cortaspam-wait); trap 'rm -f "$WAIT_DB"' EXIT

if [ "$CLEAR" = true ]; then
  echo "==> clearing app data"
  $ADB shell pm clear "$PKG" >/dev/null
fi

# Clear the log BEFORE the launch this run is judging. Without this the check reads the device's
# whole persistent buffer: on 2026-08-13 it failed the razr on a two-day-old trace and a crash
# belonging to an entirely different app, and exited before any of the database assertions below
# — the ones this script exists for — had run.
$ADB logcat -c 2>/dev/null || true

./install_android.sh --device "$DEVICE"

# Wait for the schema, do not guess at it. This was `sleep 3`, which is enough on a warm install
# and not enough on a cold first run: on 2026-08-20 a fresh install reported `user_version 0` with
# one table, and the identical re-run on the same build reported 4 and ten tables. A check that
# fails for a reason that is not the thing it checks trains you to re-run it, which is the same as
# not having it.
#
# The app creates the schema without any user interaction -- it was still on the Welcome screen
# when the poll below succeeded -- so this waits on the database rather than on the UI.
echo "==> waiting for the schema"
SCHEMA_READY=false
for _ in $(seq 1 30); do
  if $ADB shell "run-as $PKG cat databases/$DB" 2>/dev/null > "$WAIT_DB" &&
     [ -s "$WAIT_DB" ] &&
     [ "$(python3 -c "
import sqlite3,sys
try: print(sqlite3.connect(sys.argv[1]).execute('PRAGMA user_version').fetchone()[0])
except Exception: print(0)
" "$WAIT_DB")" != "0" ]; then
    SCHEMA_READY=true
    break
  fi
  sleep 1
done
if [ "$SCHEMA_READY" = false ]; then
  echo "FAIL: the database still reports user_version 0 after 30s -- the schema never created." >&2
  exit 1
fi

echo "==> crash check"
# Scoped to this app's own process. A fatal in some unrelated package is not this app's failure,
# and treating it as one makes the whole check unrunnable on a phone somebody actually uses.
APP_PID=$($ADB shell pidof "$PKG" 2>/dev/null | tr -d '\r' | awk '{print $1}')
if [ -z "$APP_PID" ]; then
  echo "FAIL: $PKG is not running after launch — it died on startup." >&2
  $ADB logcat -d 2>/dev/null | grep -B2 -A15 "FATAL EXCEPTION" | tail -30 >&2
  exit 1
fi
if $ADB logcat -d --pid="$APP_PID" 2>/dev/null | grep -q "FATAL EXCEPTION"; then
  echo "FAIL: fatal exception in $PKG (pid $APP_PID):" >&2
  $ADB logcat -d --pid="$APP_PID" 2>/dev/null | grep -A15 "FATAL EXCEPTION" | tail -25 >&2
  exit 1
fi
echo "    no fatal exceptions (pid $APP_PID)"

echo "==> pulling $DB"
TMP=$(mktemp -t cortaspam-db)
$ADB shell "run-as $PKG cat databases/$DB" > "$TMP" 2>/dev/null
[ -s "$TMP" ] && echo "    $(wc -c < "$TMP" | tr -d ' ') bytes" || {
  echo "FAIL: database is empty or unreadable — did the app actually open it?" >&2; exit 1; }

# Expected version = number of .sqm files + 1 (the baseline). A mismatch means a migration file
# is mis-numbered: N.sqm takes the schema from N to N+1, so on a version-1 baseline the first
# one is 1.sqm. Getting this wrong still "works" and still passes verifySqlDelightMigration.
EXPECTED=$(( $(ls shared/src/commonMain/sqldelight/org/carlospinan/bloqueador/app/db/*.sqm 2>/dev/null | wc -l | tr -d ' ') + 1 ))

python3 - "$TMP" "$EXPECTED" <<'PY'
import sqlite3, sys
db, expected = sys.argv[1], int(sys.argv[2])
c = sqlite3.connect(db)
version = c.execute("PRAGMA user_version").fetchone()[0]
tables  = sorted(r[0] for r in c.execute(
    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"))
indexes = sorted(r[0] for r in c.execute(
    "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'"))

print(f"    user_version : {version} (expected {expected})")
print(f"    tables       : {len(tables)} -> {', '.join(tables)}")
print(f"    indexes      : {', '.join(indexes) or 'NONE'}")

fail = []
if version != expected:
    fail.append(f"user_version is {version}, expected {expected}. A .sqm is mis-numbered, or the "
                f"device is carrying an older/newer schema — try --clear.")
for required in ("idx_call_log_action_time", "idx_call_attempt_number_time"):
    if required not in indexes:
        fail.append(f"missing index {required} — a migration did not apply")
if "CallLogEntry" not in tables:
    fail.append("CallLogEntry missing — the schema did not create")

if fail:
    print("\nFAIL:", file=sys.stderr)
    for f in fail: print("  - " + f, file=sys.stderr)
    sys.exit(1)
PY

rm -f "$TMP"
echo
echo "Device check passed."
echo "NOTE: this proves install, schema and migration. It does NOT prove ringing —"
echo "      only a real inbound call does that."
