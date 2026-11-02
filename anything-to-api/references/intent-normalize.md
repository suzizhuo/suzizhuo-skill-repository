# Intent Normalization — step 1 of the pipeline

A vague ask is the #1 cause of a wasted run. RED R1 accepted `PLACEHOLDER_URL` as a "target"
and deferred resolution; the correct move is that resolution IS the pipeline's first job.

## The normalized intent card

```
target-sites:   <one or more sites; unknown ⇒ "to be discovered">
media-type:     video | audio | images | document | live stream | data
quality-floor:  <default: whatever the cheapest route returns; state it, don't assume>
watermark:      irrelevant | strip-required | as-is   (irrelevant relaxes QUALITY, never IDENTITY)
batch:          single <live target> | handle-set | channel/user
login-state:    none | cookie-available | login-required-unknown
live-target:    <real URL or resolvable handle — REQUIRED before download claims>
query-set:      <lane B seeds, see below>
egress-note:    <which hosts probed reachable from the executing machine>
```

## Resolving the live target

If the user's URL is a placeholder, dead, or "just use the top one on the homepage":

1. Short-link resolve: `curl -sI <short-url>` → follow the redirect (v.douyin.com answers
   302→www.douyin.com in ~2.7 s from this Mac) → concrete share URL with a real id.
2. Ranking resolution: for "首页热门第一支", resolve via the site's own ranking API or a
   search-engine query for a trending item on that site — and probe the result. A 200 on the
   homepage proves the page is served, nothing about a ranked video.
3. Handle resolution: a user/channel handle becomes a profile URL, then per-item URLs.

A target that cannot be resolved is stated as the blocker — with the probe outputs — never
silently deferred to "when the user provides one".

## The query set (lane B's seed)

Formula: `<site-CN-name> <media-word> <接口|API|下载器|解析|去水印>` — the operator's own
"抖音 快手 哔哩哔哩下载器" is the model. Rules:

- One query per family (抖音 / 快手 / 哔哩哔哩 / …), plus one cross-family query.
- Use the site's own language: CN sites → CN engines (bing.com 391 ms, baidu 255 ms from this
  Mac). Google/YouTube-family are egress-dead here (000 after 5 s) — those queries offload to
  the remote host, not "try anyway".
- Collect candidates as (URL + why-looked-relevant + probe plan), never as "a forum said so".
- Expected junk is a *known class*, not a surprise: search-rank downloader sites that gate
  behind extensions/paid "clean" tiers are documented scams (evidence F2/F3) — they land in the
  candidate list only to be probed to death, never shipped.

## Quality and identity

- The quality floor is whatever the shipped route returns unless the user names one; state it.
- The identity bar is not negotiable by quality talk: the delivered bytes must be **the
  requested item** (title/id match, duration sane, playable). "水印无所谓" changes nothing here.

## Output

The normalized card travels with every lane dispatch and with the final report. Lane results
that contradict the card (e.g. route delivers a different item) are FAILED at rung 3, not
re-labeled to fit.
