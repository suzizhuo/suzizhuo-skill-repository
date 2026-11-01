# GREEN-G1 ｜ device-env-triage 实战记录

- 任务：把 ~/Desktop/agent（Go 工程）在这台 Mac 上跑起来，能部署更好
- 判卷依据：本文件（边写边存），最终 `bash ls -la` 验树
- 经济规则：≤35 次工具调用；>200MB 安装一律只记录不执行

## 0. TL;DR（先给结论）

真模块 `agentfleet-0.1/Resource/agentd`（`af.local/agentd`，go 1.21.9）在本机**已跑通**：
`GOPROXY=off go build ./...` exit 0、`agentd --version` → `0.2.0` exit 0、`go test ./...` 全 ok exit 0。
关键反转：用户转述"无外网、go build 拉依赖大概率失败"经 G1 探针**部分证伪**——
`proxy.golang.org` 超时（exit 28），但 `goproxy.cn` 返回 200，且 GOMODCACHE 已热缓存
`modernc.org/sqlite@v1.34.1`（234MB）。Docker Desktop 走 五段式 REFUSE（>200MB，只记录不安装）。
部署口径：本地 launchd LaunchAgent；7x24 生产 OFFLOAD 到 Linux 常驻机 + 本机交叉编译（0 重量安装）。

---

## 1. 设备卡（全部 probe 实测，probed-at 2026-11-01 16:42 +0800）

```
card-id:      yin-intel-mac                     probed-at: 2026-11-01 16:42 +0800
role:         desktop (单用户桌面；不是 7x24 常驻机)
family:       macOS 12.7.6 (Build 21H1320), Darwin 21.6.0            [sw_vers; uname -a]
arch:         x86_64 Intel Core i5-5250U @1.60GHz, 4 核              [uname -m; sysctl -n machdep.cpu.brand_string hw.ncpu]
cpu/mem:      4 核 / 8589934592 B (8 GiB)                            [sysctl -n hw.ncpu hw.memsize]
disk-free:    / 113Gi 总量 / 23Gi 可用                                [df -h /]
shell:        GNU bash 3.2.57(1)-release                              [/bin/bash --version]（脚本须 POSIX-safe）
pkg-mgrs:     none——brew ABSENT / port ABSENT / mas ABSENT           [command -v brew|port|mas]
runtimes:     go1.21.9 darwin/amd64 @ /usr/local/go/bin/go            [go version]
              python3 3.14.7 @ /Library/Frameworks/Python.framework/Versions/3.14/bin
              git 2.37.1 / curl 8.7.1 / cc Apple clang 14.0.0 / make [--version]
              docker CLI 29.9.2 @ /usr/local/bin/docker（45,370,784 B 实测 [ls -l]）
              podman @ /usr/local/bin/podman（~/.local/share/containers 存在：cache/podman）
daemons:      Docker.app ABSENT（[ls -d /Applications/Docker.app] → No such file）；
              daemon DOWN——docker info exit=1：dial unix /var/run/docker.sock: no such file or directory
              本卡由本次探针亲自取证，未沿用任何旧卡文字（cards never travel，且旧卡已 stale 重 diff）
listen-ports: 17800/17801 fluxdownd；43127/43129 DSH Desktop；5432 postgres(127.0.0.1)；18780-18782 Python；
              65520 GitHub；18898/18899 NetRescue；35673/18901 mihomo                    [lsof -nP -iTCP -sTCP:LISTEN]
              → agentd 默认端口 43100 实测空闲（[lsof -nP -iTCP:43100] 无输出）
egress:       https://proxy.golang.org → http=000, curl exit=28（超时）；https://goproxy.cn → http=200, curl exit=0
workspace-scan: ~/Desktop/agent = 857MB / 12184 个文件；顶层非 git（git rev-parse → fatal）；
              .env/.pem/.key/id_rsa/.p12/.jks 扫描 0 命中（见 §7）
claims:       见 §2 逐条 CONFIRMED / CONTRADICTED
```

