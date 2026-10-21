# UX-R0 — Source → Action → Feedback → Persistence 继承合同

Project补正：Q09唯一负责人UX-R1b，R2b消费/集成回归；验证Q09-01..04及独立TypeSet源样本见[验证合同](TYPESET_REFERENCE_VALIDATION.md)。功能状态仍保留原审查事实；最终实现/执行证据逐ID回填ACTION_IMPLEMENTATION.json。

此表是 [差距矩阵](UX_R0_PARITY_MATRIX.md) 的逐项实施/验收展开，按Issue #4最新五值状态执行。D/S/T代号、固定SHA/文件/行号及Suite现状证据见矩阵；T始终指SHA `892ea08f…e28bb63` 的本地TypeSet 1.1，原件不上传。**反馈与持久化栏是要实现的结果，不是当前已完成；状态栏才是当前事实。** P/I/T/C编号对应原56项，Q/F是为完整继承新增的细分检查项。

## Suite目标路径约定（全部为拟议位置，当前未创建）

下表中的缩写是批准后真实代码承接路径，而不是新增第二套核心：

- `UI/` = `modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/`。
- `CORE/` = `modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/`。
- A = `UI/accounts/AccountSettings.kt` + `CORE/accounts/AccountResolver.kt` →现有SecretVault/LiveRequestBudget。
- N = `UI/creation/NovelAiStudioScreen.kt` + `CORE/creation/CreationDraftStore.kt` →固定image-authority；优先适配D1/D4/D5/D10原Compose组件，替换它们对Desktop settings/resources/tasks的窄接口，不重写请求权威。
- D = `UI/designer/DesignConversationScreen.kt` + `CORE/designer/DesignSessionStore.kt` →D3状态流程与权威Designer。
- G = `UI/gallery/GalleryScreen.kt` + `CORE/gallery/AssetQuery.kt` →现有AssetStore/JobRecord。
- M = `UI/gallery/MetadataApplyScreen.kt`、`UI/compare/RecipeCompareScreen.kt` + `CORE/metadata/MetadataApply.kt` →权威reader/field model。
- E = `UI/editor/EditorScreen.kt`、`ObjectProperties.kt` + `CORE/editor/EditCommands.kt`、`TextLayout.kt` →现有工程引用/事务；原生替代TypeSet Canvas/IndexedDB，不用WebView。
- B = `UI/editor/StorybookScreen.kt` + `CORE/editor/StorybookLayout.kt` →同一工程/队列与渲染布局结果。
- X = `UI/export/ExportWorkspace.kt` + `CORE/export/ExportPlan.kt`、`ExportWriter.kt` →现有原字节/隐私编码与派生关联。
- L = `UI/settings/DataAndUpdateScreen.kt` + `CORE/lifecycle/WorkspaceLifecycle.kt` →原门禁/恢复与CP4安装器接口。
- W = `UI/StudioShell.kt` →原native chrome/StudioBrand。

文件名允许实现时合理调整，但每个Action ID必须回填最终文件/符号与执行证据；不得因改名/拆模块而丢功能。下列“重开”均指相同已保存工程/草稿在分发包中退出再启动，不只是重新绘制Composable。

## NAI Studio：真实成熟流程为核心，Suite多档案为扩展

