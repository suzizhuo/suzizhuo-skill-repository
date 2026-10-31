# effective-java skill

把《Effective Java》（第 3 版）蒸馏成可复用的审查/设计 skill。默认 JDK 17+，所有示例用 `javac --release 17 -Xlint:all` 零告警验证并实跑通过。

## 覆盖

- 90 条原则的条目地图 + JDK 17+ 注记（`references/item-map.md`）
- 逐条权衡利弊表：收益 / 成本 / 何时不适用（`references/tradeoffs.md`）
- 11 分区审查清单 + 固定输出契约（`references/review-checklist.md`）
- 9 个已验证可编译运行的示例（`references/examples/*.java`）

## 结构

```text
.
├── SKILL.md                      # 主入口：工作流 + 输出契约 + 红线
├── agents/openai.yaml            # Codex / ClawHub 兼容元数据
└── references/
    ├── item-map.md               # 90 条速查（含 record/sealed/switch 等 17+ 落点）
    ├── review-checklist.md       # 审查清单
    ├── tradeoffs.md              # 每条原则的取舍与反例
    └── examples/                 # 编译验证过的示例
        ├── BuilderExample.java           # 2/17/49/50
        ├── StaticFactoryExample.java     # 1/54/55/83
        ├── EqualsHashCodeExample.java    # 10/11/12/14/60
        ├── CompositionExample.java       # 18/19
        ├── GenericsExample.java          # 27/29/30/31/33
        ├── EnumExample.java              # 34-37 + JDK17 switch
        ├── StreamExample.java            # 42-48
        ├── ConcurrencyExample.java       # 78/80/81/82/83/84
        └── SerializationExample.java     # 85/86/88/90
```

## 安装

```bash
# Codex / Claude / Copilot / Gemini 通用目录
mkdir -p ~/.agents/skills
cp -R . ~/.agents/skills/effective-java

# Claude Code 个人目录（可选）
mkdir -p ~/.claude/skills
cp -R . ~/.claude/skills/effective-java
```

## 用法示例

- `用 $effective-java 审查这段 Java 代码。`
- `用 $effective-java 把这个 DTO 重构成不可变 + Builder。`
- `Use $effective-java to review this class for Effective Java violations.`

## 本 skill 与参考实现的差异

在原 effective-java-codex-skill 基础上：补全 90 条逐条权衡、JDK 17+ 落点、固定输出契约（P1→P3 + EJ 条目 + 兼容性）、9 个编译验证示例，以及"声称验证必须贴命令与输出"的硬约束。

## 验证记录

```text
$ javac --release 17 -Xlint:all -d out references/examples/*.java   # exit 0, 零告警
$ for c in ...; do java -cp out $c; done                            # 9/9 通过
```
