# evidence-mining-practice ｜ 下载器生态与接口挖掘实践（研究笔记）

> 生成日期 2026-11-01。检索通道：HN Algolia API + GitHub API（web_fetch 亲自抓取）。
> 每条事实后附来源 URL，均实际抓取过；未抓到的标 [unsourced]。

## 一、黄金案例（真实"找/挖接口下载"实践）

### G1. yt-dlp extractor 机制（谁做的：yt-dlp 社区，源自 youtube-dl fork）
- Show HN 2021-08-26，822 分：yt-dlp = youtube-dl fork 带额外功能与修复。
  来源：https://github.com/yt-dlp/yt-dlp （HN story 28319624，已抓）
- issue #14404《Upcoming new requirements for YouTube downloads》1244 分、611 评论（2025-09-24）。
  来源：https://github.com/yt-dlp/yt-dlp/issues/14404 （HN story 45358980，已抓）
- issue #15012《External JavaScript runtime now required for full YouTube support》1106 分、
  627 评论（2025-11-12）——YouTube 反爬升级到"必须跑 JS 才能拿签名"。
  来源：https://github.com/yt-dlp/yt-dlp/issues/15012 （HN story 45898407，已抓）
- Yt-Dlp Sequence Diagrams：把一次下载的接口调用流程画成时序图，挖掘行为可读化。
  来源：https://app.ilograph.com/demo.ilograph.yt-dlp/Download%2520a%2520YouTube%2520Video （HN story 48880384，已抓）
- 挖掘模式结论：extractor = 按站点封装的 Python 类，_real_extract() 里先抓页面/JS player，
  再解 signature/n-challenge，最后拿 formats——"从页面挖接口"是固定套路，代码即模板。

### G2. gallery-dl（谁做的：mikf）
- Show HN 2020-11-29，407 分：图站批量下载器，支持站点配置驱动。
  来源：https://github.com/mikf/gallery-dl （HN story 25244589，已抓）
- 风险实证：2026-04 因 DMCA 通知整体迁往 Codeberg，HN 201 分；评论直指"DMCA trolls for hire"，
  说明开源下载器本体会收到版权恐吓而非下架单个链接。
  来源：https://github.com/mikf/gallery-dl/discussions/9304 （HN story 47656659，已抓）

### G3. cobalt.tools 及同层生态（谁做的：imputnet 等；HN 侧多处口碑）
- HN 用户原话：cobalt.tools "their api was also pretty hackable too actually"——即它本身就提供
  可直接 POST 的下载 API，是"万物皆可 API"的正面样板。
  来源：https://cobalt.tools （HN comment 45406675，已抓）
- cobalt.directory：自建实例目录，说明该类服务用户要的是"无广告/无登录"下载端点。
  来源：https://cobalt.directory （HN comment 49989827，已抓）
- 同层：Omniget（作者 tonhowtf，Show HN 2026-02-18，Tauri+Rust/Svelte，灵感来自 cobalt.tools）
  公开了自己的挖掘细节：Udemy 免密登录→WebView 注入 JS 填邮箱→收 6 位验证码→取 cookie
  （Windows 用 WebView2 COM cookie API，Linux/macOS 用 JS 重定向把 document.cookie 带出的 hack）；
  自带 HLS 下载器（m3u8 解析、并行 TS 分片下载、AES-128-CBC 解密），非 DRM 内容不依赖 ffmpeg。
  来源：https://github.com/tonhowtf/omniget （HN story 47063034，已抓）
- 同层：Media-Downloader GUI（mhogomchungu）统一封装 yt-dlp / youtube-dl / gallery-dl / lux /
  you-get / svtplay-dl / aria2c / wget，是"CLI 下载器统一入口"的现成形态。
  来源：https://github.com/mhogomchungu/media-downloader （HN story 32310273，已抓）

### G4. you-get（谁做的：soimort / Mort Yao）
- GitHub 元数据（已抓 repo API）：description "Dumb downloader that scrapes the web"，
  Python，56,862 stars / 9,660 forks，默认分支 develop，官网 https://you-get.org/ ，
  建于 2012-08-20，2026-10 仍在推代码。
  来源：https://github.com/soimort/you-get

## 二、真实 extractor 源码解剖（"挖掘模式"的代码级模板）

### bilibili.py —— B 站 WBI 签名全流程（yt-dlp，已抓全文）
文件：yt_dlp/extractor/bilibili.py，113,650 bytes，sha ca37b79deea868e0fb03698de6f1b28caaca3573
来源：https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/bilibili.py

