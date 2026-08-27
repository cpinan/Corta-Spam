#!/usr/bin/env bash
# blocked_call_test.sh — assert that a call the rules block always ENDS.
#
# ring_test.sh proves a blocked call goes silent. Silent is not ended. The bug this script exists
# for was reported by a user on a Redmi Note 13 Pro running Android 16:
#
#   "I have applied the default action Block, and although the number is displayed as Blocked
#    call, it answers itself without my intervention, and I only realize when I hear that the
#    call is in progress."
#
# Three ways that happens, none of which any test in this repo could reach:
#
#   A. reject   — the ordinary path. Telecom refuses the call and it disappears.
#   B. answered — Call.reject() does nothing to a call that is no longer ringing. Anything that
#                 answers first (the ringing screen's own Answer button reached by a cheek or a
#                 pocket, a headset button, the system UI) turned the block into a no-op, and the
#                 app posted "Blocked call" over a live call. See BlockedCallPolicy.
#   C. greeting — with the auto-responder on, a blocked call is answered on purpose so the caller
#                 hears a greeting. If text-to-speech never reports back — no engine, no voice for
#                 the device language, speak() returning ERROR — nothing ended that call at all.
#                 The emulator usually ships without a TTS engine, so it reproduces C by default.
#
# Every scenario asserts the same thing: the call is GONE from Telecom within the deadline. What
# the call did on the way (rang, was answered, played a greeting) is printed, not judged.
#
# Usage:
#   ./scripts/blocked_call_test.sh auto                  # emulator only, end to end
#   ./scripts/blocked_call_test.sh auto --keep-settings  # do not restore settings afterwards
#   ./scripts/blocked_call_test.sh --device <serial> auto
#
# Needs an emulator (adb emu gsm call) and the DEBUG build (run-as reads the database).
set -euo pipefail
cd "$(dirname "$0")/.."

PKG=org.carlospinan.cortaspam
DB=cortaspam.db
DEVICE=""
MODE=""
KEEP=0

# Numbers no rule matches, so every verdict comes from default_action alone. One per scenario,
# with distinct last two digits: Telecom's history redacts all but those, and that suffix is how a
# call is found in it.
A_NUMBER=5551230041
B_NUMBER=5551230042
C_NUMBER=5551230043

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --keep-settings) KEEP=1; shift ;;
    auto) MODE="$1"; shift ;;
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

case "$DEVICE" in
  emulator-*) ;;
  *) echo "This suite places calls with 'adb emu gsm call', which only a virtual modem has." >&2
     echo "On hardware the call has to come from a second phone; ring_test.sh watch is that mode." >&2
     exit 2 ;;
esac

$ADB shell "run-as $PKG true" 2>/dev/null || {
  echo "run-as failed. This needs the DEBUG build: ./install_android.sh --device $DEVICE" >&2; exit 1; }

holder=$($ADB shell cmd role get-role-holders android.app.role.DIALER | tr -d '\r')
if [ "$holder" != "$PKG" ]; then
  echo "Dialer role is held by '${holder:-nobody}', not $PKG — nothing would screen these calls." >&2
  echo "Fix: adb -s $DEVICE shell cmd role add-role-holder android.app.role.DIALER $PKG" >&2
  exit 1
fi

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# --- settings, written straight into the database -----------------------------------------------
# The app hydrates its settings once at construction, so every write here is followed by a
# force-stop. Doing it through the UI instead would mean driving the auto-responder's new
# confirmation dialog by coordinate, which is exactly the kind of blind tap sequence that reads as
# a test failure when a layout moves.
read_settings() {
  $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$WORK/db"
  python3 - "$WORK/db" <<'PY'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
rows = dict(db.execute("SELECT key, value FROM AppSettings").fetchall())
print(rows.get("default_action", "ALLOW"), rows.get("auto_responder_enabled", "false"),
      rows.get("auto_recording_enabled", "false"))
PY
}

