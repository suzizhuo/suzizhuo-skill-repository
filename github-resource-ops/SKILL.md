---
name: github-resource-ops
description: Use when hunting, landing, or feeding GitHub resources — finding repos/tools/cracks/keygens/exploits/datasets/books/docker files/dependencies when the original repo is dead or takedown'd, choosing between local old Intel Mac and Monkey Code container destinations, screening downloads before running, snapshotting repos (shallow/mirror/archive), or packaging a repo for an AI agent to read/modify/run. Covers search recipes, fallback ladder, destination matrix + manifest, integrity screening, and read/modify/run briefing.
---

# GitHub Resource Ops

## Overview

GitHub is his resource warehouse: 找得到、复制得走、喂得动 AI。This skill is the fixed pipeline — FIND (猎取) → TAKE (落地) → FEED (投喂) — applied to every resource request, any category, any destination. Principle: 无凭据不落地，无筛查不运行。

## When to Use

- Any "GitHub 上找个…" request: tools, cracks/keygens, JAR/deps, exploits/security tools, datasets/models, books/tutorials, docker files (game-cheat / Magisk / kernel categories are PARKED: keep the recipe slot, no build until he says so)
- Original repo dead/404/taken down → fallback ladder required
- Deciding where a resource lands: MAC_LOCAL / MONKEY_CONTAINER / GitHub-reference-only
- About to run downloaded code, or hand a repo to an AI agent

## Phase 1 — FIND (query engineering, not "搜一下")

Always produce (a) executable queries, (b) ranked candidates with health signals, (c) the fallback ladder position. Reference: `references/search-recipes.md`.

- Qualifiers first: `in:name,description,readme`, `stars:>=n`, `pushed:>YYYY-MM-DD` (freshness), `archived:false`, `language:`, `topic:`, `fork:true/fork:only` (mirror hunting), `mirror:true`, `size:` — official table in references.
- Commands: `gh search repos/code/prs/issues/commits --json …`, `gh api` for machine filtering; probe connectivity first (`gh auth status`, `curl -sI https://api.github.com`); no network → web search + official mirror sources, and SAY which channel you used.
- **Fallback ladder** (dead repo is never a dead end): original → forks ranked by `pushed:>` (fork:only fork:true) → mirrors (`mirror:true`) → gists (`gh gist clone`) → archive.org snapshot of the repo/tag → other mirrors (Codeberg/GitLab/SourceForge/Bitbucket placeholder hosts).
- Category recipes (seeds, asset types, arch notes): `references/resource-types.md`.

## Phase 2 — TAKE (destination + manifest + screening)

No download without the three outputs. Reference: `references/take-and-route.md`.

1. **Destination matrix** — one line stating MAC_LOCAL / MONKEY_CONTAINER / reference-only, with the reason. Unknown destination → ask, never assume. Old Mac is the LAST resort for executing anything; container is the default sandbox.
2. **Manifest** — before/at download: path, repo URL, commit SHA or tag, arch, purpose, sha256 of assets. Storage tree: `~/gh-res/<category>/<owner>__<repo>/`. (His stated pain: "下载完就找不到" — manifest is the cure.)
3. **Screening checklist** (executable, all before first run): identity (is it a fork/typosquat of a known repo? star history sane?); text sweep (`curl … | bash`, `eval(`, `base64 -d`, `wget` to unknown hosts, binary blobs in a source repo); asset arch match (`uname -m`: darwin_amd64 vs linux arm64/amd64); checksum verify; unknown executables run ONLY in the container, never on the local Mac.

## Phase 3 — FEED (read / modify / run)

**REQUIRED SUB-SKILL:** Before an AI touches the repo, run the briefing-ai-agents ritual (goal + constraints + DoD + evidence first). Reference: `references/feed-modes.md`.

- **READ** — 10-minute anatomy: tree depth-2, build files, entry points, README claims vs code reality, dependency inventory → one-page map.
- **MODIFY** — fork + keep `upstream` remote + minimal diff (caps from briefing skill); never push to upstream.
- **RUN** — env card first (arch/OS/network of the ACTUAL target), dry-run, evidence log to declared RESULT_DIR. "跑通就行" is banned: version output + one real run log, else UNKNOWN.

## Red Flags — STOP

| Excuse | Reality |
|---|---|
| "我会搜一下" | Executable queries with qualifiers or nothing |
| "搜到了，能跑" | Screening checklist first; unknown exe → container only |
| "Repo 404，没辙" | Ladder: forks → mirrors → gists → archive.org → other hosts |
| "先下载再说" | No manifest, no download — path/SHA/arch/purpose/checksum |
| "目标肯定是内网吧" | 假设入账；无凭据的假设不触发任何破坏性动作 |
| "跑通就行" | 版本号输出 + 真实运行日志，否则 UNKNOWN |
| "本地那份一起装" | 老 Mac 是执行链最后一环，不是默认环节 |

## Evidence Base

| Move | Source |
|---|---|
| Repo search qualifier table (in:/stars/pushed/fork:only/mirror/archived/topic/size) | docs.github.com/en/search-github/searching-on-github/searching-for-repositories |
| gh CLI command inventory (search, repo clone/sync/fork, release download/verify, api, gist) | cli.github.com/manual/ |

Baseline note: a no-skill agent handled a narrow single-tool request well; this skill exists for breadth (all categories), persistence (manifest), executable screening, briefing discipline, and honest assumptions — the four places the baseline stayed silent.
