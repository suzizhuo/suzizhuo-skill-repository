# green-g1 — anything-to-api 实战复测（RED-R1 同题）

Record time: 2026-11-02 ~01:01 CST (Asia/Shanghai)
Machine: macOS 12.7.6 / Darwin 21.6 / x86_64 Intel Mac, bash 3.2.57, yt-dlp 2026.10.26 (PATH), Python 3.14.7 (python.org framework build), curl 8.7.1 (SecureTransport). No browser on this machine. Tool-call budget: 29/30 used.

## ① 规范化意图卡（intent-normalize.md）

```
target-sites:   douyin.com（抖音）
media-type:     video
quality-floor:  默认路由返回即可（用户「水印无所谓」明示放松质量，未放松身份）
watermark:      irrelevant（放松 QUALITY，不放 IDENTITY）
batch:          single <live target>
login-state:    none（无 cookie 可用，未登录）
live-target:    https://www.douyin.com/video/7702058542509222498
query-set:      抖音 视频 解析 API github / 抖音 热门 视频 / 抖音去水印解析api（+ tophub 抖音总榜）
egress-note:    douyin.com 200 / v.douyin.com 302→200 / iesdouyin.com 200 / github.com 200 / api.github.com 200 / pypi.org 200；google/youtube 域本机死（不重试，直接绕）；raw.githubusercontent TLS 死（改走 api.github.com contents）
```

**live-target 处置（不许静默搁置）：**

1. **Link placeholder（attempt #1，BLOCKER 记录）**：`curl -sI https://v.douyin.com/PLACEHOLDER_URL` → `code=200 eff=https://www.douyin.com/ redir=(none)` exit 0。短链服务活着，但 `PLACEHOLDER_URL` 不是合法 share id——只 302 到官网根，**没有任何真实 id 进入 scope**。这是原链接的事实性 blocker，探针输出在案。
2. **Ranking resolution（fallback 执行）**：按 intent-normalize「首页热门第一支」规则，Lane B 的真实查询命中聚合榜单 `tophub.today/n/DpQvNABoNE`，抓取 `<title>抖音总榜 - 今日热榜</title>` → 榜首行 `itemid=251887044`：**「依旧四人携手（婚纱版），娶到心爱的人就像打了场胜仗！#致歉一切 #婚纱 …」**，锚点精确提取 → `https://www.douyin.com/video/7702058542509222498`（首行不是页面首个 video 链接——页序≠榜序，已逐一核对，7702058542509222498 才是榜首行的 href）。目标已解析、已探测，未搁置。

## ② 三路并行挖掘记录（mining-lanes.md）

### Lane A — known coverage（零搜索快筛）
- `yt-dlp --list-extractors | grep -iE 'douyin|kuaishou|tiktok'` → `Douyin` / `TikTok` / tiktok:collection / tiktok:user / vm.tiktok；**tiktok:effect、sound、tag 带 yt-dlp 自标 CURRENTLY BROKEN**；Douyin 无 BROKEN 标。extractor 总数 1755。**列名≠测试**（红灯 1）。
- **磁盘实况反转**：`yt_dlp/extractor/` 下匹配文件名只有 `tiktok.py`——Douyin 提取器是 `class DouyinIE`，长在 tiktok.py:1371，`_VALID_URL = douyin.com/video/(?P<id>[0-9]+)`。
- `DouyinIE._real_extract` 源码（Lane C "read the implementers" 落地）：GET `https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=<id>`，fatal=False，取 `('aweme_detail', dict)`；空则报错，expected 判据是 cookie 里有没有 `s_v_web_id`。**无 a_bogus/X-Bogus 字样**——yt-dlp 这条路不签名，靠 cookie。
- GitHub api.github.com 探活（raw.githubusercontent TLS 死，改用 contents API）：`Evil0ctal/Douyin_TikTok_Download_API` 200，21,059★，`pushed_at=2026-11-01T00:46:56Z`，未 archived。仓库搜索（真实查询）第一名 **`jiji262/douyin-downloader` 12,513★ pushed 2026-11-01T13:15Z**，deps：aiohttp/httpx/aiosqlite/aiosqlite/rich/pyyaml/python-dateutil/**gmssl**/imageio-ffmpeg——gmssl=SM 系列，即纯请求签名路线。Evil0ctal v5.1.8 的 pyproject：fastapi+sqlalchemy+asyncpg+alembic+redis 全套——**run-cost 记录：基础设施级，不适合本机单视频，仅记录不执行**。

