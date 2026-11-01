# Troubleshooting — Java Cloud Deploy（按命中率排序）

## 访问层

| # | 症状 | 根因 | 修法 |
|---|---|---|---|
| 1 | 浏览器超时/打不开 | **外层防火墙（控制台）没放行 8080**——约九成 | 面板放行 TCP 8080；再查 `ufw status`（active 则 `ufw allow 8080`） |
| 2 | 你能开、同学打不开 | 校园网/运营商策略，或他连错地址 | 让同学 4G 试；确认给的是 `http://IP:8080` 不是 https |
| 3 | connection refused | 程序没在跑 / 监听不对 | `systemctl status myapp` + `journalctl -u myapp -n 50`；程序内 `curl -f 127.0.0.1:8080` 验监听 |
| 4 | 起着但 404 | yml 有 `server.servlet.context-path` 或静态首页没打进去 | `unzip -p app.jar BOOT-INF/classes/application.yml` 看 context-path 拼地址 |

## 进程/启动层

| # | 症状 | 根因 | 修法 |
|---|---|---|---|
| 5 | `UnsupportedClassVersionError ... version 65.0` | 服务器 JDK ≠ 本地 JDK（21 编 17 跑） | `java -version` → `apt install -y openjdk-21-jdk` → restart。根治：构建进容器或 `maven.compiler.release` 锁版本 |
| 6 | 起几秒就退，日志 bean 失败关键词 redis/oss/upload | 教程项目不止 MySQL，还挂着本地才有的依赖 | 服务器装 redis 并改 yml host；或注释该依赖重打包；上传目录 `mkdir -p` |
| 7 | `Port 8080 was already in use` | 端口被占 | `ss -lntp \| grep 8080` 杀进程；或改 server.port 重打包 |
| 8 | `Access denied for user` | 服务器密码 ≠ jar 内 yml | 重设对齐（§数据库）；用 `unzip -p` 亲眼核对 jar 内那份 |
| 9 | 内存型间歇死亡，`docker inspect` ExitCode 137 / dmesg OOM kill | JVM 与机器/容器内存打架；JVM 瘸着跑还过健康检查 | `-XX:MaxRAMPercentage`（带帽容器 75%，共机裸机按帽的比例再压）+ `-XX:+ExitOnOutOfMemoryError`；小机加 2G swap 把秒杀变卡顿。排查四连：`journalctl -k \| grep -i kill` → `dmesg \| grep -i oom` → `systemctl status` 看谁 137 → 按 deploy-runbook §8 重算预算 |
| 10 | 每次 restart 都丢进行中的请求 | 没配优雅停机 | `server.shutdown=graceful` + 30s 超时；exec 形式启动 |

## 数据库层

| # | 症状 | 根因 | 修法 |
|---|---|---|---|
| 11 | `Communications link failure` | MySQL 没起 | `systemctl enable --now mysql` → restart myapp |
| 12 | `Authentication plugin 'caching_sha2_password' cannot be loaded` | 老连接器撞 MySQL8 默认加密 | `ALTER USER ... IDENTIFIED WITH mysql_native_password` |
| 13 | `Unknown database` | 库没建 / 差字母 | 建库对齐；`SHOW DATABASES` 核 |
| 14 | 本地有表、上线查无表 | `ddl-auto` 默认非内嵌库为 none；`sql.init.mode` 默认只内嵌生效 | Flyway/Liquibase 或版本化 init.sql；别拿 H2 的默认行为推 MySQL |
| 15 | Docker MySQL 改了 root 密码不生效 | `MYSQL_*` 只在数据卷空时初始化 | 删卷重来，或在线 ALTER USER；备份先做 |

## 环境层

| # | 症状 | 根因 | 修法 |
|---|---|---|---|
| 16 | 中文 `??`/乱码 | locale 非 UTF-8，JDK17- 的 file.encoding 跟着 locale 走 | `LANG=C.UTF-8` + `-Dfile.encoding=UTF-8`；库表 utf8mb4；properties 里的中文优先挪 YAML。分级诊断（GREEN-2 收编，改任何东西前先做）：`SELECT HEX(列)` 看字节——`E4..E9..` 入库时健康（坏在读取层）；`3F3F` 已是问号不可逆，只能重录；`C3A5..` 双重编码，可脚本修复。JDK21 已默认 UTF-8（JEP 400），别把锅全甩 locale；动库前先 `mysqldump` |
| 17 | 时间差 8 小时 | 系统 TZ=UTC；DB/驱动时区不一致 | `timedatectl set-timezone Asia/Shanghai`；JDBC URL 带 serverTimezone |
| 18 | 证书三个月后全挂 | 手工签发无续期 | Caddy 自动续期 + 持久 data 目录；测试走 staging 端点 |
| 19 | 流水线重推 50MB 慢 | fat jar 单层，`COPY . .` 在前 | 拆层 + `COPY pom.xml` 先走 go-offline |

## SSH/系统层

| # | 症状 | 根因 | 修法 |
|---|---|---|---|
| 20 | 登录超时/被拒 | 面板没放行 22；公钥没绑；填了内网 IP | 面板放行 22；`ssh-keygen` 绑公钥；用公网 IP；兜底走厂商网页终端 |
| 21 | `Permission denied (publickey)` | 服务器只认密钥 | 面板绑公钥；或临时开密码登录补配 |
| 22 | SSH 密码总错 | 记错/实例被锁 | 控制台重置密码 |

## 万能三步（任何诡异问题）

```bash
systemctl status myapp        # 1. 活着吗
journalctl -u myapp -n 100    # 2. 死前最后说了什么（日志最后 20 行定位 80%）
java -version                 # 3. 版本对吗
```

把这三条输出贴出来，比"打不开"三个字有用一万倍。
