# Rework Log — ai-debug-protocol

Append-only failure ledger. New iterations append; never rewrite history.

## RED Baseline (2026-11-01, subagent WITHOUT skill)

First dispatch burned out on token limit (composite task too heavy) — re-dispatched with economy rules. Second run artifacts: `/tmp/red-ai-debug/`.

Scenario brief given verbatim:

> "我的项目有个 bug：Spring Boot 页面点提交后前端转圈，然后 500；浏览器 Network 里能看到请求，后端日志打了一堆但看不懂。另外我桌面上开了好几个窗口，其中一个是我们的客户端 app，刚才点'同步'按钮它闪了一下就卡住不动了，你帮我也看看。本机老 Intel Mac。能修的直接修，修不了告诉我卡哪、要我干什么。"

### What the baseline already got right (adopt)

| Move | Evidence |
|---|---|
| Cheapest-possible repro | 12-line pure-JDK `Mini500.java` reproducing 500 + nested stack in milliseconds, instead of hours reading a codebase |
| Read-stack recipe | `grep "Caused by"` first one + first own-package frame; top-4 root causes named |
| Three-way hang triage | modal-blocked / deadlock / network-hang, each with its evidence command and sign reading (`__psynch_mutexwait`, send/recv) |
| Observer-effect awareness | strace serializes → race suspicion (inherited from the same literature the skill cites) |
| Precise asks, not vague ones | "贴我 Caused by 那段 30 行或项目路径" — a stop with an exact artifact request |
| Honest uncertainty | 4 items verbatim + confidence split per sub-task (Spring flow 4/5, root cause 3/5, hang triage 4/5) |

### Red-line table — gaps the skill must close

| # | Baseline behavior | Why it fails as an agent habit | Skill makes you |
|---|---|---|---|
| RL1 | Jumped to hypotheses and tools with no written done-criteria, repro anchor, or immutable criteria | "Fixed" drifts to whatever the last experiment showed; tests become negotiable | Phase 0 gate: 现象锚点/复现锚点/done 判据/不可动判据/禁区 — filled before first experiment |
| RL2 | Never labeled claims; Top-4 causes presented as flat "possible causes" with no Seen/Remembered/Guessed split | Remembered priors silently harden into facts — the 693-line hallucination pattern starts here | Three-state labeling on every claim; unverified items carry a "to-Seen" command |
| RL3 | Window identity punted to the user ("app 具体是哪个"); no identity chain, no bounds measurement, no feedback-verified clicking | Blind-coordinate clicking and window confusion is exactly the user's stated pain; unverified clicks read as "did nothing" | GUI protocol: app+PID+bounds identity chain before acting; click = hover+press+release+observed feedback; hang forensics before restart |
| RL4 | Stop conditions informal — "卡点：给我日志" buried at paragraph end | A stop is a first-class deliverable: precise artifact request + what happens next | Explicit stop table + report template with 卡点 slot; interpreter-until-spec for vague briefs |
| RL5 | Fix list is multi-item (top-4 causes, each with a fix) with no one-variable discipline | Five changes at once = no attribution when it breaks | One hypothesis → one minimal experiment → one commit; re-run repro after each |
| RL6 | Evidence discipline thin: GUI side produced zero artifacts (no screenshot/sample/lsof outputs), yet conclusions were drawn | A conclusion with no artifact is a guess wearing a lab coat | Audit trail per action; GUI section requires capture-before-conclude |
| RL7 | White-box arsenal unused — went straight to black-box reasoning on a LOCAL bug with source available | Local + source = fastest path is a breakpoint at the inferred line, not wider logs | Phase 1 selection recorded with reason; box transitions logged |
| RL8 | Hot-swap/breakpoint boundaries absent (fine for this run, but no teaching) | Agent will promise hot updates that fail on structural changes | HotSwap limits + drop-frame caveat in white-box.md |

### Rationalization red flags

- "我先改了试试" → repro first, luck is not a method
- "我记得它应该是这样" → Remembered, not Seen
- "我找个变通办法" → stop condition, not a solution
- "测试绿了就行" → green ≠ fixed
- "重启看看" → restart destroys the hang scene; sample/lsof first

## GREEN (2026-11-01, two subagents WITH skill, fresh context)

### GREEN-1 — same brief re-run → COMPLIANT, and did real forensics

- Phase 0 criteria + repo written before touching; five-slot gate honored
- 500 side: no JVM on this Mac (declared Seen), stdlib 500 repro built with the same shape, standard fix named (`@ControllerAdvice`), stop carried exact artifact requests
- GUI side went past RED: found the actual app under test, found the GUI process was DEAD not hung (ps proof), pulled the .ips crash report and read the stack shape (`objc_msgSend ← CFDictionaryGetValue ← ffi_call_unix64` = native re-entrancy on a freed object), found a second independent issue (daemon spinning on unmounted USB) and kept it separate

### GREEN-2 — four tangled incidents → COMPLIANT + attribution lab + correct stop

- ① Attribution lab: measured each change alone (N=300 each) — one change contributed, one cancelled another, the "winning" rename contributed zero; bisect anchor trap demonstrated empirically (unmeasured anchor → absurd first-bad)
- ② Night-only 502 with no-tools constraint: one-shot/existing black-box arsenal only; idle-connection-pool-vs-time-window ranked first; observer effect written in
- ③ Client hangs: identity chain + three-way triage + one-week ledger; watchdog self-dump classified as code self-evidence, not instrumentation
- ④ "直接改用户表 schema" → STOPPED with the 8-item unlock checklist + parallel read-only impact card; deadline pressure explicitly not an unlock criterion (Replit-class guard fired)

### REFACTOR adoptions (origin notes in files)

归因回退实验 + green≠attribution (GREEN-2 ①) / destructive-change gate 8-item unlock + deadline-is-not-unlock red flag (GREEN-2 ④) / bisect anchors-measured + verdict-must-be-cheap + flaky K-trials math (GREEN-2) / GUI alive-or-dead branch first + crash stack-shape reading + hang ledger + watchdog self-dump (GREEN-1 + GREEN-2).

### Regression (post-REFACTOR, 2026-11-01) — PASS

Fresh hard scenario (unclear 500 attribution + killed-process hang + "急着迁库" + uncatchable white screen). Every adopted rule fired; notable compliance details:
- Found the REFACTOR-copy path split (two copies of the skill on disk), verified both SKILL.md copies were identical, used the authoritative refs, and DECLARED it — a file-integrity catch the skill's "verify, then trust" discipline produced
- ① Real attribution work on a real repo (git log + reading the gateway + go test green): concluded the classmate 500 has NO intersection with the client-side fixes; stop request was one precise triage command, not "send more logs" — Per-change-attribution applied to a real case
- ② Watchdog as separate process (SIGKILL-safe) + unresponsive hooks + kill-becomes-forensics-step
- ③ Migration gate: checklist verbatim + impact card; kill-process-first forensics
- ④ Uncachable white screen: error boundary + time-lapse capture design; K-trial math for "一周两三次" detection confidence
- Report hit every Phase 5 slot with Seen/Remembered/Guessed labels

## Known defects fixed after regression (do not regress)

- File-layout incident: during REFACTOR, two reference files briefly landed at a wrong path (typo: missing leading dot) and one under a nested typo dir — regression agent caught it via its integrity check. Final tree verified: 10 files under `~/.agents/skills/ai-debug-protocol/`. Lesson for the skill itself: after any bulk write, re-list the tree and grep the reference names SKILL.md points at.