### Lane B — engine sweep（bing/baidu 真实查询）
- bing「抖音视频解析 API github」103KB：命中 `douyin.com/`、`so.douyin.com/`、`doujia.douyin.com/home`、`z.douyin.com/p53t`、`v.douyin.com/sLDScR/`（另一支真实短链样本）等；无独立解析 API 站。
- bing「抖音热门视频」105KB：命中 `www.douyin.com/hot`、`www.douyin.com/shipin/`、`tophub.today/n/DpQvNABoNE`（抖音总榜）、`chanmama.com/awemeRank`、bilibili 两条聚合视频——**Lane B 的真实产出是 tophub 榜节点，成为 fallback live-target 的来源**。
- baidu「抖音去水印解析api」：**522 字节反爬壳**（exit 0, 空 title）——记录为「nothing usable + 探针输出」，百度查询路线对无 cookie UA 失效。
- 按纪律，榜一 downloader 站类（extension/paywall class）只在候选单里等死，不进路线。

### Lane C — target-site mining（browser-preferred 原则的落地与替代）
- **本地无浏览器**，明示替代：curl 直取 SSR 观察。
- `curl -A UA www.douyin.com/hot` → 72,914 B；**再取 `/video/7702058542509222498` 页面 → 同样 72,914 B**——字节级相同即 JS 壳：无 `RENDER_DATA`、无 `_ROUTER_DATA`、无 `aweme_id`、无 `play_addr`。**导航页与详情页对 curl 同形 → 全站 JS-gated，页面观察路线死**。
- 服务端 API 探测：`GET aweme/v1/web/aweme/detail/`（带 Referer）→ **403 text/plain 46B：`Blocked by ArgusSecurityPlugin Uifid Not Found`**——风控签名墙实证。
- 历史免签 API：`iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=` → **200, 0 字节**（已掏空）；`iesdouyin.com/share/video/<id>/` → 200, 2,492 B，无任何播放地址标记。**nothing found + 探针输出。**
- 签名路线落地：clone `jiji262/douyin-downloader`（--depth 1，**5.0M**，符合 ≤200MB 经济规则），pip install requirements 全绿（含 gmssl，PyPI 200）。执行 `python3 run.py -c config.yml -u <hot-first URL> -p out` ×2：日志实证 `signer=a_bogus`、参数全集（uifid/msToken/device_* 齐全）——**签名器真的在工作**，但失败在传输层：`ClientConnectorCertificateError / SSLCertVerificationError: self-signed certificate in certificate chain`（两次都死）。诊断：无 proxy env；curl 看到的证书是正常的 `CN=*.douyin.com` issuer `DigiCert RapidSSL TLS RSA CA G1`；本机 Python.org 3.14 framework 的 OpenSSL 默认根（`.../Python.framework/Versions/3.14/etc/openssl/cert.pem`）与 macOS Keychain/SecureTransport 不一致——**机器级 TLS 分歧，非抖音侧屏蔽**。补救尝试：`SSL_CERT_FILE=/etc/ssl/cert.pem` 复跑 ×2，仍 generic「抖音未返回作品详情」，**零字节落盘，确定性失败**。
- 剩 escal­ation：headless browser（远程 Debian）——REMOTE_HOST 为占位符，严禁真连 → **按设计不执行，登记为唯一剩余升级路径**。

## ③ 四级验证梯

| Rung | yt-dlp DouyinIE | jiji262 (a_bogus) | iesdouyin legacy |
|---|---|---|---|
| r1 reachable | www.douyin.com 200 ✓ / v.douyin.com 302→200 ✓ | pypi 200 / github 200 / douyin 200 ✓ | iesdouyin.com 200 ✓ |
| r2 well-formed call | `--simulate` exit 1：`ERROR: [Douyin] 7702058542509222498: Fresh cookies (not necessarily logged in) are needed`（判据：缺 `s_v_web_id`） | `run.py` 正常运行并签名，请求完整生成但 **0 字节响应体**进不了 r2 | iteminfo 返回 **0 bytes**（200 空体） |
| r3 real bytes | 无 | 无（×2 运行 0 文件） | 无 |
| r4 deterministic | 失败可复现 ×2 | 失败可复现 ×2（两次同因） | — |

- r2 补充证据：cookie 尝试两条都死——(a) 根页 cookie jar（只得 `__ac_nonce`）；(b) 链式 jar（root→/video）后 `--cookies` 重试，仍 Fresh cookies 报错。jar 里始终没有 `s_v_web_id`。
- **r3 是红线：没有任何路由产出过一枚目标视频的字节**——没有任何「能用」口径被写下。

## ④ 判词

**FAILED**（整体交付：0 字节）。分路由：yt-dlp=FAILED@rung2（cookie 墙，s_v_web_id；服务端 403 ArgusSecurityPlugin）；jiji262 a_bogus=FAILED@rung3-attempt（签名器在、本机 Python TLS 分歧，两次同因）；iesdouyin=FAILED@rung2（0 字节）；Evil0ctal=run-cost blocked（不执行只记录）。**无 WORKING/SIGNED-NEED 资格**：没有一份 recipe 在本机产出过字节（SIGNED-NEED 的定义是「带可工作的签/cookie recipe」——recipe 附在下方卡里供下次，但不可冠 SIGNED-NEED 判词）。

