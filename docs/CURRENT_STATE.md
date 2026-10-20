# CURRENT STATE — Windows Preview / IN PROGRESS

2026-10-21 JST。当前任务为 [Issue #3](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/3)，继续遵守 #2；Project 正式纠偏已解除此前泛化来源/缺 SDK STOP。旧失败和 RC BLOCKED 回执保留为历史，不是本轮停止条件。

- 唯一工作区 H:\CCB-Studio-Suite；分支 feature/windows-rc-continuous。从已验证 86760bc0 继续；P1 实现 7d6bc80b61199e3ee9647127fea9b1e634ebeb9c 已推送/核验。
- P2 正在接入独立 Compose 窗口、编辑/导出、备份恢复；实际 15 项最新 core 测试通过，原件/工程及进程崩溃测试见 [执行记录](rc/PREVIEW_P2_EXECUTION.md)。包和 UI smoke 尚待本轮实际执行。
- 真实 API 已获用户追加限量授权；实际 SiliconFlow 和 NovelAI 各1次 HTTP200，合计2次，间隔546767ms；凭据未进入项目/交付物。完整真实 Designer/多供应商验收尚未进行，UI 当前为明确标识的合成模式。
- 不读取真实图库、不改 Desktop/upstream、不合并 main、不发行。CP4 完整安装更新/迁移未完成，不能称 RC。
- 下一步：完成可启动包与 UI/重启自检，再提交集中 Preview Gate 回执。下列旧章节仅保留历史沿革。

# 历史状态快照 — CP1 结束时 RC BLOCKED（已被 Issue #3 纠偏）

更新日期：2026-10-21 JST；初始包版本 INIT-1.0（母本与原交付清单保留）。

| 项目 | 当前事实 |
|---|---|
| 项目 | CCB Studio Suite |
| 阶段 | 已实际读取 Issue #1 APPROVED_FOR_RC_RUN 和 Issue #2/R1 PASS_WITH_SCOPE；连续任务启动，CP1独立基础自动化通过（31 JVM+8 TUF tests）；CP2权威核心许可/窄SDK依赖STOP，整体RC BLOCKED |
| 立项文件 | 已整理成分文件包与合并母本 |
| 生产源码 | 单个JDK17 asset-library切片：原始流/不可变关系/事务恢复/索引/最小备份恢复；非完整应用 |
| Windows本地工作区 | H:\CCB-Studio-Suite；feature/windows-rc-continuous；起点27dbd5d578f364afb529fc33944386c0c107f0da，开始时index/worktree/untracked干净；完整继承R1，单执行者所有权记录见RC台账 |
| GitHub远程仓库 | https://github.com/MisakaPiano/CCB-Studio-Suite.git；main仍6d3d2b2，R1保持27dbd5d5；当前连续分支CP1实现9d3035b9已推送/远程核验，Drive附件回读一致；见WINDOWS_RC/DELIVERY.json，整体RC BLOCKED |
| 产品验收/发布 | 无 |
| 真实用户数据 | 未授权读取/迁移 |
| 真实付费API | 未授权 |
| 签名密钥 | 无正式密钥；合成lab签名对象仅内存，未创建/上传私钥文件 |

## 当前任务与恢复入口

[连续任务合同](../tasks/WINDOWS_RC_CONTINUOUS.md)、[checkpoint台账](rc/CHECKPOINTS.md)、[RC门槛状态](rc/RC_READINESS.md)、[依赖STOP及独立目标合同](rc/DEPENDENCY_ESCALATION.md)。当前CP1维护针对性4 PASS，真实TUF合成信任链8 PASS，完整资产模块31 PASS/0失败错误跳过，源码hash一致；旧27 PASS没有代签新版。见[真实执行回执](rc/CP1_EXECUTION.md)。

正式Gate已经实际读取，不再只依据用户转述：#1 comment6296885017允许一次性CP1–CP4，但明确遇到权威核心/许可/跨仓边界时停止受阻切片。CP2尚无被提供的合法窄SDK制品/授权链；不复制Prompt/request/fallback、不写Desktop绕过。独立CP1检查点9d3035b9已推送并远程核验，Drive原始证据已上传且回读hash/25项manifest一致；以RC BLOCKED停止。见[固定交付入口](rc/PROJECT_REVIEW.md)、[双通道回执](audits/evidence/WINDOWS_RC/DELIVERY.json)。没有Windows安装包、真实应用内A→B、凭据迁移或产品人工验收PASS。

## 已知边界

本轮已核验GitHub Desktop desktop=5ef95428421f7c693b010ac88f51d470804ab07a、官方master=979cb505958dffb1b6d64a8de657434f198ffa96；正式Desktop baseline仍1.4.1。初始SOURCE_LOCK观察保留，A0新增观察不批准生产采用。本地Desktop活动feature开始HEAD为3ece1366b142210f2db7c61a9c479cd156534b69，结束由外部活动前进到ba7b58c829fb583c0e22c100f5de8a3799406c20（app/差异空，截图证据入Git）；本会话只读，不能声称其全程状态不变。

编辑前显式Root+AllowExtraFiles校验36/36 PASS；严格模式仅两份原有建项增量EXTRA。TypeSet三文件hash一致，仅静态读。默认Root参数问题已复现并单列精确修复建议，本轮未改工具/校验清单。状态文件合法编辑后原包校验出现6份MISMATCH是预期，未重写原清单。

以上包校验与Desktop来源状态均为原A0历史观察。本轮仅运行Suite最小Gradle模块和合成测试；未运行Desktop构建/模拟器/打包、真实API、真实用户资料、HTML或签名/安装更新。产品验收矩阵仍全部NOT TESTED；A1限定自动测试结果另立执行记录，不代签产品/人工验收。

新项目不改变CCB Desktop活动任务、验收、Prompt或baseline。跨项目候选见CROSS_PROJECT_REGISTRY。

R1交付：[双通道实际回执](audits/evidence/A1_001_R1/DELIVERY.json)。固定实现SHA a9ac47120c6cbea98a7a04140de1071568b9a985；原始附件75,529bytes，Drive文件ID 1kP5IBA419AY0WKNgdN4eUKaiyGh3fabw；hash/原字节回读结果见回执。包含此说明的提交仅记录交付，源码与已验证实现一致。

最近可恢复实现checkpoint：9d3035b98adea32908d4d3e04267127d0d9c25f4，feature/windows-rc-continuous；含本说明的后续提交只补交付元数据，源码/测试/构建输入未改。CP1原始ZIP文件ID 1ElGLErALsdzwzMMJoNhAE33-EPT3rgOT，23,673bytes，SHA-256/原字节回读见回执。整体RC BLOCKED，没有Windows安装包，CP2–CP4未完成。

## Windows Preview resume / Issue #3

Latest Project correction rescinds generalized source-rights / missing-SDK STOP. Same owner resumed from 86760bc0; historical RC BLOCKED evidence remains. Current work is IN PROGRESS toward Preview Gate, with actual 69 authority + 8 Studio core synthetic tests PASS. See [Preview progress](rc/PREVIEW_PROGRESS.md) and [Issue #3](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/3). Real API use is additionally authorized at <=12 attempts/rolling hour and >=300s interval; no real calls at this checkpoint. Not RC/Preview acceptance.
