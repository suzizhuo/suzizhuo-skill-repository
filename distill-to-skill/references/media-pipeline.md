# S0 — Media Pipeline: Acquisition, Normalize, Cleanup

## Source typing & acquisition recipes (placeholders; tool names are classes, not endorsements)

| Source | Recipe | Normalize | Notes |
|---|---|---|---|
| Bilibili 视频 | `yt-dlp-class "URL" -f "best[height<=1080]"` 或抓 `.srt/.ass` 字幕优先（快、准） | 字幕→文本；无字幕→抽音轨→ASR 分块 | 合集视频：逐 P 处理，记录 BV 号+P 号 |
| YouTube 视频 | 同上；`--write-auto-subs` 取自动字幕 | 字幕→文本 | 时长 >1h 必须分块 |
| 本地 MP4 | `ffmpeg -i in.mp4 -vn -ac 1 -ar 16k out.wav` | wav→ASR | 老 Mac 上重活先估时长：1h 音频 CPU 转录以小时计，长任务默认丢容器 |
| MP3/播客 | 直接 ASR 分块 | wav→ASR | 按话题静音/章节切分 |
| PDF | `pdftotext` / PyMuPDF 抽文本层 | text | 扫描件→OCR；页号=锚点 |
| EPUB/TXT/MD | 解包/直读 | text | 章节标题=锚点 |
| 网页长文 | fetch → 正文抽取（去导航广告） | text | 采集时间与 URL 快照 |
| 图片/截图 | OCR（本地或容器） | text | 图表内容用文字复述并标注 [OCR-推断] |

## Chunking & anchoring

- 视频/音频：每 15-30 min 一段，段 ID = `seg-NN`（含起止时间戳）。每个 fragment 的锚点写成 `HH:MM:SS-HH:MM:SS`。
- 书：章节号+页码；网页：小节标题；对话/访谈：发言人+轮次。
- **锚点是 V1 核验的唯一凭据**：每条 fragment 必须能指回具体 chunk/章节，否则不许进候选池。

## Provenance schema (第一行先建，后补)

```markdown
SOURCE: <url or path> | type: bilibili/youtube/mp4/mp3/pdf/epub/web/image
LANG: <zh/en/…> | duration/pages: <…> | fetched-by: <tool+version> | date: <yyyy-mm-dd>
```

## Environment routing (his two worlds)

- MAC_LOCAL（老 Intel Mac）：只做轻活（文本抽取、OCR 小图、few-chunk 剪辑）；bash 3.2、BSD 工具语义照旧。
- MONKEY_CONTAINER：下载、长音频 ASR、ffmpeg 转码、大文件落盘默认在这里；先探 `uname -m`、磁盘空间、网络策略（能否出网下载决定"宿主下载后拷入"还是"容器内直拉"）。
- 环境未定时问一句，不猜；假设入台账。

## Cleanup contract

每个阶段结束列出该阶段中间产物并删除：mp4/wav（ASR 完成后）、raw chunks（五路抽取后）、draft（打包后）。保留：transcript-with-segment-ids（进 `~/distill/<slug>/`）、cards、glossary、coverage-audit、最终 skill 文件夹。删除动作记一行 cleanup log（删了什么、何时）。
