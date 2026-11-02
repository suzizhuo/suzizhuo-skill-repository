# evidence-site-families ｜ 站点族 API 版图 + 本机外网实测

为技能 `anything-to-api`（万物皆可 API 装配线）采集的真实机器事实与公开来源证据。
纪律：URL 全部亲自抓取；抓不到的标 `[unsourced]`，不编造。

## Machine egress map ｜ 本机外网实测图

命令（对每个目标逐个执行）：`curl -m 5 -s -o /dev/null -w '%{http_code} %{time_total}' https://HOST/`，失败者补测 `http://`。
图例：`code=000` = 连接/DNS/TLS 失败（rc 非 0）；`rc=28` = -m 超时；`rc=35` = TLS 握手失败；`rc=6` = DNS 解析失败。
实测时间：2026-11-01T12:42:44Z（本机 `date -u`）。

| 目标 | rc | http_code | time_total(s) | 判定 |
|---|---|---|---|---|
| www.bilibili.com | 0 | 200 | 0.160 | 通 |
| api.bilibili.com | 0 | 301 | 0.157 | 通（跳转） |
| www.douyin.com | 0 | 200 | 0.184 | 通 |
| v.douyin.com | 0 | 302 | 2.690 | 通（慢，跳转） |
| www.kuaishou.com | 0 | 200 | 0.375 | 通 |
| www.youtube.com | 28 | 000 | 5.005 | https 不通；http 也不通（000） |
| registry.npmjs.org | 0 | 200 | 1.901 | 通（慢） |
| goproxy.cn | 0 | 200 | 0.121 | 通 |
| www.google.com | 28 | 000 | 5.007 | https 不通；http 也不通（000） |
| cn.bing.com | 0 | 200 | 0.391 | 通 |
| www.baidu.com | 0 | 200 | 0.255 | 通 |

检索通道附带探测（m 8）：hn.algolia.com rc=0 code=200 time=1.201 ｜ api.github.com rc=0 code=200 time=0.261 ｜ github.com rc=28 code=200（8s 边界超时但拿到 200，慢）｜ **raw.githubusercontent.com rc=35 code=000（TLS 失败，换路：改走 api.github.com contents API 取 base64 解码）** ｜ docs.gallery-dl.eu rc=6 code=000（DNS 失败，gallery-dl 文档改走 GitHub 仓库）。

**结论（写进技能路线的真实边界）**：YG 本机「HTTP 层直接挖站」对国内视频/下载类站点（B站/抖音/快手/npm/goproxy/必应/百度）全部可达且延迟低；对 Google/YouTube 全家桶完全不可达（5s 超时，非限速而是出口层拦截）。凡技能流程涉及 YouTube/Google 抓取的步骤，必须走代理或改成本地样本/镜像通道，不能假设直连。

## 站点族事实 ｜ B 站

1. **[已抓取 2026-11-01]** SocialSisterYi/bilibili-API-collect（长期被视为 B 站最权威的半官方接口文档库）**已关停**：README 明示「2026年1月28日，维护者收到 B 站委托律所的律师函警告邮件，指控系统性收集整理非公开 API……即日起停止维护并删除相关文档及源代码」，当前 README 仅剩 Deprecated 声明，**view/wbi 签名文档已随之下线**（本次 grep wbi/签名 0 命中即证据）。
   来源：https://github.com/SocialSisterYi/bilibili-API-collect （经 api.github.com contents API 取 README 原文；raw.githubusercontent 本机 TLS 失败已换路）。
2. **[已抓取 2026-11-01]** 官方开放平台 openhome.bilibili.com 本机实测 HTTP 200（time 1.208s）——「HTTP 层直接挖站」对官方域可达。
3. **[已抓取 2026-11-01]** HN 讨论：GitHub dmca 仓库收录 bilibili 的 DMCA 通知（2019-04-23），160 分/110 评——B 站对接口文档/工具类仓库有长期 DMCA 治理记录。
   来源：https://news.ycombinator.com/item?id=19735463 （对应 https://github.com/github/dmca/blob/master/2019/04/2019-04-23-bilibili.md ）

## 站点族事实 ｜ 抖音 / 快手

1. **[已抓取 2026-11-01]** `Evil0ctal/Douyin_TikTok_Download_API`（21,050★，2026-11-01 仍在 push）：自托管抖音/TikTok/B 站数据采集与**无水印视频下载 API**，支持 MCP 调用、Docker 一键部署——当前抖音族「去水印/签名」链路的事实标准仓库。
   来源：https://github.com/Evil0ctal/Douyin_TikTok_Download_API
2. **[已抓取 2026-11-01]** 快手族开源解析库（均经 GitHub API 实测存在）：`cv-cat/KuaiShou-Spider`（快手逆向/全部 API/直播监听，2026-10-08 仍在 push）、`JoeanAmier/KS-Downloader`（885★，快手作品视频/图片下载，2026-10-26 push）、`Tommy131/KuaishouParser`（快手短视频解析）。
   来源：https://github.com/cv-cat/KuaiShou-Spider ；https://github.com/JoeanAmier/KS-Downloader ；https://github.com/Tommy131/KuaishouParser