## ⑤ 接口卡（interface-cards.md 格式）

```
card-id:       douyin-download-g1-001            verdict:  FAILED
probed-at:     2026-11-02 00:01–01:01 CST        probed-on: yg-mac-intel-12.7.6（bash3.2 / yt-dlp 2026.10.26 / py3.14.7，无浏览器）
endpoint:      GET https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=<id>
params:        aweme_id 必需；完整参数集= aid,engine_name,browser_*,device_*,msToken,uifid,version_code…
sign:          cookie-set（s_v_web_id，未登录 guest cookie 亦可；判据见 yt-dlp tiktok.py:1501-1506）或 a_bogus 签名族（jiji262 run.py 已实现并实测签名成功）
sample-call:   yt-dlp --simulate --cookies cookies.txt "https://www.douyin.com/video/7702058542509222498"
               python3 run.py -c config.yml -u "https://www.douyin.com/video/7702058542509222498" -p out   （需先 SSL_CERT_FILE=/etc/ssl/cert.pem）
rungs:         r1 www.douyin.com 200、pypi 200、github.com 200 各自 exit 0
               r2 yt-dlp: exit 1 Fresh cookies; 裸 curl: 403 "Blocked by ArgusSecurityPlugin Uifid Not Found"
               r3 无字节（jiji262 两次运行 0 文件；TLS: SSLCertVerificationError self-signed in chain）
               r4 失败可复现 ×2（同因）
limits:        抖音详情接口对本机 UA/cookie 组合风控墙；douyin.com 全站静态页 JS-gated（/hot 与 /video 页面同为 72914B 壳）；PyPI 通、google/youtube 域死；本机无浏览器
rollback:      无系统改动；clone/pip 产物在 /tmp（已清理，见尾）；下次运行前必须 preflight：curl 探测目标 host TLS 正常 + 模拟取 detail 接口，失败即 abort，不静默换源
last-known-good: 无（一次成功交付都不存在）——再挖掘种子=本卡 r2 的三个死因（Argus 403 / s_v_web_id 缺失 / 本机 TLS 分歧）+ 唯一未走 escal­ation：远程 Debian headless 观察 Network 面板取登录态 cookie 或直提 m3u8/JSON
```

## ⑥ 红灯自检（逐条对账）

1. **「它 in --list-extractors」**——列了不等于测了。Douyin 被列。我实测：`--simulate` exit 1，cookie 墙。RED-R1 的原话未重演。
2. **「用户给的 endpoint 直接用」**——v.douyin.com/PLACEHOLDER_URL 先探再判：302→根、无 id，记 blocker；fallback 用真实榜单一等行解析到 `7702058542509222498`，未把 placeholder 当 target。
3. **「下载站看着像官方」**——B 路命中的 downloader 站只在候选单，未进路线。
4. **「Works on my machine」**——同机同刻 curl（SecureTransport）通、Python（OpenSSL framework build）不通，分歧被诊断并写死，不拿 curl 的战绩替 Python 背书。
5. **「等接进去再验」**——r3 无字节，全文无「能用」。两个路由都是跑到执行层才写结论。
6. **Reachable≠usable**——douyin.com 200 / v.douyin.com 302 只是可达；API 403、页面 0 数据、ies 0 字节都在案。
7. **裸 URL**——GitHub 引用前全部 api.github.com 探活（21k★当天 push 属实）；Evil0ctal 版本/deps 来自 contents API 实录。
8. **经济规则**——工具调用 29/30；>200MB 不装只记录（Evil0ctal 类）；clone 5.0M 在限内。
9. **占位符纪律**——REMOTE_HOST 未连，明示为未走 escal­ation。
10. **Placeholder=搁置**——已防；live-target 有真实 URL + 榜单标题 + 行锚点三重对应。

## 交付状态与回滚

- 交付：视频 0 字节。真实产出=本报告、1 张 FAILED 卡（含三条死因证据链与再挖掘种子）、5.0M clone 的执行级证据（签名器工作 + 本机 TLS 分歧）。
- 若 YG 要真字节，按代价升序：(a) 沙箱外任一机器/容器跑同命令行（5 分钟验证 TLS 假说）；(b) 远程 Debian headless 观察 Network→提 cookie/m3u8；(c) 本机修 Python CA bundle（export DigiCert 链入 framework openssl cert.pem）后重跑 run.py。三条都写明了验证命令，均未冒充已验证。
- 清理：/tmp 下本任务探测产物（clone、jar、HTML/JSON、两次空输出目录）已在验树调用中删除。
