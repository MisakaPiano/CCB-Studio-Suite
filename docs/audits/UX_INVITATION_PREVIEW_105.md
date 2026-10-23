# 邀请 Preview 105

2026-10-23。用户明确要求优先发布以生图/图库为核心的实验版本，离开电脑期间不安排人工；附加功能的剩余人工后置。发布持续授权与范围变更见 [记录](evidence/UX_INVITATION_PREVIEW_105/PREVIEW_SCOPE_AUTHORIZATION.json)。Prompt 新工作台 PENDING，既有功能保留。

## 实际执行

- 应用源码67550683936c5e644eb4958cb0e167495f22c649；模块/工具哈希前后一致。
- 最终桌面回归实际57项通过，0失败/错误/跳过，295.863秒，离线单worker；createDistributable成功（应用映像任务UP-TO-DATE）。[摘要](evidence/UX_INVITATION_PREVIEW_105/final-regression/summary.json)与逐项/日志保留。
- 覆盖图库交互/来源/排序/标签/回收站、APNG处理结果与原件、已有Prompt参照、两页编辑/队列/导出及备份恢复拒绝覆盖。合成资料，无真实API，不能代签人工。
- 113份共享权威文件与只读Desktop固定5ef95428421f7c693b010ac88f51d470804ab07a逐字节匹配；Desktop未修改。[证据](evidence/UX_INVITATION_PREVIEW_105/AUTHORITY.json)。
- 用户确认既有实际AI设计→应用→生图，且正常重开后图与Prompt/模型/参数保留。[限定回执](evidence/UX_INVITATION_PREVIEW_105/HUMAN_CREATION_RECEIPT.json)。
- 只读GitHub预检：最新公开仍104，项目自然schedule续签run39926210958成功。[结果](evidence/UX_INVITATION_PREVIEW_105/REMOTE_PREFLIGHT.json)。

## 候选与发布

旧candidate-1仅保留审计，新candidate-2冻结上述应用映像与组装源码a416a820955a720181cb97c33131a3c4890bb786。

- 安装器168194459字节，SHA256 `afee8e329636e7ab1e71013c38e397295a377962fc9a060385ed67641901e336`；547文件清单/550实际文件（含清单及安装完成/卸载文件）核验，新目录静默安装通过，再次安装拒绝17且文件未变。[安装证据](evidence/UX_INVITATION_PREVIEW_105/INSTALLER_SMOKE.json)。
- [Preview105](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.105)已实际公开为prerelease，安装器/说明/许可/散列四附件由GitHub回读size/digest逐项匹配。源分支已推送并回读，标签精确指向a416a82。
- 更新根保持408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a，项目long-term-metadata提交c3e368307088f5c18acc1779a7ee2097a047a8db，targets4/snapshot9/timestamp9。只在安装器公开且核验后切目标，全部元数据签名/散列链和提交字节回读匹配。main及旧渠道未改。[发布结果](evidence/UX_INVITATION_PREVIEW_105/PUBLICATION_RESULT.json)。
- 发布后实际104检查已验证签名目录与105身份，但首次完整下载/安装尝试因H盘仅约174MiB被空间保护拒绝，未下载或动用户资料。此失败保留为[初次证据](evidence/UX_INVITATION_PREVIEW_105/LIVE_UPDATE_INITIAL.json)，不能记为下载/原生升级通过。正在处理本轮临时产物空间；结果另追加。
- 随后执行不暂存文件的低磁盘只读校验：实际沿安装信任根认证公开目录，再完整流式读取168194459字节安装器并核对SHA256，14.448秒；安装105检查返回UP_TO_DATE。[流式证据](evidence/UX_INVITATION_PREVIEW_105/LIVE_STREAM_FINAL.json)。此项证明真实公开下载字节和当前版本检查，不绕过产品空间保护，也不代签磁盘暂存/104→105原生重启。清理candidate-2/payload及本轮update-smoke-final已向用户提出，未答前保留。
- 用户随后批准清理并要求迁移保存key/数据。清理后实际104→105公开下载、散列、新目录安装及当前版本检查全部通过，249个合成资料文件和550个旧程序文件原字节保持；[结果](evidence/UX_INVITATION_PREVIEW_105/LIVE_UPDATE_FINAL.json)覆盖首次空间阻断，但不代签原生105重启。固定105已接入既有工作区与受保护凭据，[本地接入/进一步空间清理](UX_LOCAL_PROFILE_AND_CLEANUP.md)单列。

附加编辑/排版/交付/恢复连贯人工、当前APNG快捷键接收端及完整多来源比较人工均为未验，按用户明确范围后置，不阻断本次实验Preview。既有通过回执保留，不重复发新小包要求用户重测。不同于完整日常版/RC，PRODUCT_HOLD不作为已发布状态的代称。
