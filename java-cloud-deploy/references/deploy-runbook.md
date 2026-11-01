# Deploy Runbook — 打包、托管、Docker 升舱、配置外置

Commands assume Ubuntu 22.04/Debian 12 on the server, Mac as the client. Lanes: §1 bare-metal+systemd (tonight) · §2 Docker 升舱 (week two) · §3 配置外置 (habit, both lanes) · §4 数据库 · §5 监控/健康 · §6 域名 HTTPS · §7 发版循环.

## 1. 打包与托管（今晚路径）

### 1.1 Mac 打包

```bash
cd ~/IdeaProjects/your-project
mvn -q clean package -DskipTests
ls -lh target/*.jar
```

- 如果 `target/*.jar` 只有几 KB 且旁边有 `lib/`：缺 `spring-boot-maven-plugin`，pom 补：

```xml
<build><plugins><plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
</plugin></plugins></build>
```

- `pom.xml` 锁版本口径：`<maven.compiler.release>21</maven.compiler.release>`（不是只设 source/target）。

### 1.2 核对 jar 内焊死的配置（上传前最后一道）

```bash
unzip -p target/your-app.jar BOOT-INF/classes/application.yml
```

把 datasource url / username / password / server.port / context-path 抄下来——服务器一切部署以这份为准，不凭记忆。改了配置必须重新打包，或按 §3 用环境变量覆盖。

### 1.3 上传与 systemd 托管

```bash
scp target/your-app.jar root@公网IP:/opt/myapp/app.jar
ssh root@公网IP "mkdir -p /opt/myapp"
```

服务器上（先手动 `java -jar app.jar` 验证能起，见 SKILL.md ladder step 6 的三层验证，再托管）：

```bash
cat > /etc/systemd/system/myapp.service <<'EOF'
[Unit]
Description=myapp
After=network-online.target mysql.service
Wants=network-online.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java \
  -XX:MaxRAMPercentage=75 \
  -XX:+ExitOnOutOfMemoryError \
  -Dfile.encoding=UTF-8 \
  -jar /opt/myapp/app.jar
Environment=SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/appdb?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=SERVER_SHUTDOWN=graceful
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload && systemctl enable --now myapp
systemctl status myapp          # active (running) = 托管层绿
```

护栏三件套的来历（教学时说"为什么"）：`-XX:MaxRAMPercentage` 是百分比不是绝对值——**分母是整机总内存**，所以裸机上 MySQL 要分一份时今晚压到 50%（4G 机），75% 是容器里带 `mem_limit`/`MemoryMax` 时的口径（那时分母才是"分给你的那一份"）；固定 `-Xmx` 与机器打架就是在 60% 堆用量时被 OOMKill。`ExitOnOutOfMemoryError` 让堆耗尽即退出、交给 systemd 重启（否则 JVM"瘸着跑"还假装健康）。`-Dfile.encoding=UTF-8` 堵 JDK17 及以下 locale 乱码（JEP 400：https://openjdk.org/jeps/400 ）。给 systemd 加 `MemoryMax=` 时它是外层帽，JVM 的 75% 读的是这个帽，不是物理内存。
`SERVER_SHUTDOWN=graceful` 走 relaxed binding，不用改 yml 重打包就拿到优雅停机。

优雅停机（应用侧，改一次就行）：

