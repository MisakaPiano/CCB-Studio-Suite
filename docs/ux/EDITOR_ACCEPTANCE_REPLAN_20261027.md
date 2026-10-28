## 2026-10-28 最新优先顺序与Desktop交接完成

用户反馈最新编辑候选“依然有很多问题”，改为USER_REVIEW_ISSUES_REPORTED；等待详细反馈，暂停UI改动，不把工程／界面签收或新对话迁移标成完成。顺序：①最新更新安装技术共享给Desktop／Project；②内置默认识图模型；③再回UI。沿现有任务登记，无重复开工。

第一项已完成：仅三份交接文档提交／推送e74f7a2769595259a32d546e165c1f00107a895e，固定报告handoff/DESKTOP_UPDATE_TECHNICAL_HANDOFF_20261028.md，公开回读PASS并按用户要求发给Desktop Codex“继续完成 CCB Desktop Phase 7”。分清118源码a1202e2／映像3d0bd1f／固定包3540be44、续期元数据c1ba180 targets17/snapshot27/timestamp27；保留原失败／独立范围／MSI差异。未提交本地编辑源码、未发布安装器或改日常安装／Desktop，未因共享授予Desktop新开发权限。

第二项进入本地实施：固定原作者模型卡MIT声明及LFS散列，程序内只读资源默认加载，工作区只保存身份／设置，不逐工程复制47.6MB权重；仍允许用户自行选择模型。不是新模型准确率验收，未生成／发布新发行安装器，源资源与默认行为的自动核验进行中。原图／工程／备份保护以及资产完整人工、115体验、Prompt对比及旧UNKNOWN／失败均保持。

## 2026-10-28 工程栏去重、画布工具归位与预览控制（本地候选）

### 范围与当前结论

用户对上一份工程管理候选“基本上可以”，但要求整改重复入口和四五行常驻内容。此结论不是完整编辑或图库验收。本轮只改组织与呈现，不扩展已确认工具、不改工程格式、回收规则、签名、更新协议或日常118。

- `TypesetProjectToolbar.kt`：一个工程名称菜单同时显示名称／时间、组织路径／页数，并承载打开本机工程、历史、收起、重命名、副本、移出、保存、外部工程导入、Recipe、导入源另存和工程／文件说明。主行仅常驻新建、追加、归档、完整工程导出和废弃；不再重复快捷选择器、独立信息行、展开“工程选项”区或旧交换导出别名。危险操作仍走原确认。
- `TypesetWorkspace.kt`／`TypesetProjectArchiveWorkspace.kt`：接入统一命令，不重写控制器；历史／新建／打开／导入导航回到对应视图，清理失败记录仅在有记录时显示。撤销／重做／完整预览／成品预览导出在画布底部，与适应／缩放一行；加载状态也保留工具栏，不因重排消失。窄画布工具行可水平滚动。
- `TypesetPreviewDialog.kt`／`TypesetFullPreview.kt`／`TypesetIcons.kt`：共享预览顶栏有关闭、最大化／还原图标，标题截断，保留Esc、翻页、滚轮缩放及拖动；删掉完整预览底部重复关闭。最大化铺满当前应用可用区域，非独立系统窗口／跨显示器最大化。共享容器也用于导出预览及已有只读字段弹窗，没有修改其数据操作。
- `TypesetProjectManagement.kt`：原编辑说明纳入工程与文件帮助，实际资料位置和交换内容说明保留；无重复复制原图或工程。

### 固定输入及执行证据

HEAD基点`89be2250cbf806632a4d6eed149ecdce5a3672b9`，分支`feature/windows-rc-continuous`，本轮及累计编辑改动未提交，不能以HEAD冒充新实现的已提交版本。最终源快照：`.local/windows-preview/evidence/20261027T164447424054Z-editor-toolbar-stable-loading-20261028/FIXED_SOURCE_MAP.json`；其中71为累计源条目，不是本轮新增71文件。本轮差异相对前候选逐文件在`.local/editor-toolbar-20261028/CLOSEOUT_MAP.json`，应用运行输入在`CANDIDATE_INPUT_MAP.json`。

桌面JAR SHA-256 `1a5ec53a6c0ff60424c6e8288855e4ed1119e8dc84b904854a9baf290b94e55e`，5,683,235字节，保存在`C:/CCB-Studio-Suite-Editor-20261027/programs/editor-toolbar-review/studio-desktop.jar`。59运行输入逐项校验；core`3bccd42de3856bedfe559f2553c4955f1a193f452c9ecb81af6d4a4367da4594`、asset-library`f55abfcfa68293ca1b58e31c5aa1574c86f7d141bfc491c4ba949147dc75ebe5`复用前候选。正常MainKt／secondary workspace，不是测试窗口替身，没有新安装包、tag或版本号。

Windows本机、离线严格依赖校验，串行单worker／低优先级；只给此调用768MiB堆、512MiB元空间、2处理器，不改全局配置。使用现有`.local/editor-native-20261027/Run-EditorChecks-C.py`：首范围`TypesetToolbarWorkflowTest`、`TypesetProjectManagementWorkflowTest`、`TypesetProjectArchiveWorkflowTest.actualArchiveUi*`、`TypesetInteractionRevisionTest.groupedLayout*`、`TypesetWorkflowTest.fullPreviewKeyboard*`，以及`:studio-desktop:jar`。最终只复核2个新用例和布局／宽窄预览用例，共4，已过的工程管理未无故重复。原始命令、环境、XML逐例、耗时、构建日志、before散列均在各phase。

1. `20261027T164033379906Z-editor-toolbar-consolidation-20261028`：整体FAILED，新增测试内局部函数同行语法导致编译失败，0执行；非生产编译失败，证据／fixture保留。
2. `20261027T164133791126Z-editor-toolbar-syntax-fix-20261028`：9执行，8PASS／1FAILED。工程菜单、相关创建／收起／归档／删除合成／完整交换，以及新预览最大化／还原／关闭通过；旧测试仍假定关闭在底部，实际已移到标题栏，保留失败断言与原图。
3. `20261027T164327237632Z-editor-preview-titlebar-regression-20261028`：1执行FAILED。修正位置断言后发现排版加载期间底部工具栏消失；现已修复，失败记录保留，非静默重试。
4. `20261027T164447424054Z-editor-toolbar-stable-loading-20261028`：4执行全部PASS，sourceStable=true，49.517秒。含1320／700宽预览、翻页／Esc、导出预览边界、队列宽度／缩略图、主行去重和菜单命令，以及预览扩大还原关闭后工程／页选择／原字节不变。

最新结果9唯一PASS，2新＋7受影响既有，**不是一次全套通过**。逐例取最新且重复不累计，见`.local/editor-toolbar-20261028/VALIDATION_CASE_MAP.json`；旧18工程管理用例证据不与本轮简单相加。实际组件PNG在phase的RAW_SHORT_MAP中，已查看主行／菜单／最大化预览。成功自有fixture144项约1.60MiB留证后按owner退役，失败fixture、固定基线、人工试用资料保持。没有新AI准确率或全Windows性能结论。

### 本地候选与用户检查

旧PID31644通过正常窗口关闭，未强杀，结果在`PRIOR_WINDOW_CLOSE.json`。新PID34884出现正常工作台、响应和空stderr，原生状态见`LATEST_LAUNCH.json`、launch目录及`CLOSEOUT_MAP.json`；这是启动证据，不是原生观感验收。快捷方式：`H:/CCB-Studio-Suite/.local/editor-toolbar-20261028/Studio-Editor-Toolbar-Review.lnk`。沿用受保护的前候选审阅资料，未枚举／复制／清理用户后续导入图片，不再为本轮建立另一套原图库。

进入编辑后只需确认工程栏是否紧凑／菜单是否易找、底部撤销／重做／两种预览的位置，以及预览右上角最大化／还原／关闭。已通过工具、AI识别和删除不要求重测；新观感仍PENDING。资产完整人工、未覆盖编辑项目、115体验、Prompt对比、独立审查和旧失败／否决继续保持，不因本轮局部通过而改写。编辑改善最终签收并固定交接后再迁移新对话；无自动新会话或发布。

## 2026-10-28 编辑工程栏整合与预览窗口调整进行中

用户对本地工程管理候选反馈“基本上可以”，但否决多行重复入口和分散的工程信息；并要求撤销／重做／预览集中到画布底部，预览窗口提供关闭／最大化。沿原 UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001 做局部整合，不重开已确认编辑工具验收。工程名称菜单统一本机列表、历史、收起、重命名、副本、移出、保存、导入与说明；主要新建、追加、归档、完整工程导出与废弃常驻一行，删除确认和数据边界沿用。无发布／日常安装改动；当前旧候选字节保留，新原生与回归状态尚待核验，不能先标通过。旧失败、用户否决、资产完整人工／115体验／Prompt对比等继续保留。

## 2026-10-28 工程管理原生实施与独占进度回收（当前：本地候选待人工）

用户批准最新版demo实施，并在解释两种删除语义后选择“彻底废弃，回收独占编辑记录”；删除仅合成测试，不发布或更新日常安装。不存在本轮提交授权，HEAD仍89be2250cbf806632a4d6eed149ecdce5a3672b9，空index保持。旧人工反馈、自动失败和Project独立审查状态保持，不把本次自测改为独立验收。

### 已实施的操作与关键位置

- `TypesetProjectManagement.kt / TypesetWorkspace.kt / TypesetProjectArchiveWorkspace.kt`：顶部可见本机打开、外部导入、完整导出和文件说明，工程栏可见收起、归档、废弃、历史；保留原快速选择、追加、重命名／另存／逻辑移出、撤销重做、完整预览、成品导出、Recipe／源文件／预设入口及原三栏工具。新建不再是0张不可继续的死路，可直接打开图库选图。原生宽窗为左树右详情，窄窗上下排列；树仅节点名与缩进，完整路径另列，支持滚动与展开收起。组织选择没有固定两层限制，沿核心最多32层。
- `TypesetOrganizationChanges.kt / TypesetProjectArchives.kt / TypesetRepository.kt / TypesetEditorExtras.kt`：组织增／改／移除为暂存，取消不写入；移除仅该节点，下级与挂接工程／批次上移，稳定ID不变。新建或归档最终一次日志发布组织与工程引用，旧组织改变则拒绝，不覆盖。`prepareCreate`只生成待引用快照，不提前发布工程；失败保留可审计孤儿，未猜测删除。新建／归档不复制原图，归档不另造快照或包。
- `TypesetEditorController.kt`：收起先drain保存，再持久记录当前编辑区为空，清空视图，可从未归档列表打开；已归档只在历史。废弃预读先停止防抖写入，取消恢复自动保存，确认不flush待废弃草稿；工程与全部undo/redo入口移除。删除提交后以NonCancellable切回UI确认，避免已删除工程继续显示可保存草稿。失败和待重试状态可见，不把逻辑移除当物理回收。
- `TypesetSourceExchange.kt / TypesetProjectFileHelp`：沿现有TypeSet v1 JSON完整交换，单文件64MiB，原图Base64及当前文字／打码／排版／队列／活动页可接续；确认页说明发文件→编辑页导入→摘要→独立工程。字体文件、本机撤销链、AI模型、凭据、成品和组织树不分发；缺字体需自行准备。预设交换剥离本机工程、输出路径和删除意图，导入保留本机字段。实际保存路径／哈希沿原消息显示，不要求用户去内部UUID目录找文件。
- `StateJournal.editorReferences / TypesetSnapshotRetirement.kt`：按经校验的完整文档链取该工程历次快照及导入源交换，覆盖早已超出30步undo容量的进度；其他仍存在工程的历次快照、输出来源、字体／预设及父关联保护资源。仅合格编辑内容进入固定清单，先持久项目删除与清单，再回收。已有备份不改写或清除。
- `EditorSnapshotRetirement.java / AssetStore.java`：不提供泛用GC或媒体删除。仅允许既定标签／名称／关系的编辑文档和导入源交换，复核身份、父关系及内容共享；关系记录移入`assets/maintenance/editor-retirements/<asset-id>/`，独占内容blob、已证实同源残余payload和缓存索引按清单处理，最小意图／关系／完成审计保留。已退役ID禁止复用，旧事务恢复不能重新生成编辑内容；中断可显式重试。
- `AssetBackup.java`：精确允许上述审计命名空间及记录名，保留原严格路径约束；删除后备份／恢复包含审计与幸存资源，旧备份可恢复旧工程是明确保留规则。首次遗漏此白名单导致失败，已修且原始失败不覆盖。
- `TypesetProjectDeletion.kt / WindowsOwnedOutputDeletion.kt`：生成文件只来自本轮实际导出时记录的绝对路径、长度、SHA及可选资产ID；旧未知输出不扫描补猜。默认保留，另选并确认才处理；确认到执行再次核对原图／共享引用、修改、保护目录。Windows持有祖先目录以阻止改名，拒绝reparse／硬链接，校验并删除同一叶句柄；支持长路径，只删文件不递归删目录。记录在本机editor扩展的`outputs / retirements`，失败／中断由工程栏记录入口显式重试。

### 删除边界与仍保留的内容

废弃确实回收专属编辑内容，不只是隐藏列表；未导出草稿不强行保存。图库内部原图／媒体资产、其他工程共用的内容、输出来源需用的共享快照、现有备份和最小删除审计保持。勾选生成文件删除针对明确登记的导出副本；本轮没有启动图库内部媒体永久删除或通用本地文件管理。标为“保留”的共享／未知／外部改变文件不擅自处理，文件占用可重试。此处物理内容回收不是安全擦除磁盘或销毁用户旧备份；不宣称本地完全零记录。导入源交换中嵌入的未独立共享编辑内容也会回收，已经提取为图库原图的字节保持。

### 实际测试、失败与固定输入

逐例最新映射`.local/editor-management-20261028/VALIDATION_CASE_MAP.json`为18唯一（核心11／桌面7），重复复核不累计；各阶段命令、耗时、原始JUnit、Gradle日志、source-before/after、固定源码、差异及截图位于`.local/windows-preview/evidence/`各精确阶段。不是一次全套PASS：

| 阶段 | 实际结果 |
| --- | --- |
| 20261027T155016254419Z-editor-project-management-independent-20261028 | 核心3PASS；组织／取消／中断、完整交换／原图不变。 |
| 20261027T155314768321Z-editor-project-management-desktop-20261028 | 桌面测试代码缺失导入，编译FAILED，0执行，保留。 |
| 20261027T155443954298Z-editor-project-management-desktop-fix-20261028 | 桌面5PASS，其中3既有归档回归，非新增人工。 |
| 20261027T160813595138Z-editor-project-physical-retirement-core-20261028 | 核心9执行，8PASS/1FAIL：新审计路径未进入备份白名单。失败源码与fixture保留。 |
| 20261027T161232251260Z-editor-project-retirement-backup-fix-20261028 | 失败的备份／恢复用例修后PASS；整体因桌面UI缺失导入编译FAILED，不能改为整体通过。 |
| 20261027T161504031589Z-editor-project-management-final-desktop-20261028 | 512MiB构建GC thrashing，0执行，保留。 |
| 20261027T161711020911Z-editor-project-management-desktop-memory-20261028 | 仅本次串行构建768MiB／metaspace512MiB、2处理器，不改全局配置；桌面7PASS。 |
| 20261027T162105815402Z-editor-project-management-final-guards-20261028 | 新父目录联接替换1PASS＋NonCancellable发布修正后两个既有删除UI用例复核PASS；不增加重复总数。 |
| 20261027T162251862930Z-editor-project-import-source-retirement-20261028 | 新导入源工程内容回收、提取原图／源工程保全1PASS。 |

