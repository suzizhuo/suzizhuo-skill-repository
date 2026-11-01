# Evidence Cases — Three OS Families
date: $(date '+%Y-%m-%d %H:%M %Z')
field: device-env-triage skill references (macOS / Windows / Debian)
retrieval: web_fetch only — web_search unavailable, bash has no external network.
channels used: vendor official docs (Apple/learn.microsoft.com), man pages (unix.com mirrors, manpages.debian.org), freedesktop, HN Algolia API, GitHub APIs.
provenance rule: every URL below was web_fetched during this task and its content was actually seen.
items without a verified source are marked [unsourced] with the reason.
planned sections: ## Probe commands per family / ## Size measurement / ## Failure modes

## Probe commands per family

### macOS（设备档案卡）
- `sw_vers` — 打印 ProductName / ProductVersion / BuildVersion；无参即短列表，数据源为 /System/Library/CoreServices/SystemVersion.plist。来源：https://www.unix.com/man-page/osx/1/sw_vers/ (fetched)
- `sw_vers -productVersion` — 单独取版本号，如 12.x（Monterey 线）；脚本化判定 OS 版本门槛用这个字段。来源：同上。
- `uname -m` — machine hardware name（arm64 / x86_64）。来源：https://www.unix.com/man-page/osx/1/uname/ (fetched)。注意：Rosetta 运行时 x86_64 进程会看到 x86_64，需交叉验证，交叉手段（sysctl MIB）见下一行标注。
- `sysctl`（MIB 名 hw.* / kern.* / machdep.*）— hw.machine（硬件名）、hw.model、hw.ncpu、hw.memsize/physmem 均有文档；`sysctl -n <name>` 只出值。来源：https://www.unix.com/man-page/osx/8/sysctl/ (fetched；该页为旧版 man，未列 Apple Silicon 的 hw.optional.arm64 → 该具体 MIB 名 [unsourced])。
- `df -h` — 显示全部挂载文件系统的剩余空间，`-h` 为 base-2 人读单位；APFS 场景每个卷显示容器内可用量。来源：https://www.unix.com/man-page/osx/1/df/ (fetched)
- `system_profiler SPHardwareDataType` — 硬件配置报告；可用数据型筛选（SPSoftwareDataType/SPHardwareDataType 等）、`-detailLevel mini|basic|full`、默认超时 180 秒、进度信息在 stderr。来源：https://www.unix.com/man-page/osx/8/system_profiler/ (fetched)
- Rosetta 2（Apple 芯片转译 x86_64）事实页：https://developer.apple.com/documentation/apple-silicon/about-the-rosetta-translation-environment — 仅抓到页面标题级内容，正文未可见 → Rosetta 具体事实 [unsourced]，不得据此下断言。
- 老机器 Monterey 特情：具体机型兼容列表需 Apple 支持文档逐机型核验，本任务未抓取成功 → [unsourced]；可用 `system_profiler SPHardwareDataType` 的 Model Identifier 作为后续查表的键（该字段存在于 system_profiler 输出，抓到的 man 页只列 dataType 不含字段清单 → 字段名 [unsourced]，需实机验证）。

### Windows（设备档案卡）
- `systeminfo` — 显示 OS 配置、RAM、磁盘空间、网卡等；支持 `/fo TABLE|LIST|CSV`、可远程 `-s <computer>` 探测。来源：https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/systeminfo (fetched)
- `winget install [--id <ID>] [--scope user|machine]` — 安装指定应用；query 默认大小写不敏感子串匹配，`-e` 精确匹配；`--scope` 官方直连 "known issues relating to package installation scope"（user vs machine 的已知问题文档）。来源：https://learn.microsoft.com/en-us/windows/package-manager/winget/install (fetched)
- winget 其他探测命令：`winget search`（找应用）、`winget list`（已装）、`winget --version`（客户端版本）——文档在 install 页 Related topics 与 Usage 段被引用，本任务未单独抓取 → [unsourced] 需按页核验。
- winget 日志位置：%LOCALAPPDATA%\Packages\Microsoft.DesktopAppInstaller_8wekyb3d8bbwe\LocalState\DiagOutputDir\*.log。来源：同上 install 页 (fetched)
- `Get-PSDrive [-PSProvider FileSystem]` — 列驱动器 Used(GB)/Free(GB)——文档示例中 C 盘显示 Used/Free 两列；`Get-CimInstance -ClassName Win32_LogicalDisk` 亦可取磁盘容量。来源：https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.management/get-psdrive?view=powershell-7.4 (fetched)

