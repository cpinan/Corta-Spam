#!/usr/bin/env bash
# call_test.sh — the parts of a live inbound-call test a machine can do.
#
# device_check.sh ends by saying it proves install, schema and migration but NOT ringing. This is
# the other half. Two behaviours in this app exist only during a real inbound call and cannot be
# reached by any automated test:
#
#   * ringing      — the app declares IN_CALL_SERVICE_RINGING, so Telecom stops ringing and
#                    CallRinger must. If it fails on an OEM, that phone rings for nothing.
#                    THIS IS NOW ASSERTED BY ring_test.sh, which checks the three signals a
#                    machine can read (Telecom's stand-down line, the app's own audio player,
#                    the vibration's opPkg). Run that first; this script's `watch` only prints
#                    log lines for a human to interpret.
#   * recording    — AutoResponderRecorder captures AudioSource.MIC acoustically, because the real
#                    call sources need CAPTURE_AUDIO_OUTPUT (signature|privileged; ROLE_DIALER does
#                    not grant it). Several OEMs reserve the mic for telephony during a call, in
#                    which case start() returns false. That is an expected outcome, not a bug.
#
# The call itself needs a second phone and a human. This script does everything either side of it.
#
# Usage:
#   ./scripts/call_test.sh preflight             # is the device set up to make the test meaningful?
#   ./scripts/call_test.sh watch                 # tail the only log lines that matter, then call
#   ./scripts/call_test.sh verify                # after the call: did a recording land?
#   ./scripts/call_test.sh --device <serial> ... # pick a device
set -euo pipefail
cd "$(dirname "$0")/.."

# applicationId, not the code namespace — adb/run-as/pm/cmd role all key off this one.
PKG=org.carlospinan.cortaspam
DB=cortaspam.db
DEVICE=""
MODE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    preflight|watch|verify) MODE="$1"; shift ;;
    --help|-h) sed -n '2,21p' "$0"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
[ -z "$MODE" ] && { sed -n '2,21p' "$0"; exit 2; }

if [ -z "$DEVICE" ]; then
  DEVICE=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')
  [ -z "$DEVICE" ] && { echo "No device connected (adb devices)." >&2; exit 1; }
fi
ADB="adb -s $DEVICE"
echo "==> device: $DEVICE"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

case "$MODE" in

preflight)
  fail=0

  echo "==> dialer role"
  holder=$($ADB shell cmd role get-role-holders android.app.role.DIALER | tr -d '\r')
  if [ "$holder" = "$PKG" ]; then
    echo -e "    ${GREEN}held by $PKG${NC}"
  else
    echo -e "    ${RED}held by '${holder:-nobody}'${NC}"
    echo "    Without the role this app screens nothing and the test proves nothing."
    echo "    Fix: Settings > Apps > Default apps > Phone app > Corta Spam"
    fail=1
  fi

  echo "==> microphone permission"
  # GRANTED_BY_ROLE is normal here: holding ROLE_DIALER auto-grants the dialer permission group,
  # so the in-app "Grant microphone access" button never appears on such a device.
  mic=$($ADB shell dumpsys package "$PKG" 2>/dev/null | grep -m1 "RECORD_AUDIO: granted" | tr -d '\r' || true)
  case "$mic" in
    *granted=true*) echo -e "    ${GREEN}granted${NC}${mic#*granted=true}" ;;
    *) echo -e "    ${RED}not granted — recording cannot start${NC}"
       echo "    Fix: adb -s $DEVICE shell pm grant $PKG android.permission.RECORD_AUDIO"
       fail=1 ;;
  esac

  echo "==> auto-responder config"
  # Read the app's own key-value settings rather than trusting the UI. Recording only runs when
  # the responder is enabled AND recordingEnabled is set AND the greeting carries a consent
  # phrase — AutoResponderConfig.validate() rejects the config otherwise and nothing auto-answers.
  tmp=$(mktemp)
  if $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$tmp" 2>/dev/null && [ -s "$tmp" ]; then
    python3 - "$tmp" <<'PY' || fail=1
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
try:
    rows = dict(db.execute("SELECT key, value FROM AppSettings").fetchall())
except Exception as e:
    print(f"    could not read AppSettings ({e}); check by hand in the app")
    sys.exit(0)
