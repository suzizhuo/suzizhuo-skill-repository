# Effective Java 条目地图（第 3 版 90 条 · JDK 17+ 视角）

用途：条目级导航、学习总结、审查时定位"该改哪一条"。这是对全书原则的意译整理，不复制原文。JDK17+ 注记标出了现代 Java 表达同一条原则的首选写法；无注记的条目在 17 与新版语义一致。

## 一、对象创建与销毁（1–9）

1. **静态工厂优于构造器**：当名称能说清意图、需要缓存/实例控制、返回接口或子类型时优先；只是简单 new 时构造器更直接。
   JDK17+：`List.of` / `Map.of` / `Set.of` / `EnumSet.of` 是这一条的样板。
2. **参数多时用 Builder**：必填+多个可选、或参数间有依赖校验时，Builder 把校验收敛到一处并使调用点自解释。
   JDK17+：record 适合"少参数、透明、无需复杂校验"的载体；校验复杂或参数多仍手写 Builder。
3. **单例三选一**：`public static final` 字段最直观；静态工厂保留演进空间；枚举单例防反射与反序列化破坏，最稳。
4. **工具类不可实例化**：私有构造器，内部抛 `AssertionError`，同时可 `final`。
5. **依赖注入优于硬编码资源**：时钟、随机源、仓储、客户端、配置一律注入；静态工具类只放无状态纯函数。
6. **避免多余对象**：字面量不用 `new String(...)`；循环内预编译 `Pattern`；警惕自动装箱；可变且昂贵的对象考虑复用。
7. **消除过期引用**：自己管理内存的类（栈、缓存、监听器回调）要及时清引用，防内存泄漏；`WeakHashMap` 不放进强引用键。
8. **不用 finalizer/cleaner 做常规清理**：CFS 明确不保证执行时机；`AutoCloseable` + try-with-resources；cleaner 只作安全网。
9. **try-with-resources**：所有 `AutoCloseable` 优先；注意 suppressed 异常链，第一个 try 块异常为主、close 异常为被抑制。

## 二、所有对象都通用的方法（10–14）

10. **只在需要时覆写 equals**：逻辑相等不可缺（值对象、Map 键）；继承自父类的 equals 行为通常正确，别乱覆写。契约：自反、对称、传递、一致、对 null 返回 false；警惕与不兼容子类的相等。
    JDK17+：record 自动按组件生成 equals（显式声明组件可覆盖）。
11. **覆写 equals 必覆写 hashCode**：等价对象必须同哈希；`result = 31 * result + field.hashCode()`；`Objects.hash` 注意数组元素要深哈希；lazy 缓存哈希只用于不可变对象。
12. **toString 要有诊断价值**：默认输出无意义；含关键状态，但不含敏感字段、不含不稳定细节。
    JDK17+：record 自动生成。
13. **慎用 clone**：优先拷贝构造器/静态拷贝工厂；若必须 clone 先 super.clone 再修内部；不可变对象干脆不提供克隆。
14. **Comparable 要一致的排序**：compareTo 契约与 equals 的一致性；别用 `<`/`>` 做字段比较（溢出/NaN）；用 `Comparator` 链。
    JDK17+：`Comparator.comparingInt(...).thenComparing(...)`；record 不生成 compareTo，需要时显式实现。

## 三、类与接口（15–25）

15. **最小化可访问性**：信息隐藏是解耦与并行开发的基础；类、构造器、字段、嵌套类型能 private/package-private 就别放大；`serialVersionUID` 也要考虑可见性含义。
16. **存取器代替公有可变字段**：公有类暴露可变字段等于放弃封装不变式；例外只有 `public static final` 常量，且不能是可变数组/集合。
17. **最小化可变性**：`final` 类、`private final` 字段、构造器外不发引用、返回深拷贝；不可变天然线程安全、可自由共享。代价是每个值一个对象 → 用缓存或 Builder（见 2、6）。
    JDK17+：record 是透明不可变载体的首选形态；注意 record 的组件若是可变对象引用，仍需防御性拷贝。
18. **组合优于继承**：继承跨包使用会因超类演进而破裂（自用调用）；用转发类（forwarding class）实现包装/装饰。
19. **继承要么设计文档化要么禁止**：可覆写方法必须文档化自用点；构造器绝不调可覆写方法；`final` 类或私有构造器直接封死。
20. **接口优于抽象类做类型定义**：已有类可直接实现接口；接口是 mixin 理想载体。
    JDK17+：`sealed interface` + `record implements` 是封闭数据建模首选。
