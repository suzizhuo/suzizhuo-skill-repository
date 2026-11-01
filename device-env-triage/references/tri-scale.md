# The Tri-Scale — scoring and verdict tree

Score **one component on one device**, three axes, then verdict. This file is the scoring
reference for `SKILL.md`; the gates G0–G6 run first and a component only reaches scoring
with a filled device card.

## The three axes

### 1. Footprint — measured, never recalled

Record two numbers, not one:

| Number | Meaning | How obtained |
|---|---|---|
| download size | bytes the fetch moves | real download, then `ls -l` (or `du -h`) |
| installed size | bytes it occupies on disk after install | install on a scratch path, or the package manager's size query per OS family (references/device-cards.md) |

The RED baseline got this right (R3: downloaded yt-dlp, read 3,073,072 bytes, ran
`--version`, exit 0) and got the two-number distinction right in planning (R2: Go tarball
~67 MB download vs ~350 MB extracted). An estimate is labelled `unmeasured` and blocks the
verdict — UNMEASURED is a verdict, and it means "stop and measure", not "guess and proceed".

Default bands (operator-tunable):

| Band | Size | Typical shape |
|---|---|---|
| tiny | ≤ 50 MB | single-file CLI, no daemon |
| small | ≤ 200 MB | toolchain runtime, small SDK |
| heavy | > 200 MB | daemons, Desktop apps, full stacks |
| xlarge | > 1 GB | container engines, IDEs, kitchen-sink images |

### 2. Frequency

every-session · weekly · per-project · one-off. A component used once a quarter has no
frequency claim, whatever its utility.

### 3. Utility — the operator's four gains

efficiency · maintainability · creativity · version control. State which gain, concretely
("single-file rollback beats pip site-packages pollution" is a maintainability gain;
"checksummed lockfile so the next machine is byte-identical" is a version-control gain).
No named gain → utility is zero, whatever the README promises.

## The verdict tree

```
footprint unmeasured?
  └─ yes → UNMEASURED. Measure (download + installed). Do not proceed.
  └─ no ↓
equivalent light implementation exists?
  └─ yes → LIGHT-SWAP (install the light one; state the equivalence boundary)
  └─ no ↓
heavy or xlarge, OR always-on daemon role?
  └─ no → footprint ≤ small AND (frequent OR high utility)?
  │     └─ yes → INSTALL
  │     └─ no  → REFUSE (with data + route)
  └─ yes → device role fits the component (always-on host for a 7x24 service)?
        └─ yes → INSTALL (full G5/G6 discipline: smoke, rollback, dialect)
        └─ no  → OFFLOAD (route to the always-on host; 0 bytes local)
```

## Verdict shapes — what each answer must contain

**INSTALL** — measured bytes (download + installed) · the named utility gain · one smoke
command with expected exit/output · a one-step rollback · the collision it avoids
(ports/uid/paths). Template row for the ledger:

```
date | <card id> | yt-dlp 2026.10.26 | 3,073,072 B dl / 3.0 MiB installed | INSTALL | tiny+frequent, single-file rollback | rm ~/bin/yt-dlp | yt-dlp --version → exit 0
```

**LIGHT-SWAP** — the heavy component · the light replacement · the equivalence boundary
(what the light one cannot do) · when the boundary would be crossed.

**OFFLOAD** — the heavy component · the device-role mismatch (desktop ≠ 7x24 host) · the
target host's card id · how the artifact gets there (cross-compile = 0 bytes on target) ·
the target-side service unit (systemd, not launchd) · rollback on the target.

**REFUSE** — see references/refusal-protocol.md. Never a bare no.

**UNMEASURED** — the measurement command to run, and nothing else. This verdict expires the
moment the bytes are known.

## Worked verdicts from the RED baseline

| Component on device | RED behavior | Verdict under this skill | Why |
|---|---|---|---|
| Go 1.21.9 on RED Mac | found pre-installed via `which go; go version` | INSTALL (already satisfied — evidence recorded) | 0 MB + matches `go 1.21.9` directive; the evidence is the probe output |
| Docker Desktop (~700 MB) on RED Mac | REJECTED — daemon down, no Docker.app, no brew | REFUSE or OFFLOAD | heavy + never-running role + no utility gain on a desktop; route: remote Debian for anything containerised |
| Go toolchain on remote Debian | either +67 MB on target or cross-compile | OFFLOAD-flavoured INSTALL | target is the always-on host → building there is legitimate; cross-compile from the Mac is better (0 bytes) |
| yt-dlp on RED Mac | downloaded, measured, ran, one-file rollback | INSTALL — the golden shape | tiny + frequent + named gains (efficiency, maintainability) + smoke + rollback |

## Anti-patterns at this layer

- Scoring "the environment" as one blob instead of per component.
- Recording only download size (installed size surprised people at 350 MB vs 67 MB).
- Frequency asserted without a use pattern ("maybe someday" is one-off, not weekly).
- Utility quoted from the README instead of named as a gain for this operator's work.
- Bands treated as law instead of operator-tunable defaults — an operator who says 500 MB
  is fine for one toolchain has edited the band, and the edit is recorded, not argued.

## Install bypass — the component that skips the gates

A component can arrive on the device **without ever being scored**, because a resolver fetched
it mid-command. The canonical case (observed in a GREEN run): Go's `GOTOOLCHAIN=auto` — the
default setting — sees a `go 1.22.0` directive outrunning the installed go1.21.9 and silently
downloads a matching toolchain (measured: 225 MiB into the module cache), so the build
"succeeds". No gate, no verdict, no rollback step.

Rule: **a resolver that can install is part of the triage.** Before scoring any Go project:
`go env GOTOOLCHAIN` — if `auto`, freeze it (`GOTOOLCHAIN=local`) and let the build fail
cleanly first. Then score the directive itself: fix the directive (0 bytes) or INSTALL the
toolchain (heavy, full verdict). Same shape applies to any auto-updater, `npx`-style
run-and-fetch, or package manager that resolves versions behind the plan: freeze it, then score.

## Cheapness attribution

"Cheap" is a claim, and the claim includes its attribution. Record what makes the plan cheap —
a warm module cache (measured `du -sh`), a cross-compile that leaves the target at 0 bytes, a
pre-installed toolchain that matches the directive — and never "the target probably has it".
The RED baselines failed both halves of this: R1 never probed reachability, R2 inherited the
Mac's toolchain facts onto an unprobed cloud host. GREEN passed by attributing every "cheap"
to a measured fact.
