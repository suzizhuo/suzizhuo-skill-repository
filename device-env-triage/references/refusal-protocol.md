# The Refusal Protocol — how an agent refuses a heavy environment

Triggered by the operator's own rule: 环境很大而且不常用 → **AI 自己拒绝**. This file
defines what a refusal IS and what it never is, so the agent refuses like an engineer with
data, not like a wall.

## A refusal is an engineering deliverable

Every REFUSE or OFFLOAD verdict ships as a five-part record:

```
1. VERDICT    — REFUSE or OFFLOAD, one word, first line
2. DATA       — measured footprint (download + installed), frequency, utility assessment
3. REASON     — which gate/axis decided it (G3 score, device role mismatch, collision risk)
4. ROUTE      — the alternative path, runnable: remote host, light swap, or "when you need it, here's the command"
5. FLIP       — what evidence would change the verdict (e.g. "if it becomes daily-use, re-score")
```

Example (the canonical case, from RED R1):

> **REFUSE — Docker Desktop on this Mac.** Data: ~700 MB installed, daemon currently down,
> no Docker.app present, no brew for its deps, machine role = single-user desktop.
> Reason: heavy + never-running role + zero current utility gain (G3).
> Route: anything containerised runs on the remote Debian host (MonkeyCode compose already
> lives there); local `docker` CLI stays as-is for client use.
> Flip: if a daily-use container workflow lands on this Mac, re-score with the real image list.

## What a refusal never is

- A bare "no", "不建议", or a shrug. If parts 2–4 are missing, it is not a refusal — it is
  an omission.
- A lecture about risk, legality, or policy. The verdict is about footprint and fit, full stop.
- A permanent wall. Every refusal carries its FLIP condition; refusing is a scored state,
  not a character trait.
- A negotiation the agent loses silently. Pressure does not change the data; it changes who
  bears the cost. See below.

## Under pressure

| Pressure | Agent's move |
|---|---|
| "Just install it, I don't care about the size" | Restate the two measured numbers once; then the decision belongs to the operator — but the refusal record stays in the ledger with the override noted. |
| "The other machine has it" | Cards never travel. Offer to fill the target card; verdict re-scores on that card. |
| "It's only an estimate anyway" | Then it is UNMEASURED, not INSTALL. Measure, then re-verdict. |
| "The install command is right there in the README" | READMEs describe the happy path on the author's machine; the gates score this device. |
| Time pressure ("quick, just get it running") | The refusal takes one line. Running the G1 reachability probe takes one command. Neither is the slow part. |

## Operator override

The operator outranks the verdict. When he overrides a REFUSE/OFFLOAD after seeing the
data, the agent: installs as instructed, appends the override to the ledger
(`... | REFUSE→OVERRIDE by operator <date> | data unchanged |`), and keeps the rollback
step. The ledger then records both the engineering verdict and the human decision — the
next triage reads both.

## Tie-in with dispatch skills

When the refusal happens inside work dispatched via `briefing-ai-agents`, it rides the
rejection channel there: verdict + data + route in the same message, so the requester can
re-scope without a round trip. A refusal with no route forces a re-brief; a refusal with a
route usually ends the thread.
