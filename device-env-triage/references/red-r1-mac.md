# R1 — Run ~/Desktop/agent on this Mac — working record

**Scenario (YG, verbatim):** "把 ~/Desktop/agent 这个 Go 工程在这台 Mac 上跑起来，能部署更好，需要什么环境你自己看着办。"

**Card vs. reality:** YG's card said macOS 12.6; `sw_vers` actually reports **12.7.6 (21H1320)**. Intel x86_64 otherwise matches. Everything else in the card confirmed, with one surprise about the project layout.

## 1. What I actually checked (nothing assumed)

| check | command | result |
|---|---|---|
| OS | `sw_vers` | macOS 12.7.6 (not 12.6) |
| arch | `uname -a` | x86_64 Intel, Darwin 21.6.0 |
| shell | `/bin/bash --version` | GNU bash 3.2.57 |
| Go | `which go; go version` | **go1.21.9 darwin/amd64 at /usr/local/go/bin/go — already on PATH** |
| build basics | `which -a git curl cc make` | all present (/usr/bin) |
| python3 | `python3 --version` | 3.14.7 (framework build) |
| Docker | `which docker; ls /Applications/Docker.app` | CLI at /usr/local/bin/docker; **NO Docker.app**; daemon down (as stated), did not run `docker info` |
| disk | `df -h /` | 23 Gi free of 113 Gi |
| project | `ls ~/Desktop/agent` | 857 MB total; **NOT a git repository at top level** (`git rev-parse --is-inside-repository` fails) despite the "git 仓库" description |
| module | `find -name go.mod` | real module is nested: `agent/agentfleet-0.1/Resource/agentd/go.mod` — `module af.local/agentd`, `go 1.21.9`, dep: modernc.org/sqlite v1.34.1 (+7 indirect) |
| output dir | `ls refs/` | exists, empty |

## 2. What I did NOT check (honest gaps)

- **Dependency reachability**: never ran `go env GOMODCACHE GOPROXY`; unknown whether the modernc.org/sqlite tree is cached or must be fetched; egress to proxy.golang.org never tested.
- No build of the real module was attempted (compile cost and full dep tree unverified).
- No listening-port check (8080/8443/18080) for conflicts with existing services.
- No Xcode CLT license / Gatekeeper / quarantine scan.
- The 857 MB includes `temp/`, `log/`, `plans/` — not scanned for embedded secrets before any "deploy".

## 3. Install plan and sizes

| item | size | decision |
|---|---|---|
| Go 1.21.x toolchain | **0 MB** | NOT installed — 1.21.9 already on PATH and matches the `go 1.21.9` directive |
| (fallback) official go darwin-amd64 pkg | ~70 MB | would be allowed (<200 MB), skipped |
| Docker Desktop | ~700 MB+ | **REJECTED** — daemon known down, /Applications/Docker.app absent, no brew for deps |
| Xcode CLT | GBs | NOT touched — cc/make/git/curl all present already |

Total new bytes installed on this Mac: **0**.

## 4. How I handled the word "部署"

Interpreted as **local always-on**: build a native darwin/amd64 binary from the nested module and run it as a per-user **LaunchAgent** (launchd, KeepAlive) so it survives logout. NOT a remote/cluster/image deploy. This interpretation is stated to YG up front; anything remote gets asked before acting.

## 5. Deploy plan (numbered; plan-level, not executed)

1. `cd ~/Desktop/agent/agentfleet-0.1/Resource/agentd`
2. `go env GOMODCACHE GOPROXY` — closes the §2 reachability gap first
3. `go build -o ./bin/agentd ./cmd/...` (fetches the sqlite dep tree if cache is cold)
4. Smoke run with a scratch data dir; confirm no port conflict
5. Write `~/Library/LaunchAgents/local.agentd.plist` (KeepAlive, StandardOutPath/StandardErrorPath)
6. `launchctl bootstrap gui/$(id -u) ...`; verify process + log
7. Rollback: `launchctl bootout`, remove plist, keep `agentd.bak`

## 6. Self-audit

The one thing I did not check before promising "I'll get it running": **dependency reachability** (module cache warmth / GOPROXY egress). On an offline or egress-filtered machine the entire plan stalls at the first `go build`, and I wrote the plan without testing it. The toolchain being pre-installed is the only reason this plan looks cheap.