21. **接口默认方法是双刃剑**：default 方法能帮老接口演进，但接口作为类型定义时 public 成员永远可能被覆写；能 abstract 就别 default，default 内绝不假设实现细节。
22. **接口不放常量**：常量接口（constant interface）把实现细节泄漏给用户；常量子类私有静态常量类或枚举。
23. **类层次优于 tagged class**：带 mode 字段 + switch 的"一个类多重行为"拆成层次/多态。
    JDK17+：sealed + record + `switch` 模式匹配（`case Circle c ->`）直接替代 tag 分发。
24. **嵌套类能用静态就静态**：非静态内部类隐式持有外围实例（泄漏/序列化陷阱）；需要外围实例时才用内部类。
25. **一个源文件一个顶层类**：可读性与编译确定性。

## 四、泛型（26–33）

26. **新代码不用原始类型**：丢失类型安全；`List<Object>` 与 `List` 不同（后者连 `List<String>` 都能塞）。
27. **消除 unchecked 警告**：能改类型就改；`@SuppressWarnings("unchecked")` 只用于无法消除处，作用域最小化并写注释说明为什么安全。
28. **泛型场景用 List 不用数组**：数组协变且具体化，泛型非变且擦除；混用出 `ArrayStoreException`/堆污染。
29. **优先泛型类而非使用点强转**：让 `ClassCastException` 早早上浮到编译期。
30. **泛型方法**：类型参数让静态工具方法通用（`public static <E> Set<E> union(...)`），调用点可推导。
31. **有界通配符 PECS**：生产者 `? extends T`，消费者 `? super T`；参数既是生产又是消费（如 `Comparator`、`Comparable`）不要通配——`Comparable<P>` 永远写成 `Comparable<P super T>`。
32. **泛型 + varargs**：泛型数组是堆污染源；`@SafeVarargs` 只用于方法既不写入也不外泄该数组（Java 9+ 对 private 方法自动视为安全）。
33. **类型令牌异构容器**：`Map<Class<?>, Object>` + `cast()`；比 `Map<String, Object>` 安全。

## 五、枚举与注解（34–41）

34. **枚举代替 int 常量**：命名空间、类型安全、可挂行为、可用于 switch/集合（EnumSet/EnumMap）。
35. **不依赖 ordinal**：持久化/传输用 `name()` 或显式 code；ordinal 只配 EnumSet/EnumMap 内部使用。
36. **EnumSet 代替位域**：语义清晰的位运算集合，内部仍是紧凑位掩码。
37. **EnumMap 代替 ordinal 索引数组**：O(1) 且无需序数换算。
38. **用接口模拟可扩展枚举**：enum 不能继承，但可实现接口；API 接受接口类型即可开闭扩展。
39. **注解优于命名模式**：编译期可检、框架可读、不会默默失效。
40. **坚持 @Override**：防签名写错、意图显式。
41. **标记接口**：需要编译期类型约束（如 `Serializable`/`Cloneable`）时用标记接口；纯标注信息用注解。

## 六、Lambda 与流（42–48）

42. **Lambda 优于匿名类**：更短、`this` 指向词法作用域、无实例创建；只有需要"带状态的函数对象"时才匿名类。
43. **方法引用更清晰时优先**：`Function.identity()`、`String::toUpperCase`；参数名不重要且只有一个表达式时方法引用干净，涉及多个参数运算时 lambda 可读。
44. **优先标准函数式接口**：`java.util.function` 覆盖 90% 场景（`Predicate`/`Function`/`Supplier`/`Consumer`/`UnaryOperator`/`BiFunction`/`ToIntFunction`…）；签名不在标准集合里再自建，且要起个好名字。
45. **Stream 慎用**：清晰优先；流的强项是"同质元素序列上的聚合与转换"；复杂控制流、break/continue/早退、可变累加器，循环更清晰。
46. **流管线无副作用**：`map`/`filter` 内不修改外部状态；收集用 `collect` 而非 `forEach` 塞集合；副作用 lambda 会毁掉并行正确性。
47. **返回集合而非流**：调用方要的是可迭代/可索引/可复用的返回值；从方法返回 Stream 把复杂度推给调用方。
   JDK17+：`Stream.toList()`（JDK16）返回不可变 List，比 `collect(Collectors.toList())` 更简。
48. **并行流要有证据**：数据源易分裂（ArrayList/数组/HashMap）、计算密集、汇总量小且可结合（`reduce`/`toConcurrentMap`）、总量够大（万级以上默认）；源是 `Stream.iterate`/`Boxed`/`limit` 难分裂时不用。正确性优先。

