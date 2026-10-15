#!/usr/bin/env bash
# stall_test.sh — end-to-end check of "Waste their time" on the ringing screen.
#
# The button answers a ringing call and keeps the caller busy with canned text-to-speech lines.
# Nothing below the Telecom stack can reach it, so this drives the real thing on an emulator:
#
#   A. tap     — an unblocked call rings, the button is on the screen, and tapping it answers the
#                call (SET_ACTIVE). The emulator usually has no TTS engine, so the loop sees two
#                silent lines and the APP ends the call (REQUEST_DISCONNECT) -- which is the
#                watchdog doing its job, and the reason a call can never be left open on a phone
#                whose speech engine is broken. A working engine would run to the 5-minute cap.
#   B. hang up — the caller hangs up while the loop is running; the call must disappear and the
#                app must not crash.
#   C. audio   — after either, the loudspeaker is handed back: the route is no longer SPEAKER.
#
# Usage:
#   ./scripts/stall_test.sh auto                   # emulator only
#   ./scripts/stall_test.sh --device <serial> auto
#   ./scripts/stall_test.sh auto --keep-settings
#
# Needs an emulator (adb emu gsm call), the DEBUG build (run-as reads the database), and the app
# holding the dialer role. Settings are restored on exit.
set -euo pipefail
cd "$(dirname "$0")/.."

PKG=org.carlospinan.cortaspam
DB=cortaspam.db
BUTTON="Waste their time"
DEVICE=""
MODE=""
KEEP=0
A_NUMBER=5551230051
B_NUMBER=5551230052

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --keep-settings) KEEP=1; shift ;;
    auto) MODE="$1"; shift ;;
    --help|-h) sed -n '2,23p' "$0"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
[ -z "$MODE" ] && { sed -n '2,23p' "$0"; exit 2; }

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

case "$DEVICE" in
  emulator-*) ;;
  *) echo "This suite places calls with 'adb emu gsm call', which only a virtual modem has." >&2; exit 2 ;;
esac

$ADB shell "run-as $PKG true" 2>/dev/null || {
  echo "run-as failed. This needs the DEBUG build: ./install_android.sh --device $DEVICE" >&2; exit 1; }
holder=$($ADB shell cmd role get-role-holders android.app.role.DIALER | tr -d '\r')
[ "$holder" = "$PKG" ] || {
  echo "Dialer role is held by '${holder:-nobody}', not $PKG." >&2
  echo "Fix: adb -s $DEVICE shell cmd role add-role-holder android.app.role.DIALER $PKG" >&2; exit 1; }

WORK=$(mktemp -d)

# --- settings -----------------------------------------------------------------------------------
# Same technique as blocked_call_test.sh: write the database with the app stopped, because it
# hydrates settings once at construction.
read_default_action() {
  $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$WORK/db"
  python3 - "$WORK/db" <<'PY'
import sqlite3, sys
rows = dict(sqlite3.connect(sys.argv[1]).execute("SELECT key, value FROM AppSettings").fetchall())
print(rows.get("default_action", "ALLOW"))
PY
}
write_default_action() {
  $ADB shell am force-stop "$PKG"
  local try
  for try in 1 2 3 4 5; do
    $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$WORK/db" 2>/dev/null || true
    [ -s "$WORK/db" ] && break
    sleep 2
  done
  [ -s "$WORK/db" ] || { echo "could not read the database off the device" >&2; return 1; }
  python3 - "$WORK/db" "$1" <<'PY'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
db.execute("INSERT OR REPLACE INTO AppSettings(key, value) VALUES ('default_action', ?)", (sys.argv[2],))
db.commit()
PY
  $ADB shell "run-as $PKG sh -c 'cat > databases/$DB'" < "$WORK/db"
  $ADB shell am force-stop "$PKG"
}
ORIGINAL=$(read_default_action)
restore() {
  [ "$KEEP" = 1 ] && return
  write_default_action "$ORIGINAL" && echo "==> default action restored to: $ORIGINAL"
}
trap 'restore; rm -rf "$WORK"' EXIT

# --- plumbing (see blocked_call_test.sh for why each of these exists) ----------------------------
call_present() { [ -n "$($ADB shell dumpsys telecom 2>/dev/null | grep -m1 'Call id')" ]; }
settle() {
  if call_present; then $ADB shell input keyevent KEYCODE_ENDCALL >/dev/null 2>&1 || true; sleep 2; fi
  if call_present; then $ADB shell cmd telecom cleanup-stuck-calls >/dev/null 2>&1 || true; sleep 2; fi
  return 0
}
timeline() {   # $1 = last two digits, $2 = ignore calls up to this TC id
  $ADB shell dumpsys telecom 2>/dev/null > "$WORK/telecom.txt"
  python3 "$(dirname "$0")/lib/telecom_timeline.py" "$WORK/telecom.txt" "$1" "${2:-0}"
}
max_call_id() {
  $ADB shell dumpsys telecom 2>/dev/null | grep -o "Call *TC@[0-9]*" | grep -o "[0-9]*" | sort -n | tail -1
}
has_event() { grep -q " $2\$" <<<"$1"; }
cause_of()  { grep -m1 "^cause=" <<<"$1" | cut -d= -f2; }

