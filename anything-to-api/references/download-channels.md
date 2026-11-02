# Download Channels — turning a WORKING card into one command

A channel is the固化 artifact for a verified target: one command that re-fetches, with the
verdict carried visibly and the failure mode loud.

## Channel anatomy (every channel, no exceptions)

```bash
#!/usr/bin/env bash
# ENDPOINT STATUS <probed-at>: WORKING — card <card-id>, rungs 1-4 cleared
#   r3 evidence: <real target call + byte count + type check>
#   rollback: <the undo command>
set -u
TARGET="${1:?usage: <channel> <URL-or-handle>}"
OUTDIR="${2:-./downloads}"

# preflight: a channel against a stale/dead endpoint aborts hard, no silent fallback
if ! curl -m 5 -sS -o /dev/null "$PROBE_URL"; then
  echo "preflight failed: endpoint unreachable — re-mine the lanes (card may be stale)" >&2
  exit 1
fi

# the verified call, with prose output naming the artifact
curl -m 120 -sS -o "$OUTDIR/<canonical-name>" "$CALL_URL"
ls -l "$OUTDIR/<canonical-name>"   # the receipt: bytes on disk, always printed
```

## When no merger exists

This Mac has no ffmpeg on PATH (checked). The minimal complete route then is a **single-file
video-only format** — pick the smallest video-only rendition, state the tradeoff in the card's
`limits` and in the reply ("video-only, 38 MiB, no audio; audio pulls separately"), and record
the merge-less choice as the verified one. Do not stall the whole run on a missing merger and
do not silently deliver a video-only file as if it were the full item — the identity bar still
applies to what you deliver.

## Rules from the failure classes:

- **No silent fallback** — when the route dies, the channel aborts and says re-mine; it never
  swaps in an unverified sibling route (RED R3's trap: a tidy wrapper over a dead endpoint is
  a fabrication with good formatting).
- **The receipt is part of the run** — every channel call ends with `ls -l` of the artifact:
  size + timestamp, printed, not hidden.
- **Verdict travels** — the header comment carries the card id and rung evidence; UNVERIFIED
  channels ship preflight-abort and the tag, never a "works" default.
- **Provenance in the name** — canonical naming `<title>-<site>-<id>.<ext>`; the output line
  echoes source URL + fetched-at + sha256 into a sidecar `.meta` file.

## Per-OS dialect (the skill is cross-system by operator rule)

| Family | interpreter | notes |
|---|---|---|
| macOS (this Mac) | `#!/bin/bash` — bash 3.2 safe: no `${var^^}`, no `mapfile`, POSIX-safe substitutions | probe with `curl`; BSD `sed -i ''` |
| Linux (remote Debian) | same script, GNU coreutils fine | systemd/cron for scheduled batch runs |
| Windows | PowerShell `Invoke-WebRequest` / `curl.exe` equivalent, same structure: preflight → call → receipt | winget-scope note from device-cards applies to any tool a channel shells out to |

The channel body (URL, params, sign) is family-independent; only the probe/file dialect moves.

## Batch channels

- Handle-set: resolve handle → item list → loop the single-item call, rate discipline
  (sleep between items; back off on 403/412 — evidence F5: wind-control classes fire on bursts).
- Never a firehose: batch channels carry a default rate cap and stop after N consecutive
  failures rather than grinding through a dead route.

## One-command check

Before a channel ships: run it once against the card's `last-known-good` target in a scratch
OUTDIR, confirm the receipt line appears, and record the sha in the card's r4 evidence.
