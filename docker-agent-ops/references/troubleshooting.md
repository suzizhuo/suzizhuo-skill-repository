# Troubleshooting — docker-agent-ops

Symptom → root cause → fix. Sources in `evidence-cases.md`; entries marked (RED) came from the 2026-11-01 baseline review.

## Permissions & state

| Symptom | Root cause | Fix |
|---|---|---|
| `EACCES: permission denied, open '/app/...'` on the Debian host, fine on macOS | macOS Docker Desktop does not enforce bind-mount ownership; Linux does. Container uid (e.g. 10001) ≠ host uid | Check `id -u` on the host. Writable+persistent → named volume (chown at build). If bind mount truly needed → rebuild with matching uid or `user: "uid:gid"` |
| Container rebuilt, login/session gone | State split across a directory and a sibling file; only the directory was mounted | Mount BOTH paths (symlink the sibling file into the same named volume) |
| Everything works until the first `down && up` | Writable path never declared in the contract; nobody owned the state question | Fill the deploy contract slot `state_owner` + `survives_rebuild` BEFORE first up (RL1) |

## Staying alive

| Symptom | Root cause | Fix |
|---|---|---|
| `docker ps` shows `Up 9 days`; the agent is dead and retrying | No healthcheck, or nobody reads it | `restart: unless-stopped` + healthcheck + log caps as one set; verification script must curl `/healthz` and assert the exit code (RL2) |
| Log directory at hundreds of MB | json-file driver unbounded | `max-size` + `max-file` in the logging block |
| TUI exits and everything stops | TUI was the service itself | Headless server + `profiles: [tui]` attach client |

## Secrets

| Symptom | Root cause | Fix |
|---|---|---|
| Keys readable via `docker inspect` | `env_file`/`environment` both land in `Config.Env` | Accept it: scope the blast (fine-grained per-purpose keys), keep .env 600, exclude from tar/scp bundles, never log |
| Token-shaped strings grep in logs | Agent echoes env at startup, or error tracebacks carry the header | Startup scrub: never print the value, only `has_api_key: true/false`; run the log grep in preflight (RL4) |
| Long-lived agents all re-auth at once | Shared writable state raced, or state mounted read-only | Read-write mount, single writer, prefer injected token + unified rotation |

## Networking & exposure

| Symptom | Root cause | Fix |
|---|---|---|
| Container "healthy", agent dead | `/healthz` is a constant 200 written by hand; nothing consumes it | Healthz must be a live probe, and the ladder ends in a real model call asserting the exit code (GREEN-2 P1) |
| Key rotated yesterday, still 401 | `env_file` values freeze at container CREATE; `up -d` reuses the old container env | Compare WITHOUT printing: `docker inspect ctr --format '{{range .Config.Env}}...'` lengths vs `grep ... .env` lengths — unequal = freeze confirmed; fix with `up -d --force-recreate`; confirm with a real call, not healthz |
| `.env` with real keys inside the scp tarball | `tar -C dir .` packs dotfiles; `.gitignore`/`.dockerignore` do not stop tar | Pack with an exclude list + gate (`tar tzf \| grep -E '(^\|/)\.env$'` must be empty) + extracted-text key-shape grep (GREEN-2 P0) |
| Stale frontend model id wins | Unknown incoming id hits `select()` and falls back to the old registration | Reject or repoint unknown ids at the agent boundary; fix both sides in the same session |
| Everything stops answering after host reboot with the old rules | One-time firewall config, not replayed; or `NET_ADMIN` let someone undo it | Replay egress rules in `postStartCommand`; treat NET_ADMIN as a capability, not isolation |
| Ollama at `host.docker.internal` unreachable on Debian | macOS-only DNS name | `extra_hosts: ["host.docker.internal:host-gateway"]` |

## Relay & model switching

| Symptom | Root cause | Fix |
|---|---|---|
| Frontend shows model A while model B answers | Agent `active` hot-swapped; frontend `Model` field hardcoded | Two-sided switch checklist in `monkey-code-relay.md` (RL6) |
| MonkeyCode untouched but traffic 404s | `MONKEYAI_NET` guessed wrong; edge cannot resolve `admin` | Run the network discovery command; write the real name into .env; never guess (RL5) |
| Downstream works, upstream of the edge 404s | Admin's own `location /` behavior differs from assumption | Fallback: own edge entry for the agent path; do not edit the admin container |

## Rollback & upgrades (RED)

| Symptom | Root cause | Fix |
|---|---|---|
| Bad rebuild, no way back | `image: local` overwritten each build | Tag `agent-ops:YYYYMMDD-N`; keep the previous tag; rollback = re-point tag + up (RL7) |
| Rebuild lost .env / models.json edits | No backup step in the runbook | Pre-build snapshot into `backup-*.tgz`; restore documented |
