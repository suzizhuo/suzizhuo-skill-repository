# green-g2 — agentd 上远程 Debian 7x24 ｜ device-env-triage 实战报告

- 日期：2026-11-01（+08:00）
- 场景（YG 原话）：「同一个 Go 工程，部署到那台远程 Debian 上跑，7x24 挂着。」
- 依据：SKILL.md Loop（CARD → G0–G6 → VERDICT → PLAN → LEDGER）+ references/device-cards.md、tri-scale.md、refusal-protocol.md
- 结论一句话：**OFFLOAD 构建 + INSTALL 服务**——Mac 交叉编译（0 字节工具链上靶），目标机只落 1 个静态二进制 + 1 个 systemd unit；目标卡全字段「待探」，无一条假设。

---

## ① 目标机设备卡（全新卡，禁止从 Mac 卡继承）

```
card-id:      debian-remote-agentd (待命名)     probed-at: 未探（SSH 禁连，全字段待探）
role:         always-on host（7x24 服务宿主 = 正确角色）
family:       Debian（版本待探）
release:      待探 —— /etc/os-release
arch:         待探 —— dpkg --print-architecture / uname -m（禁止写 "amd64 是常识"）
cpu/mem:      待探 —— nproc / free -m
disk-free:    待探 —— df -h /
pkg-mgrs:     apt 存在性待探 —— command -v apt-get（Debian 默认有，仍要探）
runtimes:     Go 有无待探 —— command -v go && go version || echo "go ABSENT"
daemons:      systemd 待探 —— systemctl --version | head -1
listen-ports: 待探 —— ss -tlnp（G2 共存清盘，建端口决策之前必须跑）
services:     待探 —— systemctl list-units --type=service --state=running --no-pager
egress:       待探 —— curl -sI -o /dev/null -w '%{{http_code}}' https://goproxy.cn（G1；本方案不依赖它）
```

**必须在 REMOTE_HOST 上逐条跑的确切探测命令**（`REMOTE_HOST`/`REMOTE_USER` 为占位符，一律不真连）：

```bash
# P0 三条（卡的主字段，缺一张卡就不成立）
ssh REMOTE_USER@REMOTE_HOST 'dpkg --print-architecture; uname -m'
ssh REMOTE_USER@REMOTE_HOST 'cat /etc/os-release; command -v lsb_release >/dev/null && lsb_release -a || echo "lsb-release: ABSENT"'
ssh REMOTE_USER@REMOTE_HOST 'command -v go >/dev/null && go version || echo "go: ABSENT"'

# G2 共存清盘（端口与服务，直接喂第⑤节决策）
ssh REMOTE_USER@REMOTE_HOST 'ss -tlnp'
ssh REMOTE_USER@REMOTE_HOST 'systemctl list-units --type=service --state=running --no-pager | head -40'

# 资源与 systemd 能力
ssh REMOTE_USER@REMOTE_HOST 'df -h /; nproc; free -m; systemctl --version | head -1'

# claims 复核（YG 口述 MonkeyCode compose + admin nginx 8080/8443 → 未探明前只是 claim）
ssh REMOTE_USER@REMOTE_HOST 'docker compose ls; docker compose ps'
ssh REMOTE_USER@REMOTE_HOST 'sudo nginx -T 2>/dev/null | grep -nE "listen|server_name" | head -20'
```

`claims` 行：①「MonkeyCode compose 在跑（db/migrate/rustfs/backend 内网 + admin nginx 对外 8080/8443）」→ **CLAIM，待探**（探明命令如上）。②「Go 有无」→ 待探，不得引用 Mac 上 `go1.21.9 darwin/amd64`。③「amd64」→ 待探（RED R2 同款陷阱，见⑦）。

⚠️ **与 Mac 卡不混用显式声明**：本卡每个字段都是 REMOTE_HOST 上的探测输出；`yin-intel-mac` 卡（macOS 12.7.6/x86_64/go1.21.9 darwin/amd64，2026-11-01 复检一致）**只描述构建机**，不向本卡传递 arch、OS 版本、Go 存在性、端口、服务任何一项。Cards never travel。

---

## ② 安装清单与体积（实测，非回忆）

