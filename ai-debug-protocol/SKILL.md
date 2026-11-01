---
name: ai-debug-protocol
description: Use when an AI agent is dispatched to debug anything — a local bug, a hung GUI app, a browser/network issue, a production symptom, an SDK or environment failure. Triggers: "帮我修这个 bug", "500/转圈/卡死/闪屏", "日志看不懂", "点不动/窗口分不清", "抓包断点", "线上复现不了", "AI 修好了但没修好", "改了一堆不知道哪句起效". Also use before editing code to fix an unexplained failure, and when self-reviewing a debugging session that went sideways.
---

# ai-debug-protocol

## Overview

The protocol an AI agent follows when it is the one debugging. Covers the full target spectrum: code (local/remote), GUI apps, browser/network, environment/SDK. Core contract: **evidence before edits, one variable at a time, and a report the operator can audit.**

Three iron laws:
1. **判据先行** — before touching anything, write down what "fixed" means and what must not change.
2. **三态标注** — every claim is Seen (you observed it), Remembered (training prior), or Guessed (unverified). Never promote silently.
3. **一次一变量** — one hypothesis → one minimal experiment → one commit. Five changes at once means no attribution when it breaks.

## When to Use / Not

Use: any dispatched debugging task; any "why does this fail" investigation; self-check before declaring a fix. Not for: net-new feature work (no bug to explain); pure research reading (no experiment to run).

## Phase 0 — 判据先行 (gate, never skip)

Fill this before the first experiment:

```
现象锚点:      [user's exact words + one observable fact]
复现锚点:      [commands/steps that reproduce, or "cannot reproduce yet — next step is stabilize"]
done 判据:     [repro now behaves X; explicit pass command]
不可动判据:    [existing passing tests / behaviors that must not change — FAIL_TO_PASS + PASS_TO_PASS]
禁区:          [files/tables/endpoints the fix must not touch]
```

No done criteria → stop and ask (see reporting.md "interview until spec" — a vague brief is a stop condition, not a license to guess).

## Phase 1 — 选盒 (one decision, recorded)

| Question | Answer → Box |
|---|---|
| Can you see the source AND restart/repro locally? | **白盒** — breakpoints, evaluate, watchpoints, hot-debug (see white-box.md) |
| Only behavior/logs/network observable (no source, production, vendor binary)? | **黑盒** — strace/logs/capture/APM/bisect/delta-debugging (black-box.md) |
| Partial visibility: attached to a live process, or instrumentable, or recorded? | **灰盒** — Arthas/jad/watch/tracepoint/recording (grey-box.md) |
| GUI app unresponsive/wrong-click? | **GUI 现场协议** — gui-live-debug.md (window ID → human-grade click → hang forensics), deep execution may delegate to hot-debug |
| Browser/network symptom? | **browser-network.md** — devtools capture, conditional breakpoints, replay |

Record the choice and one reason. Moving boxes mid-investigation is allowed — record the transition and why (e.g., strace makes the race vanish = serialization evidence, hector martin).

## Phase 2 — 观测纪律

- **Read the scene first**: actual error text, actual logs, actual stack. `grep "Caused by"` + first own-package frame beats skimming the whole dump.
- **Minimal repro beats immersion**: build the cheapest possible repro (a 12-line server that reproduces the 500 is worth more than an hour of reading a large codebase).
- **Watch the observer effect**: when instrumentation changes the behavior, that change IS the clue (strace serializing a race).
- **Sign-reading table**: every tool output gets interpreted into named causes (`__psynch_mutexwait` = lock wait; `send/recv` = network hang; ExitCode 137 = OOM kill) — see per-file tables.
- **Audit trail**: every experiment logged — command, observation, conclusion, next. The log is the deliverable when memory is lost.

## Phase 3 — 修复纪律

