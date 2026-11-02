# Interface Library — cards live here, append-only

This file is the library. Cards accumulate here; spec files (interface-cards.md etc.) never
receive card appends. History: the first card was appended into interface-cards.md by the
GREEN-G2 run (2026-11-01 23:28) before this convention existed; it was moved here verbatim in
REFACTOR (2026-11-02). New cards append below, never above.

---

## card bilibili-download-001 — appended 2026-11-01 23:28 CST (green-g2 run; fixes R2's A-07 gap)

```
card-id:       bilibili-download-001            verdict:  WORKING
probed-at:     2026-11-01 23:28 CST              probed-on: yin-mac (Darwin 21.6.0, home egress; www.bilibili.com re-probed 200 this session)
endpoint:      engine call — yt-dlp BiliBili extractor (2026.10.26)
               underlying chain observed in -F trace: webpage → wbi sign → playurl (api.bilibili.com/x/player/wbi/playurl)
params:        url with real BV id; -f 30016 (480x360 video-only, ~38.14MiB, no merge needed);
               DASH pairs 100109/100022 (video-only) + 30216/30232 (audio-only, ~16.82/33.01MiB) exist;
               merged pulls need ffmpeg — NOT on this machine's PATH (checked, this session)
sign:          none at operator layer; extractor performs wbi internally
               (nav → mixin-key reorder → md5(query+w_rid)); public video needs no cookie
sample-call:   yt-dlp --no-playlist -f 30016 -o 'BiliBili-%(id)s-30016.%(ext)s' "https://www.bilibili.com/video/BV1xx411c7mD"
rungs:         r1 www.bilibili.com curl -m5 → http=200, time=0.17s, exit=0 (2026-11-01 23:15 CST)
               r2 --simulate exit=0 → "BiliBili | BV1xx411c7mD | dur=2055.637 | 字幕君交流场所";
                  -F exit=0 with output written to a file — reading the exit code through a pipe to head
                  yields SIGPIPE exit-120 (RED R2's unexplained 120 resolved: instrumentation, not site)
               r3 dl1_exit=0 → BiliBili-BV1xx411c7mD-30016.mp4, 39,980,845 B (38.13 MiB),
                  file → "ISO Media, MP4 Base Media v5"; identity = requested BV1xx411c7mD
               r4 second call dl2_exit=0 → sha256 of both artifacts identical
                  0a0a23b5230dca00f8b4a79bc968bef86208fb731112c93f546c1ea17d1a7962 → RUNG4_SHA_MATCH
limits:        30016 is video-only (no ffmpeg → no audio in one file; pull 30216 separately if audio needed);
               known 412/-352 wind-control class for B site (yt-dlp issue history) not observed from home
               egress; single-item verified — batch/long-run untested; card valid for yin-mac only (A-05)
rollback:      rm -f /tmp/bili-dl/*  (artifacts intentionally kept outside the skill tree)
last-known-good: "yt-dlp --simulate" exit 0 + ls -l receipt 39,980,845 B — BV1xx411c7mD, 2026-11-01 23:28 CST
```

Session ledger row (format per interface-cards.md):

```
date | machine card | family | component | footprint (measured) | verdict | reason | rollback | verified-by (rung evidence)
2026-11-01 | yin-mac | bilibili | yt-dlp BiliBili extractor 2026.10.26, format 30016 | 39,980,845 B | WORKING | rungs 1-4 cleared incl. r4 sha match | rm -f /tmp/bili-dl/* | r3 ls -l receipt + r4 sha256 compare
```

---

## card bilibili-download-002 — appended 2026-11-02 CST (regression-r1; supersedes 001, which it re-verifies)

```
card-id:       bilibili-download-002            verdict:  WORKING (supersedes bilibili-download-001)
probed-at:     2026-11-02 02:45–02:52 CST        probed-on: yin-mac (home egress; library read first, 001 stale → rungs re-run, not re-mined)
endpoint:      engine call — yt-dlp BiliBili extractor (2026.10.26)
                https://www.bilibili.com/video/BV1xx411c7mD
params:        url with real BV id; -f 30016 (480x360 video-only, ~38.13MiB, no merge needed);
                ffmpeg ABSENT on yin-mac (re-confirmed this session) → audio needs separate 30216 pull
sign:          none at operator layer; extractor performs wbi internally
sample-call:   yt-dlp --no-playlist -f 30016 -o '/tmp/reg1/BiliBili-%(id)s-30016.%(ext)s' "https://www.bilibili.com/video/BV1xx411c7mD"
rungs:         r1 curl -m5 → http=200, time=0.168s, exit=0
                r2 --simulate exit=0 (BV1xx411c7mD, format 30016); -F exit=0 written to file, exit read direct (no pipe)
                r3 dl exit=0 → 39,980,845 B (38.13 MiB), file=ISO Media MP4 Base Media v5, identity=BV1xx411c7mD
                r4 second pull exit=0 → sha256 identical to r3 AND to card 001's recorded sha
                   0a0a23b5230dca00f8b4a79bc968bef86208fb731112c93f546c1ea17d1a7962 → RUNG4_SHA_MATCH (cross-day)
limits:       video-only single item verified; batch/long-run untested; card valid for yin-mac egress only (A-05);
               known 412/-352 wind-control class not observed from home egress
rollback:     rm -f /tmp/reg1/* (artifacts kept outside skill tree)
last-known-good: ls -l receipt 39,980,845 B + sha256 0a0a23b5…1a7962 — BV1xx411c7mD, 2026-11-02 CST
```

