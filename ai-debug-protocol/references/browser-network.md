# Browser & Network Debug — 抓包、断点、自动化

前端"转圈然后 500/白屏/没反应"类问题的兵器卡。

## 先看事实，再看代码

1. Network 面板：请求发出去了吗？状态码？耗时？请求体 vs 响应体对不对？
2. Console：有没有先于请求的 JS 异常（白屏常见：渲染前脚本就炸了）
3. 分层判读：
   - 请求没发出 → 前端代码/拦截器/Service Worker/CORS preflight
   - 请求发出、5xx → 后端层（转 black-box/white-box）
   - 200 但没渲染 → 前端解析/状态/路由

## 条件断点（前端）

| 手法 | 用途 |
|---|---|
| 条件断点（右键断点→条件表达式） | 只在第 N 次/某 id 命中，别在循环里裸断 |
| logpoint（不中断打日志） | 观测 without stopping |
| DOM breakpoint（subtree/attribute） | "这个 class 谁改的" |
| Event listener breakpoints | "谁触发了这个 submit" |
| fetch/XHR breakpoint | URL 含关键字时停 |
| blackbox script | 把框架源码排除出调用栈 |

滥用警告与后端同构：高频路径条件断点测实测慢 ~2000 倍；只在缩过范围后用。

## 抓包与外带验证

```bash
# 从命令行复刻请求，排除浏览器因素
curl -isS -X POST 'http://TARGET/submit' -H 'Content-Type: application/json' -d '{"k":"v"}' | head -30
# 带 cookie/token 复刻时注意脱敏
```

- 请求重放：把 Network 面板的请求 Copy as cURL → 改一个变量再放，一次一变量
- CORS：看 preflight（OPTIONS）是否被拒；响应头 `Access-Control-Allow-*` 三件套
- 慢请求：TIming 细分（DNS/connect/TTFB/content）

## 自动化 CI 化的判据

- 现象 → E2E 用例（一条），失败先行，修复后转绿（FAIL_TO_PASS 纪律同主协议 Phase 0）
- 复刻按钮点击链路用同一坐标/选择器纪律（见 gui-live-debug.md 的反馈验证）

## 纪律

- 先固定最小复现页面/URL，再动代码
- 抓包产物脱敏后再贴报告（token/cookie 打码，只留结构）
- Network 没请求 ≠ 网络问题：先确认前端代码走到了发请求那行
