# Rework Log — java-cloud-deploy

Append-only failure ledger. New iterations append; never rewrite history.

## RED Baseline (2026-11-01, subagent WITHOUT skill)

Scenario brief given verbatim (user's real words):

> "我 Java 教程项目写完了，IDEA 里能跑通。想部署到公网让我同学能访问。我没买过服务器，没用过 Linux，没用过 Docker，SSH 只是听说过。项目是 Spring Boot + MySQL（黑马教程那种），本地 JDK21。本机是老 Intel Mac。给我一套从买服务器开始、到浏览器能打开我项目的完整步骤，别跳步，我看不了太简的。"

Artifacts: `/tmp/red-java-deploy/` (00-06 learner tutorial + server-setup.sh). Route chosen by baseline: 腾讯云轻量香港 / Ubuntu 22.04 / 2C4G / apt JDK21 + apt MySQL 同机 / scp jar / systemd 托管 / IP:8080。**Deliberately no Docker** ("你没用过，今晚不上容器，别自己加戏").

### What the baseline already got right (keep, adopt as teaching moves)

| Move | Evidence |
|---|---|
| 拍板式开头 | "关键决策（我替你拍的板，不商量）"：厂商/地域/系统/规格/MySQL/端口/进程管理，每条带理由 |
| 三条铁律前置 | 防火墙九成、库名密码一字不差、JDK 必须 21 |
| 教学四要素雏形 | 每步命令 + ✅预期输出 + ❌失败分支；"解释三件事（新手照做即可，想知道为什么再看）" |
| 排错表按命中率 | 05 表 14 条按占比排序 + 万能三步 |
| 时区 | `timedatectl set-timezone Asia/Shanghai`（3.1） |
| 认证插件预堵 | `mysql_native_password` 主动规避 caching_sha2_password（3.3.1，带解释） |
| **读 jar 内配置** | `unzip -p app.jar BOOT-INF/classes/application.yml` 亲眼核对（回环了"改配置要重打包"） |
| 港机绕备案 | 大陆绑域名要备案几周，香港当晚可访问——决策理由写清 |
| 手动先跑再托管 | 4.1 先 `java -jar` 验证 jar 本身能起，再上 systemd |

### Red-line table — gaps the skill must close

| # | Baseline behavior (verbatim) | Why it fails later | Skill makes you |
|---|---|---|---|
| RL1 | 部署形态只有一条路且无反顾：Docker 全程缺席，4.5 只留一句"以后项目多了再用" | 第二项目上线时零积累；配置/回滚/迁移全部从零 | 形态决策表（systemd tonight / Docker 升舱 / compose 编排）+ 每条路的迁移信号写进交接单 |
| RL2 | JVM 裸跑：`ExecStart=/usr/bin/java -jar`，无任何内存护栏 | 2C4G 上默认堆+MySQL 打架，OOM 是时间问题；瘸腿 JVM 还假装健康 | 第一天就带 `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` 并讲为什么 |
| RL3 | 无优雅停机；`Restart=always` + `SuccessExitStatus=143` 只是进程层 | 每次发版都在请求中途杀进程 | `server.shutdown=graceful` + 30s 超时，exec 形式启动 |
| RL4 | 健康判据只到进程/端口层，无探针概念，无真实响应层 | "active (running)" ≠ 应用能服务（案例：常量 200 的谎） | 三层验证（进程→端口→真实响应）+ actuator probes 分工 |
| RL5 | "服务器上照着你 jar 里 application.yml 的库名/用户名/密码建，不改代码，最少动作" | 教出"配置焊 jar、改一次重打包"的永久习惯；研究对比表明确反对 | 允许今晚省事，但同一步必须写 `Environment=SPRING_DATASOURCE_*` 示范覆盖 |
| RL6 | schema 靠手工 init.sql，ddl-auto/Flyway 全程缺席；备份留到 4.5 | 第一次改表就是第一次数据事故；H2→MySQL 默认值坑没人讲 | Flyway/版本化迁移 + 变更前 mysqldump 是第一天习惯 |
| RL7 | SSH root 密码登录，加固全推"以后"（4.5） | 公网 22 持续爆破是常态；第一台机的习惯跟一辈子 | 密钥登录+禁密码是第 2/4 步的 5 分钟动作，不是 later |
| RL8 | 有时区无 locale：`LANG`/`file.encoding` 缺席 | 中文教程项目在 US-ASCII locale 下必出乱码 | `LANG=C.UTF-8` + `-Dfile.encoding=UTF-8` 进 unit/容器 |
| RL9 | 域名/HTTPS/Caddy 只在 4.5 提了一句 | 三个月证书过期全挂是高频事故 | HTTPS 下一步写清楚走自动续期组件，不用一次签 |
| RL10 | 教学密度固定 L0，无分级 | L1/L2 用户被当小白慢速摩擦；L3 无通道 | 四要素固定形状 + 四级认知（L0-L3）分级信号与话密切换 |

### Rationalization red flags (self-check phrases seen or expected)

- "先 root 密码登上去了" → 爆破不管你是不是第一台机
- "今晚能跑就行，配置以后外置" → 以后永远不会来
- "java -jar 能起就是成功了" → 进程绿、应用死是 documented 的谎
- "防火墙等打不开再说" → 那是九成命中率的第一个坎
- "数据库同机开着没关系" → 关系在 3306 公网那一刻
- "备份明早搞" → 今晚第一次改表就是事故

## GREEN (2026-11-01, two subagents WITH skill, fresh context)

### GREEN-1 — same brief re-run → COMPLIANT + corrected the skill

- L0 calibration with an evidence table (every signal quoted from the user's own words), 13 files one per station + handoff, four-element per station grep-verified 1:1:1:1
- Guardrails present in the unit: `-XX:MaxRAMPercentage` + `ExitOnOutOfMemoryError` + `-Dfile.encoding=UTF-8`, reasoning table per flag
- **GREEN-1 corrected the skill**: the blanket "75%" was wrong for bare metal — `MaxRAMPercentage`'s denominator is TOTAL machine RAM, so on a 4G box with MySQL co-tenant tonight's number is ~50%; 75% is the number for a container WITH `mem_limit`. Adopted into SKILL.md step 6 and deploy-runbook; the mental model ("percentage, know the denominator") is the transferable lesson
- Station reorder for tonight: pack & reconcile jar → create DB → then systemd (the unit's `After=mysql.service` and env want the DB facts first). Noted in runbook §1.3 comments

### GREEN-2 — week-two hard scenario → COMPLIANT + deepened four rules

- L2 calibration stated with its evidence; taught at L2 density (no 是什么) as the contract requires
- Priorities: P0 SSH brute force (verify key in a NEW window before closing the old one, then disable password auth) → P1 charset (dump FIRST; HEX-byte triage) → P2 env externalization + MemoryMax + graceful + encoding merged into ONE unit rewrite/restart → P3 project 2 into Docker per the decision table, MySQL staying on the host (risk-ranked decision, declared)
- Four-part delivery with 10-item unverified list, D1–D8 declared decisions, "今晚不做" list with re-entry signals
- Adopted into the skill (each origin noted):
  - **Negative verification**: deliberately break the password → expect `Access denied` → restore → expect healthy. Config chains get proven, not assumed
  - **Charset HEX triage**: `E4..` healthy-at-write / `3F3F` irreversible / `C3A5..` double-encoded repairable — decide before touching anything; JDK21 default UTF-8 (JEP 400) means don't blame locale reflexively
  - **Relaxed binding** `SERVER_SHUTDOWN=graceful` — graceful shutdown without repackaging
  - **MemoryMax interplay** — systemd `MemoryMax` is the outer cap; the JVM percentage reads the cap, not physical RAM
  - **Conditional promotion** — "later" items (fail2ban, port change) promote to "tonight" when a brute-force signal arrives

### Regression (post-REFACTOR, 2026-11-01) — PASS

Fresh hard scenario (second project onto the same box + memory math + prove the env chain + charset layer diagnosis), subagent WITH the updated skill. Every adopted rule fired, and three were upgraded:

- **Caps-first percentage**: "帽就是分母" — name the cap (`MemoryMax`/`--memory`), THEN the percentage (75% with a roomy cap, 60% when co-tenants share); a full 4G budget table with MySQL RSS measurement-before-tuning
- **Two-break negative verification**: password AND url broken separately, expected failure lines quoted per break, `systemctl show -p Environment` as the sole arbiter, three-result triage, trap-protected restore; plus the `%`→`%%` / `$`→`$$` unit escaping trap
- **Charset probe without touching anything**: five-layer read-only diagnosis; JEP 400 cited so locale stops taking all the blame; console exception noted
- Extras adopted: host-network vs bridge tradeoff with explicit switching conditions; OOMKill four-command investigation; swap converts kill to stall

Iteration summary: RED baseline = competent tutorial with anti-habits embedded → 10 red lines → GREEN×2 compliant + corrections (denominator rule) → regression PASS.
