---
name: device-env-triage
description: Use when taking on a project, SDK, or system component on an unfamiliar or constrained device — Mac, Windows, Linux — and deciding what to install, refuse, swap, or offload; when a setup that used to work stops working; or when about to promise an unverified deploy plan (配环境 / 部署环境 / 能不能装 / 多大值得装吗 / 跑不通 / 环境太大 / 装不动 / 环境漂移 / works on my machine).
---

# Device Env Triage

## Overview

Environment decisions are economics, measured on a device card — not vibes, not "the user said it's fine". One loop runs per component a project needs: **CARD → GATES → VERDICT → PLAN → LEDGER**. A component enters a device only when the loop says INSTALL; heavy things that don't fit the device's role get swapped, offloaded, or refused with data. The unit of decision is one component on one device — never "the environment" as a blob.

## When to Use

- New project lands on a device: 跑起来 / 部署 / 配环境 / "需要什么装什么"
- A tool or SDK is proposed (多大 / 值得装吗), or an AI agent is about to install, pull, or daemonize anything
- A setup that used to work stops working after an update, device change, or time (上周还能跑 / works on my machine)
- Not for: pure code questions, or infra choices already made by the operator

## The Loop

### 0. CARD — fill the device card before anything else (references/device-cards.md)

Probe, never trust: user cards and handover sheets are falsifiable inputs, not ground truth. Run the reality diff first — `sw_vers` against the card, `docker info` against "daemon is down". A RED agent accepted "daemon down (as stated)" verbatim and never probed. Cards carry a probe date; stale cards re-diff.

**Cards never travel.** Machine A's facts license nothing about Machine B ("the Mac being Intel says nothing about the cloud host" — a RED agent wrote that, then built the whole remote plan on the assumption anyway). Fill each target's card with probes run on that target.

**Probe order:** the claim whose truth would flip the verdict goes first — a relayed "no network" hiding one live mirror is the difference between a dead project and a running one.

### Gates G0–G6 — each component, in order, no skipping

| Gate | Rule | Fails if |
|---|---|---|
| G0 卡验真 | card reality-diffed and dated | facts taken from user text unprobed |
| G1 可达性 | dependency reachability probed (module cache / mirror / egress) before any plan | plan written while cache warmth unknown |
| G2 共存清点 | target's listening ports and running services inventoried | "known occupied" treated as "room exists" |
| G3 三轴评分 | footprint measured in bytes (never recalled), frequency, utility | estimate presented as fact |
| G4 判词 | one of four verdicts, recorded with reason | component enters with no verdict |
| G5 自证或挂起 | plan proven by one minimal smoke run, or suspension condition stated | "plan-level" presented as done |
| G6 方言回滚 | dialect matches card's OS family; rollback before install; workspace scanned | macOS solution copied to Linux; no reverse path |

### The Ledger

Every triage appends one row per component to the project's env ledger: `date | card id | component | footprint (bytes, measured) | verdict | reason | rollback step | verified-by (smoke + exit code)`. Append-only — a changed verdict is a new row. Its job is drift replay: "上周还能跑" starts by diffing today against the ledger.

## The Tri-Scale (references/tri-scale.md)

Score each component on three axes, then verdict:

| Verdict | Condition | Shape of the answer |
|---|---|---|
| INSTALL | footprint ≤ small AND (frequent OR high utility) | measured bytes + smoke proof + one-step rollback |
| LIGHT-SWAP | heavy footprint but an equivalent light implementation exists | install the light one; state the equivalence boundary |
| OFFLOAD | heavy footprint or always-on role AND device role mismatch (a desktop cannot host a 7x24 service) | route to the always-on host; 0 bytes local; cross-compile when possible |
| REFUSE | heavy + rare + no utility gain + refusal harmless | refuse WITH data plus route — never a bare no (references/refusal-protocol.md) |
| UNMEASURED | footprint unknown | stop. Measure first. An estimate never justifies a verdict |

- **Frequency**: every-session / weekly / per-project / one-off.
- **Utility**: efficiency, maintainability, creativity, version-control gains — the operator's own four.
- **Footprint bands** (defaults, operator-tunable): tiny ≤ 50 MB · small ≤ 200 MB · heavy > 200 MB · xlarge > 1 GB.

Golden INSTALL shape (RED R3): download the artifact, read its bytes (`ls -l`), execute it, keep a one-file rollback, defer conditional extras with an explicit trigger.

## Two Evidence Rules That Override Intuition

1. **Containerizing relocates the snowflake; it does not remove it.** A container is reproducibility evidence only when the image is built from a locked, diffable spec — otherwise it scores like "works on my machine".
2. **Test the artifact the user runs.** Anthropic's postmortem: internal usage and evals did not reproduce the failure; the fix was testing the exact public build. Every INSTALL acceptance exercises the exact artifact, not a stand-in.

## Collision Is a First-Class Axis

Two reproducible environments still break each other on one machine — ports, uid/gid bind mounts, orphaned `.venv` trees. Before INSTALL, name the collision and its avoidance. Prefer pinning *and* locking (checksummed lockfile) over tools that loosely "manage versions".

## Quick Reference

| Situation | First move |
|---|---|
| "需要什么环境你自己看着办" | Card → G0 diff → discover the project's real shape before planning |
| "大概多大？值得装吗？" | Measure the actual artifact; bytes before verdict |
| "部署到远程，7x24" | Target card by probe; cross-compile; systemd not launchd; rollback pre-staged |
| Docker Desktop on a desktop Mac, daemon down, no brew | REFUSE or OFFLOAD with data — the canonical heavy-daemon verdict |
| "上次还能跑，现在不行" | Drift replay (references/drift-replay.md) |

## Common Mistakes (source tags: R# = RED baseline, F# = evidence file)

- Accepting user-stated facts unprobed (R1: "daemon down (as stated)")
- Calling a plan cheap while reachability is unknown (R1: "pre-installed toolchain is the only reason this plan looks cheap")
- Inheriting assumptions across devices (R2: "amd64 — assumed… coin flip")
- Floating versions: `:latest`, unpinned transitive deps (F2)
- Kitchen-sink images; daemons in containers (F3, F7)
- No rehearsed reverse (F4)
- Hard-coded ports; collision blindness (F9)
- Secrets baked into config or layers (F6)

## Red Flags — STOP

- "The user told me it's down / already installed" — probe anyway
- "Same as the other machine" — cards never travel
- "It's only an estimate" — an estimate is not a verdict
- "I'll verify after deploying" — G5 violation
- "brew/pip/auto will sort out the versions" — pin or lock, or it floats
- "The probe exited 0" — a fake green: old macOS lacks `timeout`; a pipe's exit code is the LAST command's. Read output, not code.
- "It just works now" — a silent resolver (`GOTOOLCHAIN=auto`) can download a 225 MB toolchain mid-build; an install that skipped the gates is still an install — freeze the resolver, then score.
