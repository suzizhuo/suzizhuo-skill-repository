# 问诊三步手册 — Triage Playbook

入口铁律：**没有证据，只给定位路径，不给修复清单**。证据包三样：报错全文最后 20 行堆栈 / 启动类与问题类的包路径 / 相关类代码。缺证据时回一句："把这三样贴来，我先读再断，不猜。"

## Step 1 — 读栈（异常在说什么）

- 最后一行 exception message 点名哪个类型缺、哪个条件失败。
- 往上找第一个 `at com.<你的项目>` 帧 → 那是真正的失败注入点/调用点。
- 分类信号：
  - `No qualifying bean of type 'X'` / `NoSuchBeanDefinitionException` → 注册链断了：X 扫到没？条件装配排除没？被 exclude 没？
  - `Consider defining a bean of type 'X' in your configuration` → X 是第三方类（DataSource、第三方 SDK）：要么你漏了 starter，要么需要显式 `@Bean`。
  - `UnsatisfiedDependencyException` → 有类型但参数解析失败：多个同类型候选缺 `@Qualifier`/`@Primary`，或构造器链断。
  - `BeanCurrentlyInCreationException` → 循环依赖：A 构造器要 B、B 构造器要 A。改为 setter/`@Lazy`/拆第三方。
  - 启动即退出 / 端口占用 → 与 bean 无关，先看 `APPLICATION FAILED TO START` 的根行和端口行。

## Step 2 — 查来源（这个 bean 到底进没进容器）

三件套，按代价从低到高：

1. **`--debug` 启动**：`mvn spring-boot:run -Dspring-boot.run.arguments=--debug`（或 IDEA VM options 加 `--debug`）。看控制台 **Conditions Evaluation Report**：`Positive matches` = 生效的自动配置；`Negative matches` = 被条件挡掉的（缺 classpath、缺 property、被 exclude）。**新手第一次就问一句：你缺的 bean 属于哪个自动配置？它在 Positive 还是 Negative？**
2. **actuator `/beans`**：加 `spring-boot-starter-actuator`，浏览器/curl `/actuator/beans`，grep 缺的类型名。在 = 已注册（问题在注入点）；不在 = 注册链问题。用完可移除依赖。
3. **IDEA 断点**：启动类 `run()` 处打断点，看 `basePackages` 与扫描根；或对启动类求值 `ctx.getBeanDefinitionNames()`。
4. **编译产物核对**：`target/classes` 下有没有 `UserService.class`。“源码有、运行时无” = 编译/刷新没到位（IDEA 没 Build、build path 漏目录），这一类根本不是注册链问题。

判断规则（写进防忘卡的那句）：**启动类所在包就是默认扫描根；任何 bean 在这个包及其子包外，对 Spring 等于不存在。**

## Step 3 — 定修法（最小改动 + 证据）

- 最小改动优先：移动包 > 补 `@ComponentScan("com.example")` > 补 starter。一次只改一处，改完重启贴日志。
- 修好判据：旧异常消失 + 目标接口 curl/调用返回一次贴出来。
- 大项目才用 `exclude`/`excludeName` 关自动配置；先搞懂它为什么生效再关。

## 关单四件套（缺一即未完成）

1. **定位链**：异常类型 → 断在哪一环（扫描/条件/注入点解析/代理）。
2. **最小修法 + 启动证据**：命令 + 日志关键行。
3. **防忘卡一张**（格式见 cards-and-ledger.md）：问句用他自己会遇到的形式，如"启动报 No qualifying bean of type 'X'，第一步看什么？"
4. **错题账一行**：日期 / 症状 / 根因 / 定位用了哪件工具 / 关联卡号。

## 高频异常速查

| 症状 | 第一动作 | 典型根因 |
|---|---|---|
| No qualifying bean | 看 bean 包是否在扫描根下 | 类放错包 / 没加 @Service/@Component / 多模块漏 @ComponentScan |
| Consider defining a bean | 问"这是第三方类吗" | 缺 starter，或第三方类需显式 @Bean |
| 同类型多个候选 | 看候选列表 | 缺 @Primary/@Qualifier |
| 循环依赖 | 画两个类的构造器 | 构造器互要；改 setter 或 @Lazy |
| @Transactional 不生效 | 看调用是否穿过代理 | 同类自调用 / 方法非 public / 代理被绕过 |
| 启动后秒退 | 搜 `APPLICATION FAILED TO START` | web 依赖缺失/端口占用/数据源失败 |
| mvn 拉依赖卡死 | swap:断网重试/换镜像 | 网络/aliyun 镜像未配（见环境卡） |
