# FEED — 把仓库喂给 AI：读 / 改 / 跑

## 0. 门禁（每次喂之前）

**REQUIRED SUB-SKILL: briefing-ai-agents 三拍。** 最小输入：仓库落地路径 + manifest 行 + 目的（读/改/跑其一）+ 约束（不许 push upstream / 只在容器跑 / 不许引新框架）+ DoD（见下）。缺一项不开工。

## 1. READ — 十分钟仓库解剖

一次交付一页纸：

1. `tree -L 2`（或 `gh repo view --json` 元信息）→ 模块划分
2. 构建文件清单：`pom.xml` / `build.gradle` / `setup.py` / `package.json` / `go.mod` / `Cargo.toml` / `Makefile` → 依赖表（名称+版本+用途）
3. 入口：main / cmd / app 启动链；配置入口（flags/config file/env）
4. README 承诺 vs 代码现实清单（声称的功能/默认值 哪些在代码里找不到 = 红旗记录）
5. 测试：有无测试、怎么跑、最近一次 CI 状态（`gh run list`）
6. 产出落到 `~/gh-res/<o>__<r>/ANATOMY.md`

验收：AI 能回答"这个仓库改 X 要动哪三个文件"而不通读全库。

## 2. MODIFY — fork + 最小 diff

- `gh repo fork --clone`（保留 upstream remote）；本地分支命名 `feat/<topic>` / `fix/<topic>`
- diff 规模上限沿用 briefing skill：≤400-700 行/单元；无第二调用点不加抽象
- 收尾自述"我删了什么"；PR 只开给自己的 fork，**不推 upstream**
- 上游更新：`gh repo sync` 或 `git fetch upstream && rebase`，冲突处置记一行 manifest

## 3. RUN — 环境卡 + dry-run + 证据

- 环境卡先行（skill 1 的 env-cards）：目标机 `uname -mrs`、OS、依赖运行时版本——探测输出贴进任务单
- dry-run / `--help` / 冒烟目标先行；任何"需要联网拉取"的步骤在容器侧先验证网络策略
- 证据落地：`~/gh-res/<o>__<r>/RUNLOG-<date>.md`（版本输出 + 一次真实运行 + 输出摘要）；manifest 状态列更新为"已跑+证据路径"
- 破坏性选项（`--force`/`--delete`/写系统路径）单独批准，named rollback

## 4. 常见喂法反模式

| 反模式 | 正法 |
|---|---|
| 丢个 URL 让 AI "看着办" | briefing 三拍 + manifest 行先行 |
| 让 AI 全库通读 | 先解剖一页纸，再指哪打哪 |
| 在本地 Mac 直接跑未知二进制 | 容器先行；本地只read |
| "帮我改进这个项目"无边界 | 先定 caps（行数/文件数/不许动的文件清单） |