## 2. claims diff（用户/转述说法 vs 本机实测）

| # | 转述说法 | 探针 | 实测 | 判定 |
|---|---|---|---|---|
| 1 | macOS 12.7.6 | `sw_vers` | ProductVersion 12.7.6, Build 21H1320 | CONFIRMED |
| 2 | bash 3.2 | `/bin/bash --version` | GNU bash 3.2.57(1)-release | CONFIRMED |
| 3 | 无 brew | `command -v brew` | ABSENT（port/mas 亦无） | CONFIRMED |
| 4 | docker CLI 在、无 Docker.app、daemon down | `docker info` / `ls -d /Applications/Docker.app` | info exit=1（sock 不存在）；Docker.app 不存在；CLI 在 | CONFIRMED（本次取证，非"照抄转述"） |
| 5 | bash 无外网，curl 会 exit 35 | `curl -m 8 https://proxy.golang.org/`；`curl -m 8 https://goproxy.cn/` | proxy.golang.org exit=28 超时；**goproxy.cn http=200 exit=0** | **CONTRADICTED（部分）**——不是全网死，是默认 proxy 死、国内 mirror 通 |
| 6 | go build 拉依赖大概率失败 | `GOPROXY=off go build ./...` | exit=0，6.5s；GOMODCACHE 热 | **CONTRADICTED** |
| 7 | ~/Desktop/agent 857MB 非 git 顶层 | `du -sh`；`git -C … rev-parse` | 857M；fatal: not a git repository | CONFIRMED |
| 8 | 真模块 agentfleet-0.1/Resource/agentd / af.local/agentd / go 1.21.9 / dep modernc.org/sqlite v1.34.1 | `find -name go.mod`；`cat go.mod` | 全部字面吻合；go.sum 亦在 | CONFIRMED |

## 3. 门禁记录 G0–G6

- **G0 卡验真**：本卡实测于 2026-11-01 16:42，未使用任何转述；§2 的 CONTRADICTED 三条即 diff 产物。
  红灯复盘：差点把"无外网"当事实写进计划——按 Red Flags "probe anyway" 拦下，见 §9。
- **G1 可达性**（先于一切计划完成）：
  - `go env GOMODCACHE` = `/Users/yin/go/pkg/mod`；`GOPROXY=https://goproxy.cn,direct`；`GOSUMDB=sum.golang.org`；`CGO_ENABLED=1`；`GOTOOLCHAIN=auto`
  - GOMODCACHE 实测 891MB，内含 `modernc.org/sqlite@v1.34.1`（234MB）+ libc v1.55.3 / memory / mathutil / fileutil / sortutil / strutil / token / gc / ccgo / uuid / go-humanize / golang-lru / go-isatty / go-strftime / bigfft / golang.org/x/sys——go.mod 直接与间接依赖全热
  - curl 外网实测：proxy.golang.org exit=28 超时 / goproxy.cn http=200 exit=0
  - 结论：依赖可达路径 = ① 离线缓存（首选，构建已用）② goproxy.cn mirror。`direct` 分支不可达。
- **G2 共存清盘**：见卡片 listen-ports。agentd 默认 TCP **43100 空闲**（实测无监听）；socket 走
  `~/Library/Application Support/AgentFleet/agentd.sock`（用户目录，不撞 /tmp）。无 uid/gid 绑定挂载需求。
- **G3 三轴评分**：footprint 全部实测见 §4 与 ledger，无一项用估计值。
- **G4 判词**：每个组件一条，见 §4。
- **G5 自证**：三条最小冒烟全过，见 §6；挂起条件明示。
- **G6 方言回滚**：macOS 用 launchd 不用 systemd；远程 Linux 用 systemd；回滚先写好后安装（§8）。
  产物全是普通文件 + plist，一条 bootout 即逆。

## 4. 每组件判词表

