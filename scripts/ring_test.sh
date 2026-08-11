#!/usr/bin/env bash
# ring_test.sh — assert that the app actually rings, and actually stays silent.
#
# call_test.sh covers the recording half of a live call and leaves ringing to a human reading
# logcat. This is the ringing half, asserted by a machine.
#
# The app declares IN_CALL_SERVICE_RINGING, which tells Telecom to stop ringing for incoming calls
# because this app will do it. If CallRinger fails on some OEM, the phone rings for nothing and the
# user misses calls with no error anywhere. That failure is invisible to every unit test in the
# repo, and it is invisible to a screenshot.
#
# Three independent signals are checked, because any one of them alone can lie:
#
#   1. Telecom logs "Ringer: Ending early -- ... letDialerHandleRinging=true" — proof the platform
#      stood down and handed ringing over. Without this line the system was still ringing and a
#      ringtone you heard proves nothing about this app.
#   2. dumpsys audio lists an AudioPlaybackConfiguration owned by THIS app's uid, state:started,
#      with usage=USAGE_NOTIFICATION_RINGTONE. This is the app's own player, not the system's.
#   3. dumpsys vibrator_manager shows a running RINGTONE vibration whose opPkg is this package.
#      An emulator reports scale 0.00 (no vibrator hardware); "running" is still the assertion.
#
# Usage:
#   ./scripts/ring_test.sh auto                  # emulator only: place calls and assert, end to end
#   ./scripts/ring_test.sh watch                 # real device: a human calls, this samples and asserts
#   ./scripts/ring_test.sh --device <serial> ...
#
# auto needs an emulator (adb emu gsm call). A physical phone has no fake modem, so on hardware the
# call has to come from a second phone and `watch` is the mode.
set -euo pipefail
cd "$(dirname "$0")/.."

PKG=org.carlospinan.cortaspam
DB=cortaspam.db
DEVICE=""
MODE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    auto|watch) MODE="$1"; shift ;;
    --help|-h) sed -n '2,31p' "$0"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
[ -z "$MODE" ] && { sed -n '2,31p' "$0"; exit 2; }

if [ -z "$DEVICE" ]; then
  DEVICE=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')
  [ -z "$DEVICE" ] && { echo "No device connected (adb devices)." >&2; exit 1; }
fi
ADB="adb -s $DEVICE"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
pass() { echo -e "    ${GREEN}PASS${NC} $1"; }
fail() { echo -e "    ${RED}FAIL${NC} $1"; FAILED=1; }
warn() { echo -e "    ${YELLOW}WARN${NC} $1"; }
FAILED=0

UID_APP=$($ADB shell dumpsys package "$PKG" 2>/dev/null | awk -F= '/userId=/{print $2; exit}' | tr -d '\r')
[ -z "$UID_APP" ] && { echo "$PKG is not installed on $DEVICE." >&2; exit 1; }
echo "==> device $DEVICE, $PKG uid $UID_APP"

# The dialer role is the whole precondition: without it Telecom never binds this app and no call
# reaches it, so every assertion below would fail for a reason that has nothing to do with ringing.
holder=$($ADB shell cmd role get-role-holders android.app.role.DIALER 2>/dev/null | tr -d '\r' || true)
# API 33 has no get-role-holders subcommand and prints "Unknown command: ..." to STDOUT, not
# stderr, so the error text arrives looking exactly like a role holder's package name.
case "$holder" in *"Unknown command"*|*"Error:"*) holder="" ;; esac
if [ "$holder" = "$PKG" ]; then
  echo "==> dialer role held"
elif [ -z "$holder" ]; then
  # API 33 and below have no get-role-holders subcommand; fall back to the permission side effect.
  if $ADB shell dumpsys package "$PKG" | grep -q "GRANTED_BY_ROLE"; then
    echo "==> dialer role held (inferred from GRANTED_BY_ROLE; this API has no get-role-holders)"
  else
    echo -e "${RED}Dialer role not held. Nothing will reach the app.${NC}" >&2
    echo "Fix: $ADB shell cmd role add-role-holder android.app.role.DIALER $PKG" >&2
    exit 1
  fi
else
  echo -e "${RED}Dialer role held by '$holder', not $PKG.${NC}" >&2
  exit 1
fi

