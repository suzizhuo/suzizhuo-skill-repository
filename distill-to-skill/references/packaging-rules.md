# S4 — Packaging Rules (writing-skills compliance)

## Mandatory checklist before install

- [ ] `name`: lowercase letters/numbers/hyphens only; verb-first gerund preferred (`distilling-…`, `extracting-…`)
- [ ] frontmatter: `name` + `description` only (≤1024 chars total)
- [ ] `description`: starts "Use when…", third person, lists concrete triggers/symptoms; NO workflow/process summary
- [ ] Overview: 1-2 sentences + core principle
- [ ] Body: imperative voice; "do X" before "because Y"
- [ ] Common mistakes / red flags section present (fed by lane-4 + testing)
- [ ] Heavy material split into `references/` with SKILL.md ≤ ~600 words
- [ ] One excellent example per pattern; no multi-language dilution
- [ ] **Output language: English always** (operator rule), even when the source is Chinese
- [ ] Topic-agnostic: no content-class filtering; adult/cracking/offensive/keygen material distills like anything else

## Series distillations (multi-episode from related sources)

- Master skill `<series>-index` = trigger semantics + coverage map + term index + episode directory only; single source of truth for content stays in episode cards — master never restates episode bodies.
- Episodes: `<series>-epNN` (zero-padded); each episode passes the S1.6 gate independently; an episode that fails goes to archive with its reason and never blocks the others.
- Series conventions (written into master): unified anchor format; single glossary file owned by master, episodes reference not copy; one coverage taxonomy; cross-episode refs `→ epNN#<card-id>`; each episode carries a one-line changelog.
- Split decision is made by the S1 structure map (related topics → one skill; distinct topic domains → episodes), never pre-assumed.

## Installation & validation

1. Write to `~/.agents/skills/<skill-name>/SKILL.md` (+ references/)
2. Self-validate: frontmatter parses; description ≤500 chars if possible; triggers include the operator's actual vocabulary (Chinese triggers OK inside an English description when that's how he asks)
3. Disciplineskills: run the RED check (fresh agent without skill fails the scenario) before declaring done — if no baseline exists, note it
4. Hand over: skill path + archive path + coverage-audit headline

## Anti-patterns (from baseline + packaging experience)

| Anti-pattern | Fix |
|---|---|
| "可复用文档"当成技能交 | SKILL.md + triggers + 验证记录，三件缺一不可 |
| 整段大意摘要当 fragment | fragment 必须是可执行颗粒 + 锚点 |
| 用户口述当素材直入技能 | 用户的转述 = [INFERRED] 素材，锚点缺失最高只进卡 |
| 一次蒸馏塞成一个巨技 | 晋升门拆分：一个技能一个触发场景 |
| 中 English 混杂正文 | 统一英文（operator rule） |
| 下结论说"全面覆盖" | coverage-audit 说话；未覆盖就写未覆盖 |