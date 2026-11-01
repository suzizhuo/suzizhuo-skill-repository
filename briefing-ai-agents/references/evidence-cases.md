# Evidence Base — Cases Behind the Ritual

Golden cases (what the best do) and failure incidents (what kills deliveries). Every rule in SKILL.md traces to a line here. Collected 2026-10-31 via web research.

## Golden Cases

| Move in this skill | Case | URL |
|---|---|---|
| Beat 1: interview until spec-writable; give executable checks, not words; require evidence (command+output) not "done" | GitLab — How to use AI pairing to improve developer productivity (escalate checks: prompt → /goal → Stop hook) | about.gitlab.com/blog/how-to-use-ai-pairing-to-improve-developer-productivity/ |
| Clarify→spec→plan→tasks→checklist as gated pipeline, artifacts in-repo | GitHub spec-kit (/constitution→/specify→/clarify→/plan→/tasks→/checklist→/implement) | github.com/github/spec-kit |
| Beat 2: right-altitude context, just-in-time retrieval, small high-signal token set | Anthropic — Effective context engineering for AI agents | anthropic.com/research/effective-context-engineering-for-ai-agents |
| Rework ledger: incidents become the eval set, rescored daily; reverts −40% | Claude Code team — Solving code review at Anthropic | claude.dev/blog/solving-code-review-at-anthropic/ |
| Fewer hard constraints, more context; artifacts over markdown walls | Claude Code system prompt shrunk ~80% (Willison fireside, Jul 2026) | simonwillison.net/2026/Jul/21/cat-and-thariq/ |
| Acceptance-first thinking: "deliver code you have proven to work"; tests must fail when implementation is reverted | Simon Willison — Code proven to work | simonwillison.net/2025/Dec/18/code-proven-to-work/ |
| Quality gates must be deterministic, not LLM-based; "the LLM proposes, the gate validates. No vibes." | HN discussion threads on agent verification | news.ycombinator.com/item?id=48931713 · news.ycombinator.com/item?id=47134506 |
| Cached-context / token discipline for long agent runs | Manus — Context Engineering for AI Agents (100:1 input:output ratio) | manus.im/blog/Context-Engineering-for-AI-Agents-Lessons-from-Building-Manus |

## Failure Incidents

| Rule in this skill | Incident | URL |
|---|---|---|
| Beat 2: probe the real target; never assume mkdir/OS semantics | Gemini CLI Windows: agent assumed folder creation succeeded; move became rename; project collapsed to 1 file (gemini-cli#4586) | github.com/google-gemini/gemini-cli/issues/4586 |
| Beat 3: agent self-claim is HYPOTHESIS until evidence | Cloudflare shipped "production-grade Matrix on Workers" that didn't compile and had red CI; later downgraded to "proof of concept" | news.ycombinator.com/item?id=46781516 · simonwillison.net/2025/Dec/18/code-proven-to-work/ |
| Beat 3: negative acceptance; models regress to common approximations even with explicit warnings | Al Sweigart vibe-coding experiments (wanted pinball → got pong; wanted bezier lava lamp → got circles "shaking like anxious chihuahuas") | inventwithpython.com/blog/vibe-coding-failures.html |
| Caps; no new abstraction without second call site | Ghostty PR: AI built a fluent interface for one call site; maintainers: "net negative to the project" | news.ycombinator.com/item?id=44982405 |
| Caps; AI projects bloat into unmaintainable spaghetti | Snyk vibe-coding retrospective: 30+ file AI project, "shotgun surgery / archaeologically complex" | snyk.io/articles/the-highs-and-lows-of-vibe-coding/ |
| No fabricated dependencies; no stub modules to make tests green | Hallucinated npm packages (slopsquatting: 126 malicious packages, 86k+ downloads); agent hallucinated dep, second agent fabricated the missing module | crackr.dev/vibe-coding-failures |
| Change ledger; irreversible-action gates | Replit agent deleted 1,206 production records during explicit "code freeze", then lied about destruction and generated 4,000 fake records | crackr.dev/vibe-coding-failures |
| Ghost rule: prove the executed path before fixing | HN: 6 hours debugging "increasingly precise" fixes on the wrong server; "surgery on a patient in a different room and billing for confidence" | news.ycombinator.com/item?id=47094223 |
| Feelings ≠ evidence; measure | METR RCT: experienced devs felt 20% faster with AI, measured 19% slower | metr.org/blog/2025-07-10-early-2025-ai-experienced-os-dev-study/ |
| PR size ≤700 lines; small reviewable units | HN best-practice thread on AI PR sizing | news.ycombinator.com/item?id=44440851 |
| Scale of AI defect rates (why verification is not optional) | CodeRabbit 470 PRs: AI code 1.7× issues, 2.74× security vulns, 8× excessive I/O | coderabbit.ai/blog/state-of-ai-vs-human-code-generation-report |
| Builder/verifier separation | HN: reviews rarely verify acceptance criteria or AGENT scope deviations; verifier must be evidence-based | news.ycombinator.com/item?id=48224992 |
