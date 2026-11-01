# Deploy Templates — docker-agent-ops

Skeletons distilled from the RED baseline (which already passed review on structure) PLUS the red-line fixes from `rework-log.md`. Adapt paths/names; the marked slots are the load-bearing parts.

## Step 0 — repo inspection outputs (fill before writing ANY build file)

```
entry_point:     [what actually runs; library-vs-service verdict with evidence]
engines:         [python/node constraint read from the repo, not habit]
pinned_versions: [exact pins; EACH name verified present on PyPI/npm — pinned-but-wrong installs nothing]
local_run:       [the entry ran once OUTSIDE the container: command + result. A missing __main__
                  guard passes every static check and still dies on container start]
```

## Skeleton A — agent service (Dockerfile)

```dockerfile
# syntax=docker/dockerfile:1
# Pick the runtime from the TARGET REPO's engines field, never the habit default.
# python side:
FROM python:3.12-slim-bookworm
# node side: FROM node:22-slim   (NOT node:20 — EOL, engines ≥22, bundled CVEs)

ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1 PIP_NO_CACHE_DIR=1

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
# Pin exact versions (RED used langchain==1.4.7, langgraph>=1.2.12,<1.3.0) — no unpinned ranges.
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY models.json ./models.json
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# RL1: non-root with an EXPLICIT uid; every writable path chowned here.
# If any writable path maps to the host (bind mount), align uid with the host (see Skeleton C).
RUN useradd --create-home --uid 10001 appuser \
 && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8000/healthz || exit 1

ENTRYPOINT ["entrypoint.sh"]
CMD ["server"]
```

`entrypoint.sh` shape: `set -eu`, one case per mode (`server` / `tui` / `shell`), `exec` the last command so signals land on the app, not the shell.

## Skeleton B — compose (the resident set)

```yaml
name: agent-ops

x-logging: &default-logging          # RL2: log caps are part of the resident set
  driver: json-file
  options:
    max-size: "10m"
    max-file: "3"

services:
  agent:
    build: { context: ./agent, dockerfile: Dockerfile }
    image: agent-ops:YYYYMMDD-N     # RL7: tag, never :local overwrite
    restart: unless-stopped         # RL2: resident set = restart + healthcheck + logcaps
    logging: *default-logging
    env_file: [.env]                # RL4: inspect-visible; keep the file 600, out of tar/scp
    environment:
      AGENT_MODELS_FILE: /app/models.json
    volumes:
      # RL1: read-only config bind; WRITABLE paths must be named volumes, decided in the contract
      - ./models.json:/app/models.json:ro
    ports:
      # RL3: service itself loopback-only; the outside world meets `edge`
      - "127.0.0.1:${AGENT_PORT:-8091}:8000"
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://127.0.0.1:8000/healthz"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 20s

  edge:                             # the ONLY door to the agent
    image: nginx:1.27-alpine
    restart: unless-stopped
    logging: *default-logging
    depends_on:
      agent: { condition: service_healthy }
    ports:
      # RL3: 0.0.0.0 ALLOWED only when AGENT_API_TOKEN is non-empty (preflight hard-fails);
      # default: "127.0.0.1:${AGENT_EDGE_PORT:-8090}:8081" + SSH tunnel
      - "${AGENT_EDGE_BIND:-127.0.0.1}:${AGENT_EDGE_PORT:-8090}:8081"
    volumes:
      - ./edge/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./edge/inc.d:/etc/nginx/inc.d:ro

  agent-tui:                        # attach client, never the resident service
    image: agent-ops:YYYYMMDD-N
    profiles: [tui]
    command: [tui]
    stdin_open: true
    tty: true
    init: true
    environment:
      AGENT_BASE_URL: http://agent:8000
      AGENT_API_TOKEN: ${AGENT_API_TOKEN:-}
    depends_on:
      agent: { condition: service_healthy }
```

## Skeleton C — preflight (the gate that runs BEFORE `up`)

Each check is a hard fail with the fix printed next to it:

```bash
# 1. engine present and versioned (compose v2)
docker version --format '{{.Server.Version}}' || fail "remote docker engine not running"
docker compose version || fail "compose v2 missing"

# 2. host uid for bind-mount alignment (RL1 — the macOS/Linux trap)
HOST_UID=$(id -u)   # Debian: real check. macOS Docker Desktop: no check, different semantics.
[ "$HOST_UID" = "10001" ] || echo "WARN: image uid 10001 != host $HOST_UID — writable bind mounts will EACCES on Linux; use named volumes or rebuild with --build-arg UID=$HOST_UID"

# 3. exposure fail-closed (RL3)
if grep -q '^AGENT_EDGE_BIND=0.0.0.0' .env; then
  [ -n "$(grep -E '^AGENT_API_TOKEN=.+' .env | cut -d= -f2)" ] || fail "edge bound to 0.0.0.0 with EMPTY token — set AGENT_API_TOKEN or bind 127.0.0.1"
fi

# 4. ports free (edge port, agent debug port)
ss -ltn | grep -q ":${AGENT_EDGE_PORT:-8090} " && fail "edge port busy"

# 5. third-party topology facts (RL5) — for MonkeyCode, see monkey-code-relay.md
docker network ls --format '{{.Name}}' | grep -i monkey || fail "MonkeyCode network not found — run the discovery command, do NOT guess a name"

# 6. RL4: secret hygiene
[ "$(stat -c '%a' .env 2>/dev/null)" = "600" ] || fail ".env must be chmod 600"
grep -rn "sk-\|gsk_\|AKIA" --include='*.log' ./logs 2>/dev/null && fail "secret-shaped string found in logs"

# 7. RL7: rollback snapshot before any (re)build
tar czf "backup-$(date +%Y%m%d-%H%M).tgz" .env models.json 2>/dev/null
```

## Skeleton D — verification ladder (run in order; each gate must be green)

```bash
bin/verify.sh alone     # 1. healthz + one real model call + /models shape — WITHOUT third parties
bin/verify.sh relay     # 2. edge routes /agent/* to agent, other paths per the relay plan
bin/verify.sh monkey    # 3. frontend can reach the agent through the agreed path — LAST
```

Rule: gate N must be green before gate N+1 starts. A red gate 2 with a green gate 1 means the agent is fine and the wiring is wrong — the report must say that split explicitly.

Env-freeze note (RED→GREEN): `env_file` values are frozen at container CREATE. `up -d` after editing `.env` keeps the old values — rotation/recreate needs `up -d --force-recreate` and a real call to confirm, not healthz.

## Skeleton E — pack gate (before every scp; RL4 + RED tar-leak)

```bash
#!/usr/bin/env bash
# bin/pack-agent.sh — exclude + gate + only then scp
set -euo pipefail
SRC="${1:-.}"; OUT="${2:-./red-agent.tgz}"
rm -f "$OUT"
tar czf "$OUT" --exclude='.env' --exclude='*.tgz' --exclude='__pycache__' \
    --exclude='.git' --exclude='backups' -C "$SRC" .
tar tzf "$OUT" | grep -E '(^|/)\.env$' && { echo "FAIL: .env in bundle — scp BLOCKED"; exit 1; }
tar tzf "$OUT" | head -40                       # human eyeball: no surprise files
tar -xzf "$OUT" -O 2>/dev/null | grep -qE 'sk-[A-Za-z0-9]{20,}|gsk_[A-Za-z0-9]{20,}' \
  && { echo "FAIL: key-shaped string in extracted text"; exit 1; }
echo "PASS — scp allowed now"
```

## Delivery contract (MANDATORY slot in every handoff — RL8)

```
交付契约（缺一项不收）:
  未验证清单: [item — the exact command that would verify it]
  信心度: N/5 — 为什么
  首个验证动作: [single command the user should run first]
  回滚一句: [one line to get back to the last good state]
```

## Skeleton F — DEPLOY-CONTRACT.md (committed artifact, GREEN-1 shape)

```markdown
# Deploy Contract
filled: YYYY-MM-DD   env: remote Debian host + MonkeyCode compose

writable_paths:   [paths → named volume `agent-state` / host-uid aligned; EACCES rule note]
state_owner:      [who holds sessions; survives_rebuild: yes/no + why]
secrets:          [.env chmod 600; excluded by bin/pack-agent.sh gates; values never logged]
exposure:         [AGENT_EDGE_BIND=127.0.0.1 default; 0.0.0.0 only with non-empty AGENT_API_TOKEN]
relay_plan:       [verified: ... | unverified: ... (verify: <cmd>) | fallback: own edge entry]
model_switch:     [follow-active via /agent/v1/models | mapping table model-map.json]
rollback:         [image tag agent-ops:YYYYMMDD-N; backup path bin/backup.sh; restore steps]
```
