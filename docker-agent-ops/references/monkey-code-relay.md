# MonkeyCode Relay & Model Switching — docker-agent-ops

Wiring MonkeyCode's frontend/API to a containerized agent on the same Debian host. Written from the RED baseline review: the baseline got the staging right and the honesty right; this file turns its five guesses into a protocol.

## The topology (verified from `chaitin/MonkeyCode` repo, 2026-10-31)

```
外部流量 → admin (nginx, maps ADMIN_PORT:8080 + HTTPS_PORT:8443)
                └─→ backend (Go, :8080 inside, never mapped)
其它服务: db (postgres), rustfs, migrate
```

Facts: all external traffic enters through `admin`. `backend` is unreachable from outside. Treat anything beyond this as UNVERIFIED until a named command prints it. Supporting fact from the compose file (2026-10-31 read): `admin` runs `read_only: true` — there is no supported way to edit its configs even if you wanted to, which is why the relay rule is "own edge entry", never "patch admin".

## Facts vs Assumptions (RL5 — split before writing any config)

| Item | Status | The command that verifies it |
|---|---|---|
| All external traffic enters via admin | verified (compose file) | read `monkeyai/docker-compose.yml` |
| compose network name | UNVERIFIED (`monkeyai_default` is a guess) | `docker network ls --format '{{.Name}}' \| grep -i monkey` |
| admin's nginx `location` structure | UNVERIFIED (contents never fetched) | `docker compose ... exec admin nginx -T \| grep -n "include\|location"` (as far as permissions allow) |
| MonkeyCode supports a custom OpenAI-compatible provider | UNVERIFIED | look in the admin UI model settings, or `monkeyai/backend` config schema; if absent → fallback below |
| Frontend hardcodes an API host | UNVERIFIED (assumed not) | browser devtools on the frontend: what origin does it call? |

Hard rule: an unverified row never ships as a config. Run the command, paste the result, update the row, then write the config.

## Two relay shapes

**Shape 1 — custom-provider (if and only if verified):** MonkeyCode gains a provider with `Base URL = http://127.0.0.1:8090/agent/v1` (or the agent's port path) and `Model = <id from models.json>`. MonkeyCode does the routing; your edge only fronts the agent.

**Shape 2 — edge takeover (default fallback, zero changes to MonkeyCode):** your nginx `edge` listens on 8090 and routes `/agent/*` → agent, everything else → `admin:8080` (via the compose network, service name — not the host IP). Frontend sees ONE origin, so no CORS problem. Worst case: your edge fails; MonkeyCode is untouched.

Never a third shape: editing files inside the `admin` container. Downside risk is unbounded and the win is small.

## Staging gate (from the baseline, kept as an iron rule)

```
up-alone + alone-verify  (green: /healthz + one real model call + /models shape)
        ↓  ONLY THEN
up-monkey + monkey-verify (frontend → edge → agent full path)
```

A red `alone-verify` means never touch MonkeyCode wiring — report that split explicitly.

## Model switching — both sides, every time (RL6)

The user's stated pain: "底层模型切换，前端也要跟着切". Two places hold a model identity:

1. Agent side: `models.json` `active` — hot-swappable without restart (`bin/switch-model.sh <id>`).
2. Frontend side: whatever the relay shape installed — a hardcoded `Model` string, or a provider pointing at `active`.

The desync failure: agent `active` flips to `glm-4`; frontend still shows/requests `deepseek-chat`. Silent. Two fixes, pick one and write it down:

- **Follow-active:** the agent exposes `GET /agent/v1/models` and the frontend reads the active entry; frontend stores no model name of its own. Structurally, desync cannot happen.
- **Mapping table:** frontend names → agent ids, committed in a file both sides read; a switch adds one row.

Hard requirement either way: the agent must REJECT or repoint an unknown incoming model id — never silently select a default. GREEN-2 found the real-world bite: a stale frontend id made `select()` silently switch the agent BACK to the old model after the operator had already switched it, i.e. the stale side wins the argument.

Switch checklist (run after ANY agent-side switch):

```
[ ] agent /models returns the new active id
[ ] frontend config points at the mapping (not a stale literal)
[ ] one end-to-end call through the REAL path (frontend → edge → agent) — not curl to the agent alone
[ ] if the new model needs a different key: .env has the key, key name matches models.json api_key_env
```

Model adds vs switches: adding a new provider to `models.json` restarts the agent (registry loads once); switching among registered models does not. Say which one the user is doing.

## What this file deliberately does not decide

- Whether the frontend build can be rebuilt at all (needs the frontend repo/build path) — verify, don't assume.
- HTTPS: out of scope here; long-lived public use gets an SSH tunnel or a TLS terminator, decided before exposing the edge port.
