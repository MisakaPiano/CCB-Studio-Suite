# Project复审原文：134dd47交付（证据，不是额外执行授权）

线程：6adcaaef-3fac-83e8-ae57-2a524281e049，turn：872b3152-fea1-4550-b864-660881dfa87e，消息：a061708d-1429-5608-b809-9a211e4cbaff。

## 本轮结论：四个残余限定关闭，下载清理仍有一个 P2；旧凭据快照补证接受

本结论只适用于**源码 `2484858`、固定交付 `134dd47`**。原第 1／3／4 项的相关残余本轮可以限定关闭；原第 2／5／6 项沿用已有范围。**新增自动清理机制尚不能整体通过，PRODUCT_HOLD / NOT RC 保持。**公开 115 仍是 `ff145d0`，不因内部源码修复而获得新版通过记录。

已实际检查生产差异、原始 XML／日志、ZIP、源码输入映射，并用隔离合成数据复现清理与入口提交决策；没有修改仓库、发布、信任链或日常资料。

### 五个残余及凭据补证的裁定

| 项目 | 实现与证据判断 | 独立裁定 |
|---|---|---|
| 当前 B 首次接管未知 `Uninstall.exe` | 接管现在绑定旧 journal 的 manifest，不再追认未知卸载器。原始测试及独立 B→C 合成复现均保留未知文件。[源码 168–174 行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/residue_cleanup.py#L168-L174) | **限定关闭，原 P1 已修** |
| 下载退役与未完成交接引用 | 旧成功误删未来候选、较新失败输出等原反例已修；但不完整终态的跨根组合仍可误删下载，详见下文。 | **OPEN／P2** |
| helper 默认入口提交 | 共享锁内比较期望旧入口、版本、资料及 runtime 根；独立探针确认六种拒绝均发生在入口文件修改之前，正常升级和明确迁移可以提交。[源码 24–64 行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/apply_update.py#L24-L64) | **限定关闭** |
| VIBE 维护门 | 每次 admission 与 HTTP 接受点均接入发送门；关闭门先于显示 maintenance。双未缓存参考的测试保留第一条响应缓存，第二条编码和后续生图零发送。[接线](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioApplication.kt#L83-L90)／[维护排水](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioApplication.kt#L448-L460) | **限定关闭** |
| 同 ID 保存策略保留冻结账户 | 实际 UI 仍调用同一核心配置方法；新分支保留既有快照，换 ID 才重新绑定。保存策略、重开和显式换账户均有执行证据。[源码 84–93 行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/BatchGeneration.kt#L84-L93) | **限定关闭** |
| 冻结旧凭据快照 | 当前档案与冻结账户的 ref 并集已纳入。原生正例的 A／plan／B 完整快照一致，含两份合成凭据；JVM 负例确认升级前可读、验证时缺失的旧 ref 会被拒绝。[快照实现](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioUpdateHandoff.kt#L33-L49) | **补证项关闭** |

## 必须修复：P2——非默认来源的不完整终态仍可能释放下载引用

**位置：**[`residue_cleanup.py:124–142`](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/residue_cleanup.py#L124-L142)，后续删除判断在 [208–213 行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/residue_cleanup.py#L208-L213)。

代码先凭 `COMPLETE`、正确 nonce、verification 三个标记认定 `verified_complete`；随后遇到非默认程序根便提前 `continue`，**尚未核验 installed、installJournal 是否齐全、journal 是否存在及身份是否匹配**。该引用因此没有进入保留集合。`FAILED_RETAINED` 也未绑定 plan nonce。

独立合成复现使用实际字节与证明散列一致的安装器：

1. 同一 cache 已有完整 A→B 成功记录及匹配下载。
2. 另一个程序根的交接 plan 仍引用该下载。
3. 将其回执设为下列状态，再执行冻结源码的清理决策。

| 非默认来源回执 | 实际结果 |
|---|---|
| `READY_TO_DRAIN`，或只有 `status=COMPLETE` | 正确保留 |
| 三个完成标记正确，但缺 installed／journal | **删除安装器和 VERIFIED.json，共 2 文件** |
| 三个标记正确，但 journal 不存在 | **删除 2 文件** |
| `FAILED_RETAINED`，nonce 错误 | **删除 2 文件** |

现有[测试 175–178 行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/test_residue_cleanup.py#L175-L178)只覆盖缺少完成标记的情况，没有覆盖上述组合。

**影响限定：**可能提前清掉仍应保留的下载，导致更新失败或重新下载；本次资料哨兵未变，没有证据证明资料或凭据丢失。正常 helper 会原子写完整回执，不能把这个异常记录边界描述成日常事故。但它确实未满足本轮明确要求的“非默认来源、不完整 COMPLETE、未验证终态引用优先保留”，因此仍须修复。独立探针仅适配了 Windows 锁／删除原语，未冒充新一轮 Windows 原生更新测试。

## 证据、自动测试与人工状态

**证据身份可以接受。**独立核对了 408 项应用输入在 `59283a0 → 2484858 → 134dd47` 间一致；原始 ZIP 的大小和 SHA 与交接一致，所有成员与固定 Git 原字节匹配。计数有一个非阻塞更正：实际是 **126 条清单文件＋manifest＝127 个 ZIP 成员**，包含 `.gitattributes`；无需因此改包或重跑。[固定映射](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/134dd47ae86a4ab5dbef579b61e19458dece2579/docs/audits/evidence/REVIEW_RESIDUALS_20261025/FIXED_SOURCE_PACKAGE_MAP.json)／[原始清单](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/134dd47ae86a4ab5dbef579b61e19458dece2579/docs/audits/evidence/REVIEW_RESIDUALS_20261025/RAW_MANIFEST.json)

- **自动测试：**最终确为 **18 个唯一 JVM、56 个唯一 Python**，分别零失败／错误／跳过；不累加首轮、上轮或本审查探针。它们是定向执行记录，不能覆盖未选入的测试。[运行索引](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/134dd47ae86a4ab5dbef579b61e19458dece2579/docs/audits/evidence/REVIEW_RESIDUALS_20261025/RUN_INDEX.json)
- **原生演练：**最终 driver PASS 有完整链支持：真实 A PID／映像句柄、生产维护与 handoff、回环 TUF／NSIS／B、3 资产＋2 合成 DPAPI 凭据匹配、所属清理、未知哨兵保留及 B 正常退出。四角色签名及元数据长度／散列链已独立验证。仍限于 **test-only A main**，不是原封 115 或真人完整 GUI 更新。[最终记录](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/134dd47ae86a4ab5dbef579b61e19458dece2579/docs/audits/evidence/REVIEW_RESIDUALS_20261025/native-final/NATIVE_UPDATE_SUMMARY.json)
- **失败保留：**本轮首个 native 的 WinError32／FAILED、上轮 FAILED 及旧证据原字节均保留。最终独立成功演练不改写它们。[本轮原始失败](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/134dd47ae86a4ab5dbef579b61e19458dece2579/docs/audits/evidence/REVIEW_RESIDUALS_20261025/native-first-failed/DRIVER_FAILURE_FROM_ORIGINAL_TMP.json)
- **用户手动：**没有新增人工验收。115 命名归档／组织树／任务栏体验继续等反馈，Prompt 比较继续 **PENDING**。
- **仍未验证：**内部 EXE 字节未提供给本环境，**独立包 SHA 复算仍为 UNKNOWN**；签名目标与构建回执相符不能替代它。真实服务、私人凭据、原生缺旧 ref 启动负例、跨用户 DPAPI、完整 Windows 灾难矩阵及长时资源表现亦未代签。

实时公开回读仍是 [Preview115](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.115)、`ff145d0`、预发布身份；元数据分支仍在 `60c527f5`。

## 清理记录与后续事项

**历史人工清理可接受为已授权操作的证据记录。**我解压核对逐文件计划与删除日志：20 根、236,530 条唯一删除记录、8,506,611,341 字节完全相符；保留清单为 1,086 项，12 个锚点在前后记录中一致，未发现计划越过所列保护范围。这是记录一致性审查，未在真实 Windows 文件系统重新检查存在性，也不是私人资料全量散列保证；不能拿它替新自动清理器验收。[清理回执及原始证据入口](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/134dd47ae86a4ab5dbef579b61e19458dece2579/docs/audits/HISTORICAL_CLEANUP_EXECUTED_20261025.md)

后续分三类：

- **必须修复：**只修上述下载引用的终态验证缺口，再定向复审这一项。
- **需要补证：**若要把结论扩展到实际内部安装包或发布，应提供可独立读取并复算的 EXE；本次源码接受不冒充包验收。
- **单列后续定位：**`q09CanvasRotateUndoMaskResetApplyAndReopen` 仍为 **FAILED，根因 UNKNOWN**，断言在 [`GuidanceSafetyTest.kt:39`](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/GuidanceSafetyTest.kt#L39)。它未被修复或重跑通过，不应因最终 18 项绿色而消失，也不在本轮重开整项 UI 开发。

既有人工导入／取消、恢复第二窗口、限定真实升级及 107 更新确认，无需为本轮重复。Desktop 报告保留为参考交接，不构成 Desktop 实施或验收许可。

### 可直接交回原 Codex 的定向任务

> 请仅修复 `2484858` 的 `residue_cleanup.completed_updates` 下载引用终态分类：在筛选程序根之前，核验全 cache 引用的终态。非默认程序根不能跳过下载引用保护；COMPLETE 必须绑定 plan nonce、必要字段和可核验的 journal／目标身份，FAILED_RETAINED 也须绑定本 plan nonce。不能证明终态的引用继续保留。补三个最小合成反例：正确完成标记但缺 journal、journal 不存在、FAILED nonce 错误；同时确认合法终态后仍可立即清理，不恢复 7 天或额外冷启动政策。提交最小差异、唯一计数和原始日志，重新冻结受影响源码／包身份；若未重打包，明确旧包不含该修复。保留所有 FAILED、q09、人工 PENDING 和 PRODUCT_HOLD / NOT RC；不重开 UI、不重做既有人工、不追加历史目录删除、不自动发布。

