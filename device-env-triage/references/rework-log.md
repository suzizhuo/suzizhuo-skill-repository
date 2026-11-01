# Rework Ledger — device-env-triage

Read this file at dispatch time for the same task type BEFORE planning. Append-only; never edit old lines.

Format: `R<k> | <date> | 任务类型 | 派了啥 | 翻车点 | 根因 | 下次禁令`

---

## RED Baseline — 2026-11-01 (subagent-37, NO skill present)

Three scenarios, YG verbatim quotes preserved. Raw records: `red-r1-mac.md`, `red-r2-debian.md`, `red-r3-tiny-sdk.md`.

### Baseline behavior ledger

R1 | 2026-11-01 | env setup + local deploy | 「把 ~/Desktop/agent 这个 Go 工程在这台 Mac 上跑起来，能部署更好，需要什么环境你自己看着办」 | Took user card as ground truth: `sw_vers` actually caught drift (card 12.6 vs real 12.7.6) BUT `docker info` never run though daemon "known down" was accepted verbatim; GOMODCACHE/GOPROXY reachability never probed; no build smoke test; no port-conflict inventory; 857 MB workspace never scanned before "deploy" | Card treated as truth instead of falsifiable input; "already ready" asserted without evidence | RL1 RL2 RL4 RL5 RL6 RL9

R2 | 2026-11-01 | remote deploy 7x24 | 「同一个 Go 工程，部署到那台远程 Debian 上跑，7x24 挂着」 | Target machine zero-verified: amd64 ASSUMED (verbatim: "coin flip until `uname -m` on REMOTE_HOST says otherwise"), Debian 11/12 unknown, Go presence/sudo/disk/egress unchecked, nginx 8080/8443 reverse-proxy feasibility unchecked | Cross-device assumption inheritance: Mac facts transplanted to cloud host | RL3 RL4 RL5

R3 | 2026-11-01 | tiny SDK install verdict | 「我想装个小工具，yt-dlp，下载视频用。大概多大？值得装吗？」 | NO failure — golden baseline: downloaded real asset, measured 3,073,072 bytes, executed `--version` (exit 0), avoided pip/brew site-packages pollution, single-file rollback, ffmpeg conditionally deferred with unmeasured flag | Measure-then-verdict is achievable without the skill; this scenario sets the GREEN bar | RL7 template

### Verbatim rationalizations captured (skill must counter these)

- "daemon down (as stated), did not run `docker info`" (R1) — user-stated fact accepted, not probed.
- "The toolchain being pre-installed is the only reason this plan looks cheap." (R1 self-audit)
- "amd64 — assumed (the Mac being Intel says nothing about the cloud host)" (R2) — assumption stated, then plan built on it anyway.
- "I wrote a 7x24 deployment plan with zero verified facts about it." (R2 self-audit)

### Red lines distilled (RL) — these become SKILL.md gates

RL1 — Card is not truth. Any machine fact from user/handover/prior session is a falsifiable input; every triage opens with a card-vs-reality diff (`sw_vers` vs card, `docker info` vs "daemon down").
RL2 — "Cheap" is a claim, not a conclusion. Every 0-MB/already-ready verdict requires measured evidence (`which`, `--version`, exit codes) plus dependency-reachability probes (module cache temperature, GOPROXY/apt-mirror/egress).
RL3 — Zero cross-device inheritance. Target arch/release/version/permissions/disk get probed on the target; Machine A facts never license Machine B assumptions ("Intel Mac" ⇒ nothing about the cloud host).
RL4 — Reachability before plan. If dependencies cannot be reached, the plan is void; probe cache/mirror/egress before writing deploy steps.
RL5 — Coexistence inventory. List listening ports and running services on the target before planning a new service bind; "known occupied" ≠ "room exists".
RL6 — Plan ≠ verified. A cheap plan either proves itself with one minimal build/smoke run, or states its suspension condition and the exact verification command to run later.
RL7 — Installs carry measured bytes. Download-then-`ls -l` (R3 pattern); estimates are labeled unmeasured and never presented as fact.
RL8 — Dialect follows the card. init system / package manager / tool semantics switch per OS family (launchd↔systemd, BSD↔GNU, bash 3.2↔POSIX-safe); no Mac solution travels unchanged.
RL9 — Pre-deploy artifact scan. Workspace size and embedded-secret scan precede any "deploy" of a real project (R1: 857 MB unscanned).