```yaml
# application.yml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

不然每次 `systemctl restart` 都在请求中途杀进程。

## 2. Docker 升舱（第二周 / 多项目时）

决策表（写进交接单，不默认）：

| 信号 | 选择 |
|---|---|
| 单项目、单机、今晚就要 | systemd（§1） |
| 第二个项目要上线 / 要干净回滚 / 要带上 MySQL redis 一条栈 | Docker 单容器 |
| app+db+反代一套编排、迁机器只带一个目录 | compose app+db |

### 2.1 多阶段 + 拆层 Dockerfile（Spring 官方口径，来源 docs.spring.io 容器镜像章节）

```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B dependency:go-offline          # 依赖层只在 pom 变时失效：最大构建提速点
COPY src ./src
RUN mvn -B clean package -DskipTests
RUN java -Djarmode=tools -jar target/*.jar extract --layers --destination extracted   # Boot 3.3+ 形式

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/extracted/dependencies/ ./
COPY --from=build /src/extracted/spring-boot-loader/ ./
COPY --from=build /src/extracted/snapshot-dependencies/ ./
COPY --from=build /src/extracted/application/ ./
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
USER 1001:1001
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

要点：改一行代码只失效 application 层，推送从 50MB 变 KB 级；JRE 不是 JDK；exec 形式 ENTRYPOINT（shell 形式下 sh 是 PID 1 不转发 SIGTERM，优雅停机失效——callicoder 记录的最常见根因）；Boot 3.2+ launcher 类是 `org.springframework.boot.loader.launch.JarLauncher`，旧全限定名 ClassNotFound。

### 2.2 compose app+db（带 healthcheck 的依赖）

```yaml
services:
  app:
    build: .
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/appdb?...
      SPRING_DATASOURCE_USERNAME: appuser
      SPRING_DATASOURCE_PASSWORD: ${DB_PASS}
    depends_on:
      db: { condition: service_healthy }     # 短形式只等"启动"，不等"能连"——第二次 up 才好的成因
  db:
    image: mysql:8.4
    environment: { MYSQL_ROOT_PASSWORD: ${DB_PASS}, MYSQL_DATABASE: appdb }
    volumes: [dbdata:/var/lib/mysql]
    healthcheck: { test: ["CMD","mysqladmin","ping","-h","127.0.0.1"], interval: 10s, retries: 5 }
volumes: { dbdata: }
```

Docker 版配置与密钥：`SPRING_DATASOURCE_PASSWORD: ${DB_PASS}` 走 .env（env_file 会进 docker inspect——见 docker-agent-ops RL4）；MySQL 的 `MYSQL_*` 只在数据卷为空时初始化，改它要先删卷。

## 3. 配置外置（一条镜像跑所有环境的习惯）

加载顺序（后者覆盖前者）：① jar 内 application.* → ② jar 内 profile → ③ **jar 外** application.* → ④ jar 外 profile。jar 内只是默认值。

| 部署差异 | 正确姿势 | 反模式 |
|---|---|---|
| 数据库/密码 | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` 环境变量 | 打进 jar，改一次重打包一次 |
| 不同环境 | `SPRING_PROFILES_ACTIVE=prod` | 多份 yml 手工换 |
| 外挂 yml | `spring.config.additional-location=file:/opt/myapp/`（叠加）或 `spring.config.import=optional:file:...` | `spring.config.location` 直接替换默认位置；少 `optional:` 前缀时文件不存在直接启动失败 |
| 排"这属性哪来的" | `/actuator/env`、`/actuator/configprops` | 猜 |

今晚铁律：用"服务器照 jar 建库"省事可以，但**同一步必须把 `Environment=SPRING_DATASOURCE_*` 这行写进 systemd unit 并示范一次覆盖**，否则习惯焊死。

坐实覆盖链（负面验证，GREEN-2/回归收编，一次 3 分钟）：唯一判官先跑 `systemctl show myapp -p Environment`（env 真进了这个进程吗）；然后**两次独立破坏**：① 密码末位改错 → 必须 `Access denied for user`；② URL 端口 3306→3307 → 必须 `Communications link failure`——两行分别出现，证明密码和 URL 各自独立生效；改回两个都看到 `Started XxxApplication` + HTTP 200。三果分诊：如期坏=链坐实；**不坏=env 没进进程**（daemon-reload 漏了/文件写错/变量名拼错，概率最大）；坏得不一样=值指错层。unit 值转义：`%`→`%%`、`$`→`$$`。此后改配置 = 改一行 + restart，不用重新打包。

## 4. 数据库

- MySQL 只听 127.0.0.1，不开公网（防火墙也不放行 3306）
- 连接器报 `Authentication plugin 'caching_sha2_password' cannot be loaded`（老教程连接器撞 MySQL8 默认加密）：`ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '...'`
- Schema：Flyway/Liquibase 或版本化 init.sql；**不要 ddl-auto 当迁移**——它只在识别为内嵌库时默认 create-drop，其余默认 none，本地 H2 上线 MySQL 后"表不建"就是这个默认值（https://docs.spring.io/spring-boot/how-to/data-initialization.html ）
- 任何变更前 `mysqldump appdb > backup-$(date +%F).sql`；第一天就验证备份能跑

## 5. 监控与健康探针

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true        # 分出 liveness / readiness
```

规矩：liveness 只放"要不要重启我"（进程内部状态），readiness 放"要不要给我流量"（含 DB/下游）。把 DB 检查挂 liveness 上，数据库抖一下所有副本同时重启。
systemd 路径的健康判据 = 三层：进程（systemctl status）→ 端口（curl 127.0.0.1:8080）→ **真实响应**（200 + 业务页面）。只有第一层绿不算绿。

## 6. 域名 + HTTPS（下一步，不是今晚）

买域名 → A 记录到 IP → Caddy 反代（自动签发+续期，data 目录必须持久）→ 80/443 面板放行。不要 certbot 手工签发一次放着（三个月后过期全挂）；测试时用 ACME staging 端点避开 Let's Encrypt 限流（https://caddyserver.com/docs/automatic-https ）。

## 7. 发版循环

```bash
# Mac: mvn package → scp
scp target/your-app.jar root@公网IP:/opt/myapp/app.jar.new
# 服务器：备份当前 jar + 原子替换 + 重启 + 看日志确认 Started
ssh root@公网IP "cd /opt/myapp && cp app.jar app.jar.bak && mv app.jar.new app.jar && systemctl restart myapp && sleep 8 && journalctl -u myapp -n 30 --no-pager"
```

判据：日志出现 `Started ... in x.xxx seconds` 且三层验证过。回滚 = `mv app.jar.bak app.jar && systemctl restart myapp`。Docker 路径：CI 构建镜像打日期 tag，服务器只 `docker compose pull && up -d`（别在服务器上 build——JDK 漂移是 UnsupportedClassVersionError 的根）。

## 8. 内存预算（多app同机，回归收编的分配骨架）

口径：**先戴帽再谈百分比——帽就是分母**。`MemoryMax`/`--memory` 之后 JVM 读的是那个帽，不是物理内存；因此带帽容器里 75%（留 25% 给线程栈/metaspace/直接缓冲），帽小或 co-tenant 多时压到 60%。

| 占用方（4G 机示例） | 配额 | JVM 口径 |
|---|---|---|
| app1（systemd） | `MemoryMax=1200M` | `-XX:MaxRAMPercentage=60` |
| app2（Docker） | `--memory=1200m` | 同 flag |
| MySQL | 先不调参 | 128M buffer pool 的 RSS 实测常 300–450M，先测再动 |
| 系统+余量 | 剩余 ~900M | 尖峰/log/sshd/docker daemon 的缓冲带 |

小机（<4G）加 2G swap：把 OOMKill（秒杀）变成卡顿（可登进去救）。OOMKill 排查四连：`journalctl -k \| grep -i kill` / `dmesg \| grep -i oom` / `systemctl status` 看 ExitCode / 想清楚是谁的 137。

## 9. 容器连宿主机 MySQL：host 网络 vs 桥接（回归收编）

要连只听 127.0.0.1 的宿主机 MySQL：最快是 `--network host` + `SERVER_PORT=8081`（不动 bind-address），代价是学不到 `-p` 端口语义；切桥接的条件（需要多容器互通/端口隔离/上 compose 编排）写进交接单。MySQL 迁进容器是独立战役，别和"上第二个项目"混做。
