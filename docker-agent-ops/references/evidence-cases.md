# Evidence Cases — Docker Deploy Agent

Research date: 2026-10-31. Collected via web_fetch + GitHub contents API + HN Algolia (no web_search key on this machine). Method: skill-from-masters (practitioner cases first, theory later).

## Golden Cases (9)

### 1. Anthropic official `.devcontainer` — the factual baseline
- URL: https://github.com/anthropics/claude-code/tree/main/.devcontainer (companion features repo https://github.com/anthropics/devcontainer-features, 314 stars)
- What: ships a working container dev environment for Claude Code while tightening BOTH "what the container can do" and "where its network can go".
- Key details:
  - `devcontainer.json` (1662 B): `runArgs` with `--cap-add=NET_ADMIN --cap-add=NET_RAW`; `remoteUser: node`; named volumes `commandhistory` + `/home/node/.claude`; `containerEnv CLAUDE_CONFIG_DIR=/home/node/.claude`; `postStartCommand sudo /usr/local/bin/init-firewall.sh`
  - `Dockerfile` (2507 B): `FROM node:20`; installs `iptables`/`ipset`/`dnsutils`/`gh`; `RUN --mount=... --chown node:node`; npm global install `@anthropic-ai/claude-code`
  - `init-firewall.sh` (4548 B): iptables + ipset domain allowlist, everything else DROP
  - sudoers scoped to ONE command: `node ALL=(root) NOPASSWD: /usr/local/bin/init-firewall.sh`, no full root
- Numbers: three core files total ~8.7 KB; container user is `node` not root; egress allowlist only

### 2. Trail of Bits `claude-code-devcontainer` — most complete threat model
- URL: https://github.com/trailofbits/claude-code-devcontainer
- What: puts Claude Code in a container with an explicit threat model; "protects against / does not protect against" written in README, not guessed.
- Key details:
  - Auth via token route `claude setup-token`, never interactive OAuth inside the container — README cites `anthropics/claude-code#8938` (interactive-OAuth-wizard bug)
  - `post_install.py` does one-time token handshake
  - iptables/ipset egress allowlist; named volumes keep login across rebuilds
  - Threat model: **protects** bypassPermissions, SSH into container, filesystem reach, cross-engagement credential leakage; **does not protect** container escape, VS Code "Reopen in Container", `NET_ADMIN` rules rewritten by the container user, delayed escape via shared `.git`
  - Ships Colima perf tuning notes
- Numbers: 961 stars / 107 forks

### 3. MonkeyCode (Chaitin) — one-command remote Linux deployment, the exact Monkey Code topology
- URL: https://github.com/chaitin/MonkeyCode
- What: enterprise AI workbench; one script pulls up the whole Docker Compose family on a remote Linux host.
- Key details: install script `https://release.monkeycode-ai.com/public/monkeyai-install.sh`; Docker Compose orchestration (not single container); server `work.baizhi.cloud`; AGPL-3.0
- Real topology read from `monkeyai/docker-compose.yml`: `db`(postgres:18.6) / `migrate` / `rustfs` / `backend`(Go, in-container `:8080`, **no external port mapping**) / `admin`(nginx, maps `MONKEYAI_ADMIN_PORT:8080` and `MONKEYAI_HTTPS_PORT:8443`). ALL external traffic enters through `admin` only.
- Numbers: 4916 stars; minimum 1 CPU core + 1GB RAM

### 4. `tim-sha256/claude-in-docker` — minimal viable + credential minimalism
- URL: https://github.com/tim-sha256/claude-in-docker (article https://timsh.org/claude-inside-docker/ , HN 265 pts / 161 comments)
- What: the whole thing is one `devcontainer.json`, highest readability.
- Key detail: git credentials use a fine-grained GitHub PAT with ONLY `Contents: RW` — blast radius shrinks from "push/read the whole account" to single-repo content RW.
- Numbers: single-file solution; HN 265 pts