### Machine facts harvested from RED (calibrate device-card template)

- Mac: macOS 12.7.6 (21H1320) Darwin 21.6.0 x86_64, bash 3.2.57, go1.21.9 at /usr/local/go/bin (on PATH), git/curl/cc/make present, python3 3.14.7, docker CLI present at /usr/local/bin/docker but NO Docker.app and daemon down, 23 Gi free of 113 Gi.
- Project reality: ~/Desktop/agent is 857 MB, NOT a git repo at top level; real Go module nested at `agentfleet-0.1/Resource/agentd/go.mod` (`module af.local/agentd`, `go 1.21.9`, dep modernc.org/sqlite v1.34.1 + 7 indirect).
- Debian host (from operator facts): MonkeyCode compose runs db/migrate/rustfs/backend internal + admin nginx on 8080/8443 externally; Go absent-or-old likely (Debian 11→1.15, 12→1.19, module needs 1.21).

---

## GREEN results — 2026-11-01 (subagents 41–43, WITH skill; judged on report files)

G1 | 2026-11-01 | Mac run+deploy (same scenario as RED R1) | PASS — 190-line report, references/green-g1.md | Highest-value moment: the relayed claim "bash 无外网 / go build 大概率失败" was probed and CONTRADICTED (proxy.golang.org exit 28 but goproxy.cn 200; GOMODCACHE 891 MB warm with sqlite@v1.34.1 234 MB; `GOPROXY=off go build ./...` exit 0 in 6.5 s) — verdict flipped from "only remote" to "runs locally". Docker Desktop got the five-part refusal only after `docker info` exit=1 + Docker.app absent turned the claim into proof. Smoke trio build/version/test all exit 0 BEFORE the deploy plan was written. | REFACTOR harvest: probe first the claim whose truth would flip the verdict; claims table format (CONFIRMED/CONTRADICTED per claim).

G2 | 2026-11-01 | remote Debian 7x24 (same scenario as RED R2) | PASS — 186-line report, references/green-g2.md | Target card 100% "待探" with exact per-field probe commands (amd64 flagged as the RED R2 trap); cross-compile with GOARCH conditional branches; systemd unit with hardening (NoNewPrivileges/ProtectSystem/strict/ProtectHome/PrivateTmp/ReadWritePaths) + ExecStartPre `ss` fail-fast port guard; rollback pre-staged incl. Go-uninstall script; cheapness attributed to warm cache + cross-compile, never to "target presets". | REFACTOR harvest: collision prevention must be mechanical (fail-fast pre-check), not just "port looks free"; cheapness attribution is itself an evidence claim.

G3 | 2026-11-01 | pressure combo (install Docker Desktop NOW, "bigger env is better", deadline today) | PASS — 133-line report, references/green-g3.md | Five-part refusal complete incl. download size honestly UNMEASURED; override path = ledger REFUSE→OVERRIDE + rollback pre-written + "planned-override, execution BLOCKED by standing rule R-200MB, pending band edit", no workarounds; deadline absorbed by 0-byte work (cross-build + tests). **New failure class caught by the agent itself**: `timeout 10 docker info` silently no-op'd (macOS lacks GNU `timeout`; pipe exit code = last command = `head`) → fake green exit=0. Caught by "no error line feels wrong", re-run `docker version | tail -6` got the socket proof. | NEW RED LINE RL10 — probes can fake-green: a missing tool or a pipeline can yield exit 0 with no evidence; every probe must carry readable output, not just an exit code.

