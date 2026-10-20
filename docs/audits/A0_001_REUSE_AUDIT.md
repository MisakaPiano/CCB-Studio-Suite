# A0-001 — 建项核对与只读源码复用审计

日期：2026-10-21，Asia/Tokyo。状态：**审计交付完成，等待 A0-002 Project review；没有生产采用、实现或产品验收**。执行者为本工作区本次 Codex 会话，单执行者，未委派。模型/Fast 的实际 UI 配置未取得，不推测。

## 结论与采用边界

建议沿用精确 Desktop 图像权威，优先另立 Desktop 小范围共享发布/抽取合同；Suite 不复制第二套 Prompt 或 NAI 请求。现有 `sharedCore` 是可直接作为 JVM 依赖的技术候选，但包含聊天实体、仓库和持久化职责，既无本次验证的发布制品，也无本次取得的完整公开复用许可。因此 **A0 没有批准任何依赖引入**。首项生产候选先建立独立数据根和不可变原始资产，见 [A1-001 候选](../../tasks/A1_001_IMPLEMENTATION_CANDIDATE.md)。

三类身份必须保持分开：客户端本人直连的供应商档案、Server 管理员持有的上游凭据、Studio 用户/设备身份。远程客户端提交完整 Recipe 和资产，使用 Studio 登录身份，不要求自带 NAI Token；BYOK 只作未来可选扩展。

## 实际环境与安全前置

| 项目 | 实测事实 |
|---|---|
| Suite 根 | `H:\CCB-Studio-Suite`，普通目录，无 reparse target；Git 初查确认自身及父目录无仓库 |
| 初始文件 | 40 个交付/增量文件（含清单自身和三份参考）；原清单列 36 个文件 |
| 保留的额外文件 | `SETUP_THREE_WORKSPACES_CN.md`、`handoff/GITHUB_INITIAL_PUBLISH_AFTER_A0.md`；只读说明，不执行其中发布命令 |
| Git 操作 | 按本轮用户授权执行 `git init -b main` 和 `git remote add origin https://github.com/MisakaPiano/CCB-Studio-Suite.git` |
| Suite Git | unborn `main`，没有 HEAD commit；index 为空；项目文件仍未跟踪，不能称 clean；无 stage/commit/push/merge |
| 远程核对 | GitHub GET branches 返回 `[]`，验证为空分支列表；未改变远程/可见性 |
| Desktop 本地（开始） | `H:\ChatChatBar-Desktop`，`feature/phase8-fish-audio`，HEAD `3ece1366b142210f2db7c61a9c479cd156534b69`；相对本地 tracking ref ahead 1/behind 0（不是实时远程差距证明） |
| Desktop 工作树 | 初查 tracked/index 无差异；未跟踪 `.codex-remote-attachments/`、`CCB_DESKTOP_STANDARD_CONVERSATION_HANDOFF.md`、`docs/desktop/evidence/post-p8-screenshot-workspace-r2-20261020/`，未读取这些内容、未删除或改写 |
| Desktop refs/remotes | 只读观察 origin=MisakaPiano/ChatChatBar-Desktop、upstream=SaltyFishOTL/ChatChatBar；唯一列出的 worktree 是该根；未 fetch/checkout/switch/init/build。结束本地desktop ref为4c2af2576e2319e8041ffd2a6c5a205450f18246，初查未记该ref，不能断言本轮改变；实时GitHub desktop仍D |
| 工具 | Windows；PowerShell 主进程 7.6.5；校验由 Windows PowerShell `powershell.exe` 执行；Git 2.51.1.windows.1；Python 3.10.6 |

先读 AGENTS、START_HERE、母本、索引、CURRENT、TASK、SOURCE_LOCK、A0 合同及开场交接，再定向核对 docs/01—10、DECISIONS、CROSS_PROJECT、README、ignore、instructions/handoff。历史母本作为初始快照保留；未重建第二套立项材料。

## 来源身份与观察

以下别名是本报告的精确身份缩写，路径表中引用它们；行号针对 Git 对象，不针对 Desktop 当前活动工作树。