覆盖：旧undo之外的独占内容真实不存在、共享快照可读、旧备份不变及删除后备份恢复、未知/修改/共享输出保留、文件占用与硬链接保留/重试、中断持久意图与事务不复活、确认后将父目录换为junction仍不能删未选目标、当前草稿取消/不强存、默认保留与选择性文件删除、同ID归档重开、本机列表隔离、深层节点与完整工程往返。所有删除是隔离合成资料，无真实服务、私人凭据或日常资料。已看实际组件树/删除截图，不代签原生手感。

首次辅助seed打包遇到不相关测试类Windows长路径，未执行seed；保留`PREPARATION_LONG_PATH_FAILURE.json`，改成仅seed/helper家族及长路径读取，生产运行没有测试宿主/seed依赖。另一次证据固定命令目录名录入错误只造成FileNotFound、未改源码，随后使用实际阶段名固定；原失败日志继续保留。

### 本地候选与下一步

固定源条目68是累计本地未提交改动；源码基点89be225不变。候选生产MainKt，core SHA256 `3bccd42de3856bedfe559f2553c4955f1a193f452c9ecb81af6d4a4367da4594`、desktop `ef474aeac1ca09bae05b051b36dd7e669be4a34d0400b20f07f34bc9a156b5c6`、asset-library `f55abfcfa68293ca1b58e31c5aa1574c86f7d141bfc491c4ba949147dc75ebe5`，共59运行输入（含参数文件），映射`.local/editor-management-20261028/CANDIDATE_INPUT_MAP.json`。原图全部合成，0账户；三个工程提供当前／本机列表／归档入口和已登记／共享／未知输出。新人工根`editor-native-review-e9c9ee73400448bba554bbd805cdbee9`已受保护，不自动清理未来人选图片。旧人工根未读取、检查或复制图片。

启动链接`.local/editor-management-20261028/Studio-Editor-Management-Review.lnk`。PID31644工作台窗口有响应、stdout/stderr为空；没有代用户操作窗口，进入顶部编辑即可试用。建议仅新流程：收起再本机重开；新建/归档内多层节点及取消；对“合成”工程废弃／归档删除，默认保留输出或明确勾选独占输出，共享项应禁用；完整工程导出后导入为独立工程续编。已确认的其他编辑工具不重测。保留规则和限制见上，不把这个本地结果当新发行升级链、完整编辑/资产人工或Project独立验收。

新安装器／tag／版本／发布／日常118／信任配置均未变化，0提交/暂存/push。用户试用和未覆盖情形继续PENDING，115体验／Prompt对比及旧独立／否决／失败保持。编辑改善签收／固定交接后才迁移新对话，不自动开启新任务。

## 2026-10-28 用户明确彻底废弃，独占进度物理回收实施中

用户澄清选择“彻底废弃，回收独占编辑记录”，替代此前未定边界；原图、共享资源、已有备份保留，生成导出文件另选确认。限定实现通过文档审计链追溯当前/undo/redo及超出撤销容量的旧快照，先排除其他工程历史、输出来源、字体/预设等引用，再固定专属编辑内容清单。只回收受控编辑文档或导入源交换的内容字节，保留最小身份/删除审计；不引入全库自动GC、媒体原图删除或跨页通用文件管理。项目删除提交后按持久意图处理物理回收，文件占用/中断可显式重试，已退役事务不能恢复成旧编辑快照。

首次物理阶段9例执行，8PASS/1FAILED（删除后备份白名单未包括新审计路径）。失败源码、原始XML、日志与隔离fixture保留，不宣称通过；已补精确路径白名单，正在定向复核。前面独立组织/交换3、桌面5各已PASS，不累加重复，首桌面缺失测试导入的编译失败保留。尚未打开新候选、未操作用户资料/原图/日常安装、未提交或发布。以下为历史时点。

## 2026-10-28 工程管理demo已批准，原生实施进行中

用户确认最新版demo并授权实施、最小自动验证和本地候选；不破坏原有功能，删除只用合成资料测试，不发布／不更新日常安装，编辑收尾后迁移对话。沿UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001，不新建重复任务。显式收起／废弃／完整工程分享、多层组织就地管理、本机工程列表与外部导入说明进入实施。13项demo结果仍只是方案检查，不能标为原生通过。

删除边界存在重要差距：现有资产与文档为不可变存储，移除当前工程不等于回收底层旧快照。已向用户解释仅移除入口与物理回收独占进度的区别；用户要求进一步说明，尚未选定处理边界。原生删除代码为未完成草稿，不能把逻辑移除写成物理回收，也不以用户原图验证。先独立验证组织、收起、完整交换等确定范围；真实删除与完整候选签收仍待闭环，旧失败／用户否决／独立审查及115／Prompt等状态保持。

## 2026-10-28 工程管理demo入口显式化（待确认，未实施）

用户要求废弃／导出放明显位置，导出按完整工程并说明内容／接续，不破坏已有功能。沿原编辑任务将“废弃当前工程…”置于工程栏，“导出完整工程”置于顶部常驻；原隐蔽工程操作菜单不再承担这两个入口。无工程时导出禁用，收起／归档／本机打开／外部导入／历史／多层组织／确认及原有成品导出保持。完整交换包含原图与当前编辑内容，明确导入为新工程继续和字体／历史／模型／凭据等不含项；不引入轻量引用格式，本机归档不生成分享包。

`.local/editor-project-management-actions-demo-20261028/`保留前版片段／输入映射和本次逐例／截图／固定映射。最终13唯一浏览器场景PASS（新增2+前版11复核，不累计重复），0网络／最终脚本错误；已看深色主界面、导出说明及360浅色。437模块输入、HEAD89be225及空index不变，无新构建／原生测试／真实文件删除／日常118／提交／发布。成功自有浏览器181项约14.5MiB留证退役（以本次BROWSER_RETIREMENT.json实值为准），前版失败和人工资料保持。仍为demo待确认，不代表原生实施或用户验收；其他待验与编辑收尾后迁移保持。

## 2026-10-28 编辑工程管理重整demo（当前：待用户确认，未实施）

### 本次明确需求与旧结论

最新人工指出新建／归档不能就地创建组织、仅两层、节点不可管理、工程打开／外部导入混淆及文件内容不透明。前一最小归档的7新增+3受影响自动PASS证据保持，但其完整管理流程状态为USER_REVIEW_ISSUES_REPORTED / NOT ACCEPTED；不能用旧demo批准或自动通过代替本次需求确认。

用户明确“先做demo给我确认，不要发布”。本次无原生实现／真实文件删除／发布权限，旧本地候选仍是上次固定字节，demo不会注入其中。原先“放弃未保存修改”的建议被否决并废止；按当前自动保存语义拆除那个误导入口。

### 当前交互合同（方案，待确认）

1. **收起当前工程**：保存当前全部进度，清空编辑区；本机工程列表保留同一ID／内容／历史，归档工程不进入该列表。此动作不等于归档或删除；重新打开继续编辑，切换已有工程先保存当前内容。
2. **废弃当前工程**：永久放弃该工程创建的全部编辑进度，包含已自动保存文字／打码／页面顺序／undo／redo。工程不再保留用于恢复，不能以“自动保存仍在”假装废弃；依赖的原图、其他工程和独立备份保留。已有备份／此前共享出去的文件不因本次废弃而被删除。真实实现必须先形成可执行引用／保留策略，本轮只演示。
3. **删除归档**：历史列表单独入口，同样确认整个工程进度的永久删除；不是仅删归档标签而留一份可用工程。**删除生成／导出文件是独立选择，默认关闭**；列明可选文件名与实际位置、选中数量和不可撤销后果，不把依赖原图列入可删范围。共享引用、未知归属或外部修改的文件不在自动删除范围；执行前重新核对归属和引用，跳过原因必须可见。图库状态需与已删除生成文件对应。示例中一个独占ZIP可选、另一共享PNG受保护。取消不修改工程；确认需勾选已理解范围，变更文件选择后重新确认。
4. **组织**：树状多层，项目／卷／章节／自定义名可连续嵌套，沿现有核心最多32层而非新造两层模型。在新建／归档弹窗直接选择、创建下级、重命名、移除节点；稳定ID关联。移除节点单独解释并确认：仅移除该节点，下级节点与挂接工程上移到父节点，根节点的内容进入未归类，不删除任何工程／资产／文件。组织变更与新建／归档一并提交，取消撤回本次节点变更；重命名不改引用。树支持展开／收起，不以所有对象平铺替代树。
5. **打开本机工程**：直接列出已自动保存且未归档的本机工程（含已收起），无需文件选择器；归档单列历史并可重新打开原ID。**导入外部工程文件**单独入口，说明来源／扩展名／内容摘要，确认后新建独立本机工程，不覆盖旧工程。新建同时选图片，不再出现只显示“选中0张”而无选择入口的死路。
6. **文件说明**：常驻“工程与文件”；本机工程内部在资料根的assets与documents管理，不是要求用户自行查找的单一工程文件。正式UI将显示实际运行资料路径并能打开所在位置；demo仅显示相对应用目录示例，不引用私有路径。原图不因收起／归档／组织变化复制，只有用户显式导出分享工程文件时才生成包含原图的新文件。

### 已核实的交换格式事实与方案说明

源码 `modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetSourceExchange.kt` 的 `export / inspect / import` 为TypeSet v1单个JSON：`app=image_typesetter_frontend_workspace`，原图以`blobDataUrl`内嵌Base64；当前页的文字、打码、排版（Studio扩展保留页字段）、页面队列／活动页与小人书内容可继续编辑。导入重新建立本机图像资产与独立工程，原源交换JSON也保留，源历史原文可能留存但不执行成Studio撤销命令。它不是目录，也不是成品图或完整备份。工程文件示例名`名称.typesetter-workspace.json`，当前单文件上限64MiB。

本机undo／redo链、字体文件、AI模型／账户凭据、导出成品、本机组织树不随此工程交换导出；对方需自行准备缺失字体。不能宣称所有本机字体引用已跨机可用或把当前可编辑内容等同完整历史备份；真正导入导出兼容和字体重绑定需在确认实施后按实际代码验证。本demo不读取用户文件，用固定示例摘要展示内容边界。

### 固定demo与实际执行

片段：`C:/Users/1/.codex/visualizations/2026/10/20/01a159f6-0089-77e1-adcb-8387addc7fd5/editor-project-management.html`。固定SHA-256：`7e869fa65b226e53901ff56455747552800321f768786291cf5d8dc7b8e0ce39`，39480字节。三栏原布局保留，新增工作流集中在工程栏／独立弹窗，没有重测已确认编辑工具人工。

证据：`.local/editor-project-management-demo-20261028/`，`FINAL_INPUT_MAP.json`、`FINAL_FRAGMENT.html`、最终`RESULT-2026-10-27T15-16-28-957Z.json`和原始截图。命令：`D:/node.exe .local/editor-project-management-demo-20261028/check.cjs`，Windows / Edge131.0.2903.112 / Playwright，明确自有profile，无已安装浏览器用户资料。11唯一场景最终PASS：三栏与初始不写状态、收起重开与归档隔离、节点CRUD／取消、同ID归档重开、创建选图／空名验证、当前废弃确认和受保护输出、归档删除确认、外部导入／导出说明、节点移除上移关联、宿主状态恢复／Esc、深浅1024/736/360/320布局／弹窗完整显示。0网络请求、0最终页面脚本错误。已查看实际宽屏归档／废弃与360浅色截图。

最初测试脚本await位置错误，0场景执行；随后demo空列表三元表达式漏条件导致SyntaxError，保留`PRE_SYNTAX_FIX.html`及失败结果。接着两次测试选择器存在双取消入口歧义／动态`:checked`索引变化，修正测试定位；所有阶段结果和3份失败profile已封存保留。重复阶段不累计成更多测试。最终成功自有浏览器产物留证后按owner清单退役，未删除任何工程、原图、人工审阅资料、旧失败或固定基线。

437模块源输入、HEAD89be2250cbf806632a4d6eed149ecdce5a3672b9及空index不变；本轮0Gradle／原生测试／原生UI操作／真实文件导入导出或删除／提交／push／发布／日常118改动。**这是demo可用，不是删除功能已实现或原生验收通过。**确认后才实施相应工程生命周期／安全文件引用逻辑；不启动跨页通用文件管理，不覆盖UX-ASSET-FS-001和旧未完成状态。其余资产完整人工、未覆盖编辑项、115体验、Prompt对比、模型准确率／许可及旧独立／失败结论保留。编辑改善收尾固定交接后再迁移新对话。

## 2026-10-27 最小工程归档原生实施与本地候选（当前）

用户已确认demo，授权实施、自动验证、本地候选试用；明确**原图和工程不重复复制、不自动清理编辑历史，不发布或更新日常安装，编辑收尾后迁移对话**。并要求各页面归档为未来永久删除/本地文件操作保留可能。本增量沿UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001；UX-ASSET-FS-001继续PLANNED，未执行真实文件操作/永久删除，也不扩展改造其他页面归档。

### 实际实现

- `TypesetProjectArchives.kt`新增`ACTIVE / ARCHIVED / REMOVED`目录状态，按稳定`projectId`关联原工程。归档名称写回同一工程元数据，自动附归档时间/既有组织ID；`current`配方引用、undo/redo、页选择、来源交换引用均保留。重新打开仅把同一ID改回进行中，不复制工程/图片；多工程并存时保存其他进行中工程。
- 管理记录放入既有`TypesetEditorExtras`扩展；`TypesetEditorExtrasStore.save`将目录及原工程名称/修订元数据通过既有工作区日志一次提交。原图、模型、字体、工程内容/撤销快照不打包或重复写入，不建归档目录/ZIP。归档前未保存编辑仍走原保存流程；**归档动作本身不新增工程内容快照**，只新增少量设置/日志记录。每次实际归档的一条设置资产修订已在合成用例核对，不承诺磁盘完全零写入。相同归档/重开重复执行为no-op。
- `TypesetEditorController`保存后再归档，失败保留工程/草稿；明确保存“当前编辑区为空”，切页或关闭重开后不擅自打开归档/其他进行中工程。首次旧资料没有该选择记录时沿原兼容规则恢复最近活动工程。
- `TypesetProjectArchiveWorkspace.kt`、`TypesetWorkspace.kt`保留原三栏、工具和底部适应画布；独立“命名并归档”“历史工程”行，名称/时间、项目/章节、封面/只读历史、重新打开原工程。项目/章节沿用图库组织树，不另建组织副本。只有用户重新打开才切换当前工程，查看封面不编辑工程。
- `TypesetSourceExchange.kt`预设导出剥离本机生命周期记录/当前工程ID，导入保留本机这些字段，不能让外来预设改变归档状态/启用另一工程。没有修改全局资料版本、更新协议或信任链；当前仍是内部候选，尚未进行新发行安装器/旧发布版升级组合核验。

