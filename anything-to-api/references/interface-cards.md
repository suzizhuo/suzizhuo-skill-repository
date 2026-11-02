# Interface Cards & Library — the persistent half of the skill

Every claimed API lives or dies here. A card is the only unit that survives a site redesign;
the library is the only reason next week's task does not re-derive (RED R2's exact failure).

## The card format

```
card-id:       <family>-<target>-<nnn>            verdict:  WORKING | SIGNED-NEED | FAILED | UNVERIFIED
probed-at:     <YYYY-MM-DD HH:MM TZ>              probed-on: <machine card-id, egress boundary>
endpoint:      <method + URL, exact>
params:        <required, optional, defaults>
sign:          none | cookie-set | <sign family + algorithm summary + where it lifts from>
sample-call:   <one runnable line>
rungs:         r1 <probe> | r2 <placeholder call output> | r3 <real target call + byte check> | r4 <second call>
limits:        rate/size/batch notes as observed
rollback:      <what to undo; for scripts: preflight-abort pattern>
last-known-good: <the rung-3 evidence line — seed for re-mining>
```

## The verification ladder (a claimed API must clear all four)

1. **Reachable** — per-host probe with its own exit code (`curl -m 5`; DNS exit 6 / timeout 28 /
   TLS 35 are different deaths and get named as such).
2. **Well-formed placeholder** — a call with placeholder params returns the expected shape.
3. **Real target → real bytes** — the requested item's URL, bytes on disk, size/type/playability
   checked. **This rung gates the word 能用.** A name in `--list-extractors` is rung 0: the
   program's claim, not your test.
4. **Deterministic** — second call reproduces the same bytes (sha where possible).

Exit codes are evidence only when captured honestly: **a pipe gives you the LAST command's exit
code** (`yt-dlp -F | head` returned SIGPIPE exit-120 while the real call exited 0 — RED R2's
unexplained 120, resolved). Write probe output to a file, read it, capture `$?` directly — never
through a pipe. A death gets named: DNS exit 6 ≠ timeout 28 ≠ TLS 35.

Name the **layer** at which death occurs, because repairs differ by layer: DNS · TCP/TLS ·
HTTP status · sign (signer produces params but the server rejects them — GREEN G1's
ArgusSecurityPlugin 403) · transport (local TLS-stack divergence killed a correctly signed
call: python OpenSSL vs macOS SecureTransport) · content (a 200 with a 522-byte anti-crawl
shell is a content-layer wall, not success). "Died" is not a verdict; "died at layer X with
output Y" is.

Any rung failure = FAILED at that rung, with the rung and probe output recorded. UNVERIFIED is
for anything not yet probed — and it travels: in the reply text, in the card, and in any script
as a verdict comment plus a preflight that aborts instead of silently falling back.
**FAILED cards are first-class**: a card whose death is named (fresh-cookies wall at rung 2,
DNS-dead endpoint at rung 1) is what stops next week's run from re-treading the same corpse.
Write them as diligently as WORKING cards.

## The library protocol

- **The library is `references/interface-library.md`** — cards append there, never into spec
  files. Append-only: cards are never edited, only superseded by a newer card with a newer
  `probed-at`.
- **Read before mining**: "上周那个接口" means read the card, re-verify, run. Re-verification on
  reuse: if `probed-at` predates the session, re-run the rungs (A-05: cards never travel across
  egress boundaries — re-probe on the executing machine).
- **Failure triggers re-mining**, seeded with the card's `last-known-good`: the lanes re-run,
  the new verdict replaces the stale card by append.
- **URL probing discipline** (A-08): every cited URL inside a card is probed before use. The
  documented evidence: B 站's two semi-official API doc repos (21k★ and 4k★) were lawyer-lettered
  offline in 2026-01/07; `JoeanAmver/TikTokDownloader` rotted to 404. The ecosystem deletes its
  own citations — assume every link is a hostage.

## The session ledger row

```
date | machine card | family | component | footprint (bytes, measured) | verdict | reason | rollback | verified-by (rung evidence)
```

## Anti-patterns at this layer

- Card written from the README, not from probes (provenance lies).
- Editing a stale card instead of appending the re-probe (history laundered).
- Re-mining before reading the library (the RED R2 failure).
- A WORKING card without `last-known-good` (unrecoverable without a full re-mine).
- Appending cards into this spec file — cards live in `references/interface-library.md`
  (the G2 card that started this convention was moved there verbatim in REFACTOR 2026-11-02).

