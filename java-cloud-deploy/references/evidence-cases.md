# Evidence Cases — Java Cloud Deploy (Spring Boot first launch)

Research date: 2026-11-01. Two lanes: (A) Spring Boot 上云部署实践, (B) 上云第一公里（选购/SSH/安全组）. Method: skill-from-masters. web_search unavailable on this machine; sources via web_fetch + HN Algolia + GitHub API.

## Golden Cases (8)

### 1. Spring 官方 Reference「Container Images → Dockerfiles」— 最权威分层镜像配方
- URL: https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html （入口 …/container-images/index.html）
- What: `java -Djarmode=tools -jar app.jar extract --layers --destination extracted` splits the fat jar into 4 layers (dependencies / spring-boot-loader / snapshot-dependencies / application); multi-stage Dockerfile COPYs each layer separately. Official wording: "Every copy step creates a new docker layer. This allows docker to only pull the changes it really needs".
- Details: three full Dockerfiles (basic / AOT cache / CDS); runtime base image example `bellsoft/liberica-openjre-debian:25-cds` (JRE, not JDK); AOT trained at build time: `RUN java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar application.jar`, entry `java -XX:AOTCache=app.aot -jar application.jar`; "Java 24 以后用 AOT cache 替代 CDS".
- Adjacent pages: 分层说明 https://docs.spring.io/spring-boot/reference/packaging/container-images/efficient-images.html ；免 Dockerfile（buildpacks） https://docs.spring.io/spring-boot/reference/packaging/container-images/cloud-native-buildpacks.html

### 2. Rajeev / CalliCoder「Dockerizing a Spring Boot Application」— 全链路单作者博客（HN 12 pts: https://news.ycombinator.com/item?id=16874347）
- URL: https://www.callicoder.com/spring-boot-docker-example/
- Details that matter:
  - Layer cache: fat jar ~50 MB, one-line change invalidates the whole layer, "every build pushes 50 MB of unchanged dependencies"; after extraction "kilobytes rather than tens of megabytes"
  - JVM: `ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"` — "Leave a quarter for thread stacks, metaspace, code cache and direct buffers"; use JAVA_TOOL_OPTIONS so deployment can override
  - Graceful shutdown: `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s`, and the exec-form ENTRYPOINT is mandatory (shell form leaves sh as PID 1 not forwarding SIGTERM — the most common root cause of "graceful shutdown does not work")
  - Health probes: `management.endpoint.health.probes.enabled=true` splits liveness/readiness; putting the DB check on liveness means "a database blip restarts every replica simultaneously"
  - compose: MySQL 8.4 + `healthcheck: ["CMD","mysqladmin","ping"]` + `depends_on: condition: service_healthy`; short-form depends_on "waits for the container to start, not for MySQL to accept connections" — the cause of "works on the second docker compose up"
  - Non-root: `USER 1001:1001`; JRE base (`eclipse-temurin:17-jre`), Alpine smaller but musl
  - Version traps: `-Djarmode=tools` is Boot 3.3+ (earlier `layertools`); launcher class moved to `org.springframework.boot.loader.launch.JarLauncher` in 3.2

### 3. David Hettler「My Go-To Stack for Web Apps: Spring Boot, React, Postgres, Docker Compose」— 单 VM 真实生产栈（HN: https://news.ycombinator.com/item?id=26054448）
- URL: https://davidhettler.net/blog/go-to-stack-for-web-apps-in-2021/
- Stack: Spring Boot (embedded Tomcat serving React static) + PostgreSQL + Docker Compose + nginx-proxy + Hetzner VM + Ansible init + GitHub Actions deploy
- Details: nginx-proxy auto-routes containers by `VIRTUAL_HOST` env; docker-letsencrypt-nginx-proxy-companion auto-issues/renews certs; publish = `docker-compose pull app && docker-compose restart app` (image built in CI, server only pulls); watchtower for updates, per-container opt-out for the database; Ansible Vault encrypts .env into git
- Numbers: Hetzner 2.96 €/月, 1 vCPU/2 GB/20 GB; "An idle Spring app takes 200-300 MB of RAM"
- Explicit conclusion: "For simple projects and early stage startups Docker Compose really is enough… there's no need to bother with Kubernetes"

