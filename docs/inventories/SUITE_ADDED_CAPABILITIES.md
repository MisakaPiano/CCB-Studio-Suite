# Suite新增与扩展能力实现清单

按图像工作室基础之上的资产/工程/交付、平台适配、运维和跨端规划列出。与继承能力有交叉，不能把原有TypeSet排版或CCB生图算法标成原创。测试结果链接只证明其限定范围；尚未回填证据与未实现明确保留。

|ID|能力|实现/验证状态|对应文件与证据|
|---|---|---|---|
|SUITE-001|资产原件字节保留及独立工程/导出|IMPLEMENTED_AUTO_HUMAN_PARTIAL|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt<br>docs/audits/UX_DAILY_USE_READINESS.md|
|SUITE-002|统一持久图库、别名与标签|IMPLEMENTED_HUMAN_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/GalleryWorkspace.kt<br>docs/audits/UX_LIBRARY_PROMPTS.md|
|SUITE-003|多收集目录、变更版本、重新授权|IMPLEMENTED_HUMAN_PENDING|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryDirectoryMonitor.kt<br>docs/audits/UX_DAILY_USE_READINESS.md|
|SUITE-004|独立Prompt资产与AI候选/图片/文字主动加入参照|IMPLEMENTED_AUTO_DEDICATED_HUMAN_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/DesignPromptReference.kt<br>docs/audits/UX_NAI_BACKGROUND.md|
|SUITE-005|多图Recipe比较、角色展示位置对齐、字段摘取|IMPLEMENTED_AUTO_DEDICATED_HUMAN_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/PromptReferenceBoard.kt<br>docs/audits/UX_LIBRARY_PROMPTS.md|
|SUITE-006|语言/视觉/NAI分类、生效状态、列表与默认选择|IMPLEMENTED_CONFIGURATION_AND_ACTIVE_LAYOUT_HUMAN_SCOPED_PASS|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioModelAccounts.kt<br>docs/audits/UX_ACCOUNT_ROLES.md|
|SUITE-007|独立视觉模型与共用默认凭据|IMPLEMENTED_CONFIGURATION_HUMAN_PASS_ACTUAL_VISION_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioModelAccounts.kt<br>docs/audits/UX_VISION_CREDENTIALS.md|
|SUITE-008|模型用途提示与不兼容用途拦截|IMPLEMENTED_AUTO_HUMAN_SCOPED_PASS|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/ModelPurpose.kt<br>docs/audits/UX_MODEL_PURPOSE.md|
|SUITE-009|生成任务不确定状态与禁止盲目重发|IMPLEMENTED_HUMAN_PENDING|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/CreationSession.kt<br>docs/audits/UX_DAILY_USE_READINESS.md|
|SUITE-010|设计、应用草稿、最终生成的来源链|IMPLEMENTED_HUMAN_PENDING|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/DesignProvenance.kt<br>docs/audits/UX_DAILY_USE_READINESS.md|
|SUITE-011|原生多工程图片编辑与历史|IMPLEMENTED_HUMAN_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/TypesetWorkspace.kt<br>docs/audits/UX_EDITOR_CLARITY.md|
|SUITE-012|连续嵌字样式与别名预设|IMPLEMENTED_HUMAN_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/TypesetWorkspace.kt<br>docs/audits/UX_EDITOR_CLARITY.md|
|SUITE-013|批量交付及多文件剪贴板|IMPLEMENTED_SINGLE_PNG_HUMAN_PASS_BATCH_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/ExportClipboard.kt<br>docs/audits/UX_NATIVE_CLIPBOARD_ELEVEN.md|
|SUITE-014|资产回收及工程引用保全|IMPLEMENTED_HUMAN_PENDING|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/AssetArchive.kt<br>docs/audits/UX_DAILY_USE_READINESS.md|
|SUITE-015|资料损坏拒绝默认覆盖及单工作区所有权|IMPLEMENTED_AUTO_HUMAN_PARTIAL|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt<br>docs/audits/UX_DAILY_USE_READINESS.md|
|SUITE-016|完整备份、恢复新目录及凭据排除|IMPLEMENTED_HUMAN_SCOPED_PASS|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioRestore.kt<br>docs/audits/UX_NATIVE_RESTORE.md|
|SUITE-017|资料格式升级建立副本|IMPLEMENTED_AUTO_HUMAN_PARTIAL|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioFormatMigration.kt<br>docs/audits/UX_RESTORED_UPDATE.md|
|SUITE-018|Windows原生窗口、文件选择及拖放|IMPLEMENTED_HUMAN_PARTIAL|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioApplication.kt<br>docs/audits/UX_FIXED_ELEVEN.md|
|SUITE-019|安全GitHub应用内下载、安装及重启|IMPLEMENTED_HUMAN_SCOPED_PASS|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioUpdateHandoff.kt<br>docs/audits/UX_LONGTERM_UPGRADE.md|
|SUITE-020|项目Release迁移与旧渠道兼容|IMPLEMENTED_HUMAN_SCOPED_PASS|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioUpdates.kt<br>docs/audits/UX_RELEASE_MIGRATION.md|
|SUITE-021|长期签名、加密云备份和独立恢复码|IMPLEMENTED_RECOVERY_SCOPED_PASS|tools/update-runtime<br>docs/audits/UX_SIGNING_CUSTODY.md|
|SUITE-022|新旧渠道自动续签及离线targets维护|IMPLEMENTED_MANUAL_RUN_PASS_NATURAL_SCHEDULE_PENDING|tools/update-runtime<br>docs/audits/UX_RENEWAL_PREFLIGHT.md|
|SUITE-023|Suite Android完整独立创作|NOT_IMPLEMENTED|未实现/待核对|
|SUITE-024|Suite Server管理员上游凭据及多用户队列|NOT_IMPLEMENTED|未实现/待核对|
|SUITE-025|跨Windows/Android/Server资产与任务协议|NOT_IMPLEMENTED|未实现/待核对|
|SUITE-026|词库独立后台任务、可见进度与就地取消|IMPLEMENTED_AUTO_NEW_BACKGROUND_UI_HUMAN_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/NaiSettings.kt<br>docs/audits/UX_NAI_BACKGROUND.md|
|SUITE-027|AI设计独立可调窗口和共享未发送输入|IMPLEMENTED_AUTO_WINDOW_HUMAN_SCOPED_PASS|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioApplication.kt<br>docs/audits/UX_NAI_BACKGROUND.md|
|SUITE-028|单一生图模型、动态费用和单机本地停止|IMPLEMENTED_AUTO_MODEL_AND_COST_LAYOUT_HUMAN_SCOPED_PASS|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/NaiSettings.kt<br>docs/audits/UX_NAI_BACKGROUND.md|
|SUITE-029|图片查看器悬浮翻页、右键及直达编辑/APNG|IMPLEMENTED_AUTO_VIEWER_ENTRY_HUMAN_SCOPED_PASS_APNG_DELIVERY_PENDING|modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioAssetViewer.kt<br>docs/audits/UX_NAI_BACKGROUND.md|
|SUITE-030|多套APNG封面、默认恢复、自动预览与批次缩略图|IMPLEMENTED_AUTO_CUSTOM_COVER_AND_PREVIEW_HUMAN_PENDING|modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/DisguiseCovers.kt<br>docs/audits/UX_APNG_COVERS.md|