|ID / 固定来源符号|用户动作|预期可见反馈|保存/重开结果|Suite目标|当前状态/证据|
|---|---|---|---|---|---|
|P01 D2 designAuthentication/refreshAccount|录入LLM与NAI、选择实际模型；Suite另加命名多档案|实际provider/模型/别名、认证失败定位|仅安全引用持久化，重开不回退合成或另一账号|A/N|SOURCE_OR_SYNTHETIC_ONLY；S4 validate/S2 AccountDialog|
|P02 D1 cost/D2 refreshAccount|看额度、刷新、修改生成设置|成本随有效参数变化、未知/过期标识|保存账号选择与请求时身份，余额刷新时间不伪造|A/N|MISSING；S2无额度|
|P03 D3 DesktopDesignConversation|发送/附加当前Prompt/继续修改/停止|完整对话与流式阶段、候选分段、停止状态|会话和各轮需求/回复/输入草稿可重开|D|SOURCE_OR_SYNTHETIC_ONLY；S3合成transport|
|P04 D3编辑并分支/D2 retryDesign/applyDesign|编辑旧轮、重试、切历史、选择回复应用|新分支标识、应用差异、历史不覆盖|会话分支/候选应用关联保存；未应用候选仍在|D/N|PARTIAL；S4平面记录|
|P05 D1 section(Prompt)|编辑画风/基础/补充/负向|分段编辑与实际合成摘要|草稿每段独立保存，历史Recipe不改|N|PARTIAL；S1两个字符串|
|P06 D4 StudioPromptField|检索Tag、接受候选、开关中文注释、全文确认/取消|候选来源与caret一致，注释不篡改原词|全文取消保留旧稿，已确认文本重开一致，词库状态可见|N|SOURCE_OR_SYNTHETIC_ONLY；仅底层来源|
|P07 D5 DesktopStudioCharacters|增删/重排/折叠/启停、正负向编辑|角色卡和有效数量、失效原因|ID/顺序/启用及正负词保存，非启用角色不误发|N|PARTIAL；S1 JSON角色|
|P08 D5 DesktopCharacterPositions|点网格/拖坐标/输入X Y/均匀重置|V4.5吸附、V5连续；聚焦区域坐标说明|位置与请求裁剪关系进入草稿/最终Recipe|N|MISSING；无位置面板|
|P09 D10 StudioGenerationSettings/Advanced|切模型/跟随策略、档位/比例/自定义尺寸、数量、Steps/CFG/Rescale/Sampler/Seed|当前有效值/校验；不可用选项说明|分模型参数和随机/固定Seed策略重开；实际种子记结果|N|PARTIAL；S2字段不全|
|P10 D10 continuousModeEnabled|设连续目标、生成、停止|目标/已完成/费用/等待/停止清楚|已提交任务及原件保留；重启不续发不确定任务|N/G|MISSING；S3 count不是完整UI连续流程|
|P11 D1图像引导/D7 prepare|导入基图、图生图、聚焦框/蒙版、minimum context、strength/noise|裁剪/蒙版覆盖与模型能力可视反馈|基图/蒙版原件引用、有效坐标和设置持久化|N/E|SOURCE_OR_SYNTHETIC_ONLY；S3 guidance空|
|P12 D7 vibeCacheMisses/prepare|选参考类型、强度/fidelity、添加移除Vibe、提取/归一化|参考缩略、参数、缓存/成本、校验|参考资产与缓存身份可回溯；历史缺资源警告|N|SOURCE_OR_SYNTHETIC_ONLY；无UI|
|P13 D2 generate/stop|真实生成/停止/错误查看|中间图未保存、最后结果、部分成功、不确定错误|发送前请求快照、已收原件、任务结果重开|N/G|SOURCE_OR_SYNTHETIC_ONLY；S3从不chain.proceed|
|P14 D1 preview/filmstrip|选新结果、开查看器、用作/另存|每个成功结果立即可见，旧选图不被中间图替换|图库资产与结果关联保存；工程集合不掩盖它|G/N|PARTIAL；U新增图不可见|
|P15 D6 DesktopStudioHistory|日期/Prompt搜索、相册折叠/展开、范围选择/批量操作|网格与数量/选择锚点，筛选结果解释|历史查询偏好/折叠状态、数据原件各自持久化|G|PARTIAL；S2仅名称列表|
|P16 D2 applyHistory/D6配方详情|复用全参数/新种子/仅Seed/用作引导|明确字段范围，缺原引导先提示|新草稿指向历史来源，历史不覆盖|G/N|MISSING|
|P17 D12 DesktopImagePostProcessPanel|Enhance/Upscale、强度/Noise/倍率、执行/停止/复制保存/回用|成本/限制、前后结果对比、进度|输出为派生原件关联、处理参数与身份保存|N/X/G|MISSING|
|P18 D2 reverseImage/applyReverseCandidate|对导入图反推、查看候选、应用|流式进度/候选角色与基础词、确认差异|输入来源与候选/应用记录保存|D/M|MISSING|
|P19 D1导入角色卡Prompt/D2 importCard|导入卡片图像相关字段|画风/基础负面填入；参考角色词用途说明|保存所选卡片来源/图像字段，不引入聊天状态|N/M|MISSING|
|P20 D1复制正向/粘贴覆盖/D4全文|复制完整文本、粘贴预览并确认、撤销|完整内容可检查，未确认不改稿|应用版本可撤销/重开；交换来源标明|N|PARTIAL|

