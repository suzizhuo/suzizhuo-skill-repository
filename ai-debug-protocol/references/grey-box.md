# Grey-Box Arsenal — 半可见现场：观测而不暂停

适用：进程活着但不能停（生产/长跑任务）、有部分插桩能力、或可以录下来事后查。

## 临床诊断（Arthas 式，JVM 标杆）

动作链（attach 与目标进程**同用户**，否则 attach 失败）：

```bash
java -jar arthas-boot.jar                 # 选中目标进程
dashboard                                  # 线程/内存/GC 总览
thread 1 | grep 'main('                   # 主线程在干什么
jad demo.MathGame                          # 在线反编译，确认跑的是哪份源码
watch demo.MathGame primeFactors returnObj  # 每次调用的返回值+耗时，不暂停
trace demo.MathGame '#cost>200'            # 慢调用链路
```

官方原话的动机：生产调试"会暂停所有线程导致服务暂停……Arthas 作为观察者永远不会暂停正在运行的线程"，无需改代码无需重启。典型黑盒痛点一句话："线上某个用户数据有问题，线上不能 debug，线下重现不了"。

其他语言的同类：eBPF/bcc、bpftrace、DTrace（观察者型，不暂停）。

## 插桩/开关

- feature flag：现场开调试通道而不发版（flag 状态写进日志，ntietz 结构化字段之一）
- 动态日志级别：调一个 logger 级别，别重启
- tracepoint/日志点：不中断打值（白盒文件里同名武器，灰盒场景的大头）

## 录像与时间旅行（AI 的观测源）

- Replay.io 式：录下完整执行，转成可查询行为数据库。核心价值原话："AIs are really good at writing code but really bad at debugging — the basic reason is a lack of context"
- 时间旅行调试：rr（Mozilla 为 Firefox 测试做）、UDB/WinDBG、GDB record——从"现在这样"倒推到"哪一步开始这样"
- omniscient debugging（Pernosco）：全执行状态可查，压缩到 <1 bit/指令
- 验收金标准（Nut 下一步）：修完后**模拟原始交互**，精确描述行为 diff——"改前改后行为变了什么"当判据，而不是"单测绿了"

## 生产红线

- 裸 JDWP 远程调试端口 = RCE 面（jdwp-shellifier 类工具直接打）；要远程调试：只内网 + 鉴权 + 用后即关
- devtools 类自动重启不进生产（官方安全红线，见 white-box.md）
- iptables/seccomp 类管控：规则重启即丢要重放；`NET_ADMIN` 是能力授予不是隔离

## 统计型灰盒

- 生产内巡检（Meta ripple）：15 天覆盖 70% fleet 静默损坏 vs 机会式 6 个月；机会式平均 180 天/台——窗口期漏网的量级
- 灰度/canary：新行为先小流量，"全量没事"不是充分判据

## 纪律

- 灰盒第一问："这个观测会让现场改变吗？"会 → 先取证再观测
- attach/观测本身写审计行；观测到的每个数值标 Seen
- 修不动就撤观测，别把现场永久插满桩
