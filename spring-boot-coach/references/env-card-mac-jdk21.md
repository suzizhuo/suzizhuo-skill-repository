# 环境卡 — 老 Intel Mac / JDK 21 / Maven / IDEA

伴读与问诊的一切命令都要能在这台机器上真跑。老机器（macOS 12.6、Intel、慢）是第一约束。

## 基线（开工先验证一次）

```bash
java -version          # 期望 21.x
mvn -v                 # 期望 Maven 3.9+，JDK 21
sw_ver                 # 12.6
bash --version | head -1   # 3.2.57（别用 GNU-only 脚本）
```

版本记录进卡里；`mvn -v` 的 Java runtime 列出的必须是 21（IDEA 里 Embedded Maven 用的 JDK 以 Settings > Build Tools > Maven 为准，不是 shell 里的）。

## 慢机器已知坑与对策

| 坑 | 现象 | 对策 |
|---|---|---|
| 首次拉依赖龟速/卡死 | `mvn` 下载 90% 不动 | `~/.m2/settings.xml` 配阿里云镜像；换 `mvn -Dmaven.wagon.http.retryHandler.count=3`；仍卡则断网重试两次 |
| IDEA 索引/构建卡 | 打开大工程风扇狂转 | File > Invalidate Caches；关无关插件；Power Save Mode 只在复习旧工程时开 |
| spring-boot:run 热重启慢 | devtools 重启卡 | 老机器默认不加 devtools 自动重启；手动停/启 |
| 端口 8080 占用 | `Port 8080 was already in use` | `lsof -i :8080` → kill 或 `server.port=8081` |
| 多 JDK 混用 | IDEA 里编译错、shell 里对 | IDEA Project SDK = 21；Language level = 21 |
| JDK 21 模块化误伤 | `InaccessibleObjectException` | 少见（Boot 3.x 已适配）；出现先看是不是老版本依赖 |

## Maven 健康三连（问诊前默认跑）

```bash
mvn -v
mvn -q dependency:tree -Dincludes=<疑似冲突的 groupId>   # 依赖冲突排查
mvn -q clean compile -o        # 离线模式验证本地仓库齐全度
```

`-o`  offline 编译能过 = 依赖已齐，之后的怪现象都不是"没下载全"。

## 教程版本对齐（黑马课程常见错位）

- 课程用的 Spring Boot 2.x：与 JDK 21 + Boot 3.x 有 API/配置差异（如 `spring.factories` 迁移 `AutoConfiguration.imports`、`javax.*` → `jakarta.*`）。**原则：工程用 Initializr 生成 Boot 3.x + Java 21，课程知识点照学，写法定差异以官方文档当前版为准**（docs.spring.io/spring-boot/reference）。
- 课程里的老写法（`WebMvcConfigurerAdapter` 等）只作历史了解，不抄进新工程。

## 约定

- 所有练习工程放 `~/spring-coach/labs/<序列级>-<主题>/`，与卡、账同根。
- 启动证据一律保存到 `~/spring-coach/logs/<日期>-<主题>.log`（`mvn spring-boot:run 2>&1 | tee`）。
