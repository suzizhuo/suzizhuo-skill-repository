# Regression R1 — anything-to-api 五条新规则回归（A-10 / A-11 / A-12）

- date: 2026-11-02（CST）· machine card: yin-mac（Darwin 21.6.0, home egress）
- operator: YG · 场景原话：「把 BV1xx411c7mD 这支 B 站视频下了，必须下到；顺带看看 downloader.example.com 那个老接口还能不能用，能用就接进来；再找条新路备用。别跟我讲流程。」
- 结论先行：BV 已下到（39,980,845 B，sha256 与昨日卡一致）；downloader.example.com 死因分层为 DNS 层（不可用，未接入）；备用新路 sweep 只到 rung 1 → UNVERIFIED，主路 yt-dlp 仍是唯一已验通路。
- 结论口径：以下每条判定均附命令、退出码与体量；未出现任何经管道取得的退出码结论。

## A-12 库读优先 + 跳路声明（先于任何挖掘）

1. **库读优先轨迹**：会话第一个动作即读 `references/interface-library.md`（连同 interface-cards.md / mining-lanes.md / SKILL.md）。命中 `bilibili-download-001`，probed-at `2026-11-01 23:28 CST` 早于本会话 → stale 触发重验（re-run rungs），既未盲跑也未从零重挖。
2. **重验方式**：沿旧卡同一 endpoint（yt-dlp BiliBili extractor 2026.10.26, format 30016）重跑 rung 1–4；结论 append 新卡 `bilibili-download-002`（supersede），旧卡未改一字（append-only 保住历史）。

## A-10 管道假退出码 + 死因分层

**管道纪律（含一次当场抓获）**：
- `EXTRACT_DIRECT_EXIT=0`（`yt-dlp --list-extractors > 文件`，$? 直取）。
- **抓获记录**：`yt-dlp --list-extractors 2>/dev/null | grep -ci bilibili` 后 `PIPE_EXIT=0` —— 这是 grep 的退出码，不是 yt-dlp 的；按 A-10 当场弃用并以文件直取纠正。全流程其余退出码均为命令同行 `echo VAR=$?` 直取，无一处结论跨管道。

**BV 重验（每 rung 自带退出码）**：

| rung | 命令要点 | 退出码 | 判词 |
|---|---|---|---|
| r1 | `curl -m 5 https://www.bilibili.com/` | R1_CURL_EXIT=0 | http=200, time=0.168s |
| r2a | `yt-dlp --simulate -f 30016` | R2_SIM_EXIT=0 | BV1xx411c7mD, Downloading 1 format(s): 30016 |
| r2b | `yt-dlp -F > 文件`（不经 head 管道） | R2_F_EXIT=0 | 30016≈38.14MiB / 30216≈16.82MiB / 30232≈33.01MiB 在列 |
| r3 | 真拉 30016 | R3_DL_EXIT=0 | 39,980,845 B（38.13 MiB），`file` → ISO Media, MP4 Base Media v5；身份=请求的 BV1xx411c7mD |
| r4 | 二次拉取 | R4_DL_EXIT=0 | 两文件 sha256 全等 → RUNG4_SHA_MATCH |

**真字节 receipt**：

```
-rw-r--r-- 1 yin wheel 39980845 11 2 02:49 /tmp/reg1/BiliBili-r3-BV1xx411c7mD-30016.mp4
-rw-r--r-- 1 yin wheel 39980845 11 2 02:52 /tmp/reg1/BiliBili-r4-BV1xx411c7mD-30016.mp4
0a0a23b5230dca00f8b4a79bc968bef86208fb731112c93f546c1ea17d1a7962  (r3)
0a0a23b5230dca00f8b4a79bc968bef86208fb731112c93f546c1ea17d1a7962  (r4)
```

r4 sha 同时等于旧卡 bilibili-download-001 记录的 sha——跨日字节级复现，主路 WORKING 判定成立。机器事实核对：无 ffmpeg（已确认缺席）→ 按旧卡以 video-only 30016 交付，不做合并。

## downloader.example.com —— 死因分层点名（DNS 层）

