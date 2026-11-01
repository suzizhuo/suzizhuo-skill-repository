# First Mile — 选购、SSH 首连、防火墙与最小卫生

For L0-L1 lanes (L3 skips). Each block: 是什么 → 为什么 → 怎么做 → 怎么验证.

## 1. 选购决策（今晚路径，不展开商业比较）

| 维度 | 今晚选择 | 为什么 |
|---|---|---|
| 产品形态 | 轻量应用服务器（Lighthouse 类） | 套餐捆绑基础资源+流量包、面板防火墙在前、界面比 ECS 简单；新手第一台不需要 VPC 自定义（来源 https://cloud.tencent.com/document/product/1207 ） |
| 地域 | 免 ICP 备案地域（如中国香港）**若域名没准备好** | 大陆机绑定域名走 80/443 要备案（周期以周计）；香港当晚即可公网访问。代价：延迟略高、部分运营商夜间不稳 |
| 系统 | Ubuntu 22.04 LTS 或 Debian 12 | `openjdk-21` 在 jammy 源里可直接 apt；教程与文档按 Debian 系写的都能照抄（Debian 理由 https://www.debian.org/intro/why_debian.zh-cn.html ） |
| 规格 | 2核4G 起步 | 实测口径：Java/Spring Boot 服务各吃 512MB–2GB，加上 MySQL，1C2G 直接 OOM 卡死、SSH 都进不去（https://news.ycombinator.com/item?id=45967814 ）。省钱可 2C2G + 2G swap，风险自负 |

**怎么验证**：控制台实例列表出现公网 IP（形如 `1.2.3.4`），状态 running。**不是**内网 IP（10.x/172.x）——填内网 IP 是最常见第一夜连不上。

## 2. SSH 首连（密钥优先，不是密码）

```bash
# Mac 本地（Terminal.app 自带，什么都不用装）
ssh-keygen -t ed25519                     # 一路回车；生成 ~/.ssh/id_ed25519(.pub)
chmod 600 ~/.ssh/id_ed25519
cat ~/.ssh/id_ed25519.pub | pbcopy         # 拷贝公钥
```

公钥粘到面板的"SSH 密钥"绑定处；没有该入口就先设 root 密码，连上后 5 分钟内补密钥（见 §4）。

```bash
ssh root@公网IP                            # 首次会问 yes/no（known_hosts），输 yes
```

**怎么验证**：看到 `root@xxx:~#` 提示符。
**失败分支**：
- `Permission denied (publickey)`：服务器只认密钥而公钥没绑 → 面板重新绑定，或临时用密码登录方式
- 超时/ refused：面板防火墙未放行 22 → 控制台放行 TCP 22；仍不行用厂商**网页终端**直连（绕过网络）再排查（https://cloud.tencent.com/document/product/1207/44642 ）
- 密码总错：控制台**重置密码**（按页面提示在线重置或重启后重置）

## 3. 防火墙两层（各管一段，都要配）

| 层 | 位置 | 管什么 | 命令/操作 |
|---|---|---|---|
| 外层 | 云控制台（轻量防火墙 / 安全组） | 实例之外的第一道闸，**浏览器打不开的九成是这里** | 面板放行 TCP 22 + 8080（来源按需 0.0.0.0/0） |
| 内层 | 系统 ufw / nftables | 系统内第二道闸 | Ubuntu 默认 `inactive`：`ufw status` 显示 active 才需要 `ufw allow 8080` |

明确不开放：3306（MySQL 只听 127.0.0.1 就够了，公开 3306 = 把数据库送给全网）。两层分工参考 https://www.digitalocean.com/community/tutorials/ufw-essentials-common-firewall-rules-and-commands 。

**怎么验证**：外层放行后，从 Mac `curl -m 5 http://公网IP:8080/` 只要"连得上"（哪怕 404）就说明两层通了；打不开先回面板查外层。

## 4. 最小卫生（5 分钟，不是"以后"）

```bash
adduser deploy && usermod -aG sudo deploy        # 非 root 日常用户
# 面板/sshd 配好后：禁用 root 密码与密码登录
sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
systemctl reload ssh
timedatectl set-timezone Asia/Shanghai           # 日志/数据库时间对齐本地
localectl set-locale LANG=C.UTF-8                # 中文项目在 US-ASCII locale 下必出乱码
[ $(free -g | awk '/Mem/{print $2}') -lt 4 ] && { fallocate -l 2G /swapfile; chmod 600 /swapfile; mkswap /swapfile; swapon /swapfile; echo '/swapfile none swap sw 0 0' >> /etc/fstab; }
```

为什么不是"以后"：公网 22 端口被持续爆破是常态（"3 个月 100 万次 SSH 尝试" https://news.ycombinator.com/item?id=11325752 ）；第一台机养成的习惯会带到所有以后的机器。
**怎么验证**：新开终端 `ssh deploy@公网IP` 能登；`ssh root@公网IP` 应被拒；`date` 显示 CST；`curl` 中文页面不乱码。

## 5. 今晚明确不做（写进交接单，别半夜加戏）

域名与 HTTPS（下一步，Caddy 自动续期见 deploy-runbook §6）、MySQL 备份 cron（第一天就要确认 mysqldump 能跑，但排程可以明早）、Docker（按 deploy-runbook §2 的决策表升舱，不是硬上）。

条件晋升（"以后"清单的触发式提前，GREEN-2 收编）：fail2ban 平时挂"以后"，但收到服务商爆破告警/`lastb` 见大量失败 → 立即提前；改 SSH 端口、禁 root 登录同理——威胁信号实体化时，这些项从"以后"变"今晚"。