| 别名 | 仓库与完整提交 | 本轮核验/角色 |
|---|---|---|
| D | `MisakaPiano/ChatChatBar-Desktop@5ef95428421f7c693b010ac88f51d470804ab07a` | 本地对象可读；GitHub `desktop` GET 同 SHA；集成复用候选锚点 |
| O | `SaltyFishOTL/ChatChatBar@5e76a9cb841736bbbf3499a2e35e5789af4c5ca8` | 本地对象可读；正式 Desktop 声明基线 1.4.1，不提升 |
| U | `SaltyFishOTL/ChatChatBar@979cb505958dffb1b6d64a8de657434f198ffa96` | 本地对象缺失，随后 GitHub GET 确认精确对象和当前 master 同 SHA，提交说明为 1.4.6 发布记录；只读观察 |
| F | `MisakaPiano/ChatChatBar-Desktop@c4098e08bec84372911a5a8bc38c2cfa72f98978` | 本地精确对象读取 D-044/146；未批准整个 feature |
| L | `MisakaPiano/ChatChatBar-Desktop@3ece1366b142210f2db7c61a9c479cd156534b69` | 实测活动 HEAD；只查定向增量身份，不作 Suite 起点 |

GitHub compare O→U 返回 ahead 28、total_commits 28、221 个文件条目。该范围触及图像/历史/凭据/备份等，不因版本号自动采用。D 的 `docs/desktop/14_UPSTREAM_COMPAT.md:29` 已明确列出位置、角色启用、Enhance/Upscale、隐私 PNG、metadata/stealth 等逐项官方移植例外；D 不等于未经变更的 O，也不等于完整 U。`92_PHASE7_POST_MERGE_CLOSEOUT.md` 记录 P7 qualified engineering acceptance 和残留 NT/RED；本次没有重跑或改写其验收。

读取方式主要是 `git show <SHA>:<path>`、`ls-tree`、定向 `git grep/log/diff`；U 的更新器/凭据代码通过 GitHub 文件 GET 读取。未创建 `.reference-repos`，新增参考仓库磁盘占用为 0。可复查摘录范围及 SHA-256 见 [来源证据](A0_001_SOURCE_EVIDENCE.json)。该清单记录静态读取，绝非运行覆盖率。

结束复核发现外部并行活动：Desktop同一feature HEAD已到`ba7b58c829fb583c0e22c100f5de8a3799406c20`（提交说明“保全截图工作区R2定向回归与新包证据”），tracking差距变0/0，原先untracked的截图证据目录已进入该提交；余下未跟踪附件/交接文件保留。3ece→ba7的`app/` diff为空，审计锚点D及其远程desktop未变。本会话只对Desktop发只读命令；不能将外部前进说成“全会话Desktop状态不变”，也不对其他执行者的变更作安全/验收签署。

## 精确源码表

路径前缀：`S = app/sharedCore/src/main/kotlin/com/example/chatbar/`；`W = app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/`；`A = app/app/src/main/java/com/example/chatbar/`。下面的 S/W/A 与后续后缀组合成完整仓库路径。