从这段源码可直接读出的"挖接口→签名→调用"套路：
1. 拿签名钥匙：GET https://api.bilibili.com/x/web-interface/nav ，从返回的
   wbi_img / img_url / sub_url 里切片取值拼接成 lookup，再按一张固定的 64 位置换表
   （mixin_key_enc_tab，注释写 "from getMixinKey() in the vendor js"）重排，取前 32 位
   作为 wbi key，缓存 30 秒一个 session。
2. 算签名：_sign_wbi() 里 params['wts'] = round(time.time())，按键名排序、过滤掉 "!'()*"
   字符，urlencode 成 query，最后 params['w_rid'] = md5(query + wbi_key).hexdigest()。
3. 带签名调用：playinfo 走 https://api.bilibili.com/x/player/wbi/playurl ，
   空间搜索走 https://api.bilibili.com/x/space/wbi/arc/search ，
   都自带 _sign_wbi 结果；401/-352 直接报错提示"等下再试"。
4. 反爬对抗写在明处：code == -401 → "you have to become a premium member to download them"；
   HTTP 412 / code -352 → "Request is blocked by server (412), please wait and try later"；
   即风控拒绝码就是接口响应本身，不需要外部资料。
5. 设备指纹参数：_dm_params() 里 dm_img_list / dm_img_str / dm_img_inter 三个字段，
   注释标明逆向来源是 https://s1.hdslb.com/bfs/seed/jinkerl/short/user-fingerprint/
   bili-user-fingerprint.mini.js 的 window.__biliUserFp__.queryUserLog，
   而 dm_img_list / dm_img_inter 因来自鼠标事件"更troublesome"被置空；
   wh/of 用屏幕分辨率+滚动位置+随机数构造——直接从站点自己的前端 JS 反推接口参数。
6. 会员/登录墙：_check_missing_formats 里缺格式时提示 become a premium member；
   BilibiliFavoritesListIE 遇到 -403 私有收藏夹直接 raise_login_required；
   WatchlaterIE 测试样本 skip 'login required'。
   → 即"登录墙"在 extractor 层的标准处理：探测 code 分支 → 抛 LoginRequiredError。

### cobalt.tools 仓库级事实（GitHub repo API，已抓）
- imputnet/cobalt：描述 "best way to save what you love"，AGPL-3.0，45,694 star / 4,028 fork，
  284 open issues，Svelte 写的前端，创建于 2022-07-08，默认分支 main，topics 含
  downloader / youtube-downloader / tiktok / instagram / social-media。
  来源：https://github.com/imputnet/cobalt
- 形态结论：cobalt = 一个 WebApp + 开放 API 端点（HN 用户证实"api was also pretty hackable"），
  即"把下载能力做成接口"这条路的工业级样板；2026 年仍在更新（pushed_at 2026-04-06）。

### gallery-dl 设计事实（contents API，已抓，默认分支 master）
- docs/ 目录构成：configuration.rst（245,137 bytes）、supportedsites.md（66,251 bytes，= 站点
  接口清单）、gallery-dl.conf（31,532 bytes）、gallery-dl-example.conf（14,727 bytes）、
  options.md、formatting.md。
  来源：https://github.com/mikf/gallery-dl/tree/master/docs
- 形态结论：gallery-dl 把"每个站点的提取规则"做成了配置文档 + extractor 类的双层结构，
  supportedsites.md 本质是一份维护中的"接口可用性目录"——可直接抄来做技能里的站点清单。

## 三、签名 / 防抓机制公开出处

### S1. B 站 WBI 签名（两个出处）
- 出处 A（代码级）：yt-dlp yt_dlp/extractor/bilibili.py，见第二节全文剖析
  （_get_wbi_key / _sign_wbi / mixin_key_enc_tab / md5(query+wts+key)）。
  来源：https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/bilibili.py
- 出处 B（文档级，尝试中）：SocialSisterYi/bilibili-API-collect 的 WBI 签名文档；
  注意：?ref=master 抓取返回 404 "No commit found for the ref master"，已换 ref=main 重试，
  若仍失败则该仓标记 [unsourced]。
  来源：https://github.com/SocialSisterYi/bilibili-API-collect

### S2. 抖音 a_bogus / X-Bogus 签名（GitHub repositories search，已抓，total_count=26）
- luochaolun/abogus（JavaScript，8 star）：来源 https://github.com/luochaolun/abogus
- amxsa/douyin_abogus_python："基于纯Python的抖音abogus的源码"
  来源 https://github.com/amxsa/douyin_abogus_python