### 为未来文件操作保留的边界（不是本轮删除功能）

`TypesetManagedResources`提供只读引用清单：项目ID、原图资产ID、current/undo/redo快照块ID、来源交换、导入字体资源ID。共享原图可同时属于多个工程；**引用清单不代表独占所有权、删除权限或完整的全工作区删除计划**。未来还须核对其他工程/批次、库关联、旧日志、备份和人工保护。文件路径由既有受控资产/目录绑定层解析，不由归档别名拼接。

现有编辑管理入口改名“移出工程列表”，保留`REMOVED`标记及完整工程/引用图，未删除本地文件；不冒充回收站或永久删除，未新增移出项恢复界面。原`TypesetRepository.remove`旧低层接口不因本轮获得物理删除能力。后续UX-ASSET-FS-001统一设计以下不同操作：解除组织/列表关联、可恢复回收站、显式永久删除原文件；未知归属、共享资源、备份保护和活动任务不能因“已归档”自动豁免。

其他页面继续沿既有身份体系：生成页`batch/group/job`与不可变receipt、图库资产/虚拟相册ID及目录采集绑定/观测、Prompt/字体/模型资源ID。本轮未改它们的清理或归档规则，后续删除须跨来源对账，不用标题或“不在当前列表”判断垃圾；具体文件管理实现、权限确认及独立验证仍待后续范围确认。

### 执行、失败与固定输入

基点`89be2250cbf806632a4d6eed149ecdce5a3672b9` / `feature/windows-rc-continuous`，本地未提交/暂存/push。Windows10 x64 / Temurin17 / Compose1.10.3 / ORT1.31.1 / Gradle9.1.0，低优先级、串行单worker、offline/strict，无真实服务或凭据。证据在`.local/windows-preview/evidence/`：

| 阶段 | 实际执行 |
| --- | --- |
| `20261027T143932639120Z-editor-minimal-project-archive-20261027` | 核心5实际PASS；桌面test编译遇新测试辅助方法的局部函数分隔语法错误，整体FAILED、桌面0执行。原始源码/编译日志和失败fixture保留。生产编译/JAR完成不等于桌面执行通过。 |
| `20261027T144212943502Z-editor-minimal-project-archive-desktop-20261027` | 修正测试辅助语法后桌面5实际PASS，0失败/错误/跳过，96.974秒、sourceStable。核心源码与上一阶段逐文件相同，核心5不重跑；不是把整体FAILED改标PASS。 |

合计**10唯一自动用例有PASS证据：7新增、3受影响既有回归**，不重做已确认人工。新项覆盖同一身份/配方/undo/redo/原字节、归档/重开幂等、备份恢复、非法名称/组织/过时修订/损坏配方拒绝、`DOCUMENT_STAGED`中断保留旧提交/待恢复文件且不静默清理、共享原图引用图及逻辑移出、实际Compose命名/组织/清空/只读历史/重新打开、启动空选择及已有活动工程保全、归档失败草稿保留；既有预设资源、切页连续性及一键AI保存回归通过。

每阶段`summary.json`、`test-cases.json`、原始JUnit/XML、`gradle.log`、`FIXED_SOURCE_MAP.json`保留。最终固定53源，map SHA256 `bbeacc9bbb417d27d48b8c86ec333bee2f983794b6928ed79863d6dfbbfb75a4`。实际组件PNG和预期中断说明经`RAW_SHORT_MAP.json`定位，已查看归档弹窗和历史页；不是原生窗口视觉/用户手感验收。核心命令`:studio-core:test --tests '*TypesetProjectArchivesTest' --tests '*TypesetAutoMaskSettingsTest'`；桌面命令`:studio-desktop:test --tests '*TypesetProjectArchiveWorkflowTest' --tests '*TypesetNavigationAndPerformanceTest.routeDisposalKeepsDraftQueueSelectionAndSavedProjectThenRestartResumesIt' --tests '*TypesetAutoMaskOneClickTest.saveOnceThenRepeatedPagesAndRestartUseLocalModelWithOneUndoAndNoDuplicateMasks'`。成功桌面自有fixture288项/833936字节已留证退役；首失败fixture及旧失败/基线/人工根不清理。

### 本地完整候选已打开，等待限定人工

`.local/editor-native-20261027/project-archive-full-review/`的`CANDIDATE_INPUT_MAP.json`、`VALIDATION_CASE_MAP.json`、`LATEST_LAUNCH.json`固定53源、10唯一用例及59运行输入（58生产JAR+args）。正常MainKt、`--secondary-workspace --pause-collections`、相同受保护人工审阅根，无test宿主、重置/复制/检查用户选图或默认入口接管。入口`Studio-Local-Review.lnk`参数读回一致。

| 授权C隔离目录`programs/editor-project-archive` | 字节 | SHA256 |
| --- | --- | --- |
| studio-core.jar | 70,134,899 | 512e9686614abc08e7a4c6f00f3a370775b9ba3709c86ac17b59a5dbf6a49424 |
| studio-desktop.jar | 5,577,082 | 72269f71f58c87e558e840e4a70316439baccde50750ef5a54e86cc1cad19c45 |

新PID21564 / 工作台窗口已实际出现，55秒后仍有响应，stdout/stderr均空；`launch-20261027T144655820Z/`保存启动/窗口状态。旧候选启动前已不在，没有强杀或操作日常程序；旧7332问题不因此宣称已解决。

当前 **MINIMAL_EDITOR_ARCHIVE_IMPLEMENTED_AUTO_VERIFIED_LOCAL_CANDIDATE_OPENED / USER_REVIEW_PENDING**。人工只需命名/组织归档、当前栏清空、历史查看/重新打开后的同工程/内容/页选择/撤销及重开保留；其他已过功能不重测。无发行版本、安装器、发布、日常118或信任变化；资产完整人工、旧编辑未覆盖项、115体验、Prompt对比、模型准确率/许可、分割/橡皮擦、原切页触发原因和旧Project/失败/否决状态保持。编辑改善试用收尾并固定交接后，再迁移新对话。

---

## 2026-10-27 最小工程归档demo，等待确认后实施（历史阶段）

用户“感觉还行”仅作当前编辑体验的限定反馈；提出归档并明确要求少本地文件。随后批准**只做最小归档demo，确认后实施**：原图与工程不重复复制，不自动清理编辑历史。沿原UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001登记`EDITOR_ARCHIVE_MINIMAL_DEMO / READY_FOR_USER_CONFIRMATION`，不新建重复任务，不将demo批准当原生开发/发布许可。

交互方案位于线程授权可视目录`C:/Users/1/.codex/visualizations/2026/10/20/01a159f6-0089-77e1-adcb-8387addc7fd5/editor-project-archive.html`，SHA256 `757429871287557397ab494eaff35db486149e1a66d38cf10f75c36c1b9ad53e`，29647字节。保留三栏编辑/底部适应画布；新增独立“命名并归档”“历史工程”入口，不把归档藏进设置。

- 归档填写名称，自动附JST时间，选择既有项目/章节（可未归类或直接放在项目下）；取消/Esc/空名称不提交。提交仅变更同一工程ID的名称、组织、时间和状态，当前编辑区清空。
- 历史工程可只读选看封面、页数、上次编辑页和历史步数；重新打开同一工程回到进行中，保留队列/文字/打码/页选择/撤销历史。已有进行中工程仍保留，可从工程选择器切回；反复归档/打开不增加工程或原图身份。
- 合成引用始终4张原图、3份工程，新增副本0；不模拟ZIP/模型/原图副本、备份新目录或历史回收。这里的数量是demo数据状态，不是实际磁盘归档机制已实现或验证。

`.local/editor-archive-demo-20261027/`保留`check-archive.cjs`、最终及各轮原始`RESULT-*.json`、`FINAL_INPUT_MAP.json`、固定最终/历史片段、宽窄/深浅截图。实际隔离headless Edge/Playwright **10唯一浏览器用例PASS**：首次不写host状态、取消/Esc、名称/组织联动、归档字段及恒定数量、历史只读、重开/重复归档、已有进行中工程保全、host恢复、名称按文本显示、1024/736/320宽度和两主题/键盘焦点；外部网络请求0、页面错误0。实图发现旧名称错误提示未随有效输入消失及主按钮悬停颜色问题，均修正并复核同10项，不将多轮累计为更多通过；旧截图/片段/结果保留。

本轮0 Gradle/原生测试/应用窗口操作，49份既有固定模块源码逐文件散列不变，HEAD仍89be225且index空；没有实施新归档存储、提交/推送、发版、日常118或真实资料改动。仅本次新注册浏览器profile/temp按owner封存证据后清理229项/13987838字节，不涉及原图、工程或编辑历史。用户确认布局/行为后才进入原生实现；原切页触发根因、卡顿实感、新AI跟随限定人工、资产完整人工、115体验、Prompt对比、旧未验/失败/独立状态保持。编辑阶段固定交接后再迁移新对话。

---

## 2026-10-27 切页工程保全、预览重复工作及AI跟随工具（历史阶段）

用户对一键AI反馈“试用效果还行”，只记限定可用；另报告卡顿和“切到顶部其他页面再回来，整个工程没了”。该报告仍 **USER_REPORTED_NAVIGATION_ISSUE / ORIGINAL_CAUSE_UNKNOWN / HUMAN_RECHECK_PENDING**：没有读取私人审阅工程来证明丢失或未丢失，没有在旧候选执行成功复现；新源码合成通过不倒填成旧根因已查明。沿既有UX-RECOVERY/W4、UX-R2、UX-EDITOR-GAP-001及编辑性能项，不重开已过人工，不新开性能全域项目。

### 本轮局部修正

- `StudioApplication.kt`的顶部导航走`navigate`：离开编辑前禁止继续编辑并完成`editor.drain()`，保存失败保留编辑页和草稿，报告未完成，不能先隐藏工程。
- `TypesetEditorController.kt`保留进程中的工程/文档/页选择/工具/属性tab；通过既有`TypesetEditorExtras`扩展记录最近工程ID，创建/打开/保存时更新；进入编辑时若控制器尚未装载工程则恢复该工程。旧资料尚无选择记录时沿已有“最近工程”规则选最后工程。不重建空工程、不覆盖未知/失败文件；明确删除工程才清除记录。
- `TypesetSourceExchange.kt`预设导出不带本机工程ID，导入/替换保留本机工程与AI配置；外来预设即使带伪造工程/模型引用，也先剥离，不能把本机切到别的工程或启用外来模型。
- `TypesetPreviewRaster.kt`增加页工作器内字体校验键：文本/样式/竖排和持久字体身份变化才重查，纯拖动/调整位置不枚举和验证相同字体；导出仍完整验证。`TypesetWorkspace.kt`不因只换选中对象而重画像素完全相同的页面。
- `TypesetQueueWorkspace.kt`采用控制器持有的缩略图LRU缓存，最多24项/总200万像素，只留显示小图；键含完整页面配方/模式/页码/尺寸，内容变化不复用旧图。属性快速变化先合并120ms再处理，切页回来复用已有小图。原图/全尺寸图不进入此缓存，不改变导出质量；不是所有编辑卡顿已解决。
- AI设置新增“跟随当前打码工具”，默认开启；执行时冻结当前所选打码或新工具参数，沿用黑、白、马赛克、模糊及强度/透明度，也兼容应用后的命名预设。不改AI类别/阈值/分辨率/外扩设置；可关闭跟随并保留AI独立效果。识别几何仍是矩形检测框，画笔路径/条纹密度等仅画笔适用参数不会变成分割结果；未新增像素分割。

### 实际执行与证据

基点仍`89be2250cbf806632a4d6eed149ecdce5a3672b9` / `feature/windows-rc-continuous`，本地未提交/暂存/push。Windows10 x64 / Temurin17 / Compose1.10.3 / ORT1.31.1；Gradle9.1.0离线strict、低优先级、单worker，无真实服务、真实凭据、日常资料操作。证据均在`.local/windows-preview/evidence/`：

| 固定阶段 | 执行 |
| --- | --- |
| `20261027T135624758508Z-editor-navigation-tool-follow-preview-20261027` | 核心1+桌面7实际8PASS，0失败/错误/跳过，141.726秒，sourceStable。新4项为路由卸载/重返/重开恢复与缩略图复用、保存冲突保留草稿、AI四效果跟随、字体重复工作/缓存边界；既有4个受影响自动用例复核，不是重做已过人工。 |
| `20261027T140009137002Z-editor-navigation-local-reference-retention-20261027` | 最后补外来工程/模型引用剥离，核心同1用例扩展后复核PASS，38.065秒、sourceStable；desktop源码与上一阶段完全一致且编译/JAR UP-TO-DATE，7项不重复运行。 |

**本轮8个唯一自动用例，其中4个新增；末次复核不累加为9。**原始JUnit/XML、`gradle.log`、`summary.json`和逐例`test-cases.json`保留。最终`FIXED_SOURCE_MAP.json`固定49个改动源，SHA256 `656f45871c38ca28d7fb8233d3151a8c633e0fa815ce967939359deb879344e1`；与桌面阶段逐文件比对不变。核心命令`:studio-core:test --tests '*TypesetAutoMaskSettingsTest'`；桌面命令`:studio-desktop:test --tests '*TypesetNavigationAndPerformanceTest' --tests '*TypesetAutoMaskOneClickTest' --tests '*TypesetInteractionRevisionTest.saveAiSettingsThenToolbarAppliesAcrossPagesWithoutReopeningConfiguration'`，生产JAR同时构建。无发行安装器核验。

性能合成记录：60次仅几何变化只校验字体1次，热键检查总136300ns；对照10次原校验总211455400ns。仅为本机合成重复工作样本，**不是可见帧耗时/完整编辑性能或用户顺畅验收**。原始`preview-font-work.txt`通过`RAW_SHORT_MAP.json`定位。只读资源采样：任务开始可用内存约2GB，构建时约450MB，候选启动前约1.35GiB；旧已知无界面7332约227MiB仍在，未强杀。仅说明有资源压力，不把卡顿全部归因给电脑，也未启动系统清理。

准备失败单列：首runner参数缺`--`分隔，解析拒绝、没有Gradle执行；首证据复制遇Windows深层合成备份路径长度限制，原fixture保留后改用短散列名保留必要PNG/性能原文，固定源码在此前已保存。`FREEZE_PREPARATION_NOTES.json`保留，不将准备失败伪装应用失败或抹去。513项/1055686字节、127项/111480字节成功自有fixture按owner留证退役；旧失败、固定基线及人工工程不清理。

### 固定完整本地候选与待验

