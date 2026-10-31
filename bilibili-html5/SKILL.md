---
name: bilibili-html5
description: "Use when 要给用户 B 站直链且浏览器打开即播/能下时。必须 playurl platform=html5，禁止默认 platform=pc，禁止默认本地代理。"
---

# B 站浏览器直链（html5）

对照 `~/Desktop/bilibili_playbook.md`。

## 规则

| 给谁 | 参数 | 地址栏 |
|------|------|--------|
| 浏览器打开/下载 | `qn=80&fnval=0&fourk=1&platform=html5` | 只要浏览器 UA，**不要 Referer** |
| curl 下载器 | 同上，或 `platform=pc` | UA 必须是 Mozilla；pc 还要 Referer |

`platform=pc`（`f=u_0_0`）地址栏无 Referer → 403 / 「未获授权」。
`platform=html5`（`f=h_0_0`）浏览器可直接打开。

交出前每条：

```bash
curl -sS -o /dev/null -w '%{http_code}' -r 0-1023 -m 20 \
  -H "User-Agent: Mozilla/5.0" "$URL"
# 期望 206
```

TTL 用 URL 里 `deadline` epoch，约 2 小时。过期重拉，不复用。

默认浏览器 Yandex，不要 Safari / `local_open`。
