## 2026-10-25 PERF-003批次投影优化（源码/自动检查完成，未发布）

固定实现98666fb，最终9个唯一定向方法PASS；1万张/1000批次四轮交替对照中位数125.6→7.9ms（含索引构建），复用1.9ms，仅元数据计算，不外推整页/RSS。见[实现/基线/限制](audits/PERF_ORGANIZATION_20261025.md)和[原始证据](audits/evidence/PERF_ORGANIZATION_20261025/SOURCE_MAP.json)。性能独立复审PENDING，不打包/发版。前一下载清理P2已获源码限定关闭；资产/编辑USER_NOT_TESTED、q09 FAILED/UNKNOWN、115体验/Prompt比较PENDING及PRODUCT_HOLD / NOT RC保留。

## 2026-10-25 Project下载清理源码P2限定关闭；接续性能专项

已读[Project原始关闭回执](audits/PROJECT_DOWNLOAD_TERMINAL_CLOSURE_20261025.md)，仅f6598d3源码/54a5ba26证据的P2 CLOSED_WITH_SCOPE，旧包不含修复、包集成未验。用户后续授权继续[性能专项](../tasks/PERFORMANCE_RESOURCE_PROGRAM.md)，本轮先PERF-003合成批次投影基线及有证据的定向优化，不发布。资产/编辑USER_NOT_TESTED、q09 FAILED/UNKNOWN、115体验与Prompt比较PENDING及PRODUCT_HOLD / NOT RC保持。以下为历史时间线。

## 2026-10-25 Project剩余下载终态P2：源码修复，未重打包

Project对134dd47限定关闭四个残余并接受旧凭据快照补证，下载引用终态仍有P2。现以f6598d39298c06ec5644a131e484e814f77b6f45修复，24个唯一清理回归通过（不累加重复轮次），待此项复审。**没有重打包，原2484858内部EXE不包含这次修复。**公开115不变，无UI/真实服务/日常资料或新历史目录清理。q09仍FAILED且根因UNKNOWN；115体验、Prompt对比继续PENDING，PRODUCT_HOLD / NOT RC保持。见[修复/源码包边界与原始证据](audits/DOWNLOAD_TERMINAL_FIX_20261025.md)。

## 2026-10-25 复审残余返工、已授权清理与Desktop参考交接

固定源码248485824e1d6d2201ceedf7d14ff66e251d344d，应用构建59283a0，408项应用输入一致；最终18 JVM/56 Python定向通过，原生A→B双合成DPAPI凭据/3资产、COMPLETE后完整清理和B正常关闭通过，内部包未发布。首轮JVM/原生失败及额外画布q09失败保留，后者未扩大UI范围。用户直接授权的20根历史残留清理完成，回收8506611341字节（7.92GiB），保护111/当前115、备份、固定基线与原审查证据；未知归属未动。

新源码独立复审PENDING，115体验与Prompt比较分别PENDING，PRODUCT_HOLD / NOT RC保持。见[Project定向交接](../handoff/PROJECT_REVIEW_RESIDUALS_20261025.md)、[Desktop技术参考](../handoff/DESKTOP_UPDATE_TECHNICAL_HANDOFF_20261025.md)、[人工清理回执](audits/HISTORICAL_CLEANUP_EXECUTED_20261025.md)。Desktop仅固定对象只读比较，没有修改、启动开发或发布；公开最新仍Preview115。

## 2026-10-25 六项定向修复与高优先级清理机制

固定源码da2ae81、未发布内部包；38 JVM/49 Python与真实原生合成更新证据。见[Project复审交接](../handoff/PROJECT_SIX_FIXES_CLEANUP_20261025.md)、[实现与限制](audits/REVIEW_FIXES_AND_CLEANUP_20261025.md)、[生命周期规则](rc/OWNED_ARTIFACT_LIFECYCLE.md)和[历史清理只读清单](audits/HISTORICAL_CLEANUP_REVIEW_20261025.md)。待独立复审，115体验及Prompt对比PENDING，旧审查包不变，无新公开发布。

## 2026-10-24 下载入口与阶段独立复审交接（文档完成）

主库main README提交8107754、旧发行库main README提交e47d47a，醒目入口指向实际最新Preview115及全部版本，明确Assets安装器与实验限制。公开页面/链接回读通过；Releases区块开启，旧release/tag/附件、两库元数据分支及工作流未改，无新版本或自动化。阶段交接冻结ff145d0/115/安装器SHA，最后可核实Project审查为678caf1创作工程限定PASS，非全项目验收；115用户试用结果PENDING。见[阶段交接](../handoff/PROJECT_PHASE_REVIEW_20261024.md)及[本轮实际变更](audits/PUBLISH_ENTRY_PROJECT_HANDOFF_20261024.md)。

## 2026-10-24 Preview115 批次组织、当前会话与固定入口已发布

[115更新](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.115)已签入原渠道，源码/组装ff145d0。46项冻结回归与13项安装/交接检查实际PASS；551文件安装/拒绝覆盖、公开107→115原生B回执及115已是最新检查通过。手动当前批次重开保留、命名归档清空，双类型时间戳与Project/Chapter归属，Explorer折叠树、常驻把手和紧凑菜单接入。更新页可创建桌面/开始菜单固定入口，旧任务栏图标需取消并重固定一次；实际任务栏人工验收待用户反馈。父进程排水为明确替身，日常资料不自动切换，无真实生成。两次精确授权清理共1515880979字节，H盘约776.5MB可用，安装包/基线/证据保留。Prompt对比PENDING。见[实际证据](audits/UX_ORGANIZATION_SESSION_115.md)。

## 2026-10-24 Preview114 图库布局与菜单字号已发布

[114更新](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.114)已签入原渠道；源码/组装f59b0c2已推送。73项冻结实际回归PASS；按原demo纠正组织树/批次卡片，收起与展开统一在分割线（保留拖宽），逐页下拉统一紧凑字号。22份菜单源码核对及实际展开截图/字号检查完成，原有图库入口、原件与来源保留。550文件安装/拒绝覆盖、实际107合成资料→114原生交接、公开下载及114当前检查通过；A父进程排水为明确替身，未代签日常真人更新或新样式验收。113布局拒绝事实保留。Prompt对比PENDING，无真实生成，日常安装未自动切换。见[证据](audits/UX_GALLERY_LAYOUT_114.md)。

## 2026-10-24 Preview114 布局与菜单纠正进行中

用户拒绝113组织树布局偏差；技术回归通过不等于样式验收。按原demo补常驻批次/虚拟相册根、自定义组织分区、选中背景及新建图标，使用丰富合成资料核对实际渲染。最新要求：收起/展开统一置于分割线，悬浮方向提示，保留拖宽；逐页自查下拉字体并统一按钮/菜单字号。原入口/原件/来源保留，Prompt对比PENDING。114未发布。

## 2026-10-24 Preview113 图库组织已发布

[113更新](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.113)已签入原渠道；源码/组装c0bb2a2已推送。59项冻结实际回归PASS，图库原有入口保留，组织树可拖宽/收起，标签栏分隔；虚拟相册只改关联，批次/分组来源从不可变回执保留。550文件安装/拒绝覆盖、实际107合成资料→113原生交接及公开下载/当前检查通过；A父进程排水为明确替身，未代签日常真人更新。Prompt对比继续PENDING，无真实生成，日常安装未自动切换。见[实际证据](audits/UX_GALLERY_ORGANIZATION_113.md)。

