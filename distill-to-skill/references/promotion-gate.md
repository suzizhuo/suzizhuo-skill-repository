# S1.5–S2 — Verify, Promotion Gate, Archive

## V1 / V2 / V3 verification (per fragment)

| Check | Pass condition | Fail handling |
|---|---|---|
| V1 来源真实性 | anchor 可在源材料指回；verbatim 或紧贴锚点的复述 | 无锚点 → 丢弃；对不上 → 标 [MISMATCH] 并记录 |
| V2 可复用性 | 产出是**可执行动作**（verb-first、可验收） | 口水/感悟 → 降级为卡片素材，不进技能 |
| V3 时效性 | 时效断言带日期戳；与当前事实冲突时以当前来源为准并注冲突 | 无法更新的旧断言 → 保留但打 [DATED] 标 |
| [INFERRED] 纪律 | 推断必须显式标注，比例 <10% | 未标注的概括 = V1 违规 |

## Promotion gate (fragment-set → Skill)

四条全中才晋升：
1. **触发语义**：存在明确"use when…"场景——一个陌生 agent 看到描述会知道何时加载
2. **可执行密度**：≥3 条动词开头的动作，且每条有锚点
3. **覆盖地图有位**：补上 coverage map 的一个空位，不与已入库技能重复（重复 → 记"served_by"归档）
4. **压力测试可过**：见 SKILL.md S3

未晋升 → `~/distill/<slug>/rejected/` + `coverage-audit.md` 记一行：fragment、失败原因（无触发语义/动作不足/已覆盖）、处置（归档 or 并入他技）。

## Coverage audit (每次蒸馏必写，持续追加)

```markdown
## coverage-audit — <slug> (<date>)
covers:
  - <主题 A> → fragments: F1,F3,F7 | 状态: 已入技能 <skill-name> / 已归档 / 待孵化
opens-and-drops:        # 材料开了头没收口的话题（未来补料清单）
  - <话题 X> 在 seg-12 提出，材料未给结论 → 待补来源
conflicts:              # 材料间/材料与现实的冲突
  - <claim> vs <current source> → 结论: <采信理由>
ledger:
  - F<k> | <claim 摘要> | V1✓ V2✓ V3✓/DATED | 晋升/归档: <技能名/原因>
```

## Cards (R/I/A1/A2/E/B)

```markdown
C<k> | 主题 | anchor
R: <verbatim/紧贴锚点的事实复述>
I: <我的概括（必须能指回 R）>
A1: <同类适用场景 1>
A2: <同类适用场景 2>
E: <何时失效/反例>
B: <一句话行动卡：做 X 不做 Y>
```

卡片存 `~/distill/<slug>/cards.md`；术语存 `glossary.md`（词→带锚点定义→首现位置）。

## 档案布局

```
~/distill/<slug>/
  provenance.md        # S0 出身记录
  transcript-segments/ # 分块规范化文本（含时间戳）
  cards.md / glossary.md
  coverage-audit.md
  rejected/            # 未晋升 fragment + 原因
  pressure-test.md     # S3 记录
```
