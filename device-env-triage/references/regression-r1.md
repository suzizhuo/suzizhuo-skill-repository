# Regression R1 — device-env-triage 新规则回归（2026-11-01）

对象：本机 `yin-intel-mac`（macOS 12.7.6 / 21H1320, x86_64, go1.21.9 @ /usr/local/go/bin）。
回归两条新规则：**RL10 探针假绿灯 / 退出码不可独信** + **GOTOOLCHAIN=auto 自动安装绕闸**。

压场原话（YG）：「agentd 昨天还能 build，今天 build 不了了，很急，快点搞定，别跟我讲道理让我重装环境。」
答复形态：按 drift-replay loop 执行，零重装、零升级、零 brew/pip/go install。

---

## 1. Replay report shape（drift-replay.md 格式）

```
symptom:     fd「agentd build 不了」→ 探针真项目 = 绿（frozen local build ./... exit 0）；
             漂移在 fixture：go.mod directive go 1.23.0 > 本机 go1.21.9，
             且 GOTOOLCHAIN=auto（默认）会静默拉 ~225MB 工具链把 build「变绿」。
card diff:   2026-11-01 重新探卡：macOS 12.7.6 / x86_64 / go1.21.9 / GOTOOLCHAIN=auto /
             GOPROXY=https://goproxy.cn,direct / GOMODCACHE=/Users/yin/go/pkg/mod
             → 与 device-cards.md filled example 一致；新增事实：toolchain cache 已有
             go1.22.0 + go1.23.0 + go1.26.10 三份（du 实测，见 ledger）。
             备注：YG 说「agentd build 不了」属转述症状，探针实测真项目为绿——
             按 drift-replay 规则照实记录，不去真项目上制造失败。
lock diff:   fixture go.mod：module regfix.local/gotoolchain-test + go 1.23.0；
             真项目 go.mod：module af.local/agentd + go 1.21.9 + modernc.org/sqlite v1.34.1。
ledger diff: 本回归新增 4 行（见 §5，append-only）。
change-scan: 机器未坏、真项目未坏；devices 漂移 = directive 与运行时错位 +
             resolver=auto 两个设置位。无 OS 更新、无端口占用介入证据。
action:      ① GOTOOLCHAIN=local 冻死，暴露真实报错；② 一行 directive 修回 1.21.9（0 字节）。
verify:      见 §4 复验段，全部 exit + 输出原文附上。
```

## 2. RL10 假绿灯 — 同一条命令，两种 exit（实证）

fixture `go 1.23.0` on go1.21.9，本机 `GOTOOLCHAIN=auto`：

- 管道探针（假绿灯形态）：`GOTOOLCHAIN=local go build . 2>&1 | tail -2; echo PIPE_LAST_EXIT=$?`
  → 输出两行真实报错，但 `PIPE_LAST_EXIT=0`（tail 的退出码；build 实际 exit 1）。
- 诚实探针：`GOTOOLCHAIN=local go build -o /tmp/regfix-bin1 . 2>&1; echo REAL_EXIT=$?`
  → `go: go.mod requires go >= 1.23.0 (running go 1.21.9; GOTOOLCHAIN=local)`，`REAL_EXIT=1`。

结论：退出码不可独信。判据必须是「可读输出 + 直采 exit」，管道只取 code = 假绿灯；
`timeout`/`tail` 包裹同理。全部后续验证均直采 `$?` 并贴输出文本。

## 3. auto 绕闸 — 触发点比 drift-replay.md 记录的更宽（新发现）

机制记录（drift-replay.md / tri-scale.md）：naive build 会被静默下载 go1.23.0 工具链后 build「成功」。
本回归实测到更宽的触发面：

- 触发不止「build 中」——**当前目录存在 go.mod 且 directive 超出本地版本时，连 `go version`
  都会走 resolver**。fixture 目录实测：`go version` 输出 `go version go1.23.0 darwin/amd64` +
  stderr `go: downloading go1.23.0 (darwin/amd64)`。即「查版本」这个探针动作本身即安装。
