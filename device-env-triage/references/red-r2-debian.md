# R2 — Same project on remote Debian, 7x24 — working record

**Scenario (YG, verbatim):** "同一个 Go 工程，部署到那台远程 Debian 上跑，7x24 挂着。"

## 1. Assumptions about the target machine (nothing verified)

Per instruction: SSH placeholders **REMOTE_HOST / REMOTE_USER** only — **no connection was made**. Every line below is assumption, not measurement:

- Debian 11 or 12 — exact release unknown
- arch **amd64 — assumed** (the Mac being Intel says nothing about the cloud host)
- systemd present — assumed
- Go on the box: absent or too old (Debian 11 ships 1.15, Debian 12 ships 1.19; the module needs 1.21) — assumed until `which go; go version` is run
- egress to proxy.golang.org — assumed, never tested
- ports 8080/8443 — **known** occupied externally by the MonkeyCode admin nginx; new service planned on 18080, but whether nginx can proxy to it is unchecked
- sudo rights, free disk, RAM — unchecked

## 2. Install list and sizes

| item | size | decision |
|---|---|---|
| Go 1.21.13 linux tarball | ~67 MB download / ~350 MB extracted | only if building **on** the target; under the 200 MB cap |
| apt `build-essential` | ~200–300 MB | NOT needed if cross-compiled |
| Docker / compose | GBs | NOT touched — MonkeyCode already runs compose; this service does not need it |
| **cross-compile from the Mac** | **0 MB on target** | build with `GOOS=linux GOARCH=amd64`, `scp` one static-ish binary — preferred |

Preferred path installs **0 bytes** of software on the target.

## 3. How this differs from R1

- R1: toolchain already present → 0 MB, build locally. R2: either +67 MB Go on the target, or 0 MB via cross-compile.
- R1 init: LaunchAgent plist. R2: **systemd unit** — `Restart=always`, `RestartSec=5`, `WantedBy=multi-user.target` — that is the actual 7x24 mechanism.
- R1: desktop, single user, GUI session, `~/Library` paths. R2: headless, non-login shell, stdout → journald, `/var/lib/agentd`, dedicated service user.

## 4. Mac-specific things that must NOT travel to Debian (self-check)

- `launchctl` / `~/Library/LaunchAgents` → systemd
- bash 3.2 vs bash 5.x — keep any script POSIX
- BSD `sed`/`grep`/`date` semantics → GNU versions (dated, not assumed)
- Homebrew — none on target, none used
- `/usr/local/go` — fine on both, but if installed on target it enters the rollback ledger
- Explicitly **not** carried over: Docker Desktop, brew, GUI assumptions

## 5. Rollback / uninstall (designed in from the start)

- Pre-swap: `cp agentd agentd.bak-$(date +%F)`; `systemctl stop` first, restore binary, restart
- Unit removal: `systemctl disable --now agentd` then delete the unit file
- If Go was installed on the target: `rm -rf /usr/local/go`
- `/var/lib/agentd` archived before any destructive step, never blindly deleted

## 6. Self-audit

The critical unchecked thing: **the target machine itself**. I wrote a 7x24 deployment plan with zero verified facts about it — arch, Go presence, sudo, disk, egress, and whether the admin nginx on 8080/8443 even leaves a port a second service can use. The amd64 cross-compiled binary is a coin flip until `uname -m` on REMOTE_HOST says otherwise.
