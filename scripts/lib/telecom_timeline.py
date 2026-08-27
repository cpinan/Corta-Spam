"""Reads one call's timeline out of a `dumpsys telecom` capture.

Used by blocked_call_test.sh. Telecom's "Historical Events" section is the only honest record of
what happened to a call: it names every state change, marks the app's own requests
(REQUEST_REJECT, REQUEST_DISCONNECT) separately from the platform's, and carries the disconnect
cause. Sampling `dumpsys telecom` in a loop instead — the obvious approach — cannot see a call
that arrives and is rejected between two samples, and each sample costs the better part of a
second.

Usage: telecom_timeline.py <dumpsys-capture> <last-two-digits-of-the-number> [after-call-id]

`after-call-id` guards against reading a call from an earlier run: the history keeps every call the
device has seen, and the suite reuses the same three numbers, so "the last block for this number"
silently answered with a previous run's call when the modem failed to deliver the new one. Telecom
numbers its calls TC@1, TC@2, ...; passing the highest id seen before the call was placed makes a
stale block read as NOCALL, which is the honest answer.

Prints "<seconds since ringing> <EVENT>" per interesting event, then "cause=<CODE>"; or NOCALL if
the history holds no call to that number. The number is matched on its last two digits because
that is all Telecom leaves unredacted ("To address: tel:********41").
"""

import re
import sys

EVENTS_OF_INTEREST = (
    "SET_RINGING",
    "SET_ANSWERED",
    "SET_ACTIVE",
    "REQUEST_REJECT",
    "REQUEST_DISCONNECT",
    "SET_DISCONNECTED",
)


def call_blocks(lines):
    """Every per-call block in the history as (id, lines), in the order Telecom printed them."""
    blocks, current = [], None
    for line in lines:
        header = re.match(r"\s*Call\s*TC@(\d+) \[", line)
        if header:
            current = []
            blocks.append((int(header.group(1)), current))
        elif current is not None:
            # The per-call timings table follows the events and contains no timestamps to confuse
            # the parser, but stopping here keeps a block to just its own call.
            if line.strip().startswith("Timings"):
                current = None
            else:
                current.append(line)
    return blocks


def address_matches(line, suffix):
    """Whether this "To address" line is the number we placed.

    The line reads `To address: tel:********41 Verstat: not Presentation: Allowed`, so the number
    is neither the whole line nor its end — matching on the end of the line found nothing at all
    and reported every call as NOCALL.
    """
    address = re.search(r"To address: tel:([*\d]+)", line)
    return bool(address) and address.group(1).endswith(suffix)


def main():
    capture, suffix = sys.argv[1], sys.argv[2]
    after = int(sys.argv[3]) if len(sys.argv) > 3 else 0
    with open(capture, errors="replace") as handle:
        lines = handle.read().splitlines()

    mine = [
        block
        for call_id, block in call_blocks(lines)
        if call_id > after and any(address_matches(line, suffix) for line in block)
    ]
    if not mine:
        print("NOCALL")
        return

    events, cause = [], "unknown"
    for line in mine[-1]:
        stamp = re.match(r"\s*(\d\d):(\d\d):(\d\d)\.(\d\d\d) - ([A-Z_]+)", line)
        if not stamp:
            continue
        hours, minutes, seconds, millis, name = stamp.groups()
        at = int(hours) * 3600 + int(minutes) * 60 + int(seconds) + int(millis) / 1000
        events.append((at, name))
        if name == "SET_DISCONNECTED":
            code = re.search(r"Code: \(([A-Z_]+)\)", line)
            if code:
                cause = code.group(1)

    if not events:
        print("NOCALL")
        return

    # Everything is reported relative to the moment the call started ringing, because that is when
    # the app first sees it. Absolute clock times would only be readable next to a logcat.
    ringing = next((at for at, name in events if name == "SET_RINGING"), events[0][0])
    for at, name in events:
        if name in EVENTS_OF_INTEREST:
            print(f"{at - ringing:.1f} {name}")
    print(f"cause={cause}")


if __name__ == "__main__":
    main()