place_and_wait() {   # $1 = number, $2 = last two digits; sets BASE_ID
  local attempt
  for attempt in 1 2 3; do
    BASE_ID=$(max_call_id)
    $ADB shell am start -n "$PKG/org.carlospinan.bloqueador.app.MainActivity" >/dev/null 2>&1
    sleep 2
    $ADB shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
    $ADB emu gsm call "$1" >/dev/null
    for _ in 1 2 3 4 5 6; do
      [ "$(timeline "$2" "$BASE_ID")" != "NOCALL" ] && return 0
      sleep 1
    done
    warn "no call arrived (attempt $attempt); clearing stuck calls"
    $ADB shell cmd telecom cleanup-stuck-calls >/dev/null 2>&1 || true
    sleep 2
  done
  return 0
}

# Taps the node whose text is $1, from a uiautomator dump. Prints nothing; returns 1 when the
# node never shows up. Found by text rather than by coordinate so a layout change fails loudly
# instead of tapping something else.
tap_text() {
  local try
  for try in $(seq 1 10); do
    $ADB shell uiautomator dump /sdcard/stall_ui.xml >/dev/null 2>&1 || true
    $ADB pull /sdcard/stall_ui.xml "$WORK/ui.xml" >/dev/null 2>&1 || true
    local xy
    xy=$(python3 - "$WORK/ui.xml" "$1" <<'PY' 2>/dev/null || true
import re, sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter("node"):
    if n.get("text") == sys.argv[2] or n.get("content-desc") == sys.argv[2]:
        l, t, r, b = map(int, re.findall(r"\d+", n.get("bounds")))
        print((l + r) // 2, (t + b) // 2)
        break
PY
)
    if [ -n "$xy" ]; then $ADB shell input tap $xy; return 0; fi
    sleep 1
  done
  return 1
}

audio_route() {  # prints the active route as Telecom reports it
  $ADB shell dumpsys telecom 2>/dev/null | grep -m1 -oE 'active=\[?[A-Z_]+' | head -1 || true
}

# --- A + C ----------------------------------------------------------------------------------------
echo
echo "======================================================================"
echo " A. Tapping \"$BUTTON\" answers the call, and the app ends it"
echo "======================================================================"
settle
write_default_action ALLOW
$ADB logcat -c
place_and_wait "$A_NUMBER" 51
if [ "$(timeline 51 "$BASE_ID")" = "NOCALL" ]; then
  fail "no call was delivered — the virtual modem or Telecom is wedged, so nothing was tested"
else
  if tap_text "$BUTTON"; then
    pass "the button was on the ringing screen"
    # The loop ends itself: two silent lines on an engine-less emulator, a few seconds each.
    for _ in $(seq 1 45); do
      line=$(timeline 51 "$BASE_ID")
      has_event "$line" SET_DISCONNECTED && break
      sleep 2
    done
    sed 's/^/    /' <<<"$line"
    has_event "$line" SET_ACTIVE && pass "tapping the button answered the call" || fail "the call was never answered"
    if has_event "$line" SET_DISCONNECTED; then
      has_event "$line" REQUEST_DISCONNECT \
        && pass "the app ended the call itself (REQUEST_DISCONNECT)" \
        || warn "the call ended, but not by this app's request — the caller side or Telecom did it"
    else
      fail "the stalled call was still up 90s later — the watchdog did not end it"
    fi
    sleep 2
    route=$(audio_route)
    echo "    audio route after the call: ${route:-unknown}"
    grep -q SPEAKER <<<"$route" && fail "the loudspeaker was left on after the call (C)" || pass "the loudspeaker was handed back (C)"
  else
    fail "\"$BUTTON\" never appeared on the ringing screen"
  fi
fi

# --- B --------------------------------------------------------------------------------------------
echo
echo "======================================================================"
echo " B. The caller hanging up mid-stall ends the call cleanly"
echo "======================================================================"
settle
$ADB logcat -c
place_and_wait "$B_NUMBER" 52
if [ "$(timeline 52 "$BASE_ID")" = "NOCALL" ]; then
  fail "no call was delivered — nothing was tested"
else
  if tap_text "$BUTTON"; then
    sleep 3
    $ADB emu gsm cancel "$B_NUMBER" >/dev/null
    for _ in $(seq 1 10); do
      line=$(timeline 52 "$BASE_ID"); has_event "$line" SET_DISCONNECTED && break; sleep 1
    done
    has_event "$line" SET_DISCONNECTED && pass "the call ended after the caller hung up" || fail "the call outlived the caller's hang-up"
  else
    fail "\"$BUTTON\" never appeared on the ringing screen"
  fi
fi
if $ADB logcat -d 2>/dev/null | grep -q "FATAL EXCEPTION"; then
  fail "the app crashed during the run:"; $ADB logcat -d | grep -A8 "FATAL EXCEPTION" | head -20
else
  pass "no crash in logcat"
fi

echo
[ "$FAILED" = 0 ] && echo -e "${GREEN}All stall scenarios passed.${NC}" || { echo -e "${RED}Stall scenarios failed.${NC}"; exit 1; }
