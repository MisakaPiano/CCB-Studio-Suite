# A1-001 — 独立数据根与原始资产提交/恢复（实施候选）

日期：2026-10-21 JST。状态：**PROPOSED / NOT AUTHORIZED / NOT STARTED**。由A0审计提出，须A0-002 Project review明确批准后才能执行。不是完整A1阶段、Windows首版或更新功能已通过。

## 问题与目标

Desktop运行时保存的是composeResult后图像；旧存储的单文件原子替换和permissive loadAll也不足以证明Suite资产耐久性。建立只接收合成bytes的最小JVM垂直切片：独立数据根→导入不可变原件→验证size/hash→提交资产与最小Recipe/工程关联→退出重开→故障恢复。原始文件保留逐字节；失败不能把corrupt当missing并覆盖。

## 精确起点与来源

Suite当前main无commit/HEAD，index为空；不能凭空填写基线SHA。实施前重新读取CURRENT/TASK/AGENTS/SOURCE_LOCK与A0报告，核对当时实际HEAD/branch/index/untracked，登记实际起点。若此前已获单独发布任务并提交，用其真实commit，不执行本候选来偷偷首次提交。

本任务独立实现Suite数据职责；D=`MisakaPiano/ChatChatBar-Desktop@5ef95428421f7c693b010ac88f51d470804ab07a`仅为`JsonFileStorage` strict读、`DesktopDataRootOwnership`、`DesktopDataOperationCoordinator`的参考。**不复制Prompt/NAI请求、不引入整个sharedCore或邻接工作树**，公开复用许可未齐全不阻塞独立合成底座。遇需复制来源代码先补可审查范围与许可，不能自行扩大任务。

## 允许路径与最小范围

- 根`settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`gradle/`、Gradle wrapper文件：仅最小JVM构建和可验证wrapper/依赖锁；检查本机可用缓存和JDK17，记录来源、版本、下载量，不改Desktop构建/全局环境。
- `modules/asset-library/**`：一个真实消费者的原始资产仓库、提交记录/恢复、typed结果及测试；先不建全部规划模块。首轮可不引入SQLite；以后加索引必须可重建。
- `platform/windows/**`：仅确需的根所有权/文件IO适配及小范围进程测试；否则留在上述JVM切片，不预建平台空模块。
- `tools/**`：仅必要合成fixture/故障子进程入口；`docs/audits/**`与`docs/DECISIONS.md`：本任务设计和真实执行证据；CURRENT/TASK/索引更新事实。
- `.local/a1-001/**`：隔离合成根/故障样本/测试临时目录。无真实用户资料。

建议构建版本起点JDK17/Kotlin2.3.20，来源D；只选最小需要的serialization/coroutines版本，若不同写清理由，不凭A0版本表宣称兼容。依赖/Wrappers下载若环境受限，记录限制；不能启动Desktop重构建或删除缓存。

## 提交与恢复合同（设计候选，实施时说明精确提交点）

1. 显式根选择，Suite命名空间，拒绝Desktop/官方数据根、符号链接/路径穿越；持有合作单写所有权；第二进程安全拒绝。
2. 原件输入是bytes/受控流，不先decode。写独立临时文件，记录hash/size和事务意图，再发布唯一原始资产、提交关系文档；提交成功才向调用者报告Saved。
3. 编辑/导出只生成派生物。去重可复用已验证内容，但两个来源/关联不丢；同名异bytes不覆盖；同hash内容异常须显式Corrupt，不能静默替换。
4. 事务日志可重入。启动扫描区分未完成、已发布未登记、关系已提交但索引滞后；故障前已收到图像保留在可解释恢复路径；不自动清理无法证明归属的文件。
5. Missing/Corrupt/ReadError/UnsupportedVersion分开；未知版本/额外字段的原bytes不变。索引不是唯一权威；坏索引不删除原图。
6. 根维护停止接纳新写入并等待已登记IO；排水失败保留所有权/根和可见错误，不强杀，不回退默认空根。

schema名称/字段和具体fsync/fallback语义由实现说明，评审时对照上面行为；任何弱保证明确披露，不宣称任意断电无损。

## 验收与必要测试

| ID | 可验收行为 |
|---|---|
| A1-D01 | 合成PNG/JPEG/任意可保管bytes导入后逐字节及size/hash一致；多次重启仍一致；派生操作后原件不变 |
| A1-D02 | 重复输入、同名不同内容、重复逻辑提交、并发关联不产生覆盖/串线；idempotency边界明确 |
| A1-D03 | 意图写后、原件发布后、文档提交前后、可重建索引更新前后逐点故障注入；重启给出稳定结果，原图/关系不静默丢失 |
| A1-D04 | 空间/权限错误、截断写、读失败、坏hash、损坏文档、未知新格式、丢失索引均显式报告，旧bytes不被默认值覆盖 |
| A1-D05 | 进程级双开被拒绝；正常关闭释放锁，排水超时不释放仍在用的根；不访问Desktop真实根 |
| A1-D06 | 小型/100/1000合成资产恢复与重建结果一致，有界流式读写；报告实测资源，暂不编造性能SLA |
| A1-D07 | 最小备份/恢复验证可只用合成根复制与manifest核对，明确尚非最终用户备份UI；含恢复中断保护，secret默认排除 |

只跑此模块unit tests与必要的独立小型进程故障测试；记录源码SHA/未提交diff身份、JDK/OS、测试数量、失败/跳过及原始证据。Gradle编译与tests实际执行分开。无真实API，无全量Desktop/Android构建，不做产品GUI/安装打包；本任务不承诺人工验收。

## 更新责任与下一独立切片

本任务另输出更新器设计记录：评估安装版MSIX与可信元数据成熟框架、程序/数据路径、identity/channel、排水与重启确认接口，列出尚需的真实A→B实验。**不创建签名密钥、不做系统安装、不自写更新协议、不把文件故障harness叫完整应用内更新。**

后续A1-002候选再单独批准完整更新垂直原型：选定库/版本/可信根与密钥保管、内部A/B包、实际安装链、坏包/过期/回滚/冻结/取消/安装失败/启动失败和资料保全。第一份长期使用Windows版本之前仍必须通过docs/05全门槛。

## 禁止变化与停止点

不写生产创作/生成/Server/Android UI；不导入NAI凭据/真实Prompt/图库；不改官方Prompt/request/schema/fallback；不改Desktop任何文件/分支；不开放网络端口、不真实付费、不删缓存/历史包；不创建密钥/远程/发布，不commit/push/merge（除非该轮另有用户明确授权）。不为恢复测试读取真实数据。

最小切片与必要证据、更新设计记录完成即停，回报实际路径/changed files/真实Git状态/验证/不足，等待Project审查。数据格式涉及已保存数据丢失风险或超出允许范围时，先保全并提交精确差异；不能擅自扩成整套应用。
