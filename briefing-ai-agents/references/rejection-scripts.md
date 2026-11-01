# Rejection Scripts — 把模糊否定翻译成可执行修正令

Operator rejects in feelings ("感觉不对", "太乱了", "看不懂", "又不对了"). The agent's job: convert each into an executable fix order BEFORE touching code. If the delta cannot be named, ask for a reference — never guess-rewrite.

## Translation Table

| Operator says | Executable fix order | Forbidden response |
|---|---|---|
| "感觉不对 / 不对劲" | Name the observable delta: which element/behavior differs from expectation, with expected vs actual. If no reference exists, ASK for one (screenshot/sample) first. | Silent full rewrite |
| "太乱了 / 看不懂" | Apply the readability recipe: (1) rename the confusing identifiers to domain words; (2) split functions named by operator's complaint into ≤N-line units; (3) comments only where the WHY is non-obvious; (4) delete, don't add. Report "我删了什么" as first line. | Adding more abstraction/structure |
| "太啰嗦 / 过度了" | Cut to minimal implementation: delete unneeded branches, unused params, premature interfaces. One diff, single purpose. | Rewriting everything |
| "它说 done 但其实没成" | Demand the evidence chain: exact command + output + affected artifact. UNKNOWN until produced. Report which DoD lines were FAIL/UNKNOWN. | Arguing that it works |
| "改回去 / 又不对了" | Diff-only revert of the specific hunk that caused it; single-purpose patch; re-run the DoD evidence. | Big-bang regeneration |
| "不是我想要的" | Re-open Beat 1: re-interview with multiple choice; the spec was wrong, so rewrite the spec first, then the diff. | "Improving" the wrong thing faster |
| (mid-task) "我突然想起来还要…" | Ledger entry FIRST (what new constraint, which old decision it voids), then re-plan, then implement. See change-ledger.md. | Silently changing course |

## Scale Caps (cite when operator says bloated)

| Cap | Value | Why (case) |
|---|---|---|
| Changed lines per single unit | 400–700 | small PRs review clean; big ones hide AI defects (HN #44440851) |
| Files created for a "small task" | ≤3 unless operator approves more | a 30-file AI project collapsed into unmaintainable spaghetti (Snyk vibe-coding retrospective) |
| New abstraction layer | 0 without a named second call site | Ghostty PR: fluent interface for one call site = net negative |
| Longest function after the fix | ≤60 lines or operator's stated limit | unreadable code is the operator's core complaint |
| Frameworks/dependencies for one-off scripts | 0 new | every dep must resolve in the probed env (hallucinated-package incidents) |

## Readability Recipe (when operator can't follow the code)

1. First action: rename to the operator's own words (the names he used when asking).
2. Then: one entry point, explicit steps, no cleverness (no nested ternaries, no one-letter vars).
3. Then: comments ONLY where the why is non-obvious; no noise comments.
4. End with: "我删掉了 <X>" — a deletion report, not an addition report.
5. Check: could the operator explain this code to someone else after one read? If not, it isn't done.

## Tone

Verdict first, evidence second, zero process narration. The operator is the boss; the deliverable talks.