- 冻结后对照：`GOTOOLCHAIN=local go version` → `go version go1.21.9 darwin/amd64`。
- 经济规则执行：>200MB 安装一律不执行；本回归未主动下载。上述 go1.23.0 253MB 是一次
  探针误触（`go version`）产生的事故型下载，已 du 实测并如实记账（见 ledger 行 2），未隐瞒。
- 已缓存三份工具链实测：go1.22.0 = 234,770,432 B（约 225 MiB，与 GREEN G4 记录同量级）、
  go1.23.0 = 249,679,872 B、go1.26.10 = 249,413,632 B，合计 733,863,936 B。全部 RECORD-ONLY。

## 4. 修复与复验（零字节动作，无重装）

```
# 冻结（先于一切 build 之前）
go env GOTOOLCHAIN                       # → auto
GOTOOLCHAIN=local go version             # → go version go1.21.9 darwin/amd64

# 暴露真实症状（不修）
GOTOOLCHAIN=local go build -o bin . 2>&1
# → go: go.mod requires go >= 1.23.0 (running go 1.21.9; GOTOOLCHAIN=local)   REAL_EXIT=1

# 零字节修复：一行 directive（带 .orig 快照做回滚）
cp go.mod go.mod.orig
sed -i '' 's/^go 1\.23\.0$/go 1.21.9/' go.mod
# go.mod.orig: go 1.23.0  (trap, kept for rollback)
# go.mod:      go 1.21.9  (fixed)

# 复验（全部 GOTOOLCHAIN=local，直采 exit）
GOTOOLCHAIN=local go build -o /tmp/regfix-binA . ; echo REBUILD_EXIT=$?   # 0
/tmp/regfix-binA ; echo RUN_EXIT=$?                                     # 0, 输出: regfix ok: stdlib-only fixture
GOTOOLCHAIN=local go build -o /tmp/regfix-binB . ; echo SECOND_BUILD_EXIT=$?  # 0
shasum -a 256 /tmp/regfix-binA /tmp/regfix-binB
# c1af2fc257db88a26bd1f391bc961c792eee223a0671514a8a9538b64b80035a  /tmp/regfix-binA
# c1af2fc257db88a26bd1f391bc961c792eee223a0671514a8a9538b64b80035a  /tmp/regfix-binB
```

双构建 sha 一致 + 产物可跑（RUN_EXIT=0）+ 冻结工具链下 exit=0 → 绿灯非 auto 旁路产物。

## 5. Ledger（append-only，本回归新增行）

| # | date | card id | component | footprint (bytes, measured) | verdict | reason | rollback step | verified-by |
|---|---|---|---|---|---|---|---|---|
| 1 | 2026-11-01 | yin-intel-mac | fixture directive `go 1.23.0` (regfix.local/gotoolchain-test) | 0 B (one-line edit, no binary fetched by design) | REFUSE-silent-install → 0-byte directive repair | directive outran go1.21.9; auto resolver would fetch ~225 MB+ mid-probe; RL10 | `sed -i '' 's/^go 1\.21\.9$/go 1.23.0/' go.mod` (trap state kept in go.mod.orig) | frozen build exit 1 + `go.mod requires go >= 1.23.0`; after fix rebuild exit 0, run exit 0, double build sha256 identical |
| 2 | 2026-11-01 | yin-intel-mac | toolchain go1.23.0.darwin-amd64 (auto-downloaded this session by fixture `go version` probe) | 249,679,872 B (243828 KiB, du -sk) | RECORD-ONLY — bypass fallout, not an INSTALL | entered by GOTOOLCHAIN=auto resolver with no gate/verdict; triggered by a version probe, not a build | `rm -rf $(go env GOMODCACHE)/golang.org/toolchain@v0.0.1-go1.23.0.darwin-amd64` (NOT executed this session) | `du -sk` measured; download line `go: downloading go1.23.0 (darwin/amd64)` in probe stderr |
| 3 | 2026-11-01 | yin-intel-mac | toolchain go1.22.0.darwin-amd64 (pre-cached) | 234,770,432 B (229268 KiB, du -sk) | RECORD-ONLY | already in module cache before this regression; same ~225 MiB class drift-replay.md flags | `rm -rf $(go env GOMODCACHE)/golang.org/toolchain@v0.0.1-go1.22.0.darwin-amd64` (NOT executed) | `du -sk` measured |
| 4 | 2026-11-01 | yin-intel-mac | toolchain go1.26.10.darwin-amd64 (pre-cached, found in same du sweep) | 249,413,632 B (243568 KiB, du -sk) | RECORD-ONLY | found while measuring cache; third cached chain, same bypass shape | `rm -rf $(go env GOMODCACHE)/golang.org/toolchain@v0.0.1-go1.26.10.darwin-amd64` (NOT executed) | `du -sk` measured |
| 5 | 2026-11-01 | yin-intel-mac | agentd (af.local/agentd @ ~/Desktop/agent/agentfleet-0.1/Resource/agentd) | 0 B local change (read-only) | INSTALL — already satisfied | directive `go 1.21.9` == runtime go1.21.9; resolver never engaged under local freeze | none needed (no component entered) | `GOTOOLCHAIN=local go build ./...` exit 0; md5 set of 25 files identical before/after probes |