| # | 组件 | 落在哪 | download | installed | 处置 |
|---|---|---|---|---|---|
| 1 | agentd 静态二进制 | 目标机 | 0（本地构建） | **9,441,280 B**（`ls -l` 实测，stripped，ELF x86-64 static） | INSTALL（传输上靶） |
| 2 | systemd unit 文件 | 目标机 | 0 | ~1 KB（见④） | INSTALL |
| 3 | /var/lib/agentd 数据目录（sqlite） | 目标机 | 0 | 按运行增长（初建 ~MB 级） | INSTALL |
| 4 | Go 1.21.9 工具链 | **不落目标机** | ~67 MB | ~350 MB（RED R2 实测口径） | **OFFLOAD：交叉编译；>200MB 只记录不执行**。重测命令：`apt-get -s install golang-go; apt-get --print-uris install golang-go` 或下载 tarball 后 `ls -l` |
| 5 | apt 依赖（libc 等） | 目标机 | 0 | 0 | **不需要**——`CGO_ENABLED=0` 纯 Go 静态二进制（modernc.org/sqlite 为 ccgo 纯 Go 实现，已在构建中验证） |

Mac 侧新增安装：**0 字节**（go1.21.9 今日复检在位：`go version → go1.21.9 darwin/amd64`）。

---

## ③ 交叉编译决策（本机 0 字节上靶）

- **决策**：在 yin-intel-mac 上交叉编译，目标机不装工具链。理由：工具链是构建期角色、7x24 宿主永不需要它（tri-scale: 角色错配 → OFFLOAD，route = 构建机）。
- **命令（已冒烟，G5 通过）**：

```bash
cd ~/Desktop/agent/agentfleet-0.1/Resource/agentd
GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -ldflags="-s -w" -o agentd-linux-amd64 ./cmd/agentd
# exit 0；2.6s；产物 ELF 64-bit x86-64, statically linked, stripped, 9,441,280 B
# go version -m：go1.21.9 + modernc.org/sqlite v1.34.1 等依赖全部钉版本（go.sum 在库）
```

- **GOARCH 条件分支（目标 arch 待探，禁止预设）**：
  - 探明 amd64 → 用上面产物，传输即走。
  - 探明 arm64（或其它）→ 一条命令重编，其余方案不变：
    `GOOS=linux GOARCH=arm64 CGO_ENABLED=0 go build -ldflags="-s -w" -o agentd-linux-arm64 ./cmd/agentd`
- **离线保障**：模块缓存已温（`$(go env GOMODCACHE)/modernc.org/sqlite@v1.34.1` 在库），本机 bash 无外网也可完成构建——构建便宜不是"目标预装"造成的（RED R1 教训），而是缓存温 + 交叉编译两个可证事实。
- **冒烟边界**：本机只能证"构建产物正确"（build exit 0 + file + go version -m）；"在目标跑起来"的冒烟挂起，条件 = REMOTE_HOST 探测通过后：`scp` 上靶 → `systemctl is-active agentd` + `journalctl -u agentd -n 50` + `ss -tlnp | grep 18080`。

---

## ④ systemd unit + 回滚预置

`/etc/systemd/system/agentd.service`（ Restart=always、journald、专用用户、/var/lib 路径，全齐）：

```ini
[Unit]
Description=agentd (af.local/agentd) 7x24 fleet daemon
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=agentd
Group=agentd
WorkingDirectory=/var/lib/agentd
Environment=AGENTD_ADDR=127.0.0.1:18080
Environment=AGENTD_DB=/var/lib/agentd/agentd.db
ExecStartPre=/bin/sh -c '! ss -tlnn | grep -q ":18080 " || { echo "18080 occupied, abort"; exit 1; }'
ExecStart=/usr/local/bin/agentd
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal
SyslogIdentifier=agentd
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/lib/agentd
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

安装序列（占位符，禁真连）：

```bash
scp /tmp/agentd-linux-amd64 REMOTE_USER@REMOTE_HOST:/tmp/agentd
ssh REMOTE_USER@REMOTE_HOST 'sudo useradd --system --home-dir /var/lib/agentd --shell /usr/sbin/nologin agentd \
  && sudo install -d -o agentd -g agentd -m 0750 /var/lib/agentd \
  && sudo install -m 0755 /tmp/agentd /usr/local/bin/agentd \
  && sudo install -m 0644 /tmp/agentd.service /etc/systemd/system/agentd.service \
  && sudo systemctl daemon-reload && sudo systemctl enable --now agentd \
  && systemctl is-active agentd && ss -tlnp | grep 18080'
