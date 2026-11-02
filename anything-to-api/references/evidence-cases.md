# Evidence Cases — anything-to-api (research hub)

Research date: 2026-11-01. Two lanes, both practice-first (skill-from-masters):
(A) downloader ecosystems & mining practice — full detail in `evidence-mining-practice.md`
(265 lines, every URL fetched); (B) site-family API maps + measured egress — full detail in
`evidence-site-families.md` (111 lines). Channels on this machine: HN Algolia API +
GitHub contents/search API via `web_fetch`; `web_search` unavailable; `raw.githubusercontent`
TLS-fails (rc=35) → contents API with base64; `bash curl` for all host probes.

## The measured egress map (this Mac, 2026-11-01) — the pipeline's hard boundary

Reachable, low latency: bilibili 200 (0.16 s) · api.bilibili 301 · douyin 200 (0.18 s) ·
kuaishou 200 (0.38 s) · baidu (0.26 s) · bing (0.39 s) · goproxy.cn (0.12 s) ·
api.github.com (0.26 s) · hn.algolia (1.2 s). Slow but alive: v.douyin 302 (2.7 s) ·
registry.npmjs.org (1.9 s) · github.com (8 s boundary). Dead: **google.com and youtube.com,
000 after 5 s timeout — egress-layer blocked, not rate-limited** · raw.githubusercontent
(TLS fail). Consequence for the skill: HTTP-layer mining covers every CN video site locally;
Google/YouTube-family intents offload to the remote Debian host before any design.

## Golden cases

| # | Case | The move that works | Source |
|---|---|---|---|
| G1 | yt-dlp extractor as the compressed practice | per-site extractor source = the sign flow in code; yt-dlp's own `CURRENTLY BROKEN` tags mark dead extractors; 1755 extractors on this machine | github.com/yt-dlp/yt-dlp (README + `bilibili.py` full source, fetched) |
| G2 | gallery-dl | config-driven, 20k★, supported-sites list in repo — the zero-code coverage lane | github.com/mikfd/gallery-dl |
| G3 | cobalt-class self-host API | save-from-URL as a first-class API design (media POST → task → download), 45.7k★ AGPL | cobalt repo + HN |
| G4 | `Evil0ctal/Douyin_TikTok_Download_API` | self-hosted Douyin/TikTok/B站 data + no-watermark download API with MCP + Docker — the Douyin-family de-facto stack (21,050★, pushed this session's date) | github.com/Evil0ctal/Douyin_TikTok_Download_API |
| G5 | `putyy/res-downloader` | multi-family unified assembler: 视频号/小程序/抖音/快手/小红书/m3u8/酷狗/QQ音乐 (21,021★) | github.com/putyy/res-downloader |
| G6 | the browser-first insight | "a video that plays in the browser has its m3u8 in the Network panel; login walls block downloaders, not observation" — practitioner's own words in the GDCVault issue thread | HN/issue threads cited in `evidence-mining-practice.md` |

## Sign/anti-bot mechanisms (public sources)

- **B 站 WBI**: nav endpoint → mixin-key reorder table → md5(query + w_rid); three independent
  implementations found. **Both semi-official doc repos died in 2026-01/07 by lawyer letter**
  (SocialSisterYi/bilibili-API-collect 21k★, Nemo2011/bilibili-api 4.1k★ — READMEs now only
  deprecation notices) — local archives only.
- **抖音 a_bogus / X-Bogus**: pure-Python sign implementations in GitHub repos (Evil0ctal);
  version drift between X-Bogus and a_bogus is real — batch-try current+previous patterns.
- **m3u8/HLS**: two public paths — referer+cookie stitching, and stream decrypt (HN 42-pt +
  58-pt threads).
- **Cookie injection**: Playwright official auth docs (storageState files contain sensitive
  cookies; injection flow documented) — the login-wall branch's reference.
- **JSON replay**: any observed XHR replayed with lifted params (`json-server` shows the
  static-JSON-as-API baseline pattern).

## Failure modes (each with a real source in the lane files)

F1 search-rank "downloader sites" that cannot download · F2 fake APIs that force a browser
extension then still serve ads · F3 app-store paid "clean" tiers (pure extraction: the free
ecosystem proves watermark-stripping is baseline, 500+ free repos) · F4 silent API changes
(yt-dlp's broken-extractor issues run in the hundreds; Whyp.it/XHS redesign; Boosty extractor
perma-failed without notice) · F5 wind-control: 412/-352 codes, PO Token, JS-runtime
requirements (yt-dlp issue #15012: "external JavaScript runtime now required for full YouTube
support"), IP reputation · F6 login walls on free content · F7 watermark re-charging · F8
malicious CLI/packages in the downloader supply chain (npm malicious-package reporting) ·
F9 DMCA/legal-letter takedowns of doc repos (B 站 both, 2026).

## The three conclusions the skill is built on

- **C1 — Claimed ≠ called, at every layer.** Extractor-listed is the program's claim; a
  `--simulate` is your test; a real-target byte run is the only proof. The RED baseline nearly
  shipped "yt-dlp 能下抖音" with zero execution — the rung ladder exists because of that
  sentence.
- **C2 — The first worksite is the browser's Network panel.** Login walls and player JS gate
  the downloader, not the observer; mining starts at observation, then reads the implementers'
  extractor source, then lifts the sign, then replays.
- **C3 — Nothing cited survives on trust.** Doc repos get lawyer-lettered, URLs rot, hosts die
  per-machine. Every claim carries a probe date; every reuse re-probes; the library is the only
  memory that exists.

## Gaps — explicitly unsourced

bilibili-API-collect doc content (repo dead at fetch time) · gallery-dl's `--list-extractors`
wording (README 0 hits — supportedsites.md used instead) · Chrome DevTools official docs
(three fetch attempts failed) · `JoeanAmery/TikTokDownloader` (404 — the rot example itself) ·
no official Douyin sign documentation (reverse-engineering repos only) · you-get/gallery-dl
source dissection · PO Token (bgutil) mechanism · cobalt OpenAPI surface. Rests on HN-index
records where marked; nothing above is asserted beyond what was seen.