## 导入、元数据与跨工作台桥梁

|ID / 来源|用户动作|反馈|保存/重开结果|目标|当前状态/证据|
|---|---|---|---|---|---|
|I01 D1 pickImage/desktopImageIngress|文件/多选/拖入/剪贴板图片|逐项状态，成功图聚焦；格式限制明确|原文件bytes/size/hash保存；剪贴板raster标明不是原始文件|G|PARTIAL；样例可用，完整导入未签|
|I02 D2 importMetadata|勾选字段、角色关/覆盖/新增、应用/取消|现值→候选值，未知字段提示|草稿可撤销；角色ID/启停/位置按权威合并规则|M/N|PARTIAL；U弹窗可开|
|I03 D11 readStudio/alpha|读取PNG文本/alpha隐藏参数|区分缺失/损坏/无法读，原图仍可用|未知原字段留存；不改原件|M|SOURCE_OR_SYNTHETIC_ONLY|
|I04 Suite REQ07 / S3 compareRecipes|选两图、看完整差异、取回字段|A/B并排/独立或联动缩放、分组全部/差异|源Recipe不变；新草稿保存应用来源|M/G|PARTIAL；当前字符串popup|
|I05 D9 strip / S5 encode|选隐私PNG、预览、复制/保存|明确清元数据/LSB、效果与来源区别|输出与原件分离，派生身份/结果清单|X|PARTIAL；U导出失败|
|I06 D9 disguise/restore|APNG伪装/还原/查看结果|动图/原始结果状态、支持的文件复制|保留源动画字节；派生可验证；无静态冒充|X|MISSING|
|I07 D9 copyDesktopImageResult/另存|复制图片/文件/批量文件、选路径、显示副本|成功接收端/失败可另存，目标可见|保存清单；剪贴板缓存生命周期独立管理|X|MISSING|
|I08 Suite bytes / T4179差异|导出原字节或无编辑重编码|两种输出明确标识|原字节hash一致，重编码有新身份|X|SOURCE_OR_SYNTHETIC_ONLY；E核心测试|
|I09 D6选择/删除，Suite多工作区|单选/Ctrl/Shift/全选/全不选/反选/移除|聚焦与目标数、隐藏所选、删除影响提示|移除工程成员不删原件；显式图库删除需引用检查/恢复策略|G/E|PARTIAL；U基本动作|

## TypeSet 普通图片编辑：全量能力簇

