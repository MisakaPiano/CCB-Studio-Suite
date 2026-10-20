# Windows RC Continuous — active contract snapshot

Authoritative issue: https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2

Latest reviewed data baseline: `27dbd5d578f364afb529fc33944386c0c107f0da`. Source snapshot and complete formal Gate/comments: `../docs/audits/evidence/WINDOWS_RC/CONTRACT_SOURCE.json`. User activation is in this Codex chat; historical task prohibitions are superseded only within this contract. Read CURRENT_STATE and checkpoint ledger before resuming.

# Windows Studio 首个长期使用版：一次性连续开发到 RC

**用户目标：**不按约 20 个内部 Slice 反复请求真人验收，而是做完工程性检查点 CP1–CP4、把可测试的 Windows Release Candidate 交用户集中验收。**这不是零测试冲刺，也不授权立即公开发布。**

## 唯一前置门禁
- 必须先完成 [A0-002 Project Gate #1](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/1)，审查结论标记 `A0-002: APPROVED_FOR_RC_RUN`；若 `REVISION_REQUIRED` / `BLOCKED`，停止生产修改。
- 任务执行前复核 `main` 实际 SHA 与工作树、已有正在执行的 Codex 任务/分支。初始参考：`6d3d2b22651f9744807925f74c6941f681bbef02`，后续不能继续假定 HEAD 不变。
- 定向读取 `AGENTS.md`、`docs/00_INDEX.md`、`docs/CURRENT_STATE.md`、`docs/TASK_REGISTRY.md`、`docs/SOURCE_LOCK.json`、`docs/02_PRODUCT_REQUIREMENTS.md`、`docs/04_DATA_SAFETY.md`、`docs/05_UPDATE_AND_RELEASE.md`、`docs/10_ACCEPTANCE_MATRIX.md`、`docs/audits/A0_001_REUSE_AUDIT.md`、`docs/audits/A0_001_DEPENDENCY_AND_GAPS.md` 与 `tasks/A1_001_IMPLEMENTATION_CANDIDATE.md`。来源审计已做，不得无差别重做全仓审计。
- 在新 `feature/windows-rc-continuous` 分支实施（若已存在则先验证身份，绝不覆盖），`main` 仅用于只读基准和之后人工决策。保持一个主执行者，禁止与别的 Codex 共享可写工作树。
- 开始前将 Project 已审核的 CP 合同纳入 Suite 新分支的文档及任务登记，**不要改写历史 A0 回执**。

## 连续工程检查点（约 20 内部 Slice；允许在同等验收下合并/拆分）
### CP1 / 数据和更新基础（预算 ~6）
最小 JVM/Gradle 可重现构建，独立数据根和单写所有权、原始资产不可变字节与哈希、安全的 Recipe/工程文档、跨文件/索引提交日志与崩溃重开、备份/恢复、更新器 A→B 内部垂直演练方案及合成脚手架。最先以 A1-001 资产底座候选为依据，不采用 permissive loader 静默跳过损坏数据。**验收**：故障注入/重启恢复/备份校验自动化全部通过；损坏/未知格式不被默认覆盖。

### CP2 / Windows 独立 Studio 生图（预算 ~6）
保持 CCB 已验证的 NovelAI Prompt/请求核心权威，构建独立 Compose Desktop Studio（非聊天容器必需）、AI 设计和继续修改 Prompt、NAI 生成及中间结果接收、History/原图保存、LLM/NAI 命名账户安全选择与冻结真实 provider 身份、导入/拖入图片、Metadata、完整 Recipe/Prompt 对比和源追溯。可用 mock/loopback；**不能**为了独立 Studio 复制第二套未经等价验证的 Prompt/NAI serializer。需要修改 Desktop 时停止该依赖切片，提交 Desktop 独立复用合同，其他不依赖部分可继续。

### CP3 / 非破坏图像编辑（预算 ~5）
参考 TypeSet Forge 1.1 的**实际本地只读参考文件**，不要将 HTML/IndexedDB/浏览器渲染作为原生运行核心。实现可持久化编辑图层与撤销/重做、横竖排基本嵌字、矩形/画笔/圈选打码、多选/拖序/批量操作、分类导出、原图与派生输出分开、普通/隐私导出差异。未冻结的富文本/小人书/PDF 不得挤占首版底线。**验收**：合成图像/多工程/缺字体/退出重开/失败保全回归通过。

### CP4 / Windows Release Candidate 门禁（预算 ~3）
完整应用内版本发现、用户确认、下载、发布者/包/渠道/版本可信校验、安装、重启与完成回执；**不是打开外部下载网页**。使用隔离测试密钥/环境进行内部版本 A→B 真实更新验证、无损数据迁移、错误签名/错误包/取消/断网/磁盘不足/安装失败/旧版拒写新数据/恢复失败案例。输出可供用户测试的 Windows 发行包、可复查 manifest、已知限制和**按一次启动批次组织的真人验收清单**。未建立正式发行签名身份不冒充已公开发行；不创建/上传正式私钥。

## 执行纪律
- **每个 CP 自动运行必要编译、聚焦单测、集成/故障注入、数据身份检查**；该 CP Green 且未越权可直接执行下一个 CP，不等待用户逐 Slice 批准。
- 每个 CP 使用明确的 checkpoint commit，经过 Secrets/隐私文件检查后正常推送远程开发分支；保持对应 source SHA、原始测试报告、RED/SKIPPED/NOT TESTED 与剩余风险。可维护**一个 Draft PR** 作为进度入口，但禁止自动 merge。
- 普通编译错误、可定位的单元测试失败在授权范围内自行修复并重跑；不能以跳过关键测试换取 GREEN。
- 分支完成/任务执行器时间或上下文中断时，从最近**已推送**的 checkpoint 延续。不要重启 A0，不另建互相冲突的工作树，不要求用户逐 Slice 介入。无任务运行器时本 Issue/分支也可用来人工重新启动 Codex；**Issue 本身不代表后台任务已启动**。
- 禁止：真实用户数据/SecretStore 读取、真实付费 API、正式密钥生成/上传、任意公开网络监听/服务运营、强杀残留进程、清理未授权缓存/历史包、重写官方 Prompt、未经许可跨仓写入、生产自动发版。
- 所有正式首版 REQ-01–12 必须覆盖或显式记录 RC BLOCKED；Windows v0.x **不因仍是 Preview 而允许清空数据根**。服务器集中管理员 API/多端队列是保留的后续 B1 设计，本任务不强制 BYOK，也**不实施 Server/Android 功能**。

## 交付与项目审查
交付完整：CP1–4 实际 commits / HEAD / compare 或 Draft PR、主要改动、自动化测试原始输出、可重现 Windows 发行包与校验、版本 A→B 更新结果、数据/凭据保全证据、未通过/未测清单、集中真人 Windows 测试步骤。实际无包或更新环节未通过时明确 `RC BLOCKED`，不能以文档或截图代替。

**Stop escalations**：数据丢失/迁移危险；供应商/上游语义越权；关键功能需要 Desktop 写入；秘密/账号/签名/付费操作；无法安全通过测试；授权/许可证阻塞；发行前实机无法验证。其余非根本缺陷自主解决。

`Project Review` 与用户真人验收在 CP4 之后集中执行；未经独立审查不 merge main、不公开发行。