# CallRinger honours the system ringer mode, so on a silenced phone it correctly plays nothing.
# Asserting "the app rings" there would report FAIL for behaviour that is right, which is worse
# than not running: a test that cries wolf gets ignored the day it is telling the truth.
mode=$($ADB shell settings get global mode_ringer 2>/dev/null | tr -d '\r' || echo 2)
case "$mode" in
  2) echo "==> ringer mode normal" ;;
  1) echo -e "${RED}Ringer is on VIBRATE.${NC} CallRinger will not play a ringtone, correctly." >&2
     echo "This test cannot prove ringing here. Fix: $ADB shell settings put global mode_ringer 2" >&2
     exit 2 ;;
  *) echo -e "${RED}Ringer is SILENT (mode_ringer=$mode).${NC} CallRinger will play nothing, correctly." >&2
     echo "This test cannot prove ringing here. Fix: $ADB shell settings put global mode_ringer 2" >&2
     exit 2 ;;
esac

# ---------------------------------------------------------------------------
# Assertions, run while a call is ringing.
# ---------------------------------------------------------------------------

assert_ringing() {   # $1 = human label
  echo "==> $1: expecting the app to ring"

  if $ADB logcat -d 2>/dev/null | grep -q "letDialerHandleRinging=true"; then
    pass "Telecom stood down (letDialerHandleRinging=true)"
  else
    fail "Telecom did NOT hand ringing over — IN_CALL_SERVICE_RINGING may not be in effect"
  fi

  local players
  players=$($ADB shell dumpsys audio 2>/dev/null | sed -n '/players:/,/^$/p' || true)
  if echo "$players" | grep "u/pid:$UID_APP/" | grep -q "state:started"; then
    if echo "$players" | grep "u/pid:$UID_APP/" | grep -q "USAGE_NOTIFICATION_RINGTONE"; then
      pass "app's own player started with USAGE_NOTIFICATION_RINGTONE"
    else
      # A started player with the wrong usage still routes to the wrong stream: it will not follow
      # the ringer volume or respect silent mode.
      fail "app is playing audio but NOT as USAGE_NOTIFICATION_RINGTONE"
    fi
  else
    fail "no started audio player owned by uid $UID_APP — the phone is silent"
  fi

  local vib
  vib=$($ADB shell dumpsys vibrator_manager 2>/dev/null | sed -n '/mCurrentVibration:/,/mNextVibration:/p' || true)
  if echo "$vib" | grep -q "opPkg: $PKG"; then
    if echo "$vib" | grep -q "status: running"; then
      pass "RINGTONE vibration running, attributed to $PKG"
    else
      warn "vibration attributed to $PKG but not running (device may have no vibrator)"
    fi
  else
    warn "no vibration attributed to $PKG (emulators often report none)"
  fi
}

assert_silent() {    # $1 = human label
  echo "==> $1: expecting silence"

  local players
  players=$($ADB shell dumpsys audio 2>/dev/null | sed -n '/players:/,/^$/p' || true)
  if echo "$players" | grep "u/pid:$UID_APP/" | grep -q "state:started"; then
    fail "app started an audio player for a call it blocked — the phone rang"
  else
    pass "no audio player started — phone stayed silent"
  fi
}

# Pulls the call log. Only works against a debuggable build; the ringing assertions above do not
# need it, so a release build degrades to those rather than failing outright.
last_log_rows() {    # $1 = how many
  local tmp; tmp=$(mktemp)
  if $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$tmp" 2>/dev/null && [ -s "$tmp" ]; then
    python3 - "$tmp" "$1" <<'PY'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
for i, n, a, rt in db.execute(
        "SELECT id, number, action, rule_type FROM CallLogEntry ORDER BY id DESC LIMIT ?",
        (int(sys.argv[2]),)):
    print(f"    #{i} {n} {a}/{rt or '-'}")
PY
  else
    echo "    (call log unreadable — release builds are not debuggable; ringing checks above still stand)"
  fi
  rm -f "$tmp"
}

case "$MODE" in

