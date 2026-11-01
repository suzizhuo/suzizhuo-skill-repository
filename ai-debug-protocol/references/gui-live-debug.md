# GUI Live Debug — 窗口识别、真人级点击、卡死取证

针对痛点原话："分不清应用程序窗口 / 调试天天卡住还闪屏 / 不能像真人一样点击"。macOS 为本机环境；deep execution 委派 skill `hot-debug`（attach+companion-click 实点法）。

## 第一步：确认你操作的是哪个窗口（身份链，不是猜）

```bash
lsappinfo list apps                    # 所有前台/后台 app 与 PID
lsappinfo info-in <PID>                # 该 app 是否为 active/frontmost/unresponsive
pgrep -fl APPNAME                      # 进程名 → PID（多个实例要列全）
```

判读者色：**frontmost/active**（它在最前）/ unresponsive（系统标了无响应=先兆）。多窗口同名时用 bounds 定位窗口而非只认 app：

```bash
osascript -e 'tell application "System Events" to get {position,size} of every window of (first process whose name is "APP")'
```

规则：**身份链三件套**——app 名 + PID + 窗口 bounds/标题，三者对齐才算认准。截图留证同步做（`screencapture -x /tmp/win-<ts>.png`），报告里"我点的是这个窗口"必须可回放。

## 第二步：像真人一样点击（坐标校准 + 反馈验证）

macOS 无系统级 cliclick 时最小依赖路线：
1. 辅助功能权限先打开（系统设置→隐私→辅助功能，终端要勾）——没开时一切点击类命令静默失败，这是"点了没反应"的第一嫌疑
2. `osascript` System Events 点击（按 UI 元素，比盲点坐标稳）：

```bash
osascript -e 'tell application "System Events" to tell process "APP" to click button "同步" of window 1'
```

3. 盲坐标兜底：`cliclick`/Python `pyautogui`/Quartz `CGEventPost`——坐标必须来自上一步 bounds 测量，点前先 mouse move 到位，点后**验证 UI 反馈**（按钮态变/窗口标题变/列表刷新）才算"点到"
4. 真人粒度：鼠标移到目标上（hover 态）+ 按下/抬起两段（有的 UI 只在 mouseUp 触发）

判据：点击 ≠ 送达。验证反馈才算一次完整点击，写进行为。

## 第三步：卡住/闪屏取证（先判活/死，再三向分流）

第 0 步永远先是**活/死判定**：`pgrep -fl APPNAME`。进程不在 = 它不是卡住，是已经崩了——走崩溃取证；进程在 = 才进下面分流。GREEN-1 实测教训："闪一下不动"是 GUI 进程 SIGSEGV 死后留的死窗，按"卡死"分流全部落空。

| 观察 | 取证 | 判读 |
|---|---|---|
| **进程已死** | `ls ~/Library/Logs/DiagnosticReports/ \| tail` 取 .ips；看首个异常帧与栈形 | PyObjC/ctypes 栈形 `objc_msgSend ← CFDictionaryGetValue ← ffi_call_unix64` = native 重入已释放对象；对照源码修 |
| 弹了模态被主窗挡 | 枚举全部窗口：`osascript ... get name of every window of process "APP"` | 有隐藏 dialog → 它是阻塞源，处理它 |
| 主线程死锁/忙等 | `sample PID 10 -file /tmp/hang-sample.txt`（卡住瞬间抓，抓 10 秒） | 栈顶 `__psynch_mutexwait`=等锁；自旋=忙等 |
| 网络同步挂起 | `lsof -i -p PID` + 上面的栈 | 栈在 send/recv 且连接 ESTABLISHED→查对端服务 |

频率是资产：一天两三次的复现，建一周台账（时间/前置操作/样本栈），模式自己浮出来；别急着改代码。客户端加 5 秒看门狗自 dump 是"改代码加自证"，不算往现场装探针。

闪屏专项：一次截图定态（`screencapture`），连续两张间隔 1s 对比是否闪退重绘；若是启动即闪退，抓 DiagnosticReports 的首个异常帧。

## 与 hot-debug 的接口

需要源码级断点/companion-click 实点时，加载 skill `hot-debug` 并按它的 attach→companion-click→real-hit 流程走；本文件负责的"认窗口/点得动/卡住取证"是它的前置事实来源，两边的证据互相引用（PID、bounds、截图文件名）。

## 纪律

- 每个 UI 动作落审计行：命令 + 截图文件名 + 观察到的反馈
- 不许"盲点坐标赌一把"：没有 bounds 测量就没有点击
- 卡住先取证再重启：重启 = 销毁现场，等拿到 sample/lsof 输出
- 截图/样本放 /tmp 或指定工作区，报告里可引用；涉敏感界面先问能不能截图
