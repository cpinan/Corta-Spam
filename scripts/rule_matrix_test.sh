#!/usr/bin/env bash
# rule_matrix_test.sh — drive a real call through every branch of the rule engine.
#
# The unit tests cover RulePrecedenceResolver directly and they are thorough. They are also the
# reason six features once shipped, passed their tests and never executed: the resolver was right
# and nothing reached it. This suite asserts the other thing — that a call arriving from Telecom
# lands on the branch it should, through PassthroughInCallService, the repositories, SQLite and
# back out as a call-log row.
#
# Every scenario places a real (emulator-simulated) call and asserts the resulting CallLogEntry's
# action and rule_type. It seeds its own rules and settings, so it does not care what was on the
# device beforehand — but it DOES overwrite them. See "device state" below.
#
# Usage:
#   ./scripts/rule_matrix_test.sh                 # all phases
#   ./scripts/rule_matrix_test.sh --list          # print the matrix without running it
#   ./scripts/rule_matrix_test.sh --device emulator-5554
#
# Requires: an emulator (adb emu gsm call), a DEBUG build (run-as reads the database), and the
# dialer role. It checks all three before touching anything.
#
# Device state: rules, settings and the call log are REPLACED. Contacts are read but never
# written — the contact scenarios are skipped with a warning when the address book lacks them,
# rather than silently passing.
set -euo pipefail
cd "$(dirname "$0")/.."

PKG=org.carlospinan.cortaspam
DB=cortaspam.db
DEVICE=""
LIST_ONLY=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) DEVICE="$2"; shift 2 ;;
    --list) LIST_ONLY=1; shift ;;
    --help|-h) sed -n '2,23p' "$0"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
PASSED=0; FAILED=0; SKIPPED=0

# --- the matrix ------------------------------------------------------------------------------
# phase | number | expected action | expected rule_type | description
# rule_type "-" means NULL (the default-action paths write no tag).
MATRIX=$(cat <<'EOF'
A|+34900111222|BLOCKED|MANUAL|a manually blocked number
A|+34901555666|BLOCKED|PATTERN|a number matching the pattern +34901*
A|+212612345678|BLOCKED|COUNTRY|a country rule for +212
A|+2348001234567|BLOCKED|SPAM|a prefix on the bundled spam list
A|+34700111222|ALLOWED|CONTACTS|a manually allowlisted number
A|+34999888777|ALLOWED|-|no rule matches and the default action is ALLOW
A|+34902222333|BLOCKED|MANUAL|a manual block outranks the allowlist entry for the same number
B|+34999888777|BLOCKED|-|no rule matches and the default action is BLOCK
B|+34700111222|ALLOWED|CONTACTS|the allowlist still wins under a BLOCK default
C|+34999888777|BLOCKED|SCHEDULE|quiet hours are active
C|+34700111222|ALLOWED|CONTACTS|the allowlist bypasses quiet hours
EOF
)

if [ "$LIST_ONLY" = 1 ]; then
  echo "phase | number | action | rule_type | scenario"
  echo "$MATRIX" | awk -F'|' '{printf "  %-5s %-16s %-8s %-10s %s\n", $1, $2, $3, $4, $5}'
  echo
  echo "  plus: 3 calls to trip an ACTION rule, a contact saved in national format called"
  echo "        in E.164 (phase E), a contact saved in E.164 called nationally (phase F),"
  echo "        and a repeated-caller bypass — each needs more than one call or the address book."
  exit 0
fi

if [ -z "$DEVICE" ]; then
  DEVICE=$(adb devices | awk 'NR>1 && $2=="device" && $1 ~ /^emulator-/ {print $1; exit}')
  [ -z "$DEVICE" ] && { echo "No emulator attached. This suite needs 'adb emu gsm call'." >&2; exit 1; }
fi
case "$DEVICE" in
  emulator-*) ;;
  *) echo "$DEVICE is not an emulator; a physical phone has no fake modem to call from." >&2; exit 2 ;;
esac
ADB="adb -s $DEVICE"

$ADB shell pm list packages 2>/dev/null | grep -q "$PKG" || { echo "$PKG not installed on $DEVICE." >&2; exit 1; }
# run-as is the whole seeding mechanism, and it fails on a release build with "not debuggable".
$ADB exec-out run-as "$PKG" ls databases >/dev/null 2>&1 || {
  echo -e "${RED}run-as failed.${NC} This needs the DEBUG build: ./install_android.sh --device $DEVICE" >&2; exit 1; }