auto)
  case "$DEVICE" in
    emulator-*) ;;
    *) echo -e "${RED}auto needs an emulator.${NC} A physical phone has no fake modem to place a" >&2
       echo "call with. Use: ./scripts/ring_test.sh watch --device $DEVICE" >&2
       exit 2 ;;
  esac

  # A number that has called recently trips the repeat-caller and action rules — three attempts in
  # five minutes got a "should ring" number BLOCKED mid-test on 2026-08-11 and invalidated the run.
  # Deriving it from the clock keeps every run a first-time caller.
  SUFFIX=$(date +%s | tail -c 7)
  ALLOWED="+3480${SUFFIX}"
  case "$ALLOWED" in *0000) ALLOWED="${ALLOWED%0}1" ;; esac   # *0000 is a seeded demo pattern

  BLOCKED=$($ADB exec-out run-as "$PKG" cat "databases/$DB" 2>/dev/null \
    | python3 -c "
import sqlite3,sys,tempfile,os
d=sys.stdin.buffer.read()
if not d: sys.exit(0)
f=tempfile.NamedTemporaryFile(delete=False); f.write(d); f.close()
try:
    r=sqlite3.connect(f.name).execute('SELECT number FROM BlockedNumber ORDER BY id LIMIT 1').fetchone()
    print(r[0] if r else '')
finally:
    os.unlink(f.name)" 2>/dev/null || true)

  # Memory of a real trap: a call left RINGING blocks every later 'gsm call' with a silent OK.
  $ADB emu gsm cancel "$ALLOWED" >/dev/null 2>&1 || true

  echo
  echo "=== 1/2  unmatched number $ALLOWED — must ring ==="
  $ADB logcat -c
  $ADB shell input keyevent 26 >/dev/null 2>&1 || true   # screen off: the case FSI exists for
  sleep 2
  $ADB emu gsm call "$ALLOWED" >/dev/null
  sleep 6
  assert_ringing "$ALLOWED"
  resumed=$($ADB shell dumpsys activity activities 2>/dev/null | grep -m1 ResumedActivity | tr -d '\r' || true)
  case "$resumed" in
    *InCallActivity*) pass "InCallActivity is on top over the lock screen (full-screen intent)" ;;
    *) fail "InCallActivity is not resumed — got: ${resumed:-nothing}" ;;
  esac
  $ADB emu gsm cancel "$ALLOWED" >/dev/null 2>&1 || true
  sleep 3

  echo
  if [ -n "$BLOCKED" ]; then
    echo "=== 2/2  blocked number $BLOCKED — must stay silent ==="
    $ADB logcat -c
    $ADB shell input keyevent 26 >/dev/null 2>&1 || true
    sleep 2
    $ADB emu gsm call "$BLOCKED" >/dev/null
    sleep 6
    assert_silent "$BLOCKED"
    $ADB emu gsm cancel "$BLOCKED" >/dev/null 2>&1 || true
    sleep 2
  else
    warn "no blocked numbers on the device — skipping the silence half"
    echo "    Add one in the app, or the test only proves the phone can ring, not that it can not."
  fi

  echo
  echo "==> last call log rows"
  last_log_rows 4

  echo
  if [ "$FAILED" = 0 ]; then
    echo -e "${GREEN}Ringing verified.${NC} This was an emulator — an OEM that reserves audio during"
    echo "a call still has to be checked with: ./scripts/ring_test.sh watch --device <phone>"
  else
    echo -e "${RED}Ringing FAILED. Users of this build would miss calls silently.${NC}"
    exit 1
  fi
  ;;

watch)
  echo "==> Place a call to this phone from another phone NOW."
  echo "    Sampling for 45s; assertions run while it is ringing. Do not answer it."
  $ADB logcat -c
  for i in $(seq 1 45); do
    state=$($ADB shell dumpsys telecom 2>/dev/null | grep -c "state=RINGING" || true)
    if [ "${state:-0}" -gt 0 ]; then
      echo "==> ringing call detected after ${i}s"
      sleep 2   # let CallRinger start; asserting the instant Telecom sees the call races it
      assert_ringing "inbound call"
      echo
      echo "==> last call log rows"
      last_log_rows 3
      echo
      if [ "$FAILED" = 0 ]; then
        echo -e "${GREEN}Ringing verified on this hardware.${NC}"
      else
        echo -e "${RED}Ringing FAILED on this hardware — this OEM needs a fix before release.${NC}"
        exit 1
      fi
      exit 0
    fi
    sleep 1
  done
  echo -e "${RED}No ringing call seen in 45s.${NC}" >&2
  echo "If the phone did ring, Telecom never reported RINGING and this app was not bound —" >&2
  echo "check the dialer role." >&2
  exit 1
  ;;
esac