1. State the root cause in one sentence. If the patch doesn't plausibly touch that root cause, don't write it (shallow-patch guard).
2. One change at a time. Re-run the repro after each.
3. Never make tests green by editing tests/assertions/mocks. If a test itself is wrong, that is a separate, argued commit.
4. **归因回退实验**: when several changes went in together and tests went green, attribution is lost — recover it by re-running each change alone against the repro (N trials for flaky symptoms). GREEN-2 measured the real thing: three changes, one contributed, one cancelled another, the "winning" rename contributed zero. Green ≠ attribution.
5. Remove debug residue: no leftover prints/flags/debug endpoints in the final diff; review checklist item.
6. HotSwap boundaries (Java): method bodies only; signature/member changes → restart route (devtools dual-classloader / JRebel / DCEVM); synthetic members can fail a body-only swap.
7. drop frame ≠ undo: local variables reset, global state does not revert.
8. **Destructive-change gate**: schema migration, prod config, data surgery without a RESTORE-VERIFIED backup + rehearsed rollback = stop, regardless of deadline pressure. Unlock checklist in reporting.md.

## Phase 4 — 验收 (no "looks done")

- Same minimal repro now passes, verified by command output — not by claim.
- Original interaction replay where possible: run the user's original steps and describe the behavior delta (Replay.io Nut standard).
- No regression on 不可动判据.
- The fix's "why it addresses the root cause" sentence survives an outsider's reading.

## Phase 5 — 报告与停机 (mandatory slots)

```
调试报告:
  证据:      [commands + outputs actually observed — Seen]
  假设:      [declared as Remembered/Guessed]
  实验:      [one per hypothesis, with result]
  结论/卡点: [root cause in one sentence, or the exact blocker]
  判据:      [done check result + 不可动 items status]
  未验证:    [each with the command that would verify it]
  信心度:    N/5, why
```

**Explicit stop conditions** (stop and report; never grind out a workaround):
- Missing info/permission, conflicting constraints, scope beyond the brief
- Two failed hypotheses with the same shape — the model of the system is wrong, not the patch
- Any destructive operation requested without a verified rollback

## Red Flags — stop and redo

- "我先改了试试看" before any repro exists → luck is not a method
- Five edits in one diff → attribution is already gone
- "测试绿了" as the only acceptance → green ≠ fixed; SWE-bench graders overstate merged-worthiness by ~24pp
- "我记得这个 API/类是……" → that's Remembered, not Seen — verify at the source
- "我找到一个变通办法" around a permission/constraint → that is a stop, not a solution
- "All tests pass" with no output attached → evidence, not adjectives
- "删掉这个失败测试就行了" → tests are immutable criteria
- leftover debug print in the final diff → OpenWRT/TP-Link class incidents started exactly here
- "急着上线" pressure on a destructive change → deadline is not an unlock criterion; it is exactly when the gate matters most (Replit deleted a prod DB during a "freeze")
- "测试绿了就交付" with several un-attributed changes → green is one data point; attribution needs the per-change experiment

## References (deep detail per box)

- `references/black-box.md` — strace/log/APM/flamegraph/bisect/delta-debugging arsenal, sign tables
- `references/white-box.md` — breakpoint mastery (conditional/hit-count/tracepoint/watchpoint), alter-flow weapons, HotSwap limits
- `references/grey-box.md` — Arthas clinical moves, feature flags, recording/time-travel
- `references/gui-live-debug.md` — window identification, human-grade clicking, hang/flicker forensics (delegates deep GUI work to the hot-debug skill)
- `references/browser-network.md` — devtools capture, conditional breakpoints, request replay, automation
- `references/case-library.md` — standard and generic cases indexed by symptom
- `references/reporting.md` — report template, interview-until-spec, stop protocol
- `references/evidence-cases.md` — 20 golden cases + 17 failure modes with URLs (SWE-agent ACI, METR, Claude Code, Arthas, marcan, geofft, JGC…)
- `references/rework-log.md` — RED baseline findings and the red-line table this skill was built against