### Debian（设备档案卡）
- `dpkg-query -W -f='${binary:Package} ${Version} ${Architecture} ${Installed-Size}\n'` — 查 dpkg 数据库；`-W/--show` 配 `-f/--showformat` 自定义输出，可引用字段含 Package、Version、Architecture、Installed-Size、Status（默认格式 `${binary:Package}\t${Version}`）；`-l` 列全部匹配包。来源：https://manpages.debian.org/bookworm/dpkg/dpkg-query.1.en.html (fetched, dpkg 1.21.22)。
- `apt-get -s install PKG`（--simulate/--just-print/--dry-run/--recon/--no-act）— 不落盘的模拟安装，输出计划中的 dpkg 操作行（Inst/Remv/Conf）；非 root 也可跑。来源：https://manpages.debian.org/bookworm/apt/apt-get.8.en.html (fetched, apt 2.6.1)。
- `apt-get --print-uris install PKG` — 只打印待下载 URI，每个 URI 带文件 size 与期望 MD5（不下盘）。来源：同上 (fetched)。
- `apt-get -d install PKG`（--download-only）— 只下载不解包不安装，可用于落盘前测量下载体。来源：同上 (fetched)。
- Debian 版本代号确认：manpages.debian.org 中 bookworm（Debian 12）为现存发布分支之一，另有 testing/unstable；本文件 Debian 命令事实均取自 bookworm 页面。来源：同上两页页眉 (fetched)。
- lsb_release 与 systemctl 探测：见下方补充（抓取进行中）。

### Debian（补充）+ macOS bottle 分发事实
- `lsb_release` man 页定位：bookworm 的 lsb-base 包在该路径 404，页面提示 trixie 改由 lsb-release 包提供 `/trixie/lsb-release/lsb_release.1.en.html` → 命令语义页按 trixie 抓取；「发行代号/codename 查询」的文档侧确认由该 man 页承载。来源（404 页本体，fetched）：https://dyn.manpages.debian.org/bookworm/lsb-base/lsb_release.1.en.html 与 https://manpages.debian.org/bookworm/lsb-base/lsb_release.1.en.html（被重定向到 dyn，间接证实包名变更）。具体命令开关明细见 trixie 页（下次抓取）。
- `systemctl is-enabled SERVICE` — 退出码 0=enabled；输出态含 enabled/disabled/static/masked/linked 等（systemd 262 手册 Table 3）。来源：https://www.freedesktop.org/software/systemd/man/latest/systemctl.html (fetched)
- `systemctl is-active SERVICE` / `systemctl status` — 运行时状态探测；status 的 Loaded: 行显示 enablement 状态，Active: 行显示运行状态。来源：同上 (fetched)
- 「enable ≠ start」官方原文：Enabling units should not be confused with starting (activating) units, as done by the start command. Enabling and starting units are orthogonal — 这正是「apt 装了服务但没 enable/没 start」翻车模式的机制依据。来源：同上 (fetched)
- `systemctl daemon-reload` — 重载全部 unit 文件并重建依赖树；升级/装新 unit 后需执行。来源：同上 (fetched)
- Homebrew formula JSON（formulae.brew.sh/api/formula/jq.json，fetched）：bottle 按 macOS 版本×架构分发（arm64_tahoe / arm64_sequoia / arm64_sonoma / sonoma / arm64_linux / x86_64_linux），每文件带 cellar 与 sha256；**无 size 字段** → Homebrew 预装体积不能从这个 API 直接读数 [unsourced via this API]。generated_date: 2026-11-01（fetched）。

