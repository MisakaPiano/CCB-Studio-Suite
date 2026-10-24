# 下载入口与 Project 阶段交接：实际执行回执

2026-10-24 Asia/Tokyo。仅文档及现有证据整理。用户正在试用115，本轮不操作日常安装或要求提前确认。开始在Suite授权根、`feature/windows-rc-continuous@4c0f35deaba980cbc068b8ac3968bc730a952f8d`，index/worktree/untracked为空；未覆盖无关编辑，没有reset/clean/stash或切换主工作树。

## 远端实际文件

| 仓库/分支 | 唯一修改文件 | 实际提交 |
|---|---|---|
| MisakaPiano/CCB-Studio-Suite / main | README.md | [810775488b9ecc298b5295dde4e7d04e8fa20b22](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/810775488b9ecc298b5295dde4e7d04e8fa20b22) |
| MisakaPiano/CCB-Studio-Suite-Releases / main | README.md | [e47d47abf5e77a94f9c0c7f71695cc0ad6b35eeb](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/commit/e47d47abf5e77a94f9c0c7f71695cc0ad6b35eeb) |

通过GitHub Contents接口在各默认分支形成并推送文档提交；提交父节点与预检一致，每份commit的files严格只有README.md。没有合并应用分支。旧库原正文原字节保留为新迁移提示的后缀；主库同步纠正旧“不是可运行软件/拟议仓库”描述，并给出当前限制与交接入口。

重新枚举实际非draft Preview，最新为115，而非把114硬编码为永久最新版。115发布页、Git tag `ff145d051bae286ab3e27cadd41195f8bd4c4e18`、安装包168890283字节及GitHub digest均与本地既有安装器散列相同：`356e749bd346af4a1d419a0d8b48b1e27d2314e6fce5763fa0933917247862dc`。只读校验，没有执行或改写安装包。

首页下载指向115发布页，另有固定全部版本链接；注明在Assets取实际windows-x64.exe，Source code ZIP/TAR不是安装器。明确实验性邀请Preview、非稳定/完整RC验收，Prompt比较待办及115新流程、完整人工/资源/故障验证边界。

## 持续分支实际文件

- `handoff/PROJECT_PHASE_REVIEW_20261024.md`：限定外部基线、冻结身份、归并能力变化、五种状态、重点审计及未关闭事项、原Project判断任务。
- `docs/rc/PREVIEW_UPDATE_CONTINUITY.md`：沿原流程增加人工同步首页链接步骤；修正日常目录已改名和115固定启动器说明，不新增自动化。
- `tasks/PUBLISH_ENTRY_PROJECT_HANDOFF_20261024.md`：本轮用户明确授权、两个默认分支README例外和禁止事项。
- `docs/00_INDEX.md`、`docs/CURRENT_STATE.md`、`docs/TASK_REGISTRY.md`：新增本轮文档完成状态及交接索引，不改旧失败历史。
- 本审计文件及 `docs/audits/evidence/PROJECT_PHASE_20261024/`：公开审查原文/身份、源差异列表、11次开发运行索引、35个白名单原始运行文件的ZIP与可读副本、远端文档提交和公开回读记录。完整实际路径见该目录 `DOC_CHANGE_RECEIPT.json`；不复制图片、用户数据或密钥。

第一份持续分支文档提交为 `ef6a784`；最终收尾文档提交在本文件所在后续commit中。应用源码及release冻结仍是ff145d0，没有新应用版本。

## 实际检查

已读取本地及主库默认分支AGENTS，旧发行库根无AGENTS。只读获取两库默认分支/README/既有工作流/全部发行与tag、所有4个Issue及15条评论。原Project聊天未读取，最后可回读公开Project限定审查为 [678caf1](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/4#issuecomment-6319270550)，不把执行Codex的报告提升为独立验收。原始授权原件亦未读取，未重设抽象版权门槛。

6个公开页面返回200并含预期内容；两个README回读逐字节相同，35个原始文件ZIP CRC/逐项SHA通过，GitHub回读ZIP散列匹配。[完整回读结果](evidence/PROJECT_PHASE_20261024/PUBLIC_DOCUMENT_VERIFICATION.json)。公开首页嵌入数据 `payload.sidebarAbout.sections.releases` 显示16条release/tag，并实际存在Releases标题；初始卡片内容是客户端加载占位，不声明浏览器最终卡片视觉已测，也不为此调整Pre-release标记。

两库所有tag与release ID/状态/附件大小/digest保持原样；主库long-term-metadata仍60c527f5，旧库仍7f3cc7d8，两个update-metadata分支同样未变。工作流未改，原schedule/workflow_dispatch不会被README push触发；未访问签名托管、创建私钥、写TUF元数据、触发续签或发新版本。标准Git认证只在内存供授权API使用，不记明文、不装入交接。

本轮没有Gradle、产品用例、真实Provider、GUI验收或全项目Project复审执行。交接中46项/13项以及其他历史计数均标明原执行身份；不能当作本轮重新跑过。

## 文档检查中保留的纠正

准备旧README时，首轮本地“原正文后缀相同”断言发现Windows换行转换；没有远端写入，改为显式UTF-8原始字节组合后通过。首次staging检查发现原始Gradle日志行末空白，未提交；精确原字节在ZIP内保存，可读副本统一换行并清理行末空白后检查通过。两项都是文档准备问题，不是产品测试失败/通过的新结论。网页读取工具首次cache miss后改用普通公开HTTP和GitHub API核验，没有借不完整页面声称通过。

下一动作：用户将固定交接交给原Project独立审查；115命名归档/组织树/任务栏试用结果继续PENDING。收到具体反馈前不自动改代码、新发版或代签验收。