## 2026-10-24 Preview112：批次历史与安全停止已发布

[112更新](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.112)已签入原更新渠道，应用/组装acf71fc已推送。40项冻结实际回归通过；暂停和结束均保留已发送请求的响应，暂停可继续、结束不可直接继续。新增独立多步撤销/重做、完整底部工具对应右键入口及键盘导航。550文件安装、实际107合成资料→112原生交接回执/测试固定入口、公开下载和112已是最新检查通过；父进程排水在探针中为明确替身，日常真人更新及新原生交互未代签。见[实现/证据](audits/UX_BATCH_HISTORY_112.md)。429整批手动恢复和冷却保留，Prompt继续PENDING，无真实生成，日常安装未自动切换。

## 2026-10-24 Preview111已发布并核验原更新入口

[111更新](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.111)已签入现有渠道，应用/组装1c9385a已推送。37项冻结实际回归、550文件安装/拒绝覆盖、实际107合成资料→原生111回执与固定测试入口、公开下载和111最新检查通过；A进程身份/排水在探针中为明确替身，日常用户更新仍未验。按原demo补齐批次入口、固定栏进度、分组进度及全部待生成位置；更新子进程临时文件使用缓存所在盘。C盘清理538份旧构建诊断日志，约351.55MiB，依赖/资料保留。发布初期CDN曾返回110，未把旧目标记为111通过。见[111证据](audits/UX_BATCH_PARITY_111.md)。Prompt继续PENDING，无真实生成，日常安装未自动切换。

## 2026-10-24 Preview110已发布，111按demo补齐

[110跨版本启动修复](audits/UX_UPDATE_REPAIR_110.md)已发布：c9e4607源码/组装、35项冻结回归、实际107合成资料→原生110启动回执及公开下载/安装/固定入口/当前检查通过；A父进程在探针中为明确替身，日常用户更新仍未验。用户在测试窗口已看到批次入口和工作台，但拒绝样式差异，正在按原demo补齐111入口、分组进度与右侧完整任务格，及更新临时空间归属。已按用户授权清理C盘约351.55MiB旧构建日志，依赖及资料保留。见[111范围与证据](audits/UX_BATCH_PARITY_111.md)。Prompt继续PENDING，无真实调用，111尚未发布。

## 2026-10-23 Preview108 大图资源更新已发布

[108更新](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.108)已签入原渠道。33项最终回归、107实际公开下载/安装108及当前检查PASS；应用c17ceb2/组装04719be已推送。大图读取减半，24次合成显示转换累计分配481→309MB、耗时3.46→2.49秒；60轮页面和60轮大图切换通过，非小时级无泄漏或整机RSS下降结论。旧程序550文件、合成资料247文件不变，日常安装未自动切换；108原生重启待用户正常使用确认。Prompt继续PENDING。见[范围与证据](audits/UX_PERFORMANCE_108.md)。

## 2026-10-23 Preview107 大图库性能更新已发布

[107更新](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.107)已接入原签名渠道。62项最终回归、106实际公开下载/安装107及当前版本检查PASS；代码9abc967/组装5907779均已推送。万项合成元数据：名称排序407→39ms，搜索243→3ms，多选107→4ms，标签585→13ms；非整UI结论。实际107合成资料暖空闲30秒CPU78ms、工作集约350MiB，正常关闭退出；截图超时不冒充视觉通过。用户已确认日常应用更新重启正常、图库与参数保留；随后截图确认107已是最新版且程序/资料/凭据状态验证完成，限定人工更新验收PASS。Prompt继续PENDING，性能专项继续。见[完整范围与证据](audits/UX_PERFORMANCE_107.md)。

## 2026-10-23 Preview106 性能更新已发布

[106更新](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.106)已上线并签入既有渠道。图库/回收站卡片及资产选用器改用静态256px缩略图，与队列共用8MiB像素LRU、双解码并发和取消排水；24张合成PNG保留像素72→4.5MiB、冷解码1.35–1.71s→0.25–0.27s，非整机RSS结论。52项最终回归、105实际公开下载/新目录安装和106当前检查PASS。日常独立105入口未改，可在应用“更新”获取106；原生一键重启因自动窗口捕获超时仍未验，当前不安排用户电脑操作。Prompt保持PENDING，长期性能专项继续。代码e08429e/组装839205b，见[实际证据](audits/UX_PERFORMANCE_106.md)。

## 2026-10-23 独立安装的持续更新已核验

105自带更新运行时实际连接项目签名渠道，续签run39945483509成功且保持105目标原字节，独立客户端接受targets4/snapshot10/timestamp10。最终45项回归通过，连续两次合成换版的固定入口/资料参数保持；已安装547文件重新核验通过。只有测试/运维记录变更，无新应用包、无真实创作调用。后续从现有固定入口使用、在应用内更新；下一份实际发行时补105→新版本原生重启，不冒充已通过。见[证据](audits/UX_UPDATE_CONTINUITY.md)与[持续发布约束](rc/PREVIEW_UPDATE_CONTINUITY.md)。

## 2026-10-23 独立105安装与清理完成

80项已获精确批准并删除，实际回收15.34GiB。用户随后要求使用版移出源码仓库，当前入口为`H:/CCB-Studio-Suite-Preview/Launch-CCB-Studio.lnk`：独立版本程序、data/workspace与data/runtime已配置，5016资料文件复制/散列匹配、加密凭据可读，547程序文件核验通过；原资料保留为备份，仓库旧入口retired。`.local/ACTIVE_STUDIO_INSTALLATION.json`已更新，禁止误清理其活动路径及备份字段。迁移后H盘约14.79GiB可用，未调用真实服务、未上传私人资料/凭据，原生界面启动未测。见[实际接入与清理记录](audits/UX_LOCAL_PROFILE_AND_CLEANUP.md)。

## 2026-10-23 Preview 105 已发布

状态 **INVITED_PREVIEW_PUBLISHED / NOT RC**。[邀请Preview105](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.105)与现有签名更新渠道已上线。组装源码a416a82（应用6755068），57项最终合成回归、547文件新目录安装/拒绝覆盖通过；公开完整安装器流式下载及105已是最新检查通过。额外104→105本地分段下载/安装演练初次因磁盘空间保护停止，未验不记通过，清理本轮两处临时产物待用户答复。用户明确附加编辑/排版等人工后置，不再安排当前电脑操作；Prompt新设计PENDING。见[实际发布与证据](audits/UX_INVITATION_PREVIEW_105.md)。main、Desktop/upstream、旧桥接渠道未改，私人资料未读。

## 2026-10-23 邀请 Preview 优先收尾

用户要求多来源 Prompt 工作台设计先 **PENDING / 邀请后续做**，本轮优先完成邀请 Preview；既有功能保留，不代签完整比较专项。用户补充确认 AI 设计→应用→生图已试过，正常重开后图片、Prompt/模型/参数保留。用户随后明确离开电脑期间不再人工，以生图/图库为核心尽快发布实验Preview；编辑交付/恢复等附加人工后置为已知待验。当前剩余最新安装/更新自动验证及发行材料，见[Preview 收尾范围](rc/INVITATION_PREVIEW_105.md)。此前已通过图库/标签/磁贴等项目不重测；未发布前仍为 PRODUCT_HOLD / NOT RC。

