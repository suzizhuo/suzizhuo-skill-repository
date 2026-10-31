# Effective Java 审查清单（JDK 17+）

用于审查/重构 Java 代码。从上到下扫，拿到足够高价值发现就停——不要把全部风格偏好都倒出来。每条对应 `item-map.md` 条目号。

## 1. 构造与生命周期

- [ ] 静态工厂：命名说清意图、实例受控/缓存、返回接口或子类型时用了没有（1）
- [ ] Builder：参数多/可选/相互依赖时用了没有；校验收敛在一处（2）
- [ ] 单例：public static final 字段 / holder / 枚举三选一；DCL 必须 volatile（3, 83）
- [ ] 工具类：私有构造器（4）
- [ ] 依赖注入：时钟/随机/仓储/客户端是否硬编码成静态（5）
- [ ] 资源：`AutoCloseable` 全部 try-with-resources；没有 finalizer/cleaner（8–9）
- [ ] 热路径：`new String(字面量)`、循环内 `Pattern.compile`、装箱、数组拷贝（6）

## 2. 对象契约

- [ ] `equals`：只在逻辑相等需要时覆写；五性质齐全；不与不兼容子类相等；不用 `==` 比浮点，用 `Double.compare`/`Float.compare`/`BigDecimal.compareTo`（10）
- [ ] `hashCode`：与 equals 字段一致；`31` 循环或 `Objects.hash`；数组字段深哈希（11）
- [ ] `toString`：有诊断价值，无敏感字段，无不稳定细节（12）
- [ ] `clone`：是否本该用拷贝构造/拷贝工厂（13）
- [ ] `Comparable`：排序一致、可传递、与 equals 的关系写进文档；不用 `>`/`<` 比较字段（14）

## 3. 类与接口

- [ ] 可见性：类/构造器/字段/嵌套类型能不能再收（15）
- [ ] 公有可变字段：一律改访问器；public final 数组/集合仍算可变（16）
- [ ] 不可变性：能 `final` 的字段/类；不可变天然线程安全（17）
- [ ] 防御性拷贝：入参先拷再校验（TOCTOU），返回值拷出；`Date`/数组/集合是重点（50）
- [ ] 继承：跨包继承慎用；构造器不调可覆写方法；自用点文档化或 `final` 封死（18–19）
- [ ] 接口：不加常量接口；default 方法是否被迫使用（20–22）
- [ ] tag 类：带 mode 字段 + 分发 switch —— 拆层次；JDK17 优先 sealed + record + 模式匹配（23）
- [ ] 嵌套类：不需要外围实例就 `static`（24）
- [ ] 一个源文件一个顶层类（25）

## 4. 泛型与类型安全

- [ ] raw type：新代码零容忍；`List`/`new ArrayList()` 都算（26）
- [ ] unchecked 警告：可消除则消除；`@SuppressWarnings` 最小作用域+注释（27）
- [ ] 数组 vs List：泛型元素类型处不用数组（28）
- [ ] 强转上浮：使用点 cast 改泛型类/泛型方法（29–30）
- [ ] PECS：生产者 `? extends`，消费者 `? super`；`Comparable<P>` 写 `P super T`（31）
- [ ] varargs + 泛型：堆污染；`@SafeVarargs` 只在安全时用（32）
- [ ] 异构容器：`Map<Class<?>, ...>` 类型令牌 vs `Map<String, Object>`（33）

## 5. 枚举与注解

- [ ] int/String 常量冒充枚举：换 enum（34）
- [ ] ordinal 落库/落线：换 `name()` 或显式 code（35）
- [ ] 位域：`EnumSet`（36）；ordinal 索引数组：`EnumMap`（37）
- [ ] 可扩展枚举：`enum implements` 接口，API 收接口（38）
- [ ] 命名模式：注解替代（39）；`@Override` 坚持（40）；标记接口场景（41）

## 6. Lambda 与 Stream

- [ ] 匿名类能换 lambda 则换；需要 `this`/状态时保留（42）
- [ ] 方法引用 vs lambda：可读性择优（43）
- [ ] 函数式接口：优先 `java.util.function` 标准件（44）
- [ ] Stream 滥用：早退/复杂分支/可变累加被硬写成 stream（45）
- [ ] 副作用：`map`/`filter`/`peek` 改外部状态（46）
- [ ] 返回 Stream：调用方要集合/索引/复用时换集合返回；17 用 `Stream.toList()`（47）
- [ ] 并行流：源可分裂、汇可结合、计算密集、数据量级足够——否则不用（48）
- [ ] stream 里 checked 异常被 sneaky-throw 吞掉（45–46, 73）