|ID / T行号与符号|动作|反馈|保存/重开结果|目标|当前状态/证据|
|---|---|---|---|---|---|
|T01 T1033 serializeCurrentWorkspace/1203 openWorkspaceById/1256/1266/1281|多队列新建命名、切换、另存、删除、导入导出|当前工程/保存状态；另存克隆；删除确认|原生工程与原件引用/队列/编辑/metadata可重开，另存独立历史|E/L|PARTIAL；S6 newProject只有基本模型|
|T02 T1783–1833选择/拖序/2213–2250排序|Ctrl/Shift选、多选拖、上下/顶底/指定序号前后|选择数/插入线/边界不能移动反馈|顺序+可撤销批次；重开一致|E|PARTIAL；U只上下移|
|T03 T1467 applyQueueThumbSize/4629 showHoverPreview|调格子大小/侧栏宽度、悬停、点当前图|网格布局/悬浮预览，不抢编辑焦点|尺寸/侧栏偏好重开；不复制数据核心|E|PARTIAL|
|T04 T3306 textSelectionGeometry/3838 pointFromEvent/3901 pointerdown|选对象、拖动/改尺寸、画布定位|手柄/边框/光标与源像素坐标|对象几何入工程；视图缩放独立，不改变几何|E|PARTIAL；Suite精确缩放平移是整合增强，非谎称T有独立滚轮引擎|
|T05 T2687 syncTextControls/2730 updateSelectedText/2738 addText|新建/选择/删除、输入或切横竖排|选中即属性目标，改动立即可见|文字/方向/样式和undo保存；新建只继承样式|E|PARTIAL；U竖排FAILED|
|T06 T2932 wrapStyledLines/3281 textMetrics|改横排宽度、行距字距、对齐|自动换行、基线/溢出，选框随排版|框尺寸与显式换行保留；预览/导出同布局|E|MISSING|
|T07 T3108 verticalTokenizeStyled/3250 verticalColumnsStyled|竖排高度、标点优化/紧凑度，输入中日文/拉丁数字|上下书写右向左列，替代字形/旋转/全角直立正确|仅显示转换，原文本不变；布局参数重开|E|PARTIAL；S5 codepoint demo|
|T08 T2612 parseInlineTagToStyle/2636 parseRichTextTokens|同框选区混合字体/颜色/粗斜/下划线/删除线/字号/描边|选区样式与渲染匹配；嵌套与转义可读|样式runs/原文本保存；兼容源轻标签导入语义|E|MISSING|
|T09 T2687控件/3435 drawTextBoxDecoration|字重、字体/描边色宽、背景/边框色宽透明度、文字透明度|实时属性效果，横竖排一致|完整样式参数可恢复；不是仅fontSize|E|MISSING|
|T10 T2900 fontUpload / FontFace|选择内置/系统字体，导入TTF/OTF/WOFF/WOFF2|样张、缺字/字体不可用、授权依赖说明|本地资源引用与hash可恢复；文件不自动公开再分发|E|SOURCE_OR_SYNTHETIC_ONLY；S5仅缺字体告警|
|T11 T3610 drawBrushMask/3669 drawLassoMask/3698 drawMask|矩形、画笔、圈选拖绘|路径/区域实时可见、松手可撤销|独立非破坏遮罩对象保存|E|USABLE_REAL；仅U基础合成拖绘|
|T12 T1305 getCurrentMaskToolSettings/3698|选马赛克/模糊/黑白条，强度、笔宽、条密度/宽、点密度|参数立即更新当前对象或明确新建默认|每对象参数与工具默认分开保存|E|PARTIAL；S1 brush固定18|
|T13 T3724 drawMaskSelection/3502 hitText + Suite层需求|选对象/显隐/重排/锁定/移除|图层与画布同步，锁定无法误拖|顺序/显隐/锁定保存；删除可撤销|E|PARTIAL；U显隐/移除通过；完整层列表属Suite增强|
|T14 T4514 batchApplyCurrent/4538 batchCopyPrevious|套当前样式/各自前一页样式|目标预览、内容/位置保留、首图跳过数|一次批次可撤销；使用操作前源快照|E|MISSING；S6只是复制层|
|T15 T1649 pushWorkspaceHistory/1700 undo/4087 keydown|撤销/重做、删图/清空/排序/批次撤销；文本输入优先|命令名称、历史边界、恢复效果|保证工程可再编辑，有限历史预算明确；不声称无限历史|E|USABLE_REAL；仅U基础图层undo/redo|
|T16 T1405/1415/1431/1444、2525/2545/2564|文本预设增改用删；打码四槽覆盖/恢复/启动优先|所选预设/自定义状态、覆盖确认|预设可独立备份导入；启动优先与当前默认解释清楚|E|MISSING|
|T17 T4155 getEntryExportVariants/4227 buildBatchExportJobs|选四分类、当前/完整序列|每类包含全目标序列，无对象页也输出|类目录/编号/每文件hash/完成清单|X|MISSING|
|T18 T2014 extractPngSourceChunks/2045 extractNaiStealth/4179|开关保留NAI源数据|PNG文本/隐写状态；JPEG丢失/WebP不保证提示|元数据与编辑Recipe分开；不伪造原始签名仍有效|X/M|PARTIAL；S5仅SuiteRecipe|
|T19 T4129 getExportSpecForEntry/4254/4282|PNG/原格式/JPEG/WebP，选目录、当前/全册ZIP|扩展名与实际编码一致，路径/进度/失败反馈|完整分类序列可回验；ZIP安全路径/无Secrets|X|PARTIAL；S6内部目录|
|T20 T3768/4372/4425|图片编辑/小人书切换、整册PDF|见F01–F10全量分解|同一工程可重开两种模式|B/X|MISSING；无延期授权|