### 4. HN「Ask HN: Describe deploy process」— 自建蓝绿 + 旅程测试（公司实践）
- URL: https://news.ycombinator.com/item?id=18791380 （原帖 id=18790477, Concourse 脚本 https://github.com/karmakaze/concourse-ec2 ）
- Details: per-stack blue/green scripts (Spring Boot/Node/Vue/Python); staging → manual trigger to production; RDS PostgreSQL + Liquibase; "Journey Test verifies that the top 10 actions work as designed"; production health check every 10 minutes
- Numbers: top-10 actions as release gate; 10-minute prod health cadence

### 5. 腾讯云轻量应用服务器文档中心 — 新手第一台机的官方口径
- URL: https://cloud.tencent.com/document/product/1207
- Details: 套餐整体售卖基础资源+流量包、热门软件一键构建、明示比普通 CVM "更简单易用、更贴近应用"; 文档树覆盖产品对比/购买指南/防火墙/Linux 登录实例; 登录失败排查页 https://cloud.tencent.com/document/product/1207/44642；公网访问验证 https://cloud.tencent.com/document/product/1207/44577

### 6. Debian 官方《选择 Debian 的理由》中文页
- URL: https://www.debian.org/intro/why_debian.zh-cn.html
- Details: 每包合理默认配置+常规安全更新、7 万软件包、平滑升级、LTS≥5 年、主流云平台有官方云映像、社区治理不受单一公司控制; 教程多按 Debian 写，apt 命令可照抄

### 7. HN 内存基准实测 — 1C2G 跑不动 Spring Boot + MySQL
- URL: https://news.ycombinator.com/item?id=45967814
- Details: 开发者亲述服务各占 512 MB–2 GB（Java/Spring Boot 所致），同开几个就耗尽 → 起点配置要 2C4G 或先加 swap

### 8. HN 经典「3 个月 100 万次 SSH 尝试」— 22 端口公网暴露即被爆破
- URL: https://news.ycombinator.com/item?id=11325752
- Details: 第一台机就该密钥登录、禁密码、必要时改默认端口

## Failure Modes (11)

### 1. ExitCode=137 / OOMKilled（容器里 Java）
- Symptom: hours later `docker inspect` shows `State.ExitCode=137`; cgroup OOM kill in `docker events`/`dmesg`; app heap only half used
- Root cause: (a) pre-JDK10 JVM reads host total memory, ignores cgroup; (b) even container-aware JVMs: heap ≠ total ("total JVM memory is heap+permgen+threads*stacksize") — MaxRAMFraction=1 eats thread stacks/metaspace/code cache/direct buffers and still gets killed; (c) fixed `-Xmx` does not follow container limit changes ("the two disagreeing is how you get an OOM kill at 60% heap usage")
- Fix: percentage not absolute — `JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"`, explicit `mem_limit` in compose; ExitOnOutOfMemoryError lets the platform restart it (otherwise "a JVM that has exhausted its heap often limps along serving errors while still passing a TCP health check")
- URLs: https://www.callicoder.com/spring-boot-docker-example/ ; https://news.ycombinator.com/item?id=16635882

### 2. IDEA 里正常，上云 404 / 连接被拒
- Root cause: port/listener. Default 8080 override via `server.port`/`SERVER_PORT` (relaxed binding); the classic self-own is `server.address=127.0.0.1` (a local debug habit) — in a container that listens on loopback only; other half: compose `ports` source:target written backwards
- Fix: never set `server.address` in the container; publish only what the reverse proxy needs; bind docker `127.0.0.1:8080:8080` so only Caddy/nginx enters from the host loopback; self-check inside the container `curl -f http://127.0.0.1:8080/actuator/health`, then from the host through the proxy
- URLs: https://docs.spring.io/spring-boot/how-to/webserver.html ; https://www.callicoder.com/spring-boot-docker-example/
- Honest gap: firewall/SELinux docs fetch failed; on Debian expect ufw/nftables + cloud security group + Docker's DOCKER chain (no SELinux on Debian — RHEL-family thing). Not first-party verified, flagged.

