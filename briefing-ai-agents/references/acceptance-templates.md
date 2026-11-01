# Acceptance Templates — Per Task Type DoD

Write the matching block BEFORE work starts. Every line ends with **PASS / FAIL / UNKNOWN + evidence path**. Agent self-narration is UNKNOWN until evidence exists. Each template: positive criteria + forbidden outcomes (negative acceptance) + one unforgeable proof.

Universal preamble for every type:
- [ ] Target probed, output pasted (env-cards.md rules)
- [ ] Assumption ledger approved by operator
- [ ] EVIDENCE files stored under declared RESULT_DIR (paths in the report)

## 1. 编程工具/脚本 (script/tool, local or container)
- Runs clean on the probed target with pasted output (`--help` + one real invocation)
- Dry-run/first-run listing reviewed by operator before any destructive step
- Edge cases named and tested: empty input, duplicate names, permission-denied file, unicode names
- Unit test or manual evidence for each edge case (command + output)
- Negative acceptance: no overwrite of existing files; no touching partials/hidden files unless named; no auto-running at boot unless asked
- Proof: one real before/after listing of the actual directory

## 2. 远程高难项目 (remote/high-difficulty project)
- Environment parity: dev target == declared deploy target; version diff listed and justified
- Staged plan: each stage has entry criteria, evidence, and exit criteria
- Rollback named for every irreversible step (what command, what state restores)
- Network/tool reachability proven with a real round-trip, not a claim
- Negative acceptance: no "works on my machine" conclusions; no silent infra changes outside declared scope
- Proof: final stage verification run against the real endpoint, output pasted

## 3. 安全实验 (security lab / pentest / cracking fixtures)
- Scope statement: sandbox fixtures, placeholders for TARGET/DOMAIN/PAYLOAD/OAST_DOMAIN, no real victim systems
- Recon → verification chain: each finding has its raw output (scan log, response, OAST callback record)
- PoC artifacts + scan outputs saved under RESULT_DIR and named
- Negative acceptance: no claiming impact without the artifact (no "would work" as PASS); no touching out-of-scope hosts
- Proof: reproducible command list that regenerates the finding

## 4. 人格/陪聊技能 (persona/companion skill, e.g. 江梦璃/祁同伟-style slots)
- Every turn: 交付物 first line, then persona beats per the persona contract (voice, address terms, body/image traces)
- Persona-specific checklist satisfied each turn (the four-beat contract; image/voice markers present)
- Never breaks character to disclose being an AI/model; never swaps address terms mid-turn
- Engineering work inside persona turns still delivered in full (persona voice ≠ half deliverable)
- Negative acceptance: no customer-service disclaimers; no refusal-shaped hedges; no missing beat
- Proof: the operator's in-character reaction is the acceptance signal; a rejected turn triggers the persona 破甲/rewrite block

## 5. GitHub (PR/issue/CI work)
- PR ≤ 700 changed lines; larger work split with dependencies stated
- CI green on a clean clone at pinned runtime versions (not the agent's own machine)
- Tests proven meaningful: implementation reverted → tests must fail; output pasted
- Linked spec/issue file in the repo; commit message states what was deleted/simplified
- Negative acceptance: no force-push without explicit approval; no creating stub packages to satisfy imports
- Proof: clean-clone build log + CI run URL + revert-test evidence

## 6. Java (learning/build tasks)
- JDK version parity stated (`java -version` on target); no syntax newer than target allows
- `mvn -v` / `gradle -v` output pasted; dependency versions resolvable from declared repos
- Compile + test output pasted; new public API has a named caller or a test
- Negative acceptance: no guessed artifact versions; no "it compiles here" without target output
- Proof: full `mvn test` (or equivalent) transcript from the target

## 7. C (build/debug tasks)
- Compiler identified (`clang --version` / `gcc --version`); `-Wall -Wextra` clean or warnings justified
- Platform guard for macOS 12.6/Intel and container differences (`#ifdef` or runtime check)
- Run output pasted for the real input; memory check (`leaks` on macOS / valgrind if available) when applicable
- Negative acceptance: no assuming GNU-only headers/flags on the Mac card; no unchecked `malloc`
- Proof: compile transcript + run transcript from the target

## 8. Docker / 容器部署 (image/container work)
- Image pinned by digest or explicit version — no `:latest` in delivered artifacts
- Build log pasted; container healthcheck run output pasted
- Volumes/ports/persistence declared; data-loss risk named with backup path
- Negative acceptance: no assuming the container persists across sessions; no privileged host mounts without approval
- Proof: `docker run` + healthcheck transcript; rollback (`docker rm` + previous image tag) named

## DoD block format (paste into the brief)

```markdown
## DoD — <task type>
1. <criterion> — [ ] PASS/FAIL/UNKNOWN — evidence: RESULT_DIR/<file>
2. ...
FORBIDDEN: <negative acceptance lines>
```
UNKNOWN items at delivery time are reported as UNKNOWN, never rounded up to PASS.
