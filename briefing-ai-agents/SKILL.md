---
name: briefing-ai-agents
description: Use when dispatching any work to an AI agent (Claude Code, Monkey Code container, desktop assistant) — vague requirement, "帮我做个…", agent claims done unverified, environment/version mismatch, over-engineered code the human can't read, mid-task requirement drift, or rework/驳回 after a bad delivery. Covers requirement clarification, environment fact-cards, acceptance templates, rejection scripts, and change ledgers for mixed task types (coding, remote projects, security labs, persona skills, GitHub, Java, C, Docker).
---

# Briefing AI Agents

## Overview

Spec quality is the delivery bottleneck: agents build the wrong thing because "done" was never defined. This skill is the fixed ritual every agent must run before and during dispatched work, so a vague human request becomes a one-pass correct delivery. Principle: 先仪式后动手 — no code, no file move, no claim before the three beats complete.

Beats may be DRAFTED in one turn to save the user's rounds, but each beat is complete only on the user's explicit approval ("按默认/同意"); until a beat is approved the next beat stays a draft and zero action runs.

## When to Use

- User hands over a vague/unfinished requirement ("你看着办", "乱七八糟的")
- Agent is about to write code, run commands, or deploy in an environment it has not probed
- Agent reports completion, or user wants to 驳回/返工 a delivery
- Task spans user's mixed environments: local old Intel Mac (macOS 12.6) and/or Monkey Code cloud container
- New requirements surface mid-task

When NOT to use: pure Q&A, reading files the user points at, work already fully specified with a written spec.

## The Ritual (every dispatch, in order)

### Beat 1 — Clarify until spec-writable
Interview the user as multiple-choice questions, max 3 rounds, until you could write a one-page spec yourself. Give concrete defaults they can approve with one word ("按默认开跑" = go). Never skip clarifying because "user said 看着办" — that delegates the DECISION, not the DEFINITION. Stop and ask before any destructive or irreversible action, no matter how confident you feel.

### Beat 2 — Environment fact-card + assumption ledger
Before writing any path-, version-, permission-, or dependency-dependent code:
1. **Probe the actual target machine** and paste real command output into the brief: `uname -mrs; sw_ver 2>/dev/null; bash --version | head -1; docker version --format '{{.Server.Version}}' 2>&1 | head -1`. Asking "which machine?" is NOT a substitute for probing it — the Gemini-CLI data-loss bug was an agent that assumed `mkdir` succeeded (gemini-cli#4586).
2. **Verify capability, never assume it.** List the tools this session actually has (can you see the container's execution output? reach the network?). "我能看到容器输出" must be proven, not stated.
3. **Assumption ledger**: every remaining assumption written as a numbered line the user approves or corrects. Unapproved assumptions block destructive steps.

Use `references/env-cards.md` for the operator's two standing cards (MAC_LOCAL, MONKEY_CONTAINER).

### Beat 3 — Acceptance contract (DoD)
Write the done-definition BEFORE work starts, from `references/acceptance-templates.md` for the task type. Rules:
- Each criterion gets **PASS/FAIL/UNKNOWN + evidence path**. An agent sentence "I built and tested this" is a HYPOTHESIS until command output exists (Cloudflare shipped "production-grade" that didn't compile — simonwillison.net/2025/Dec/18/code-proven-to-work).
- **Negative acceptance**: list forbidden outcomes/behaviors that instant-fail. Models regress to common approximations even with explicit warnings (inventwithpython.com/blog/vibe-coding-failures.html).
- One unforgeable positive proof: a real run, a real request, a real screenshot.

## During Work

- **Change ledger** (user remembers new requirements mid-task): append to the 当前有效约束 ledger FIRST, state which earlier decision it voids, then re-plan. Irreversible actions (delete DB, migrate, force-push, publish, wipe) need separate explicit approval with a named rollback. A Replit agent deleted 1,206 production records during a "code freeze" — late constraints must be written down, not remembered (crackr.dev/vibe-coding-failures).
- **Scale caps**: minimal implementation first; ≤400–700 changed lines per unit; name a file-count cap before starting; a new abstraction requires a named second call site. End every unit with "what did you DELETE?" — AI code bloats (CodeRabbit: 1.7× issue rate, coderabbit.ai/blog/state-of-ai-vs-human-code-generation-report).
- **No fabricated dependencies**: every dependency must resolve in the probed target env; no stub/shim/invented module to make tests green (hallucinated-package incidents, crackr.dev).
- **Ghost rule**: before fixing a bug, prove the change is on the executed path (minimal repro + observable log first). METR RCT: engineers felt 20% faster, measured 19% slower (metr.org/blog/2025-07-10-early-2025-ai-experienced-os-dev-study/) — feelings are not evidence.
- **Big jobs split builder/verifier**: whoever builds is never the sole verifier; the verifier reports only correctness gaps with file+line evidence, no style nits.

## Rejection & Rework

- Translating "感觉不对/太乱/看不懂" into executable fix orders → `references/rejection-scripts.md`.
- Every rework appends one line to the rework ledger in the operator's skill folder: 派了啥 / 翻车点 / 根因 / 下次禁令. At dispatch time, read the ledger for the same task type first.
- User-facing voice stays 口语化: verdict first, then evidence, no process narration.

## Red Flags — STOP, restart the beat

- "Asking is cheaper than connecting" → probe the machine; that assumption IS the Gemini-CLI bug.
- "Small task, no formal DoD needed" → small tasks are where the habit forms; Cloudflare's claim died there.
- "The agent said it tested it" → hypothesis until output exists.
- "User said 看着办 so I skipped questions" → defaults + one-word veto, not silence.
- "I can probably see the container output" → prove the tool exists first.
- "User remembered something new mid-task, I'll just handle it" → ledger entry first (Replit rule).
- "It works on my side" → in whose environment? name the probed target.

## Evidence Base

Full case table with URLs: `references/evidence-cases.md` (golden cases + failure incidents this ritual is built from).