CONSENT = ["this call may be recorded", "esta llamada puede ser grabada",
           "इस कॉल को रिकॉर्ड किया जा सकता है", "esta chamada pode ser gravada"]
enabled   = rows.get("auto_responder_enabled", "")
recording = rows.get("auto_recording_enabled", "")
script    = rows.get("auto_responder_script", "") or ""
bad = []
print(f"    auto_responder_enabled  : {enabled or '(unset)'}")
print(f"    auto_recording_enabled  : {recording or '(unset)'}")
print(f"    greeting                : {script[:60] or '(default)'}")
if enabled not in ("true", "1"):
    bad.append("auto-responder is off — blocked calls will be rejected, not answered")
if recording not in ("true", "1"):
    bad.append("recording is off — nothing will be captured")
elif not any(c in script.lower() for c in CONSENT):
    bad.append("greeting has no consent phrase — validate() fails and the responder never answers")
for b in bad:
    print(f"    !! {b}")
sys.exit(1 if bad else 0)
PY
  else
    echo -e "    ${YELLOW}could not pull the database (release build is not debuggable?)${NC}"
    echo "    Install the debug build to read config, or verify in the app by hand."
  fi
  rm -f "$tmp"

  echo "==> blocked numbers (the caller must match one)"
  $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$tmp" 2>/dev/null || true
  if [ -s "$tmp" ]; then
    python3 - "$tmp" <<'PY'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
rows = db.execute("SELECT number FROM BlockedNumber ORDER BY id DESC LIMIT 5").fetchall()
print("    " + (", ".join(r[0] for r in rows) if rows else "(none — the test call will ring normally)"))
PY
  fi
  rm -f "$tmp"

  echo
  if [ "$fail" = 0 ]; then
    echo -e "${GREEN}Preflight passed.${NC} Now: ./scripts/call_test.sh watch --device $DEVICE"
  else
    echo -e "${RED}Preflight failed — fix the items above or the call proves nothing.${NC}"
    exit 1
  fi
  ;;

watch)
  echo "==> clearing logcat, then tailing. Place the call now; Ctrl-C when it ends."
  echo "    Expected on success : 'Recorder' lines with no warning, call ends after the greeting"
  echo "    Expected OEM refusal: 'Microphone unavailable during the call'"
  echo
  $ADB logcat -c
  $ADB logcat | grep --line-buffered -iE "AutoResponderRecorder|PassthroughInCallService|CallRinger|MediaRecorder|IncomingCallNotifier"
  ;;

verify)
  tmp=$(mktemp)
  echo "==> recording files on device"
  files=$($ADB shell run-as "$PKG" ls -la files/recordings/ 2>/dev/null | tr -d '\r' || true)
  if [ -n "$files" ]; then echo "$files" | sed 's/^/    /'; else echo "    (directory absent or empty)"; fi

  echo "==> call log rows"
  if $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$tmp" 2>/dev/null && [ -s "$tmp" ]; then
    python3 - "$tmp" <<'PY'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
rows = db.execute(
    "SELECT id, number, action, rule_type, recording_path "
    "FROM CallLogEntry ORDER BY id DESC LIMIT 5").fetchall()
if not rows:
    print("    (call log is empty — the call was never screened; is the dialer role held?)")
for i, n, a, rt, rp in rows:
    print(f"    #{i} {n} {a}/{rt or '-'} recording={rp or 'none'}")
with_audio = [r for r in rows if r[4]]
print()
if with_audio:
    print(f"    RESULT: {len(with_audio)} of the last {len(rows)} entries carry a recording.")
    # run-as starts in the app's data dir, so it wants the path relative to that, not the
    # absolute one stored in the column.
    rel = with_audio[0][4].split("/files/")[-1]
    print("    Play it in the app, or pull it:")
    print(f"      adb exec-out run-as org.carlospinan.cortaspam cat files/{rel} > rec.m4a")
else:
    print("    RESULT: no recording attached to any recent call.")
    print("    If logcat showed 'Microphone unavailable', the OEM refused — expected on some")
    print("    devices, and the honest README wording covers it. If logcat showed nothing at")
    print("    all, the call was never auto-answered: re-run preflight.")
PY
  else
    echo "    could not pull the database"
  fi
  rm -f "$tmp"
  ;;
esac