## 2026-10-23 多来源Prompt设计待选（历史，现已暂缓）

图库59e2fe8四项已获人工通过并推送403d9b0。按用户要求，下一组Prompt先设计后实施：[底部/右侧摘取工作台提案](rc/PROMPT_REFERENCE_DESIGN_PROPOSAL.md)含三来源直入、字段/参数、角色展示对齐和摘取确认。概念交互及两布局三宽度合成QA通过，非生产通过；等待选布局，不改实际Prompt页面、不请求服务、不发新包。

## 2026-10-23 磁贴、拖动、目录导入与选中样式纠正

用户再次拒绝e469723包视觉，明确要求整块磁贴、跟手拖动、恢复文件/目录导入菜单，以及普通多选仅描边/蒙版、勾选模式仅一处标准复选框。[具体修复](audits/UX_GALLERY_DEMO_PARITY.md)已完成。用户对59e2fe8固定包回复“这四项正常，手感和显示符合要求”，四项限定人工PASS，见[回执](audits/evidence/UX_GALLERY_TILES_SELECTION/HUMAN_ACCEPTANCE.json)。不重测这组；继续多来源Prompt先设计后实施及其余邀请门槛，未测项目不代签。

## 2026-10-23 已确认demo样式偏差修复

固定330cee35包的图库交互、标签/回收站恢复与重开获限定人工通过；APNG组用户答“暂未测试”。随后用户拒绝实际页面外观，要求严格按已认可demo还原并核查功能遗漏。当前优先修复视觉与入口差距，邀请版保持PRODUCT_HOLD；不以先前交互回执代签样式。见[样式/功能对账](audits/UX_GALLERY_DEMO_PARITY.md)。已恢复原demo分区、卡片、窄详情和回收站紧凑列表，补独立导出与四类来源；21项回归及最后12项定向通过（有重叠），113份权威文件不变。新视觉等待用户确认。

## 2026-10-23 图库/回收站统一交互实装，邀请版连续推进

用户已认可概念并明确授权继续至邀请 Release，长期分类/tag映射与广泛性能专项后置。正在接入共享筛选/导航/标签浏览、批量标签安全撤销、默认隐藏勾选框、Ctrl/Shift/框选与拖到用途、可调详情及回收站多选/筛选恢复；APNG浮层菜单与队列选择同步改进。[实际实现与证据](audits/UX_GALLERY_RECYCLE_INTERACTION.md)：广泛桌面45项通过、最后核心/桌面24项定向通过（有重叠），构建成功；正在冻结固定包，新原生人工尚未验收。旧原件/权威113文件/原大目录暂停边界保留；邀请剩余多Prompt专项、编辑交付恢复及固定安装候选按事实推进，不以先前概念或旧包回执代签。

## 2026-10-23 回收站入口与长期标签计划

已核实旧包可从图库图片工具或资产管理进入回收站，当前为保留原件/引用的可逆隐藏与单项恢复；概念稿遗漏的独立入口已补成工具区固定“回收站N”，并补混合资产预览/搜索/类型、多选恢复和空状态，v6合成概念通过，非生产验收。用户确认的[长期标签管理/分类/原始tag映射计划](../tasks/TAG_MANAGEMENT_PROGRAM.md)已正式登记为PLANNED，未启动整个长期模块；当前图库接入照常，PRODUCT_HOLD / NOT RC。

## 2026-10-23 图库设计获认可，拖动预览与标签溢出补齐

用户追加要求正常悬浮菜单、默认关闭的勾选模式与Ctrl/Shift/框选、紧凑内置搜索/日期动作、相册整卡进入，以及独立的访问历史/上一级导航；并要求主动考虑双击/右键/拖动到用途。已做可交互概念和[操作规则提案](rc/GALLERY_INTERACTION_PROPOSAL.md)，实际隔离浏览器验证指定概念交互及多宽度，不是生产修复或新包。第二轮概念已按用户修订合并导航筛选、去除并列APNG页签，补可调预览、工具图标/磁贴与自定义、实际所选内容进入用途及批量标签分组演示；目录同步仍会入库保留原件，不是纯路径引用。用户已同意第二轮布局，追加标签块/数量/联想及撤销重做/剪切复制粘贴删除和快捷键；新增概念已合成验证并明确文本焦点、回收站/队列与只读目录边界，仍待接入生产页面。用户对本版设计给出仅缺拖动预览前提的认可；已澄清并在概念补缩略图/数量/目标与Esc取消，标签采用单行横滚、固定输入、长名提示及“全部N”管理，v4合成验证通过而非生产通过。原“全部N”现澄清为“已选N”条件管理；两个输入均补“浏览已有标签”多选追加，v5概念验证覆盖搜索、去重、取消保留草稿与焦点隔离。按认可设计进入后续实际接入，不重问基础布局；之后Prompt仍先设计后实施；上轮导入/指定操作限定PASS保留，不扩展为所有组织体验通过。原大目录仍暂停，PRODUCT_HOLD / NOT RC。

## 2026-10-23 导入性能、队列与紧凑页面新包

用户已限定确认aaf98b4包两组pass；另报3图约1分钟/24图约10分钟及布局/队列问题，现已处理并保留原回执。合成80旧图+24新图基线39.919秒，首次优化10.610秒、最终8.729秒；不外推真实库。存储/核心42项及最终桌面20项各轮限定证据见[记录](audits/UX_IMPORT_QUEUE_LAYOUT.md)。新包`.local/windows-rc/import-queue-layout-review/Start-import-queue-layout-review.lnk`220文件核验，继续暂停收集；用户已对fe9e821新包回复“明显改善，阶段和用时清楚”“队列和这些布局都正常”，限定签收导入改善/阶段反馈及队列/本轮布局，见[回执](audits/evidence/UX_IMPORT_QUEUE_LAYOUT/HUMAN_ACCEPTANCE.json)；未提供人工精确耗时，不外推全程序资源或大库。性能资源专项独立维护PERF-001–007，多Prompt重点仍单列，PRODUCT_HOLD / NOT RC。

## 2026-10-23 长期性能与资源专项

用户要求单独维护全程序响应与资源优化，已建立[专项合同/工作表](../tasks/PERFORMANCE_RESOURCE_PROGRAM.md)：PERF-001导入优先，随后启动/切页、搜索分组、预览、交付、收集及空闲CPU/内存/磁盘基线。不是定时任务，不扩大真实资料/服务调用授权，不因单项改善关闭专项。

## 2026-10-23 相册排序与紧凑APNG工作台

已补常驻折叠控件、未变生成记录缓存、根及每相册独立自然排序，APNG紧凑分类工具栏、本页文件/目录本层/拖放/粘贴及结果专用右键。相关25项回归、最后边界2项（有重叠）通过；220文件新包`.local/windows-rc/gallery-sort-apng-review/Start-gallery-sort-apng-review.lnk`逐项核验，继续暂停目录收集。合成输入album-A/album-B/apng-batch便于直接测试。见[实现与证据](audits/UX_GALLERY_SORT_COMPACT_APNG.md)，新增原生交互待验；此前已通过项保留，重点多Prompt仍单列。三类表33项扩展，Desktop转交报告更新；PRODUCT_HOLD / NOT RC。

## 2026-10-23 可视封面库及图库目录分组