$ADB shell dumpsys package "$PKG" | grep -q "GRANTED_BY_ROLE" || {
  echo -e "${RED}Dialer role not held${NC} — no call will reach the app." >&2
  echo "Fix: $ADB shell cmd role add-role-holder android.app.role.DIALER $PKG" >&2; exit 1; }

echo "==> device $DEVICE, debug build, dialer role held"

WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT

# Refuses anything that is not a real SQLite file. The first version of this script had no such
# check and destroyed the device database: its push used `exec-out`, which does not forward stdin,
# so `cat` saw EOF and wrote a zero-byte file over it. A later pull then read that empty file back
# and happily pushed it again. Validating both directions makes that unrecoverable sequence
# impossible — an empty or corrupt file now stops the run instead of propagating.
valid_sqlite() {
  [ -s "$1" ] || return 1
  [ "$(head -c 15 "$1" 2>/dev/null)" = "SQLite format 3" ]
}

pull_db() {
  $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$WORK/db" 2>/dev/null
  valid_sqlite "$WORK/db" || {
    echo -e "${RED}Pulled database is empty or not SQLite.${NC} Refusing to continue." >&2
    echo "Recover with: $ADB shell run-as $PKG rm -f databases/$DB databases/$DB-journal" >&2
    echo "then relaunch the app, which recreates the schema." >&2
    return 1
  }
}

# `exec-out` is for reading binary stdout and does NOT forward stdin. `adb shell` does, so the
# write direction has to use it — and the result is read back before the run is allowed to go on.
push_db() {
  valid_sqlite "$WORK/db" || { echo "refusing to push a non-SQLite file" >&2; return 1; }
  $ADB shell "run-as $PKG sh -c 'cat > databases/$DB'" < "$WORK/db"
  $ADB exec-out run-as "$PKG" cat "databases/$DB" > "$WORK/verify" 2>/dev/null
  valid_sqlite "$WORK/verify" || { echo -e "${RED}Push left an invalid database on the device.${NC}" >&2; return 1; }
}

# Seeds rules + settings for a phase, then restarts the app. The restart is not optional:
# SqlSettingsRepository hydrates once in init and AndroidContactsGateway caches for five minutes,
# so a running process would keep answering from the state this function just replaced.
seed() {
  local phase="$1"
  # Stop first, then read: a live process holds the SQLite connection and would race the write.
  $ADB shell am force-stop "$PKG"
  sleep 1
  pull_db || { echo "could not pull the database" >&2; exit 1; }
  python3 - "$WORK/db" "$phase" <<'PY'
import sqlite3, sys
db = sqlite3.connect(sys.argv[1]); phase = sys.argv[2]
c = db.cursor()
for t in ("CallLogEntry", "CallAttempt", "BlockedNumber", "AllowlistedNumber",
          "PatternRule", "CountryRule", "ActionRule", "ScheduleRule"):
    c.execute(f"DELETE FROM {t}")

now = 1  # created_at is only used for ordering here
c.execute("INSERT INTO BlockedNumber(number,label,created_at) VALUES ('+34900111222','manual',?)", (now,))
# Same number in both lists: precedence says the manual block wins.
c.execute("INSERT INTO BlockedNumber(number,label,created_at) VALUES ('+34902222333','manual-wins',?)", (now,))
c.execute("INSERT INTO AllowlistedNumber(number,label,created_at) VALUES ('+34902222333','should-lose',?)", (now,))
c.execute("INSERT INTO AllowlistedNumber(number,label,created_at) VALUES ('+34700111222','allowed',?)", (now,))
c.execute("INSERT INTO PatternRule(pattern,label,enabled,created_at) VALUES ('+34901*','pattern',1,?)", (now,))
c.execute("INSERT INTO CountryRule(country_code,country_name,enabled,created_at) VALUES ('212','Morocco',1,?)", (now,))

settings = {
    "blocking_enabled": "true",
    "auto_allow_contacts": "true",
    "spam_provider_enabled": "true",
    "welcome_shown": "true",
    "permissions_prompt_shown": "true",
    "auto_responder_enabled": "false",
    "repeated_caller_bypass_count": "0",
    "default_action": "ALLOW" if phase in ("A", "C") else "BLOCK",
    # Off for the whole suite, and this is load-bearing. The emergency-callback exemption
    # short-circuits EVERY rule by design, so a device that reports
    # Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE makes all fourteen scenarios return
    # ALLOWED with no rule -- which reads exactly like a dead rule engine. An emulator that has
    # ever dialled 112 can report it indefinitely: on 2026-08-20 this cost an hour, on a build
    # whose engine was fine (14/14 the moment this was set). The exemption has its own unit
    # tests; this suite is about rule precedence, so it pins the variable rather than inheriting
    # whatever telephony state the device is in.
    "emergency_callback_exemption": "false",
}
if phase == "C":
    # A window that certainly contains "now": the whole day.
    c.execute("INSERT INTO ScheduleRule(label,start_minute,end_minute,enabled,created_at)"
              " VALUES ('all-day',0,1439,1,?)", (now,))
if phase == "D":
    c.execute("INSERT INTO ActionRule(label,attempts,window_minutes,pattern_id,enabled,created_at)"
              " VALUES ('insistent',3,5,NULL,1,?)", (now,))
for k, v in settings.items():
    c.execute("INSERT OR REPLACE INTO AppSettings(key,value) VALUES (?,?)", (k, v))
db.commit(); db.close()
PY
  push_db
  $ADB shell am start -n "$PKG/org.carlospinan.bloqueador.app.MainActivity" >/dev/null 2>&1
  # Wait for the process to actually exist rather than sleeping a guessed interval. A fixed 4s was
  # enough on a warm app and not enough on the first launch after an install, where dexopt and Koin
  # init push startup out — the first call then arrived before PassthroughInCallService was bound
  # and produced no call-log row at all, failing a scenario that was not broken.
  local i pid
  for i in $(seq 1 30); do
    # `|| true` is load-bearing: this script runs with `pipefail`, so the pipeline takes pidof's
    # exit code rather than tr's, and a not-yet-running process makes the assignment itself fail
    # under `set -e` — aborting the run before the first scenario with no output at all.
    pid=$($ADB shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)
    # `[ -n "$pid" ] && break` would abort the whole run under `set -e` on the first iteration,
    # because a not-yet-started process makes the test — and therefore the statement — non-zero.
    if [ -n "$pid" ]; then break; fi
    sleep 1
  done
  # Written as an if, not `[ -z "$pid" ] && { ... }`: under `set -e` that form returns non-zero on
  # the *success* path and kills the run before the first scenario, with no output explaining why.
  if [ -z "$pid" ]; then
    echo -e "${RED}App did not start after seeding.${NC}" >&2
    return 1
  fi
  sleep 3   # Telecom binds the InCallService a moment after the process is up.
}

