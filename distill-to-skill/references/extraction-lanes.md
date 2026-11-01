# S1 — Structure Map & Five Extraction Lanes

## Step 0: structure map (before any extraction)

One page, from the normalized text:
- 目录/时间轴骨架（章节 → 段 ID 映射）
- 材料类型判定：方法论 / 叙事 / 教学 / 访谈 / 工具演示 → 决定五路权重
- 声明材料的**未覆盖预告**：哪些话题被开了一个头就没了（预登记进 coverage-audit）

## The five lanes (parallel sub-agents; brief each via briefing-ai-agents)

每 lane 拿到：完整规范化文本（分块带 ID）+ provenance 记录 + 任务单。输出统一 fragment schema：

```markdown
FRAG-<lane>-<k>
claim: <one executable statement, verb-first where possible>
anchor: <seg-NN HH:MM:SS / ch3 p57 / "quote ≤15 words">
type: model | rule | example | anti-pattern | term
reuse-value: <what action this enables for a future user>
[INFERRED]: <only if the lane generalized beyond the source; else absent>
```

### Lane 1 — framework (框架浓缩者)
模型、分层、流程、分类法、检查表。输出：候选技能骨架（适用场景一句话 + 核心流程 3-7 步 + 边界"何时不用"）。

### Lane 2 — principles (原理浓缩者)
"为什么这样做"的机制、判断启发式、因果链。输出：imperative 规则（每条例：“做 X，不做 Y，因为 Z（锚点）”）。

### Lane 3 — cases (案例浓缩者)
带数字/人名/结果的真实案例、前后对比。输出：examples 素材（每条含来源锚点；无名无数的"据说"案例降级）。

### Lane 4 — counter-examples (反例浓缩者)
作者点名警告的做法、失败模式、常见误解。输出：common-mistakes 素材（每条配 paired-rule）。

### Lane 5 — terms (术语浓缩者)
术语表：词 → 定义（用材料里的上下文，不许引外部百科除非溯源）→ 首次出现锚点。

## Lane brief 模板（给每个子代理的任务单）

```
目的: <lane 名称与产出>
输入: <segments/chapters 清单 + provenance>
约束: 只从给定材料提取；每条 fragment 带锚点；不许补全材料没说的；不准摘要整段大意，要可执行颗粒
验收: fragment 数 ≥<按材料长度定>；每条 anchor 可指回；INFERRED 标注率 <10%
```