## 队列、资源及连续工作细项（补足容易被能力簇遮蔽的要求）

|ID / 对应簇 / 来源|动作|反馈|保存/重开结果|目标|当前状态|
|---|---|---|---|---|---|
|Q01/T02 T1729 currentStartNumber/1734 currentDigits/4245 buildVariantFileName|设置开始编号/编号位数|队列编号与四类文件名对应，非法值可修正|编号配置保存；各分类同一图编号相同|E/X|MISSING|
|Q02/T02 T4574 batchDeleteSelectedEntries/clearAllEntries:2280附近|批量删除/清空当前队列|确认范围、可撤销，图库原件不误删|工程删除命令保存；撤销恢复对象/次序|E|MISSING|
|Q03/T03 T4099 renderFullPreview/4113 openFullPreview|完整预览、前/后、左右键、Esc|最终效果无编辑框、页数/边界禁用|当前页/模式不丢；预览不是导出副作用|E/B|PARTIAL|
|Q04/T01 T1067 exportCurrentWorkspaceFile/1093 importWorkspaceFile|导入源1.0/1.1队列、导出可再编辑工程|版本/资源/冲突报告；不直接执行输入|有界解析到新工程，图片、文字、遮罩、故事页完整；原文件留存|E/L|MISSING|
|Q05/T16 T1115 exportPresetsFile/1127 importPresetsFile|独立导入导出预设|列出样式/工具/分类/侧栏/缩略偏好变化|合并或替换预览后提交；不影响原件/密钥|E|MISSING|
|Q06/T12 T1528 applyCurrentSettingsToLaterEntries|设为后续图片默认参数|说明影响后续切图/新图，已有对象不偷改|连续工作默认与启动槽位分别保存|E|MISSING|
|Q07/T01 T552 imageUpload/2194 loadImageFiles|PNG/JPEG/WebP/GIF/BMP/多文件拖放|格式支持、失败逐项；GIF静态编辑明确|原GIF保全，仅派生帧编辑；不伪造保动画|G/E|PARTIAL；S5只PNG/JPEG|
|Q08/T10 T2900 fontUpload，README18/20|字体资源导入/本地解析、使用内置字体|支持格式/缺字/缺字体提示；不隐式联网|字体映射可重建；缺资源提示保留工程|E|PARTIAL；仅font字符串/告警；源浏览器刷新字体也有局限|
|Q09/P11 D9 checkpoint/process/rotate/mosaic/reset|原图工具旋转90°、区域打码、画笔、蒙版清除、撤销重做/重置|前后结果/蒙版覆盖，应用到引导或导出副本|原件不变；旋转与引导区域/蒙版更新明确，保留变换历史|E/N/X|PARTIAL；Suite有基础遮盖，无完整CCB图像工具|

## TypeSet小人书/连环画完整目标（不得默认延期）

|ID / 来源|动作|反馈|保存/重开结果|目标|当前状态|
|---|---|---|---|---|---|
|F01 T4372 setWorkspaceMode|切换图片编辑/小人书|同一队列，图上文下，当前模式明确|workspaceMode存工程，普通编辑层保留|B|MISSING|
|F02 T3768 storybookMetrics/3810 resizeStoryCaptionEditor|输入长短旁白/空段|输入区与页面向下增高，不压原图|每页caption/最低高度保存，重开同布局|B|MISSING|
|F03 T4330 readStorybookStyleControls/normalizeStorybookConfig|改页宽/边距/图文间距/文字padding/最小高|尺寸标记实时变化，边界校验|页面样式保存|B|MISSING|
|F04 T4330/3784 drawStorybookPage|字体/字号/行距/对齐/纸色/字色/边框色|中文排版、整页/图片边框、首行缩进|完整样式和字体引用重开|B|MISSING|
|F05 T4389 storybookPreset/commitStorybookStyle|传统/简洁/复古、自定义|样式更新但本页旁白保留|默认样式与页样式分别记录|B|MISSING|
|F06 T4412 applyStoryStyleToEntries|应用所选/全部页|目标数、样式同步、各页内容不覆盖|批次可撤销，重开一致|B/E|MISSING|
|F07 T3742 getStorybookPageNumber/4407 keydown|开始编号/显示页码/Ctrl+Enter连续下一页|当前页/页码、首尾边界、输入焦点正确|页序/编号/每页旁白保存|B|MISSING|
|F08 T4179 buildEntryVariantBlob/4378 modeUI|当前页/整册文件夹/页面ZIP|每页前后预览/进度，长短页高正确|派生页与工程对应；合成页不带单图NAI隐写承诺|B/X|MISSING|
|F09 T4425 getJsPDF/exportBookPdfBtn|整册PDF|总页数/顺序/每页尺寸/失败原因|独立PDF输出、工程不替换；原生PDF库替代CDN|B/X|MISSING|
|F10 T1033 serializeCurrentWorkspace/ensureStorybook|保存/另存/导入旧队列/重新打开|缺省页样式迁移、无旁白丢失|工程含两种模式与每页内容；旧格式字段验证保留|B/E/L|MISSING|