3. **[已抓取 2026-11-01]** `JoeanAmver/TikTokDownloader` 仓库经 api.github.com 实测返回 **Not Found**——旧 TikTokDownloader 地址已失效/改名，技能里凡引用该 URL 的步骤需换用 `JoeanAmier` 账号下仓库。
4. **[已抓取 2026-11-01]** HN 对抖音/TikTok 的讨论以政策/隐私新闻为主（CitizenLab 安全分析 70 分、剪贴板 1847 分），**技术向签名/接口讨论极少**——该族签名时效类事实以 GitHub 仓库/issue 为主要可信来源。
   来源：https://news.ycombinator.com/item?id=26565191

## 站点族事实补充 ｜ B 站（交叉验证）

5. **[已抓取 2026-11-01]** `Nemo2011/bilibili-api`（4,165★，另一权威 B 站接口库）**同样关停**：README 声明「对非公开接口、认证体系、访问控制机制、付费内容保护与会员体系进行逆向分析与适配……未获得官方授权……即日起停止维护并永久关停」，并附上海市弘安律师事务所侵权告知函。
   来源：https://github.com/Nemo2011/bilibili-api （api.github.com contents API 取 README 原文）
   → **B 站两大半官方文档库在 2026-01/07 相继被律师函打掉**，「HTTP 层直接挖站」路线在 B 站已无权威文档可依，技能需自带本地镜像/分支存档策略。

## 站点族事实 ｜ 通用模式

1. **[已抓取 2026-11-01]** `putyy/res-downloader`（21,021★，2026-11-01 仍 push）：覆盖视频号、小程序、抖音、快手、小红书、直播流、m3u8、酷狗、QQ音乐的公开下载器——「多站点族统一装配」的现实参考实现。
   来源：https://github.com/putyy/res-downloader
2. **[已抓取 2026-11-01]** m3u8 提取公开模式：HN 帖《Illegal streams, decrypting m3u8's, building a better stream experience》（42 分）与 VLC 自定义 Referrer 播 m3u8 的 gist（58 分）——即 referer/cookie 拼接 + 解密两条公开路径。
   来源：https://news.ycombinator.com/item?id=21687819 （https://blog.jonlu.ca/posts/illegal-streams ）；https://news.ycombinator.com/item?id=40931893 （https://gist.github.com/milesrichardson/4661c311199b98023701f6cecd3b89b0 ）
3. **[已抓取 2026-11-01]** JSON replay 公开模式：`typicode/json-server` README 实证「`npm install json-server` 即可把 JSON 文件当 REST API 用」——静态 JSON 即 API 的标准做法（技能里 JSON replay 环节的官方文档出处）。
   来源：https://github.com/typicode/json-server
4. **[已抓取 2026-11-01]** cookie 注入公开模式：Playwright 官方文档 auth 页（本机实测 220KB UTF-8）明示 storageState 文件含敏感 cookie、登录流注入 cookie 的完整流程。
   来源：https://playwright.dev/docs/auth （浏览器上下文 cookie 注入另见 https://playwright.dev/docs/api/class-browsercontext ）

## 站点族事实 ｜ yt-dlp / gallery-dl 支持站点查询

1. **[已抓取 2026-11-01]** yt-dlp README 实证：`--list-extractors  List all supported extractors and exit`，且支持站点清单维护为仓库内 [supportedsites.md]。仓库 204,379★、2026-10-29 仍 push。
   来源：https://github.com/yt-dlp/yt-dlp （api.github.com contents API 取 README 原文；supportedsites.md 同仓）
2. **[已抓取 2026-11-01]** gallery-dl：README.rst 实测 404，已换路（README.md 重试中）；技能引用需用 GitHub 现路径。仓库：https://github.com/mikfd/gallery-dl

## 翻车模式（每族至少 1 个，全部带实测 URL）

1. **B 站风控 412**：[已抓取 2026-11-01，GitHub issues 搜索 `bilibili 412 in:title` 实测 4 条近期命中]
   - `AzureSonw/SonwMusicBot#6`「fix Bilibili video links returning HTTP 412」（closed，2026-10-25）https://github.com/AzureSonw/SonwMusicBot/pull/6
   - `Bryan-Kuang/FM-Hachimi#163`「fix(bilibili): recover playback from risk-control HTTP 412」（closed，2026-10-14）https://github.com/Bryan-Kuang/FM-Hachimi/pull/163
   - `viuuro/qq-agent-xiaoruan#2`「修复云服务器 IP 抓网页 412」（open，2026-10-21）https://github.com/viuuro/qq-agent-xiaoruan/pull/2
   - `Downvid/downvid#56`「[BiliBili] … Unable to download JSON m…」（closed，2026-10-14）https://github.com/Downvid/downvid/issues/56
   → 教训：机房/云 IP 抓 B 站 412 是常态，技能需带 wbi 签名 + 风控识别 + 重试/换 cookie 槽位。
