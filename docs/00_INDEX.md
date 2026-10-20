# 00 — 文档入口

## 每次任务首读

1. [当前状态](CURRENT_STATE.md)
2. [任务登记](TASK_REGISTRY.md)
3. [来源锁](SOURCE_LOCK.json)
4. 本任务合同；当前 [A1-001](../tasks/A1_001_IMPLEMENTATION_CANDIDATE.md)

## 产品与工程合同

| 文件 | 用途 |
|---|---|
| [01章程](01_PROJECT_CHARTER.md) | 定位/项目边界 |
| [02需求](02_PRODUCT_REQUIREMENTS.md) | 平台/模式/首版 |
| [03架构](03_ARCHITECTURE_AND_REUSE.md) | 共享/适配/无环 |
| [04数据](04_DATA_SAFETY.md) | 原图/工程/恢复 |
| [05更新](05_UPDATE_AND_RELEASE.md) | 首版更新/发行 |
| [06服务端](06_SERVER_SECURITY.md) | 队列/撤销/授权 |
| [07来源](07_SOURCE_MAP.md) | 精确入口与证据 |
| [08协作](08_GIT_AND_COLLABORATION.md) | Git/工作区/反哺 |
| [09路线](09_ROADMAP.md) | 切片和退出标准 |
| [10验收](10_ACCEPTANCE_MATRIX.md) | 测试与证据分类 |
| [决定](DECISIONS.md) | 初始约束/待设计 |
| [跨项目](CROSS_PROJECT_REGISTRY.md) | 候选与目标验收 |

## 初始化和交接

[使用入口](../START_HERE_CN.md) · [Project指令](../instructions/PROJECT_INSTRUCTIONS.md) · [Codex开场](../handoff/CODEX_START_MESSAGE.md) · [新Project接手](../handoff/NEW_PROJECT_OPENING.md) · [Desktop协作说明](../handoff/DESKTOP_COLLABORATION_NOTE.md) · [参考文件](../references/README.md) · [打包边界](../PACKAGING_NOTES.md)。

[合并母本](../CCB_STUDIO_SUITE_INIT_20261020.md)用于初始上传/引继，是有日期快照。后续以Git的分文件CURRENT和当前任务维护；不要求同步改写历史母本。

源码未实施前不要因这些文件存在宣称功能PASS。

## A0-001 历史交付（A0-002 已 PASS_WITH_SCOPE）

[精确源码审计](audits/A0_001_REUSE_AUDIT.md) · [依赖、差距与变更回执](audits/A0_001_DEPENDENCY_AND_GAPS.md) · [来源及验证证据](audits/A0_001_SOURCE_EVIDENCE.json) · [A1-001任务（本轮限定授权）](../tasks/A1_001_IMPLEMENTATION_CANDIDATE.md)。A0没有产品实现或测试验收。

[Project审阅交接入口](../handoff/PROJECT_A0_REVIEW.md)：用户在A0交付后追加授权上传Git；原报告中的无commit/push是审计结束时的历史事实，当前交付使用Git实际commit定位。

## A1-001（2026-10-21，实施/合成验证完成，待审查）

用户提供A0-002结论PASS_WITH_SCOPE，允许从6d3d2b2在独立功能分支执行数据切片；实施时授权不含Git提交/推送；用户随后要求远程交付，本次追加Git交付入口见[Project A1审阅交接](../handoff/PROJECT_A1_REVIEW.md)。见[实际实施/21项测试回执](audits/A1_001_EXECUTION.md)、[未提交补丁](audits/evidence/A1_001/source.patch)、[文件状态与SHA清单](audits/evidence/A1_001/WORKING_TREE.json)、[模块合同与运行方式](../modules/asset-library/README.md)、[更新器后续设计](audits/A1_001_UPDATER_DESIGN.md)。A0报告和来源JSON保留原始快照，产品验收矩阵不提前改PASS。
