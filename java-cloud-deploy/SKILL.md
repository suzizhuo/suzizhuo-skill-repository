---
name: java-cloud-deploy
description: Use when a Spring Boot tutorial project goes public for the first time — first cloud server purchase, first SSH, first deploy — or when a Java project that runs in IDEA fails on the server. Triggers: "教程项目部署上线", "Spring Boot 上云", "第一次买服务器", "SSH 连不上", "jar 部署 systemd", "UnsupportedClassVersionError", "数据库连不上", "浏览器打不开", "8080 端口", "备案", "改配置要重新打包吗".
---

# java-cloud-deploy

## Overview

A learner's Spring Boot project goes from IDEA to a public URL — tonight, on their first Linux server, without teaching anti-habits that cost weeks later.

**Core principle: teach the right habit the first time.** Tonight's path may be the simplest one (bare metal + systemd), but every shortcut taken tonight must carry its upgrade path (env vars not baked config, JVM flags not defaults, key login not root passwords) so week two doesn't have to unlearn anything.

**Second principle: calibrate before teaching.** Ask what they've actually done; teach at that level, not at yours.

## When to Use

- "教程项目写完了，IDEA 能跑，想部署到公网" — the full first-launch chain
- Already deployed but broken on the server (404, refused, class version, DB auth)
- Any "下一步正规化" question: config externalization, Docker, HTTPS, backups, CI

Not for: production-grade multi-node architecture (that's a design task); non-JVM stacks.

## Calibrate the learner FIRST (one question, then pick the lane)

| Level | Signals | Teach |
|---|---|---|
| L0 纯小白 | never SSH'd, never touched Linux, "Docker 是什么" | full ladder, 四要素 per step (是什么/为什么/怎么做/怎么验证), expect 2–4 h |
| L1 会命令没部署过 | SSH/scp 会，装过环境，没上过线 | runbook only; skip 是什么/为什么; concepts in one line |
| L2 部署过翻过车 | "之前搭过但卡在…" | troubleshooting first; ladder as reference; cite the red lines |
| L3 要正规化 | running but on baked config/root/password | contracts and checklists only; no step-by-step |

## The Launch Ladder (in order; each gate verifies before moving on)

1. **开服前决策** — vendor/region/os/size from criteria (教学版见 references/first-mile.md). Tonight's rule: region that dodges ICP filing if a domain isn't ready; 2C4G floor for Spring+MySQL; Debian or Ubuntu 22.04 LTS (openjdk-21 in repos). Port plan on paper first: 22, 8080, and 3306 NEVER public.
2. **SSH 首连** — key login from the start (`ssh-keygen -t ed25519`, public key into the panel), public IP from the console, password as fallback not default. Login fails → console web terminal, then check panel firewall for 22.
3. **服务器最小卫生** — non-root user + key-only auth (5 minutes, not "later"), `timedatectl set-timezone Asia/Shanghai`, `LANG=C.UTF-8` (a Chinese-tutorial project breaks in US-ASCII locale), swap if under 4G.
4. **打包并核对** — `mvn package` produces a fat jar (if it's a few KB next to `lib/`, the spring-boot-maven-plugin is missing — check). JDK parity check: `java -version` on the server must be 21, matching the local build (`maven.compiler.release=21`). Then READ the frozen config: `unzip -p app.jar BOOT-INF/classes/application.yml` — the deploy must match what's inside the jar, not memory.
5. **部署形态决策（写下来，不默认）** — bare-metal+systemd (tonight, one service, fastest) / Docker single container (week two, or multi-project) / compose app+db (when db moves into its own managed container). Criteria table in references/deploy-runbook.md. Locking the learner into tonight's shape without naming the next one is a red line.
6. **托管与 JVM 护栏** — systemd unit with `Restart=always`; JVM flags from day one: `-XX:MaxRAMPercentage` (percentage, never a fixed -Xmx) + `-XX:+ExitOnOutOfMemoryError`. Know the denominator: the percentage is of TOTAL machine RAM on bare metal, so on a shared box (MySQL co-tenant) tonight's number is lower (~50% on 4G); 75% is the CONTAINER-limit number with `mem_limit`/`MemoryMax` set. Graceful shutdown pair: `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s` (or env `SERVER_SHUTDOWN=graceful`, relaxed binding, no repackage), so `systemctl restart` doesn't kill in-flight requests.
7. **数据库** — MySQL on the same box, bound to 127.0.0.1, never 3306 public; schema discipline: Flyway/Liquibase or a versioned init.sql, never ddl-auto in production (it silently defaults to `none` outside embedded DBs); backup (`mysqldump`) before any schema change, on day one, not later. Credentials: `mysql_native_password` when the tutorial's old connector errors on `caching_sha2_password`.
8. **配置外置习惯** — jar holds defaults only; deployment differences go to env vars (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_PROFILES_ACTIVE`). Tonight's shortcut ("build the DB on the server to match the yml") is allowed ONLY if the env-var override is taught in the same step — otherwise the learner bakes the repackage-reupload habit.
9. **公网访问与三层验证** — panel firewall AND host firewall both open 8080 (the panel is the outer gate, ufw the inner; the browser-timeout failure is ~90% this). Verify in layers, never jump: ① app process alive ② port answering ③ real HTTP response. IP:port for classmates is fine tonight; domain+HTTPS (Caddy auto-renew, not certbot-once) is the documented next step.
10. **排障按命中率** — symptom table ordered by frequency (references/troubleshooting.md); the universal three: `systemctl status` → `journalctl -n 100` → `java -version`. Logs last 20 lines beat any verbal description.

## Teaching contract (every step, L0-L2)

Each step ships four parts: **是什么**（one line + analogy when the term is new） → **为什么**（which failure forces this step） → **怎么做**（copy-paste command） → **怎么验证**（command + expected output, and for config/override steps a NEGATIVE test too: break it deliberately, watch the expected failure, restore）. A step missing 怎么验证 is not delivered. Full ladder with analogies and common misconceptions: references/teach-ladder.md.

## Red Flags — stop and redo

- "先 root 密码登上去，安全以后再说" → the first public box is when habits form; key login takes 5 minutes
- "服务器照 jar 里的配置建库，最省事" (without adding the env-var move) → teaches config-in-jar forever
- "java -jar 能跑就行" (no JVM flags) → the OOM kill arrives at 2 a.m., not during the tutorial
- "防火墙等访问不通再说" → that's the 90% timeout, and the check is one click
- "SSH 22 端口没人扫" → a first-machine classic: 1M attempts in 3 months is the documented norm
- "先上线，观察日志以后再说" → process alive ≠ app serving; the real-response check is one curl

## Delivery slot (every handoff)

未验证清单（每条带验证命令）+ 信心度 N/5 + 首个验证动作 + 回滚一句。

## References

- `references/first-mile.md` — vendor/region/os decisions, SSH first connect, firewall layers, minimal hardening
- `references/teach-ladder.md` — learner calibration details, the four-part teaching shape, analogy table, misconceptions
- `references/deploy-runbook.md` — jar, systemd, Dockerfile (layered), compose, env-var config map, actuator probes
- `references/troubleshooting.md` — symptom → root cause → fix ordered by hit rate
- `references/evidence-cases.md` — 8 golden cases + 11 failure modes with URLs (Spring official, CalliCoder, Hettler, vendor docs, HN)
- `references/rework-log.md` — RED baseline findings and the red-line table this skill was built against