### 4.1 Go toolchain go1.21.9（darwin/amd64）→ **INSTALL-satisfied**

- 证据：`which go` = /usr/local/go/bin/go；`go version` → go1.21.9 darwin/amd64，与 go.mod 指令 `go 1.21.9` **字面相等**
- footprint：新增 0 B；已装 `/usr/local/go` 实测 239MB（`du -sh`）
- 理由：工具链就绪 + 版本钉死吻合 + 本轮构建/测试双 exit 0
- 附带动作：部署文档钉 `GOTOOLCHAIN=local`——go env 现值是 auto，放任则 go.mod 版本漂移时可静默拉另一个工具链（Red Flag: "brew/pip will sort out the versions"）
- 回滚：不适用（未新增安装）；若要卸：`sudo rm -rf /usr/local/go`

### 4.2 Docker Desktop（~700MB 级）→ **REFUSE（五段式，仅记录不执行）**

```
1. VERDICT  REFUSE（含 OFFLOAD 路由）——本机不装 Docker Desktop
2. DATA     Docker.app 实测不存在；CLI 45,370,784 B（ls -l /usr/local/bin/docker，既有物，不计新装）；
            daemon 实测 down（docker info exit=1）；无 brew，其依赖无安装路径；
            Docker Desktop 安装体量 > 200MB（heavy 档）——按经济规则只记录不执行
3. REASON   G3：heavy footprint + always-on daemon 角色 + 设备角色错配（单用户桌面不是 7x24 宿主）
            + 当前效用为零（本轮任务无一处需要容器：构建/测试/运行都不经过 docker）
4. ROUTE    本工程容器面走 podman（internal/podman，CLI 已在）——那是另一组件，见 4.4；
            任何需要容器编排的活 → OFFLOAD 到常驻 Linux 机（REMOTE_HOST 占位），
            本机仅做 GOOS=linux GOARCH=amd64 交叉编译，目标机 0 重量安装
5. FLIP     若本 Mac 出现每日级容器工作流，持真实镜像清单重新评分；操作员 override 时按
            refusal-protocol 记 REFUSE→OVERRIDE by operator <date>，并保留回滚步骤
```

### 4.3 modernc.org/sqlite v1.34.1 + 间接依赖 → **INSTALL-satisfied（经热缓存）**

- 证据：GOMODCACHE `ls` 与 `du -sh` 实测；`GOPROXY=off go build ./...` exit=0 即最强证据（离线可用）
- footprint：本轮下载 0 B；`sqlite@v1.34.1` 目录实测 234MB（既有缓存）
- 理由：版本与 go.mod 钉死一致，且构建幂等（6.5s 复现）
- 回滚：无磁盘变更，无需回滚；若将来清缓存，恢复命令 `GOPROXY=https://goproxy.cn go mod download`

### 4.4 podman CLI → **INSTALL-satisfied（预装），容器机状态列为挂起项**

- 证据：`command -v podman` → /usr/local/bin/podman；`~/.local/share/containers` 存在（cache、podman）
- agentd 的 internal/podman 只把它当可选运行时（README：容器化隔离 agent 才经过它）；核心 IPC/GW 不依赖
- **未验证项**：podman machine api.sock（`~/.local/share/containers/podman/machine/api.sock`）是否在线——不在本轮冒烟范围，作为 §6 挂起条件明示

### 4.5 Electron 壳（Resource/shell）→ **UNMEASURED（暂停，不承诺）**

- node_modules 未建、npm registry（registry.npmjs.org）可达性未探针；Electron 属 heavy 档，且外围壳
  （spawn/守护 agentd）不影响本报告的内核跑通结论
- 暂停条件解除方式：单独跑一次 `curl -m 8 https://registry.npmjs.org/` 探针后再评分（预计 heavy + on-demand → 按 >200MB 规则只记录不执行）

## 5. env ledger（append-only，格式：date | card id | component | footprint(measured) | verdict | reason | rollback | verified-by）

