---
name: docker-agent-ops
description: Use when putting an open-source agent project (langchain-style, GitHub repo) into Docker on a remote Debian cloud host, wiring MonkeyCode's frontend API to the containerized agent, or debugging agent-container failures. Triggers: "把 GitHub agent 装进 Docker", "langchain 容器化部署", "MonkeyCode 接 agent", "容器跑 TUI agent", "docker agent EACCES/权限", "容器重启就丢状态", "Up 9 days 但 agent 卡死", "模型切换前端不同步", "镜像升级回滚", "edge 暴露面/token".
---

# docker-agent-ops

## Overview

Run a third-party agent inside Docker on a remote Debian host — and keep it alive, reachable, cheap, and rollback-able.

**Core principle: declare before you run.** Every path that gets written, every secret that moves, every port that opens, and every unverified assumption gets written down BEFORE the first `docker compose up`.

## When to Use

- A repo like `langchain-ai/langchain` must run as a service inside Docker (note: it is a monorepo with no service — you will write the agent wrapper yourself)
- MonkeyCode (or any third-party console) must relay to your containerized agent
- Symptom matches: `EACCES` on the Linux host but fine on macOS; `Up 9 days` yet dead; state lost after restart; tokens exposed; model switch desyncs the frontend

Not for: single-container toy demos; containers on macOS as the runtime (host is a client — use `docker context` to the remote engine).

## The Iron Chain (in order; a red gate stops the chain)

1. **Inspect the repo first.** Entry point? server? TUI? Is it a library pretending to be a service? Pin versions from the repo's engines, never habit defaults (`node:20` is EOL; full images, not alpine shortcuts). Then two hard checks: every pinned package name EXISTS on the registry (a pinned-but-wrong name installs nothing — `langchain` vs `langchain-classic` burned the RED run), and the entry runs ONCE outside the container before any image is built (RED's compose passed every static check; the service still died on startup for a missing `__main__` guard).
2. **Fill the deploy contract** (below) before writing any compose.
3. **Alone before together.** Prove `/healthz` + one real model call + `/models` shape with agent + edge ALONE. Third-party wiring starts only after this gate is green.
4. **Headless resident, attach TUI.** The server is the resident process; the TUI is a `profiles: [tui]` attach client with `tty` — never the service itself.
5. **Resident set of three.** `restart: unless-stopped` + healthcheck + log size caps ship together. Missing one = rework. A `/healthz` that returns a constant 200 proves nothing — the verification ladder ends in a REAL model call asserting the exit code. Never trust `docker ps` uptime alone ("Up 9 days" is a documented busy-loop disguise).
6. **Relay with fallback.** Facts vs assumptions split first (see monkey-code-relay.md); unverified row never ships as config. Fallback = your own edge entry. **Never edit the third party's container configs** — downside unbounded, win small.
7. **Switch both sides.** Agent-side model switch AND frontend config move together, every time, verified through the real path — see monkey-code-relay.md checklist.
8. **Arm rollback first.** Tag the image (`agent-ops:YYYYMMDD-N`), snapshot `.env` + `models.json` before the first build, write the one-line rollback BEFORE the first deploy.

## Deploy Contract — mandatory, before first `up`

Seven slots, one line each, filled BEFORE any compose exists (file form: `references/deploy-templates.md` Skeleton F):

1. `writable_paths` — every write target → named volume or host-uid aligned
2. `state_owner` — who holds sessions/history; survives_rebuild yes/no + why
3. `secrets` — .env 600; out of tar/scp; log-grepped; inspect-visible accepted, blast minimized
4. `exposure` — loopback default; 0.0.0.0 REQUIRES non-empty token (preflight hard-fails)
5. `relay_plan` — verified facts | assumptions + verify command | fallback
6. `model_switch` — follow-active or mapping table; pick one, write it down
7. `rollback` — image tag + backup path + restore steps

## Red Flags — stop and redo

- "先 root 跑起来，权限以后再说" → ownership only bites later, on Linux, when it's expensive
- "端口只在内网" → the Debian host IS the public internet
- "密钥放环境变量最方便" → `docker inspect` shows every env var
- "MonkeyCode 大概率能填 Base URL" → probably is not a protocol; run the check
- "前端模型名写死没事" → silent desync after the first hot-swap
- "今晚能跑就行" → every delayed state decision reprices at the worst moment
- "tar -C dir . 打全量就好" → dotfiles (`.env`) ride along; `.gitignore`/`.dockerignore` do not stop tar — a pack gate runs before every scp
- "改完 .env up -d 就行" → `env_file` freezes at container CREATE; rotation means recreate, not restart

## Delivery slot (every handoff)

未验证清单（每条带验证命令）+ 信心度 N/5 + 首个验证动作 + 回滚一句。缺一项 = not delivered.

## References

- `references/deploy-templates.md` — Dockerfile / compose / preflight / verification-ladder skeletons with the red-line fixes baked in
- `references/monkey-code-relay.md` — topology facts, relay shapes, model-switch checklist
- `references/troubleshooting.md` — symptom → root cause → fix tables
- `references/evidence-cases.md` — 9 golden cases + 12 failure modes with URLs (Anthropic devcontainer, Trail of Bits, MonkeyCode, herdctl, sandcat…)
- `references/rework-log.md` — RED baseline findings and the red-line table this skill was built against
