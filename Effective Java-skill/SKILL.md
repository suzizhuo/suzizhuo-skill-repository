---
name: effective-java
description: Use when writing, reviewing, refactoring, or explaining Java code, or when a request mentions Effective Java, static factories, builders, immutability, equals/hashCode, generics, PECS wildcards, Optional, streams, exceptions, thread safety, serialization, Java code review, or Java refactoring — especially on JDK 17+ codebases.
---

# Effective Java（JDK 17+）

## Core Stance

Effective Java 是设计与审查透镜，不是风格指南。默认前提：API 要简单、类型安全、难以误用、边界文档化、不可变优先。每条规则先过一遍"何时不适用"再出口（`references/tradeoffs.md`）。只意译原则，条目号做导航锚点，不复制原文。

## When to Use

- 审查/重构 Java 代码，要求按严重度出清单
- 新写 API / 值对象 / DTO / Builder / 静态工厂 / 单例
- equals/hashCode、泛型 PECS、Optional、stream、异常体系、线程安全、序列化问题
- 用户点名 Effective Java、条目号、"Java 最佳实践"、Java 17+ 代码评审

## Workflow

1. 分类任务：review / design / refactor / explain。
2. review/refactor → 读 `references/review-checklist.md`，按固定顺序扫：**正确性 → API 设计 → 封装 → 类型安全 → 清晰度 → 性能**（性能永远最后，先测量再优化）。
3. 条目级映射或学习总结 → `references/item-map.md`；做取舍判断 → `references/tradeoffs.md`；要可抄的代码 → `references/examples/*.java`。
4. 先看契约再谈风格：构造方式、可变性、相等语义、泛型、异常、线程安全档位、序列化、二进制兼容。
5. 改代码用保持行为的最小改动；只有用户明确要求重设计才动公开 API。

## Output Contract — 评审输出（固定形状，不许自由发挥）

每条发现固定四行：

```text
- [P1|P2|P3] 问题一句话（`File.java:line`）：什么场景下出什么错。
  EJ lens：条目号 + 原则名。
  Fix：最小可落地改动（≤10 行直接给代码片段）。
  Compat：公开 API / 序列化形式的破坏性与过渡方案（如 @Deprecated 两版迁移）。
```

- P1 = 正确性/集合语义/并发/资源/二进制兼容；P2 = 封装/类型安全/契约；P3 = 可读性/命名/风格。
- 清单分区没命中就写"第 N 区无发现"，不凑数、不追加未要求的总结段落。
- 看不到调用上下文（是否共享、是否跨版本反序列化）就标注"需确认"，不猜。

## Output Contract — 设计输出（固定顺序）

1. **契约声明**：构造方式、可变性、相等语义、线程安全五档之一（immutable / thread-safe / conditionally thread-safe / not thread-safe / thread-hostile）及理由。
2. **代码**：单文件、可编译、标注目标版本（默认 JDK 17+）。
3. **EJ lens**：每个关键决策 ↔ 条目号，一行一条。
4. **权衡**：备选方案 + 为什么不用（record vs Builder、sealed vs tag 类、stream vs 循环）。
5. **验证**：实际执行过的命令 + 真实输出；没跑就写"未验证"，禁止口头宣称"冒烟通过/零警告"。

## High-Value Heuristics

- 命名可澄清、实例受控、返回接口时用静态工厂；简单 new 就清楚的用构造器（1）。
- 参数多/可选/相互依赖 → Builder，校验收敛一处；少参数透明载体 → record（2/17，JDK16+）。
- 不可变优先：final 类 + final 字段 + 深防御；`List.copyOf`/`Set.of` 做快照（17/50）。
- equals/hashCode 同生同死；toString 有诊断价值不带敏感字段；BigDecimal 用 compareTo 比较；货币用 long/BigDecimal 不用 double（10–12/60）。
- 组合转发优于继承；构造器不调可覆写方法（18/19）。
- 泛型零 raw；PECS：生产者 `? extends`，消费者 `? super`；`Comparable<P>` 写 `P super T`；varargs 堆污染用 `@SafeVarargs`（26–33）。
- enum 替代 int 常量；`EnumSet`/`EnumMap`；ordinal 不落库不传输（34–37）。
- stream 只做无副作用转换；早退/分支/checked 异常用循环；方法返回集合不返回流；并行要有证据（45–48）。
- 入口校验、失败原子、空集合不返回 null、Optional 只做返回、异常转译保 cause 不吞异常（49/54/55/69–77）。
- 并发先想不可变与托管集合，executor 优于裸线程，懒初始化用 holder 或 DCL+volatile（78–84）。
- 新设计不上 Java 序列化；被迫序列化 → 显式 serialVersionUID + 序列化代理 + readObject 防御 + `ObjectInputFilter`；有可变状态的单例别用 readResolve 收敛（= 丢数据）（85–90）。

## Guardrails

- 不把每个类改不可变、每个构造器改 Builder、每个循环改 stream——每条建议都要能说出 tradeoff。
- 不引入框架；JDK 17+ 语言/库特性优先（record/sealed/switch 表达式/文本块/`List.of`/`Stream.toList`）。
- 公开 API 默认保持兼容；破坏性变更只在用户明确要求时给，并附迁移方案。
- 缺上下文不下结论，标注"需确认"。
- JDK 17 JPMS：`setAccessible` 进 JDK 内部受限，属架构约束不是代码技巧，别默默依赖。

## Red Flags — 出现即停、改输出

- 没贴命令和输出就说"已验证/冒烟通过/零警告" → 贴证据，或改写"未验证"。
- "DTO 而已不用校验" → 值对象校验收敛在构造器一处就是 2/49。
- "stream 更现代" → 早退/分支/checked 异常，循环更清晰（45）。
- "double 算钱先跑着" → 十进制精确用 long cents/BigDecimal（60）。
- "加锁就稳了" → 先看不可变/线程封闭/托管集合（78/81）。
- "public 字段以后再收" → 写进 Compat 行，别默认没人依赖（15/16）。
- "equals 用全字段最稳" → 可变字段进 equals，放进 HashSet 后改一个字段对象就丢（10/11）。

## File Map

- `references/item-map.md` — 90 条速查 + JDK 17+ 注记 + 按任务定位
- `references/tradeoffs.md` — 每条原则：收益/成本/何时不适用 + JDK 17 迁移权衡
- `references/review-checklist.md` — 11 分区审查清单 + 输出契约
- `references/examples/*.java` — 9 个 `javac --release 17 -Xlint:all` 零告警实跑验证示例
