"""Prints the app's own verdict on the last incoming call from a number.

Used by blocked_call_test.sh, as the second opinion next to Telecom's timeline: the timeline says
what happened to the call, this says what the rules decided about it. They can disagree — a call
Telecom shows as REJECTED with no row here was decided by something other than this app.

`direction = 'INCOMING'` is not decoration. An earlier version of the suite answered calls with
KEYCODE_CALL, which redials when there is nothing to answer; the resulting outgoing rows to the
same number were then read back as the incoming verdict, and reported an ALLOWED call that had
never come in.

Usage: last_decision.py <database> <number>
"""

import sqlite3
import sys


def main():
    database, number = sys.argv[1], sys.argv[2]
    row = (
        sqlite3.connect(database)
        .execute(
            "SELECT action, rule_detail FROM CallLogEntry "
            "WHERE number = ? AND direction = 'INCOMING' ORDER BY id DESC LIMIT 1",
            (number,),
        )
        .fetchone()
    )
    print(f"{row[0]} {row[1] or ''}".strip() if row else "no row")


if __name__ == "__main__":
    main()