write_settings() {  # $1 = default_action, $2 = auto_responder_enabled, $3 = auto_recording_enabled
  # Stopped first, then read: the app hydrates its settings once at construction and holds an open
  # handle on this file. Editing it underneath a live process is how a scenario ends up running
  # against the settings it was supposed to have replaced.
  $ADB shell am force-stop "$PKG"
  # Retried: straight after a reboot the app's data directory is not readable for a second or two,
  # and a failed read here used to abort the whole run under `set -e`.
  local try
  for try in 1 2 3 4 5; do
    $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$WORK/db" 2>/dev/null || true
    [ -s "$WORK/db" ] && break
    sleep 2
  done
  [ -s "$WORK/db" ] || { echo "could not read the database off the device" >&2; return 1; }
  python3 - "$WORK/db" "$1" "$2" "$3" <<'PY'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
for key, value in (("default_action", sys.argv[2]),
                   ("auto_responder_enabled", sys.argv[3]),
                   ("auto_recording_enabled", sys.argv[4])):
    db.execute("INSERT OR REPLACE INTO AppSettings(key, value) VALUES (?, ?)", (key, value))
db.commit()
PY
  $ADB shell "run-as $PKG sh -c 'cat > databases/$DB'" < "$WORK/db"
  $ADB shell am force-stop "$PKG"
}

ORIGINAL=$(read_settings)
echo "==> settings before this run: $ORIGINAL"
restore() {
  [ "$KEEP" = 1 ] && { echo "==> settings left as the run set them (--keep-settings)"; return; }
  # shellcheck disable=SC2086
  write_settings $ORIGINAL
  echo "==> settings restored to: $ORIGINAL"
}
trap 'restore; rm -rf "$WORK"' EXIT

# --- call plumbing ------------------------------------------------------------------------------
# Assertions are read out of Telecom's own per-call event history (`dumpsys telecom`, "Historical
# Events"), not out of polling. A `dumpsys telecom` costs the better part of a second, and these
# calls are decided in one to four: the first version of this script polled, sampled a rejected
# call as "still ringing" for fifteen seconds, and reported a working fix as broken. The history
# names who ended the call and when — REQUEST_REJECT and REQUEST_DISCONNECT are this app's own
# requests, and the disconnect cause separates a rejection from a hang-up.
call_present() {
  [ -n "$($ADB shell dumpsys telecom 2>/dev/null | grep -m1 'Call id')" ]
}

# Telecom on this image can be left holding a call that never finished being created — the anomaly
# watchdog logs "STATE_TIMEOUT ... isCreateConnComplete=false", and from then on every `adb emu gsm
# call` answers OK and delivers nothing at all, which reads as the app silently screening nothing.
# `cleanup-stuck-calls` is what clears it.
settle() {
  if call_present; then
    $ADB shell input keyevent KEYCODE_ENDCALL >/dev/null 2>&1 || true
    sleep 2
  fi
  if call_present; then
    $ADB shell cmd telecom cleanup-stuck-calls >/dev/null 2>&1 || true
    sleep 2
  fi
  call_present && warn "Telecom is still holding a call; the next scenario may be meaningless"
  return 0
}

# Prints the timeline of the most recent call to a number ending in $1: one "<seconds-since-ringing>
# <EVENT>" per line, then "cause=<CODE>". Prints NOCALL if no such call is in the history.
timeline() {   # $1 = last two digits of the number, $2 = ignore calls up to this TC id
  $ADB shell dumpsys telecom 2>/dev/null > "$WORK/telecom.txt"
  python3 "$(dirname "$0")/lib/telecom_timeline.py" "$WORK/telecom.txt" "$1" "${2:-0}"
}

# The highest call id Telecom has issued. Everything this suite asserts is about the call it just
# placed, and the history keeps every call before it -- against the same three numbers, run after
# run.
max_call_id() {
  $ADB shell dumpsys telecom 2>/dev/null |
    grep -o "Call *TC@[0-9]*" | grep -o "[0-9]*" | sort -n | tail -1
}

has_event() { grep -q " $2\$" <<<"$1"; }
at_event()  { grep -m1 " $2\$" <<<"$1" | cut -d' ' -f1; }
cause_of()  { grep -m1 "^cause=" <<<"$1" | cut -d= -f2; }
show_timeline() { sed 's/^/    /' <<<"$1"; }