## 七、方法（49–56）

49. **入口即校验**：public 方法校验参数，违法抛 `IllegalArgumentException`/`NullPointerException`/`IndexOutOfBoundsException`；private 假设用 `assert`。
   JDK17+：Helpful NullPointerExceptions（JDK14+）帮你定位 NPE，但别依赖它替代显式校验。
50. **防御性拷贝**：可变入参先拷贝再校验（防 TOCTOU），可变返回值拷出；`Date`/`Calendar`/数组/集合。
    JDK17+：`Instant`/`LocalDate` 不可变天然免疫；`List.copyOf`/`Map.copyOf` 做不可变快照。
51. **签名设计即 API 设计**：名字自解释、参数少而有序（接口/ranges 优于具体实现）、慎用重数量、`clone` 返回值优先后缀约定、避免长参数列表（超过 4 个考虑参数对象）。
52. **重载是编译期选择**：同名重载行为必须一致；同一实参能匹配多个重载时给用户惊喜；varargs 与固定参数重载要避免歧义。
53. **varargs 要给必填锚点**：`int... nums` 空调用合法有时是 bug；`(int first, int... rest)` 至少保一个参数。
54. **空集合/空数组优于 null**：调用方每次都要判 null 迟早漏一个。
   JDK17+：`List.of()`/`Set.of()`/`Map.of()` 就是不可变空容器常量。
55. **Optional 只做返回值**：不用于字段、参数、集合元素；不用 `Optional.of(null)`；基本类型用 `OptionalInt`/`Long`/`Double`；容器返回空容器而非 `Optional.empty()`。
56. **公开 API 写 Javadoc**：契约三件套——前置条件、后置条件、副作用；每个 `@throws` 说明触发条件；线程安全、单位、所有权（谁可变）写进文档；`{@code}` 包裹代码；实现行为说明用 `@implSpec`（JDK8+），内联示例 `{@snippet}`（JDK18+）。

## 八、通用编程（57–68）

57. **局部变量作用域最小**：首次使用处声明；for 头声明；大块逻辑抽方法，顺便把循环变量关进笼子。
58. **for-each 优先**：三种情况仍要索引/迭代器：需要索引数学、要 `Iterator.remove`、要对同一容器并发协作遍历。
   JDK17+：`List.copyOf`/`Set.of` 之后 for-each 遍历天然无 `ConcurrentModificationException` 焦虑。
59. **懂并用标准库**：`java.util`/`java.util.concurrent`/`java.util.stream`；别自己造轮子（重复造轮易引入 26–48 全部问题）。
60. **精确计算用 int/long/BigDecimal**：float/double 不表达十进制精确值；货币禁止浮点；`BigDecimal(String)` 或 `valueOf(double)`；`setScale` 必须显式 `RoundingMode`。
61. **基本类型优于装箱**：装箱类型 `==` 是引用比较、可为 null、慢；只有泛型容器/可空/null 三态才装箱。
62. **字符串只表文本**：枚举、类型化值、合适的数据类型替代拼装字符串；不要用 `"true"`/`"1"` 传布尔或 code。
63. **注意重复字符串连接**：循环拼接用 `StringBuilder`；一次性拼接编译期可折叠；多行字面量用 text block（JDK15+），少用 `\n`。
64. **用接口引用对象**：`List<String> x = new ArrayList<>()`；例外：框架要求具体类型（如 `AbstractStringBuilder` 不会出现）、需要具体类特有 API。
65. **反射只给框架**：编译期检查丢失、性能差、破坏封装。
   JDK17+：JPMS 强封装，`setAccessible` 进 JDK 内部更受限——需 `opens` 或 `--add-opens`，17 上这已是真实工程约束，别默默依赖。
66. **native 方法慎用**：JNI 无内存安全、不可移植、难调试；跨语言首选独立进程/IPC；JDK19+ 的 Panama FFM API 是库绑定的现代路径，17 上评估为后续迁移留口。
67. **先测量再优化**：坏优化比不优化更坏；JMH 微基准别手写；性能需求写成可复现的基准再动手。
68. **遵循命名惯例**：类型/接口名词首字母大写，方法动词小驼峰，常量全大写下划线，enum 常量同常量；`HttpUrl` 式缩写首字母大写（老例外：URL/XML 类长期传统）。

## 九、异常（69–77）