`.local/editor-native-20261027/navigation-tool-follow-full-review/CANDIDATE_INPUT_MAP.json`固定59输入（58生产JAR+args），使用正常MainKt、相同受保护人工审阅根、`--secondary-workspace --pause-collections`，没有test宿主/默认入口接管/自动更新。人工选择的图片未检查、复制或收为证据。

| 保留程序位于授权C隔离目录`programs/editor-navigation-tool-follow` | 字节 | SHA256 |
| --- | --- | --- |
| studio-core.jar | 70,111,973 | 3a81523c1d401296d9b97c646f76642c032c3f19b42f61dd633d7d027e42d5b1 |
| studio-desktop.jar | 5,495,528 | bcbc87110672db398b7c22c295854951307a3b846a92e1567c56e53e889ebc4e |

旧PID33392在本次启动前已不在，未归因其退出原因、未强关。新PID1796 / 工作台窗口已实际出现，29秒后有响应、stdout/stderr空；`LATEST_LAUNCH.json`及`launch-20261027T140223012Z/`保存启动、窗口/日志状态。入口`Studio-Local-Review.lnk`参数读回一致。仅确认启动，不代签视觉或卡顿改善。

当前只需人工复核：进入编辑并确认工程；做一处文字/打码修改后立即点顶部图库/资产管理再回编辑，确认工程/内容/页选择保留；观察拖动/切图手感；AI保持跟随，切换打码效果后直接自动打码确认沿用，结果仍可撤销。其他已过布局/导出等不重测。新源码状态 **SCOPED_AUTO_PASS_LOCAL_CANDIDATE_OPENED / NAVIGATION_AND_PERFORMANCE_HUMAN_PENDING**。新布局无扩张、无提交/发布/日常118/信任变化；资产完整人工、旧编辑未覆盖项、115体验、Prompt对比、模型准确率/许可、分割/橡皮擦及旧Project/失败/否决仍保留。待这组编辑改善收尾、固定交接后再迁移新对话。

---

## 2026-10-27 AI配置保存与一键执行，本地完整候选已重开（历史阶段）

用户对实际完整应用反馈“可以用，就是不太方便”，要求配置一次后直接一键打码，窗口只配置；并确认沿Studio仓库来源记录加入上游追踪。记 **AI_USER_REPORTED_USABLE_WITH_UX_FOLLOWUP**，不提升为模型准确率、完整编辑或Project验收。沿原UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001，无重复任务；源码基点`89be2250cbf806632a4d6eed149ecdce5a3672b9` / `feature/windows-rc-continuous`，本地未提交/暂存/推送，无安装器/tag/发布/日常118/信任改动。

### 当前行为与实现

- 工具栏“AI 打码设置”（齿轮）只设置本地模型、类别、阈值/IoU/分辨率/外扩、当前页或所选页、效果/强度/透明度；“保存配置”后关闭，不在弹窗识别/选择结果。首次未配置时点击“AI 自动打码”会引导配置。
- “AI 自动打码”读取已保存配置，直接本地识别并加入可编辑打码对象；切图/关闭重开继续使用。不保存弹窗中尚未确认的修改。识别时显示进度/取消；取消在原生调用返回后停止，不保证中途抢占原生推理。
- `TypesetAutoMaskController.kt`冻结工程/目标/配置；`TypesetEditorController.commitAutoMasks`在UI/提交锁内核对工程及快照，拒绝过时结果，一次原子保存，整次可撤销。相同区域/参数不重复添加，未选页完全保持，已有手动打码不移除，原字节不改。零命中不等于识别正确。
- `TypesetAutoMaskSettings.kt`及既有`TypesetEditorExtrasStore`持久化配置；用户按保存时将所选模型作为带专用tag的不可变本地资源保留，按SHA去重、读回校验。图库不把模型显示为图片；沿既有资产备份保留。原绝对模型路径不写配置，重开不依赖下载位置。更换后的旧模型资源暂保留，无自动垃圾删除新规则。
- `TypesetSourceExchange.kt`不把模型/config加入预设导出；导入/替换预设保留本机AI配置，不从外部预设激活模型。未更改全局资料版本/更新协议；本地模型仍不随公共程序分发，许可UNKNOWN。

### 本增量自动执行与失败保留

环境Windows10 x64 / Temurin17 / Gradle9.1.0 / Compose1.10.3 / ORT1.31.1，串行单worker、offline/strict。下列均为`.local/windows-preview/evidence/`内独立阶段，原始`summary.json`、gradle.log/XML和`FIXED_SOURCE_MAP.json`留存：

| 阶段 | 实际结果/适用范围 |
| --- | --- |
| `20261027T131819379091Z-editor-one-click-ai-20261027` | 4执行，核心配置/备份1PASS、过时快照拒绝1PASS，重开/组件2FAILED，整体FAILED。重开失败为合成fixture把runtime/catalog放入workspace，已改为兄弟目录；组件只报告IllegalStateException，确切根因UNKNOWN，后续未复现，不能冒称已证明根因。失败源码/原始结果完整保留。 |
| `20261027T132137569445Z-editor-one-click-ai-guards-20261027` | 修正fixture/明确快照错误后控制器+实际组件2PASS，sourceStable；不是新增2个唯一用例。 |
| `20261027T132450725806Z-editor-one-click-ai-scope-final-20261027` | 最终桌面3PASS，0失败/错误/跳过，33.223秒、sourceStable；补未选页精确不变及幂等。核心源码与首阶段已通过用例一致，未重跑。 |

**4个唯一应用用例具有PASS证据，不将4+2+3执行次数合计为9通过。**覆盖配置保存/重开/备份、模型资源去重与预设交换、本地Constant ONNX一键跨页/零重复/原图保持/撤销、过时快照拒绝、真实Compose配置入口与工具栏不重开弹窗、未保存参数不用。Constant模型仅合成管线，不是实际人体识别准确率。已看实际组件`one-click-ai-settings.png`及`one-click-ai-toolbar-result.png`；不代用户原生视觉/手感。两个PASS自有fixture分别135项/566320字节、189项/641817字节按owner留证退役，失败/旧基线/人工根保留。

最终命令见阶段summary完整arguments：`:studio-desktop:test --tests '*TypesetAutoMaskOneClickTest' --tests '*TypesetInteractionRevisionTest.saveAiSettingsThenToolbarAppliesAcrossPagesWithoutReopeningConfiguration'`；核心首阶段`:studio-core:test --tests '*TypesetAutoMaskSettingsTest'`。新工具3个Python合成用例单独记录，不累加成应用回归。

### 新固定本地候选

目录`.local/editor-native-20261027/one-click-ai-full-review/`：`CANDIDATE_INPUT_MAP.json`、`VALIDATION_MAP.json`、`LATEST_LAUNCH.json`、`launch-20261027T133642062Z/LAUNCH.json`和`STATUS.json`、stdout/stderr及`Studio-Local-Review.lnk`。固定源码map SHA256 `bd04b228dd21aac801db6bdf74a6c35f8728962dd4b2c4801cead0ac95017bbe`；映射应用源码及构建输入，不将后加的追踪脚本伪装为该Gradle输入。

| 程序（授权C隔离区`programs/editor-one-click-ai`） | 字节 | SHA-256 |
| --- | --- | --- |
| studio-core.jar | 70,111,356 | 99553fff1f4d4f025773d212e19bea4807d7b82fcd7caad43eb18fd1ce8a88e2 |
| studio-desktop.jar | 5,470,454 | 5f60827f8ee57ba768377eba01a9fcd8827141b0e34111ad3f0404a73c70cf87 |

正常Alt+F4保存关闭旧完整候选PID1956，确认退出后才重开同一受保护审阅资料；没有强杀/重置/复制/检查用户所选原图。59输入（58生产JAR+args）逐项核验，生产MainKt、`--secondary-workspace --pause-collections`，没有test宿主或jpackage启动标记。**PID33392 / 窗口“CCB Studio Suite · 工作台”已实际出现，8秒后存活/有响应，stdout/stderr均0字节**；只是启动状态核对，新一键人工PENDING。旧失败/旧非AI限定人工通过不重测、不覆盖；不宣称跨所有机器或全部编辑验收。

当前只需新流程人工：编辑→“AI 打码设置”→首次选择原本本地模型→保存；之后点“AI 自动打码”检查直接生效与一次撤销，换图直接点；正常重开后应继续沿用配置。实际准确率不足仍可迭代，其他已通过项不用重测。

### 来源及持续追踪

已沿`references/PUBLIC_SOURCE_LINKS.md`、`docs/SOURCE_LOCK.json`、`docs/07_SOURCE_MAP.md`加入`duxingz/unified-image-tool`，固定`ed42f1f459dde621086f7763c36a46d768d091cb` / `v1.22.2`；原锁条目保持。`references/UPSTREAM_TRACKING.json`和`tools/Check-Upstream.py`比较公开commit/release/资产散列/许可，错误保留基线，相同状态不重复通知，不自动采用/下载/运行或修改git remote。

3个合成Python用例实际PASS，原始`upstream-evidence/unit-tests.log`与`RESULT.json`保留；公开手工检查确认同SHA/tag/资产散列，许可null。首次Python URLError保留，改走Windows现有验证HTTPS栈后RECOVERED；未改证书/系统信任。heartbeat **studio / Studio 上游变更检查**已读回ACTIVE，每天09:00 Asia/Tokyo，只有新变化/故障/恢复才提醒，**首次定时执行尚未观察**，不代签调度验收。详情/命令见公共引用说明；追踪工具的源码/结果另行封存，不与应用测试混算。

当前 **ONE_CLICK_AI_IMPLEMENTED_AUTO_VERIFIED_FULL_APP_OPENED / NEW_FLOW_HUMAN_PENDING / NO_RELEASE_OR_DAILY_CHANGE**。像素分割/局部橡皮擦、模型兼容/准确率/再分发许可、旧编辑未覆盖场景、资产完整人工、115体验、Prompt对比及旧独立/失败/用户否决保留。旧失败进程/错误窗资源问题没有因此宣称关闭。完成编辑改善收尾、固定实际交接后再迁移新对话，不自动创建或发布。下方为旧阶段事实。

---

## 2026-10-27 非AI新增项用户通过；改用完整应用试用AI（历史阶段）

2026-10-27 22:02 JST补记：用户要求重开。原完整候选PID24936仍在后台、有响应且日志为空，随后由agent正常关闭，用户澄清“如果是你预料之中的关闭那就没事了”；该次退出不登记为已证实崩溃。已按原固定58JAR/相同受保护资料目录重开，PID1956，5秒后仍运行且有工作台窗口；新独立日志`full-application-review/restart-20261027T130205840Z`，旧日志保留。0源码变化/0新测试，不重测已通过项；AI继续由用户试用并按需截图，不再代用户操作窗口，未发版/改日常118。

用户明确“其他的通过了，AI识别……没测成”，记 **NON_AI_R3_USER_REPORTED_PASS_WITH_SCOPE / AI_NOT_TESTED**。对应已给的限定步骤：图层规整/手柄拖序及撤销、切图工具/tab/参数继承、分类参数/色盘/唯一选择栏、单向竖排引号；不扩大为所有编辑/资产/导出人工通过。原回执 `.local/editor-native-20261027/review-candidate-r3/USER_FEEDBACK_20261027.json` 保留。

用户随后确认“先整合到整个应用”“AI打码不符合要求再改”，本次采用完整Studio的**隔离本地试用**，AI效果不再作为本次整合前置阻塞，但不把它记为已验。沿UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001，无新任务ID；发布/日常118禁止不变。当前不是新发行版本，也没有重新打包NSIS。

### 专用审阅入口的两项真实失败

1. 编辑专用host没有外部图片导入入口。仅在test宿主补“导入测试图片”，复用生产`c.importImages()`；`20261027T123106743184Z-editor-ai-review-import-entry-20261027` 1个新合成场景PASS（0失败/错误/跳过、sourceStable）：取消不改工程、原字节导入/追加、Constant ONNX管线/应用/撤销。**该用例注入选图结果，不验证真实Windows文件框，不能替代后面的原生失败**。生产JAR/依赖未改，旧通过不重跑不累加。测试fixture77项/237544字节留证退役。
2. 首次host封包漏`ComposableSingletons$EditorReviewCandidateKt.class`，用户收到NoClassDefFoundError；失败9类host/原args/日志/清单保留。补齐10类后`jdeps --missing-deps`通过；随后用户实际点击选图又收到“Native file dialog requires the attached main window”。这是专用host漏`StudioFileDialogs.attach`，不是完整应用当前Main缺这行。用户截图/原生栈在`review-ai-import/native-stderr.log`和`review-ai-import/reopened/native-stderr.log`对应留存；未改为PASS。没有继续修改已通过的编辑功能来遮盖宿主问题。

原失败无窗口PID7332仍保留、未强杀；AI专用旧窗曾尝试正常关闭，但电脑控制的element缓存不可用、键盘确认后仍有错误框，**未确认关闭**。不操作日常程序、不结束无关进程。旧合同禁止强杀残留进程，此资源收尾仍OPEN。上轮Esc中断保留；本次用户明确继续后才恢复电脑操作。没有把仍运行/失败资料当垃圾。

### 实际完整应用候选

- 正常生产入口`io.github.misakapiano.ccbstudio.desktop.MainKt`，完整Workbench/图库/导入/编辑/AI均为真实应用页面；启动classpath为58项生产运行JAR，**不含test宿主JAR**。
- 生产core SHA256 `29ada8c58d8215bb4e31ece626360552d1945de242872845170f0a10122a4fdf`，desktop SHA256 `0952f7b8dc8a8ddcbbe1c90f02677c367f4c944638df6238a6a59d0294329449`，与用户已试用R3完全相同。本次0新Gradle/编译/测试，不将启动核对算新回归。
- 已检查固定JAR实际字节码中的`StudioFileDialogs.attach`及对应生成onDispose类中的detach，见`compiled-main-file-dialog-binding.txt`和`compiled-main-dialog-detach.txt`。首次核对脚本把detach也要求在主类，断言中止且尚未seed/启动；`FIRST_PREFLIGHT_ASSERTION.json`保留，随后改为检查编译器实际生成的dispose类。不是应用功能失败，未隐去该准备错误。
- `.local/editor-native-20261027/full-application-review/CANDIDATE_INPUT_MAP.json`固定58JAR/args、实际源码基点89be225、原源码map及窗口绑定依据；`LAUNCH.json`、`LOCAL_REVIEW_STATUS.json`、空stderr、系统窗口观察对应PID24936、窗口“CCB Studio Suite · 工作台”。**只确认实际原生完整窗口已打开，文件框/AI效果仍待用户试用**；没有自动视觉/所有页面人工通过声明。切到完整窗口后无内容快照请求因API要求至少text或screenshot而拒绝，未把失败当视觉通过。
- 入口`.local/editor-native-20261027/full-application-review/Studio-Local-Review.lnk`。独立资料`.local/disposable/editor-native-review-42e8fbda39a14b7ca6418a75cce139d9/payload/workspace`，runtime同payload，初始12张合成风景/0账户。`--secondary-workspace --pause-collections`，未设置jpackage.app-path；按既有启动代码不自动联系更新服务，也不接管默认入口/清理日常程序。没有读取真实凭据、私人图库或调用真实生成服务。
- 新完整审阅根、先前AI导入根均已加入`.local/PROTECTED_ARTIFACTS.json`并有`MANUAL_REVIEW_RETAIN.json`：可能由用户手选导入的图片/路径不收进测试证据、不自动清理，后续保留至明确处置。agent不浏览或复制用户所选原图；源码/公开文档仅记录隔离根与构建信息。所有旧人工工程/失败仍保留。

