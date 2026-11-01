# FIND — Query Engineering & Fallback Ladder

## Qualifier cheat table (official, docs.github.com/en/search-github/searching-on-github/searching-for-repositories)

| Need | Qualifier | Example |
|---|---|---|
| Name/desc/README | `in:name,description,readme` | `scanner in:readme` |
| Stars 区间 | `stars:>=500`, `stars:10..20` | `nuclei stars:>=5000` |
| Forks 区间 | `forks:>=205`, `fork:only` | `sqlmap fork:only` |
| 新鲜度 | `pushed:>2026-07-01`, `created:<2020-01-01` | `poc pushed:>2026-01-01` |
| 语言 | `language:go` | `subfinder language:go` |
| 主题 | `topic:vulnerability-scanner` | `topic:awesome-list` |
| 弃疗过滤 | `archived:false` | `exploit archived:false` |
| 体量 | `size:<50000`（KB） | 老 Mac 免拉巨型库 |
| 镜像仓库 | `mirror:true` / `mirror:false` | 找搬运站用 true |
| 组织/用户 | `org:`, `user:` | `org:projectdiscovery` |
| 授权 | `license:mit` | 二开友好过滤 |

## Machine-search commands (connectivity probe first)

```bash
gh auth status; curl -sI https://api.github.com   # 探通；不通 → 换 web 检索并声明渠道
gh search repos "<关键词>" --sort stars --limit 20 \
  --json fullName,stargazersCount,pushedAt,description,license
gh search repos --sort stars "<关键词>" in:name,description,readme topic:<topic> stars:>=500 pushed:>2026-01-01
gh search code "<代码特征>" --repo <owner>/<repo>
gh search prs/issues "<问题关键词>" --repo <owner>/<repo> --state open
```

Selection ranking (give 3 candidates, ordered): stars × freshness(pushed) × issue 区维护者活跃度 × release 有正式资产（带 checksum）> 直接 clone master。

## Fallback Ladder (repo 死了 ≠ 没辙)

1. **forks**：`gh search repos "<name>" fork:only --sort updated` → 按 pushed 最新排序找活 fork
2. **mirrors**：`mirror:true` + `mirror:false` 组合找搬运仓库
3. **gists**：`gh gist clone <gist-id>`
4. **archive.org**：wayback 快照取指定 tag 的源码/资产（dead repo 的标准退路）
5. **其他托管占位**：Codeberg / GitLab / SourceForge / Bitbucket 同名项目检索

每级都要报"当前在第几级、为什么降级"。

## Category seeds (种子词表，详见 resource-types.md)

工具 · JAR/依赖 · 破解/keygen · exploit/安全 · 数据集/模型 · 电子书/教程 · docker 部署 · 外挂/面具/内核（PARKED）
