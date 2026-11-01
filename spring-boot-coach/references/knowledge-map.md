# 知识地图与反魔法序列 — The Map

## 为什么先有这张图

他的原话："太乱太多了"。乱的根源不是知识多，是不知道每个知识点挂在哪。本文件是总图：任何教程章节、任何报错、任何"这个注解干嘛的"，都先归位到图上一个节点，再开讲。

## 总图（Spring 宇宙 → 他所在层）

```
Spring Framework（核心：IoC 容器 + AOP）
   └─ Spring Boot（自动装配 + 内嵌服务器 + 约定配置）
        ├─ Spring Web（MVC/REST）          ← 教程主战场
        ├─ Spring Data（JPA/JDBC/Redis）
        ├─ Spring Security（认证授权）
        └─ 其它（Cache/Task/Validation/Actuator…）
```

规则：**没搞懂 Framework 层之前，Boot 的每个魔法都会变成恐惧**（Without proper Spring framework knowledge, you will sooner or later get lost — marcobehler.com/guides/spring-framework）。

## 反魔法序列（课程唯一引擎）

每级 = 一个最小可跑工程 + 启动证据 + "没有 Spring 会怎样"对照。禁止跳级直接教注解。

| 级 | 主题 | 最小工程 | 讲什么 | 验收问题 |
|---|---|---|---|---|
| 0 | 纯 Java 装配痛 | 两个类手工 `new` 互传依赖 | `new` 爆炸、多 DAO 共享 DataSource 只能各建各的 | 说出不用 Spring 的三个痛 |
| 1 | 全局单例收依赖 | Application 单例持有 DataSource | 还是调用方主动要；类一多成怪兽类 | 这个方案的剩余问题 |
| 2 | 构造器声明依赖 | UserDao(DataSource ds) | IoC 思想：只声明，不负责找 | "倒转"倒在哪里 |
| 3 | IoC 容器 + @Bean | AnnotationConfigApplicationContext + @Configuration + @Bean | 有人替我们装配；bean=容器创建并管理的实例；scope 默认 singleton | ctx.getBean 拿到什么 |
| 4 | 组件扫描 | @ComponentScan + @Component + @Autowired | 免手写 @Bean；扫描根规则；构造器注入默认生效（Spring 4.2+） | bean 在扫描根外会怎样 |
| 5 | 自动装配 | @SpringBootApplication 拆三合一；starter 依赖触发；--debug 条件报告 | @Configuration+@ComponentScan+@EnableAutoConfiguration；条件生效/失效怎么看；exclude/excludeName | 解释一个Positive/一个Negative |
| 6 | 代理与 AOP | @Transactional 自调用实验 | 为什么"事务没开"：代理边界；CGLib vs JDK 代理；同类自调用不切 | 画一次调用穿代理的路径 |

完成后补层：Web（一次请求的生命周期）、Data（仓库接口怎么被实现）、Security（过滤器链与自动配置关系）。

## 黑马教程章节挂靠表（骨架，随章节扩充）

| 教程章节 | 挂靠节点 | 必须落到的最小工程 | 章末四件套 |
|---|---|---|---|
| （IoC/DI 入门） | 级 2-3 | 手写构造器 → `@Bean` | 跑通+卡+账+能讲 |
| （注解开发/@ComponentScan） | 级 4 | 包结构实验：bean 移出扫描根 | 同上 |
| （SpringBoot 入门/自动配置） | 级 5 | `--debug` 报告逐行读一个 | 同上 |
| （Web/REST） | Web 层 | curl 一次真接口 | 同上 |
| （事务 @Transactional） | 级 6 | 自调用失效实验 | 同上 |
| （待填——他把章节名发来） | — | — | — |

使用：每开一章先填一行（章节→节点），章末四件套齐了才划掉。

## 一句话记忆位（给防忘卡用）

- "bean 从哪来"链：`@SpringBootApplication` = 扫描根 + 自动配置开关；扫描根下的 `@Component` 家族 → BeanDefinition → 容器按类型解析注入点。
- 扫描根规则：启动类所在包 + 子包；外部的 bean 等于不存在。
- 条件报告：`--debug` 一开，每个自动配置为什么生效/失效，白纸黑字。