用户已确认5563a3a包目录分组清楚、滚动条正常，独立封面库布局符合预期、两套管理及重开保留正常。[实现与限定回执](audits/UX_COVER_LIBRARY_GALLERY.md)保留独立自动预览/缩略图、APNG实际交付和多Prompt专项待验；此前目录恢复/小同步限定人工通过仍保留。三类能力表同步Suite31项，原大目录继续暂停，PRODUCT_HOLD / NOT RC。

## 2026-10-23 旧窗口目录收集阻塞

用户已确认cc93735恢复包暂停打开正常，配置、目录和已入库图片可见；随后合成小目录单次同步、切页及两张纯色图片可见也通过。[限定人工回执](audits/evidence/UX_COLLECTION_RESPONSIVENESS/HUMAN_ACCEPTANCE.json)。原大目录继续暂停，其重扫、进度/停止交互及全量完整性仍未签收；下一组为原先待验的APNG交互。

用户澄清为旧测试窗口；线程诊断确认整批收集持锁、图库在界面线程等待。经用户明确允许后仅结束已核验的旧Suite进程，未删资料。[诊断与修复](audits/UX_COLLECTION_RESPONSIVENESS.md)记录后台进度/停止、锁范围和暂停启动恢复入口。最终桌面15项通过，首轮核心10项通过；220文件恢复包核验，快捷方式`.local/windows-rc/library-recovery-review/Start-library-recovery-review.lnk`默认暂停收集。暂停状态恢复已获用户确认，原目录重扫与完整性仍未签收。PRODUCT_HOLD / NOT RC。

## 2026-10-23 APNG封面与自动预览接续

用户追加多套自定义伪装封面、恢复默认、默认静止封面效果、免手点自动预览及当前批次缩略图定位最新。见[实现/验证](audits/UX_APNG_COVERS.md)，保持原件和共享编解码器，原生新交互待签收。新包与快捷方式以该记录最终段为准；先前studio-usability-review为已签收查看器/设计窗口的阶段包。[Desktop报告](rc/DESKTOP_DANBOORU_STATUS_REFERENCE.md)包含词库状态、查看器右键、自定义封面及自动预览边界。PRODUCT_HOLD / NOT RC。

## 2026-10-23 NAI人工澄清与后台/费用整理

用户已确认真实词库更新/检查最新、注释/补全、V5体力及一次NAI生图。完成独立后台词库、单一生图模型/紧凑顶部/动态费用、独立AI设计窗口，取消单机固定频率门槛与全局停止，保留各任务就地取消。补齐AI候选/文字直接加入多Prompt参照、查看器悬浮翻页/右键/当前图编辑及APNG入口。[实现与逐轮证据](audits/UX_NAI_BACKGROUND.md)保留40项、15项、6项及收尾定向结果（有重叠，不累加为独立总数）；用户已确认查看器入口、独立AI窗口和动态显示正常；封面/自动预览新交互仍待测。阶段新包快捷方式为 `.local/windows-rc/studio-usability-review/Start-studio-usability-review.lnk`，220文件逐项核对，不公开发布。[Desktop转交报告](rc/DESKTOP_DANBOORU_STATUS_REFERENCE.md)同时包含词库状态和大图右键；[重点多Prompt人工单](rc/PROMPT_COMPARISON_MANUAL_ACCEPTANCE.md)之后单独验收。三类能力底表已同步Suite30项，仍不是全量语义签收。PRODUCT_HOLD / NOT RC；邀请版剩余连贯编辑/交付/恢复、多Prompt专项、完整创作来源重开闭环和固定安装候选。

## 2026-10-23 NAI配置、Danbooru词库与V5体力

[实现及证据](audits/UX_NAI_SETTINGS.md)：NAI账户与本次生图模型分开；新增创作设置及校验后原子词库更新，中文注释实际连接本地词库；V5显示服务返回的Opus体力，未知不冒充满格。30项回归和最终7项定向复验实际通过，220文件固定本地包逐项核验，快捷方式参数已核对，真实网络词库/V5额度与新界面人工尚未验收。无新公开包，PRODUCT_HOLD / NOT RC。

## 2026-10-23 账户三类管理及生效状态

[实现记录](audits/UX_ACCOUNT_ROLES.md)：顶部显示默认语言、实际视觉来源和NAI账户/生图模型；三类管理及当前生效标记分开，编辑不等于选用，共用凭据不称默认。沿用现有视觉关联，未改格式；向量配置暂不进入创作界面。首轮20项、收尾15项实际通过，220文件新本地包核验。真人新布局待反馈，无新发布，PRODUCT_HOLD / NOT RC。

## 2026-10-23 名称、模型用途及三类清单

[实现与验证](audits/UX_MODEL_PURPOSE.md)：默认凭据/保存/设为当前名称已改清；不兼容用途在列表、保存及传输前三层拦截。20项回归与4项追加UI验证通过，最终本地包220文件核验；新界面真人待测，未发布。[三类来源与实现底表](inventories/README.md)已建立，覆盖固定官方384单元/图片候选207、Desktop集成337及开发快照差异104、Suite扩展25项。待映射/未验证逐项保留，未宣称全部语义审核完成。PRODUCT_HOLD / NOT RC。

## 功能清单入口（2026-10-23）

[清单覆盖范围与入口](FEATURE_INVENTORY_INDEX.md)：已有75项动作实现映射及来源差距/需求/验收文件，但尚无CCB图片全量、Desktop全产品和Suite新增能力三份独立且同步到当前的总表。不将既有75项冒称全量覆盖。

## 模型列表与档案保存修复（2026-10-23）

[完整对照](audits/UX_MODEL_ACCOUNTS.md)：补齐远端列表/筛选选择，修复重复保存新建档案，保存成功接回编辑器并保留原凭据；切页保留草稿，关闭/更新前拒绝未保存档案。119项桌面实际回归通过。固定本地包和真人验证继续，尚未发布新版本。[邀请测试版差距](rc/INVITED_TEST_READINESS.md)列明剩余人工核心流程与更广模型管理差异，不冒称整个Desktop模型管理已经移植。

## Release迁移完成与最新反馈（2026-10-23）

[迁移证据](audits/UX_RELEASE_MIGRATION.md)：100–103及12附件迁回项目Releases且旧包不变；104桥接包新旧两端发布。用户确认103从旧地址升级104并再次检查最新；A/B各547文件、10条合成资产与状态核验通过，104实际使用项目仓库。两端续签运行成功，旧渠道保持104桥接及离线续签责任。仍是PRODUCT_HOLD / NOT RC。用户可人工测试，不需频繁checkpoint；新增模型接口缺少模型列表反馈待对照Desktop核实，见[日常版待办](rc/FIRST_DAILY_USE_READINESS.md)。

## 续签检查修复完成 checkpoint（2026-10-23）

[修复与证据](audits/UX_RENEWAL_PREFLIGHT.md)：版本化元数据缺失/损坏的4个场景已复现并修复，46项回归通过；GitHub部署运行39782382904成功，远端签名链和目标不变已回读。没有改103包或密钥，没有人工测试。源码与发行仓库当前均公开，分仓不是当前鉴权必需；未迁移仓库。接续仍按日常版人工待办，用户暂不能测试。
## 日常版条件核对 checkpoint（2026-10-22）