## 安全、窗口与维护

|ID / 来源|动作|反馈|保存/重开结果|目标|当前状态|
|---|---|---|---|---|---|
|C01 D8 DesktopStudioWorkspace/现有native chrome|宽窄切换/拖分隔/预览专注/系统窗口动作|P7关系与可达按钮，不只折叠左列|视图几何与草稿/工程分离|W/N/E|PARTIAL；U基本窗口通过|
|C02 S7 catalogInternal/operation/close|打开异常目录/单图读取/维护关闭|明确错误与正常原图可读，活动锁保留|无静默清理，恢复可重入|L/现有asset-library|SOURCE_OR_SYNTHETIC_ONLY；历史R1/CP1证据|
|C03 S6 backup/StudioRestore.restore|备份→新目录恢复→打开|验证清单/缺凭据解释，恢复成功可直接进入|原根不改、新根可重开|L|NOT_TESTED；U未知|
|C04 StateJournal/StudioState|正常退出/异常重开/旧版本迁移|保存状态、恢复/版本阻断、不确定任务|提交链保全，旧数据可验证，不付费重发|L/各Store|PARTIAL；E仅旧合成工程|
|C05 SecretVault/LiveRequestBudget|重开账号、预算满等待/取消|实际身份/无法解密/节流原因可见|密文/引用与预算安全持久化|A/L|SOURCE_OR_SYNTHETIC_ONLY|
|C06 docs05/Issue2 CP4|应用内检查、下载验证、安装、重启|版本/渠道/进度/真实结果和恢复|A→B数据/凭据保全、失败不毁原件|L/updater后续组件|MISSING|
|C07 用户验收/平台合同|IME/键盘/高DPI/辅助窗|可读可用/焦点/蓝logo统一|切视图不丢输入；包身份与截图绑定|W/所有Screen|NOT_TESTED|

## 完整性与边界

TypeSet README第1–3章（定位/启动）对应T01/Q07；第4章对应F01–F10；第5章T01/Q04；第6章T02/T03/T14/Q02/Q03；第7章Q01/F07；第8–10章T05–10/T16；第11章T11/12/16/Q06；第12章Q05；第13章T15/Q02；第14章T18；第15/17章T17/19/Q01；第16章Q03；第18–20章Q07/Q08/T18及本方案资源边界；第21章由W4整体串联。HTML控件与事件入口补充了启动优先槽、连续默认和故事书导出，不只按README标题作“已支持”判断。

来源差异明确保留：TypeSet GIF仅静态处理、WebP隐藏metadata不保证、字体刷新有限制、不是完整专业DTP shaping；Suite不应宣称它们已由来源验证。Windows原生等位适配必须记录实际能力和未覆盖项，缺口是待实现，不是默认取消。鼠标缩放、统一图层锁定、持久图库、完整两图比较、统一多API档案属于Suite桥接增强；不虚构为源工具已经具备。

跨产品导入优先提供源TypeSet工作队列/预设有界解析器及已验证CCB格式适配，不执行HTML、不加载远程脚本、不绕过凭据隔离。所有源用户功能通过本表回填真实GUI动作→效果→保存→重开证据后才能解除相应缺口；总产品Gate仍由集中真人验收决定。
