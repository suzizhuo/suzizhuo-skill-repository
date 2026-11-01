# Drift Replay — "it worked last week"

The second half of the skill: a setup that used to run and now doesn't is not a new
project, it is a **diverged** one. Do not re-triage from scratch; replay against the
record. Failure class F1 (environment drift) is the most common cause and the least
visible one — nobody knows the environment changed until it breaks.

## The replay loop

1. **Freeze the symptom** — exact command, exact error, which component failed. No fixes yet.
2. **Card diff** — re-probe the device card (`sw_vers`/`uname -m`/`df`/`docker info` … per
   family) and diff against the stored card. OS point-releases, disk-full, a daemon that
   stopped, a tool that updated itself: all show up here. The RED baseline caught exactly
   this class once (card said 12.6, `sw_vers` said 12.7.6).
3. **Lock diff** — diff the project's pinned state: `go.sum` / `mise.lock` / `package-lock.json`
   / image digests. A floating transitive dependency moved under a pinned direct one
   (evidence F2: LiteLLM pulled a breaking transitive dep through `dspy`).
4. **Ledger diff** — the env ledger's verdicts vs today: what was installed since, by whom,
   with which smoke result.
5. **Change-scan** — what changed on the device since the last green run: OS updates,
   self-updating tools, ports taken by another service (F9 collisions), removed paths.
6. **Restore or re-lock** — smallest action that re-establishes the last green state:
   re-lock to the recorded checksum, restore the backup binary, stop the port squatter.
   Re-triage (full loop) only if the requirement itself changed.

## Rules that make replay work

- **The ledger must have existed before the break.** "上周还能跑" is only replayable if last
  week's run wrote a row. This is why every INSTALL appends one.
- **Test the artifact that ran.** Evidence C2 (Anthropic postmortem): a verification that
  exercises anything other than the exact shipped artifact can be perfect and still wrong.
  Replay verifies with the same binary/config that was green before.
- **Drift is detected on a schedule, not on failure.** Baseline + periodic re-diff + alert
  on divergence is the only pattern the evidence supports (F1-B); a one-off `diff` at
  breakage time tells you what broke, not when.
- **Containerizing moves the snowflake, it does not remove it** (C1). A container fixes
  drift only when the image is built from a locked, diffable spec. "Works in my container"
  re-opens the same question at the image layer.

## Replay report shape

```
symptom: <command + error, verbatim>
card diff: <changed facts, probe outputs>
lock diff: <moved pins, checksums>
ledger diff: <verdicts since last green>
change-scan: <OS/tool/port changes>
action: <smallest restore, or re-lock, with the command>
verify: <the smoke command, exit code>
```

## Silent toolchain drift — the resolver that installs behind your back

Go's `GOTOOLCHAIN=auto` (the default) turns a `go` directive that outruns the installed
toolchain into a silent download: the build "succeeds" and a 225 MiB toolchain lands in the
module cache with no gate, verdict, or rollback. Mechanism facts (GREEN G4 + regression R1,
both measured):

- The **`go` directive gates the build** (`go 1.22.0` on go1.21.9 → `GOTOOLCHAIN=local` fails
  cleanly with `go.mod requires go >= 1.22.0`); a bare `toolchain go1.22.0` line does **not**
  gate — it is only a suggestion, and go1.21.9 compiles fine.
- `GOTOOLCHAIN=auto` resolves the gap by downloading — success is the symptom, not the cure.
- Replay move: freeze the resolver (`GOTOOLCHAIN=local`) to expose the real symptom, then the
  minimal fix is a one-line directive edit (0 bytes), not the 225 MiB install. If the newer
  toolchain is genuinely required, it enters the ledger as a heavy install with full verdict
  and a rollback row — it does not arrive silently.
- **The trigger surface is wider than "build"** (regression R1, measured): in a directory
  whose go.mod directive outruns the local toolchain, even `go version` downloads and reports
  the *new* version — "just checking the version" is itself an install. `go env` does **not**
  trigger. Freeze rule: prefix `GOTOOLCHAIN=local` on **any** go subcommand that reads go.mod
  in a project directory, before the first one runs.
- Regression R1's measured cache state: three toolchains already resident (go1.22.0 =
  234,770,432 B, go1.23.0 = 249,679,872 B, go1.26.10 = 249,413,632 B) — 733,863,936 B that
  entered through this bypass across sessions. Freeze-first prevents growing this pile.

## Common replay traps

- Re-installing before diffing — reinstalling a floating version reproduces the break.
- Fixing the symptom on one device and calling it global (cards never travel; the sibling
  machine keeps its own card).
- Rebuilding "from memory" instead of from the ledger/backup (F4: no rehearsed reverse).
- Assuming the project changed — most "it broke" events are the *device* changing.
- Manufacturing a failure to match the narrative — a reported break that does not reproduce
  is reported as non-reproducible with the probe outputs attached, never re-created on the
  real project to make the story true. The symmetric trap also holds: a project-side edit
  (a source file changed yesterday) is not env drift — list both lines, merge neither.