[剩余条件](rc/FIRST_DAILY_USE_READINESS.md)、[返回后的人工清单](rc/DAILY_USE_MANUAL_CHECKLIST.md)、[自动补验](audits/UX_DAILY_USE_READINESS.md)已保存。最新45项更新/恢复回归通过；103安装547文件和332模块源码核对、105声明匹配、两张合成图片及接收目录准备完成。用户离开电脑且要求保留checkpoint：不开始人工测试/真实API，不重复已通过102→103。下一步用户回来做图库/编辑/导出组，实际提供者最后另确认范围。PRODUCT_HOLD / NOT RC。
## 长期签名102→103完整升级通过（2026-10-22）

[实际记录](audits/UX_LONGTERM_UPGRADE.md)：经用户明确授权发布102/103测试包，长期根真实GitHub应用内下载、安装重启、资料核验及再次检查最新通过；A/B各547文件、10条合成资产与状态快照一致，原备份未变。用户确认并提交103最新截图。44项回归通过，发布目标后的续签工作流成功并回读验证。[Desktop参考报告](rc/WINDOWS_UPDATE_REFERENCE.md)已补充长期渠道来源。无需追加人工操作；首个日常版产品验收、自然定时触发和跨机恢复未完成，PRODUCT_HOLD / NOT RC。
## 长期签名、加密备份与续签部署（2026-10-22）

[操作说明](rc/LONG_TERM_SIGNING_OPERATIONS.md)与[验证记录](audits/UX_SIGNING_CUSTODY.md)：正式四角色密钥已创建，Drive 两份加密备份已下载校验、恢复码路径实际恢复通过，用户确认独立恢复码已保存。43 项测试通过；GitHub 续签工作流实际运行成功，远端签名链回读通过。正式渠道为空，尚无新根安装包；自然定时触发和跨机器恢复未测。PRODUCT_HOLD / NOT RC。以下旧记录按时间保留。
## 真实 GitHub 应用内升级已验证（2026-10-22）

[Desktop 参考报告](rc/WINDOWS_UPDATE_REFERENCE.md)：经用户授权发布测试包后，真实公开 GitHub 100→101 下载、安装、重启、再次检查最新版本完成。A/B 各547文件、10条合成资产与整体状态快照核验通过；原备份未变，原本缺失的凭据仍未配置。人工截图及 COMPLETE 交接证据完整保留；首次夹具误用备份目录导致 FOREIGN_ROOT 的失败也已记录。长期密钥/续签尚未部署，本批是一次性测试信任；PRODUCT_HOLD / NOT RC。

## GitHub 渠道实现与本地安装验证（2026-10-22）

[实施记录](audits/UX_GITHUB_CHANNEL.md)：39 项实际通过，公共 GitHub 传输、共用安装前复核、preview 打包和持久元数据准备已实现；实际 A/B 安装器通过合成传输下载/安装，旧程序与合成资料未变。真实 GitHub 发布/重启尚未验证，正式密钥未建立；不提供完整成功的 Desktop 报告。用户已授权并完成公开发行仓库仅 README 创建；已回读核验，无 Release/安装包，源码仓库权限未变。PRODUCT_HOLD / NOT RC。

## 长期更新渠道方案（2026-10-22）

[具体方案](rc/LONG_TERM_UPDATE_CHANNEL_PLAN.md)：只读参考官方 Android v1.4.6 GitHub 发布/下载流程，建议独立发行仓库的公开 preview、不主动推广；保留真正私有备选。列明现有客户端传输适配、持久签名/续签、发布顺序和验收。仅方案，未创建密钥或外部发布；用户已重新表示可配合人工测试，优先人工截图。PRODUCT_HOLD / NOT RC。

## 固定 0.2.12 内部包与长期使用条件（2026-10-22）

[实际记录](audits/UX_FIXED_TWELVE.md)：3bea16b 最新界面完整桌面 115 项实际 PASS；新固定内部包构建、546 文件安装核验及路径/覆盖拒绝通过，失败与中断尝试保留。本轮没有启动应用或开展人工测试。[长期使用版剩余条件](rc/FIRST_DAILY_USE_READINESS.md)明确集中体验验收及持久更新渠道仍未完成；PRODUCT_HOLD / NOT RC。

## 图库/编辑清晰度与设置沿用（2026-10-22）

[实际记录](audits/UX_EDITOR_CLARITY.md)：按用户反馈收起次要操作与长说明，复现并修复连续嵌字样式回退，提供快捷命名预设；自动回归与源 APNG 接线核验记录保留。[单张 PNG 文件粘贴](audits/UX_NATIVE_CLIPBOARD_ELEVEN.md)经用户及字节核对通过，不能代替多文件/APNG。用户要求本轮改好告知即可，暂不开展新人工测试；PRODUCT_HOLD / NOT RC。

## 角色展开样式与实机输入记录（2026-10-22）

[实际记录](audits/UX_ROLE_HEADER.md)：按用户反馈将角色折叠移至标题左侧，并澄清位置重置含义；两项相关回归实际 PASS。用户确认安装版 11 中文输入/切页保留和位置 X/Y/重置可用；剪贴板接收端尚未测，继续新固定包，PRODUCT_HOLD / NOT RC。

## 固定 0.2.11 安装验证（2026-10-22）

[实际记录](audits/UX_FIXED_ELEVEN.md)：a50f428 固定包实际安装、546 文件核验及路径/已有目录拒绝通过，独立合成窗口已启动并收到用户截图。各模块最近执行与当前源码逐一匹配；更新过期追踪描述，原生输入/剪贴板与集中验收仍待验证，PRODUCT_HOLD / NOT RC。

## 角色位置与账号额度修正（2026-10-22）

[实际记录](audits/UX_CREATION_CONTROLS.md)：复现并修正角色位置控件重叠、保存选用 NAI 账号后沿用旧余额；两次实际失败保留，定向 2 项及完整桌面 114 项实际 PASS。实际安装代码拒绝未来资料格式，原生错误弹窗仍未测；继续新固定包及原生门槛，PRODUCT_HOLD / NOT RC。

## 恢复工作区实际更新与角色流程（2026-10-22）

[固定 9→10 实际更新](audits/UX_RESTORED_UPDATE_ACTUAL.md)：用户确认重启，B 完成记录与 10 条资产/先前缺失凭据状态一致，546 个安装文件核验通过。[角色流程](audits/UX_ROLE_WORKFLOW.md)：实际工作台启停/重排至请求及重开定向通过，完整桌面 112 项实际 PASS。固定安装包未替换，继续其余门槛，PRODUCT_HOLD / NOT RC。

## 恢复工作区更新修复（2026-10-22）

[实际记录](audits/UX_RESTORED_UPDATE.md)：原工作区 7→8 实际更新及再次检查最新版本通过；恢复窗口缺失凭据导致的安装前失败已复现并定向修复。可读凭据丢失仍拒绝，继续完整回归和新固定包验证，PRODUCT_HOLD / NOT RC。

## 安装版备份恢复第二窗口（2026-10-22）

[实际记录](audits/UX_NATIVE_RESTORE.md)：用户完成 0.2.7 备份/恢复新目录/打开第二窗口；61 个文件三方散列、文档链及凭据排除只读验证通过。原窗口保留，继续固定 7→8 更新，PRODUCT_HOLD / NOT RC。

## 原生选择框安装版观察（2026-10-22）