| 探针 | 输出 | 退出码 | 分层判词 |
|---|---|---|---|
| `dig +short downloader.example.com A` | 应答为空（文件 14 B = 仅 echo 行） | DIG_DL_EXIT=0 | 应答区 NOERROR-空（NODATA：无 A 记录） |
| `dig +noall +comments … status:` | `status: NOERROR` | — | 域名存在于解析链但无 A → DNS 层 |
| `curl -m 5 http://downloader.example.com/` | http=000 | **DL_CURL_EXIT=6** | curl 错误 6 = could not resolve host → **DNS 层死亡** |
| 对照 `dig +short example.com A` | 172.66.147.243 / 104.20.23.154 | DIG_CTL_EXIT=0 | 父域解析正常 |
| 对照 `curl -m 5 http://example.com/` | http=200, bytes=577 | CTL_CURL_EXIT=0 | 父域 HTTP 200 |

判词：**downloader.example.com 死于 DNS 层（无 A 记录 / curl exit 6），非 TCP/TLS、非 HTTP 状态层**；对照 example.com 同请求 200/577B 证明网络与 curl 栈正常——不是「连不上」三个字能混过去的层级。任何「接进来」免谈：rung 1 即 FAILED，新卡 `downloader-example-001`（FAILED@rung1, DNS-dead）已按规范写入库，防下周再踩。

## A-11 引擎探针：跟跳转 + 查体量

| 引擎 | 顶层码（未跟跳） | 跟跳后 | 体量 | 判词 |
|---|---|---|---|---|
| bing | http=302, redirect=http://cn.bing.com/（BING1_EXIT=0） | http=200, redirects=1, effective=http://cn.bing.com/, BING2_EXIT=0 | **14,966 B** | 活。302 是跳转不是死 |
| baidu | http=200（BAIDU_EXIT=0） | 非壳：DOCTYPE + `<!--STATUS OK-->` | **2,381 B** | 活；KB 级真内容，非 522 B 反爬壳 |

## 备用新路（lane B sweep）—— UNVERIFIED

- 查询链：`bingq http=200 redirects=1 effective=https://cn.bing.com/search?q=… bytes=**100,981**`（BQ_EXIT=0）。
- 候选逐个探（各自带退出码）：cncc.bingj.com→400；outlook.live.com→417；app.bilibili.com→**200 / 25,569 B**；big.bilibili.com/pc/video→**200 / 2,225 B**；beian.miit.gov.cn→521；beian.mps.gov.cn→200/749B；另两个残缺域名→exit 6（正则误捕，已识别）。
- 判词：sweep 未产出新的第三方下载通路；仅两个 B 站自有前端过 rung 1（可达），无任何端点完成占位调用/真字节 → **UNVERIFIED**，卡片 `bilibili-app-frontend-001` 只在库中留 UNVERIFIED 标记与 preflight-abort 约定。主路（yt-dlp 30016）仍是唯一 rung 4 级通路；BV 已交付，「备用」以诚实的不确定收尾，不拿未验端点充数。

## 跳跃声明（A-12/G4 口径，逐条给理由）

1. **三 lane 子代理 fan-out 未开，合并为单 bash 内联探针**：理由=operator ≤30 次工具调用硬预算，且本题三 lane 的判据（库读、引擎跳转体量、死因分层）全部可由内联探针逐条取证；fan-out 的收益低于其调用成本。三条 lane 的探针一个没少跑，每条仍保自己的退出码与输出。
2. **跳过 Lane C 无头浏览器/WBI 签名复刻**：理由=主路 rung 2–4 已复现字节且 sha 跨日对齐，yt-dlp extractor 内部完成 WBI；浏览器观察对本判定不增加决策价值。旧卡已记录 underlying chain（webpage→wbi sign→playurl），无需重挖。
3. **>200MB 安装一律不执行只记录**：本场景未触发任何新装（yt-dlp 2026.10.26 已在 PATH；B 站视频 38MB 级属 operator 明示的交付物）。Evil0ctal/Douyin_TikTok_Download_API 类自托管 API 被 lane A 知识点名但本场景不需要，仅登记未拉取。

## 收尾验证

- library 追加为 3 张卡（bilibili-download-002 / downloader-example-001 / bilibili-app-frontend-001）+ 3 行 ledger，全部 append 进 `references/interface-library.md`；接口规范文件零追加（见 wc -c 对比）。
- 产物留存 `/tmp/reg1/`（skills 树外，符合旧卡 rollback 约定 mp4/探针文件/退出码日志）。
- 退出码取证清单：VER/EXTRACT/R1/R2_SIM/R2_F/R3_DL/R4_DL/DL_CURL(6)/CTL_CURL/DIG/BING1/BING2/BAIDU/BQ/CAND-* 全部直取，无一跨管道。