- Superheroff/TikTokApi_aBogus_Test："aBogus_1.0.1.1~aBogus_1.0.1.20可用性测试"——
  仓库名本身说明签名算法小版本漂移（1.0.1.1→1.0.1.20）要靠脚本批量试。
  来源 https://github.com/Superheroff/TikTokApi_aBogus_Test
- jackluson/a_bogus_douyin："抖音a_bogus参数"，topics=[abogus, douyin]，
  homepage https://abogus.jackluson.workers.dev/ 来源 https://github.com/jackluson/a_bogus_douyin
- hanzheng1954/douyin-abogus-analysis：2026-08 新建，"抖音 web a_bogus 签名算法完整逆向分析：
  动态 trace、VM 反汇编、SM3 管线还原、Node/Python 签名器与 4K 解析工具"——
  说明了"挖签名"的标准动作链：动态 trace → VM 反汇编 → 还原哈希管线（SM3）→ 重实现。
  来源 https://github.com/hanzheng1954/douyin-abogus-analysis
- search 入口（可复现）：https://api.github.com/search/repositories?q=abogus

## 四、翻车模式（每个配已抓取的真实出处）

### F1. 下载器站失效圈套：搜索排前面的"下载站"根本不能下载
- HN 2023-12-31，gkbrk：SEO 排前面的两个链接不是 YouTube 下载器，只链到别的下载器，
  页面连输入 YouTube URL 的地方都没有；About us 是教会文案——纯 SEO 垃圾页。
  来源：https://danluu.com/seo-spam/ （HN story 38821248 / comment 38823631，已抓）

### F2. 假接口圈钱：钓鱼式"下载器"逼装扩展，装完仍是广告/诈骗
- 同帖 shaldengeki 描述：某 youtube downloader 站自称经 "Norton SafeWeb" 检查，
  一交互就弹全屏安装浏览器扩展 "CyberShield"；Google 知识卡显示 CyberShield 声称是
  popup blocker 实为 badware（开广告标签、推假更新和技术支持诈骗）；不装扩展就下不到视频。
  来源：https://danluu.com/seo-spam/ （HN comment 38822137，已抓）
- hamasho：搜 "youtube" 首位不是官网而是 scammy 站点，"terrible and unethical job by Google"。
  来源：https://danluu.com/seo-spam/ （HN comment 38825084，已抓）

### F3. 圈钱订阅/假 App：应用商店付费下载器纯属骗钱
- HN（2015-12-22）MichaelGG：Windows Store 里 "FreeFlix Free Movies"、"Free Mp3 Downloader"
  是随机垃圾；搜 "Game of Thrones" 首位是 $5 paid app 且连网站都没有；
  微软曾按 $100-200/app 补贴 shovelware，等于花钱制造垃圾生态。
  来源：http://www.theverge.com/2015/12/18/10571504/microsoft-lumia-950-xl-review
  （HN story 10772797，已抓）

### F4. API 改版不告：站点静默改版/关停 API，工具方只能不停追
- GitHub search issues（q=repo:yt-dlp/yt-dlp broken in:title）total_count=597——
  "broken"类 issue/PR 是下载器生态的常规工作量，不是异常。
  检索入口：https://api.github.com/search/issues?q=repo:yt-dlp/yt-dlp+broken+in:title
- 实例1 GoogleDrive：PR #10501 "Almost rewritten GoogleDriveFolderIE: the API doesn't seem
  to be working anymore"——API 直接失效，重写为网页抓取。
  来源：https://github.com/yt-dlp/yt-dlp/pull/10501
- 实例2 RadioFrance：PR #9933 "Because the JS api is no longer accessible, the extractor now
  scrapes through the HTML on the webpage"——API 关停，回退 HTML 解析。
  来源：https://github.com/yt-dlp/yt-dlp/pull/9933
- 实例3 tvplay：PR #10398 "Fixed TVPlay extractor as it was broken likely due to site being
  updated"——站点前端改版即断。
  来源：https://github.com/yt-dlp/yt-dlp/pull/10398
- 实例4 CeskaTelevize：issue #6539 "Looks like something has been changed in this web today"
  → HTTP 404，2023-03 开到 2026-10 仍未关（168 评论），单站点维护可拖三年。
  来源：https://github.com/yt-dlp/yt-dlp/issues/6539

### F5. 风控封禁：验证码/指纹/TLS 检测，工具方被 403/412 反制
- B 站 extractor 实证：HTTP 412 / code -352 "Request is blocked by server, please wait and try
  later"；-401 会员墙（见第二节 bilibili.py 剖析）。
  来源：https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/bilibili.py
