# Rework Log — docker-agent-ops

Append-only failure ledger. New iterations append; never rewrite history.

## RED Baseline (2026-11-01, subagent WITHOUT skill)

Scenario brief given verbatim (user's real words):

> "我在远程 Debian 云主机上跑 Monkey Code（compose 全家桶）。现在想把一个开源 agent 项目装进 Docker 跑起来，就 github.com/langchain-ai/langchain 这种，Monkey Code 那边的 API/前端接口要能转到这个 agent，底层模型切换的话前端也要跟着切。我用终端 TUI 的方式启动 agent。本机是老 Intel Mac，docker daemon 都没起过，只当客户端用。给我一套能直接抄的东西，今晚就要用。"

Artifacts: `/tmp/red-docker-agent-ops/` (Dockerfile, compose ×2, nginx edge ×3, Makefile, agent app: server/tui/agent_core/config, entrypoint, preflight/verify/switch-model scripts, models.json, .env.example, README)

### What the baseline already got right (keep, do not re-teach)

| Move | Evidence in baseline |
|---|---|
| Inspected the repo before writing anything | "我核过 langchain-ai/langchain 的 master 分支根目录……没有 Dockerfile、没有 compose、没有 server、没有 TUI 入口——它是纯 monorepo" → decided to write the agent service itself |
| Read the real MonkeyCode topology from the compose file | `db`/`migrate`/`rustfs`/`backend`(8080, unexposed)/`admin`(nginx, 8080+8443) → "所有外部流量只从 admin 进" |
| Staged integration: alone before together | `up-alone` + `alone-verify` gate before `up-monkey`; "先独立跑通，别一上来就接" |
| Never touched MonkeyCode's configs | "我不去改它的配置——改用我自己的 nginx 当新入口……最坏情况也只是我这边起不来，MonkeyCode 一根毛都不掉" |
| headless service + attach TUI | `agent-tui` is `profiles: [tui]` with `stdin_open/tty`; the server is the resident process |
| restart + healthcheck + log caps as one set | `restart: unless-stopped`, healthcheck 15s/5s/5 retries in BOTH compose and Dockerfile, json-file `max-size 10m max-file 3` |
| non-root with explicit uid | `useradd --uid 10001 appuser` + `chown -R appuser:appuser /app` + `USER appuser` |
| secrets only in .env, env NAMES in models.json | .env.example header "只写这里，绝不写进 models.json / Dockerfile / 镜像层"; `/models` only returns `has_api_key: true/false` |
| agent port bound to loopback only | `"127.0.0.1:${AGENT_PORT:-8091}:8000"` |
| shell tool off by default | `AGENT_ALLOW_SHELL=false` "这是安全默认" |
| `down` scoped to its own project | "down 只清 red-agent 这个 project、不碰 MonkeyCode 数据" |

### Red-line table — gaps the skill must close

| # | Baseline behavior (verbatim) | Why it fails in the real scenario | Skill makes you |
|---|---|---|---|
| RL1 | Zero writable path declared; persistence skipped: "没做持久化、没做会话存储。history 由调用方每次带进来" | State ownership is a decision, not an omission. First `docker compose down && up` (or a `logs` volume someone adds later) loses whatever the user assumed was durable. And uid 10001 vs host uid mismatch then bites exactly on the next writable volume added — macOS host does not check ownership, the Debian host does. | Declare every writable path AND its owner in the contract; writable+persistent → named volume (Docker-managed, chowned at build) or explicit host-uid alignment via `id -u` in preflight |
| RL2 | Restart/healthcheck/log caps present, but fake-alive check absent — no way to tell a busy-loop from a live agent without curling `/healthz` by hand | "Up 9 天" hidden busy loop is the classic failure (research #6). | `make ps` must show health state; verification script curls `/healthz` and asserts the exit code, not just `docker ps` output |
| RL3 | Token empty by default with 0.0.0.0 edge binding: `AGENT_API_TOKEN=` + `AGENT_EDGE_PORT` on 0.0.0.0; mitigation only in prose: "今晚别把 8090 直接暴露公网，哪怕有 token" | An open edge on a public cloud host is an open relay to paid model APIs — anyone burns their key's quota or reads the system prompt. | Exposure is fail-closed: edge binding 0.0.0.0 requires non-empty token; preflight HARD-FAILS otherwise; default is 127.0.0.1 + SSH tunnel |
| RL4 | `.env` advised `chmod 600`, but env_file values still land in `docker inspect`; no grep-the-logs self-check; no "keep .env out of the tar/scp bundle" rule | `env_file` hides nothing from `docker inspect`; the tarball the user scp's carries `.env` if `.gitignore` alone is trusted. | Secret chain checklist: 600 perms + excluded from tar/scp + checked against logs/image layers + fine-grained per-purpose keys |
| RL5 | Relay plan carried 5 explicit guesses (admin nginx contents unread, `MONKEYAI_NET` guessed `monkeyai_default`, custom-provider support unverified, same-origin assumption unverified, snapshot versions) | The guesses are honest, but they are guesses delivered at the same confidence as facts. The user cannot tell which steps are verified. | Relay facts vs assumptions split into two columns before writing configs; unverified → run the named check command; check fails → fallback = your own edge entry, never edit MonkeyCode's admin |
| RL6 | Model switch: agent hot-swap exists (`make switch-model`), frontend advice is a single sentence: "填 Base URL = http://<IP>:8090/agent/v1、Model = deepseek-chat" | The user's stated pain is "底层模型切换，前端也要切". A hardcoded frontend `Model` desyncs the moment the agent's `active` changes — silent, and the frontend shows the old model name while the new one answers. | Two-sided switch checklist: after any agent-side switch, re-check the frontend mapping; either frontend follows `active` or a mapping table is committed; mismatch is a checklist gate, not a footnote |
| RL7 | `image: red-agent:local`, rebuild overwrites in place; no backup of `.env`/`models.json`; no upgrade path recorded | First upgrade loses the ability to roll back; a bad rebuild is a cold start with unknown state. | Tag every image (`red-agent:YYYYMMDD-N`); back up `.env` + `models.json` + volumes inventory before rebuild; rollback = re-tag + restore + up, written down BEFORE the first deploy |
| RL8 | Uncertainty list + confidence 3/5 + "推到 4-5 的两个动作" appeared only because the RED prompt FORCED it | Without the demand, a strong-baseline agent ships confident-looking configs with its guesses invisible. | Delivery contract is structural: every handoff MUST carry unverified list + confidence + first verification action + one-line rollback — the template slot, not prose nearby |

### Rationalization red flags (self-check phrases seen or expected)

- "先用 root 跑起来，权限以后再说" → ownership only bites later, on Linux, when it is expensive
- "健康检查加了就行，日志没事" → 574 MB log files are a documented failure
- "端口只在内网没关系" → the Debian host IS the public internet
- "密钥放环境变量最方便" → `docker inspect` shows every env var
- "MonkeyCode 大概率能填 Base URL" → probably is not a protocol; run the check
- "前端模型名写死没事" → silent desync after the first hot-swap
- "今晚能跑就行，持久化以后加" → state decisions get cheaper every day you delay and most expensive the night you need them

## GREEN (2026-11-01, two subagents WITH skill, fresh context)

### GREEN-1 — same brief re-run → COMPLIANT (verified from artifacts, not the narrative)

| Rule | Evidence in `/tmp/green1-docker-agent-ops/` |
|---|---|
| Contract before compose | `DEPLOY-CONTRACT.md` exists as a standalone artifact |
| RL1 writable paths | config bind `:ro`; writable state → named volume `agent-state:/app/state` |
| RL2 resident set | `restart: unless-stopped` + healthcheck + `x-logging` caps; plus `init: true` and `tmpfs: [/tmp]` (adopted as good defaults, not required) |
| RL3 fail-closed | `"${AGENT_EDGE_BIND:-127.0.0.1}:..."`; token required for 0.0.0.0; service port loopback-only |
| RL7 rollback | `image: green-agent:${IMAGE_TAG:-20261101-1}`; `bin/backup.sh` exists |
| Relay | `facts/relay-facts.md` splits verified vs assumption; Shape 2 (own edge) is the default; admin is noted `read_only: true` |
| Both-sided switch | follow-active chosen and argued ("结构上不可能失配"); `bin/switch-check.sh` end-to-end verifier; `model-map.json` for the mapping option |

Adopted into the skill (origin noted in files): pin-exists-on-registry check + local entry run before build (`langchain==1.4.7` was the wrong name — real artifact is `langchain-classic`; and a missing `if __name__ == "__main__"` killed the server after all static checks passed); Bearer gate tested with wrong/null/right tokens ("prove the lock is locked"); healthz-as-live-probe ladder.

### GREEN-2 — harder incident scenario → COMPLIANT + found deeper failure modes

Correctly ranked four live incidents P0→P3 with mechanical criteria; arm-rollback before any change; 8-item assumption list with commands; confidence 4/5; four-part delivery contract present.

Adopted into the skill:
- **Constant-healthz fake-alive** (P1): `/healthz` hardcoded 200 while everything downstream 502s — escalates the resident-set rule to "a healthz that always answers 200 proves nothing; ladder ends in a real call".
- **Tar dotfile leak** (P0): `tar -C dir .` packs `.env`; `.gitignore`/`.dockerignore` do not stop tar — pack gate skeleton added to deploy-templates.
- **env_file freeze** (P0.4): `up -d` after editing `.env` keeps stale env — recreate, not restart.
- **select() silent revert** (P3): a stale frontend model id actively switched the agent BACK after the operator's switch — mapping must reject or repoint, never silently default.
- Reasoning moves kept as expectations: hypotheses labeled with falsification commands (H1), grep-to-find-the-writer before touching permissions, backup-before-change + "good-state" backup after key rotation (a rollback must not resurrect a revoked key).

### Regression (post-REFACTOR, 2026-11-01) — PASS

Fresh hard scenario (tar bundle carrying a fresh `.env` + rotated key still 401 + constant-200 healthz), subagent WITH the updated skill. Every adopted rule fired:

- P0 tar leak → ran `bin/pack-agent.sh` gates instead of scp'ing the bundle (Skeleton E)
- P1 rotate/still-401 → env-freeze root-caused to container-CREATE semantics; fix `up -d --force-recreate`; criteria = a real model call, not healthz
- P2 constant healthz → live-probe口径; went further: patched the endpoint, reverse-verified missing-key → 503, and fixed the "HTTP 503 but body status=ok" inconsistency
- Unprompted sweep found RL3 (edge hardcoded 0.0.0.0 + empty default token) and RL7 (`image: :local` untagged) — rules fired beyond the ask
- Adopted as expectation: secret-safe diagnostics (compare env-value LENGTHS container-vs-disk, never print values)

Iteration summary: RED baseline strong (structure right) → 8 red lines written → GREEN-1/GREEN-2 both compliant + 6 moves adopted → regression PASS.