# The AVD's virtual modem gives up after a handful of calls -- reliably right after one that was
# answered, which is two of the three scenarios here. `adb emu gsm call` still answers OK and
# `dumpsys telephony.registry` still says the radio is in service; the call simply never arrives,
# and from the outside that is indistinguishable from an app that screens nothing. A reboot is the
# only thing that brings it back.
recover_device() {
  warn "the virtual modem stopped delivering calls; rebooting the emulator (about a minute)"
  $ADB reboot
  $ADB wait-for-device
  local booted=""
  for _ in $(seq 1 60); do
    booted=$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    [ "$booted" = "1" ] && break
    sleep 3
  done
  # Booted is not the same as on the network: the radio registers seconds later, and a call
  # placed before it does is accepted by the console and delivered to nobody -- which is the exact
  # failure this function exists to clear.
  local registered=""
  for _ in $(seq 1 20); do
    registered=$($ADB shell dumpsys telephony.registry 2>/dev/null | grep -c "mVoiceRegState=0" || true)
    [ "${registered:-0}" -gt 0 ] && break
    sleep 3
  done
  sleep 5
  # The role does not survive on its own here any more than it survives a reinstall.
  $ADB shell cmd role add-role-holder android.app.role.DIALER "$PKG" >/dev/null 2>&1 || true
  $ADB shell cmd telecom cleanup-stuck-calls >/dev/null 2>&1 || true
  sleep 2
}

# Places the call and waits for Telecom to actually create it, recovering the device in between
# attempts. `adb emu gsm call` answers OK whether or not a call follows, and a wedged Telecom
# swallows them silently -- which is indistinguishable, from the outside, from an app that screens
# nothing.
place_and_wait() {   # $1 = number, $2 = last two digits; sets BASE_ID
  local attempt
  for attempt in 1 2 3; do
    BASE_ID=$(max_call_id)
    place_call "$1"
    for _ in 1 2 3 4 5 6; do
      [ "$(timeline "$2" "$BASE_ID")" != "NOCALL" ] && return 0
      sleep 1
    done
    [ "$attempt" = 3 ] && break
    if [ "$attempt" = 1 ]; then
      warn "no call arrived; clearing stuck calls and trying again"
      $ADB shell cmd telecom cleanup-stuck-calls >/dev/null 2>&1 || true
      sleep 2
    else
      recover_device
    fi
  done
  return 0
}

# Warms the process before the call. A cold start costs three to four seconds on this AVD, all of
# it inside the window being measured, and none of it is what any of these scenarios is about.
place_call() {   # $1 = number
  $ADB shell am start -n "$PKG/org.carlospinan.bloqueador.app.MainActivity" >/dev/null 2>&1
  sleep 2
  $ADB shell input keyevent KEYCODE_HOME >/dev/null 2>&1 || true
  $ADB emu gsm call "$1" >/dev/null
}

# Answers whatever rings next, starting before the call is even placed.
#
# The window is narrow and closes on its own: a warm app rejects a blocked call about 0.8 s after
# it starts ringing, and one `adb shell input` costs roughly a third of that. Firing the hooks
# from a background loop that is already running when the call arrives is what makes the answer
# land first often enough to be worth running. It still loses sometimes, and the scenario says so
# rather than claiming a pass.
#
# KEYCODE_HEADSETHOOK, not KEYCODE_CALL: with no call to answer, CALL opens the dialer and redials
# the last number, and that outgoing call then sat there looking exactly like a blocked call that
# had never ended. HEADSETHOOK does nothing when there is nothing to answer.
answer_whatever_rings() {
  (
    for _ in $(seq 1 12); do
      $ADB shell input keyevent KEYCODE_HEADSETHOOK >/dev/null 2>&1 || true
    done
  ) &
  ANSWERER=$!
}

last_incoming_decision() {   # $1 = full number
  $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$WORK/verify.db" 2>/dev/null || return 1
  python3 "$(dirname "$0")/lib/last_decision.py" "$WORK/verify.db" "$1"
}

# --- A --------------------------------------------------------------------------------------------
echo
echo "======================================================================"
echo " A. A blocked call with the auto-responder off is rejected"
echo "======================================================================"
settle
write_settings BLOCK false false
place_and_wait "$A_NUMBER" 41
sleep 10
line=$(timeline 41 "$BASE_ID")
show_timeline "$line"
echo "    app decision: $(last_incoming_decision "$A_NUMBER")"
if [ "$line" = "NOCALL" ]; then
  fail "no call was delivered — the virtual modem or Telecom is wedged, so nothing was tested"
