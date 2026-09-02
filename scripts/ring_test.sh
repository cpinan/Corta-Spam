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
#   ./scripts/ring_test.sh dnd                   # emulator only: the Do Not Disturb truth table
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
    auto|watch|dnd) MODE="$1"; shift ;;
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

# `appId=` is what current Android prints; `userId=` is the older name and is what the emulator
# this script was written against (API 33) still uses. Matching only the old one made the script
# exit "not installed" on a razr 50 ultra (Android 16) that had the app installed and running --
# so `watch`, the mode that exists specifically for real hardware, had never once run on real
# hardware. Both spellings are accepted, and a missing package is now distinguished from a field
# rename by asking pm directly.
UID_APP=$($ADB shell dumpsys package "$PKG" 2>/dev/null \
  | awk -F= '/appId=|userId=/{gsub(/[^0-9]/,"",$2); if ($2 != "") {print $2; exit}}' | tr -d '\r')
if [ -z "$UID_APP" ]; then
  if $ADB shell pm list packages 2>/dev/null | grep -q "^package:$PKG$"; then
    echo "$PKG is installed on $DEVICE, but neither appId= nor userId= was found in" >&2
    echo "dumpsys package output. The uid field has been renamed again — fix this grep." >&2
  else
    echo "$PKG is not installed on $DEVICE." >&2
  fi
  exit 1
fi
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