- Mediaite：issue #17247，用户原话 "It has worked 99.9% of the time for years until yesterday.
  Now it always fails. Same result via proxies."，报错 HTTP 403；该 issue 被打了
  impersonation 标签（描述："Browser impersonation related. HTTP/TLS fingerprinting,
  curl_cffi, etc."）——说明 2026 年风控主线已经变成浏览器/TLS 指纹伪装。
  来源：https://github.com/yt-dlp/yt-dlp/issues/17247
- 账号级连坐：HN 用户 schoolornot——Roku 以"装过违反 ToS 的 M3U player"为由永久封禁
  其侧载权限，装播放器本身成了封号理由。
  来源：https://blog.roku.com/developer/2021/10/26/idk （HN comment 29047678，已抓）

### F6. 登录墙：免费内容强制登录换取
- bilibili.py 实证：BilibiliFavoritesListIE 遇私有收藏夹 code -403 直接 raise_login_required；
  Watchlater / danmaku 字幕都要登录 cookie 才给（_TESTS skip 'login required'）。
  来源：https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/bilibili.py
- Omniget 实证：Udemy 改免密登录后，下载课程必须 WebView 走真实登录流程再偷 cookie
  （见 G3），即"登录墙"把下载问题转成了登录态维持问题。
  来源：https://github.com/tonhowtf/omniget

## 五、m3u8 / HLS 提取机制公开出处
- Omniget Show HN 自述：自带 HLS 下载器（m3u8 解析、并行 TS 分片、AES-128-CBC 解密），
  非 DRAM 内容不依赖 ffmpeg。来源：https://github.com/tonhowtf/omniget （HN story 47063034）
- HN 2014：HLS 本质 = "a m3u playlist of mp4 files that get downloaded in succession"
  （mitchty），即索引文件+分片序列，是可脚本化的接口。
  来源：http://www.apple.com/live/2014-sept-event/ （HN comment 8291902，已抓）

### F7. 水印二次收费/去水印能力被包装成付费点
- 免费侧证据：TikTokDownloader 类工具长期把"无水印下载"当基础功能；抓 JoeanAmery/TikTokDownloader
  仓库 API 返回 404（该路径当前不可用，标 [unsourced]，改用 search 入口另找）。
  检索入口：https://api.github.com/search/repositories?q=tiktok+downloader+watermark

## 六、devtools Network → replay 模式（公开出处）
- yt-dlp issue #9106（GDCVault，一手玩家原话）："The following video ... can be played without
  an account, but when trying to download it, GDCVault requires login information... especially
  since you can find the base m3u8 stream by looking at network requests and download the talk
  directly!"——即：浏览器能放=接口就在 Network 面板里，抓到 m3u8 直接下；登录墙只挡下载器不挡观察。
  来源：https://github.com/yt-dlp/yt-dlp/issues/9106
- 同 issue 附带工程细节：_parse_html5_media_entries 抓空导致 IndexError，说明"登录墙"在
  extractor 层的表现是解析函数返回空列表。
- 时序图资料：Yt-Dlp Sequence Diagrams（ilograph）把"下载一个 YouTube 视频"的接口调用顺序
  画出来，等价于人工 replay 的脚本化预演。
  来源：https://app.ilograph.com/demo.ilograph.yt-dlp/Download%2520a%2520YouTube%2520Video

## 七、签名对抗升级链（2026 现状实证）
- YouTube PO Token：issue #17375 / #17376（2026-08）——mweb 无 cookie 场景先撞
  "playability status: LOGIN_REQUIRED / Sign in to confirm you're not a bot"，再过
  "GVS PO Token + bgutil PO Token Provider"这关，GoogleVideo media URL 仍间歇 403；
  报告者实测：公网 VPN/机房 IP 失败率明显更高，同 IP 同配置换视频/换时间也时好时坏——
  即风控已细化到 IP 信誉 + 令牌提供者（bgutil）+ 并发间歇性，命令行端无法稳定搞定。
  来源：https://github.com/yt-dlp/yt-dlp/issues/17375
  来源：https://github.com/yt-dlp/yt-dlp/issues/17376
- 浏览器/TLS 指纹：issue #17247 被打 impersonation 标签（说明直写 "HTTP/TLS fingerprinting,
  curl_cffi"）——反爬检测重心已从"参数签名"移到"客户端指纹"。
  来源：https://github.com/yt-dlp/yt-dlp/issues/17247

