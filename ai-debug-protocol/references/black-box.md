# Black-Box Arsenal — 只看行为时的兵器卡

适用：无源码、生产环境、vendor 二进制、"复现不了/日志看不懂"阶段。

## 武器总表

| 武器 | 什么时候用 | 拿什么 | 判什么 |
|---|---|---|---|
| 读日志正确姿势 | 一切之前先做 | `grep "Caused by"` 取**第一个**，再找栈里**第一个自己包名**帧 | 病灶行比"整屏红"重要 |
| strace/dtruss | 程序没报错但行为不对；系统调用层取证 | 每次 open/connect/write 真相 | 后台在偷偷连什么/读什么；marcan：一上就不崩=并发线索 |
| lsof / ss -lntp | "连不上"/"端口占用" | 谁在听、谁在连 | listened≠reachable（绑定地址+防火墙两层） |
| tcpdump + Wireshark | 网络层 symptom | 包级事实 | 重传/拒绝/RST 谁发的 |
| APM / 火焰图 | 慢、方差、偶发 | 热点与时间线 | Netflix FlameScope：亚秒热力图找模式再出火焰图；Java 要 `-XX:+PreserveFramePointer` |
| git bisect run | "以前是好的，什么时候坏的" | 自动定位引入 commit | 复现必须可判定输出；flaky 用 git bayesect（概率判定） |
| Delta Debugging | 失败输入太大 | 自动最小化触发条件 | Zeller 方法论；调试书在线版 |
| 最小复现构造 | 一切之前 | 12 行复刻 vs 读半天大仓 | 复现是唯一可反复验证的锚 |
| 统计/对照实验 | "我这台是好的" | in-prod 稳态视角 | Meta ripple：15 天覆盖 70% fleet 静默损坏，机会式要 6 个月 |

## 日志分级（黑盒的可筛选性就是一切）

- 打底只有 INFO + ERROR：会不会叫醒我，一个维度定级（ntietz）
- 结构化字段补充分级：trace id、时间戳精度、业务对象 id、flag 状态、模块名
- 反模式：修完就删的调试日志（说明一开始日志就不够）；六级混用刷屏的 WARNING

## 符号判读表

| 观察到的 | 命名的根因方向 |
|---|---|
| ExitCode 137 / cgroup OOM kill | 内存超限（堆≠JVM 全部；固定 -Xmx 与限额打架） |
| `__psynch_mutexwait` 在栈顶 | 等锁/死锁 |
| `send/recv` 卡住 | 网络对端不响应 |
| 加 strace 后不崩 | 观测者效应：并发 timing 被 serialize——本身就是并发线索 |
| 官方二进制也崩 | 排除本地构建/环境，往硬件/内核层走（marcan 路线） |
| curl 通、浏览器不通 | 绑定地址/防火墙/代理/前端硬编码 host |
| "以前好好的" | 别修 bug，先 bisect 那个"以前" |

## bisect 自动化的形状（geofft 内核案例）

```bash
git bisect run sh -c 'make defconfig && make -j8 && \
  qemu-system-x86_64 -nographic -append console=ttyS0 -kernel arch/x86/boot/bzImage \
  -initrd initrd/initrd.gz | grep ^Success'
```

要点：判定输出一行 grep；复现环境装进 initrd；无人值守半小时跑完 16 个 commit。flaky 场景换 `git bayesect`（二元判定改概率判定）。

三条实测纪律（GREEN-2 实验台收编）：
- **锚点必须先实测**：good/bad 两端各跑一次确认判定函数真的翻转，再开 bisect——GREEN-2 故意用未实测锚点，bisect 输出"first bad = 一个无行为变化的 commit"，结论荒谬
- **判定必须便宜**：ddmin 判据跑一次 60 秒的输入，最小化一小时不收敛；同样输入做到 0.19 秒立刻收敛。判定慢，等于没有判定
- **偶发判定要设计强度**：单次失败率 p 的偶发，跑 K 次全过的误判率是 (1-p)^K——要"确认修好了"先算 K，别跑三次就宣布

## 纪律

- 黑盒阶段禁止改代码：观测没做完，改动即污染现场
- "quit thinking and look"：数据没看就提假设 = 瞎猜
- 每发一枚黑盒武器，写审计行：命令→观察→结论
