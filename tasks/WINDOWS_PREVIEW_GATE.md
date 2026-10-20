# Windows Preview Gate / Issue #3

Source: https://github.com/MisakaPiano/CCB-Studio-Suite/issues/3

# CCB Studio Suite · Windows 连续开发恢复任务（Preview-first）

**用户明确授权：在既有连续开发合同范围内持续实施、自检、自修，正常工程问题不要停下询问；先推进到可交给用户集中验收的 Windows Preview Gate。**
**任务状态：READY / 待原 Windows Codex 主执行者接手。本 GitHub Issue 的创建本身不等于本地 Codex 已经启动。**

## 1. 权威和优先级

- 原连续工程合同：[Issue #2](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2)；有效批准：[Issue #1：A0-002 APPROVED_FOR_RC_RUN](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/1#issuecomment-6296885017)。
- **纠偏覆盖原 CP2 推断性来源 STOP：** [Project Gate 纠偏](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/1#issuecomment-6299072689) 与 [Codex 纠偏](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2#issuecomment-6299089187)。用户 `USER-DIRECT` 声明原 CCB 作者已给其完整授权并放弃相关权利、Desktop 为其本人项目；不要仅凭公开仓库未设 LICENSE 或缺少预制 SDK 推断禁止复用。这不替代对具体有冲突的第三方素材做精确排除，不授权修改 Desktop 工作树。
- **本 Issue 是针对后续执行的最新用户要求**：先做 Preview Gate，之后集中让用户处理主观/存疑项，再推进正式 RC，原 `REQ-01–12` 和完整安全更新的首个长期使用版本门槛不得取消。本 Issue 不追改已有历史 `RC BLOCKED` 和失败回执。

## 2. 从真实 checkpoint 接续

- 唯一允许写入的工作目录：`H:\CCB-Studio-Suite`；不在 `H:\ChatChatBar-Desktop` 中工作。
- 远程连续分支：`feature/windows-rc-continuous`；经检查时最新交付 `86760bc067712ab26d7d4b74d45ad44a0d0e18e7`；CP1 真正实现 `9d3035b98adea32908d4d3e04267127d0d9c25f4`。
- 开场阅读：`AGENTS.md`、`docs/00_INDEX.md`、`CURRENT_STATE.md`、`TASK_REGISTRY.md`、`SOURCE_LOCK.json`、`docs/rc/CHECKPOINTS.md`、`docs/rc/CP1_EXECUTION.md`、`docs/rc/DEPENDENCY_ESCALATION.md`、本 Issue 及 Issue #1/#2 正式评论。
- **先实际检查** Git HEAD、分支、index、worktree、untracked、远程 checkpoint、`.local/windows-rc/owner.json` 和已有活跃 Codex。只允许一个主执行者占有同一 Suite 工作树。若另一执行者仍活跃，则不能抢占或并行覆盖，报告精确所有权冲突。
- 沿用现有分支及最新安全 checkpoint，后续远程若已前进则只读核对增量，不强行 reset/rebase/clean/stash。保留 R1、CP1、旧 FAILED/SKIPPED/UNKNOWN 证据；不重新执行 A0，也不为了开工重复 CP1 已完成的稳定测试。

## 3. 持续执行、自修和绝不伪造验收

1. **正常工程问题不 STOP**：编译、依赖闭包、UI 接线、mock、合理资源约束、可复现单测/集成测试失败，在授权范围内自行定位原因、修改、重新运行**可靠且适用**的测试，直至该检查点真正通过。分阶段安全 commit + push，在 GitHub 保留真实 SHA 与失败沿革，不按内部 Slice 找用户签字。
2. **对以前已证明不可靠/失败的验收方法设置禁用标识**：不能再次把它们当作有效的 PASS/FAIL 判定，更不能重复用旧失败脚本得出新的验收结论。例如未修复的 `Verify-Package.ps1` 默认 Root、此前失败的 Gradle 默认 native 缓存入口、把 TUF 合成 trust lab 当作真实 Windows A→B 安装验收、把 mock 成功当真实 Provider 通过。可以先修正或替换测试方法，验证可信性后再使用；完整保留之前失败记录。
3. **“测不到”不等于“失败”**：由于缺少真实 API 授权、设备/安装设施/签名权限、外部服务或无法在当前环境可靠测量的项目，严格登记 `NOT TESTED / UNKNOWN / ENVIRONMENT UNAVAILABLE`，说明阻碍与验证所需条件；不能改名 `FAILED`，也不能冒充 `PASS` 或跳过关键工程验证后称 RC GREEN。
4. **存疑/主观事项先保全、集中待用户 Preview 验收**：外观细节、交互取舍、真实供应商效果、需要真人观察的非破坏性不确定项登记在 `PREVIEW_REVIEW_ITEMS`，附可复现步骤、截图/合成证据与安全临时实现；不要自行臆测用户偏好并反复改动。若存在数据损坏、凭据泄漏、不可安全升级等严重危险，不得以“待验收”为由继续危险操作。
5. 不陷入无限失败重试或不可重复的测试：确定方法可靠→最小复现→修复→重测；环境不可测则标注状态并推进不依赖的工作。少量、可信和可定向的回归优先，只有阶段门禁要求时才运行耗时的大规模回归。

## 4. CP2 实施 — 不再将“没有现成 SDK”当 STOP

按 Desktop 精确来源 `MisakaPiano/ChatChatBar-Desktop@5ef95428421f7c693b010ac88f51d470804ab07a` 的已审核共享语义，在 **Suite 工程内部**建立固定 SHA、闭包完整且可重现的受控共享图像核心/窄构建依赖。保留唯一权威 `NovelAiPromptAuthority`、Designer、NAI 请求/模型/fallback/guidance/frame/metadata 语义，配 golden 向量与可执行等价回归；不要复制第二套独立 serializer，也不要依赖活跃 Desktop 目录。没有预制制品是本任务需解决的工程问题，不是额外审批条件。

继续独立 Compose Windows Studio：AI 写/改 Prompt、NAI 生图的 mock/loopback 与中间结果处理、原图保存、命名 LLM/NAI 账户/SecretRef、任务冻结身份、图库/多图导入、metadata、完整 Recipe 比较、重启/错误保全。真实付费供应商仍 `NOT TESTED`，不调用真实账户/Token。

## 5. CP3 与 Preview Gate（先于正式 RC）

- 推进非破坏编辑工程、批量遮盖/马赛克、横竖排嵌字、排列/撤销重做、普通/隐私导出等可在合成资产上验证的功能；工程、原件、导出严格分离。
- **近期目标是产生真正可启动的 Windows Preview 候选包及明确的 Preview Gate 审查材料**，而不是因真人测试尚未进行就直接将所有事项标 FAILED。运行已可行的自动编译、可靠单测、受控 UI smoke/重启、数据持久化和基本打包验证。
- Preview 的结论明确区分：
  - `PREVIEW_READY_FOR_USER_REVIEW`：实际启动/包身份与关键安全工程测试有证据；剩余真人/外部测试已列明。
  - `PREVIEW_GATE_WITH_GAPS`：有可启动包，非严重但不可可靠测量/待真人决定的事项单列，**未测绝非 PASS**；交给用户统一验收是否放行后续 RC。
  - `PREVIEW_BLOCKED_SAFETY`：确有严重数据/密钥/安全危险、无法安全运行或无法生成最小可启动包；应提供精确证据而非宽泛猜测。
- 内部 Preview 只能以**隔离合成数据**验证；若尚未通过首个长期使用版完整应用内更新、迁移和数据保全要求，不得把 Preview 当作可长期保存真实用户作品的正式版本，不得公开发行。后续 CP4 真实 A→B 更新与完整 RC 仍按原 Issue #2 保留，不自动删减。

## 6. 真正严重问题才升级，其他自主推进

STOP 仅针对有具体证据的严重问题：不可保全资料的风险、真实秘密泄漏、未经授权付费/访问私人账户、擅改官方 Prompt/请求/fallback、跨仓生产写入、真实第三方资源限制（精确到文件）、不可安全使用/迁移的实际关键缺陷、正式签名/系统信任/公开网络设置需追加授权，以及工作树被别的执行者占用。解释影响范围和已尝试的合规替代；无关工作可继续，但不可制造虚假通过。
不得强杀其他 Java/Codex、清除历史包和缓存、修改 Desktop/upstream、开放服务或变更公开仓库权限。

## 7. 交付、资源和停止点

- 持续工程检查点：一个 Suite Codex 主执行者、低资源串行测试；安全 staged commit/push 当前开发分支，证据指向真实源码 SHA，历史失败原件不擦除。
- GitHub：https://github.com/MisakaPiano/CCB-Studio-Suite
- 私有非 Git 证据 Drive：https://drive.google.com/drive/folders/1DcysN49RtUDnY9jx4wZ4z41F_j4ttUT2
- 保留日志/ZIP/manifest 原始 size/hash 与 Git commit 映射，不泄漏 Token/私密 Prompt/图库/密钥；Drive 权限不可私改。
- **只在到达 Preview Gate，或出现确有证据且无法在授权内安全解除的严重 STOP 时，才向用户提交最终交付回执**。普通缺陷自己改、自己测、继续到门禁；存疑和真人事项集中列入 Preview review，不逐项打断。
- 最终必须交付固定 commit/分支、Preview 包与 SHA-256、可复现测试与已知失败、`PREVIEW_REVIEW_ITEMS`、明确的 `PASS/NOT TESTED/UNKNOWN` 矩阵、后续 CP4 完整更新差距和一次性人工验收步骤。若 Preview 不能安全交付，明确列出真实阻塞，不夸大产物。
- 若执行器/上下文会话中断，先保存可验证 checkpoint，再由同一授权执行者从最近已推送提交继续；**Issue 不代表自动创建后台运行器**。

**直接开始持续实现，除严重 STOP 不要因抽象版权/政策疑虑、普通测试错误、尚未有 SDK、或真人项目未测而擅自停工。不得自动合并 main、正式发行或调用未经授权的付费服务。**

## 本会话直接追加授权与界面要求

- 用户允许只读使用 Desktop 已保存的真实 API；所有实际请求共享每滚动小时最多12次、相邻至少300秒的预算。失败/重试/Planner/Repair均计入，不自动重试不确定付费结果。用户贴出的后备凭据不写入合同、代码、日志或交付物。
- 后续所有 Suite 窗口统一 CCB 小象 logo，绿色改为蓝色。固定来源与蓝色衍生见 studio-desktop/resources/brand/PROVENANCE.json；不修改 Desktop 图标。
- 屏幕控制当前不可用，不能宣称鼠标/视觉自动验收通过。用户提供的截图证实首个窗口已渲染，并暴露出文字对比度问题；应用内绘制/流程/退出/重启自检与真人屏幕验收分列。

- 用户追加要求参考最新 CCB Desktop，现代化整个界面及带功能按钮的自绘顶栏；固定视觉来源 ab3d06abcaca2df8b21c734841d7c4e744099f51，独立于图像请求权威。保留原有编辑/保全行为，窗口控制接入排水关闭。