[实际交付](audits/UX_NATIVE_INSTALLED.md)：固定源码 08733d5 的 0.2.7 安装/文件验证通过，用户截图确认真实 Windows 原生导入框；Drive 分卷与证据已上传并回读。取消返回已由用户确认；恢复第二窗口及 0.2.8 更新继续，PRODUCT_HOLD / NOT RC。

## Windows 原生选择与分步恢复（2026-10-22）

[实际记录](audits/UX_NATIVE_PICKERS.md)：固定 Desktop 原生选择器适配全部入口；恢复来源/位置分步及错误来源提前提示。定向 16 项、desktop 完整 108 项实际 PASS；首次编译失败和旧安装版 MISSING 保留。原生手选与固定包验证继续，PRODUCT_HOLD / NOT RC。

## 字体回退与导出反馈（2026-10-22）

[实际记录](audits/UX_GLYPH_EXPORT_FEEDBACK.md)：修复小人书测量/绘制字体不一致、导出进度重绘阻塞及冷字体载入；停止/明确重试/复制失败文件保留已验证。core 129 项与最终 desktop 95 项分别实际 PASS，三轮失败保留。继续固定包与原生验证，PRODUCT_HOLD / NOT RC。

## Recipe 往返与小人书批次验证（2026-10-22）

[实际记录](audits/UX_FLOW_COMPLETION.md)：修复合法大 Recipe 导出后无法导入、勾选队列页切走当前样式来源；批次撤销/重开及四模块完整 351 项实际 PASS。18 项动作映射补齐，原生验证与产品验收仍单列，PRODUCT_HOLD / NOT RC。

## 启动与真实安装故障验证（2026-10-22）

[实际记录](audits/UX_STARTUP_SAFETY.md)：未知格式无新增文件、迁移五边界进程中断、实际排水超时与 B 启动失败保全；长路径故障已保留并补写入前门禁。218 项核心/桌面回归及最新 92 项桌面回归实际 PASS。继续新安装包与剩余门槛，PRODUCT_HOLD / NOT RC。

## 连续生成及更新进程验证（2026-10-22）

[实际记录](audits/UX_CONTINUOUS_PROCESS.md)：连续批次/停止/部分结果/重开不重发定向 PASS，桌面完整 91 项 PASS；真实 CLI/HTTP 更新故障 8 项 PASS，历史失败保留。继续安装/迁移故障与固定包验证，PRODUCT_HOLD / NOT RC。

## 新工作台备份恢复进度（2026-10-22）

[实际记录](audits/UX_LIBRARY_BACKUP.md)：接入已有安全核心，恢复到新目录、缺凭据/目录重连提示及直接打开入口；3 项定向、90 项桌面完整实际 PASS，失败保留。安装版第二窗口与集中真人验收未签；继续完整工程门槛，PRODUCT_HOLD / NOT RC。

## 实际更新与界面反馈修正（2026-10-22）

[实际记录](audits/UX_UPDATE_FEEDBACK.md)：用户操作真实 A→B，格式 2→3 独立复制与 10 条资产/1 个合成凭据核验 COMPLETE；同版本误报失败、下载进度/完成反馈及 Desktop 顶栏图形已修正。Python 23 项、定向 7 项、桌面完整 87 项实际 PASS，历史失败保留。继续完整门槛，PRODUCT_HOLD / NOT RC。

## 格式 3 复制迁移进度（2026-10-22）

[实际记录](audits/UX_FORMAT_THREE_MIGRATION.md)：独立副本迁移、启动确认/签名目标绑定及真实旧 A 读取拒绝已验证；完整 208 项实际PASS，旧备份锁初始化失败及修正保留。A安装包已封存，继续B打包和真实2→3应用内更新，PRODUCT_HOLD / NOT RC。

## 更新数据格式交接进度（2026-10-22）

[实际记录](audits/UX_UPDATE_DATA_BRIDGE.md)：签名目标输出格式、独立迁移目录与确认绑定已接线；6项定向、桌面完整81项及Python20项实际PASS，权限/编码失败保留。当前仍为数据格式2，继续固定A包与格式3复制迁移/真实更新，PRODUCT_HOLD / NOT RC。

## 大资产恢复修复进度（2026-10-22）

[实际记录](audits/UX_LARGE_ASSET_RECOVERY.md)：33 MiB合成归档实际复现恢复失败并修复；流式校验保留原件/损坏拒绝，12项定向、完整核心及维护120项实际PASS。继续数据能力与实际更新门槛，PRODUCT_HOLD / NOT RC。

## 可恢复资产移除进度（2026-10-22）

[实际记录](audits/UX_ASSET_ARCHIVE.md)：图片/Prompt/整册回收站、恢复与统一标签选用已接线；5项核心、2项桌面定向和完整196项实际PASS，首次测试编译失败保留。继续大资产恢复及数据读取/更新门槛，PRODUCT_HOLD / NOT RC。

## TypeSet 颜色兼容进度（2026-10-22）

[实际记录](audits/UX_TYPESET_COLORS.md)：162组独立源颜色样本、富文本/透明色及非法值恢复已验证；7项定向、完整189项实际PASS，历史失败保留。继续资产维护与数据读取/更新门槛，PRODUCT_HOLD / NOT RC。

## 设计应用与图片来源进度（2026-10-22）

[实际记录](audits/UX_DESIGN_PROVENANCE.md)：明确候选应用→最终请求、撤销/重做来源及编辑图回查源图已接线；10项定向、完整186项实际PASS，首次复制入口测试失败保留。继续剩余TypeSet/资产与更新门槛，PRODUCT_HOLD / NOT RC。

## TypeSet 队列操作进度（2026-10-22）

[实际记录](audits/UX_TYPESET_QUEUE.md)：按序号/右键移动、长队列边缘滚动、编辑后缩略预览与侧栏拖动已接入；最终完整180项实际PASS，全部失败保留。继续设计请求关联和剩余工程门槛，PRODUCT_HOLD / NOT RC。

## Desktop 相册与参照结构进度（2026-10-22）

[实际记录](audits/UX_HISTORY_REFERENCE.md)：固定Desktop源码的分组/筛选/选择策略已接入统一资产；Prompt与参数切换、角色对齐和摘取保留。13项定向、完整175项实际PASS；继续TypeSet和更新门槛，PRODUCT_HOLD / NOT RC。

## 自动资产收集进度（2026-10-22）

[实际记录](audits/UX_COLLECTIONS.md)：专用导入路径、多目录自动收集、恢复后重连门禁及维护排水已接线。最终完整166项实际PASS，初始化错误与历史超时证据保留；继续 Desktop 相册源码继承、TypeSet与更新门槛，PRODUCT_HOLD / NOT RC。

## 资产目录与多 Prompt 参照进度（2026-10-22）

[实际记录](audits/UX_LIBRARY_PROMPTS.md)：独立Prompt/标签别名/目录手动同步、图片Prompt预览及多来源角色拖动对齐已接线。核心91实际PASS、最终桌面68实际PASS，首轮完整回归1失败和修正保留；自动收集及完整恢复继续，PRODUCT_HOLD / NOT RC。

## 统一资产回用进度（2026-10-22）

[实际记录](audits/UX_SHARED_ASSETS.md)：编辑图片/PDF/ZIP资产与来源快照接线，跨引导/编辑选用保存重开；完整152项实际PASS。继续目录入口/Prompt资产/别名标签及原UX恢复，PRODUCT_HOLD。