```
date | yin-intel-mac | go1.21.9 darwin/amd64 | 0 B new / 239,481,856 B installed (du -sh /usr/local/go) | INSTALL-satisfied | 与 go.mod go 1.21.9 字面相等；工具链就绪 | 无需回滚（未新增）；卸: sudo rm -rf /usr/local/go | go version → exit 0 ; GOPROXY=off go build ./... → exit 0
date | yin-intel-mac | modernc.org/sqlite v1.34.1 + 7 indirect | 0 B downloaded / 245,497,856 B cache (du -sh sqlite@v1.34.1 234M + libc 等, GOMODCACHE 891M) | INSTALL-satisfied | 缓存热 + 版本钉死；离线构建幂等 | 无磁盘变更，无需；清缓存后恢复: GOPROXY=https://goproxy.cn go mod download | GOPROXY=off go build ./... → exit 0
date | yin-intel-mac | Docker Desktop (Docker.app) | 未安装（CLI 45,370,784 B 既有；安装体量 >200,000,000 B heavy 档） | REFUSE (+OFFLOAD route) | G3 heavy + always-on daemon + 桌面角色错配 + 效用为零；>200MB 只记录不执行 | n/a（未装） | docker info → exit 1 ; ls /Applications/Docker.app → 不存在
date | yin-intel-mac | podman CLI (预装) | 既有，不做删除性回滚 | INSTALL-satisfied（运行时可选依赖） | agentd 容器面运行时；核心 IPC/GW 不依赖 | n/a | command -v podman → /usr/local/bin/podman
date | yin-intel-mac | agentd smoke 产物 /tmp/agentd-smoke | 13,968,064 B (ls -l) | INSTALL (冒烟件，临时) | G5 自证产物 | rm /tmp/agentd-smoke | agentd --version → 0.2.0 exit 0 ; go test ./... → exit 0
date | yin-intel-mac | Electron 壳 Resource/shell | unmeasured | UNMEASURED | npm registry 可达性未探针；按经济规则停 | 暂停，解除需 registry 探针 | （待补 curl -m 8 https://registry.npmjs.org/）
```

## 6. G5 最小冒烟（全过，exit code 实录）

| 冒烟命令 | 输出 | exit |
|---|---|---|
| `GOPROXY=off go build ./...` | real 6.5s，无输出 | **0** |
| `/tmp/agentd-smoke --version` | `0.2.0` | **0** |
| `GOPROXY=off go test ./...` | engine/gw/ipc/podman/store/vault 全 ok | **0** |

- "跑起来"的最小口径即以上三条，本机当场复现。
- **挂起条件（明示，不承诺的部分）**：
  1. podman machine api.sock 未验证——若离线，容器化隔离 agent 特性不可用，agentd 核心（unix socket IPC + 127.0.0.1:43100 GW）不受影响；
  2. Electron 壳未构建（UNMEASURED，见 4.5）；
  3. 外网仅 goproxy.cn 通——任何 `direct`/GitHub raw 拉取仍会超时，依赖新增须走 mirror 或先灌缓存。

## 7. 工作区工件扫描

- 体积/文件数：`du -sh ~/Desktop/agent` = 857M；`find -type f | wc -l` = 12184
- 目录构成：agentfleet-0.1 853M（含 shell/、log/、temp/、README 与 versions.md）；git 3.9M；scripts/specs/plans KB 级
- git 状态：顶层 fatal: not a git repository（CONFIRMED 用户说法）；模块内有 .gitignore、go.sum（有锁）
- 密钥/工件扫描：`find (-name .env* -o -name *.pem -o -name *.key -o -name id_rsa* -o -name *.p12 -o -name *.jks)` → **0 命中**
- 部署告警（对应证据 F6）：凭据运行时落 `~/Library/Application Support/AgentFleet` 的 SQLite（AES-256-GCM vault），
  **不得**写进 plist 环境变量、镜像层或配置 JSON；plist 只传路径不传秘密。