2. **抖音签名时效/算法变更**：[已抓取 2026-11-01，`douyin signature in:title` 实测命中]
   - `Youhai020616/douyin#16`「detail 端点适配 x-secsdk-web-signature — 修复视频数据为空」（open，2026-10-24）https://github.com/Youhai020616/douyin/pull/16
   - `IvanaGyro/yt-dlp#2`「Generate a_bogus signature to fix web API extraction」（open，2026-07-03）https://github.com/IvanaGyro/yt-dlp/pull/2
   - `go-olive/olive#188`「douyin: fix a_bogus signature and room info retrieval」（open，2026-01-05）https://github.com/go-olive/olive/pull/188
   → 教训：x-secsdk / a_bogus / X-Bogus 家族频繁改版，签名器必须版本化、随浏览器 JS 走。
3. **下载站/站点改版**：[已抓取 2026-11-01，yt-dlp 仓 `changed in:title` 排序实测]
   - `yt-dlp#17466`「Whyp.it changed their urls」（closed，2026-08-18）https://github.com/yt-dlp/yt-dlp/issues/17466
   - `yt-dlp#16684`「Awaan.ae has been changed to Dubaiplus.net」（open，2026-05-10）https://github.com/yt-dlp/yt-dlp/issues/16684
   - `yt-dlp#16519`「[Xiaohongshu] XHS has changed its site URL to Rednote.com」（open，2026-04-19）https://github.com/yt-dlp/yt-dlp/issues/16519
   → 教训：域名迁移/结构改版直接打死解析器，SKILL 需要 URL 形态指纹 + 每周回归探测。
4. **yt-dlp extractor 因站改失效**：[已抓取 2026-11-01，`broken extractor in:title` 实测 total=75]
   - `yt-dlp#16296`「[Boosty] Extractor broken - returns CSV file instead of video」（open，2026-03-21）https://github.com/yt-dlp/yt-dlp/issues/16296
   - `yt-dlp#16063`「[ie/hketv] Replace extractor (broken since 2022) with simple kaltura」（closed，2026-02-25）https://github.com/yt-dlp/yt-dlp/pull/16063
   → 教训：extractor 可静默烂两年，靠 `--list-extractors` 自检 + 抽样回放双保险。label 实测名称为 `core:extractor`（无 `extractor-broken`）。
5. **文档/仓库级关停（B 站特有，最狠）**：SocialSisterYi/bilibili-API-collect 与 Nemo2011/bilibili-api 双双于 2026 年收律师函关停（见上两节）→ 教训：技能不能依赖单一上游文档源，必须本地镜像 + 多仓互备。

## 站点族事实补充 ｜ gallery-dl（修正 owner：mikf，非 mikfd）

1. **[已抓取 2026-11-01]** `mikf/gallery-dl` 实测：20,319★，默认分支 master，2026-10-31 仍 push，GitHub 仓库存活。
   来源：https://github.com/mikf/gallery-dl
2. **[已抓取 2026-11-01]** README.rst 实证站点清单维护于 `docs/supportedsites.md`（HTTP 200，文件头注明 auto-generated by scripts/supportedsites.py，「Consider all listed sites to be potentially NSFW」）。
   来源：https://github.com/mikf/gallery-dl/blob/master/docs/supportedsites.md
3. **[已抓取 2026-11-01]** README.rst 同时给出**按 extractor 注入 cookie 的配置模式**：`"extractor": { ... "cookies" <https://gdl-org.github.io/docs/configuration.html#extractor-cookies> }`，以及「URL 无匹配 extractor 时静默忽略」「可按 extractor 名强制指定」两条行为约定。
4. **[已抓取 2026-11-01]** HN 201 分帖：《Media scraper Gallery-dl is moving to Codeberg after receiving a DMCA notice》——gallery-dl 已宣布因 DMCA 迁往 Codeberg，文档域名亦换为 gdl-org.github.io。
   来源：https://news.ycombinator.com/item?id=47656659 （对应 https://github.com/mikf/gallery-dl/discussions/9304 ）

## 缺口（未抓到或抓不到的，如实标注）

- gallery-dl 的 `--list-extractors` 命令行字样：README.rst grep 0 命中 → **[unsourced]**（仅确认 supportedsites.md 清单文件这一等价物）。
- 抖音/快手**官方**签名文档：不存在公开官方文档，全部为逆向仓/issue 佐证（可信度：GitHub 高星活跃仓 > issue > HN）。
- raw.githubusercontent.com / docs.gallery-dl.eu / playwright.dev 之外：github.com 全站 20s 级慢，HTML 页非必要不抓，全部走 api.github.com contents API。
- web_fetch 工具在本机实测失败（TypeError: fetch failed），本任务检索全部由 bash curl 完成。
- YouTube/Google 系（含 yt-dlp 对 YouTube 的实际抓取能力）本机不可达，未做下载实测。