使用正常流程：顶部“图库”→“导入图片”→“导入图片文件”，选图后底部“进入编辑”（或编辑里“从资产库追加”）。进入编辑后“AI自动打码”→选择本地模型`C:/CCB-Studio-Suite-Editor-20261027/models/nsfw-anime-medium-x1280.onnx`→当前页/类别/参数→开始本地识别→预览勾选→应用。模型SHA仍a12ac553…7a828；纯本地CPU，不需要账户或远程服务。0区域不能据此确认识别正确。检测框覆盖、非像素分割/局部橡皮擦，实际效果、模型许可/正式分发仍OPEN。

当前 **FULL_APP_LOCAL_REVIEW_OPENED / NON_AI_R3_USER_PASS / AI_ITERATIVE_TRIAL_PENDING / NO_RELEASE_OR_DAILY_CHANGE**。资产完整人工、旧编辑未覆盖场景、115体验、Prompt对比、旧独立/失败状态分别保留；本次不重做已确认人工，编辑阶段交接完成后再迁移新对话，不自行创建对话或发布。

---

## 2026-10-27 连续编辑、图层拖序及本地 AI 候选 R3（已打开，人工待验）

用户对 R2 回答“基本上通过”，仅记 **USER_REPORTED_BASIC_PASS_WITH_FOLLOWUPS**，不等于完整编辑验收或 Project 独立验收。本增量仍沿 UX-RECOVERY/W4、UX-R2、UX-EDITOR-GAP-001：连续工具/属性、分类参数、色盘、单组选中操作、单向竖排引号及规整图层/明显拖序，随后接入用户明确要求的本地 AI；不扩大到其余完整图片管线。源码基点仍 `89be2250cbf806632a4d6eed149ecdce5a3672b9` / `feature/windows-rc-continuous`，**本地未提交/暂存/推送，无版本、安装器、发布、日常118或信任链改动**。下方 R2 的“尚待反馈/AI未接入”保留为历史阶段。

### 本轮实现与边界

| 项目 | 实际行为与关键源码 | 状态 |
| --- | --- | --- |
| 连续编辑 | `TypesetEditorController.kt` 持有工具和属性页；切图保留刚才工具/文字样式/完整打码设置，不用目标图片默认值覆盖当前工具；目标图片已有对象不改变 | 自动通过，新候选人工待验 |
| 打码参数 | `TypesetWorkspace.kt` 按效果/形状显示有效字段：马赛克块大小、模糊强度、画笔宽度，黑白条纹密度/厚度及马赛克/模糊笔点密度；实心矩形/套索不堆无效画笔参数 | 自动通过，非新增所有 unified 效果 |
| 色盘 | `TypesetColorPicker.kt`：二维饱和/亮度、色相/透明度滑条、常用色、预览；应用才改工程，取消不改；原十六进制/CSS输入保留 | 实际 Compose 控件通过，人工待验 |
| 队列选择 | `TypesetQueueWorkspace.kt` 唯一一排“全选 / 反选 / 清除”，删高级区重复全选/取消入口 | 自动通过 |
| 竖排引号 | `WindowsVerticalTypography.kt` 只在显示流把 `“ ” ‘ ’` 对应为 `﹃ ﹄ ﹁ ﹂`；原文、富文本偏移和原有 `『』「」` 不变，无反向转换 | 原生轮廓/原文与序列化回归通过 |
| 图层 | `TypesetLayerPanel.kt` 等高全宽行，左侧六点手柄/移动光标、明确插入线，名称和显隐/锁定对齐；底部2×2操作。松手只产生一条历史，可撤销；取消/页面或层序改变不提交拖序 | 真实指针拖序及撤销自动通过，已查看实际组件图；长列表拖动自动滚屏未新增 |
| AI | `LocalOnnxMaskDetector.kt`、`OnnxModelBoundary.kt`、`TypesetAutoMaskController.kt`、`TypesetAutoMaskDialog.kt`：Java ONNX Runtime CPU 本地识别，单批复用会话并关闭，类别/置信度/IoU/分辨率/外扩，当前或所选页，先预览勾选，再一次可撤销应用；不改原图、不上传、不调用服务 | **本地检测框覆盖已接线/自动通过；准确率、像素级分割及局部橡皮擦未验/未完成** |

模型只由用户明确选择本地文件，本次启动复用；不自动下载、不持久写入工程或导出模型。限定兼容含 `names` 类别元数据的 YOLO NCHW float32，128MiB输入上限、输入尺寸及候选数有界；在调用原生运行时前拒绝外置张量路径/训练图/本地函数库。未知类别顺序不猜测。取消在原生推理返回后停止后续页，不把中断结果自动写入工程；应用前拒绝过时工程快照。兼容性/各种设备资源上限和模型识别准确率没有全面验证，不宣称任意模型安全或适用。

新增锁定依赖 `com.microsoft.onnxruntime:onnxruntime:1.31.1`，JAR 50,942,631字节、SHA256 `854b4fae9b5202371eb8cd978d2e872eafb75664ef2db2f3c91f2b095b518b62`；`modules/studio-desktop/build.gradle.kts`、`gradle.lockfile` 和 `gradle/verification-metadata.xml` 只补该组件。官方 v1.31.1 MIT LICENSE / ThirdPartyNotices 收入 desktop resources/licenses；没有改更新信任配置。**尚未重新核验发行安装包体积/签名/公开升级链**，当前不是发行候选。

### unified-image-tool 固定对照与后续差距

