# Mining Lanes — the parallel fan-out

Three lanes, dispatched together as subagents (dispatch via the `briefing-ai-agents` protocol:
intent, constraint card, probe budget, return shape = candidates with evidence). A lane that
finds nothing returns "nothing found" + its probe outputs. Narration is not a lane result.

## Lane A — known-coverage sweep (zero-search fast path)

| Probe | What it proves | What it does NOT prove |
|---|---|---|
| `yt-dlp --list-extractors \| grep -iE '<family>'` | the extractor name exists; yt-dlp's own `CURRENTLY BROKEN` tags flag dead ones | that the extractor works today |
| locate the extractor source (`yt-dlp --list-extractors` verbose, or repo grep: GREEN G1 found Douyin living inside `tiktok.py:1371`) | where the sign flow actually lives — read it before re-deriving | that the flow still matches the live site |
| gallery-dl supported-sites list (`supportedsites.md` in repo; the `--list-extractors` wording is [unsourced] for gallery-dl) | site family is claimed supported | anything about this site's current state |
| GitHub search `"<site> download API"` / `"<site> parser"` | active repos exist (check last push date) | repo works on this machine |
| MCP registries / awesome lists | an ecosystem entry exists | callability |

Known repos observed alive (probe before use — the ecosystem deletes its own citations):
`Evil0ctal/Douyin_TikTok_Download_API` (21,050★, TikTok/Douyin/B站, no-watermark, MCP, Docker),
`putyy/res-downloader` (21,021★, multi-family incl. m3u8), `cv-cat/KuaiShou-Spider`,
`JoeanAmier/KS-Downloader`, `Tommy131/KuaishouParser`, `mikfd/gallery-dl`, cobalt-class
self-host APIs.

## Lane B — engine sweep

- Engines that work from this Mac: bing.com, baidu.com. Google/YouTube: egress-dead → offload.
- **Follow the redirect before judging an engine dead**: bing.com answers 302 top-level and
  lands on cn.bing.com (200, ~0.74 s) — probe with `curl -L` or record the 302, never
  "302 → dead".
- **Check the body, not just the code**: baidu answered 200 with a 522-byte anti-crawl shell in
  GREEN G1 — a 200 that small is a wall, not results. Real content is KB-scale; record the
  byte count alongside the code.
- Queries come from the intent card's query-set; candidates get (URL + probe plan).
- Downloader-site red flags (all documented failure classes, evidence F2/F3/F7): gates behind
  a browser extension, paid "clean/no-watermark" tier, no visible API docs, pop-under ads,
  asks to install an "accelerator". These are probed as skeptically as anything else, and a
  probe that only resolves DNS does not make them a route.
- Self-host API repos found here go to Lane A-style checks (last push, deps, run cost) before
  any suggestion to run them.

## Lane C — target-site mining (the first worksite is the browser)

The counter-intuitive core (GDCVault issue, HN-verified): **login walls block downloaders, not
observation**. A playable video exposes its stream in the browser's Network panel. So the mining
order for a target site is:

1. **Observe**: open the item in any browser (headless on the remote host when local has none),
   record Network: m3u8 manifests, JSON XHR endpoints, player config, response headers
   (referer/cookie requirements).
2. **Read the implementers**: the site's active extractor source (yt-dlp/gallery-dl extractor
   for the family) — e.g. yt-dlp's `bilibili.py` shows the full WBI flow: nav endpoint →
   mixin-key table reorder → md5(query+w_rid). Extractor source is the compressed practice.
3. **Lift the sign**: WBI-family (md5 over reordered query), a_bogus/X-Bogus family (Douyin;
   version drift is real — batch-try current+previous patterns), timestamp/nonce nonces.
4. **Replay with HTTP**: curl/stdlib with lifted params, storageState cookie injection when
   needed (Playwright docs pattern: cookie file into browser context, or header replay).
5. **m3u8 branch**: referer+cookie stitching and/or HLS decrypt paths (two documented public
   patterns) → segment pull → remux.

## Escalation ladder

HTTP probe → yt-dlp engine (`--simulate` first, then `-F`, then real) → signed replay →
headless browser (remote Debian first: full egress, zero local browser) → genuine login wall
= user-in-the-loop. Each rung costs a probe; escalation is not free, so HTTP-first unless the
target is JS-gated.

## Lane dispatch contract

Each lane subagent gets: the normalized intent card, a probe budget (default ≤20 actions),
"candidates with evidence" as the only return shape, and the per-host probe discipline
(one `curl -m 5` per host with its own exit code — never one command for many hosts).