69. **异常只用于异常条件**：不用异常做控制流；可预期的状态用返回值/状态枚举。
70. **checked/runtime 分工**：调用方能合理恢复用 checked；编程错误（前置破坏）用 runtime，通常意味着 bug。
71. **别设计无法处理的 checked**：调用方只能 try-catch-忽略的 checked 是 API 痛苦源；捕获了什么都做不了 = 不该 checked。
72. **优先标准异常**：`IllegalArgumentException`（参数值）/`IllegalStateException`（对象状态）/`NullPointerException`（禁止 null）/`IndexOutOfBoundsException`（越界）/`ConcurrentModificationException`（并发修改）/`UnsupportedOperationException`（不支持的操作）。
73. **转译保 cause**：底层异常泄漏 = 抽象泄漏；`throw new HigherLevel(msg, cause)`。
74. **文档化每个异常**：每个方法 `@throws` 写触发条件；checked 必写，常见 unchecked 也写。
75. **异常消息含捕获状态**：哪个参数、什么值、什么期望；不含敏感信息（token、密码、完整栈里的用户数据）。
76. **失败原子性**：失败操作不损坏对象状态；校验先于副作用；复杂操作先算临时副本后提交；不可变对象天然原子。
77. **不吞异常**：空 catch 与 `catch (Exception e) { e.printStackTrace(); }` 都是吞；恢复不了也至少让上层知道（日志 + 指标 + 重新抛出或包装）。

## 十、并发（78–84）

78. **同步一切对共享可变数据的访问**：只同步写不同步读照样撕裂；JMM 无 volatile 保证 64 位原子性已是历史（long/double 实际都原子，但复合操作不是）；`volatile` 只保可见性不保 `count++` 原子。
79. **避免过度同步**：临界区内不调外部/可覆写方法（死锁+破坏不变式）；能不可变/线程 confine 就别进锁；锁内工作最小化。
80. **executor/task 优于裸线程**：`ExecutorService`/`CompletableFuture`；线程是昂贵资源。
    JDK17+：可执行简单并发任务评估虚拟线程（JDK21 GA；17 LTS 上作为升级项关注，pin 场景注意 I/O 与 synchronized）。
81. **concurrent 工具优于 wait/notify**：`ConcurrentHashMap`、`BlockingQueue`、`CountDownLatch`、`CompletableFuture`；永远 `while (!condition) wait()`；`notifyAll` 而非 `notify`。
82. **文档化线程安全档位**：immutable / thread-safe / conditionally thread-safe / not thread-safe / thread-hostile，五档写明；私有锁对象（`private final Object lock = new Object()`）防外部锁干扰。
83. **懒初始化谨慎**：静态字段用 holder idiom；实例字段双检 + `volatile`；单例枚举最省心。
84. **不依赖线程调度**：`Thread.yield`/priority/busy-wait 换不来正确性；循环里忙等 = 饿死 CPU；用 `CountDownLatch`/`CyclicBarrier` 表达"等到某状态"。

## 十一、序列化（85–90）

85. **优先替代方案**：JSON/Protobuf/显式二进制格式；Java 序列化是攻击面（反序列化 gadget）与兼容性长期债务。
86. **实现 Serializable 极其谨慎**：一旦发布，序列化形式就是公共 API 的一部分；显式 `serialVersionUID`；内部类别序列化；对不可信输入默认设防。
87. **自定义序列化形式**：默认形式暴露内部表示 + 浪费空间；`writeObject`/`readObject`/`transient` 显式表达逻辑形式。
88. **readObject 防御**：`defaultReadObject` 之后重建并校验全部不变量（等价构造器检查）；对私有字段做防御性拷贝。
   JDK17+：`ObjectInputFilter`（JDK9+）对不可信流设深度/数组长度/引用数上限，17 上默认可用。
89. **枚举保实例控制**：`Serializable` 枚举跨反序列化仍唯一。注意：**有可变状态的单例不要用 `readResolve` 收敛——那等于把反序列化读到的数据整个丢弃**；要么改无状态单例 + 外部仓储，要么直接阻断序列化。
90. **考虑序列化代理**：`writeReplace` + 私有静态 `SerialProxy` + `readResolve`，代理即逻辑形式，把不变量检查收敛到构造器；对不可变对象是首选。

## 快速定位（按任务选条目）

- 新值对象/DTO：2、10–14、17、49–50、56（JDK17：record 优先）
- 公开 API 审查：1–2、15–22、26–33、49–56、69–77
- 集合/工具类：26–33、42–48、54–57、59–61
- 并发服务：5、17、78–84
- 枚举/状态机建模：23、34–38（JDK17：sealed + switch 模式匹配）
- 遗留序列化包袱：85–90
- 性能排查：6、45–48、57–61、63、67、78–81
- 异常体系设计：69–77
