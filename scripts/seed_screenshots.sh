#!/usr/bin/env bash
# seed_screenshots.sh — put the app into the state the store screenshots are taken in.
#
# The listing screenshots need an app that looks lived-in: counters that are not zero, a call log
# with real reasons on it, rules in the lists. That state used to be produced by hand, which meant
# every retake was a different app and nobody could reproduce the last one. On 2026-08-20 all ten
# shipped screenshots had to be retaken at once — they still showed Material 3's baseline purple,
# because they predated the theme — and there was nothing to re-run.
#
# This seeds only what the app owns: its rules, its settings and its call log. Contacts belong to
# the device, so they are checked and reported rather than written; see CONTACTS below.
#
# Timestamps are relative to now, so the Home counters read the same on any day it is run.
#
# Usage:
#   ./scripts/seed_screenshots.sh                          # seed, leave the language alone
#   ./scripts/seed_screenshots.sh --locale es-ES           # seed and switch the app to Spanish
#   ./scripts/seed_screenshots.sh --locale-reset           # hand the language back to the system
#   ./scripts/seed_screenshots.sh --device emulator-5556
#
# Requires: a DEBUG build (run-as reads and writes the database) and, for the language switch,
# API 33+ (`cmd locale set-app-locales`, which needs no reboot).
#
# After it runs, capture into docs/store/ as NN_name.png (es_NN_name.png for Spanish) and then
# run ./scripts/play_assets.sh, which pads them to the 9:16 Play insists on.
set -euo pipefail
cd "$(dirname "$0")/.."

PKG=org.carlospinan.cortaspam
DB=cortaspam.db
DEVICE=""
LOCALE=""
LOCALE_RESET=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --locale) LOCALE="$2"; shift 2 ;;
    --locale-reset) LOCALE_RESET=true; shift ;;
    --help|-h) sed -n '2,25p' "$0"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

if [ -z "$DEVICE" ]; then
  DEVICE=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')
  [ -z "$DEVICE" ] && { echo "No device connected (adb devices)." >&2; exit 1; }
fi
ADB="adb -s $DEVICE"
echo "==> device: $DEVICE"

$ADB shell pm list packages 2>/dev/null | grep -q "$PKG" || { echo "$PKG not installed." >&2; exit 1; }
$ADB shell "run-as $PKG true" 2>/dev/null || {
  echo "run-as failed. This needs the DEBUG build: ./install_android.sh --device $DEVICE" >&2; exit 1; }

WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT

# Contacts are the device's, not the app's. Writing them would mean RawContacts + Data inserts and
# a cleanup nobody would run; naming what is missing is more honest than a half-populated address
# book that makes the keypad screenshot look broken for reasons the script hid.
CONTACTS="Ana Torres|Ana Lucia Prado|Andres Molina|Clinica Sur"
echo "==> contacts"
HAVE=$($ADB shell content query --uri content://com.android.contacts/data/phones \
        --projection display_name 2>/dev/null | sed -n 's/.*display_name=\(.*\)$/\1/p' | tr -d '\r')
MISSING=""
IFS='|' read -ra WANT <<< "$CONTACTS"
for c in "${WANT[@]}"; do
  grep -Fxq "$c" <<< "$HAVE" || MISSING="$MISSING  - $c"$'\n'
done
if [ -n "$MISSING" ]; then
  echo "    WARNING: the keypad screenshot expects these contacts and this device has not got them:"
  printf "%s" "$MISSING"
  echo "    Add them in the Contacts app, or the search results will be empty."
else
  echo "    all four present"
fi

echo "==> seeding rules, settings and call log"
$ADB shell am force-stop "$PKG"
sleep 1
$ADB exec-out run-as "$PKG" cat "databases/$DB" > "$WORK/db" 2>/dev/null
[ -s "$WORK/db" ] || { echo "could not read the database — has the app been launched once?" >&2; exit 1; }

python3 - "$WORK/db" <<'PY'
import json, sqlite3, sys, time

db = sqlite3.connect(sys.argv[1])
c = db.cursor()
now = int(time.time() * 1000)
MIN, HOUR, DAY = 60_000, 3_600_000, 86_400_000

for t in ("CallLogEntry", "CallAttempt", "BlockedNumber", "AllowlistedNumber",
          "PatternRule", "CountryRule", "ActionRule", "ScheduleRule"):
    c.execute(f"DELETE FROM {t}")

# --- rules, so the Lists screen has something on it -------------------------------------------
c.executemany("INSERT INTO BlockedNumber(number,label,created_at) VALUES (?,?,?)", [
    ("+34902100200", "Insurance cold calls", now - 12 * DAY),
    ("+34910555111", "Energy switching", now - 9 * DAY),
    ("+34600998877", None, now - 3 * DAY),
])
c.executemany("INSERT INTO AllowlistedNumber(number,label,created_at) VALUES (?,?,?)", [
    ("+34900700111", "Clinica Sur", now - 20 * DAY),
])
c.executemany("INSERT INTO PatternRule(pattern,label,enabled,created_at) VALUES (?,?,?,?)", [
    ("+34902*", "Premium-rate numbers", 1, now - 15 * DAY),
    ("+34803*", "Adult-line prefix", 1, now - 15 * DAY),
])
# Bare digits, no "+": the app stores country codes that way (see Countries.kt) and the display
# string is "Country: %1$s (+%2$s)", so seeding "+212" renders as "(++212)".
c.executemany("INSERT INTO CountryRule(country_code,country_name,enabled,created_at) VALUES (?,?,?,?)", [
    ("212", "Morocco / Western Sahara", 1, now - 8 * DAY),
])
c.executemany(
    "INSERT INTO ScheduleRule(label,start_minute,end_minute,enabled,created_at) VALUES (?,?,?,?,?)",
    [("Nights", 23 * 60, 7 * 60, 1, now - 18 * DAY)],
)