```

**回滚预置（安装前写好，一条脚本逆行；含 Go 若上靶的卸载）**：

```bash
ssh REMOTE_USER@REMOTE_HOST 'sudo systemctl disable --now agentd \
  && sudo rm -f /etc/systemd/system/agentd.service \
  && sudo systemctl daemon-reload \
  && sudo rm -f /usr/local/bin/agentd \
  && sudo rm -rf /var/lib/agentd \
  && sudo userdel agentd \
  && echo rollback-done'
# Go 若上靶（仅"目标机自建" fallback 真发生过才执行；本方案不走此路，先记录）：
#   sudo systemctl stop agentd && sudo apt-get purge -y golang-go golang-* ; sudo rm -rf /usr/local/go
#   sudo sed -i "\|/usr/local/go/bin|d" /etc/profile     # PATH 行一并摘除
```

---

## ⑤ nginx 8080/8443 共存处理（新服务端口选择依据）

- **claim（待探）**：admin nginx 已占 8080/8443 对外；db/migrate/rustfs/backend 走内网（端口号本身也待探）。
- **选择：agentd 绑 `127.0.0.1:18080`**（单端口，plain HTTP）。
- **依据**：
  1. 18080 ∉ {8080, 8443}——不碰 admin nginx 已声明占用，且与 rustfs 常用 9000/9001、postgres 5432 等常见内网端口错开；
  2. 仅绑回环：本服务默认只服务本机/内网调用方，不新增公网暴露面；确需对外时再走既有 admin nginx 加 `location` 或独立 server block，**不改动 admin plane 现有配置**；
  3. 碰撞不再是"相信没人用"：unit 里 `ExecStartPre` 拉起前 `ss -tlnn` 预检 18080，占用即拒绝启动（fail fast，不进入 Restart 抖动）；
  4. 端口是单文件单一 `Environment=AGENTD_ADDR` 行，探明 `ss -tlnp` 后若要改，改一行 + `daemon-reload` 即走；
  5. G2 结论：端口清盘命令已列入①的探测清单，**未探明前不承诺最终端口号**，只给默认值与改法。

---

## ⑥ Ledger 行（append-only；本行即 session dispatch notes 的落点）

```
2026-11-01 | debian-remote-agentd（全字段待探） | agentd（af.local/agentd，go1.21.9，modernc.org/sqlite v1.34.1） |
  binary 9,441,280 B measured；目标机工具链 0 B；Go-on-target fallback ~350 MB installed 仅记录不执行 |
  OFFLOAD(build from yin-intel-mac) + INSTALL(service on remote) |
  7x24 宿主 = 服务正确角色；工具链构建期角色错配 → 交叉编译；静态 CGO_ENABLED=0 免 apt 依赖 |
  rollback: systemctl disable --now agentd; rm unit; rm -rf /var/lib/agentd; userdel agentd（+Go 卸载脚本预置） |
  verified-by: local cross-build exit 0 + file(ELF x86-64 static) + go version -m；上靶冒烟挂起（条件：REMOTE_HOST 探测 + systemctl is-active + ss -tlnp | grep 18080）
```

---

## ⑦ 门禁自检（G0–G6）

- G0 卡验真：目标卡新建、逐字段挂探测命令；Mac 卡当日复检（sw_vers/uname -m/go version 与 RED 卡一致）。未接受任何口述为事实。
- G1 可达性：本机无外网 → 靠已温模块缓存完成构建（可证）；目标 egress 列入探测，本方案不依赖。
- G2 共存清盘：目标端口/服务**待探**（命令已给）；碰撞靠 ExecStartPre fail-fast + 端口错开设计。
- G3 三轴：工具链 350 MB 实测口径 + 构建期一次性频率 + 零宿主效用 → OFFLOAD；二进制 9 MB + 每会话高频 + 7x24 高效用 → INSTALL。
- G4 判词：每组件一词带理由（见上表 + ledger）。
- G5 自证或挂起：本地构建冒烟 exit 0；上靶冒烟条件显式挂起。
- G6 方言回滚：systemd（非 launchd）、Debian 用户/路径方言；回滚脚本安装前预置；Mac 侧无新增需扫工件（产物在 /tmp，可 `rm /tmp/agentd-linux-amd64` 收尾）。
