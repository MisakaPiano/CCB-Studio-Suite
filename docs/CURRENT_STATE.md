# CURRENT STATE — Windows RC 连续任务 / RC BLOCKED（CP2依赖）

更新日期：2026-10-21 JST；初始包版本 INIT-1.0（母本与原交付清单保留）。

| 项目 | 当前事实 |
|---|---|
| 项目 | CCB Studio Suite |
| 阶段 | 已实际读取 Issue #1 APPROVED_FOR_RC_RUN 和 Issue #2/R1 PASS_WITH_SCOPE；连续任务启动，CP1独立基础自动化通过（31 JVM+8 TUF tests）；CP2权威核心许可/窄SDK依赖STOP，整体RC BLOCKED |
| 立项文件 | 已整理成分文件包与合并母本 |
| 生产源码 | 单个JDK17 asset-library切片：原始流/不可变关系/事务恢复/索引/最小备份恢复；非完整应用 |
| Windows本地工作区 | H:\CCB-Studio-Suite；feature/windows-rc-continuous；起点27dbd5d578f364afb529fc33944386c0c107f0da，开始时index/worktree/untracked干净；完整继承R1，单执行者所有权记录见RC台账 |
| GitHub远程仓库 | https://github.com/MisakaPiano/CCB-Studio-Suite.git；main仍6d3d2b2；A1原分支远程3c17b4c9已核验；本轮明确允许R1功能分支提交/推送及指定Drive附件上传；实际结果见 R1 DELIVERY.json；后续仅补交付回执，未改实现 |
| 产品验收/发布 | 无 |
| 真实用户数据 | 未授权读取/迁移 |
| 真实付费API | 未授权 |
| 签名密钥 | 无正式密钥；合成lab签名对象仅内存，未创建/上传私钥文件 |

## 当前任务与恢复入口

[连续任务合同](../tasks/WINDOWS_RC_CONTINUOUS.md)、[checkpoint台账](rc/CHECKPOINTS.md)、[RC门槛状态](rc/RC_READINESS.md)、[依赖STOP及独立目标合同](rc/DEPENDENCY_ESCALATION.md)。当前CP1维护针对性4 PASS，真实TUF合成信任链8 PASS，完整资产模块31 PASS/0失败错误跳过，源码hash一致；旧27 PASS没有代签新版。见[真实执行回执](rc/CP1_EXECUTION.md)。

正式Gate已经实际读取，不再只依据用户转述：#1 comment6296885017允许一次性CP1–CP4，但明确遇到权威核心/许可/跨仓边界时停止受阻切片。CP2尚无被提供的合法窄SDK制品/授权链；不复制Prompt/request/fallback、不写Desktop绕过。先完成独立CP1检查点及远程交付，再以RC BLOCKED停止。没有Windows安装包、真实应用内A→B、凭据迁移或产品人工验收PASS。

## 已知边界

本轮已核验GitHub Desktop desktop=5ef95428421f7c693b010ac88f51d470804ab07a、官方master=979cb505958dffb1b6d64a8de657434f198ffa96；正式Desktop baseline仍1.4.1。初始SOURCE_LOCK观察保留，A0新增观察不批准生产采用。本地Desktop活动feature开始HEAD为3ece1366b142210f2db7c61a9c479cd156534b69，结束由外部活动前进到ba7b58c829fb583c0e22c100f5de8a3799406c20（app/差异空，截图证据入Git）；本会话只读，不能声称其全程状态不变。

编辑前显式Root+AllowExtraFiles校验36/36 PASS；严格模式仅两份原有建项增量EXTRA。TypeSet三文件hash一致，仅静态读。默认Root参数问题已复现并单列精确修复建议，本轮未改工具/校验清单。状态文件合法编辑后原包校验出现6份MISMATCH是预期，未重写原清单。

以上包校验与Desktop来源状态均为原A0历史观察。本轮仅运行Suite最小Gradle模块和合成测试；未运行Desktop构建/模拟器/打包、真实API、真实用户资料、HTML或签名/安装更新。产品验收矩阵仍全部NOT TESTED；A1限定自动测试结果另立执行记录，不代签产品/人工验收。

新项目不改变CCB Desktop活动任务、验收、Prompt或baseline。跨项目候选见CROSS_PROJECT_REGISTRY。

R1交付：[双通道实际回执](audits/evidence/A1_001_R1/DELIVERY.json)。固定实现SHA a9ac47120c6cbea98a7a04140de1071568b9a985；原始附件75,529bytes，Drive文件ID 1kP5IBA419AY0WKNgdN4eUKaiyGh3fabw；hash/原字节回读结果见回执。包含此说明的提交仅记录交付，源码与已验证实现一致。