# The highest CallLogEntry id currently on the device, or 0. Used as the baseline a call's own
# row must beat, so a scenario can never be judged on the previous scenario's row.
max_log_id() {
  pull_db 2>/dev/null || { echo "-1"; return; }
  python3 - "$WORK/db" <<'PY'
import sqlite3, sys
try:
    r = sqlite3.connect(sys.argv[1]).execute("SELECT MAX(id) FROM CallLogEntry").fetchone()
    print(r[0] if r and r[0] is not None else 0)
except Exception:
    print(0)
PY
}

# Places one call and returns "ACTION|RULE_TYPE" for the row THAT call produced.
#
# Polls for a row newer than the pre-call baseline instead of sleeping a fixed interval and
# reading `ORDER BY id DESC LIMIT 1`. That older form read whatever row happened to be newest,
# which is only the right row when the write beat the sleep. On 2026-08-13 it did not: the
# bundled-spam scenario reported the country scenario's row, and the allowlist scenario then
# reported the spam row arriving late. Two scenarios "failed" that had in fact behaved correctly.
#
# The same defect can pass a scenario for the wrong reason -- any run where consecutive
# expectations coincide reads a stale row and calls it a match -- which is why this is a
# correctness fix and not a flake workaround.
place_call() {
  local number="$1"
  local baseline; baseline=$(max_log_id)
  $ADB emu gsm cancel "$number" >/dev/null 2>&1 || true
  $ADB emu gsm call "$number" >/dev/null
  sleep 5
  $ADB emu gsm cancel "$number" >/dev/null 2>&1 || true

  local i got
  for i in $(seq 1 15); do
    sleep 1
    pull_db 2>/dev/null || continue
    got=$(python3 - "$WORK/db" "$baseline" <<'PY'
import sqlite3, sys
db, baseline = sys.argv[1], int(sys.argv[2])
try:
    r = sqlite3.connect(db).execute(
        "SELECT action, rule_type, rule_detail FROM CallLogEntry WHERE id > ? ORDER BY id DESC LIMIT 1",
        (baseline,)).fetchone()
except Exception:
    r = None
# rule_detail rides along because rule_type alone cannot explain every outcome: an
# emergency-callback exemption logs ALLOWED with a NULL rule_type and puts its reason here, so
# without it a failure reads "expected BLOCKED/MANUAL, got ALLOWED/-" while the row it was read
# from said {"type":"emergency_callback"}.
print(f"{r[0]}|{r[1] or '-'}|{(r[2] or '-')}" if r else "")
PY
)
    [ -n "$got" ] && { echo "$got"; return; }
  done
  # No new row in 15s. Reported as its own outcome rather than silently inheriting an old row:
  # "this call was never logged" is a real finding and must not look like a wrong rule_type.
  echo "NO_NEW_ROW|-|-"
}

