# White-Box Arsenal — 有源码时的断点与热调试

适用：本地/可控环境、能重启、有源码。选了白盒就不用黑盒的笨办法：读帧比读堆栈截图强。

## 断点阶梯（从轻到重，先轻后重）

1. **列断点**——定位定点命中
2. **条件断点**——命中即求值才停机。警惕成本：万次循环普通断点实测比无断点慢 ~2000 倍；远程调试 1ms RTT 时每秒最多 200 次迭代。写条件前先想"这个循环多少次"
3. **hit count（第 N 次命中）**——"前 99 次都对，第 100 次崩"
4. **tracepoint（打点不中断）**——要值不要停机；表达式拼消息（IDEA：非挂起点 + Evaluate and log）
5. **data breakpoint / watchpoint**——"谁把这字节写坏了"的正解；硬件 watchpoint 扰动最小（jcranmer：软断点干扰 race，硬件 watchpoint 立刻抓到）
6. **方法断点/异常断点**——接口被谁调用、异常在哪个 throw 点出生
7. **drop frame（Reset Frame）**——重跑一段。**只重置局部变量，全局状态不回滚**——当"撤销"用会出更诡异的结果；真回滚用时间旅行（rr/UDB/GDB record）
8. **表达式求值**——现场定义 struct、调函数、制造循环/分支（配合改写变量注入调试分支）
9. **Force Return / Throw Exception**——不必复现产生某值的条件就能走那条路径

## 改变执行流（IDEA 官方清单）

- Reset Frame / Drop to Frame、Force Return、Throw Exception、非挂起 Evaluate-and-log
- 手册：https://www.jetbrains.com/help/idea/altering-the-program-s-execution-flow.html

## 热调试/热部署边界（Java 为例）

| 变更 | 标准 HotSwap | 该走哪 |
|---|---|---|
| 方法体 | ✅ | 直接 swap |
| 加字段/方法、改签名、改继承 | ❌ class redefinition failed | devtools 双 classloader 重启 / JRebel / DCEVM |
| **只改方法体也可能失败** | ⚠️ | 编译器顺带生成 synthetic 成员（class literal、匿名/内部类）——预期管理 |
| 改已在调用栈里的方法 | ⚠️ 等退出后才生效 | 退出重进 |

- spring-boot-devtools：不变 jar 进 base classloader、在改代码进 restart loader；**生产红线**（官方："must not be done in a production environment where running devtools is a security risk"）；与 AspectJ weaving 不兼容
- JRebel：监控 .class + 类加载器翻译；开了它 devtools 自动重启关掉
- 生产要观测：见 grey-box.md（Arthas），裸 JDWP = RCE 面，禁止

## 读栈配方（本地 500/异常）

```bash
grep -m1 "Caused by" boot.log          # 第一个 Caused by 才是根
# 栈里第一个属于你自己包名的帧 = 病灶行
```

Top 根因速查（教程项目常见）：NPE（注入/查询 null）、javax→jakarta 迁移错包、Jackson 字段对不上（反序列化 400/500）、DB 连不上（URL/凭据/监听地址）。

## 纪律

- 先宽后窄：printf/日志负责宽，断点负责窄（JGC Tao：单步是从最底层开始的错误方法）
- 一个断点假设一个结论：命中后先记录再单步，别一路 F8 滑过去
- 修完最小复现验证 + 删调试残留（断点/临时 print 不进最终 diff）