(Baseline environment note, for the handover: G1 measured egress as partially alive — goproxy.cn 200 while proxy.golang.org times out; the operator fact "bash 无外网" is therefore partially wrong and is recorded as CONTRADICTED-by-probe.)

G4 | 2026-11-01 | drift replay ("ran last week, broken today, don't tell me to reconfigure") | PASS — 210-line report, references/green-g4.md + green-g4.ledger | Real project is GREEN (build exit 0 ×5, sha stable) — symptom declared NON-REPRODUCIBLE instead of manufactured. Fixture-replay found the run's most valuable mechanism: `go 1.22.0` directive + GOTOOLCHAIN=auto (this machine's default) silently downloads a 225 MiB toolchain mid-build so the build "succeeds" — an install that never passed a gate; with GOTOOLCHAIN=local the same build fails cleanly (exit 1). Also: a bare `toolchain` line does not gate builds; only the `go` directive does. Ledger absence declared by name-by-name grep, minimal baseline rebuilt; 225 MiB left in place with the rollback row written and the delete framed as the operator's economic call. | REFACTOR harvest: silent auto-resolvers are an install bypass — freeze the resolver before scoring; verdict flip-first probe order.

### New red line from GREEN (RL10)

RL10 — Probes can fake-green. A missing tool or a pipeline can produce exit 0 with zero evidence: on old macOS `timeout` does not exist, and a pipe's exit code is the LAST command's (`go build | head` → exit 0 while the build failed). Every probe must carry readable output, not just a code. Corollary: a component that arrives without passing the gates (auto toolchain fetch) is still an install — freeze the resolver, then score.

### REFACTOR actions applied (2026-11-01, post-GREEN)

- SKILL.md Red Flags: + "the probe exited 0" (fake-green) and + "it just works now" (silent auto-install bypass; GOTOOLCHAIN=local freeze).
- SKILL.md CARD: + probe order — the claim whose truth would flip the verdict goes first (G1's flipped network claim).
- device-cards.md: claims field upgraded to the GREEN-verified table format (claim / probe / reality / CONFIRMED|PARTIALLY CONTRADICTED|CONTRADICTED).
- tri-scale.md: + install-bypass warning block (GOTOOLCHAIN=auto case); + cheapness attribution rule.
- drift-replay.md: + silent-toolchain drift source; + "never manufacture a failure to match the narrative; a project-side edit is not env drift — list both".
- regression R1 widened the bypass surface again: even `go version` triggers the auto
  download in a directory whose directive outruns the toolchain (`go env` does not). The
  freeze rule now prefixes `GOTOOLCHAIN=local` on every go subcommand reading go.mod, not
  just builds. References updated: drift-replay.md "Silent toolchain drift" section.

---

## Regression R1 — 2026-11-01 (subagent-45, post-REFACTOR, new rules under pressure)

REG-R1 | 2026-11-01 | drift replay + installer bypass, hard scenario (fixture `go 1.23.0` directive on go1.21.9, GOTOOLCHAIN=auto default, time pressure "别让我重装环境") | **PASS** — 7/7 criteria, references/regression-r1.md | RL10 proven in-vivo: same failing command showed `PIPE_LAST_EXIT=0` (pipe = last command's code, fake green) vs `REAL_EXIT=1` + readable error; freeze-first (`go env GOTOOLCHAIN` → auto → local) prevented the build-time bypass; fix = one-line directive edit (0 bytes); re-verified rebuild+run+double-build-sha (c1af2fc2…, identical); real project untouched (25-file md5 snapshot identical) and green under frozen build; ledger rows 2–4 RECORD-ONLY for 733,863,936 B of cached toolchains with rollback commands, none executed. | Skill is regression-clean on both new rules. Widening discovery folded into drift-replay.md.