# place_call returns "ACTION|RULE|DETAIL". The detail was added so a failure can explain itself;
# these two keep every comparison reading the first two fields, which is what a scenario asserts.
# Split with `read` rather than trimmed with `${got%|*}` -- rule_detail is JSON and is not
# guaranteed free of the delimiter.
outcome_of() { local a r d; IFS='|' read -r a r d <<< "$1"; printf '%s|%s' "$a" "$r"; }
detail_of() { local a r d; IFS='|' read -r a r d <<< "$1"; printf '%s' "$d"; }

# Prints the row's own reason, and names the one that silently invalidates the whole suite.
explain_detail() {
  local d="$1"
  # `case` rather than `[ -z ] || [ = - ] && return`: that reads as (A || B) && C in shell, which
  # is not what it looks like and is one edit away from being wrong.
  case "$d" in "" | "-") return 0 ;; esac
  printf "         reason recorded on the row: %s\n" "$d"
  if [[ "$d" == *emergency_callback* ]]; then
    printf "         ${YELLOW}This device is reporting emergency callback mode.${NC} Every rule is\n"
    printf "         short-circuited while it does, so every scenario here will read as a dead\n"
    printf "         rule engine. An emulator that has dialled an emergency number can report it\n"
    printf "         indefinitely — wipe it, or use another one.\n"
  fi
}

check() {   # number, expected_action, expected_rule, description
  local number="$1" want_a="$2" want_r="$3" desc="$4"
  local got; got=$(place_call "$number")
  local got_a got_r got_d
  IFS='|' read -r got_a got_r got_d <<< "$got"
  if [ "$got_a" = "$want_a" ] && [ "$got_r" = "$want_r" ]; then
    printf "    ${GREEN}PASS${NC} %-38s %s/%s\n" "$desc" "$got_a" "$got_r"
    PASSED=$((PASSED + 1))
  else
    printf "    ${RED}FAIL${NC} %-38s expected %s/%s, got %s/%s\n" "$desc" "$want_a" "$want_r" "$got_a" "$got_r"
    explain_detail "$got_d"
    FAILED=$((FAILED + 1))
  fi
}

run_phase() {
  local phase="$1" title="$2"
  echo
  echo "=== phase $phase — $title ==="
  seed "$phase"
  echo "$MATRIX" | awk -F'|' -v p="$phase" '$1==p {print $2"|"$3"|"$4"|"$5}' | while IFS='|' read -r n a r d; do
    check "$n" "$a" "$r" "$d"
  done
}

# A subshell in the while-loop above would lose the counters, so phases that need running totals
# are counted from the printed output instead. Keep the summary honest by recounting at the end.
OUT=$(mktemp); trap 'rm -rf "$WORK" "$OUT"' EXIT