elif has_event "$line" SET_ACTIVE; then
  fail "the blocked call was connected — the caller was let through"
elif ! has_event "$line" SET_DISCONNECTED; then
  fail "the blocked call was still up 12s later"
elif [ "$(cause_of "$line")" != "REJECTED" ]; then
  fail "the call ended, but as $(cause_of "$line") rather than a rejection by this app"
else
  pass "rejected after $(at_event "$line" SET_DISCONNECTED)s, never connected"
fi

# --- B --------------------------------------------------------------------------------------------
echo
echo "======================================================================"
echo " B. A blocked call that gets answered anyway is hung up"
echo "======================================================================"
settle
write_settings BLOCK false false
BASE_ID=$(max_call_id)
answer_whatever_rings
place_call "$B_NUMBER"
wait "$ANSWERER" 2>/dev/null || true
sleep 12
line=$(timeline 42 "$BASE_ID")
if [ "$line" = "NOCALL" ]; then
  recover_device
  write_settings BLOCK false false
  BASE_ID=$(max_call_id)
  answer_whatever_rings
  place_call "$B_NUMBER"
  wait "$ANSWERER" 2>/dev/null || true
  sleep 12
  line=$(timeline 42 "$BASE_ID")
fi
show_timeline "$line"
echo "    app decision: $(last_incoming_decision "$B_NUMBER")"
if [ "$line" = "NOCALL" ]; then
  fail "no call was delivered — the virtual modem or Telecom is wedged, so nothing was tested"
elif ! has_event "$line" SET_ACTIVE; then
  # Not a failure of the app: the rules rejected the call before the answer landed. Calling that a
  # pass would be worse — this run never reached the state the scenario exists for. Scenario C
  # reaches the same answered-and-blocked call deterministically.
  warn "the answer lost the race with the rules, so this run only repeated scenario A"
elif ! has_event "$line" SET_DISCONNECTED; then
  fail "an answered blocked call was never ended — this is the reported bug"
elif ! has_event "$line" REQUEST_DISCONNECT; then
  fail "the call ended, but nothing in this app asked it to (cause $(cause_of "$line"))"
else
  pass "answered at $(at_event "$line" SET_ACTIVE)s, hung up by the app at $(at_event "$line" SET_DISCONNECTED)s"
fi

# --- C --------------------------------------------------------------------------------------------
echo
echo "======================================================================"
echo " C. A blocked call answered for a greeting is always hung up"
echo "======================================================================"
echo "    text-to-speech engines installed: $($ADB shell pm list packages 2>/dev/null | grep -ci 'tts\|texttospeech' || true)"
settle
write_settings BLOCK true false
place_and_wait "$C_NUMBER" 43
sleep 25
line=$(timeline 43 "$BASE_ID")
show_timeline "$line"
echo "    app decision: $(last_incoming_decision "$C_NUMBER")"
if [ "$line" = "NOCALL" ]; then
  fail "no call was delivered — the virtual modem or Telecom is wedged, so nothing was tested"
elif ! has_event "$line" SET_ACTIVE; then
  warn "the auto-responder did not answer the call, so the greeting watchdog was not exercised"
  echo "    (check that auto_responder_enabled took effect, and that the greeting validates)"
elif ! has_event "$line" SET_DISCONNECTED; then
  fail "a call answered for the greeting was still connected 30s later — the reported bug"
else
  pass "answered at $(at_event "$line" SET_ACTIVE)s, ended at $(at_event "$line" SET_DISCONNECTED)s"
fi
$ADB logcat -d 2>/dev/null | grep -E "greeting never (started|finished)|refused the greeting|failed to initialise" | tail -2 | sed 's/^/    /' || true

settle

echo
if [ "$FAILED" = 0 ]; then
  echo -e "${GREEN}Every blocked call ended, on every path reachable here.${NC}"
else
  echo -e "${RED}A blocked call stayed connected. That is the bug users hear as the app answering by itself.${NC}"
  exit 1
fi