# Same reasoning as the ringer mode above, for the other setting that legitimately silences the
# phone: with default_action=BLOCK, the unmatched number this test rings with is blocked by the
# default path and CallRinger is stopped a few hundred milliseconds in — correctly. The test then
# printed "Ringing FAILED. Users of this build would miss calls silently", which is both false and
# the most alarming line in the suite. It is easy to hit by accident, because rule_matrix_test.sh
# leaves the device on BLOCK when its last phase is one of the default-BLOCK ones.
#
# Read from the app database, not from a flag: nothing else knows what the user's default action is.
# A release build has no run-as, so an unreadable database is not treated as a failure here.
default_action=$($ADB exec-out run-as "$PKG" cat "databases/$DB" 2>/dev/null > /tmp/ring_test_db.$$ \
  && python3 -c "
import sqlite3, sys
try:
    r = sqlite3.connect('/tmp/ring_test_db.$$').execute(
        \"SELECT value FROM AppSettings WHERE key='default_action'\").fetchone()
    print(r[0] if r else 'ALLOW')
except Exception:
    print('')
" || echo "")
rm -f "/tmp/ring_test_db.$$"
if [ "$default_action" = "BLOCK" ]; then
  echo -e "${RED}Default action is BLOCK.${NC} The unmatched test number will be blocked, and the" >&2
  echo "app will correctly stop ringing — this test cannot tell that apart from a broken ringer." >&2
  echo "Fix: set Settings > Default action to Allow, or re-run after a phase that leaves it there." >&2
  exit 2
fi
[ -n "$default_action" ] && echo "==> default action $default_action"

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

# Whether the ringing notification was posted, independent of whether anything made a sound.
# Do Not Disturb silences a call; it must not hide one. Answer and Decline live on this
# notification, so a filtered call that posts nothing leaves the user no way to take it.
assert_notified() {  # $1 = human label
  if $ADB shell dumpsys notification --noredact 2>/dev/null | grep -q "$PKG.*incoming_calls_v2\|incoming_calls_v2.*$PKG"; then
    pass "ringing notification posted ($1)"
  else
    fail "no ringing notification posted ($1) — a silenced call must still be answerable"
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

dnd)
  # The Do Not Disturb truth table, on a device.
  #
  # Declaring IN_CALL_SERVICE_RINGING took zen filtering away from Telecom and gave it to this
  # app, which for nine releases never did it: AudioManager.ringerMode does not move when Do Not
  # Disturb turns on, so CallRinger rang for every call the user had asked not to hear. Unit tests
  # cover RingerPolicy's decisions; only a device shows that NotificationManager reports what the
  # policy expects and that the ringer actually stays quiet.
  #
  # Both directions are asserted. A fix that simply stopped ringing under Do Not Disturb would
  # pass "stranger is silent" and be a far worse bug than the one it replaced.
  case "$DEVICE" in
    emulator-*) ;;
    *) echo -e "${RED}dnd needs an emulator${NC} (adb emu gsm call). On hardware, call from a" >&2
       echo "second phone with Do Not Disturb on and watch." >&2
       exit 2 ;;
  esac

  # Which callers this device's Do Not Disturb actually lets through, read from the device rather
  # than assumed. The AOSP default here is PRIORITY_SENDERS_STARRED, not contacts -- and picking
  # an ordinary contact for the "must still ring" half then fails against an app that is behaving
  # perfectly, which is exactly what happened the first time this test ran.
  SENDERS=$($ADB shell dumpsys notification 2>/dev/null \
    | sed -n 's/.*priorityCallSenders=\(PRIORITY_SENDERS_[A-Z]*\).*/\1/p' | head -1 | tr -d '\r')
  SENDERS=${SENDERS:-PRIORITY_SENDERS_ANY}
  echo "==> device Do Not Disturb lets calls through from: $SENDERS"

  CONTACT=$($ADB shell "content query --uri content://com.android.contacts/data/phones --projection data1" 2>/dev/null \
    | sed -n 's/.*data1=\(.*\)$/\1/p' | tr -d '\r' | head -1)
  if [ -z "$CONTACT" ]; then
    echo -e "${RED}No contacts on this device.${NC} The half of this test that proves the fix did" >&2
    echo "not simply deafen the phone needs one. Run ./scripts/seed_screenshots.sh first." >&2
    exit 2
  fi

  # Starred-only is the default policy, so the contact this test rings with has to be starred or
  # the app is right to stay silent and the test is wrong. Starred through raw_contacts, which is
  # what the aggregated Phone.STARRED column the app reads is derived from.
  if [ "$SENDERS" = "PRIORITY_SENDERS_STARRED" ]; then
    RID=$($ADB shell "content query --uri content://com.android.contacts/data/phones --projection raw_contact_id:data1 --where \"data1='$CONTACT'\"" 2>/dev/null \
      | sed -n 's/.*raw_contact_id=\([0-9]*\).*/\1/p' | head -1 | tr -d '\r')
    if [ -n "$RID" ]; then
      $ADB shell "content update --uri content://com.android.contacts/raw_contacts --bind starred:i:1 --where \"_id=$RID\"" >/dev/null 2>&1 || true
      echo "==> starred $CONTACT (raw_contact $RID) so it qualifies under this policy"
      # The gateway caches the address book for five minutes; a star it has not seen is a star
      # the ringer will not act on.
      $ADB shell am force-stop "$PKG" >/dev/null 2>&1 || true
      sleep 2
    fi
  fi

  SUFFIX=$(date +%s | tail -c 7)
  STRANGER="+3480${SUFFIX}"
  case "$STRANGER" in *0000) STRANGER="${STRANGER%0}1" ;; esac

  restore_dnd() { $ADB shell cmd notification set_dnd off >/dev/null 2>&1 || true; }
  trap restore_dnd EXIT

  place() {   # $1 = number, $2 = seconds to wait
    $ADB emu gsm cancel "$1" >/dev/null 2>&1 || true
    $ADB logcat -c
    $ADB shell service call notification 1 >/dev/null 2>&1 || true
    sleep 1
    $ADB emu gsm call "$1" >/dev/null
    sleep "${2:-6}"
  }

  echo
  echo "=== 1/3  priority only, stranger $STRANGER — must stay silent ==="
  $ADB shell cmd notification set_dnd priority >/dev/null
  sleep 2
  place "$STRANGER" 7
  assert_silent "stranger under Do Not Disturb"
  assert_notified "stranger under Do Not Disturb"
  $ADB emu gsm cancel "$STRANGER" >/dev/null 2>&1 || true
  sleep 3

  echo
  echo "=== 2/3  priority only, contact $CONTACT — must still ring ==="
  place "$CONTACT" 7
  assert_ringing "contact under Do Not Disturb"
  $ADB emu gsm cancel "$CONTACT" >/dev/null 2>&1 || true
  sleep 3

  echo
  echo "=== 3/3  total silence, contact $CONTACT — must stay silent ==="
  $ADB shell cmd notification set_dnd none >/dev/null
  sleep 2
  place "$CONTACT" 7
  assert_silent "contact under total silence"
  $ADB emu gsm cancel "$CONTACT" >/dev/null 2>&1 || true
  sleep 2

  restore_dnd
  trap - EXIT

  echo
  echo "==> last call log rows"
  last_log_rows 4

  echo
  if [ "$FAILED" = 0 ]; then
    echo -e "${GREEN}Do Not Disturb honoured.${NC} Silenced the stranger, still rang the contact,"
    echo "and total silence beat both."
  else
    echo -e "${RED}Do Not Disturb FAILED.${NC}"
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