{
  run_phase A "default ALLOW — block rules, allowlist, precedence"
  run_phase B "default BLOCK — the default path and the allowlist under it"
  run_phase C "quiet hours active"

  echo
  echo "=== phase D — action rule: 3 attempts in 5 minutes ==="
  seed D
  N="+34903111000"
  for i in 1 2 3; do
    got=$(place_call "$N")
    if [ "$i" -lt 3 ]; then
      printf "    attempt %s: %s\n" "$i" "$(outcome_of "$got")"
    else
      if [ "$(outcome_of "$got")" = "BLOCKED|ACTION" ]; then
        printf "    ${GREEN}PASS${NC} %-38s %s\n" "the 3rd attempt trips the action rule" "$(outcome_of "$got")"
      else
        printf "    ${RED}FAIL${NC} %-38s expected BLOCKED/ACTION, got %s\n" "the 3rd attempt trips the action rule" "$(outcome_of "$got")"
        explain_detail "$(detail_of "$got")"
      fi
    fi
  done

  echo
  echo "=== phase E — a contact saved the way it is dialled ==="
  echo "    (regression for the 2026-08-11 fix: national-format contact vs an E.164 call)"
  seed B   # default BLOCK, so only the contacts path can let this through
  # Capture the WHOLE data1 value, then strip formatting. The old pattern was
  # 's/.*data1=\([0-9+][0-9]*\).*/\1/p', whose digit class stops at the first space: the emulator
  # contact "611 99 88 77" came out as "611", so this dialled +34611. That is 5 digits, and
  # PhoneNumberParser requires code.length + 4 before it will read "34" as a country code -- so
  # the engine correctly declined to match and the scenario reported a contact-matching
  # regression that did not exist. The test data was the bug.
  # The trailing `|| true` is load-bearing under `set -e -o pipefail`: `grep -v '^+'` exits 1 when
  # it filters *everything* out, which is exactly the case this line is trying to detect -- an
  # address book whose contacts are all international. Without it the pipeline's failure killed the
  # whole script here, so the SKIP branch below could never run, phase E printed neither a result
  # nor a reason, and the run ended on a bare `exit 1` with the summary line never reached. Another
  # guard that had never once executed.
  NAT=$($ADB shell content query --uri content://com.android.contacts/data/phones --projection data1 2>/dev/null \
        | sed -n 's/.*data1=//p' | tr -d '\r' | grep -v '^+' | head -1 | tr -cd '0-9' || true)
  if [ -n "$NAT" ]; then
    got=$(place_call "+34${NAT}")
    if [ "$(outcome_of "$got")" = "ALLOWED|CONTACTS" ]; then
      printf "    ${GREEN}PASS${NC} %-38s %s\n" "contact '$NAT' matched +34$NAT" "$(outcome_of "$got")"
    else
      printf "    ${RED}FAIL${NC} %-38s expected ALLOWED/CONTACTS, got %s\n" "contact '$NAT' vs +34$NAT" "$(outcome_of "$got")"
      explain_detail "$(detail_of "$got")"
    fi
  else
    printf "    ${YELLOW}SKIP${NC} no national-format contact in the address book\n"
    echo "    Seed one, e.g.:"
    echo "      $ADB shell content insert --uri content://com.android.contacts/raw_contacts --bind account_name:s:'' --bind account_type:s:''"
    echo "    then a phone_v2 data row with a number that has no leading '+'."
  fi

  echo
  echo "=== phase F — a contact saved internationally, called from a domestic line ==="
  echo "    (regression for the 2026-08-14 fix: the gateway digit-stripped the '+')"
  # The mirror of phase E, and the half that was still broken for three days after E was fixed.
  # PhoneNumberParser.sameNumber decides whether the national form may bridge two numbers by
  # reading the leading '+', so AndroidContactsGateway handing it digit-normalised numbers made a
  # contact saved '+34900123456' state no country -- and a call delivered as '900123456', which is
  # ordinary for a domestic call, stopped matching it. Under default BLOCK the contacts path is the
  # only thing that can let this through, so a regression here shows up as BLOCKED, not as a
  # cosmetic miss.
  seed B
  INTL=$($ADB shell content query --uri content://com.android.contacts/data/phones --projection data1 2>/dev/null \
         | sed -n 's/.*data1=//p' | tr -d '\r' | tr -d ' ' | grep '^+34' | head -1 || true)
  if [ -n "$INTL" ]; then
    NSN=${INTL#+34}
    got=$(place_call "$NSN")
    if [ "$(outcome_of "$got")" = "ALLOWED|CONTACTS" ]; then
      printf "    ${GREEN}PASS${NC} %-38s %s\n" "contact '$INTL' matched $NSN" "$(outcome_of "$got")"
    else
      printf "    ${RED}FAIL${NC} %-38s expected ALLOWED/CONTACTS, got %s\n" "contact '$INTL' vs $NSN" "$(outcome_of "$got")"
      explain_detail "$(detail_of "$got")"
    fi
  else
    printf "    ${YELLOW}SKIP${NC} no +34 contact in the address book\n"
  fi
} | tee "$OUT"

echo
P=$(grep -c 'PASS' "$OUT" || true)
F=$(grep -c 'FAIL' "$OUT" || true)
S=$(grep -c 'SKIP' "$OUT" || true)
echo "==> $P passed, $F failed, $S skipped"
if [ "$F" -gt 0 ]; then
  echo -e "${RED}The rule engine did not behave as specified on a real call path.${NC}"
  exit 1
fi
if [ "$S" -gt 0 ]; then
  echo -e "${YELLOW}Some scenarios were skipped — that is not a pass.${NC}"
fi
echo -e "${GREEN}Every covered branch behaved as specified end to end.${NC}"
echo "Device rules, settings and call log were replaced by this run."
