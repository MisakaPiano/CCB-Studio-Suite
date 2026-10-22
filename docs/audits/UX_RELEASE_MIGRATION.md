# Release 迁回项目仓库与旧地址兼容

2026-10-23 JST。用户明确要求迁回项目仓库、保留旧更新地址；随后恢复人工测试，并要求不再频繁 checkpoint。范围为发行迁移和必要的104过渡测试包，不是日常版发布或源码分支合并。

## 已执行

- [项目 Releases](https://github.com/MisakaPiano/CCB-Studio-Suite/releases)成为主入口。100–103四份历史预发布及12个附件已复制，逐一核对GitHub返回的SHA-256、大小与本地原件；新tag指向原组装源码。原发行仓库的附件ID和字节均保留。
- 两条历史元数据分支原样复制到项目仓库。长期根未替换；仅snapshot/timestamp在线密钥通过GitHub公钥加密的Secrets API部署，root/targets仍留本机。项目main只新增维护工作流、锁定依赖、维护脚本，并在README前添加迁移说明；没有合并feature应用源码。
- 发布[104迁移测试包](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.104)，旧仓库保存相同附件。两边附件上传并校验后才签发104目标，元数据各以单次非强制提交发布。
- 104组装源码`b5c44c67b1f842f512c44d0da73eded45695cc19`，应用镜像仍为`3bea16b7405d1c1890d840e76ff3dee759faad5c`。安装器167322788字节，SHA-256 `1a68695144fdc1a775dd1be4061c00d80953572289019312c68fca0cc6a2ebe4`。未声称重新运行桌面构建或旧46项回归。

## 实际升级验证

独立工作区`.local/windows-rc/release-migration/real-103-104/restored-workspace`，由产品恢复入口建立，不使用真实资料。

未修改的103客户端从原仓库发现104；用户完成下载、保存退出、安装、重启并确认104显示最新。交接`55fac9f1-e41c-4ddb-9e67-881349b683e1`为COMPLETE。A/B各547文件核验通过；10条合成资产与整体状态相同，原备份未变。凭据状态为MISSING_BEFORE_UPDATE，不是可读真实凭据保全验收。

升级后的104安装配置固定`MisakaPiano/CCB-Studio-Suite`及`long-term-metadata`；直接运行该安装版更新客户端，以独立缓存访问项目仓库，实际返回UP_TO_DATE/104。没有编辑已安装配置来绕过迁移。

[项目续签运行39824206682](https://github.com/MisakaPiano/CCB-Studio-Suite/actions/runs/39824206682)与[旧仓库续签运行39824210494](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/actions/runs/39824210494)均成功。回读根、目标签名和版本化文件，续签仅将snapshot/timestamp从6提升到7，104目标保持不变。这是workflow_dispatch，不是自然schedule验收。

项目元数据最终提交`aae7b23415ef5ad5959dd7cb0cc8abb31bf09f26`，旧渠道`7c672b72105e51d670526ed52d859bef33169ee3`。公开证据见[evidence目录](evidence/UX_RELEASE_MIGRATION/)，包括附件、渠道、发布、安装交接与客户端回执。

## 兼容维护规则

1. 旧仓库不能删除、改名或停止长期元数据维护。旧102/103固定旧仓库，先更新到104，104才切到项目仓库。
2. 旧长期渠道可固定104桥接包；未来版本从项目仓库发行即可，无需无限复制每个新版本。必须保留104附件并继续在线续签；旧targets也需要离线定期续签，不能只维护项目渠道。
3. 两边targets当前均在2027-01-20 UTC到期，至少在2027-01-13前维护；root到期2027-10-22。不能把7天timestamp自动续签称为永久免维护。
4. 100/101使用已丢弃私钥的临时测试根，历史下载和元数据仍保留，但不宣称已自动接入长期信任链；需安装长期渠道包，不修改旧客户端信任配置。
5. 所有当前版本仍为测试prerelease，未设为latest日常版；未完成产品集中验收、真实提供者路径、跨机恢复或Authenticode。

## 执行问题与保留

发布辅助脚本首次因字符串拆分截断导入代码报SyntaxError，第二次仅改rsplit仍未限制次数，第三次加maxsplit=1后执行成功；这两次均在任何发布操作前退出。更早channel脚本一次语法错误同样发生在执行前。没有覆盖或删除远端附件，也没有以失败尝试冒称成功。

本机可接续脚本及构建件在`.local/windows-rc/release-migration`和`.local/windows-rc/*migration*.py`；公开证据不包含私钥、恢复码或密码。下一步回到日常版功能核对，升级迁移无需用户再做一遍。