### 5. foldr.uk — Pro-subscription OAuth state split repair
- URL: https://foldr.uk/claude-code-pro-subscription-docker/
- What: keeps Pro-subscription users logged in inside containers without repeated re-auth.
- Key detail: Claude Code's persistent state is SPLIT — `~/.claude/` (directory) + `~/.claude.json` (sibling file); mounting only the directory loses the file; use a symlink to pull both into one `claude-data` named volume; pitfall: the entrypoint overwrites already-refreshed credentials.
- Numbers: login survives container rebuilds

### 6. VirtusLab/sandcat — zero real credentials inside the container
- URL: https://github.com/VirtusLab/sandcat
- What: container agents reach external services while credentials never enter the container.
- Key details: all traffic through mitmproxy + WireGuard transparent proxy; allow/deny engine governs DNS too; **secret substitution** — container processes only ever see placeholders, never real tokens.
- Numbers: 200 stars

### 7. herdctl — multi-agent Docker resource constraints
- URL: https://github.com/edspencer/herdctl (article https://edspencer.net/2026/2/4/run-claude-code-agents-docker-herdctl )
- Key details: `docker: enabled: true`; `user: "1000:1000"` (explicit alignment with host uid — see failure mode 1); `memory 4g`; `pids_limit 100`; minimal PAT per agent
- Numbers: per-agent memory cap 4g, process cap 100

### 8. Docker Sandboxes (vendor solution) — harder isolation than namespaces
- URL: https://www.docker.com/products/docker-sandboxes/ ; blog https://www.docker.com/blog/docker-sandboxes-run-claude-code-and-other-coding-agents-unsupervised-but-safely/
- Key detail: MicroVM / hypervisor isolation instead of pure Linux namespaces — closes the "does not protect container escape" line in Trail of Bits' threat model.
- Not verified: `docs.docker.com/sandboxes/` fetch failed; only product page and blog retrieved.

### 9. `smithclay/claudetainer` — counter-example
- URL: https://github.com/smithclay/claudetainer
- What: brew tap distribution of a devcontainer; **already deprecated, superseded by Claude Code Remote Control**.
- Why it matters: do not copy the 2024–early-2025 "hand-rolled agent container" structure; the official remote-control path got cheaper.
- Numbers: 107 stars

## Failure Modes (12)

### 1. Volume permission EACCES / uid mismatch
- Symptom: works on macOS host, dies immediately on a Linux cloud host with `EACCES: permission denied, open '/app/dist/...'` — hundreds of log lines.
- Root cause: container process writes bind mount as root or uid≠1000; **macOS Docker Desktop's virtiofs/gRPC-FUSE does not do real ownership checks, Linux does**; the same compose file means different things in the two places.
- Fix: `RUN --mount=type=cache --chown` or `COPY --chown=node:node` in the image; `user: "1000:1000"` in compose; writable data goes to named volumes; on remote Linux the uid must explicitly match the host uid.
- Sources: https://github.com/microsoft/scope/issues/1599 (TS5033 EACCES on dist/, ~860 log lines); https://github.com/ptarmiganlabs/butler-sos/issues/1600 (macOS vs Linux ownership, uid 1000)

### 2. Split login-state directories: `~/.claude` ≠ all state
- Symptom: `~/.claude` is mounted, yet after container rebuild it still demands re-login / re-auth.
- Root cause: state lives in two places — `~/.claude/` directory and `~/.claude.json` file; mounting the directory alone misses the file.
- Fix: symlink `~/.claude.json` into the same named volume; entrypoint must not overwrite proxy-refreshed credential files (copy-back refresh = wasted).
- Source: https://foldr.uk/claude-code-pro-subscription-docker/

### 3. Interactive OAuth hangs inside the container
- Symptom: first `claude` run in a container freezes at the interactive wizard.
- Root cause: no working interactive OAuth wizard path inside the container (known bug).
- Fix: `claude setup-token` on the host first, inject the token via env/volume, start non-interactively in the container.
- Sources: https://github.com/trailofbits/claude-code-devcontainer (README cites https://github.com/anthropics/claude-code/issues/8938)

### 4. Refresh-token race / read-only mount mass logout
- Symptom: several long-running agents all demand re-auth after a while.
- Root cause: multiple processes refresh the same token file concurrently; login dir mounted read-only so refreshes cannot persist.
- Fix: read-write mount for state dir; single writer (or per-agent config dir); prefer token injection + unified rotation.
- Sources: https://github.com/anthropics/claude-code/issues/54443 ; https://github.com/foundry-ai/foundry/issues/613

