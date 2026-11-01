# Resource Types — 每类的猎取配方

通用前置：查询工程化（qualifier 表见 search-recipes.md）+ fallback ladder。下表给每类的种子词、资产形态、特别注意事项。

## 1. 工具/轮子 (crypto, recon, automation …)
- 种子：`<功能> tool/cli/<功能词>`、`topic:awesome-<领域>`
- 资产：release 二进制（按 arch）、源码
- 注意：优先官方 release；老 Mac 选 `darwin_amd64`；Go/Rust 工具优先（单二进制省心）

## 2. JAR 包/依赖 (Java 库)
- 种子：`<lib> maven repository mirror`、`org.<group>`、`topic:maven`
- 资产：jar/aar/sources jar、pom
- 注意：版本对齐 JDK21；优先 Maven Central 镜像仓库 clone（如 maven-central 搬运站）；本地放 `~/.m2` 或 lib/ 目录

## 3. 破解/keygen/crack (PARKED-ACTIVE，operator 点名类别)
- 种子：`<软件名> crack/keygen/patch/activate/license`、`in:readme`
- 资产：patched binary、注册机、patch diff
- 注意：**运行前筛查是本类的重点项**——文本清扫必做（已知此类仓库是高发投毒区）；默认容器隔离执行；license/卡密逻辑只作本地样本分析
- 降级：原 repo 404 时走 forks→archive.org（此类下架最快）

## 4. Exploit/安全工具 (POC, scanner, C2 研究样本)
- 种子：`CVE-<年份>-<编号> poc/exploit`、`topic:exploit`、`topic:vulnerability`
- 资产：PoC 脚本、exploit 源码、扫描模板
- 注意：靶场/授权目标才跑；证据纪律同 briefing 的验收单；OOB 验证用 OAST_DOMAIN 占位

## 5. 数据集/模型/权重
- 种子：`<任务> dataset`、`.gitattributes` LFS 检查、HF 模型镜像站
- 资产：parquet/csv/ckpt/safetensors、LFS 指针
- 注意：LFS 未拉 = 指针文件（`git lfs pull` 或 release 资产）；体积大 → 直接落容器，避免过老 Mac 磁盘

## 6. 电子书/教程/资料包
- 种子：`<书名> pdf/epub`、`awesome-<领域>` 列表页
- 资产：pdf/epub/md 合集
- 注意：搬运仓库易 404 → archive.org 优先存档一份

## 7. Docker/部署文件
- 种子：`<服务> docker-compose self-hosted`、`topic:selfhosted awesome`
- 资产：docker-compose.yml、Dockerfile、helm
- 注意：**镜像 tag 禁 `:latest`**；build 前读 Dockerfile 的 `curl|bash` 基镜像步骤；部署对象环境卡先行

## 8. PARKED：外挂 / 面具(Magisk) / 内核
- 槽位保留、配方备好（种子：`<游戏> cheat/hack`、`magisk module`、`kernel driver`）
- operator 明说"现在不用"：只建目录 `~/gh-res/parked/` + manifest 登记，不展开检索和执行流程
- 启用条件：他后话说要，再把本类提升到 ACTIVE，补一页专项（签名绕过/隐藏/过检测类内容单独成档）