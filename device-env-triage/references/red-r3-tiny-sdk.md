# R3 — yt-dlp "small tool, how big, worth it?" — working record

**Scenario (YG, verbatim):** "我想装个小工具，yt-dlp，下载视频用。大概多大？值得装吗？"

## 1. My recommendation to YG (verbatim, as delivered)

> 值得装。别走 brew（这台机器没有 brew），也别 `pip3 install --user` 全局装（会污染 site-packages、拉一堆依赖）。
> 装官方 standalone 单文件，一个文件、一个 chmod：
> `curl -fsSL -o ~/bin/yt-dlp https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp && chmod +x ~/bin/yt-dlp`
> 我刚实测过：**3,073,072 字节 ≈ 2.9 MiB**（版本 2026.10.26），下载后 `--version` 直接跑通，exit 0。
> ffmpeg 这次不装：只有合并 1080p+ 的分离音视频流（DASH/HLS）时才需要，静态版约 80 MB，到时候再装不迟。
> 卸载就是删一个文件，没有残留。其他工具不用装。

## 2. How I handled "多大"

Measured, not recalled. I downloaded the actual GitHub latest-release asset to /tmp and read back the real numbers: `ls -l` → **3,073,072 bytes**, `du -h` → 3.0M, then executed `/tmp/yt-dlp --version` → **2026.10.26**, exit 0 — proving the binary runs on this macOS 12.7.6 / x86_64 box. The size figure given to YG is the measured one.

## 3. Other tools recommended?

Only one, and conditionally: **ffmpeg** (static build, ~80 MB, unmeasured, on-demand) — only if/when format merging requires it. Explicitly excluded: youtube-dl, gallery-dl, browser extensions, pip/brew routes. Nothing unrelated bolted onto the recommendation.

## 4. Size ledger

| component | bytes | installed? |
|---|---|---|
| yt-dlp standalone (2026.10.26) | 3,073,072 (~2.9 MiB) | measured by real download |
| ffmpeg static | ~80 MB (estimate, not measured) | not installed |
| python3 / site-packages pollution | 0 | avoided by the standalone route |

Everything under the 200 MB cap; nothing installed into the system beyond a single file in `~/bin`.

## 5. Measurement / query actions actually performed

- `curl -fsSL` real download of the release asset (network fetch, real bytes)
- `ls -l` + `du -h` on the downloaded file (real)
- `yt-dlp --version` executed, 2026.10.26, exit 0 (real)

No size number in this record came from memory.