### 5. Node version defaults already fail engines and carry CVEs
- Symptom: agent fails engines check right after install; or trivy reports a pile of highs.
- Root cause: stock Dockerfiles default `FROM node:20` or `alpine` while `claude-code` engines require Node ≥22; Node 20 EOL 2026-04-30; the bundled npm layer carries CVEs.
- Fix: `FROM node:22`/`node:24` full image (not alpine shortcut); pin global npm packages; run trivy after build.
- Sources: https://github.com/gane5hvarma/harness-adapters/issues/6 (Node 20 EOL, engines ≥22, trivy 1 CRITICAL / 22 HIGH); https://github.com/actava-ai/chi-bench/issues/50; https://github.com/cloudflare-omnigent/api-gateway/issues/56

### 6. No restart policy → stuck busy loop faking "Up 9 days"
- Symptom: `docker ps` shows `Up 9 days` but the agent died long ago and is infinitely retrying, burning logs.
- Root cause: bare `docker run` with no restart policy and no healthcheck; supervisor-style orphan processes blow up logs.
- Fix: `restart: unless-stopped` + healthcheck; agent-side session management + heartbeat/watchdog; log rotation with size caps; decide cold-start vs resident explicitly.
- Sources: https://news.ycombinator.com/item?id=47320329 ; https://github.com/lolimmlost/aidj/issues/336 ; https://github.com/barlito/youl-coin-api/issues/146 (574 MB looping log)

### 7. macOS vs Linux toolchain semantics
- Symptom: same container behaves differently on an old Intel Mac vs the cloud host (paths, ownership, permission bits).
- Root cause: Docker Desktop VM/file-sharing layer vs native Linux overlayfs + bind mount semantics.
- Fix: treat the host as a client only — use `docker context` to the remote Linux engine; local machine runs no containers. This is exactly how DevPod / VS Code Remote work.
- Sources: https://news.ycombinator.com/item?id=44548260 ; https://news.ycombinator.com/item?id=44548653

### 8. DevPod is not "pure client" — remote must already have docker
- Symptom: assuming DevPod-style client tools bring their own runtime; hits a wall deploying to the Monkey Code remote container.
- Root cause: DevPod's SSH provider requires docker ALREADY on the remote host; it is an orchestration layer, not an engine.
- Fix: remote Linux host gets docker engine + compose first (the MonkeyCode install script does exactly this); tool layers only schedule.
- Source: https://news.ycombinator.com/item?id=44549075

### 9. GUI in containers wastes a whole day
- Symptom: trying to put a GUI tool in a container; X11/display forwarding burns a day and still fails.
- Root cause: containers have no display by default; macOS hosts have no X server to forward to.
- Fix: container runs headless only; GUI/IDE stays on the host and interacts via code in shared volumes; code-server/wayland only if a browser IDE is truly needed.
- Sources: https://news.ycombinator.com/item?id=44547649 ; https://news.ycombinator.com/item?id=44547758

### 10. Forcing Docker Desktop onto an old Intel Mac
- Symptom: Docker Desktop stalls, eats resources, feels bad.
- Root cause: Docker Desktop is a heavyweight VM on old Intel Macs.
- Fix: Colima (or OrStack) as a lightweight `docker` CLI backend; the host is a control plane only, load goes to the remote Linux box.
- Sources: https://news.ycombinator.com/item?id=44548928 ; https://news.ycombinator.com/item?id=44549034

### 11. Firewall rules lost on restart; `NET_ADMIN` lets the container rewrite them
- Symptom: configured iptables allowlist reverts to full internet on container restart; worse, the in-container user can rewrite the rules back.
- Root cause: iptables rules are not persistent; `--cap-add=NET_ADMIN` also grants the container process table access.
- Fix: `postStartCommand` replays rules on every start (what Anthropic does); accept `NET_ADMIN` as a capability grant, not isolation; for "truly cannot see credentials" use a proxy + secret substitution, not iptables alone.
- Sources: https://github.com/trailofbits/claude-code-devcontainer ; https://github.com/VirtusLab/sandcat