### F8. 水印二次收费："去水印"是免费生态的基础功能，收费即圈钱
- GitHub repositories search（q=tiktok downloader watermark）total_count=506——506 个免费仓库
  把"去水印下载"当基础功能，佐证任何把去水印拆出来卖订阅/单次的都是圈钱设计。
  检索入口：https://api.github.com/search/repositories?q=tiktok+downloader+watermark
  - n0l3r/tiktok-downloader：描述直写 "download video from tiktok with watermark or without
    watermark"，topics 含 tiktok-download-without-watermark。
    来源：https://github.com/n0l3r/tiktok-downloader
  - hansputera/tiktok-dl："Free TikTok Video Downloader | Watermark and Without Watermark Support"，
    自带站点 https://tdl.hanifu.id 来源：https://github.com/hansputera/tiktok-dl
  - TerminalWarlord/TikTok-Downloader-Bot（466 star）：TG bot "download TikTok videos without
    any watermark" 来源：https://github.com/TerminalWarlord/TikTok-Downloader-Bot
  - TufayelLUS/TikTok-Video-Downloader-PHP（344 star，2026-10 仍在推）："one page TikTok video
    downloader (watermark free) script" 来源：https://github.com/TufayelLUS/TikTok-Video-Downloader-PHP

### F9. 钓鱼 CLI / 恶意打包：包管理器是下载器产业链最脏的一段
- HN 2026-06-01《Malicious npm packages detected across Red Hat Cloud Services》775 分 / 454 评论。
  来源：https://github.com/RedHatInsights/javascript-clients/issues/492
- HN 2025-01《Snyk security researcher deploys malicious NPM packages targeting cursor.com》574 分。
  来源：https://sourcecodered.com/snyk-malicious-npm-package/
- HN 2020-11《Malicious NPM package opens backdoors on programmers' computers》。
  来源：https://www.zdnet.com/article/malicious-npm-package-opens-backdoors-on-programmers-computers/
- HN 2025-07《North Korean XORIndex malware hidden in 67 malicious NPM packages》。
  来源：https://www.bleepingcomputer.com/news/security/north-korean-xorindex-malware-hidden-in-67-malicious-npm-packages/
- HN 2025-03《Lazarus Group deceives developers with 6 new malicious NPM packages》。
  来源：https://cyberscoop.com/lazarus-group-north-korea-malicious-npm-packages-socket/
- 教训：任何"xxx-downloader CLI"从 npm/PyPI/Pip 安装都是供应链风险面；
  Search total_count=847（npm malicious package）说明这是常态而非例外。

## 八、WBI 签名的第二/第三出处（GitHub repositories search，total_count=12）
- 000ylop/bili-wbi-sign-rs（Rust）："Wbi sign for Bilibili"
  来源：https://github.com/000ylop/bili-wbi-sign-rs
- lc6464/BiliBiliWbiSign（C#）："哔哩哔哩 WBI 签名计算器"
  来源：https://github.com/lc6464/BiliBiliWbiSign
- likerai7817-boop/bilibili-video-downloader（Flask）："dedicated wbi-signed B站 channel
  (yt-dlp + manual wbi signing)"——yt-dlp 之外手工补 WBI 的现成例子。
  来源：https://github.com/likerai7817-boop/bilibili-video-downloader
- 检索入口：https://api.github.com/search/repositories?q=bilibili+wbi+sign

## 九、[unsourced] 与缺口清单
- SocialSisterYi/bilibili-API-collect：contents API 以 ref=master 和 ref=main 均返回 404
  "No commit found for the ref"，未抓到文档正文 → WBI 中文文档出处标 [unsourced]；
  已用 yt-dlp 源码 + 三个独立 WBI 实现仓库替代，机制本身有据。
  未抓到：https://github.com/SocialSisterYi/bilibili-API-collect
- Chrome DevTools 官方文档（network / network/reference）：web_fetch 三次均 TypeError 失败，
  标 [unsourced]；"Network→replay"模式已由 yt-dlp issue #9106（玩家原话：从 network requests
  里找 base m3u8 stream）与 ilograph 时序图两条出处覆盖。
- JoeanAmery/TikTokDownloader：repo API 404，标 [unsourced]；已用 506 仓库的 search 结果替代。
- HN Algolia 无 "bilibili wbi signature" 命中（nbHits=0），WBI 的社区讨论主要在中文 GitHub 侧。
- 尚未覆盖（后续可挖）：you-get extractor 源码级解剖、gallery-dl extractor 源码、
  cobalt API 的 OpenAPI 文档、f2/TikTokDownloader 完整逆向报告、Po Token provider(bgutil) 机制。
