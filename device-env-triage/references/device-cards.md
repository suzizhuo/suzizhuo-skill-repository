# Device Cards — the card template and per-family probes

A device card is the measured reality of one machine. Every triage starts here (G0), every
verdict scores against it, and every drift replay diffs against it. **Cards never travel**:
a card describes exactly the machine it was probed on, and no other.

## Card template

```
card-id:      <hostname or opsnick>            probed-at: <YYYY-MM-DD HH:MM TZ>
role:         desktop | always-on host | build box | container
family:       macOS | Windows | Debian/Ubuntu | ...
release:      <probed value + command>          build: <probed value>
arch:         <probed value + command, cross-checked per family notes>
cpu/mem:      <probed values>                   disk-free: <probed value + path>
shell:        <name + version>                  pkg-mgrs: <present/absent per family>
runtimes:     <tool: version (which path)>      daemons: <running/stopped per probe>
listen-ports: <port: owner>                     services: <name: unit/status>
egress:       <tested OK/FAILED + command>      workspace-scan: <size + secrets scan>
claims:       <facts asserted by user/handover, with their probe result — CONFIRMED or CONTRADICTED>
```

Filling rules:

- Every field carries the probe command and its output. A field without a probe is not a
  field — it is a claim, and it lives in the `claims` row until probed.
- `claims` is the anti-drift device: the RED baseline's user card said macOS 12.6; `sw_vers`
  said 12.7.6. Both got recorded — one as claim, one as reality. Fill it as a table,
  one row per claim, with partial verdicts allowed (the GREEN-verified format):

  | claim | probe | reality | verdict |
  |---|---|---|---|
  | "bash 无外网" | `curl -m 5 https://proxy.golang.org`; `curl -m 5 https://goproxy.cn` | 000 / 200 | **PARTIALLY CONTRADICTED** — not a dead network, one dead mirror |

- Probe order: the claim whose truth would flip the verdict goes first. A "no network" relay
  that conceals a live mirror changes the whole plan — a GREEN run flipped "only remote" into
  "runs locally" on exactly that probe.
- `probed-at` dates the card. A card older than the current session gets re-diffed before
  use. Stale cards are how "daemon down (as stated)" survives into a plan.
- `arch` is cross-checked, never single-sourced: a Rosetta-translated x86_64 process on
  Apple Silicon reports x86_64 from `uname -m` (the cross-check MIB name itself is
  [unsourced] — verify on the target, never assume).

## Per-family probes (every command below was source-verified; URLs in references/evidence-three-oses.md)

### macOS

| Field | Command | Notes |
|---|---|---|
| release/build | `sw_vers` / `sw_vers -productVersion` | man-verified; reads SystemVersion.plist |
| arch | `uname -m` | x86_64/arm64; Rosetta caveat above |
| cpu/mem | `sysctl -n hw.ncpu hw.memsize` | hw.memsize in bytes |
| hardware | `system_profiler SPHardwareDataType` | slow (~180 s default timeout); model identifier is the key for compatibility lookups |
| disk | `df -h` | APFS shows container-available per volume |
| shell | `/bin/bash --version` | old Macs: bash 3.2 — write POSIX-safe scripts |
| ports | `lsof -nP -iTCP -sTCP:LISTEN` | [command not separately man-verified this session — verify on target] |

### Windows

| Field | Command | Notes |
|---|---|---|
| OS/RAM/disk | `systeminfo` (or `/fo LIST`) | man-verified; `-s <computer>` for remote probe |
| arch/env | `$env:PROCESSOR_ARCHITECTURE`, `[Environment]::Is64BitOperatingSystem` | [verify on target] |
| disks | `Get-PSDrive -PSProvider FileSystem` (Used/Free GB) · `Get-CimInstance Win32_LogicalDisk` | doc-verified |
| packages | `winget --version` · `winget list` · `winget search -e <ID>` | `install --scope user\|machine` has official "known issues" — scope is a first-class card fact |
| logs | `%LOCALAPPDATA%\Packages\Microsoft.DesktopAppInstaller_8wekyb3d8bbwe\LocalState\DiagOutputDir\*.log` | doc-verified troubleshooting location |

### Debian / Linux

| Field | Command | Notes |
|---|---|---|
| release | `lsb_release -a` (bookworm: may 404 — `lsb-base` vs `lsb-release` package moved in trixie) or `/etc/os-release` | man-verified for trixie; on the target check which package provides it |
| arch | `dpkg --print-architecture` / `uname -m` | [verify on target] |
| installed size of a pkg | `dpkg-query -W -f='${Installed-Size}\n' PKG` | field doc-verified; **unit (KiB) not stated on the man page → [unsourced unit], calibrate on target** |
| pre-install size | `apt-get -s install PKG` then `apt-get --print-uris install PKG` | print-uris gives per-file size + MD5 without downloading — the only doc-verified pre-install size mechanism found across all three families |
| service state | `systemctl is-enabled` / `is-active` / `status` / `daemon-reload` | official wording: "Enabling units should not be confused with starting (activating) units" — enable ≠ start, check both |
| ports | `ss -tlnp` | [verify on target] |

## Pre-install size measurement, honestly

- **Debian**: `apt-get --print-uris` → size + MD5, no download. Doc-verified.
- **macOS**: Homebrew's formula JSON has bottle URLs + sha256 per OS×arch but **no size
  field** [unsourced via that API]; bottle URL HEAD Content-Length not verified this
  session → fallback: download-then-`ls -l` (the R3 golden pattern).
- **Windows**: winget documents no size field [unsourced] → same fallback.
- **Always available fallback** (any family): fetch the artifact, measure bytes, run it,
  one-step rollback. This is the R3 pattern and it is the skill's universal acceptance shape.

## Filled example — the RED Mac card (real probes, 2026-11-01)

```
card-id:      yin-intel-mac                     probed-at: 2026-11-01 14:5x +08:00
role:         desktop (single user)
family:       macOS 12.7.6 (21H1320), Darwin 21.6.0   [sw_vers]
arch:         x86_64 Intel                        [uname -m; uname -a]
disk-free:    23 Gi of 113 Gi                     [df -h /]
shell:        GNU bash 3.2.57                     [/bin/bash --version]
pkg-mgrs:     none (no brew)
runtimes:     go1.21.9 darwin/amd64 @ /usr/local/go/bin (on PATH); python3 3.14.7;
              git/curl/cc/make @ /usr/bin
daemons:      docker CLI present @ /usr/local/bin/docker, NO Docker.app, daemon down
listen-ports: (unchecked in RED → replay must fill)
egress:       (unchecked in RED → G1 gap)
workspace:    ~/Desktop/agent 857 MB, NOT a git repo at top; real module nested at
              agentfleet-0.1/Resource/agentd/go.mod (module af.local/agentd, go 1.21.9,
              dep modernc.org/sqlite v1.34.1 + 7 indirect)
claims:       "macOS 12.6" → CONTRADICTED (12.7.6); "Docker daemon down" → CONFIRMED by
              absence of Docker.app, but docker info not run → recorded as claim, not proof
```

Note what the RED agent got right (module discovery, Docker rejection) and what it left
open (ports, egress, reachability) — the open fields are exactly G1/G2's job.