## 7. 方法与防御

- [ ] 入口校验：public 方法校验参数抛标准异常；private 用 assert（49, 72）
- [ ] 签名：名字自解释、参数少、布尔参数隐藏模式、重载行为一致（51–53）
- [ ] 空返回：空集合/空数组，不 null（54）
- [ ] Optional：只做返回；不用在字段/参数/集合；不用 `of(null)`；基本类型用 OptionalInt/Long/Double（55）
- [ ] Javadoc：契约、`@throws`、单位、线程安全、所有权；`@implSpec`、`{@code}`（56）

## 8. 通用编程

- [ ] 局部变量作用域；首次使用处声明（57）
- [ ] for-each 可用性：索引数学/`Iterator.remove`/协作遍历除外（58）
- [ ] 标准库：重复造轮子的实现先查 `java.util`（59）
- [ ] 精度：float/double 做货币/精确计算（60）；装箱泛滥（61）；字符串塞非文本数据（62）；循环字符串拼接（63）
- [ ] 接口类型引用（64）——接口优于反射（65）
- [ ] 命名规范：类型名词大驼峰、方法动词小驼峰、常量全大写下划线、`HttpUrl` 式缩写（68）

## 9. 异常

- [ ] 异常做控制流（69）
- [ ] checked/runtime 分工：恢复不了的 checked（70–71）
- [ ] 标准异常复用（72）
- [ ] 转译丢 cause（73）
- [ ] 未文档化 `@throws`（74）；消息无定位信息或泄敏感（75）
- [ ] 失败破坏对象状态（76）
- [ ] 空 catch / printStackTrace 了事（77）

## 10. 并发

- [ ] 只同步写不同步读；非 volatile 的 64 位/复合操作（78）
- [ ] 临界区内调外部/可覆写方法；锁粒度过大（79）
- [ ] 裸 `new Thread`/`Thread.run()` 直跑（80）
- [ ] wait/notify 手写：缺 while 循环、用 notify 不用 notifyAll（81）
- [ ] 线程安全档位未文档：immutable/thread-safe/conditionally thread-safe/not thread-safe/thread-hostile（82）
- [ ] 懒初始化：非 holder/DCL-volatile/枚举（83）
- [ ] `computeIfAbsent` 误用：mapping 返回 null 会被 CHM 抛 NPE；秒级慢 load 占 bin 锁 → 换 Future 单飞（81）
- [ ] 依赖 yield/priority/busy-wait 换正确性（84）

## 11. 序列化

- [ ] 新设计上 Java 序列化：先评估 JSON/Protobuf（85）
- [ ] `Serializable` 落地：`serialVersionUID` 显式声明；内部类/ lambda 捕获（86）
- [ ] 自定义 serialized form：`transient` + `writeObject/readObject`（87）
- [ ] `readObject` 防御：defaultReadObject 后重建校验不变量 + 私有可变字段拷贝（88）
- [ ] `readResolve` 收敛单例：仅适合无状态单例；有可变状态的收敛 = 静默丢数据，改枚举单例或阻断序列化（89）
- [ ] 不可信流：`ObjectInputFilter`（JDK9+）配深度/长度/引用上限——放在**创建流处**，不放 `readObject` 内（88）
- [ ] 枚举实例控制（89）；不变量多的类上序列化代理（90）

## 输出契约（每条发现的格式）

```text
- [P1|P2|P3] 问题一句话（`File.java:line`）：具体危害——什么场景下出什么错。
  EJ lens：条目号 + 原则名。
  改法：可直接落地的最小改动（必要时给代码片段）。
  兼容性：公开 API/序列化形式是否变化；是否需 @Deprecated 过渡。
```

- P1 = 正确性/集合语义/并发/资源/二进制兼容风险；P2 = 封装/类型安全/契约；P3 = 可读性/命名/风格。
- 只输出被清单命中的问题；没命中就明说"该分区无发现"，不要凑数。
- 没看到调用上下文不下并发/序列化结论——标注"需确认"而不是猜。