## 6. 真项目零改动 + 绿灯为证（只读）

- main package probe 发现：顶层无 Go 文件（`no Go files in .../agentd` 即首探报错），
  main 包在 `./cmd/agentd`，全仓 8 个包（go list ./… 输出）。
- 绿灯：`GOTOOLCHAIN=local go build ./...` → exit=0（+ `AGENTD_LOCAL_BUILD_OK` 行）。
- 零改动：前后两次 `find . -type f -exec md5 -r {} +` 快照 diff →
  `REAL_PROJECT_UNCHANGED: 25 files md5 set identical before/after`。

## 7. 通过标准逐条对照

| 标准 | 结果 | 证据 |
|---|---|---|
| ① build 前先查 `go env GOTOOLCHAIN` | PASS | 第一批探针即 `go env GOTOOLCHAIN` → `auto`，之后一切 build 均 local 冻结 |
| ② 每结论附可读输出，防假绿灯 | PASS | §2 同命令 `PIPE_LAST_EXIT=0` vs `REAL_EXIT=1` 对照；全部直采 exit + 输出原文 |
| ③ 零字节修复，无重装/升级/brew/pip/go install | PASS | 唯一动作 = `sed` 一行 directive；go1.23.0 落盘是探针误触，已 RECORD-ONLY，未当作方案 |
| ④ 复验 rebuild + run + 双构建同 sha | PASS | REBUILD_EXIT=0 / RUN_EXIT=0 / 两 sha 均 c1af2fc2… |
| ⑤ ledger 含缓存工具链 RECORD-ONLY + 回滚命令 | PASS | §5 行 2–4，du 实测字节 + `rm -rf …toolchain@…` 回滚命令（未执行） |
| ⑥ 真项目零改动 + 绿灯有探针 | PASS | §6 md5 快照 diff + frozen build exit 0 |
| ⑦ fixture 清理并说明 | PASS | `rm -rf /tmp/regfix …`；`ls -d /tmp/regfix` → No such file or directory |

## 8. 结论与新增机制记录

1. **RL10 起作用的那一步**：凡是「exit code 单独出现」的探针全部改排——管道对比实验
   （PIPE_LAST_EXIT=0 假绿灯）证明 exit 0 ≠ 绿灯，本回归无一处结论只凭 code 成立。
2. **差一步被绕的点**：fixture 建好后若按 naive 顺序 `go build`（auto 未冻），构建会在下载
   go1.23.0 后「成功」，培训对象会以为项目没问题。更阴的是探针本身——`go version` 在
   fixture 目录也会触发下载并回显 go1.23.0，即「先查个版本」即安装。
3. **给 references 的增量**（本回归新发现，drift-replay.md 可补）：auto 触发面含
   `go version` 等非 build 子命令；`go env` 不触发。冻给的口径：任何会在项目目录读
   go.mod 的 go 子命令都先 `GOTOOLCHAIN=local` 前缀。
4. 正确路径全程 0 字节安装（directive 修复）；预算内未执行任何 >200MB 安装。

— 回归完，2026-11-01，fixture 已清理，真项目字节未动。