# --- call log ---------------------------------------------------------------------------------
# Relative to now, so Home's today / this week / this month counters read the same on any day.
# Spread deliberately: three today, three more inside the week, three more inside the month.
def reason(kind, **kw):
    return json.dumps({"type": kind, **kw}, separators=(",", ":"))

rows = [
    # number, minutes_ago, action, rule_type, rule_detail, direction
    ("+34902100200", 42,          "BLOCKED", "MANUAL",  reason("manual"), "INCOMING"),
    ("+34902884411", 3 * 60 + 15, "BLOCKED", "PATTERN", reason("pattern", pattern="+34902*"), "INCOMING"),
    ("+212661234567", 6 * 60,     "BLOCKED", "COUNTRY", reason("country", countryCode="212", countryName="Morocco / Western Sahara"), "INCOMING"),
    ("+34900123456", 7 * 60 + 20, "ALLOWED", "CONTACTS", None, "INCOMING"),
    ("+34900700111", 9 * 60,      "ALLOWED", None,      None, "OUTGOING"),

    ("+34910555111", 26 * 60,     "BLOCKED", "MANUAL",  reason("manual"), "INCOMING"),
    ("+34803112233", 2 * 24 * 60, "BLOCKED", "PATTERN", reason("pattern", pattern="+34803*"), "INCOMING"),
    ("+34600445566", 3 * 24 * 60, "BLOCKED", "SPAM",    reason("spam", source="Bundled list", confidencePercent=92), "INCOMING"),
    ("+34900123999", 4 * 24 * 60, "ALLOWED", "CONTACTS", None, "INCOMING"),

    ("+34600998877", 9 * 24 * 60,  "BLOCKED", "MANUAL",   reason("manual"), "INCOMING"),
    ("+34902777888", 14 * 24 * 60, "BLOCKED", "SCHEDULE", reason("quiet_hours"), "INCOMING"),
    ("+34611223344", 19 * 24 * 60, "ALLOWED", "REPEATED_ALLOWED",
     reason("allowed_repeated", attempts=3), "INCOMING"),
]
c.executemany(
    "INSERT INTO CallLogEntry(number,timestamp,action,rule_type,rule_id,rule_detail,recording_path,direction)"
    " VALUES (?,?,?,?,NULL,?,NULL,?)",
    [(n, now - m * MIN, a, rt, rd, d) for n, m, a, rt, rd, d in rows],
)

# --- settings ---------------------------------------------------------------------------------
# The shipped defaults, stated rather than inherited: a screenshot must show the app a new user
# gets, not whatever the last test run left behind. rule_matrix_test.sh in particular leaves
# default_action=BLOCK and the emergency exemption off.
settings = {
    "blocking_enabled": "true",
    "auto_allow_contacts": "true",
    "spam_provider_enabled": "true",
    "notifications_enabled": "true",
    "notify_unknown_callers": "true",
    "default_action": "ALLOW",
    "auto_responder_enabled": "false",
    "auto_recording_enabled": "false",
    "repeated_caller_bypass_count": "3",
    "emergency_callback_exemption": "true",
    "emergency_callback_mode_since": "0",
    "last_emergency_call_at": "0",
    "welcome_shown": "true",
    "permissions_prompt_shown": "true",
}
for k, v in settings.items():
    c.execute("INSERT OR REPLACE INTO AppSettings(key,value) VALUES (?,?)", (k, v))

db.commit()
blocked_today = sum(1 for _, m, a, *_ in rows if a == "BLOCKED" and m < 24 * 60)
print(f"    {len(rows)} call-log rows ({blocked_today} blocked in the last 24h), "
      f"3 blocked numbers, 2 patterns, 1 country rule, 1 schedule, 1 allowlist entry")
db.close()
PY

python3 -c "
import sqlite3,sys
sqlite3.connect(sys.argv[1]).execute('PRAGMA schema_version').fetchone()
" "$WORK/db" || { echo "refusing to push a non-SQLite file" >&2; exit 1; }
$ADB shell "run-as $PKG sh -c 'cat > databases/$DB'" < "$WORK/db"

if [ "$LOCALE_RESET" = true ]; then
  echo "==> handing the language back to the system"
  $ADB shell cmd locale set-app-locales "$PKG" --locales "" >/dev/null 2>&1 || true
elif [ -n "$LOCALE" ]; then
  echo "==> app language: $LOCALE"
  $ADB shell cmd locale set-app-locales "$PKG" --locales "$LOCALE" >/dev/null 2>&1 || {
    echo "    could not set a per-app locale (needs API 33+); set the system language instead" >&2; }
fi
echo "    now: $($ADB shell cmd locale get-app-locales "$PKG" 2>/dev/null | tr -d '\r')"

$ADB shell am start -n "$PKG/org.carlospinan.bloqueador.app.MainActivity" >/dev/null 2>&1
echo
echo "Seeded. Capture into docs/store/ then run ./scripts/play_assets.sh:"
echo "  01_home  02_keypad  03_calllog  04_lists  05_settings   (prefix es_ for Spanish)"
echo "The keypad shot is taken with 'Ana' typed, so the contact search has results in it."
