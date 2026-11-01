# Evidence Cases — device-env-triage (research hub)

Research date: 2026-11-01. Two lanes, both practice-first (skill-from-masters):
(A) reproducibility methodology — full detail in `evidence-methodology.md` (598 lines, 49 URLs);
(B) three-OS-family probe/size/failure facts — full detail in `evidence-three-oses.md` (68 lines, 29 URLs).

Method note: `web_search` unavailable on this machine; retrieval via `web_fetch` + HN Algolia API +
GitHub contents API. Every URL below was fetched and seen during the session; items not verified
are marked `[unsourced]`. `[hn-index-only]` = HN record seen, article body not opened.

## Retrieval channels that worked

| Channel | Endpoint | Status |
|---|---|---|
| HN Algolia | `https://hn.algolia.com/api/v1/search?query=…&tags=story\|comment` | working — highest yield |
| GitHub contents/search API | `https://api.github.com/repos/<org>/<repo>/contents/<path>?ref=main` | working |
| Direct fetch | `web_fetch <url>` | intermittent; `raw.githubusercontent.com` and hub.docker.com failed |

## Golden cases

| # | Case | The move that works | Source |
|---|---|---|---|
| G1 | `jdx/mise` | commit `mise.toml` (request) **and** `mise.lock` (exact version + checksums) — next machine byte-identical; reads idiomatic `.nvmrc`/`.python-version` instead of inventing formats | api.github.com/repos/jdx/mise/contents/README.md?ref=main (fetched, base64-decoded); Ask HN "What SDK/Version manager do you use?" 2025-03-14 |
| G2 | `cachix/devenv` | one declarative file owns tools + env + tasks + **process manager** (ordering, restart, readiness, socket activation) + secretSpec (runtime-resolved, never in config) | api.github.com/repos/cachix/devenv/contents/README.md?ref=main (fetched); HN 33655542, 382 pts / 158 comments |
| G3 | Replit Nix at fleet scale | reproducibility industrialized at fleet scale; honest counter-case: jade.fyi "Flakes aren't real" (60 pts/81 comments) — the frontend is contested, the content-addressed closure is the durable half | [hn-index-only] |
| G4 | "Works on my machine" retrospectives | four real postmortems: container wall (217 pts/239 comments), hardcoded secrets + deploy-is-secret, Vagrant drift, hermetic builds | HN 36885598 + others; see methodology §G4 |
| G5 | RED baseline R3 (this skill's own) | download the real artifact → `ls -l` the bytes (3,073,072) → run `--version` exit 0 → single-file rollback → conditional extras deferred with a trigger | references/red-r3-tiny-sdk.md |

## Failure modes

Methodology lane (F1–F9, each with a real source in `evidence-methodology.md` §3):
F1 environment drift (snowflake host) · F2 floating versions incl. the LiteLLM-via-`dspy` transitive
break · F3 image bloat (8 GB image as a shipped "feature") · F4 no rehearsed reverse (Anthropic
postmortem: "neither our internal usage nor evals initially reproduced") · F5 arch/host assumptions
(arm64, CUDA, uid/gid) · F6 secrets baked into config/layers · F7 daemon-in-container / assume-the-host-runs-it ·
F8 registry gating (internal-registry credentials) · F9 parallel-environment collisions (ports, paths, uids).

Three-family lane (numbered, one URL each — full text in `evidence-three-oses.md` §Failure modes):

| # | Family | Failure | Key source |
|---|---|---|---|
| 1 | macOS | x86_64 binary on Apple Silicon → `bad CPU type in executable`; author fine on Windows, broke on Mac — the assumption-inheritance disease | news.ycombinator.com/item?id=50207196 |
| 2 | macOS | same via Homebrew Cellar; earlier on Catalina | news.ycombinator.com/item?id=42879613, id=21186260 |
| 3 | macOS | old-Intel ceiling: Monterey dropped from official support; OCLP unofficial extension (205 pts) | news.ycombinator.com/item?id=35538223; id=31823892; OCLP model pages [unsourced] |
| 4 | macOS | APFS full → even `rm` fails with no space left; reboot/recovery needed; Chrome data wiped | news.ycombinator.com/item?id=49347120 |
| 5 | Windows | winget scope: official "not all installers support user vs machine scope consistently" — the "installed but unfindable" mechanism | learn.microsoft.com/windows/package-manager/winget/troubleshooting#scope-for-specific-user-vs-machine-wide |
| 6 | Windows | install-failure visibility: `winget error 1603`, non-standard exe codes, UA bans | same troubleshooting page |
| 7 | Debian | installed-but-not-running: policy-rc.d in containers blocks service start "and nothing starts the service afterwards or says how" | github.com/williamsharkey/tabcomputer/issues/149; QubesOS/qubes-issues#2238; calaos-build PR#1; openvox-ca PR#558 |
| 8 | Debian | install-succeeded ≠ healthy: tailscale on Debian 12 "installation complete", service fatal | github.com/tailscale/tailscale/issues/21707 |
| 9 | Linux | size/disk illusions: /tmp siltation → ENOSPC while `df` half-empty; `Available=0` but Use% stuck at 94.9% misses monitoring | news.ycombinator.com/item?id=49612773; id=5440148; id=49081588 |
| 10 | Cross | "works on my machine → works in my container" (219 pts) — the snowflake moves, it does not melt | news.ycombinator.com/item?id=36885598; id=2068555 |

## The four conclusions the skill is built on

- **C1 — Containerizing relocates the snowflake.** A container is reproducibility evidence only if the image comes from a locked, diffable spec; otherwise it scores like "works on my machine".
- **C2 — Test the artifact the user runs.** Anthropic's first remediation was not more evals but staff using the exact public build. Every INSTALL acceptance exercises the exact artifact.
- **C3 — Reproducibility ≠ lockability; only lockability travels.** Prefer version request + checksummed lockfile as the portable signal; treat flakes/devcontainer/devenv as optional frontends.
- **C4 — Collisions are a first-class axis.** Two reproducible environments still break each other on one machine (ports, uid/gid, orphaned `.venv`) — the axis checklists omit.

## Hard mechanism findings (family lane)

- `apt-get --print-uris install PKG` prints each URI with size + MD5, no download — the only doc-verified pre-install size mechanism across the three families (manpages.debian.org bookworm apt 2.6.1).
- systemd official wording: "Enabling units should not be confused with starting (activating) units" — enable ≠ start; probe both (`systemctl is-enabled/is-active/status`). freedesktop.org systemd man (fetched).
- Homebrew formula JSON has bottle URLs + sha256 per OS×arch but **no size field** [unsourced via this API]; winget documents no size field [unsourced] → both fall back to download-then-measure.
- `dpkg-query -W -f='${Installed-Size}'` field exists (dpkg 1.21.22 man, fetched) but the unit is not stated → [unsourced unit], calibrate on target.
- macOS `uname -m` lies under Rosetta (translated x86_64 process reports x86_64); the cross-check MIB name is [unsourced] — verify on target.
- lsb_release man page path moved (bookworm `lsb-base` 404 → trixie `lsb-release`); on old Debian, check which package provides it.

## Gaps — explicitly unsourced (do not quote as fact)

Docker official docs and a primary devcontainer.json sample (fetch failures) · NixOS/templates flake ·
the "56% of time on environment setup" vendor claim (no cited study) · quantified cost-of-drift ·
brew/winget bottle or package pre-install sizes · `du`/Rosetta-MIB/powershell-recursive-sum man pages ·
Chinese-language primary sources (no channel) · mise/asdf/direnv benchmark · all `[hn-index-only]` items.