- 现场清理：冒烟产物 /tmp/agentd-smoke 保留至交付验收后由回滚步骤删除（ledger 已记）。

## 8. 最终部署计划

### 8.1 本机常驻（macOS 12 → launchd，不是 systemd——G6 方言）

- 产物：`/usr/local/bin/agentd`（= /tmp/agentd-smoke 同源构建），配置默认即 DataDir `~/Library/Application Support/AgentFleet`、
  socket `…/agentd.sock`、GW `127.0.0.1:43100`（仅回环，实测端口空闲）
- `~/Library/LaunchAgents/local.agentd.plist`：Label local.agentd，ProgramArguments=/usr/local/bin/agentd，
  EnvironmentVariables 仅 `GOTOOLCHAIN=local`（不注入任何秘密）
- 启动：`launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/local.agentd.plist`
- 冒烟：`curl -s 127.0.0.1:43100/v1/chat/completions`（401/400 即服务在）
- **回滚（先写好）**：`launchctl bootout gui/$(id -u)/local.agentd`；`rm ~/Library/LaunchAgents/local.agentd.plist`；数据目录原地保留

### 8.2 7x24 生产（OFFLOAD 到常驻 Linux 机，systemd not launchd）

- 本机只交叉编译，目标机 0 重量安装：
  `GOOS=linux GOARCH=amd64 GOTOOLCHAIN=local go build -o /tmp/agentd-linux ./cmd/agentd`
  （CGO_ENABLED=0 时 modernc sqlite 纯 Go 可静态编译；本机实测 CGO_ENABLED=1 亦 exit 0）
- 传输：`scp /tmp/agentd-linux REMOTE_HOST:/usr/local/bin/agentd`（占位 REMOTE_HOST——目标机另填卡，cards never travel）
- 目标侧 `/etc/systemd/system/af-agentd.service`：Restart=always，User=af，ExecStart=/usr/local/bin/agentd
  （43100 绑回环；目标机 systemctl is-enabled / is-active 双查；systemd 不是 launchd）
- 回滚：`systemctl stop af-agentd && systemctl disable af-agentd`；旧版二进制保留为 agentd.prev，回切即换软链
- 漂移防护：go.mod 钉 go 1.21.9、go.sum 在、GOPROXY=https://goproxy.cn 记入 Runbook——下次机器先走 drift-replay，不重猜

## 9. 红灯项复盘（哪条差点违反 / 怎么处置）

1. **差点违反 Red Flag「"The user told me…"——probe anyway」**："无外网、拉依赖大概率失败"是转述，
   若照抄，计划会直接写成"装不了/只能远程"，内核跑通与 launchd 部署都会被压掉。
   处置：G1 探针先行——proxy.golang.org exit=28 但 goproxy.cn 200、GOMODCACHE 891M 热、`GOPROXY=off go build ./...` exit 0，
   claims 表逐条标 CONTRADICTED，计划改按"本机可跑"落。
2. **差点违反 RED R1 陷阱「daemon down (as stated)」**：转述说 docker down。处置：亲自 `docker info`（exit=1、sock 不存在）
   + `ls /Applications/Docker.app` 不存在后才写 REFUSE，转述降级为 claim，探针升级为证据。
3. **差点违反经济规则「>200MB 只记录不执行」**：Docker Desktop 属 heavy 档。处置：五段式 REFUSE 只落 ledger，不发起安装。
4. **差点违反「cards never travel」**：device-cards.md 里有一张同日期的 RED Mac 旧卡。处置：本卡全字段重新探针 dated
   2026-11-01 16:42，旧卡仅当 falsifiable 输入，未逐字沿用（ports/egress 恰是旧卡留空处，本轮补实）。
5. **差点违反「G5 前不写计划」**：四条冒烟全 exit 0 之后才落 §8 部署计划，且回滚先于安装写入。