### Debian（lsb_release 补充）
- `lsb_release`（trixie 12.1-1）：裸 POSIX shell 实现，读 /etc/os-release（覆盖 /usr/lib/os-release）；开关 -i/-d/-r/-c/-a/-s/-v/-h；与旧 Python 版字节级兼容输出。来源：https://manpages.debian.org/trixie/lsb-release/lsb_release.1.en.html (fetched)。bookworm 路径 404 → 老系统上可能无此 man 页/由 lsb-base 提供，需按目标机实际核。

## Size measurement
- macOS 安装前：formulae.brew.sh API 有 bottle 分架构×OS 版本 URL 与 sha256，**无 size 字段** → 体积读数 [unsourced via this API]；bottle URL HEAD 取 Content-Length 的办法本任务未验证 → [unsourced]。来源：https://formulae.brew.sh/api/formula/jq.json (fetched, generated_date 2026-11-01)。
- macOS 安装后：`df -h` 量剩余空间（-h 为 base-2 人读单位，支持多文件系统过滤）→ 来源 https://www.unix.com/man-page/osx/1/df/ (fetched)。`du -sh <路径>` 量目录体积为 POSIX 常识，但本任务未抓 du(1) man 页 → [unsourced]。
- Windows 安装前：winget 文档不提供体积字段（install 页选项无 size；show 页未抓）→ [unsourced]。诊断日志目录 %LOCALAPPDATA%\Packages\Microsoft.DesktopAppInstaller_8wekyb3d8bbwe\LocalState\DiagOutputDir 已取证（troubleshooting 页）。
- Windows 安装后：`Get-PSDrive [-PSProvider FileSystem]` 输出 Used(GB)/Free(GB)（docs 示例）；`Get-CimInstance -ClassName Win32_LogicalDisk` 输出 Size/FreeSpace。→ 来源 https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.management/get-psdrive?view=powershell-7.4 (fetched)。目录递归求和的 Measure-Object 用法未抓 → [unsourced]。
- Debian 安装前：`apt-get -s install PKG` 模拟（不改系统、非 root 可跑，打印计划中的 dpkg 操作行 Inst/Remv/Conf）；`apt-get --print-uris install PKG` 打印每个待下载 URI 的 size 与 MD5。→ 来源 https://manpages.debian.org/bookworm/apt/apt-get.8.en.html (fetched, apt 2.6.1)。流传的 "Need to get X MB of archives" / "After this operation, Y MB of additional disk space will be used." 字样未在本次抓取的 man 页逐字出现 → 该引用字样 [unsourced]，需实跑 apt-get -s 记录。
- Debian 安装后：`dpkg-query -W -f='${Installed-Size}\n' PKG` 取 Installed-Size 字段（字段存在已取证）→ 来源 https://manpages.debian.org/bookworm/dpkg/dpkg-query.1.en.html (fetched)。该字段单位（KiB）未在该页明示 → [unsourced unit]。

## Failure modes (numbered, URL each)
出处标注约定：HN 条目 URL 经 hn.algolia.com API 检索响应（fetched）见到 title/points/comments/正文快照，未逐条打开讨论页正文；GitHub 条目 URL 经 api.github.com search issues 响应（fetched）见到标题与 issue body。