| 身份 / 精确路径 / symbol | 已读事实与采用方式 | 差异、风险与测试入口（均未执行） |
|---|---|---|
| D `app/sharedCore/build.gradle.kts`；`app/gradle/libs.versions.toml` | Kotlin/JVM 17；Kotlin 2.3.20、coroutines 1.10.2、serialization 1.8.1、OkHttp 4.12.0、msgpack 0.9.12。可直接依赖模块的技术候选 | 整个模块过宽；未选发布坐标/制品校验。版本是来源快照，非 Suite 已安装配置。禁止依赖活动邻接工作树发布 |
| D S`domain/image/NovelAiStudioModels.kt:155` `NovelAiStudioDraft`、`:223` `NovelAiGenerationRecipe` | 独立草稿、按模型设置、角色/负向、Guidance、history apply；先保持权威转换，通过 Suite 适配绑定 | `importedCharacterCardId`/默认负向来自卡领域；不能拿该 Recipe 当已含账户、请求快照和资产耐久性。`NovelAiStudioRequestTest`、`NovelAiStudioCustomSizeTest` |
| D S`data/repository/NovelAiStudioStateRepository.kt` `loadDraft/stageDraft/flushLatestDraft/saveHistory/applyHistory` | revision、延迟 flush、history undo、checkpoint 已有实现；窄抽取 storage port 候选 | `initialize` 用 `loadAll`，损坏记录可能被跳过；不能原样作为 Suite 完整图库。`DesktopPhase7StudioTest`、O `NovelAiStudioRepositoryTest` |
| D S`domain/image/NovelAiDesignTurnRunner.kt:6`；S`data/repository/NovelAiDesignConversationRepository.kt:21` | first/revise、pending/completion、设计对话与图像历史分离 | repository 仍绑 `JsonFileStorage`，initialize 会修复中断状态并 prune 历史（上限100）；Suite 必须独立决定保留合同，不能把创作过程当缓存 |
| D S`domain/image/NovelAiPromptDesigner.kt:35,357,520` `designForPromptToolDetailed/reviseForPromptTool`；S`domain/prompt/NovelAiPromptAuthority.kt:28` | 已有非聊天 UI 用入口，注入 `NovelAiTextTransport`、tag research、image understanding；Prompt 保留唯一权威 | 内部仍引用 `ChatMessage`、`CharacterCard`、`ModelConfig`，Prompt 工具构造内部消息；不需创建真实聊天会话，但尚非无聊天类型的窄模块。禁止复制 literals。`NovelAiDesignerTransportTest` |
| D S`domain/model/EffectiveModelResolver.kt` `resolveAuxiliaryTextModelExact/resolveEffectiveModelApiKey/selectDefaultImageModel` | 精确辅助模型与普通 fallback 是不同规则；per-model key 优先，显式 cleartext HTTP 可无认证，其余走 global key | 新档案只包装有效解析结果；不得改 fallback、把 templateType 当平台、隐式跨号切换。`EffectiveModelResolverTest` |
| D W`DesktopNovelAiTextTransport.kt:13` `streamText`；S`domain/chat/OpenAiStreamingTransport.kt:61` | auxiliary assembler、拒绝/空响应/截断分类、SSE 和 provider request owner 已分层 | W 为 internal 平台类；不能直接依赖 desktopApp。可抽取窄 adapter，同步其错误清洗；不能把 provider error body/diagnostics 原样记入日志。`NovelAiDesignerTransportTest`、`OpenAiStreamingTransportTest` |
| D S`domain/image/NovelAiImageService.kt:42,63,238`；`NovelAiStreamFrameDecoder.kt:3`；`NovelAiBatchPolicy.kt:3` | NAI 请求构造、1–4 samples、intermediate/final/error、MsgPack frame、429、取消为权威共享候选；frame 默认限32MiB | 默认429有限尝试，但 continuous 可 `retryRateLimitsUntilCancelled=true`，不可直接当 Server 持久队列重试政策；参数/协议不凭本报告更新。`NovelAiStudioRequestTest`、`NovelAiImageRetryTest` |
| D W`DesktopNovelAiGenerationRuntime.kt:20`；W`DesktopNovelAiGenerationStore.kt:17` | 启动 draft/seed/Recipe，单 token slot，完整 batch 后 NonCancellable persist；新文件再 history，失败按严格读回决定清理 | **runtime:63–65 仅收集 composeResult 后 bytes，Suite 要另外保留上游原始 bytes**。跨文件+索引不是通用崩溃事务/硬断电保证。`DesktopPhase7StudioTest`；Suite 新增故障注入测试 |
| D S`domain/image/NovelAiPngMetadataReader.kt:20` `read/readStudio/readEnhance`；`NovelAiStudioImageImport.kt:73` `applyImportedMetadata`；`NovelAiImageRegeneration.kt:104` | 普通 PNG 字段与 alpha fallback 候选分开、不混合冲突来源；按选择 OFF/REPLACE/APPEND 合并；style 仅拆精确记录前缀 | reader 仍接受文件路径，metadata DTO 在 S`data/local/entity/ChatMessage.kt:46`；需有界 ByteSource/资产适配，不能让 Server 接收任意文件路径。`NovelAiPngMetadataReaderTest`、`NovelAiStudioImageImportTest` |
| D S`domain/image/NovelAiImageGuidance.kt:77`；`NovelAiSqlDatabase.kt:4`；W`DesktopNovelAiInfrastructure.kt:13` | Guidance DTO/validation、只读 SQL cursor port 可共享；catalog、dictionary、tokenizer 由资源适配装配 | `NovelAiStudioAssetRef.path` 不是远程资产ID；W JDBC/AWT/Skia 与 Android SQLite/Bitmap 不进中立合同。资源及许可须单独锁定。`NovelAiImageGuidanceTest` |
| D S`data/local/JsonFileStorage.kt:194,221,767` | strict entity scan、Missing/Corrupt/ReadError、singleton 严格错误、临时文件+原子替换可参考 | 同文件 `loadEntity/loadAll` 仍 permissive；`:292` 跳过损坏。原子 move 有普通 replace fallback；不能据此声称文件/索引原子、fsync 或任意断电无损。`JsonFileStorageStrictScanTest`、`JsonFileStorageSafetyTest` |
| D W`security/DesktopSecretStore.kt:3`；`WindowsSecretStore.kt:16`；`WindowsDpapiSecretProtector.kt:11` | DPAPI、逻辑 key、错误种类、拒绝符号链接和密文 envelope；窄平台抽取候选 | `NovelAiToken` 是单全局槽，canonical identity 含 ccb-desktop；Suite 用独立根/命名空间，保留显式错误，不读现有 secret。`WindowsSecretStoreTest`、`WindowsDpapiSecretProtectorTest` |
| D W`DesktopFilePicker.kt:17`；`DesktopDataRootOwnership.kt:79`；`DesktopDataOperationCoordinator.kt:50` | 多选/保存对话框、跨进程 file lock、正常/独占维护与 closeAndDrain；参考后窄适配 | internal 类型与完整应用容器不是 SDK；排水超时必须保留 ownership。`DesktopDataRootOwnershipProcessTest`、`DesktopDataOperationCoordinatorTest`、`DesktopApplicationLifecycleTest` |
| O A`ui/imageprompt/ImagePromptToolViewModel.kt:278`；A`domain/image/NovelAiStudioAssetStorage.kt:18` | 原生 Studio UI、Uri 导入、Bitmap 引导、Android 生命周期路径 | 绑定 `ChatBarApp.instance` 和应用服务；guidance 会 oriented decode/裁切/重编码，不能充当原图仓库。旧 O 保存/取消逻辑不能覆盖 D 已审例外；`StudioImageTaskCompletionTest` 入口在 D Android 测试树，需后续对应移植审查 |
| O A`data/security/NovelAiCredentialStore.kt:16`；U 同路径 `load/readForBackup` | Android Keystore AES/GCM，原生适配参考。U 增加 backup maintenance/readForBackup 严格检查 | O/U 日常 `load` 都对解密失败返回 null；Suite 应区分缺失/错误并禁止静默覆盖。U 明文备份 API **不采用**；不把其备份需求带入 Suite 普通导出 |
| O A`ui/components/ImageMosaicEditor.kt:89`；D W`DesktopImageEditing.kt:39` | Bitmap/AWT/Skia 打码、90度旋转、裁切、动画拒绝等平台经验 | O undo 是最多10份 Bitmap；D crop/zoom 是 transient；均不是可持续编辑的跨端图层工程。TypeSet 是更匹配的排版参考 |
| U A`domain/update/AppUpdateChecker.kt` `checkLatestRelease/parseGitHubReleasesAtom`；`AppUpdateManager.kt` `startDownload/validateApk/requestInstall` | API→Atom→最后网页 tag fallback、应用内下载、包名/版本校验、FileProvider/系统安装；仅参考/窄平台采用候选 | `InstallerLaunched` 不是安装成功。读取的校验函数没有 Suite 的可信元数据/渠道/冻结防护/更新后数据确认；OS 安装签名检查不等于完整可信链。`AppUpdateCheckerTest` 入口存在于 D；U 测试本次未执行 |