### 12. Credentials written in plaintext + visible via argv / `docker inspect` / logs
- Symptom: API keys sit in plaintext in container files; a Grok-CLI-class incident uploaded the whole home directory in plaintext; `docker inspect` and process argv expose tokens directly.
- Root cause: agents cache secrets as local files; tokens passed via `-e KEY=...` (visible in `docker inspect`).
- Fix: enforce on the egress gateway (container never holds real credentials); inject tokens via files/volumes not argv; secret-scan images and logs; fine-grained PATs shrink blast radius.
- Sources: https://github.com/AutoForge/AutoForge/issues/290 (ban tokens in argv/inspect/images/logs); https://news.ycombinator.com/item?id=48892468 (Grok CLI incident, original post https://twitter.com/a_green_being/status/2076598897779020159); https://news.ycombinator.com/item?id=44533044 (OneCLI: agent wrote secrets to local files)

## Contrast Table

| Do this | Not that | Why |
|---|---|---|
| `claude setup-token` on the host, inject token via env var / volume | interactive OAuth wizard in container, or token via `-e KEY=...` in argv | interactive wizard is a known-broken path (#8938); argv values are visible to `docker inspect` and process listings |
| build-time `COPY --chown=node:node` + compose `user: "1000:1000"` | run as root, or uid 0 writing bind mounts | macOS skips ownership checks, Linux enforces them — identical configs, opposite outcomes, EACCES guaranteed |
| symlink BOTH `~/.claude` and `~/.claude.json` into one `claude-data` named volume | mount only `~/.claude`, or bind the whole `$HOME` | state is split across two paths; binding `$HOME` ships SSH keys and `.gitconfig` into the container |
| `FROM node:22`/`node:24` full image + pinned `@anthropic-ai/claude-code`, trivy after build | default `FROM node:20` or alpine | engines require ≥22, Node 20 is EOL, bundled CVEs (1 CRITICAL / 22 HIGH) |
| ipset domain allowlist + `postStartCommand` replay each start + proxy-side secret substitution | default bridge with full internet; one-time iptables config | rules die on restart; `NET_ADMIN` lets the container user undo them; iptables cannot protect credentials already inside the container |
| healthcheck + `restart: unless-stopped` + agent-side heartbeat + log size caps | bare `docker run`, no policy, no probe | "Up 9 days" may be a stuck busy loop; logs can reach 574 MB |
| fine-grained GitHub PAT (`Contents: RW` only), one per agent | mount the whole `~/.ssh` + `~/.gitconfig` | blast radius shrinks from "entire account" to "single-repo content RW" |
| commit `devcontainer.json` / `Dockerfile` / `compose.yml` into the repo, rebuild never loses config | one-shot `docker run` with a long parameter string | reproducible, reviewable, rebuildable; long parameter strings are untraceable and unrecoverable |
| old Intel Mac: Colima / OrStack for the `docker` CLI, load on remote Linux | force Docker Desktop | Desktop stalls on old Intel Macs; host as control plane only is the stable shape |
| remote Linux host has docker engine + compose first, tools only orchestrate | assume DevPod/Remote tools bring their own runtime | DevPod SSH provider explicitly requires remote docker |
| write down "does not protect against container escape"; sensitive repos go to MicroVM/hypervisor sandboxes | treat Linux namespaces as a security boundary | escape paths are real (shared `.git`, "Reopen in Container", `NET_ADMIN`) |
| container runs the headless agent; IDE/GUI stays on the host | put GUI apps in containers and configure X11 forwarding | macOS has no X server — pure time waste; shared volumes are enough |

## Declared Gaps (not fabricated)

- Docker official sandboxes docs page failed to fetch (`docs.docker.com/sandboxes/`); only the product page and blog were retrieved, technical details unverified.
- No authoritative Docker compose orchestration doc for AI agents exists — `docs.docker.com/ai/` returns 404; compose orchestration details all come from third-party repos and HN practice, no vendor spec.
- No reliable first-party golden case found for Codex-in-Docker; excluded from this report.
- devclass.com coverage was bot-blocked twice; abandoned as a source.
