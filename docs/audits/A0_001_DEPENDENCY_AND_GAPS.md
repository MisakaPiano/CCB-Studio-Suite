# A0-001 — 依赖、差距与交付回执

日期：2026-10-21 JST。**审计交付，等待 Project review；以下方案均为候选**。精确 repo/SHA/path/symbol 见 [源码审计](A0_001_REUSE_AUDIT.md)，可复查文件身份见 [证据清单](A0_001_SOURCE_EVIDENCE.json)。

## 最小依赖图与版本

```text
Windows app ─┬→ Suite 领域 / 资产端口 / 工程合同
Android app ─┤       ↑ 平台存储与渲染适配
Server ──────┘
生成适配 → 固定版本 CCB 图像核心（先独立来源抽取/发布审查）
Windows / Android 渲染 → 通用编辑操作模块 ← 将来 Desktop 适配
```

Suite 与完整 Desktop 应用间不建立构建依赖；Server 不依赖 UI/Windows/Android 模块。没有真实消费者前不创建空模块。

| 项目 | 来源快照 / 建议 | 采用状态 |
|---|---|---|
| CCB 图像语义 | Desktop D 精确对象，包含其明确官方移植例外；官方 O 仍是其正式 1.4.1 baseline | 审计候选，无制品/源码已引入 |
| JVM/Windows | JDK17、Kotlin2.3.20、Compose1.10.3，来自 D 构建文件 | A1 最小 JVM 可用此组候选；本机 JDK/Gradle 未运行或验证 |
| 核心库 | coroutines1.10.2、serialization1.8.1、OkHttp4.12.0、msgpack0.9.12 | 来源版本，不宣称最新/已安装 |
| Android | D minSdk26、compile/target36、AGP9.0.1；O Manifest com.example.chatbar | 建议 min26 起做兼容 spike，用户目标设备/API 未核实，不冻结永久最低版本 |
| 应用身份 | Android 候选 `io.github.misakapiano.ccbstudiosuite`；Windows display/package 候选 `CCBStudioSuite`，安装版数据根候选 `%LOCALAPPDATA%\CCBStudioSuite` | 与已读 CCB `com.example.chatbar`、`ChatChatBarDesktop` 不同；未做商店/全机冲突查询、Publisher/签名未定，不是已注册身份 |
| 业务索引 | SQLite + 不可变文件/版本化文档/提交日志 | 选型推荐；driver/Room 等不在 A0 冻结，Android 原生SQLite与Windows JDBC需独立适配 |
| 更新 | Windows 安装版 MSIX 优先 spike；可信元数据用成熟框架候选 TUF | 没有库/发布源/证书获准，不等于可发布 |
| 本地 Composite Build | 开发可用独立精确只读来源副本；发行须固定制品或重现来源 | 不得 include 活动 `H:\ChatChatBar-Desktop`。依据 [Gradle 文档](https://docs.gradle.org/current/userguide/composite_builds.html)；本次无 reference repo 副本 |

## 有优先级的差距清单

| ID | 源码/需求差距 | 推荐处置与最小测试 | 阻塞范围 / 下一所有者 |
|---|---|---|---|
| GAP-01 | sharedCore 可复用但图像DTO/Designer依赖 ChatMessage、CharacterCard、ModelConfig、JsonFileStorage；未发布窄SDK | Desktop 单独合同给出 import闭包、无语义漂移抽取/制品与Prompt/request黄金样本等价性；Suite不复制 | 生成集成采用；独立 Desktop review，非本轮阻塞 |
| GAP-02 | 原始/合成/Guidance未按 Suite 资产层分离；D runtime只将compose后bytes存入history | 原始bytes先落盘+hash验证；每个派生结果指向原资产；JPEG/PNG/动画输入不重编码原件 | 首版 DATA-01/09；A1-001 |
| GAP-03 | 单文件原子替换不等于跨文件/索引事务；loadAll可跳过corrupt | 提交日志+strict scan+启动reconcile；逐提交点崩溃、空间/权限失败、不确定提交可重入 | DATA-03/04；A1-001 |
| GAP-04 | 完整工程/AI设计历史没有独立耐久/未知字段保留合同；旧history可100条prune | 版本化工程文档保存原始未知bytes；只读拒绝未知新版本；明确保留策略，禁止自动裁作品；重启/损坏测试 | DATA-02；A1后续工程切片 |
| GAP-05 | 原图/工程/Recipe/设计过程/索引/凭据不能用同一备份机制 | 流式hash manifest、新根恢复、校验后维护切换；普通导出排除secret；中断恢复保留原根 | DATA-05/06；A1后续备份切片 |
| GAP-06 | D-044是需求，NAI单slot，多模型≠多账户 | stable account/profile+SecretRef+有效解析快照；旧单Key迁移到默认profile可回退，错误不覆盖；账户A→B切换不影响A已启动任务；fallback黄金测试 | GEN-03/05；A2档案合同 |
| GAP-07 | Android credential load 将解密错误折叠成null；U明文backup API不适合Suite | 新适配显式Missing/Locked/Corrupt/ReadError，保护旧密文，不自动创建替代值；合成provider测试后真实Keystore另验 | Android凭据接入；C1 |
| GAP-08 | continuous 429可无限等，已发送任务没有持久“不确定”归属 | 不改请求权威，外围预算/状态机限制；取消发送前原子协调，发送后未知不重付费；mock fault测试 | GEN-04/NET-01—03；A2/B1 |
| GAP-09 | Server 没有实现：管理员集中token、用户隔离、持久队列、公平调度/预算/幂等/撤销 | Studio身份提交完整Recipe+资产ID；派发冻结管理员执行账号；mock上游、双用户/双设备、重启、发送竞争测试 | B1/B2；不要求客户端BYOK，不改产品要求 |
| GAP-10 | 跨端Recipe含本地path、没有协议资产身份/版本冲突合同 | 资产ID+hash上传，独立验证大小/数量/解码预算、授权与幂等；拒绝任意path/Endpoint；冲突测试 | NET/SEC/COMP；B1/C1 |
| GAP-11 | 完整可编辑图层/操作/批量撤销尚缺，旧平台Bitmap undo不适合长期工程 | 纯操作合同+平台renderer；多选目标快照/批量一撤销单位；缺字体保留内容并报告 | EDIT-01—03；A3 |
| GAP-12 | metadata保留/容器去除/隐私烧录是不同能力，原图签名不能沿用到改像素结果 | 普通导出保留来源标识；隐私输出重渲染与隐写/隐藏内容预算检查；不伪造原图签名，不宣传绝对匿名 | EDIT-04；A3，真实样本另授权 |
| GAP-13 | Desktop updater仍P15 PENDING；官方Android安装器没有Suite完整可信链/排水/完成确认 | 安装版MSIX spike+成熟可信元数据框架评估；内部A→B真实升级链、错包/签名/渠道/架构/过期/rollback/freeze/空间/安装失败/启动失败测试 | UPD-01—05；A1更新切片，首个长期使用版阻塞 |
| GAP-14 | 数据版本、程序回退与更新后新增作品未有合同 | 优先兼容；迁移先快照/预算+提交标识；旧版拒写未知新格式，不能通过回退旧快照删除新作品 | 首版；A1更新/迁移切片 |
| GAP-15 | 根/命名空间/品牌/应用ID/渠道尚未冻结 | 独立应用身份，Suite不接Desktop根；用户设备矩阵和冲突检查，发行前冻结签名与渠道 | 数据接入/发行；A1/C1/发布任务 |
| GAP-16 | 代码/TypeSet/字体/品牌/资源复用许可范围不齐 | 分文件权利依据与notice清单；不要给混合来源套统一LICENSE；需要时单独目标授权核对 | 对应公开源码复制/分发；不阻塞本轮只读审计或独立合成数据底座 |
| GAP-17 | U相对O触及221文件，未完整逐项审查；feature未整合 | 保留新观察，按具体集成任务选最小diff和回归，不升级baseline；format-repair/Fish/截图增量不导入 | 选定新能力时；sync候选 |
| GAP-18 | Verify-Package默认Root参数失败 | 单独修参数求值时机；保留原manifest；明确已知失败不阻塞A0 | 工具维护候选，非A1前置 |

## 持久化选型比较与建议

| 方案 | 优势 | 缺口/成本 |
|---|---|---|
| 全JSON+文件 | 易检查/交换，小型原型简单 | 跨对象事务、扫描/检索、并发和恢复成本大；permissive loader风险明显 |
| 全数据库含图像BLOB | 关系事务集中 | 大原图流式备份/恢复、迁移、人工救援成本；数据库损坏影响唯一副本 |
| 不可变原始文件+版本文档+SQLite索引+日志 | 原图可独立救援；工程/Recipe可交换；索引可重建 | 需明确跨文件/DB提交协议与故障注入；推荐该组合，不声称天然原子 |

首次切片不必引入SQLite：先以明确manifest/journal证明原始文件和关系提交，再加入查询索引作非权威派生物。后续索引从提交文档重建；Prompt、编辑层、任务来源不能只存可丢的索引。规模用合成1/100/1000资产覆盖正确性和有界内存；实际性能阈值待测量，不能编造容量/速度保证。

推荐提交边界：根所有权→写事务意图→临时原始bytes并核hash→发布唯一资产→提交关系文档→更新可重建索引→标记完成；失败/重启扫描分清未完成、已收到未登记、引用缺失/损坏、已发布但索引滞后，保留可恢复文件。最终协议和flush保证在实施中设计审查，A0不冻结schema。

## TypeSet Forge 1.1 能力盘点（静态证据）

统一身份：`.reference-assets/typeset-forge-1.1/TypesetForge_1.1.html`，200005 bytes，SHA256 `892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63`；README/CHANGELOG hash仍与参考manifest一致。以下是可见实现，不是功能PASS。

| 能力 | 精确入口 | Suite建议范围 |
|---|---|---|
| 多图队列/多选/拖序/批量 | `moveSelectedEntriesBefore:1810`、`getSelectedEntriesInQueueOrder:4469`、`buildBatchExportJobs:4227` | 首版必要，原生交互重做；操作锁定目标集合/顺序 |
| 工作区保存/交换 | `serializeEntryForWorkspace:1016`保留sourceBlob及texts/masks；`serializeCurrentWorkspace:1033`；IndexedDB `openWorkspaceDB:872`；JSON import/export1067/1093 | 借鉴对象分离，浏览器持久化不移植；包内原图bytes是否完整仍需实际测试 |
| 横排文字/图层/样式 | `parseRichTextTokens:2636`、`drawHorizontalLine:3356`、`drawText:3458` | 首版可编辑横排、层次、基本样式；混合字体/富文本建议后续增强，需Project确认该分期，保留未知内容 |
| 竖排/标点 | `verticalTokenizeStyled:3108`、`verticalColumnsStyled:3250`、`drawVerticalGlyph:3392` | 首版中日文基础竖排；复杂禁则/组合/专业DTP shaping后续独立验收 |
| 矩形/画笔/圈选打码 | `buildContinuousBrushMaskCanvas:3568`、`drawBrushMask:3610`、`drawLassoMask:3669`、`drawMask:3698` | 首版矩形/画笔/圈选+遮盖/马赛克；模糊作能力候选，不能夸称绝对不可逆 |
| 撤销/重做 | `pushWorkspaceHistory:1649`最多80快照；`pushHistory:1688`局部100；`undo:1700/redo:1709` | 借鉴工作区事务；原生命令与持久工程独立，不能把undo窗口当作品保留限制 |
| metadata | `extractNaiStealthFromImage`、`injectNaiStealthIntoCanvas:2081`、`buildEntryVariantBlob:4179` | 普通保留/隐私导出分开；隐写注入不是原图真实性证明 |
| 四类导出 | `EXPORT_VARIANT_INFO:4148`、`drawEntryVariantToCanvas:4163`、`buildEntryVariantBlob:4179` | 原始bytes导出独立命名；四类渲染批量可作首版输出候选，碰撞/失败不得覆盖已有结果 |
| “原始排序” | `buildEntryVariantBlob:4193–4222`仍drawImage+canvasToBlob，返回rendered-original | **不是原始bytes导出**；不可作为Suite原图保全机制 |
| 小人书/可变页高/PDF | `drawStorybookPage:3784`、`getJsPDF:4425`、PDF按钮4428，JPEG raster页加入jsPDF | 后续增强；不扩大首版，不承诺可编辑PDF或矢量文字 |
| crop/resize/rotate | 静态搜索命中主要为预览scale、文本框resize和竖排glyph旋转；未定位通用整图几何工程入口 | **未证明具备**通用裁切/缩放/旋转；用D平台几何经验+Suite新工程合同独立实现 |
| 字体/外部库 | HTML7–9 Google Fonts；fontUpload463；`getJSZip:4282` CDN 3.10.1；`getJsPDF:4425` CDN 2.5.2 | 未加载/下载；原生字体打包、缺字体fallback与字库许可另查；无CDN运行依赖移植 |

## 本轮文件变更

| 类型 | 精确路径 |
|---|---|
| 新增交付 | `docs/audits/A0_001_REUSE_AUDIT.md` |
| 新增交付 | `docs/audits/A0_001_DEPENDENCY_AND_GAPS.md` |
| 新增交付 | `docs/audits/A0_001_SOURCE_EVIDENCE.json`（来源身份/摘录范围/初始文件hash/验证事实；无源码或秘密副本） |
| 新增候选 | `tasks/A1_001_IMPLEMENTATION_CANDIDATE.md` |
| 最小状态维护 | `docs/00_INDEX.md`、`docs/CURRENT_STATE.md`、`docs/TASK_REGISTRY.md`、`docs/SOURCE_LOCK.json`、`docs/CROSS_PROJECT_REGISTRY.md`、`tasks/A0_001_BOOTSTRAP_AND_AUDIT.md` |
| 本地Git元数据 | 新建 `.git/`，main/origin；空index，没有commit |
| 忽略的审计工作文件 | `.local/a0_read.py`、`.local/a0_source_reads.jsonl`、`.local/a0_initial_files.json`、`.local/a0_finalize.py`；仅源码读取/校验辅助，不是生产模块，不提交 |

其余原始交付、两份用户增量说明、TypeSet原件、母本、Verify脚本和BUNDLE/SHA清单保留。因本仓库是unborn，`git status`把全部未忽略文件都列为untracked；上述变更集合以编辑前hash快照比对区分，不能把所有untracked说成本轮新增。

## 验证回执

| 检查 | 真实结果/局限 |
|---|---|
| 编辑前原始包 | 严格模式 FAIL：两份已存在增量文件 EXTRA；显式Root+AllowExtraFiles **36/36 PASS** |
| 默认Root | Windows PowerShell **FAIL已复现**；已报告独立精确补丁，未修改脚本 |
| 原始清单保护 | 最终按初始hash比对：仅上表6个受清单覆盖状态文档变化，其他34个初始文件未变；不更新原始hash使其绿灯 |
| 最终JSON/相对链接 | 全部项目JSON可解析；Markdown本地文件目标检查通过（跳过代码块/远程URL；不冒充外站链接可用性验证） |
| Git忽略/index | 三份参考、reference repos、local/runtime/secrets/data/backups/keys哨兵路径被ignore；index为空；无私密参考已跟踪 |
| 空白检查 | unborn下`git diff --check`本身为空操作；另对10个交付变更文件逐文件检查行尾空格/冲突标记，并以临时空文件`git diff --no-index --check`检查文本；通过后才记录PASS |
| 敏感内容范围 | 仅扫描本次10个变更交付文件的私钥/常见token形态和冲突标记，无命中；不是全盘secret认证；不读真实数据/SecretStore/浏览器缓存 |
| 外部项目 | 结束Desktop外部HEAD前进到ba7b58c829fb583c0e22c100f5de8a3799406c20，截图证据目录由untracked进入提交；其他附件/交接文件仍untracked，tracked/index diff空。3ece→ba7的app/差异空；GitHub desktop仍D。初查未记录本地desktop ref，结束值4c2af2576e2319e8041ffd2a6c5a205450f18246不作变化推断。本会话仅只读外部，不能宣称外部状态全程不变 |
| 产品能力 | Gradle/编译/自动测试/模拟器/打包/更新安装/真实供应商/人工验收均 **NOT RUN / NOT TESTED** |

实际检查命令与最终结果还记录在来源证据JSON的validation字段；失败不会改名PASS。A0没有产品版本、提交SHA、PR或发布物。

## 未决与具体下一动作

本轮文档审计无阻塞；公开源码/资源采用受GAP-16约束，实际供应商/设备/安装链等保留NOT TESTED。首项建议是 [A1-001独立数据根与原始资产提交/恢复](../../tasks/A1_001_IMPLEMENTATION_CANDIDATE.md)，只作为可审查候选，既不替代整个A1阶段，也不降低首版完整更新门槛。

**现在停止，交 Project 执行 A0-002：审阅上述三份证据及A1候选，决定采用边界与下一项合同。** 本轮不自动首次commit/push、不运行A1、不修改Desktop登记、不发布。