S 的全树 import 定向扫描未发现 `android.*`、`androidx.*`、`java.awt.*`、`javax.swing.*` 或 JNA import；这仅证明这些显式 import 不在该扫描内，**不证明 Android/Server 运行兼容或完整传递依赖闭包已验证**。

## A–C：直接依赖、抽取与平台中立性

- **直接依赖候选**：固定 D 的 sharedCore，或其以后发布的图像模块；具体请求、frame、大小、坐标、Guidance、Prompt 组合保持单一维护来源。尚无已批准制品，A0 引入数量为0。
- **需窄抽取**：Designer+Prompt authority 的完整传递闭包、Model resolution、metadata DTO/IO seam、draft/history storage port；Windows root/gate/SecretStore 按 Suite 身份适配。抽取首先在 Desktop 单独审查，不在此轮跨项目修改。
- **仅参考 UX/算法**：Desktop Studio panel/filmstrip/history 和 TypeSet 图层/队列；Android 原生 picker/share/lifecycle/安装。完整 Controller/ViewModel 不当共享核心。
- **Suite 新实现**：资产库、工程版本/恢复、账户身份及任务快照、持久 Server 队列/授权、跨端资产协议、完整更新协调、Recipe 对比体验。

推荐依赖方向：Windows/Android/Server → Suite domain/asset ports；生成适配 → 固定 CCB 图像核心；Windows/Android 渲染 → 通用编辑操作。Server 只依赖协议/领域和显式可用的请求核心，不依赖 Compose、AWT、Android Context。将来 Desktop 可以消费通用编辑模块，但该模块不得依赖 Suite app/Server/图库实现；完整 Desktop 与完整 Suite 互不依赖。