### 3. 配置改不动 / 改了不生效
- Root cause: config data order. Load order (later overrides earlier): ① in-jar application.* ② in-jar profile ③ **outside-jar** application.* ④ outside-jar profile. In-jar is only defaults — deployment differences belong in env vars or outside files. Reverse trap: `spring.config.location` REPLACES defaults, and a missing location without `optional:` throws `ConfigDataLocationNotFoundException` and the app does not start
- Fix: one image, all environments — `SPRING_DATASOURCE_URL`, `SPRING_PROFILES_ACTIVE` etc.; external files via `spring.config.additional-location` (additive) or `spring.config.import=optional:file:/run/secrets/`; diagnose with `/actuator/env`, `/actuator/configprops`
- URLs: https://docs.spring.io/spring-boot/reference/features/external-config.html

### 4. MySQL 8 容器起不来 / 连不上（部分未验证）
- Explicitly unverified (fetch failures: hub.docker.com/_/mysql, dev.mysql.com 403, docker-library raw 404): root auth plugin switched to `caching_sha2_password` (old drivers error); `MYSQL_ROOT_PASSWORD`/`MYSQL_DATABASE`/`MYSQL_USER` apply only when the data dir is EMPTY — re-setting them on an existing volume changes nothing
- Verified part: use Flyway/Liquibase for schema ("Using the basic schema.sql and data.sql scripts alongside Flyway or Liquibase is not recommended…"); default traps: `spring.jpa.hibernate.ddl-auto` defaults `create-drop` only for embedded DBs without Flyway/Liquibase, otherwise `none`; `spring.sql.init.mode` default embedded-only — H2→MySQL switch silently creating no tables
- URL: https://docs.spring.io/spring-boot/how-to/data-initialization.html

### 5. UnsupportedClassVersionError（本地 21 / 服务器 17）
- Root cause: compile-time vs runtime JDK mismatch; framework minimums too — "Spring Boot 4.1.2 requires at least Java 17 and is compatible with versions up to and including Java 27" (Maven 3.6.3+, Gradle 8.14+)
- Fix: unify the build — builder stage in Docker with a pinned JDK image; local Mac only edits code; `maven.compiler.release=21` (not just source/target); server runs JRE containers only, no system JDK
- URL: https://docs.spring.io/spring-boot/system-requirements.html

### 6. 中文乱码 / 时间差 8 小时
- Root cause: (a) JDK 18+ defaults UTF-8 (JEP 400); ≤17 follows OS locale — containers often sit in C/POSIX so `Charset.defaultCharset()` lands on US-ASCII; on ≤17 `-Dfile.encoding=UTF-8` is the supported switch (the property-setting route "has never been supported"); javac needs `-encoding UTF-8`; (b) Spring Boot imports `application.properties` as ISO-8859-1 by default (YAML is not affected); (c) TZ mismatch across container/DB/driver
- Fix: explicit `LANG=C.UTF-8` / `-Dfile.encoding=UTF-8` + `TZ` in the container; prefer YAML; JDBC URL serverTimezone + Jackson zone; ISO-8601 timestamps in logs
- Unverified: JDBC `serverTimezone` param name and MySQL container TZ env — flagged
- URLs: https://openjdk.org/jeps/400 ; https://docs.spring.io/spring-boot/reference/features/external-config.html

### 7. 改一行 → 全量重下依赖 / 推送 50 MB
- Root cause: fat jar is one ~50 MB layer (code is kilobytes, the rest is Spring/Hibernate/Jackson); `COPY . .` before `COPY pom.xml` rebuilds the Maven repo every time
- Fix: `COPY pom.xml` → `mvn -B dependency:go-offline` → `COPY src`; jarmode 4-layer COPY ordered by change frequency; or `./mvnw spring-boot:build-image` (buildpaks: layered, non-root, container-aware heap calculator)
- URLs: https://www.callicoder.com/spring-boot-docker-example/ ; https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html

### 8. 证书三个月后过期 / 重建容器撞 Let's Encrypt 限流
- Root cause: renewal treated as a one-time action; cert storage in the container writable layer (rebuild = new identity = re-issue)
- Fix: auto-renewing component — Caddy "keeps all managed certificates renewed" and requires a "writeable and persistent" data directory, retries with CA fallback and switches to the ACME staging endpoint to dodge rate limits; or nginx-proxy + letsencrypt companion; test against `acme-staging-v02`
- URLs: https://caddyserver.com/docs/automatic-https ; https://davidhettler.net/blog/go-to-stack-for-web-apps-in-2021/