只读对照 commit [ed42f1f459dde621086f7763c36a46d768d091cb](https://github.com/duxingz/unified-image-tool/tree/ed42f1f459dde621086f7763c36a46d768d091cb)，重点 [README](https://github.com/duxingz/unified-image-tool/blob/ed42f1f459dde621086f7763c36a46d768d091cb/README.md)、[detector.py](https://github.com/duxingz/unified-image-tool/blob/ed42f1f459dde621086f7763c36a46d768d091cb/app/core/detector.py)、`effects.py` / `image_ops.py` / `tool_panel.py`。本地只读副本 `.local/editor-unified-reference-20261027`；没有复制对方实现进生产源码。

| 能力 | Studio 当前对应 | 差距/取舍 |
| --- | --- | --- |
| 本地分割、类别选择及阈值/外扩 | 本轮真实 ONNX 推理/可选类别和参数，检测框进入可编辑对象 | 分割原型尚未转为逐像素掩膜；不能写成等同原版分割 |
| 自动+手动结果局部橡皮擦 | 可编辑/隐藏/删除整个打码对象、撤销 | 局部擦除仍 OPEN，不能以删除整个对象冒充 |
| 模糊/黑线/白雾/光效/马赛克 | 现有 TypeSet 马赛克/模糊/黑白覆盖及条纹画笔 | 白雾/光效、方向黑线尚未整合，不在本次默默全量加入 |
| 图片与文字水印、九宫格锚点/比例/不透明度 | 现有文字框/命名预设 | 图片贴图与统一锚点水印管线待定 |
| 批量格式/缩放/重命名/水印 | 已有队列批量编辑、格式/范围/编号前缀/文件夹ZIP及小人书导出，原件字节副本单列 | 统一批量缩放/水印管线未完整实现；不把已有导出能力重复造轮子 |
| 文件夹导入、连续浏览、撤销/原字节快拷 | Suite 图库已有目录采集，编辑已有队列/查看器/历史和原件副本 | 不登记为“全应用缺失”；编辑直接目录入口是否合并另定 |

优先建议为补像素掩膜与局部擦除，再评估图片水印和统一批处理；这些未自动启动。源码仓库/模型未核实到适用的再分发许可，**不打包发布参考模型或复制未知许可代码**。本次用户只授权 C 隔离目录内模型合成验证：公开 v1.22.2 archive API SHA256核验、仅取ONNX、不执行外部EXE；当前测试模型 `nsfw-anime-medium-x1280.onnx` SHA256 `a12ac5532e93be9dfeb96a77fc3f3647791335c9df0de9a18fcd503f7877a828`，来源/解包项在 `C:/CCB-Studio-Suite-Editor-20261027/models/MODEL_PROVENANCE.json`。仅当前本地测试，不随候选程序捆绑；正式模型来源/授权与准确率仍 OPEN。

### 自动证据与失败（不累加重复项）

Windows10 x64 / Temurin17.0.20.1 / Gradle9.1.0 / Compose1.10.3 / ONNX Runtime1.31.1。原串行单worker、离线严格依赖校验，运行时缓存限定用户授权 C 隔离目录；不改变全局缓存/系统配置。候选 `VALIDATION_CASE_MAP.json` 映射 **9个唯一场景**：9项阶段PASS，末尾仅修 AI 小数显示再复核其中1项PASS，不能合计为10项或重新累加此前R2场景。

| 证据目录（均在 `.local/windows-preview/evidence/`） | 实际执行 |
| --- | --- |
| `20261027T113243139745Z-editor-continuous-tools-palette-layers-20261027` | 核心引号/连续设置2PASS，组件图层拖动1FAILED；整阶段FAILED |
| `20261027T113931586585Z-editor-layer-grip-hit-target-20261027` | 修复整行clickable合并手柄命中，真实手柄拖序/色盘等1PASS，sourceStable |
| `20261027T115242797932Z-editor-ai-native-wiring-20261027` | AI弹窗缺导入，编译FAILED，0JUnit |
| `20261027T115510948891Z-editor-ai-native-wiring-import-repair-20261027` | ONNX依赖锁缺项，FAILED，0JUnit；未关闭strict校验 |
| `20261027T115759340382Z-editor-ai-native-locked-wiring-20261027` | 新JUnit方法返回类型错误，initializationError；不是推理用例已执行 |
| `20261027T120058828357Z-editor-ai-native-execution-20261027` | 修正后真实ORT/控制器/外置路径拒绝/公开模型合成输入4PASS |
| `20261027T120609286568Z-editor-ai-native-ui-and-runtime-20261027` | 修正NMS在外扩前执行、冻结结果参数后4项及真实弹窗1项PASS，并补齐runtime锁域 |
| `20261027T121136842779Z-editor-r3-locked-final-20261027` | 无update-locks的离线strict，核心1+桌面8实际9PASS，0失败/错误/跳过，88.199秒、sourceStable |
| `20261027T121401048684Z-editor-r3-ai-parameter-display-20261027` | 实际组件发现0.300000011…显示不整齐，修显示与断言后只复核AI弹窗1PASS，52.266秒、sourceStable |

最终9项覆盖：单向原生引号；切页连续设置且不覆盖页面配方；真实色盘/分类参数/唯一选择排/图层手柄拖动/撤销；真实AI弹窗加载/推理/预览/选择/应用/撤销；真实ORT预处理/NMS/外扩/取消；外置权重拒绝；过时快照/幂等应用/原图散列；公开模型只对生成的抽象图推理；宿主合成资料/禁止覆盖重置/无账户。自编Constant ONNX只验证管线，不冒充识别能力。真实公开模型对320×240抽象合成图返回0区域，最后样本1.3860977秒含会话创建，**recognitionAccuracy=NOT TESTED**；不是人体识别命中率或所有机器性能承诺。

收尾 `git diff --check` 另发现 `TypesetWorkspace.kt:152` 一行纯空格（本编辑增量引入，R2没有）；当前固定运行输入保留该行，列为格式警告，记录 `review-candidate-r3/DIFF_WHITESPACE_CHECK.json`，不宣称全部检查零问题。

每阶段原始XML/gradle.log/summary/source-before保留；当前44项改动完整副本和逐文件hash在最后阶段 `FIXED_SOURCE_MAP.json`，额外固定锁文件、校验元数据、本地runner/init脚本，构建期间一致。旧阶段封存时宿主文字已变导致首hash检查中止，随后使用旧已固定且hash匹配的宿主副本完成封存，记录 `120609.../FREEZE_NOTE.json`；不是篡改旧输入。该阶段patch仅诊断，重建以固定source map为准。旧失败/用户否决/Project结论从未改为通过。

### 固定本地候选与人工步骤

候选目录 `.local/editor-native-20261027/review-candidate-r3`；`CANDIDATE_INPUT_MAP.json`、`VALIDATION_CASE_MAP.json`、`LOCAL_REVIEW_STATUS.json`、`LAUNCH.json`、`native-events.jsonl` 与 `Start-Editor-Review-r3.lnk`。59个运行JAR和参数逐项校验后启动，实际 OS窗口和绘制/显示/工程打开事件已确认，PID31916，标题 **Studio 编辑本地审阅 · 连续工具与AI · 非日常118**。本次无鼠标/键盘输入，不关闭仍存在的旧R2窗口/不重置旧工程；弱无障碍树只给系统外框，未重试此前超时的窗口截图。实际离屏图层/AI弹窗组件PNG已查看，**不是可见原生视觉/性能或用户通过**。

| 固定生产文件（位于用户授权 `C:/CCB-Studio-Suite-Editor-20261027/programs/editor-review-r3`） | 字节 | SHA-256 |
| --- | --- | --- |
| studio-core.jar | 70,097,166 | 29ada8c58d8215bb4e31ece626360552d1945de242872845170f0a10122a4fdf |
| studio-desktop.jar | 5,449,850 | 0952f7b8dc8a8ddcbbe1c90f02677c367f4c944638df6238a6a59d0294329449 |

最终源码map SHA256 `5d75e68bb67998f75a52de26e9727d7d70bdf92946be55a02b0a23291954fd02`。合成资料 `.local/disposable/editor-native-review-07179e339f6b4a2c8898de990fd82739/payload`，12张1216×832风景、无账户/真实凭据、原件散列清单；服务请求禁止，正式主入口/更新器未运行。

只检查新增项，不重做此前R2基本通过：

1. 第二页“图层”：按住六点手柄拖一层，看插入线、松手顺序/叠放是否改变，再撤销；显隐/锁定按钮对齐且不跳tab。
2. 选画笔/打码效果与参数后切到下一图片：工具、属性tab和设置继续沿用；新绘制使用当前设置，原对象不被换样式。
3. 对照马赛克/模糊/黑白画笔的参数是否对应；点颜色的“色盘取色”，试应用/取消；队列只一排全选/反选/清除。
4. 第一页输入的弯引号显示为竖排引号，原有『』「」保留；正文内容不被反写。
5. AI入口可选择 `C:/CCB-Studio-Suite-Editor-20261027/models/nsfw-anime-medium-x1280.onnx`，加载/设置/识别/返回；仅合成风景可能0区域，不因此确认准确率。正检出勾选应用/撤销已用明确Constant合成模型自动验证，未把它伪装为实际识别器。无需私人图片或真实服务。

本次新成功fixture已按owner/封存证据退役（84/282/41项；更早4项推理阶段43项），失败/人工工程/固定基线保留。C授权区域内仅移除已核对hash的公共ZIP、未选用备用模型和重复下载的ORT JAR共3文件292,138,325字节；当前模型、候选运行输入/缓存和所有来源记录保留，`OWN_TEMP_RETIREMENT.json` 可追溯。没有扫描/删除整盘、回收站、日常安装、备份或归属不明文件。

当前状态 **LOCAL_R3_OPENED / AUTO_PASS_WITH_SCOPE / NEW_HUMAN_PENDING / EDITOR_FULL_ACCEPTANCE_OPEN / INDEPENDENT_NOT_REQUESTED**。局部橡皮擦/像素分割、真实识别准确率/模型再分发、发行包验证、资产完整人工、115体验、Prompt对比及旧未关闭独立事项分别保留。编辑改善签收/固定交接后才迁移新对话，不提前新建或自行发布。

---

## 2026-10-27 二次修正本地候选已打开，等待限定人工反馈

当前状态 **LOCAL_REVIEW_OPENED / USER_ACCEPTANCE_PENDING / EDITOR_NOT_ACCEPTED**。用户最新问是否要避开鼠标；已告知目前不再操作窗口，可以正常使用电脑并试用新窗。旧审阅PID28212已通过Alt+F4正常关闭，日志`NORMAL_CLOSE_ORIGINALS_UNCHANGED`，旧合成工程和程序/证据保留。新窗口“Studio 编辑本地审阅 · 二次修正 · 非日常118”，PID21904，1320×900；`WORKSPACE_OPENED`、`NATIVE_WINDOW_DRAWN`、`NATIVE_WINDOW_SHOWING`及`EDITOR_OPENED`已记录，无服务请求。两次实际窗口捕获均报`window capture timed out: timed out waiting on channel`；离屏真实组件截图已查看，但**不把窗口标题/绘制事件当成人工视觉或操作验收**。后续不再代用户操作此窗。

### 实际修正与固定输入

- `WindowsVerticalTypography.kt`用DirectWrite排版、字体原生竖排方向和轮廓；`TypesetLayout.kt`及`TypesetRenderer.kt`量测/绘制同一轮廓。字体/富文本/下划线删除线/换列/空文本/导入字体、移动复用有合成检查；有界缓存最多12项且限制轮廓段数。原文不全角化。只支持当前vertical-rl/mixed，不声明复现CSS全部模式或旧TypeSet手工字形像素。
- `TypesetControls.kt`、`TypesetWorkspace.kt`按固定demo分区、紧凑双列属性/固定四标签、底部适应画布和缩放；字体菜单与按钮同为12sp。旧全部操作入口保留在对应分组。队列选中背景统一为较深强调底色。
- `TypesetQueueWorkspace.kt`保留真实长队列滚动、多选拖序/边缘自动滚动/独立预览，滑条与Ctrl滚轮改变缩略图尺寸，分割线拖宽在松开时一次保存；点击队列实际获得键盘焦点。
- `TypesetImageViewport.kt`用于完整预览/导出预览：鼠标指向缩放、拖动平移、悬浮前后按钮、左右键/0/F/加减/Esc；视图状态不改编辑页/原图。
- `TypesetClipboard.kt`为画布对象和队列页提供显式剪切/复制/粘贴、撤销/重做等；锁定对象不能剪切，写剪贴板失败先保留原对象，页引用限定同一工作区；输入框不走对象快捷键。回归为新旧Compose剪贴板API注入独立合成内存剪贴板，不读取系统私人内容。
- 旧流程发现并修复真实交互缺陷：选择图层后自动跳到文字属性，导致解锁入口消失。显式打开的图层面板现在保持，选新打码工具则进入打码属性。

源码基点仍为`89be2250cbf806632a4d6eed149ecdce5a3672b9`、`feature/windows-rc-continuous`，模块改动未提交/暂存/推送；不是新源码commit或发行版本。最终完整逐文件源码副本与散列：`.local/windows-preview/evidence/20261027T051828602960Z-editor-revised-layer-selection-20261027/FIXED_SOURCE_MAP.json`（SHA256 `562878ffeb6c15618fbf6be64d579abe1c2f54e1d83fee6b6bb244738a6b9f8b`）。

构建映射`.local/editor-native-20261027/final-r2/BUILD_INPUT_MAP.json`：

| 文件 | 字节数 | SHA-256 |
| --- | --- | --- |
| studio-core.jar | 70,096,599 | aa6ba06380960f732ab8ffce3b79911ec227e4fabdabfc3c7ed66a53c03b7390 |
| studio-desktop.jar | 5,283,326 | 98e52fa44ace74289b215f0f7bda65809f094f2238aebb5f390e5596722937b2 |

候选目录`.local/editor-native-20261027/review-candidate-r2`：`CANDIDATE_INPUT_MAP.json`固定62个运行时JAR/9个测试宿主类/参数文件，`VALIDATION_CASE_MAP.json`逐例映射，`Start-Editor-Review-r2.lnk`仅工作区内入口；`LAUNCH.json`和`native-events.jsonl`为启动证据。只含12张1216×832合成图/合成工程，无账户；原件清单位于独立owner根`.local/disposable/editor-native-review-ee1dc174debb4e36b33a386dbfe3c14e/payload/originals.json`。入口/全部运行输入逐个散列核验后启动，未触及日常118/系统桌面入口。

### 自动验证及失败保留

Windows10 x64 / Temurin17.0.20.1 / Gradle9.1.0 / Compose1.10.3，原串行隔离执行器、离线严格依赖校验。**12个核心及11个桌面唯一场景具有逐例PASS证据，分阶段复用；不是一次最终全套执行，也不把重复执行相加。** 完整原始XML、命令/环境/源码散列见候选的`VALIDATION_CASE_MAP.json`：

- `051054...editor-interaction-native-revision...`核心12实际PASS，当前核心逐文件与该次输入完全相同；整体阶段因后续桌面新测试编译失败仍为FAILED，保留原记录。
- `051346...editor-interaction-native-focus...`新交互3 PASS，固定源码/原始布局及查看器图保留；该次布局测试主题宿主未包Surface，文字取色不代表实际候选，已修测试宿主。
- `051551...editor-revised-retained-workflows...`11实际执行，10 PASS、图层自动跳转1 FAILED；原始XML/固定源码/图像保留，不改为整体PASS。
- `051828...editor-revised-layer-selection...`只复核实际修正影响的布局/图层/竖排/命名预设/拖动/历史/重开2项，2 PASS。其余9个桌面场景复用前阶段，包括长队列拖序、缩略图/栏宽、焦点剪贴板、完整/导出预览、四分类范围导出、导入字体冷启动和审阅宿主归属检查。

两次成功fixture按owner封存退役115项/1,127,646字节和137项/1,476,947字节；失败fixture及旧候选/人工合成工程、固定源码、原始结果与失败视觉样张保留。没有清理用户资料或将失败隐去。

### 这次请用户只核对

1. 第一页竖排：中文直立、西文转向、标点/换列观感，字体/字号/框高修改；对照当前三栏排列和底部适应画布。
2. 队列：拖分割线、滑条/Ctrl滚轮、条目/网格切换和普通滚轮滚动；拖序后撤销，选中/当前页是否清楚。
3. 完整预览：滚轮缩放、拖动、悬浮前后、左右键与0/Esc；关闭后编辑页/多选不被切换。
4. 画布点选对象后试Ctrl+C/V、Ctrl+X、Ctrl+Z与Ctrl+Shift+Z；正文输入框内试同样文本编辑快捷键；图层选择/锁定/解锁入口不跳走。
5. 第二页已有9层打码，连续画笔/拖动时的实际手感。本轮自动引擎样本不代签真人顺畅，也不要求重测主题/日常更新。

AI检测/模型、局部橡皮擦和完整编辑人工仍OPEN；资产完整人工、115体验、Prompt对比、既有独立失败/用户否决保持。无安装器/tag/发版/签名或日常安装更新；编辑改善尚未签收，不提前迁移新对话。

## 2026-10-27 本地候选用户否决后的二次修正（进行中）

用户指出第一份可见原生候选竖排方式不是字体原生模式、排列与已确认demo差异明显、适应画布位置被改、左队列需拖宽及缩略图缩放，独立图片查看需滚轮/平移/悬浮前后/键盘，编辑需实际快捷键。该候选为 **USER_ISSUES_REPORTED / NOT ACCEPTED**；首次窗口可见和旧组件检查不提升为布局、视觉或手感通过。旧候选程序、其人工合成工程和事件保留，不覆盖、不重置。

本次仍沿UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001，不新开课题。采用三栏、单图编辑、左队列滚动的已确认方向。工具/工程条固定分组，右侧文字常用参数双列对齐，扩展项折叠，适应画布/缩放回到画布底栏。保留全部旧工程/导出/批量/预设入口。队列滑条和Ctrl滚轮缩略图调节、边框拖宽；预览局部视图变换，不改变当前编辑页/多选；画布与队列独立焦点分派对象/页面快捷键，输入框保持原生文本编辑。

竖排转为Windows x64 DirectWrite字体原生 vertical-rl/mixed：字体自身竖排字形/方向/度量，不用手动逐字旋转或把ASCII替换为全角来冒充。中文直立、西文整段转向，原文本、工程字节结构和存储标记不改写；已有竖排工程的新渲染外观会相应变化。旧TypeSet1.1的逐字符displayCh参考样本原件及此前结果保留；它们不再作为本次原生竖排像素一致性判据。横排/小人书仍承接原规则。**只接入当前竖排模式，不宣称CSS表中全部五种模式、所有字体/Windows环境均已验。** 本地导入字体由私有内存集合加载，不安装到系统、不联网取字体；预览和导出共用轮廓。

原始依据为W3C CSS Writing Modes4、Microsoft DirectWrite Vertical Text、Win64调用约定与微软公开SDK声明，声明/来源散列在`.local/editor-native-20261027/font-api-reference/SOURCES.json`。本轮无外部实现代码复制、模型下载、服务调用、私有图库读取、信任变化或日常118操作。

实际阶段（均源基点89be225，改动尚未提交/暂存/推送）：

- `.local/windows-preview/evidence/20261027T045408644439Z-editor-native-font-vertical-20261027`、`...045514120678Z-...vertical-repair...`：COM回调包装的编译失败，JUnit0；第二次本地修正脚本GBK读取失败，未把它算成新回归。
- `...045556957942Z-editor-native-font-vertical-callbacks-20261027`：2项执行FAILED，Win64按值POINT_2F回调ABI错误；保留原输入/栈。
- `...045951985232Z-editor-native-font-vertical-outline-20261027`：2项PASS，但实际样张CJK方向不对；**视觉FAILED，不能据2项测试宣称正确**。新增笔墨方向/边界断言，使用TextAnalyzer1原生方向矩阵修正。
- `...050326012882Z-editor-native-font-vertical-transform-20261027`：3项PASS，实际原生样张已查看；中文直立、西文转向，fixed-source/raw/XML保留。使用错误Python版本的首次fixture清理失败未删文件；改用项目已有Python3.13后按owner清单成功退役22项/303938字节，留证不覆盖。
- `...051054223108Z-editor-interaction-native-revision-20261027`：核心12项PASS（4字体原生、5受影响源参考、3缓存/完整渲染），桌面测试编译FAILED，整体FAILED；不相加为整套通过。错误在新测试宿主decode参数和局部函数格式，生产桌面编译已过。
- `...051209780976Z-editor-interaction-native-compile-repair-20261027`：3项执行FAILED；测试假设BasicTextField内容边界等于按钮边界、测试图片坐标字符串错误、Compose新剪贴板接口未注入。固定失败输入和原始XML保留。随后改为对照标签/输入中心并为新旧剪贴板API注入独立合成内存剪贴板，不读取系统私人剪贴板。

新交互场景正重新运行；尚无最终新本地候选或人工通过。AI检测/模型、局部橡皮擦、可见窗口实际性能/编辑完整人工均保持OPEN/NOT ACCEPTED；资产完整人工、115体验、Prompt对比及原独立失败/用户否决保持。不发版/更新日常/自动迁移对话。

# 编辑器验收：两份参考、三方面对照与交互重规划

2026-10-27，Asia/Tokyo；Suite基点89be2250cbf806632a4d6eed149ecdce5a3672b9／日常Preview118。本轮由用户实际试用触发：认为与TypeSet参考差异大，进一步指出主要是UI交互差且操作卡顿；明确选择**保留左队列／中央画布／右属性三栏，操作分组和行为对齐TypeSet**。这些是新反馈和布局选择，不是功能通过声明。

沿用UX-RECOVERY/W4、UX-R2、UX-EDITOR-GAP-001及既有编辑性能项，不新建重复开发任务。编辑人工状态由USER_NOT_TESTED进入**USER_TEST_STARTED_ISSUES_REPORTED / NOT ACCEPTED**；资产管理完整人工、115体验、Prompt对比、任务栏及独立状态各自保持。

## 2026-10-27 最新确认及原生实施开始

### 本地原生审阅候选已打开（2026-10-27 13:05 JST）

用户新授权“继续准备本地审阅候选，能自动验证先做，打开后给人工清单；不发布／更新日常，编辑收尾后迁移”。本次没有新增生产编辑逻辑；只新增test源 `EditorReviewCandidate.kt` 与 `EditorReviewCandidateTest.kt`，前者以真实`TypesetWorkspace`包成单独Windows审阅窗口，不装入发行JAR，不暴露服务／更新页，不运行正式启动器或改变默认入口。初始12张1216×832合成风景、无账户和真实凭据；第1页竖排，第2页九层打码，第3页横排与图层，其余用于队列滚动；带两种命名打码与一种文字预设。

新增1项隔离回归实际PASS（0失败／错误／跳过，sourceStable）：准确fixture根／owner约束、拒绝重置已有资料、12张合成资产与对象、无账户、预设和修改旁白前后原图散列。证据 `.local/windows-preview/evidence/20261027T035957025484Z-editor-local-candidate-host-repair-20261027/`，使用原runner筛选`*EditorReviewCandidateTest`及`nativePerformanceInputs`；未重跑前一增量方法或累计为新方法。首轮 `20261027T035819618648Z-editor-local-candidate-host-20261027` 因新审阅宿主缺导入与闭合括号而编译FAILED、JUnit0；原输入及日志保留。成功fixture108项／1,551,703字节已留证退役，失败fixture保留。

固定候选映射 `.local/editor-native-20261027/review-candidate/CANDIDATE_INPUT_MAP.json` 包含审阅host的9个class、约37KiB host JAR、62项运行JAR路径与散列、参数文件、源码map；生产core／desktop仍为上节固定散列。复用预装JDK17和已锁定依赖，本地保留的生产JAR不变，没有新installer／版本／tag或公开产物。`Start-Editor-Review.lnk`仅保存在该候选目录，未写桌面／开始菜单／任务栏。入口最初未加引号的`@argfile`字符串在COM读回时为空，校验阻止启动；失败快捷方式字节保留为`.bin`，明确加引号后读回并经真实快捷方式重开成功，深层原因未独立诊断，不推论为正式更新器缺陷。

第一次Windows可见宿主PID26332正常打开，通过正常Alt+F4关闭，`NORMAL_CLOSE_ORIGINALS_UNCHANGED`记录及进程退出确认；第二次通过真实本地快捷方式重开PID31588，记录`NATIVE_WINDOW_DRAWN`、`NATIVE_WINDOW_SHOWING`1320×900和`EDITOR_OPENED`，唯一标题 **Studio 编辑本地审阅 · 20261027 · 非日常118** 已激活并留给用户。事件日志 `native-events.jsonl`、入口读回、两次启动记录、原始标准输出／错误以及 `LOCAL_REVIEW_STATUS.json`留存。原图初始散列在合成payload内；在进行的用户审阅资料不封存清理。

电脑控制首次截图报 **FrameArrived timed out: timed out waiting on channel**；按技能恢复后只能读取OS窗口标题和控件，Compose内容未暴露，关闭按钮坐标动作报 **coordinate input geometry is unavailable**，重新读取窗口后改用正常Alt+F4成功退出。未强杀。**这是原生启动／退出／重开核验，不是自动视觉通过或端到端性能测量**；未改用私有截图接口规避。完整人工仍NOT ACCEPTED。

人工只检本轮编辑，不重测旧主题／更新：

1. 队列：条目／网格切换和滚轮，多选拖序，撤销／重做，独立预览关闭后当前编辑页／选择保持；长拖序与Esc已有自动检查，人工主要看反馈是否清楚。
2. 第1页／第3页文字：选择现有对象、改内容／字号／横竖排、拖动与画布平移，适应画布；确认操作分组、显示和选中反馈顺手。
3. 第2页：连续画笔、矩形、圈选，切模糊／马赛克、调强度、撤销；重点反馈实际卡顿位置及发生在按住绘制、松开后还是切页。引擎热帧数据不能替代手感。
4. 预设与批量：保存自定义名称并应用，设新建默认不改已有对象；选两页仅应用文字或打码，确认另一类与正文／位置保持。关闭重开后保留修改；重开入口在候选目录，复用同一合成工作区，绝不重新seed。
5. 预览／导出：任选两页，PNG四分类及ZIP导出一次，核对所选页／编号／分类名称和预览；导出到此payload的`review-exports`即可。原图副本另选、PDF整册含完整图页的自动／人工状态分列，不要求此轮人工穷举全部格式。

候选数据根：`.local/disposable/editor-native-review-16f57429807c485292ed167901e20b06/payload/`。可自由修改这里的合成内容；不引导用户导入私人图片或真实账户。AI自动打码与局部橡皮擦未接入不要求点测；其他完整编辑／资产人工、115体验、Prompt、旧失败及独立状态保持。状态 **OPEN_FOR_USER_REVIEW_NOT_ACCEPTED**，暂不迁移新对话。

### 本轮原生实现与自动结果（当前；以下执行中记录为历史）

状态为 **NATIVE_COMPONENT_IMPLEMENTED_WITH_OPEN_GAPS / EDITOR_NOT_ACCEPTED**。用户批准的是第三版UI和左队列滚动／中央单图方向，不是新原生程序验收。基点仍为 `89be2250cbf806632a4d6eed149ecdce5a3672b9`，`feature/windows-rc-continuous`；本轮模块改动未提交，未暂存、push、打包、发版或更新日常118。不能把这个旧HEAD当成包含下列新实现的commit。

实际实现：

- `TypesetQueueWorkspace.kt`：条目／网格、独立滚动条、缩略图或手柄单多选拖序、行列插入目标、边缘滚动、Esc取消、松开才提交、独立预览。切视图不改工程或历史，复用原选择、编号、批量移动及撤销。
- `TypesetWorkspace.kt`／`TypesetIcons.kt`：图标＋文字工具、明确对象移动／画布平移；文字、打码、图层、预设分组和选择联动。原工程新建／追加／交换／管理、全图预览、历史及小人书入口保留并重分组；适应画布只复位视图。批量操作显示目标页数和文字／打码／两者，上一页样式仍取操作前快照。
- `TypesetPresets.kt`／`TypesetEditorExtras.kt`：文字与打码命名预设保存／应用／覆盖／改名／删除／新建默认；保留TypeSet四个旧打码槽。新建默认不改已有对象，现有对象应用保留正文和几何。复用不可变预设交换资产、原journal的`sourceExchangeId`，不新增全局状态字段／资料版本；未知源字段与历史快照继续保留，重开／备份恢复／合并验证。
- `TypesetExport.kt`／`TypesetExportDialog.kt`：新增`plain`表示按所选格式编码的无编辑页面，旧API的`original`继续原字节复制。UI四类完整页序列为01原始排序／02仅打码／03打码并嵌字／04仅嵌字，原图副本另选；文件夹、ZIP、范围、格式、前缀、元数据与Recipe保留。小人书完整图片页与整册PDF明确分开，PDF启用时固定整册，图片页同时保留；工程／预设交换有独立入口。新分类或前缀使用本地导出回执v4，旧请求仍v3；这不是更新协议、TUF或工作区格式升级。新回执旧程序不保证可验证。文件前缀及回执目录白名单、路径检查和文件散列继续校验。
- `TypesetPreviewSession.kt`：单个串行、合并预览请求，原图解码复用与未变图层前缀缓存；复用原TypeSet文字／打码渲染原语及不可变原图采样，渲染异常丢弃部分写入，避免错误重试污染。未改Prompt／生成请求或正式更新逻辑。

自动证据根为 `.local/windows-preview/evidence/`，每个阶段保留 `summary.json`、逐例 `test-cases.json`、原JUnit XML、`gradle.log`、执行参数及源码前后散列；成功阶段另有`FIXED_SOURCE_MAP.json`／`fixed-source/`／`tracked-change.patch`与`raw/`截图。**核心JUnit和真实Compose组件离屏场景不等于Windows可见窗口、人手试用或独立验收**。表内阶段不相加为独立总数，尤其命名预设3项因后续交换字段变化在导出阶段重验。

| 阶段目录 | 实际结果 | 覆盖与限制 |
| --- | --- | --- |
| `20261027T030218702122Z-editor-ui-first-native-20261027` | 7 PASS，0失败／错误／跳过，sourceStable | 缓存3、队列／独立预览／拖序／取消及分隔4；含合成1216×832九遮罩笔划样本 |
| `20261027T031946827873Z-editor-named-presets-20261027` | 整体FAILED；核心4 PASS，桌面编译失败 | 保存原失败：误将返回Unit的操作调用`join`；保留失败输入，不计桌面通过 |
| `20261027T032100874546Z-editor-named-presets-ui-repair-20261027` | 1 PASS，sourceStable | 修正为等待实际活动任务后，真实预设组件CRUD／默认与对象区分通过；不重跑上阶段核心凑数 |
| `20261027T033901823513Z-editor-categories-batch-20261027` | 11 PASS，0失败／错误／跳过，sourceStable | 核心10含导出3、批量4、受变更影响预设3；组件1验证范围／各类完整序列／命名／独立选择与重开 |
| `20261027T034246651794Z-editor-retained-workflows-20261027` | 8 PASS，0失败／错误／跳过，sourceStable | 仅受影响旧编辑流程：文字连续样式、竖排与拖动锁定／撤销重做、手动三种打码、WebP、四分类剪贴板／Recipe、宽窄预览、导出中断／显式重试、工程与预设交换 |

执行统一使用 `tools/Run-PreviewChecks.py --phase <上述phase> --keep-fixtures -- ...`，完整实际Gradle参数／筛选在各阶段summary中；Windows、JDK17.0.20、Gradle9.1，离线严格依赖校验、单worker／单测试fork、低优先级，无真实服务。旧测试只按新入口／分类语义调整选择器和期望，未删历史行为断言。当前生产／测试源码固定副本以最后阶段的source map为准。`.local/editor-native-20261027/final/BUILD_INPUT_MAP.json`绑定最后源码输入及保留的编译JAR：core SHA256 `ac623c19dfbec3202f8dc3ee97e9db14fc216fa08b80b11899fc5092ddeee87e`，desktop SHA256 `06a6159efa09253516336f906189592886a9e9cf4674de6a3a76abdef45e9163`；不是安装器或发布候选，不可写成Preview119。

性能只报告首阶段固定工程样本：8帧全量渲染中位89.089ms，缓存热帧15.4555ms，首缓存帧74.8284ms；累计渲染图层80→17且逐像素一致。原始CSV与`ENGINE_SUMMARY.json`留存，**这是ENGINE_ONLY_NOT_UI_LATENCY**，不是输入到显示延迟、FPS、全部机器或用户已感到顺畅的证明。端到端可见窗口／实际使用和资源长期有界尚待核验，不能关闭全部性能待办。

成功fixture在留存原始输出、源码和清单后按既有owner机制退役：首阶段145项／1,240,686字节，预设修正97项／312,385字节，分类阶段411项／752,100字节，保留流程534项／3,561,872字节。各自`fixture-retirement-after-raw-copy.json`可追溯；失败fixture、旧失败、源参考、固定证据和编译留证保留，未清其他目录或日常资料。

未完成／不得代签：AI本地推理、模型来源／兼容与许可、检测预览应用和局部橡皮擦仍未接入；本轮AI入口明确待接入，不伪造检测框、不把删除对象冒充局部擦除。TypeSet完整逐字字形及外部原版全面互通、全部小人书／PDF组合、持续大工程性能及新Windows可见候选未验；已有保留回归不是这几项通过。编辑完整人工仍NOT ACCEPTED，资产完整人工、115体验、Prompt对比各自PENDING，原Project独立结论／q09失败及旧用户否决不变。

下一具体动作：依据本固定输入准备仅含合成资料的本地原生审阅入口，分组核验新UI与实际卡顿，再处理AI等未闭环项；不借UI批准自动发布／更新日常。编辑改善收尾后才固定交接并迁移新对话，当前不提前迁移。

用户已明确答复：**左侧队列滚动，中央单图编辑**。随后确认“现在的demo好像已经没什么问题了，就按最新版这个UI来做”，因此以第三版 `editor-queue-and-export.html` 为当前UI实施依据；不再将中央连续多图作为待选方向。以下“澄清未答／方案待确认／生产未改”均保留为此前时点，不覆盖新确认。

沿原任务落实原生队列条目／网格、滚动条、单多选拖序／边缘滚动／取消、独立预览及工具图标和属性分组。复用原工程、保存、撤销、原图保护及TypeSet动作逻辑。交互视图切换仅为编辑器会话状态，不新增工程版本／撤销记录。适应画布重置视图，不改对象坐标。

聚焦卡顿修复：每页单个串行合并预览请求，复用原图和未变图层前缀；仍由原渲染原语和不可变原图取样，失败时丢弃部分写入缓存。新增合成逐像素／错误重试／1216×832九遮罩及连续笔划的工程样本，不能外推UI延迟或所有设备顺畅。当前发行／日常118／信任不动，AI模型／其余功能差距及用户完整编辑验收仍未完成；编辑改善收尾后才固定交接迁移新对话。

本阶段执行器 `.local/windows-preview/evidence/20261027T030218702122Z-editor-ui-first-native-20261027`，执行中记录不预签最终PASS；原失败及旧方案检查继续保留。后续最终结果单独登记。

## 2026-10-27 用户补正：上一版方案覆盖不足，必须补齐

以下为用户在三栏方案后的新明确反馈，覆盖下文仍写“候选”的相应项；不是功能已实现或验收通过：

- 工具命名明确为**选择／移动对象**（文字框及打码对象）和**平移画布**（仅查看视图）；两者都不是修改原图文件或调整底图位置。
- TypeSet完整竖排进入正式范围：上到下、右到左换列、框高／列距／字距、标点专用字形、ASCII全角直立和原始文字不变。上一版方案的CSS竖排切换不足以证明采用了TypeSet排版规则；其小人书图上文下模式另行保留。
- **文字与打码配置均要求自定义命名、保存和复用**，界面须提供保存新预设、选择／应用、覆盖、重命名及删除，区分新建默认、当前对象与多页应用。参考TypeSet原始文字预设可命名、打码为四个内置可覆盖槽；用户此次要求打码也能起别名，不能只恢复固定槽便标满足。
- 队列需明确展示多选批量操作、顺序调整、当前参数应用到所选、各自上一张设置、移除所选确认及撤销；保留各页文字和位置。预览包括缩略图悬停、独立完整预览、上一张／下一张以及四种输出版本切换，不以点击队列换当前编辑页代替预览功能。
- **unified-image-tool的AI自动打码由候选升级为明确需求**。需在后续原生实现中落实本地模型前提、检测当前／所选图片、结果预览、应用及手动修补；目前没有推理接线或模型运行证据，不生成虚假检测框或将演示计作模型通过。其他尚未明确的unified能力仍按差距盘点确定范围。

### 导出须完整展示，另有已确认语义差异

TypeSet README15.1–15.5规定的四种分类可任意组合勾选：

| 分类 | 图片内容 | 规则 |
| --- | --- | --- |
| 01_原始排序 | 不加打码／嵌字 | **仍按选定格式重新编码**，不是原始文件字节复制 |
| 02_仅打码 | 仅应用打码 | 没有打码的图片也输出，分类序列完整 |
| 03_打码并嵌字 | 同时应用两者 | 分类序列完整 |
| 04_仅嵌字 | 仅应用文字 | 没有文字的图片也输出，分类序列完整 |

方案还须展示：PNG／原格式／JPEG／WebP，当前页／所选页／全部页范围，文件夹与ZIP，源数据保留／隐私导出和命名／编号。小人书保留当前完整页、整册图片／ZIP及整册PDF，整页不能伪装为单张原图的源元数据。

现有TypesetExportDialog已有四个variant、范围／格式／ZIP／小人书PDF及预览分类选择入口，因此不能概括为底层全无；上一版demo只用按钮和状态提示代替，**设计展示不足**。源码TypesetExport中`variant=="original"`实际为逐字节原图副本，忽略统一重编码格式；与TypeSet的01_原始排序存在真实差异。后续须把“按序导出无编辑版本”和“原图副本（字节保全）”明确分开，不改写原件、不混淆隐私政策或旧请求兼容；目前未修改生产导出行为，状态OPEN。

下一版方案先完成以上功能／动作清单，再完整展开预览与导出面板，不以单个入口作为覆盖证明。日常118、已发布包和生产模块不变；原生／性能／AI与用户整体验收均未通过。本节只本地记录，不公开原用户资料或上传新提案。

## 三方面及来源边界（初版记录；以上补正优先）

### 2026-10-27 完整工作流方案第二版（本地，未实施到原生）

#### 后续用户补正与第三版队列／导出方案（当前）

用户指出“点击切页”表达不清，要求队列可滚动、**缩略图＋条目／网格两种视图切换**，确认按住拖动排序，并要求工具栏配图标，再次询问TypeSet多种导出。原三栏选择保持。本次已异步澄清“滚动”是否还包括中央连续多图；尚未收到回答，**不把左队列滚动和中央单图编辑的当前假设标成用户确认**，中央连续多图未实施。

第三版 `C:/Users/1/.codex/visualizations/2026/10/20/01a159f6-0089-77e1-adcb-8387addc7fd5/editor-queue-and-export.html`，最终SHA-256 `cff9001a07625c769c548d6f35ac4954b974d6f912f3c635a4c4b41a4f6c094c`；第二版不覆盖。左队列独立滚轮浏览，切换两种显示保持当前页／多选／内容；支持从缩略图或手柄拖动，插入标记、单张／多选整组、边缘自动滚动、松开提交、Esc取消及撤销／重做。工具栏采用图标＋文字，保留原入口。12张合成风景用于长队列演示，不读取用户图库。

原生只读核对：`TypesetQueueWorkspace.kt` 的 `TypesetQueue` 已有 `LazyColumn`、`detectDragGestures`、`movePagesTo`、拖动边缘 `scrollBy`；不能称原生完全没有拖序，也不能把本次浏览器动作当成原生网格／拖序测试。旧第二版只有顺序按钮，确有展示缺失。生产代码未改。

再读TypeSet README_1.1 第4.5／5.3／6.3／15.1–15.5／16与原生 `TypesetExportDialog.kt`：多分类、格式、文件夹／ZIP、小人书当前完整页／整册页／PDF和独立工程／预设备份分开。第三版“导出”直接显示设置，分“普通图片”“小人书完整页／PDF”“工程／预设备份”；原始排序按格式编码，与原字节副本／隐私例外分开，默认汇总每类文件数，完整列表按需展开，不把操作按钮压到长列表之后。独立预览可返回导出设置。工程／预设入口明确只是方案，不读写文件或声称TypeSet队列格式兼容。

执行 `D:\node.exe .local/editor-queue-v3-20261027/check-queue.cjs`：最终12个唯一浏览器场景PASS（包含缩略图直接拖动与普通点击不冲突），深浅色／1180、1024、736、360、320宽度无横向溢出／脚本错误，图标使用本机已有Lucide实际渲染并检查。Windows隔离Edge131.0.2903.112，网络拦截；最终原始结果 `RESULT-2026-10-27T02-42-07-938Z.json` 与 `FINAL_INPUT_MAP.json`。首次Esc取消失败 `FAILED-2026-10-27T02-37-24-926Z.json` 保留：焦点落在body导致仅root监听漏掉Escape，已在队列手势活动期间增加窗口级取消监听。中间两份11项结果各自留存，不与本次12项或上一版13项累计。

两次隔离浏览器临时清理217／184项、14,137,756／15,238,758字节；逐项清单与两份 `BROWSER_RETIREMENT_20261027-*.json` 保留，两个自有浏览器根均已不存在，0失败／未知删除／强杀。失败、截图及固定旧方案保留。**浏览器方案通过；原生编译／JUnit／性能测量／模型运行仍为0**，编辑NOT ACCEPTED，日常118／发布／信任均不变。下一动作：承接具体UI反馈及滚动澄清，继续原生交互、导出差异和聚焦卡顿修复；编辑收尾前不提前迁移对话。

沿原 UX-RECOVERY/W4、UX-R2、UX-EDITOR-GAP-001，不新建任务。用户继续授权编辑改善，并明确**编辑改善完成后才移动到新对话继续**；当前不提前迁移。收尾交接需固定实际源码、测试及原始证据、已确认人工、失败和待验项；不能用本方案的完成代替编辑器收尾。

线程内新方案 `editor-complete-workflow.html` 保留上一版 `editor-review-three-column.html` 作为覆盖不足的历史。第二版位置：`C:/Users/1/.codex/visualizations/2026/10/20/01a159f6-0089-77e1-adcb-8387addc7fd5/editor-complete-workflow.html`，最终SHA-256 `bc8e3ffcd851a151dac6a32b16265f05b151720601c60bc28f4aaa7044161fb4`。工程选择／新建、资产追加及工程选项入口保留；预览内这些入口明确提示不读图库或改工程。

| 动作组 | 本版可检查内容 | 边界 |
| --- | --- | --- |
| 三栏与工具 | 左格子队列、中画布、右文字／打码／图层／预设；明确对象移动与画布平移 | 只演示部分对象与范围操作，不等同原生多对象、局部橡皮擦或真实打码 |
| 竖排 | 上下、右左显示；框宽／框高、字距／列距、标点／ASCII开关；输入文字不被全角化改写 | 浏览器CSS显示不证明TypeSet逐字字形、排版及导出一致性；原生仍待验 |
| 命名预设 | 两类自定义名称、保存／应用／覆盖／改名／删除、新建默认与现有对象区分 | 本预览内存状态；文件交换入口有明确未接线提示，未证明原生持久化 |
| 多页 | 多选目标、当前文字／打码／两者、各自上一张快照、序号调整、移除确认及撤销重做 | 正文、位置保留；尚未替换原生拖序和保存逻辑 |
| 预览 | 悬停编辑后示例、独立上一张／下一张、四种输出版本、无选择框 | 不改变当前编辑页；真实渲染／导出相等性未验证 |
| 导出 | 四分类任意组合及每类完整序列、格式／范围／编号、文件夹／ZIP、元数据、原字节副本、小人书整页与整册PDF | 只生成可查看的输出结构，不生成真实文件；原始排序与原字节副本已在方案中分开，生产语义仍OPEN |
| AI自动打码 | 当前／所选、设备、置信度／IoU／分辨率／外扩、预设，检测→预览→勾选→应用→手修流程 | 明示模型未接入，检测／应用禁用，无虚假结果，无模型或真实服务调用 |

实际检查：Windows隔离Edge `131.0.2903.112`，网络全部拦截，13个唯一方案脚本场景通过；1180／1024／736／360／320宽度及深浅色均无横向溢出／脚本错误，已查看宽屏、导出、窄屏截图。最终逐例 `.local/editor-workflow-v2-20261027/RESULT.json`，执行器 `check-workflow.cjs`；执行命令 `D:\node.exe .local/editor-workflow-v2-20261027/check-workflow.cjs`。这些是 **BROWSER_PROTOTYPE_ONLY**，不累加上一版8类／中间12项成为新功能测试数。

三个失败原始JSON保留：两次脚本未先展开当前属性／队列区，一次多选设置与单页移除断言不一致；期间将原型“至少留一页”的不必要限制改为可移除全部、空工程可撤销。所有更正均仅在方案与其检查脚本；不覆盖原失败。最后固定方案、执行器及结果散列另见本地 `FINAL_INPUT_MAP.json`。

两次自有隔离浏览器清理分别239项／15,656,998字节和最终复核184项／15,250,799字节；均逐文件快照后清理，两个浏览器自有根消失、0失败、未知删除0、无强杀。`BROWSER_MANIFEST_FIRST.json`／`BROWSER_OWNED_RETIREMENT_MANIFEST.json` 和两个 `BROWSER_RETIREMENT_20261026-*.json` 留证，保留失败／截图／源方案。中间dce方案结果另保留 `RESULT_BEFORE_RETAINED_ENTRIES.json`，最终补回工程原入口后的同13项复核不累计。未触及日常安装。

状态：**UI_PLAN_V2_READY_FOR_REVIEW / NATIVE_NOT_IMPLEMENTED / PERF_NOT_MEASURED / AI_NOT_CONNECTED / EDITOR_NOT_ACCEPTED**。生产模块与89be225无差异，编译／JUnit／原生应用启动／发布0；新候选、新版本和日常118均无变化。下一动作是检查这份完整交互方案，再沿已确认三栏及原TypeSet动作合同实施原生交互、导出语义与聚焦性能修复，不重测已确认主题／更新，不把资产完整人工、115体验或Prompt对比提为通过。用户要求的新对话迁移在编辑改善收尾后办理。

| 方面 | 参考和目的 | 当前判断／接续 |
| --- | --- | --- |
| 嵌字排版与连续编辑 | TypeSet Forge1.1：文字、横竖排／富文本、打码、格子队列、命名预设、四类批量导出、小人书与PDF | 原全功能采用合同保持。不能仅因底层有API或旧测试通过，认定入口、操作反馈及整套工作流已验；本轮先修交互缺口 |
| 图片处理能力与整合范围 | [unified-image-tool固定来源](https://github.com/duxingz/unified-image-tool/tree/ed42f1f459dde621086f7763c36a46d768d091cb)：本地自动识别／手动修补／橡皮擦、贴图水印及批处理管线 | 先只读盘点，区分现有能力、缺失候选、需要另定前提的能力，不默认整库或所有功能都纳入首轮实现 |
| Studio交互组织 | 在原生三栏和既有数据保护基础上，把工具／对象／图层／批量目标组织清楚 | 提交可操作方案，确认后按受影响路径实施；整合不另造一套项目、图像或Prompt权威核心 |

性能作为贯穿三方面的验收门槛，不作为增加工具按钮的理由，也不自动扩张到整个PERF计划或其他项目。

TypeSet本地只读原件：`.reference-assets/typeset-forge-1.1/TypesetForge_1.1.html`，SHA256 **892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63**。同时读取README_1.1和原[动作合同](UX_R0_ACTION_TRACE.md)、[布局合同](UX_R0_WORKFLOW_AND_LAYOUT.md)、[独立源样本合同](TYPESET_REFERENCE_VALIDATION.md)。原件未运行／修改／上传；旧独立样本及失败仍按固定证据保留。

unified来源冻结在**ed42f1f459dde621086f7763c36a46d768d091cb**，只读通过GitHub连接器查看README、目录、app/ui/editor_canvas.py与tool_panel.py。README blob99d9fd68969a6147c5423faab7d1c32cfec4df19；canvas blob3429cb9bc38c087e050cbc46b3790123d9945ba5；panel blobdefc2014ed3c682e582d569c8c304b8a6e8055ea。本轮没有下载／运行发行包、ONNX模型、安装依赖或调用服务，也未复制外部实现到生产源码；代码复用许可及模型／运行时前提尚未审核。

## 已确认的交互缺口

1. **队列浏览**：TypeSet README6.1要求格子缩略图；Suite TypesetQueueWorkspace仍为LazyColumn单列。应保留已有Ctrl／Shift、多选拖序、悬停及编号行为，恢复网格浏览与合理缩略图尺寸，长队列单独滚动。
2. **工具入口**：当前工具条、画布上方“添加文字”和右侧预设分散；操作模式与工具标签换行。工具区应集中显示选择／文字／矩形／画笔／圈选／平移，选中状态与当前操作提示清楚；不能让用户靠试按钮找功能。
3. **属性与图层**：TypesetWorkspace的对象按钮包在属性栏顶部，之后连续堆放文字／打码全部设置；已有合同要求属性分组与独立图层列表。拟将右栏分为“文字／打码／图层／预设”，选中对象联动对应属性，保留所有原控件入口。
4. **连续处理**：明确区分新建工具参数、当前对象属性、当前页和多选页目标；应用样式前显示目标数量，不覆盖各页文字与位置。已有优先／默认继承规则仍需按TypeSet核验，不能在UI重排时改语义。
5. **画布反馈**：适应画布应复位视图，不修改对象几何；拖动／调整框尺寸应即时反馈，输入框编辑与画布快捷键互不抢焦点。缩放、平移、选择和撤销分别核验，不用按钮存在代替操作可用。

## 卡顿：已定位的可疑链路，未宣告修复

当前TypesetWorkspace中previewPage包含每次拖动的临时遮罩／文字，produceState以它为key。每次变化都发起typesetPreviewRaster，进入TypesetRenderer.render重新解码原图并逐层计算；Java2D计算并不因协程取消立即停止。多遮罩／模糊情况下，这条链路值得优先验证。现在是**静态代码发现，不是已量化根因或修复PASS**。

unified的EditorCanvas提供可参考的处理原则：`_render_drag_stroke`从笔划前基图计算当前笔划完整包围框，说明预览必须与最终几何／效果一致；`_render_drag_erase`处理增量区域；wheelEvent与_fit_to_content明确视图缩放。其效果采样／合成语义并不自动等于TypeSet，不能直接替换Suite渲染核心。

拟用合成1216×832图、9个已存在遮罩加一条连续笔划核验：输入到预览延迟、单次重绘工作量、是否积压并发任务、内存随拖动／切页是否有界，以及松开前后效果和最终导出是否一致。具体数值先采集，再提出可复查目标，不虚构FPS／所有机器顺畅保证。不读取用户原图，不扩张为整个工程性能重跑。

## 方案及推进顺序

1. **本轮盘点与交互方案**：保留三栏，主要路径可试；只说明UI分组／选择／反馈，示意图和笔划不作为真实模糊、导出或模型实现。
2. **用户确认交互方向与整合优先项**：先确定最不顺手的动作和unified中真正需要的能力。自动识别、橡皮擦、贴图、水印、批处理管线分别列候选，不因参考项目提供就默认为本次全量要求。
3. **原生实现与聚焦自动验证**：先处理当前交互／卡顿；复用现有持久化、撤销／重做、原图保护、导出和旧测试。按实际变更跑最小必要回归，保存失败和资源观察；UI草图不能代替原生表现。
4. **分组人工验收**：先单图文字与打码，再连续多页／预设／批量，再小人书与导出；通过哪组就只签哪组。不重复索要已确认主题或原更新测试。

核心验收动作：添加／选择文字→编辑内容→拖动／改框→横竖排；连续多笔矩形／画笔／圈选→调整参数→撤销重做；图层选择／隐藏／锁定／顺序；多页选择／拖序／批量样式；保存／关闭／重开；预览与最终输出及原件字节保持。卡顿测试贯穿动作，不能只测空场景。

## 本轮实际交付与未完成

- 可操作方案：线程内editor-review-three-column.html；可切工具／属性／页面、文字方向、图层、示例笔划和撤销重做、批量目标及小人书旁白。**方案不连接真实图片处理引擎、不读取本地图片、不产生导出。**
- 浏览器原型检查使用隔离Edge配置，8类交互、1180／736／360宽度与脚本错误检查通过，仅表示方案脚本／布局通过，不是原生回归或用户验收。原始记录在`.local/editor-interaction-20261027/DEMO_QA.json`。
- 初步局部UI与重绘缓存草稿已保留到`.local/editor-interaction-20261027/UNVALIDATED_EDITOR_DRAFT.patch`及相邻未验证文件；按最新先重规划的方向，从生产工作树移出，规范化内容与89be225一致。**编译0、JUnit0、原生性能测量0；不能称已修复。**
- 本轮不打包、发版、更新日常118、动私人工程、改信任链或操作Desktop；原q09失败／UNKNOWN、旧否决与Prompt pending保持。

本方案阶段不将UI提案或尚未确定的外部整合能力公开发布；保留在现有文档／任务体系中供确认。下一步：用户检查三栏交互方案，按具体反馈确定原生交互改造顺序与unified候选范围。