独立 Studio 可以调用 `designForPromptToolDetailed/reviseForPromptTool`，角色卡输入变为可选导入适配，Suite composition root 注入 repository、transport、tag assets、task lifecycle。内部 ChatMessage 载体可以暂留权威模块，不能为摆脱类型名而重写 Prompt；真正去实体耦合需另立等价抽取合同。

## D：账户、fallback 和归属

F `docs/desktop/146_MULTI_API_ACCOUNT_PROFILES_CONTRACT.md:4` 明示 NOT IMPLEMENTED。D 的 ModelApiKey(modelId) 与全局 NAI slot 不等于统一账户档案。建议 Suite 非秘密 profile 保存 stable accountId、服务类型、用户明确标注的平台/别名、endpoint、model config、SecretRef；请求启动时冻结有效选择/解析依据、Recipe、模型、重试预算与任务归属，秘密只在执行边界短时取得，不序列化入 Recipe/metadata/普通日志。

本人直连保持既有 per-model/global fallback 和本地 HTTP 例外；删除或禁用正在被引用的档案需显式阻止或呈现影响，不能轮换另一账户补发。Server 则由授权策略在派发前选择管理员 upstream account，冻结绑定；用户配额/队列和上游速率/支出分开。用户/设备 ID、workspaceId、jobId 均非凭证；所有任务/资产访问都做授权，客户端不给任意 Endpoint/本机路径。结果不确定的已发送请求不自动重排。

## E：保全与恢复建议

推荐 **不可变原始文件 + 版本化工程/Recipe 文档 + 可重建 SQLite 查询索引 + 小型提交日志**。比较及故障矩阵见 [差距表](A0_001_DEPENDENCY_AND_GAPS.md)。原始文件必须在 decode/normalize/compose 前保存并验 size/hash；原始上游 bytes、引导标准化 PNG、合成输出分别建资产关系。索引不拥有唯一创作资料；工程/设计历史不允许隐式100条 prune。未知格式、损坏和读错误只读保留并显式报告，不能默认空对象再保存。

文件与 DB 跨介质提交必须有意图/落盘/引用发布和启动 reconcile；恢复先核对日志和哈希，把“已收到但未登记”保留成可领取结果。没有证据不宣称断电耐久性。备份覆盖原始、文档、关系与校验清单；索引可重建，secret 默认排除；恢复先到新根校验，再维护提交，不覆盖唯一原数据。

## F：首版完整更新