## 创作页源码继承修正（2026-10-21）

[实际记录](audits/UX_CREATION_LAYOUT.md)：直接继承源图标/固定栏/缩略条并恢复连续编辑及窄窗摘要；desktop 完整64项实际PASS，首轮编译失败保留。产品恢复继续，PRODUCT_HOLD/NOT RC。

## UX-R4 实际A→B进度（2026-10-21）

[实际回执](audits/UX_R4_ACTUAL_AB.md)：磁盘阻塞已解除；真实A窗口下载/确认→NSIS安装→B启动/数据与测试凭据指纹核对COMPLETE。更新后继续编辑重开1项PASS，Drive证据已上传。故障覆盖与剩余UX继续，PRODUCT_HOLD/NOT RC。

## UX-R4 实际更新空间阻塞（2026-10-21）

[实际回执](audits/UX_R4_AB_SPACE_BLOCK.md)：A应用可信检查成功，下载因H盘剩余约147MiB被安全拦截。B未安装/重启，RC BLOCKED；已保留失败和精确清理候选，等待空间解除。

## UX-R4 维护排水与交接进度（2026-10-21）

[执行记录](audits/UX_R4_HANDOFF.md)：desktop完整62项、随后7项定向、Python更新组件18项实际PASS。实际A→B尚未执行；继续固定包演练，PRODUCT_HOLD。

## UX-R4 实际安装器进度（2026-10-21）

[实际记录](audits/UX_R4_INITIAL_INSTALLER.md)：真实NSIS安装544个文件核验、拒绝覆盖；新Python8项PASS。原生窗口存在但截图超时，视觉未验证；尚未串联A→B，继续实施，PRODUCT_HOLD。

## UX-R4 可信客户端进度（2026-10-21）

[实际记录](audits/UX_R4_TRUSTED_CLIENT.md)：隔离Python/TUF和检查/确认下载界面接线；desktop完整60项、最终主题3项、TUF8项/客户端7项实际PASS，失败保留。安装/重启/数据保全未完成，继续R4及剩余UX，PRODUCT_HOLD。

## UX-R2 长页PDF兼容（2026-10-21）

[实际记录](audits/UX_R2_LONG_PDF.md)：完整142项实际PASS，长页经Poppler/PDFium独立尺寸及像素核验；保留UserUnit方案兼容失败和验证脚本错误。完整恢复与R4继续，PRODUCT_HOLD。

## UX-R3 APNG进度（2026-10-21）

[实际记录](audits/UX_R3_APNG.md)：桌面57项完整PASS，最后确认状态修正后5项定向PASS；伪装/还原、批次文件交付、GIF时序、原件/副本重开和静态确认已测。失败记录保留；继续完整恢复和R4，PRODUCT_HOLD。

## UX-R3 导入进度（2026-10-21）

[实际记录](audits/UX_R3_INGRESS.md)：54项桌面完整实际PASS，核心未改未重跑；导入逐项回执、文件原字节/位图来源、取消文字粘贴拦截和重开已测。系统原生拖放/剪贴板未签；继续APNG和剩余恢复，PRODUCT_HOLD。

## UX-R3 选择应用进度（2026-10-21）

[实际记录](audits/UX_R3_RECIPE_SELECTION.md)：完整136项实际PASS；确认字段应用、取消、草稿冲突保护、撤销/重做和重开已测，初始编译失败保留。继续导入/图库及完整R4，PRODUCT_HOLD。

## UX-R3 PNG字段进度（2026-10-21）

[实际记录](audits/UX_R3_PNG_FIELDS.md)：完整132项实际PASS；最终页面可读性修正后4项定向PASS。PNG未知字段单图查看/两图比较、复制及勾选保留已验证，历史失败保留；继续完整R3/R4，PRODUCT_HOLD。

## UX-R2 预览可靠性进度（2026-10-21）

[实际记录](audits/UX_R2_PREVIEW_RELIABILITY.md)：完整工作台路径连续两次52项实际PASS；字体冷缓存和最大队列空间已测，独立挂载弹窗的历史超时及根因UNKNOWN保留。继续R3与安全更新，PRODUCT_HOLD。

## UX-R3 图库与比较进度（2026-10-21）

[实际记录](audits/UX_R3_GALLERY.md)：完整126项实际PASS；随后边界/GUI定向4项PASS，编译失败保留。图库筛选、比较/缩放/复制及确认回用保存重开已测；完整R3和安全更新继续，PRODUCT_HOLD。

## UX-R2 完整预览进度（2026-10-21）

[实际记录](audits/UX_R2_FULL_PREVIEW.md)：desktop49项完整实际PASS；完整预览键盘/状态隔离、宽窄输出布局及复制回归通过，初始失败与问题截图保留。core未改，继续恢复，PRODUCT_HOLD。

## UX-R2 预设消费进度（2026-10-21）

[实际记录](audits/UX_R2_PREFERENCE_CONSUMERS.md)：122项完整实际PASS；导出/队列/故事/连续打码默认已接入真实消费者，保存重开与备份恢复通过。继续完整预览、图库和安全更新，PRODUCT_HOLD。

## UX-R2 格式兼容进度（2026-10-21）

[实际记录](audits/UX_R2_IMAGE_FORMATS.md)：120项完整实际PASS，保留首轮GUI测试FAILED。WebP/GIF/BMP原件、首帧/默认图编辑、WebP/原格式导出与重开已验证；完整偏好/图库/更新继续，PRODUCT_HOLD。

## UX-R2 图片坐标兼容进度（2026-10-21）

[实际记录](audits/UX_R2_IMAGE_COORDINATES.md)：115项完整实际PASS。旧工程保留raw坐标，新工程记录EXIF方向，原件不变；格式/图库/应用内安全更新继续，PRODUCT_HOLD。

## UX-R2 四分类导出与Recipe恢复进度（2026-10-21）

[执行记录](audits/UX_R2_EXPORT_METADATA.md)：112项完整实际PASS；四分类完整序列、PNG元数据/隐写保留、隐私清除、隔离剪贴板及Recipe确认/撤销已测。保留编译失败，继续格式/图库/更新，PRODUCT_HOLD。

## UX-R2 工程与预设交换进度（2026-10-21）

[执行记录](audits/UX_R2_SOURCE_EXCHANGE.md)：107项完整实际PASS；源程序导出→Studio编辑→源程序回读、独立导入/取消、预设合并/替换、原件及备份恢复已验证。继续完整导出/图库/更新，PRODUCT_HOLD。

## UX-R2c 小人书与文件导出进度（2026-10-21）

[执行记录](audits/UX_R2C_BOOK_EXPORT.md)：完整100项实际PASS，随后JPEG质量定向3项PASS；整册PNG/ZIP/PDF、目录选择接线、故障保全和重开已测。继续源交换/完整导出/图库/更新，PRODUCT_HOLD。

## UX-R2预设与默认进度（2026-10-21）

[执行记录](audits/UX_R2A_PRESETS.md)：跨工程文字预设、四槽打码/启动默认、源新建文字限制已接入；完整core53+desktop43共96实际PASS。保留全部历史失败，PRODUCT_HOLD；继续源文件交换、完整导出、图库与安全更新。

## UX-R2对象与队列进度（2026-10-21）