### 9. 买了服务器连不上 / SSH 被拒
- Root cause: security group doesn't allow 22, or the internal IP was used; or server only accepts keys while the local key isn't generated/bound
- Fix: public IP from the console; allow 22 + 8080 in the panel; fallback via the vendor's web terminal; `ssh-keygen -t ed25519`, bind the public key in the panel, `chmod 600 ~/.ssh/id_ed25519`
- URLs: https://cloud.tencent.com/document/product/1207/44642 ; https://news.ycombinator.com/item?id=8743374

### 10. 1C2G 卡死，SSH 都进不去
- Root cause: JVM+MySQL exceeds 1 GB, cgroup OOM killer, swap exhausted
- Fix: start at 2C4G; `-Xmx512m` cap; add 2G swap before optimizing
- URL: https://news.ycombinator.com/item?id=45967814

### 11. 22 端口公网爆破
- Root cause: SSH open to the internet with password auth
- Fix: key-only login from day one; disable password auth; consider a non-default port
- URL: https://news.ycombinator.com/item?id=11325752

## Contrast Table

| Do this | Not that | Why |
|---|---|---|
| `JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"` + compose `mem_limit` | hardcode `-Xmx2g` in the Dockerfile, or 90%+ RAM percentage | fixed heap doesn't follow container limit; heap isn't the JVM's whole footprint |
| `COPY pom.xml` → `dependency:go-offline` → `COPY src` + jarmode 4-layer COPY | `COPY . .` then `mvn package`, or copy the built jar directly | fat jar = one 50 MB layer; one change = full push |
| defaults in the image, differences via `SPRING_*` env vars; external files via `additional-location` / `optional:file:` | bake prod yml into the jar; use `spring.config.location` without `optional:` | in-jar is defaults only; `location` REPLACES defaults and a missing path without `optional:` is a startup crash |
| exec-form ENTRYPOINT + `server.shutdown=graceful` + 30s timeout | shell-form ENTRYPOINT, or no graceful shutdown | sh as PID 1 swallows SIGTERM — the #1 "graceful shutdown doesn't work" cause |
| db healthcheck (`mysqladmin ping`) + `depends_on: condition: service_healthy`; schema via Flyway/Liquibase | short-form depends_on; `sql.init.mode=always` + ddl-auto | short form waits for start, not for "accepts connections"; init defaults are embedded-only |
| CI builds images; server pulls: `docker-compose pull && restart` | local jar → scp → build on the server; system JDK on the server | compile/run JDK drift → UnsupportedClassVersionError; server-as-JRE-only removes the variable |
| explicit `LANG=C.UTF-8` / `-Dfile.encoding=UTF-8` + `TZ`; prefer YAML | inherit host locale; Chinese text in application.properties | C/POSIX locale → US-ASCII charset; properties imports as ISO-8859-1 |
| Caddy / nginx-proxy+companion auto-renewal with persistent data dir; test on ACME staging | issue once and forget; certs in the container writable layer | rebuild = re-issue = LE rate limits |
| liveness vs readiness probes split | one `/actuator/health` for both | a DB blip on liveness restarts every replica at once |
| single VM + compose ("一份镜像+一个反代+一个 compose 文件"跑稳) | K8s/Helm from day one | "Docker Compose really is enough… no need to bother with Kubernetes"; 2.96 €/月 1C2G Hetzner carried this stack |
| light application server (panel firewall, bundled plans) for the first machine | full ECS/VPC for a tutorial project | the panel is the outer gate; ufw the inner — both layers, one machine |

## Declared Gaps (not fabricated)

- MySQL Docker docs: hub.docker.com/_/mysql fetch failed, dev.mysql.com 403, docker-library raw failed → the caching_sha2_password detail and "env applies only on empty data dir" are unverified
- DigitalOcean initial-server-setup + ufw pages truncated by nav chrome; Mac first SSH command details and a full ufw rule list unverified
- Domestic vendor real-time pricing and Debian 12 cloud image specifics not retrieved (no web_search key)
- HN results skew English-community
