---
name: anything-to-api
description: Use when a user wants a capability or a download that no tool on PATH provides — "找个能下 X 的" / "这个站的视频怎么批量下" / "去水印" / "有没有现成 API 能替我干" (抖音 / 快手 / B站 / YouTube 下载 / 找接口 / 找 API / 万物皆可 API) — or when a previously working API or download path needs re-verification after a suspected site change.
---

# Anything to API

## Overview

Any user intent becomes a verified working API path — or an honest verdict. The product is never "I searched": it is a called endpoint that produced real bytes, recorded in a card that survives the site's next redesign. Three laws carry this skill: **a claimed API is executed before it is claimed**; **the search is parallel and engine-first**; **every working path is persisted and re-verified on reuse**. Scope gate per operator rule: public + free routes, legality-agnostic; cracked/bypass paths are in scope when they deliver the artifact.

The unit of work is one *lane result*: a candidate route with its probe evidence attached — never narration about what might work.

## When to Use

- A download/media/file intent the current tools can't do (找下载器 / 怎么下 / 去水印 / 批量)
- "有没有现成 API/在线服务能替我干 X" — convert, compress, recognize, parse, fetch, translate
- A previously working path is suspected broken (改版 / 失效 / 风控)
- Not for: one-shot facts a `web_fetch` answers, or tasks a tool already on PATH does (then just do it — and write a one-line card)

## The Pipeline — per intent, no stage skipped

### 1. Normalize the intent (references/intent-normalize.md)

Turn the ask into: target site(s) · media type · quality floor · watermark stance · batch size · login/cookie state · **a live target** (real URL or resolvable handle). A placeholder URL is not a target — resolving one (search, ranking, handle lookup) is the pipeline's job, not the user's. Output: the constraint card + the query set (the operator's own "抖音 快手 哔哩哔哩下载器" is a legitimate query seed).

### 2. Parallel lanes (references/mining-lanes.md)

Dispatch three lanes **together** (subagents, per the `briefing-ai-agents` protocol: intent, constraints, probe budget, candidates-with-evidence as the return shape):

| Lane | Mines | Canonical first probes |
|---|---|---|
| A — known coverage | yt-dlp `--list-extractors` (grep family; note yt-dlp's own `CURRENTLY BROKEN` tags), gallery-dl supported sites, GitHub `"<site> download API"` search, MCP registries | zero-search fast path |
| B — engine sweep | Bing/Baidu for the query set (reachable from this Mac: baidu 255 ms, bing 391 ms; **Google/YouTube-family are egress-dead here → offload or proxy, never assume**) | downloader sites, self-host APIs |
| C — target-site mining | page JS → XHR/JSON endpoints → sign params (WBI / a_bogus families), cookie needs, m3u8 manifests; **the first worksite is the browser's Network panel, not the source** — login walls block downloaders, not observation | extractor source dissection, storageState cookie injection |

A lane that found nothing returns "nothing found" + its probe outputs. Silence is not a lane
result. A lane may be **skipped only with a declared reason recorded in the report** (e.g. GREEN
G4's budgeted single-bash lane consolidation) — an undeclared skip is a failure.

### 3. Escalation ladder

HTTP (curl/stdlib, per-host probe with its own exit code) → yt-dlp engine (`--simulate` / `-F` before any real pull) → signed replay (lift the sign from extractor source or page JS) → headless browser (remote Debian preferred: full egress, no browser on this Mac) → user-in-the-loop only at a genuine login wall. Route bypass claims get verified for what they ARE: goproxy.cn answered 200 in a RED run and is still a Go module proxy, not an HTTP proxy.

### 4. Verification ladder — every claimed API clears all rungs (references/interface-cards.md)

1. Reachable: per-host probe, own exit code (DNS/TCP/HTTP).
2. Placeholder call returns a well-formed response.
3. **Real target → real bytes**, checked (size, type, playability, is-it-the-requested-item).
4. Second call reproduces.

Rung 3 is the line. Before 能用 / "works" appears in any reply, card, or script, rung-3 bytes exist. A name in `--list-extractors` is the program's claim; `--simulate` is your test.

### 5. Verdicts

**WORKING** (rungs 1–4 cleared, card written) · **SIGNED-NEED** (works with a documented sign/cookie recipe, recipe attached) · **FAILED** (dead at rung N, rung + probe output recorded) · **UNVERIFIED** (never leaves the desk unmarked — a handed-over URL earns zero trust beyond its probe; the artifact carries the tag in code comment, command line, and reply, and any script built on it preflights and aborts instead of falling back silently).

### 6. Persist (references/interface-cards.md, references/download-channels.md)

One card per API (endpoint / params / sign / limits / sample call / probe date / verdict). Cards accumulate into the interface library — append-only, cross-session. Re-use means **re-verify first** (probe date stale → re-run the rungs; failure → re-mine the lanes seeded with the card's last-known-good). Every固化 target also gets a one-click channel script.

## Evidence rules that override intuition

1. **The first worksite is the browser, not the code.** A player that plays in the browser exposes its m3u8/JSON in the Network panel; login walls block downloaders, not observation.
2. **The ecosystem deletes its own citations.** B 站's two semi-official API doc repos (21k★ and 4k★) were lawyer-lettered offline in 2026; `JoeanAmver/TikTokDownloader` rotted to 404. Every cited URL is probed before use.
3. **Egress is per-host and per-machine.** This Mac reaches every CN video host in under 3 s and cannot reach Google/YouTube at all. Cards never travel across egress boundaries.

## Quick Reference

| Intent | First move |
|---|---|
| 下抖音视频 | Lane A: `yt-dlp --simulate` on the real URL; Lane B: self-host API (`Evil0ctal/Douyin_TikTok_Download_API`, 21k★); Lane C: a_bogus family |
| 下 B 站 | `yt-dlp <BV号> --simulate` — reproduced this session, exit 0; doc repos dead, use local archives only |
| 下快手 | `KuaiShou-Spider` / `KS-Downloader` family (GitHub, probe the repo first) |
| YouTube / Google 域 | egress-dead locally → OFFLOAD to remote Debian before any design |
| "直接接进来用" | Verification ladder before 能用; UNVERIFIED stays UNVERIFIED |
| "上周那个接口" | Read the card, re-verify, run — never re-derive from narrative |

## Common Mistakes (each anchored in RED or evidence)

- Claiming support from a listing — RED R1's unspoken sentence: "yt-dlp 能下抖音" with zero execution
- Wrapping a dead endpoint in a tidy command — RED R3: fabrication with good formatting
- Single-lane runs — a download intent with zero engine sweep is incomplete
- Treating a placeholder as a target — "首页热门第一支" must be resolved, not deferred
- Reachable = usable — goproxy.cn is a Go proxy; a 200 homepage is not a ranked video
- Citing naked URLs — DMCA'd repos and rotted links are why A-08 exists
- Quality vs identity — "水印无所谓" relaxes quality, never the identity check

## Red Flags — STOP

- "It's in `--list-extractors`" — a listing is not a test
- "The user handed me the endpoint" — probe first, always
- "The downloader site looks official" — search-rank downloader sites are a documented scam class (fake APIs, forced extensions, paid "clean" tiers)
- "Works on my machine" — per-host egress differs; probe on the executing machine
- "I'll verify after wiring it in" — no rung-3 bytes, no 能用, no card-worthy claim
