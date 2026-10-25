# UX-DISPLAY-THEME-001：源码接线，验证因资源预检推迟

2026-10-25 Asia/Tokyo。状态 **SOURCE IMPLEMENTED / NOT COMPILED / NOT TESTED / USER NOT TESTED**，不是功能验收或第3步内部包完成。沿用17:49 JST用户队列：本轮性能段收尾 → 显示主题 → 内部包/验收 → 后三组需求；不新增任务或执行者。注册事项的确认提交4b38e7b仅改任务记录，不启动所登记的后续需求。

## 固定源码与参考

- 本轮应用/新测试源码：[42146d3705f5cbc9ed392735f9c4727f8a8f67d7](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/42146d3705f5cbc9ed392735f9c4727f8a8f67d7)。初稿2c56b3f，随后42146d3补图片工具独立窗口的主题提供器；两段均未编译/执行测试，不记为两轮通过。
- 前置生产性能边界db46098445d1c1064ee77104fe320268cc6d0f93及字体证据1aa1fde不改；旧性能raw/失败/未采用方案及固定索引不改、不重跑。
- Desktop只读参考：[d53d46ca8fe0dbac11c33e85b38e07099b105da8](https://github.com/MisakaPiano/ChatChatBar-Desktop/commit/d53d46ca8fe0dbac11c33e85b38e07099b105da8)。核对DesktopTheme、DesktopAppearanceController、DesktopManagePanel、DesktopSettings与共享ThemeColor五个固定文件；读取时工作副本文本均与该提交一致，字节散列见[参考记录](evidence/DISPLAY_THEME_20261025/DESKTOP_REFERENCE.json)。Desktop其他备份/映射等修改不属于本轮，未写入该仓库。
- [源码与包状态映射](evidence/DISPLAY_THEME_20261025/SOURCE_MAP.json)列出11个应用/测试文件的固定Git blob SHA-256。f6598d3为新源祖先仅证明源码包含；无新包/版本/tag/签名或更新元数据，**包内下载清理修复仍NOT BUILT / NOT VERIFIED**，公开115及旧2484858包不代表本次源。

## 实际接线与边界

Desktop实际显示入口包含跟随系统/浅色/深色、中性/CCB原生/自定义强调色、预设色和恢复默认，并非角色颜色或完整换肤。本轮按该交互做最小Suite接线：

- 主标题栏增加带提示与无障碍名称的齿轮入口“显示主题”，打开单独对话框；保留原路由、布局及工具入口。自定义提供色块和6位RGB输入，不引入新的颜色依赖；已有共享ThemeColor只读复用，不重写其模型/权威Prompt或请求逻辑。
- 默认仍为深色蓝色，不自动改变用户当前安装/设置。仅用户切换后，通过现有LibraryRepository更新`studioDisplayAppearance`偏好；现有不可变修订链、备份路径及其他最新偏好保留。未知格式、损坏记录或读取失败禁止主题写入，不静默重置；保存成功后才切换显示，失败走现有回执。启动读取在首帧后发起，额外读取成本尚未测量。
- `StudioAppearance.kt`集中模式、配色、前景对比选择、保存和主题提供器。`Main`、`LibraryPresentation`、`WorkspaceUi`及SuiteSourceAdapters的旧颜色入口改为读取同一可观察palette；主窗口、AI/批次/封面/图片工具独立窗口、菜单、标题按钮、批次警示、排版队列选中态跟随。每进程一个应用工作区，各窗口共用状态；没有跨进程设置广播。
- 图片原件、排版文档/画布内容及封面像素不重着色；有实际图片时保留预览中性背景，空预览/输入框随主题。Windows关闭按钮红色和白色图标保留原交互；系统拥有的窗口装饰仍由系统管理，未改OS设置。窗口布局、提示、对比度和系统主题变化的实际表现均待运行/人工验证。

## 执行事实与资源门

实际只执行源码只读核对、有限目录体量统计、静态差异/空白检查与记录生成。**未启动Gradle/JVM测试、UI宿主或安装包构建**。源码提交成功不代表编译成功。

[原始预检](evidence/DISPLAY_THEME_20261025/RESOURCE_PREFLIGHT.json)：H盘观测可用1,432,711,168字节（约1.334GiB）。四个明确模块build目录逻辑长度有限遍历均完成、未跟随重解析点：desktop143,500,389、core287,836,985、image-authority10,949,292、asset-library418,505字节。规划包络为这些既有输出的2倍、Gradle元数据128MiB、编译临时64MiB、合成测试16MiB、日志证据16MiB及并行Desktop余量1GiB，合计2,194,033,190字节（约2.043GiB）。这是**保守估算，不是实测峰值或最小所需空间**；未通过此预算，不为启动测试而下调余量。读取时还存在Desktop Java owner47608，无强杀或抢占。

精确测量UTC未捕获，JSON明确区分后续recordedUtc与早先读数，不把记录创建时间冒充精确测量时间。判定`DEFERRED_SPACE_AND_DESKTOP_OWNER`发生在启动前，实际新增测试0；不是测试失败、跳过或PASS。未清理/移动用户文件、日常安装、真实隔离副本、凭据、原始证据或备份，未扩展I盘使用；没有恢复扫描或修改安全/重启设置。

仅暂停需要这些资源的验证/打包步骤，源码核对与留证继续。内部打包需主题验证后单独核算目标卷和解包/运行时/安装产物并存峰值，当前未获资源准入；不能将小测试预算用于包构建。新开任何重负载前重新读实际空间与Desktop owner，不复用本次快照当作未来准入。

## 待执行回归与验收

测试命令（**未执行**）：`python tools/Run-PreviewChecks.py --phase display-theme-20261025 -- :studio-desktop:test --tests '*StudioAppearanceTest'`。须先有新鲜空间预算及Desktop让行门、固定执行源码、离线严格依赖和现有单worker/低优先级；仅新建所属合成fixture，按既有生命周期封存证据后退役。JDK17是构建配置目标，本轮未启动JVM确认环境。

[逐例状态](evidence/DISPLAY_THEME_20261025/test-cases.json)明确四个新增唯一JUnit方法均**NOT EXECUTED / NOT COMPILED**，循环中的模式/颜色样本不另计方法，也不与旧测试累加：

|新增方法|准备覆盖的风险|当前|
|---|---|---|
|modesAndColorStylesHaveReadableForegroundsWithoutChangingTheExistingDefault|模式解析、强调色前景对比、现有默认|未编译/未执行|
|strictSettingsRejectFutureMalformedAndOutOfRangeValuesInsteadOfReplacingThem|未来/损坏/错误类型/越界输入拒绝|未编译/未执行|
|saveUsesLatestMetadataAndReopensWithoutChangingOriginalImageBytes|最新元数据合并、重复保存、合成原图字节/重开|未编译/未执行|
|damagedLatestAppearanceBlocksWritesAndRetainsItsRevisionAndOriginal|加载后被破坏的最新记录不可覆盖、合成原图保留|未编译/未执行|

运行后仍需隔离UI核对：三种模式和三种配色；色块/自定义输入/默认恢复；各页、菜单、右键与独立窗口；窄窗口和系统主题变化；失败回执与重开保存。不得对真实凭据/图库或日常安装做自动测试。完成隔离与内部包后再交用户实际验证，**当前没有用户或Project通过**。

资产/编辑仍USER_NOT_TESTED，115体验/Prompt比较分别PENDING，q09 FAILED/root UNKNOWN、旧失败/用户否决、路径错误前态与UTC UNKNOWN及禁止再访问、PRODUCT_HOLD / NOT RC全部保留；新Project材料未发送。下一动作是资源满足后只执行这组新回归并修复实际问题，随后才进入第3步内部包，不能借本项启动后三组需求或公开发布。

## 低负载准备停点

后续协调核对只补齐了[现有人工清单的主题/内部包入口](../rc/DAILY_USE_MANUAL_CHECKLIST.md)，不改上述固定源码或原始资源记录、不新增执行。当前已完成可独立进行的源码接线与静态核对、Desktop参考冻结、4方法草稿、源码/预检留证、人工与包身份验收清单准备；没有编译/测试/打包后台进程。下一必需步骤为实际编译和隔离回归，停在既定空间预算不足的准入决定，并继续服从Desktop资源优先；资源满足后才重新预检，不因暂时idle重复核对、加测量、清理、跨盘使用或越过整版验收门。

## 后续资源重检及A窗口：空间满足，Desktop准入到期

原空间线程按其精确授权清理10份MSI副本后，本线程重新只读核算四个build目录，保留原H盘2,194,033,190字节规划包络，另明确C盘启动/Gradle临时256MiB＋余量512MiB预算（非实测峰值）；不降低原预算或新增本线程清理/跨盘权限。首次重检H盘3,090,518,016、C盘10,223,579,136字节均满足，Desktop Java owner47608仍在。源42146d3与执行文档HEAD1c3c9d9之间modules/tools相同；输入检查、两次相隔30秒安静观测、启动前空间检查及未知Java owner保守阻塞都在本轮本地门内，未放宽原串行/低优先级/离线严格依赖条件。

**2026-10-25 20:10:58 JST（11:10:58 UTC）30分钟窗口到期，明确RESOURCE_GATE_TIMEOUT_NO_TEST_STARTED：runnerInvoked=false、新增JUnit0、runner日志0字节、未产生Runner阶段目录。**这不是功能测试失败/跳过或通过；未编译、未测、未打包状态继续。20:11:27 JST后续只读观测H盘3,090,264,064、C盘9,589,731,328字节仍满足预算，Desktop owner47608仍存在，未操作它。累计进程读写/CPU不能证明即时繁忙，本轮只报告资源门未获准入；观察日志仅记录准入状态变化，不冒充完整连续性能采样。

[RESULT及原始资源门文件](evidence/DISPLAY_THEME_RESOURCE_GATE_20261025_A/RESULT.json)共10份小文件约16KB，含预算重检、门脚本/状态/变化记录、空runner日志、实际收集脚本与逐文件大小/SHA清单。首次证据收集使用无效Python编码名`utf8-sig`，在首读gate-status时失败、尚未复制证据文件；已记录COLLECTOR_FIRST_FAILURE，原内联命令保留在会话，未声称另有其保存文件。仅改为`utf-8-sig`后向同一已核空的本轮目录以CreateNew方式收集；未重跑资源门或测试，不新增任何通过计数，原公开失败/基线/raw不改。

UPD-CLEAN-PROTECTION-001随57876f1只登记和静态核对，[范围与结论](../rc/OWNED_ARTIFACT_LIFECYCLE.md#upd-clean-protection-001历史保护与当前入口冲突待整版合成验收)独立于这4个主题方法：代码保留受保护111，无自动ACTIVE记录迁移分支；解除旧保护需一次性人工审定及独立归属证明，未知仍保留。该合成用例仍留到主题后的正常内部包验收，实际未执行，真实App未访问/修改。当前所有低负载准备完成、无Studio后台任务；下一步等待真实Desktop资源变化后新鲜预检，不重复同一门制造活动，不越过主题验证启动包/后三组或清理。