1. macOS — 架构错装（x86_64 二进制上 Apple Silicon）：`zsh: bad CPU type in executable`；作者在 Windows 机器上一切正常，到 Mac 才炸（正是「假设继承」实例）。出处：https://news.ycombinator.com/item?id=50207196（"Tell HN: Steamcmd doesn't work on Apple Silicon"，2026-10，6 评；快照含 `file steamcmd` → Mach-O 64-bit x86_64 与 Godot universal binary 对比）。
2. macOS — Homebrew 侧同类：`/usr/local/Cellar/teemoji/.../libexec/teemoji: Bad CPU type in executable`。出处：https://news.ycombinator.com/item?id=42879613（Teemoji 帖评论，2025-01）。更早：Homebrew 部分 broken package 在 Catalina 上同报错 https://news.ycombinator.com/item?id=21186260 。
3. macOS — 老机器系统上限（Monterey 特情类）：官方不再支持老 Intel 机型，OpenCore Legacy Patcher 是非官方续命路线；有人用 OCLP 让 11 年机 MacBook Air 跑 Monterey。出处：https://news.ycombinator.com/item?id=35538223 （205 分/84 评）；https://news.ycombinator.com/item?id=31823892 （"resurrected an 11 year old Macbook Air running Monterey"）；机型清单页 https://dortania.github.io/OpenCore-Legacy-Patcher/MODELS.html 与 MONTEREY-DROP 页来自 Algolia 快照 URL，本任务未直接抓取内容 → [unsourced]。
4. macOS — 磁盘满：Intel Mac APFS 满后连删文件都报 no space left on device，需重启/恢复模式才能清；连带 Chrome 清空本地数据。出处：https://news.ycombinator.com/item?id=49347120 （Linux 7.3 vRAM 帖评论，2026-08）。
5. Windows — winget scope 不确定：官方 troubleshooting 明示 "Not all installers support installing in 'user' scope vs. 'machine' scope consistently"；EXE 安装器 scope 行为不确定、user scope 可能仍需 UAC、是否管理员组会影响 installer 自决 → 「装完找不到」类问题的官方机制出处。来源：https://learn.microsoft.com/en-us/windows/package-manager/winget/troubleshooting#scope-for-specific-user-vs-machine-wide (fetched)；scope 已知问题清单（本任务未抓内容）：https://github.com/microsoft/winget-cli/issues?q=is%3Aissue+is%3Aopen+label%3Aarea-scope → [unsourced]。
6. Windows — 装失败的可见性：winget 退出码表（`winget error 1603`）、exe 安装器错误码非标准；403 可因 ISV 封禁 WinGet UA。来源：同上 troubleshooting 页 (fetched)。补充：Win11 的 TPM/CPU 门槛迫使人绕过或留守 Win10 — https://news.ycombinator.com/item?id=49403775 （hdiutil deprecated in macOS 27 帖评论；「You might be able to do that in Windows, if you bypass TPM and CPU requirements or if you stay with Windows 10 extended updates」）。
7. Debian — 装了服务却没起来：Debian 惯例是装上即 enable/start（Qubes#2238 引述 "Debian policy of 'enable services for all installed software'"，https://github.com/QubesOS/qubes-issues/issues/2238 ；purpleidea/mgmt#29 对比 redhat 不自动起，https://github.com/purpleidea/mgmt/issues/29 ），但容器/镜像里 policy-rc.d（exit 101）会拦下启动且无后续提示：tabcomputer#149「denies service starts during apt install; nothing starts the service afterwards or says how」https://github.com/williamsharkey/tabcomputer/issues/149 ；calaos-build PR#1：Docker 的 policy-rc.d stub 漏进 rootfs 导致升级也不重启服务 https://github.com/calaos/calaos-build/pull/1 ；deb-systemd-invoke 遵循 policy-rc.d（openvox-ca PR#558）https://github.com/voxpupuli/openvox-ca/pull/558 。机制依据（官方）：enable 与 start 正交、is-enabled/is-active/status 可查 — https://www.freedesktop.org/software/systemd/man/latest/systemctl.html (fetched)。
8. Debian — 装成功≠服务健康：UGreen NAS 上 Debian 12 bookworm，tailscale 更新"安装完成"但服务起不来（fatal error）。出处：https://github.com/tailscale/tailscale/issues/21707 （body via api.github.com search issues, fetched）。
9. Debian/Linux — 体积与磁盘假象：/tmp 淤积触发 "no space left on device" 而 df 看着半空（自建 CI runner 案例，附 systemd-tmpfiles 清理方案）https://news.ycombinator.com/item?id=49612773 ；df 的 Available=0 但 Use% 卡在 94.9% 导致监控漏报 https://news.ycombinator.com/item?id=5440148 ；安装期 ENOSPC 但实际有大量空间 https://news.ycombinator.com/item?id=49081588 。
10. Cross — 假设继承/works on my machine：219 分巨帖 "It works on my machine turns to it works in my container" https://news.ycombinator.com/item?id=36885598 ；梗源 "Works on My Machine certification" https://news.ycombinator.com/item?id=2068555 。本条与设备卡规则的映射：A 机器事实不许可 B 机器结论（steamcmd 帖 #1 是同一病征的实例）。
