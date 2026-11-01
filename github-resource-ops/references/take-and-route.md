# TAKE — Destination Matrix, Manifest, Screening

## 1. Destination Matrix (选归宿，一行给理由)

| Destination | Use when | Constraints |
|---|---|---|
| MAC_LOCAL `~/gh-res/` | 只读代码、小体积源码、本地构建产物 | 老 Intel Mac、慢；**未知可执行文件不落这里执行** |
| MONKEY_CONTAINER | 要跑的东西：扫描器/工具/容器镜像/docker 部署 | 先进容器探 `uname -m` + `cat /etc/os-release`；容器无外网则宿主下载后拷入 |
| GitHub-reference-only | 只需要链接/日后查阅 | 记 manifest，不落地 |

Unknown destination → ask, never guess. One resource, two destinations = two manifest rows.

## 2. Manifest — 无清单不落地

存储树：`~/gh-res/<category>/<owner>__<repo>/`；容器对应路径挂载卷内同构。

```markdown
# manifest.md (每条资源一行，追加式)
M<k> | <date> | <category> | owner/repo | URL | commit SHA / tag | arch | 目的(读/改/跑) | dest (MAC|CONTAINER|ref) | sha256:<asset> | 来源渠道(github|web|archive.org|mirror) | 状态(已筛查/未跑/已跑+证据路径)
```

药到病除的一条：他"下载完就找不到"——清单就是检索入口：`grep <关键词> ~/gh-res/*/*/manifest.md`。

## 3. 复制手法（按目的选）

| 目的 | 手法 | 备注 |
|---|---|---|
| 读源码 | `git clone --depth 1 <url>` | 快；老机器首选 |
| 全量留档 | `git clone --mirror` | 含所有分支/tag；
| release 资产 | `gh release download <tag> --repo <o>/<r> --pattern '*<arch>*'` | 按 `uname -m` 选 darwin_amd64 / linux_amd64 / linux_arm64 |
| 单文件 | `gh api repos/<o>/<r>/contents/<path> --jq .content \| base64 -d` | 抓指定文件不拉库 |
| dead repo 源码 | archive.org wayback → codeload tarball 或 fork 的 tag | 降级时记录渠道 |
| huge repo | `--filter=blob:none` / sparse-checkout | LFS 资产单独处理 |

## 4. Screening Checklist — 运行前，逐项过，全 PASS 才运行

- [ ] **身份**：知名仓库的 fork/typosquat？star 历史 vs 创建时间合理？owner 有无其他可信仓库？
- [ ] **文本清扫**（`grep -rn` attr pattern）：`curl .* | .*bash`、`eval(`、`base64 -d`、`wget`、陌生域名/ip、`chmod +x` 后立刻执行、加密/混淆 blob
- [ ] **二进制清查**：`file <bin>` 类型符合声明？`strings` 扫可疑 URL/IP？release 资产与 repo 内二进制分离正常吗？
- [ ] **架构匹配**：当前 target 的 `uname -m` 输出 vs 资产命名（`*_darwin_amd64` / `*_linux_amd64*` / `*_arm64*`）
- [ ] **校验和**：release checksum 文件存在则逐项 `sha256sum -c`；否则自算入 manifest
- [ ] **隔离**：以上任一项存疑 → 只在容器跑，老 Mac 不执行；容器默认不挂载宿主家目录、网络最小化

## 5. 证据纪律

跑完保存：版本号输出（`<tool> --version`）、一次真实运行日志（目标冒烟即可，路径写进 manifest 证据列）。没有这两样 = UNKNOWN，不许对外说"跑通了"。
