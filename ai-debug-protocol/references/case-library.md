# Case Library — 标准案例与通用案例（按症状索引）

每个案例：症状 → 选盒 → 关键动作 → 转折点 → 判据。完整来源在 evidence-cases.md。

## 标准案例（来自研究的一线实录）

### C1 Go runtime 随机 SIGSEGV（官方二进制也崩）
- 选盒路径：黑盒（官方二进制规避本地因素）→ 灰盒（strace/GOMAXPROCS 观测者实验）→ 白盒（汇编定性）
- 转折链：坏硬件（排除）→ strace 让崩溃消失=并发 → 内核二分锁定 .config → 分包编译二分定位 vclock_gettime.o → 反汇编：栈探针与 Go 的 104 字节余量冲突
- 判据：最小 reproducer 在修复前后行为相反；官方 commit 落地
- 可迁移招式：观测者效应当证据；把"是否含 bug"做成可判定输出交给自动 bisect

### C2 内核回归半小时 bisect
- 黑盒起手：strace 拿事实（backlog=0）→ `git bisect run` 无人值守
- 转折：结论反直觉——程序一直用错 API，旧内核兜底掩盖
- 判据：命中 commit 后单跑该 commit 复现
- 可迁移：判定一行 grep；复现环境装进 initrd

### C3 生产偶发数据处理错误（Arthas 临床式）
- 灰盒：attach 同用户 → dashboard → thread → jad 确认跑的源码 → watch 抓返回值与耗时
- 判据：watch 到特定输入对应错误输出
- 可迁移：先确认"线上跑的是哪份代码"再谈现象

### C4 AI 693 行幻觉（Seen/Remembered/Guessed 失守）
- 现象：终端输出被截断（明确警告中间缺块）→ 用先验补出仓库里不存在的类 → 每轮坐实
- 判据：三态标注 + "未见先回源再开口"
- 可迁移：工具输出截断警告=一等公民停机条件

### C5 "全绿"PR 被 maintainer 打回（浅层 patch）
- 现象：grader 全过；人工盲审主因 core functionality failure；grader 高估 24.2pp
- 判据：说清"为什么触及根因"再提交
- 可迁移：判据与"可合入/可维护"不等价，要第二把尺子

### C6 Replit 删生产库（谎报+无护栏）
- 现象：口头 freeze → 重跑 migration → 表清空 → 否认
- 判据：关键操作=确定性护栏（权限收窄/副本/只读），证据=命令+输出
- 可迁移：自述不是验收；口头约束不是护栏

### C7 agent "变笨"（观测被偷偷丢弃）
- 现象：每轮清 thinking 历史 → 忘性重复 → eval 复现不了
- 判据：给非功能退化建回归；prompt 改动逐行 ablation
- 可迁移：症状像"全面退化"先查观测/上下文管道

### C8 GUI 卡死三向分流（本机场景）
- 观察：闪一下卡住 → 枚举窗口/查 unresponsive → sample/lsof
- 判读：`__psynch_mutexwait`=锁；send/recv=网络；模态=阻塞源
- 判据：拿到样本栈与连接态才重启

## 通用案例（症状 → 起手式）

| 症状 | 起手式（按顺序） |
|---|---|
| 页面 500，日志一屏 | `grep -m1 Caused by` + 第一个自己包名的帧；构造最小复刻 |
| 转圈然后超时 | Network 看 TTFB；后端看线程栈（sample/arthas thread） |
| 以前好的 | git bisect run，别读代码猜 |
| 偶发/并发 | strace 观测者实验；条件断点；硬件 watchpoint |
| 内存型死亡 | ExitCode 137 查 OOM；堆百分比口径；ExitOnOutOfMemoryError |
| 线上不能停 | 观察者型灰盒（Arthas/eBPF）；禁 JDWP |
| 卡死/闪屏 | GUI 三向分流（gui-live-debug.md） |
| 抓包异常 | curl 复刻隔离浏览器因素 |
| 改了到处不对 | 停下：一次一变量，回滚到单一变更 |
| AI 修完没修好 | 原始交互重放 + 行为 diff 描述 |

## 反模式速查（翻车即查）

幻觉式修复 / 测试当万能钥匙 / 浅层 patch / 无判据长跑 / 谎报完成 / 观测丢失假失忆 / 变通绕约束 / 多改无法归因 / 只会 printf / 断点满天飞 / 黑盒瞎猜 / 日志六级麻 / 撞运气式 heisenbug / 生产裸 JDWP / 调试残留进库 / 热 swap 改结构 / drop frame 当撤销。