[执行与界面证据](audits/UX_R2A_OBJECTS_QUEUE.md)：core50+desktop43共93项实际PASS；对象锁定/层序、工程另存/删除、Ctrl/Shift多选拖序及重开已测。七次运行含失败保留；PRODUCT_HOLD，继续预设/交换/完整导出与R3–R4。

## UX-R2批次与打码进度（2026-10-21）

[来源比较与执行记录](audits/UX_R2B_MASKS.md)：12组源图对照、真实Compose指针/保存/重开已测；修复防抖保存取消误报。最新desktop完整42 PASS，core46实际PASS来自前次运行且源码未变。失败保留，PRODUCT_HOLD，继续完整R2–R4。

## UX-R2a 原生排版工程进度（2026-10-21）

[实际执行证据](audits/UX_R2A_EDITOR.md)：core+desktop完整80项PASS，后续字体格式定向4项PASS；10次构建/测试尝试及失败保留。来源样本对照、工程保存/重开、字体与备份恢复已测；完整编辑/批量/整册导出继续实施，原生视觉和真人验收NOT TESTED，PRODUCT_HOLD。

[TypeSet来源样本冻结](audits/TYPESET_SOURCE_REFERENCE.md)：16项输出及独立PDF/ZIP核验，原件启动失败与测试副本差异明示；继续R2原生实现，非产品PASS。

[UX-R1b创作/回用检查点](audits/UX_R1B_CREATION.md)：39 desktop实际PASS；保留17次构建/测试运行。继续完整UX恢复，非RC。

# 当前执行：UX恢复连续实施

[正式授权合同](../tasks/UX_RECOVERY_CONTINUOUS.md) · [源样本/Q09补正](ux/TYPESET_REFERENCE_VALIDATION.md) · [75项实现证据台账](ux/ACTION_IMPLEMENTATION.json)。UX-R0审查通过不等于产品通过，当前PRODUCT HOLD。

[UX-R1a工作台工程检查点](audits/UX_R1A_WORKBENCH.md)：真实应用接线/本地HTTP流程，23项desktop新测试PASS；原生截图超时单独记NOT TESTED，继续R1b。

[UX-R1b引导/Q09中间检查点](audits/UX_R1B_GUIDANCE.md)：155项新执行PASS，R1b仍在实施；未达到产品验收。

# 当前审阅入口：UX-R0 产品恢复合同

**PRODUCT HOLD / NOT ACCEPTED。** Issue #4最新完整继承要求下，已交付[整体Project审阅入口](ux/UX_R0_PROJECT_REVIEW.md)、[来源差距矩阵](ux/UX_R0_PARITY_MATRIX.md)、[逐动作继承合同](ux/UX_R0_ACTION_TRACE.md)、[GUI工作流/宽窄结构](ux/UX_R0_WORKFLOW_AND_LAYOUT.md)、[完整实施方案](ux/UX_RECOVERY_IMPLEMENTATION_PLAN.md)。[当前任务](../tasks/UX_R0_PRODUCT_RECOVERY_AUDIT.md)与[最新Issue范围](../tasks/UX_R0_ISSUE4_FULL_ADOPTION_ADDENDUM.md)。本轮仅文档，Project批准后再实现；下面均保留历史。

# 00 — 文档入口

**当前用户验收结论：USER_REVIEW_FAILED。见[首轮反馈与源码定位](rc/PREVIEW_USER_REVIEW_1.md)。下面Preview交付入口保留历史，不代表用户通过。**

**最新入口：[Windows Preview Gate完整交付](rc/PREVIEW_PROJECT_REVIEW.md) · [集中真人验收](rc/PREVIEW_REVIEW_ITEMS.md) · [GitHub/Drive回执](audits/evidence/WINDOWS_PREVIEW/DELIVERY.json)。状态PREVIEW_GATE_WITH_GAPS；实现209b5252已推送，待集中review，历史RC BLOCKED不可替代当前状态。**

## 每次任务首读

1. [当前状态](CURRENT_STATE.md)
2. [任务登记](TASK_REGISTRY.md)
3. [来源锁](SOURCE_LOCK.json)
4. 当前 [Windows RC连续合同](../tasks/WINDOWS_RC_CONTINUOUS.md)与[checkpoint恢复台账](rc/CHECKPOINTS.md)；A1/R1原合同和证据保留历史，不从main重做

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

## A1-001-R1（2026-10-21，针对性安全补强）

原A1@3c17b4c9审查PASS_WITH_SCOPE由用户提供；本轮只修复目录完整性和备份/关闭排水竞争。见[R1 Project审阅入口](audits/A1_001_R1_PROJECT_REVIEW.md)、[真实实施/失败沿革/恢复语义](audits/A1_001_R1_EXECUTION.md)、[本轮27项实际结果](audits/evidence/A1_001_R1/FINAL_TEST_CASES.json)、[源码身份](audits/evidence/A1_001_R1/SOURCE_IDENTITY.json)。GitHub/Drive交付以实际回执为准；不改旧证据或产品验收矩阵，不自动继续更新器/其他阶段。

R1实际交付：[GitHub/Drive回执](audits/evidence/A1_001_R1/DELIVERY.json)及[固定实现和附件入口](audits/A1_001_R1_PROJECT_REVIEW.md)。实现a9ac4712已远程核验；Drive ZIP原字节回读hash一致，后续提交仅补交付记录。

## Windows RC 连续任务（2026-10-21）

已实际读取[#1正式Gate](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/1#issuecomment-6296885017)、[#2完整合同与R1审查](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2)。从R1@27dbd5d5独立分支继续；[RC状态](rc/RC_READINESS.md)、[CP台账](rc/CHECKPOINTS.md)、[更新A→B计划/真实TUF合成lab边界](rc/UPDATE_A_B_PLAN.md)、[权威依赖STOP](rc/DEPENDENCY_ESCALATION.md)、[合同原始快照](audits/evidence/WINDOWS_RC/CONTRACT_SOURCE.json)。不改历史证据，不把CP1库/信任lab称为RC。

CP1 [实际执行/环境/测试与STOP回执](rc/CP1_EXECUTION.md)：31项完整JVM回归和8项真实TUF合成信任验证已执行；整体RC BLOCKED，未产出安装包。

Windows连续任务实际远程交付：[RC BLOCKED/CP1固定审阅入口](rc/PROJECT_REVIEW.md)、[机器恢复台账](rc/CHECKPOINTS.json)、[GitHub/Drive实际回执](audits/evidence/WINDOWS_RC/DELIVERY.json)。

## Windows Preview resume / Issue #3

Latest Project correction rescinds generalized source-rights / missing-SDK STOP. Same owner resumed from 86760bc0; historical RC BLOCKED evidence remains. Current work is IN PROGRESS toward Preview Gate, with actual 69 authority + 8 Studio core synthetic tests PASS. See [Preview progress](rc/PREVIEW_PROGRESS.md) and [Issue #3](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/3). Real API use is additionally authorized at <=12 attempts/rolling hour and >=300s interval; no real calls at this checkpoint. Not RC/Preview acceptance.

当前执行入口：[Preview任务](../tasks/WINDOWS_PREVIEW_GATE.md)、[P2进度](rc/PREVIEW_P2_EXECUTION.md)、[集中待验项](rc/PREVIEW_REVIEW_ITEMS.md)。历史 CP1 STOP 已纠偏；请以 CURRENT STATE 页首和后续 Preview Gate 回执为准。