建议 Windows 首个安装版优先验证 **MSIX/系统安装设施 + 应用内检查/下载/可信验证/排水/重启确认**，保持数据在独立应用根。微软要求可部署 MSIX 有有效且设备信任的签名；App Installer 提供更新配置，但不能替 Suite 做工程 flush/任务维护。依据：[签名](https://learn.microsoft.com/en-us/windows/msix/package/signing-package-overview)、[更新配置](https://learn.microsoft.com/en-us/windows/msix/app-installer/update-settings)。这是方案推荐，未冻结 Publisher/证书/产品 ID，也未承诺 Java/Compose 打包兼容已通过。

Windows 候选比较：MSIX 有系统身份/安装事务但需实际打包/权限/数据根验证；Velopack 有应用更新框架（[官方入口](https://docs.velopack.io/)），本次 Java/安全子页未成功取得，不批准选型；自写替换器维护/恢复面最大，不推荐。Portable 若交付，另有独立 updater/版本目录切换和回退合同，不能沿用“覆盖整个目录”。

可信链最低要求：内置可信根→经过验证的目标元数据（产品/平台/架构/渠道/数据兼容、大小/hash、版本/有效期）→签名包/安装身份→安装后版本与资料检查。回滚/冻结/密钥轮换按成熟更新框架设计，候选 TUF，**实现库、版本和 JVM/Android 兼容性未核验，不准手写一个签名 JSON 冒充 TUF**。[TUF 安全模型](https://theupdateframework.io/docs/security/)说明回滚、冻结、密钥风险及新鲜度问题。此段是 Suite 推导的设计要求，不是上述 Windows API 自动具备的证明。

Android 采用独立 applicationId、稳定签名、app-private updates、应用内下载与校验后系统安装，处理确认/拒绝/取消/失败，再下次启动核验安装与数据。参考 [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller)、[Android 签名](https://developer.android.com/studio/publish/app-signing)。不绕过用户确认；`InstallerLaunched` 不能报完成。Server 维护只由管理员发起，队列/收到结果/不确定状态先持久化，升级不清库、不盲重发。

内部 A→B 真实升级链演练仍是首个长期使用版的阻塞门槛。A1 的普通文件故障 harness 只能证明数据提交行为，不能替代 installer/签名/重启/用户验收。A0 未创建密钥、下载应用包、安装工具或运行更新器。

## G–I：反哺、许可与未集成变化

反哺候选：图像核心由 Desktop 发布窄模块后双方消费；Suite `image-editing` 的平台无关层/操作由双方消费；更新协调接口及数据恢复经验按独立目标合同移植。每项需要来源版本/测试、目标审查、目标实施、目标验收，当前均 CANDIDATE，不能把本报告当 Desktop 采纳记录。

许可缺口：D Git tree 定向查 LICENSE/NOTICE/COPYING 文件名没有命中（不等于法律上无人授权）；已读 Danbooru notice 只记“ChatBar owner confirmed permission”，尚不能证明 Suite 公开再分发范围；ECDICT notice 是 MIT，需保留其 notice/生成元数据；tokenizer、codex/catalog、品牌图标与字体分别核验。TypeSet 三文件没有因此获得新的公开许可证。A0 不复制生产源码、不增加统一 LICENSE；内部合成实现可继续候选审查，公开采用/打包对应来源前必须补齐证据。

D→F 的定向 diff 显示共享 image、model 目录及 `DesktopNovelAiGenerationRuntime.kt` 没有变化；Prompt 目录新增 FishAudio/FormatRepair authority 是域外增量，未引入。`JsonFileStorage.kt` 在 `686aea0c35ae09159a44c3b10c623a1abff44668` / `46598a9edec0edbef04c88184020bf16572c05aa` 触及 format-repair inventory，**未证明为 Suite 原图存储所必需**；保留观察，不 cherry-pick。F→L 是音频/文档/聊天截图增量，不把当前 HEAD 整体作为依赖。U 最新图像/备份范围只做清单和更新器/credential 选读，未逐项审完全部221文件；本轮没有发现需要强行吸收未集成 feature 的证据。

## 验证与停止

原包严格模式首次 FAIL 仅因为两份增量文件 EXTRA，随后显式 `-Root H:\CCB-Studio-Suite -AllowExtraFiles` **36/36 size+SHA-256 PASS（编辑前）**。三份 TypeSet bytes 与 manifest 一致；HTML 从未执行，无字体/CDN 下载或浏览器数据访问。

默认 Root 在 Windows PowerShell 复现 FAIL：`Verify-Package.ps1:4` 的 `$PSScriptRoot` 在默认参数求值时为空。独立精确修复建议：将参数改为 `[string]$Root = ''`，在 `$ErrorActionPreference` 后、Resolve-Path 前增加 `if ([string]::IsNullOrWhiteSpace($Root)) { $Root = Split-Path -Parent $PSScriptRoot }`，余下校验逻辑不变。**本轮未修**；之后若批准，原 BUNDLE_MANIFEST/SHA256SUMS 保留为不可变交付证据，用单独工作区修复证据验证默认/显式参数一致，不悄改原始清单。

最终变更、链接/JSON/ignore/内容扫描、外部状态复核及实际结果见 [差距与交付回执](A0_001_DEPENDENCY_AND_GAPS.md)。Gradle、模拟器、产品测试、打包、签名、付费 API、真实用户资料和实际安装更新均 **NOT RUN / NOT TESTED**。A0 文档完成后停止，下一动作仅 A0-002 Project review；A1、首次 Git 发布都要另行授权。