---

## card downloader-example-001 — appended 2026-11-02 CST (regression-r1; FAILED cards are first-class)

```
card-id:       downloader-example-001            verdict:  FAILED (at rung 1 — DNS layer)
probed-at:     2026-11-02 03:00 CST               probed-on: yin-mac (home egress; example.com control probed same minute)
endpoint:      http://downloader.example.com/     (handed-over "上周那个接口" class — probed, not trusted)
params:        none observed — death is pre-request
sign:          n/a (no request ever formed)
sample-call:   curl -s -m 5 -o /dev/null -w '%{http_code}' http://downloader.example.com/   → http=000
rungs:         r1 DNS: dig +short downloader.example.com A → empty answer, status NOERROR (NODATA, no A record);
                   curl exit=6 (could not resolve host)  → DIED AT LAYER: DNS
                   control: dig example.com → 172.66.147.243 / 104.20.23.154; curl example.com → 200, 577 B,
                   exit=0 → parent domain healthy, network/TCP/HTTP layers NOT implicated
                r2/r3/r4 not reached (r1 failure aborts the ladder)
limits:        death is per-subdomain at DNS layer; do not spend TCP/TLS/HTTP probes on it unless the
               zone record appears; if the host ever gains an A record, re-probe from rung 1 with its own exit code
rollback:      nothing to undo (no artifact, no config touched)
last-known-good: none — no successful call ever recorded; control evidence only (example.com 200/577 B)
```

---

## card bilibili-app-frontend-001 — appended 2026-11-02 CST (regression-r1; lane-B sweep result, UNVERIFIED travels)

```
card-id:       bilibili-app-frontend-001         verdict:  UNVERIFIED (rung 1 only — no download capability tested)
probed-at:     2026-11-02 03:06 CST               probed-on: yin-mac (home egress; bing 302→cn.bing.com followed per A-11)
endpoint:      https://app.bilibili.com/  (200, 25,569 B)  +  https://big.bilibili.com/pc/video  (200, 2,225 B)
params:        unknown — no player/API call exercised; these are site front-ends, not proved downloaders
sign:          unknown
sample-call:   curl -sL -m 8 -o /dev/null -w '%{http_code} %{size_download}' https://app.bilibili.com/
rungs:         r1 per-candidate probes, each own exit code: app.bilibili.com → 200/25,569 B exit=0;
                   big.bilibili.com/pc/video → 200/2,225 B exit=0; (sweep junk: cncc.bingj.com 400,
                   outlook.live.com 417, beian.miit.gov.cn 521; two regex-truncated pseudo-hosts → exit 6)
                r2 placeholder call: NOT RUN — no documented endpoint identified from these hosts
                r3/r4: not reached
limits:        bing query evidence: 200, 1 redirect, cn.bing.com/search, 100,981 B body — sweep produced
               NO new third-party download route; only these two bilibili-owned front-ends cleared rung 1
rollback:      none; any future script built on this card MUST preflight-abort on rung-1 failure,
               never silently fall back to a second unverified route
last-known-good: none recorded (rung-1 reachability only); backup-route task closed UNVERIFIED —
               primary route remains bilibili-download-002 (rung-4 verified)
```

Session ledger rows (regression-r1, 2026-11-02):

```
date | machine card | family | component | footprint (measured) | verdict | reason | rollback | verified-by (rung evidence)
2026-11-02 | yin-mac | bilibili | yt-dlp BiliBili extractor 2026.10.26, format 30016 (stale card re-verified) | 39,980,845 B | WORKING | rungs 1-4 cleared; r4 sha matches card 001 cross-day | rm -f /tmp/reg1/* | r3 ls -l + r4 sha256 0a0a23b5…1a7962
2026-11-02 | yin-mac | downloader.example.com | http://downloader.example.com/ | 0 B (no request formed) | FAILED | died at DNS layer (NODATA + curl exit 6); control example.com 200/577 B same session | none | dig empty-answer + curl exit=6 + control 200
2026-11-02 | yin-mac | bilibili | app.bilibili.com + big.bilibili.com/pc/video (lane-B sweep) | 25,569 B / 2,225 B (rung-1 bodies) | UNVERIFIED | reachability only; no endpoint/dowload capability tested; no new route found | none (preflight-abort rule attached) | per-candidate curl with own exit codes
```
