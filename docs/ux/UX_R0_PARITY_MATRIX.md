# UX-R0 — 产品工作流与复用差距

最新范围：Issue #4 于2026-10-21T02:49:22Z新增FULL SOURCE ADOPTION；本矩阵采用完整继承，包含小人书/PDF，无默认延期。逐行用户动作/反馈/持久化/目标位置见 [动作追踪合同](UX_R0_ACTION_TRACE.md)。

状态：**DRAFT FOR PROJECT REVIEW · PRODUCT HOLD / NOT ACCEPTED**。2026-10-21。本报告是静态源码审查与既有真人反馈的归纳，不是新一轮软件测试。

## 身份与证据规则

- Suite 审查起点 `608be5083bb58092988fa68d24b8c8ea646f5a27`；产品实现仍为 `209b5252e06ea55b6cdf970da131b330a407e882`。下列 Suite 链接固定到实现，不能随分支漂移。
- Desktop P7 行为参考固定 `5ef95428421f7c693b010ac88f51d470804ab07a`。本轮使用 `git show` 读取该对象；没有读取活动工作树作为权威，也没有更换 SOURCE_LOCK 或 Prompt 来源。此前反馈记录的 `70b5c30c` 只是历史观察。
- TypeSet Forge 1.1：本地 `.reference-assets/typeset-forge-1.1/TypesetForge_1.1.html`，SHA-256 `892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63`，200005 bytes。`T:行号/函数` 均指该文件。仅静态读；没有运行 HTML、下载其脚本/字体或上传原件。完整来源清单见 [SOURCE_EVIDENCE.json](../audits/evidence/UX_R0/SOURCE_EVIDENCE.json)。
- `U`：[用户首轮实际反馈](../rc/PREVIEW_USER_REVIEW_1.md)，不是本轮重测。三张真实 P3 用户截图见 [视觉证据说明](UX_R0_PROJECT_REVIEW.md#视觉证据及边界)。
- `E`：[P3 工程交付](../rc/PREVIEW_PROJECT_REVIEW.md)与其原始证据，仅支持当时指定源码的自动化范围；本轮没有重新运行测试。

状态采用最新Issue的五值：`USABLE_REAL` 仅指定范围实际用户操作已验证；`SOURCE_OR_SYNTHETIC_ONLY` 底层源码或合成链路存在、真实工作流未接；`PARTIAL` 有入口/局部动作但不完整；`MISSING` 明确缺失；`NOT_TESTED` 证据不足。原Issue旧分类已映射，历史测试结果未改：SOURCE_ONLY/SYNTHETIC_ONLY→SOURCE_OR_SYNTHETIC_ONLY；UI_ENTRY_ONLY→PARTIAL；USABLE_REAL_FLOW→USABLE_REAL。USABLE_REAL不意味着真实付费请求，也不升级整条故事。

## 源码索引

Desktop 链接均固定到上述 P7 SHA：

|代号|实际读取的文件/锚点|证明范围|
|---|---|---|
|D1|[Panel:94](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopNovelAiStudioPanel.kt#L94)|分段 Prompt、导入、引导、工具窗口与结果表面|
|D2|[Controller:163](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopNovelAiStudioController.kt#L163)|身份、账户、历史回用、metadata、设计/生成/取消|
|D3|[DesignConversation:24](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopDesignConversation.kt#L24)|设计历史、编辑分支、逐轮应用、流式进度和输入区|
|D4|[PromptField:93](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopStudioPromptField.kt#L93)|Tag/caret/注释/全文确认与取消|
|D5|[Characters:29](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopStudioCharacters.kt#L29)|卡片、顺序、启用、正负词、5×5/连续位置|
|D6|[History:59](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopStudioHistory.kt#L59)|日期/搜索/相册/范围选择/配方回用|
|D7|[Guidance:20](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopNovelAiGuidance.kt#L20)|参考/Vibe缓存/聚焦区域/蒙版准备，UI见D1:137|
|D8|[Workspace:43](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopStudioWorkspace.kt#L43)|900dp宽窄切换、独立全高预览、固定生成栏|
|D9|[ImageTools:151](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopImageToolsDialog.kt#L151)|隐私清理、APNG伪装/还原、复制、另存、结果回用|
|D10|[Controls:215](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopStudioControls.kt#L215)|模型/尺寸/数量/Steps/CFG/Rescale/Sampler/Seed/连续目标|
|D11|[Metadata:11](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopImageMetadata.kt#L11)|PNG/alpha读取适配|
|D12|[PostProcess:18](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopImagePostProcessPanel.kt#L18)|Enhance/Upscale、成本、前后对比、停止、结果保存|

Suite 固定实现索引：

|代号|文件与行号|本轮观察|
|---|---|---|
|S1|[Main:138](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/209b5252e06ea55b6cdf970da131b330a407e882/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/Main.kt#L138)|单Controller；创建工程209、设计211、生成216、画层222、导出228|
|S2|[WorkspaceUi:55](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/209b5252e06ea55b6cdf970da131b330a407e882/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/WorkspaceUi.kt#L55)|单Inspector284；图库178、画布225、账号351|
|S3|[Generation:19](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/209b5252e06ea55b6cdf970da131b330a407e882/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/Generation.kt#L19)|Recipe、合成设计24、合成请求/任务39、字符串比较|
|S4|[Models:12](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/209b5252e06ea55b6cdf970da131b330a407e882/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/Models.kt#L12)|账户validate仅SYNTHETIC；Layer27；工程快照与撤销48；状态版本1|
|S5|[Renderer:32](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/209b5252e06ea55b6cdf970da131b330a407e882/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/EditorRenderer.kt#L32)|基础文字/遮盖；47代码点竖排；82编码|
|S6|[Workspace:14](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/209b5252e06ea55b6cdf970da131b330a407e882/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt#L14)|原件/工程/导出52/备份76/关闭86/识别根92|
|S7|[AssetStore:233](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/209b5252e06ea55b6cdf970da131b330a407e882/modules/asset-library/src/main/java/io/github/misakapiano/ccbstudio/assets/AssetStore.java#L233)|R1目录分类/索引恢复；111操作门禁、301排水|

## A. Studio 创作闭环（W1，兼 W2/W3）

|ID|参考能力与位置|Suite现状/证据|状态|恢复要求|
|---|---|---|---|---|
|P01|D2:163–207身份/账户，D1:194账户区|S4:21拒绝非合成；S2:351只能增加合成别名|SOURCE_OR_SYNTHETIC_ONLY|真实LLM/NAI分档案、密钥录入、有效模型、错误定位；保留安全存储|
|P02|D1:77成本、D10设置、D2账户刷新|S2无账户额度/成本表面|MISSING|生成前估算、未知值、额度新鲜度、实际扣费不冒称精确|
|P03|D3多轮对话/附加当前Prompt/流式/停止|S1:211+S3:24硬编码杯子并拼接输入；U明确无法验证|SOURCE_OR_SYNTHETIC_ONLY|完整设计对话与明确应用步骤，保留权威Designer|
|P04|D3:70分支、85重试、89逐轮应用；D2:477|S4 DesignRecord仅平面历史；S2列文字记录|PARTIAL|分支/重试/历史切换/未应用候选可回到草稿|
|P05|D1:94–118画风/基础/补充/基础负向|S1 prompt/negative两个字符串；S2折叠表单|PARTIAL|完整分段、当前用户修改、AI建议、最终请求分离|
|P06|D4 Tag候选/翻译注释/全文编辑/caret|权威模块有相关源码，S2 Field无绑定|SOURCE_OR_SYNTHETIC_ONLY|选择候选才替换；IME/全文取消不丢草稿；本地词库来源和失败状态|
|P07|D5角色卡顺序/禁用/正负词/数量限制|S1:210反序列化JSON；S2角色JSON框|PARTIAL|可视角色卡；不靠JSON输入正常使用|
|P08|D5:72位置：V4.5网格/V5连续，聚焦坐标|S1仅可输入JSON，缺位置工作区|MISSING|位置预览+数值编辑；失效字段解释而非静默丢弃|
|P09|D10:215完整设置和分模型语义|S2:296–302仅模型、宽高、Steps、Guidance、Seed|PARTIAL|模型/尺寸档位/比例/数量/自定义/CFG Rescale/Sampler/随机种子；权威范围校验|
|P10|D10:243连续生成目标|S3可count但UI无完整连续控制|MISSING|显式目标、逐任务成本/停止/部分成功；遵守全局调用节流|
|P11|D1:137、D7图生图基图/聚焦/蒙版|权威图像核心存在；S3 guidance始终默认空|SOURCE_OR_SYNTHETIC_ONLY|独立引导工作区，准确请求裁剪坐标与结果合成|
|P12|D7精确参考/Vibe缓存、D1:155–167参数|无导入/移除/强度/缓存成本UI|SOURCE_OR_SYNTHETIC_ONLY|按模型启用参考；资产保留与缓存命中可解释|
|P13|D2:383生成/538停止，D1:375中间图标识|S3拦截器不发网络；独立live probe不在UI路径|SOURCE_OR_SYNTHETIC_ONLY|真实provider可注入，发送前快照，取消与不确定结果不自动重发|
|P14|D1:174–253结果/filmstrip/复用|S3 final入库，但S2:166/188被当前工程页集合过滤；U看不到新图|PARTIAL|结果选择、全部图库、工程成员各有集合；成功原件必有可见入口|
|P15|D6:124搜索/日期/折叠相册/范围选择|S2仅名称筛选与窄列；U不符合CCB历史|PARTIAL|真正历史相册、生成组、设计/来源筛选、复用路径|
|P16|D2:246–267历史回用模式，D6:192|S2无参数/Seed/新种子/引导回用|MISSING|选择回用范围；缺原参考告警；应用可撤销|
|P17|D12 Enhance/Upscale、停止/结果比较|权威相关能力不能从现有按钮证明接入|MISSING|图像工具内独立后处理页，成本/前后预览/保存/用作；不能称为本地缩放|
|P18|D1:304反推流式候选/320角色/应用|无反推入口|MISSING|导入图片后反推→候选→选择应用；真实调用显式身份/费用|
|P19|D1:104角色卡Prompt导入与语义解释|无角色卡选择与按字段预览|MISSING|只提取授权导入的图像字段；不引入聊天全应用，参考Prompt不误送生图|
|P20|D1:96复制/粘贴覆盖；D4全文确认取消|仅常规文本框，没有结构化互换确认|PARTIAL|分段复制/粘贴预览/撤销；整套跨App预设不是P7已完成能力|

## B. 导入、比较、结果交付（W2/W3）

|ID|参考能力与位置|Suite现状/证据|状态|恢复要求|
|---|---|---|---|---|
|I01|D1:86导入/180拖放；D11读取|S1:203文件选择/多选/拖放存在；U样例通过，不等于任意PNG导入|PARTIAL|导入事务完成后聚焦新图，逐文件回执；原字节与读取状态分开|
|I02|D2:297 metadata选择；D1当前图片工具|S2:328原始JSON弹窗；U可打开|PARTIAL|可读分段、字段勾选、角色关/覆盖/新增，先预览后应用|
|I03|D11 PNG/alpha stealth；T:2014/2045|权威reader存在，UI没有完整选择回用；无本轮真实PNG覆盖测试|SOURCE_OR_SYNTHETIC_ONLY|受限解码、完整字段、未知字段原样留存；不能把缺metadata当无原图|
|I04|Suite REQ07完整两图研究比较（超出D6详情）|S3 flatten字符串，S2:331 popup；没有两图画布|PARTIAL|并排图/缩放、字段差异、完整正负Prompt/角色/位置/参考/实际请求；逐字段取回|
|I05|D9隐私元数据+LSB清理|S5 PrivacyPngEncoder；S2直接导出；U导出流程失败|PARTIAL|导出工作区有模式解释、预览、路径、验证回执；不等同伪装|
|I06|D9:168 APNG伪装/还原；Desktop89精准3项用户通过|Suite没有接线|MISSING|独立设置/前后预览/还原/文件复制与保存；动画不得静态化冒充成功|
|I07|D9复制结果/复制文件/另存/显示副本|S1:228直接写内部exports后显示路径|MISSING|目的地选择、单图复制/多文件复制、冲突确认、失败保留；不得只有弹框路径|
|I08|Suite原件保全要求；T“原始排序”重新编码|S6:60 ORIGINAL直接取原字节，E原字节自动化；U没有独立签字|SOURCE_OR_SYNTHETIC_ONLY|保留该能力；原字节副本单独标签和hash，不与无编辑重编码混淆|
|I09|D6删除与选择，Suite安全范围|U全选/点图/上下移可用；没有全不选/反选；S1:202单击即toggle|PARTIAL|聚焦与选择分离；可见范围选择；删除工程成员不删除原件|

## C. TypeSet 编辑与排版（W4）

TypeSet 下列结论为源码存在/行为推导，不是本轮对 TypeSet 的真人运行签字。Suite 必须达成工作流等效，不要求复制 HTML、IndexedDB 或外部CDN。

|ID|TypeSet/参考定位|Suite现状/证据|状态|恢复要求|
|---|---|---|---|---|
|T01|T:1033/1067/1093队列保存/导入导出、1266另存|S6 newProject有引用/顺序/历史；U不知“＋”做什么|PARTIAL|明确“新建编辑工程”，命名/成员预览/自动保存/另存/可携带工程导入|
|T02|T:1783–1833单选/Ctrl/Shift/成组排序，2213上下顶底|U基本排序通过；S2无清空/反选/组拖动|PARTIAL|队列顺序与图库顺序分离，选择范围显式，批量可撤销|
|T03|T:1467缩略图大小，4629悬停，4113全屏预览|S2固定窄缩略图/基础预览|PARTIAL|可调队列、处理前后缩略图、当前图标记；全屏返回保留上下文|
|T04|T:3301画布到CSS单位、3306手柄、3838坐标、3910附近pointer交互|S2:225只收拖绘；缩放有表面，无对象选中/移动/缩放手柄|PARTIAL|选择工具、平移、适应/100%、手柄、边界/坐标反馈；缩放后命中正确|
|T05|T:2687/2730同步选中文字与属性；2886切换writingMode|S2:309新工具参数和314应用现有层分离；U竖排FAILED|PARTIAL|选中对象即属性目标，编辑即时预览，一次操作可撤销，无隐藏刷新步骤|
|T06|T:2932 wrapStyledLines、3281 textMetrics|S5:54仅按显式换行drawString，不按框宽排版|MISSING|自动换行、框尺寸/溢出提示、基线/对齐/行距字距|
|T07|T:2973–3278竖排字形/全角ASCII/标点/右向左换列|S5:47逐代码点移动，缺标点字形/组合字规则|PARTIAL|中日文标点、混合拉丁数字、换列、原文本不变；真实字体样例验收|
|T08|T:2584/2636富文本token、3356/3392按样式绘制|S4 Layer单一font/color/fontSize；S2仅font/size|MISSING|同框混合字体/颜色/大小/粗斜体/描边，稳定选区编辑，兼容纯文本|
|T09|T:2687样式控件、3435背景/边框|S4部分color底层，S2无完整属性|MISSING|颜色/透明度/描边/背景/边框/装饰线/对齐；属性分组，不堆长列|
|T10|T文本字体上传（README8.2）/字体选择|S5缺字体告警阻止导出，UI仅自由填名|SOURCE_OR_SYNTHETIC_ONLY|保留缺字体阻断；系统字体选择和授权字体本地导入，不隐式下载/随包再分发|
|T11|T:3610/3669/3698矩形/画笔/圈选遮罩|U三种拖绘+撤销实测通过，S5:59形状|USABLE_REAL|仅“基础合成图画遮盖”这一窄范围通过；保留坐标/形状思想，补选择/移动/参数|
|T12|T:1305/3546/3698马赛克/模糊/黑白条、强度/笔宽/密度|S1:225画笔固定18；S5仅mosaic或solid|PARTIAL|三形状×四效果、宽度/强度/条纹/点密度、实时效果和预设|
|T13|T:3458/3698按对象绘制，Suite图层需求|U显示/隐藏/移除通过；没有图层选择/重排/锁定|PARTIAL|独立图层列表，选中状态与画布同步；顺序影响输出可解释|
|T14|T:4514批量样式保留文字/位置；4538各自上一张|S6:45只向多页追加同一层，不是批量套样式|MISSING|预览目标/差异，保留每图文字位置；“复制图层”另一个显式操作|
|T15|T:1649/1700历史、输入优先撤销|S4快照保留30步；U按钮基本通过|USABLE_REAL|仅基础图层undo/redo通过；补拖拽合并事务/IME本地撤销/批次整体撤销|
|T16|T:1405打码预设/2525文本预设/1115预设导入导出|无预设管理|MISSING|命名/覆盖/删除/恢复预设，携带schema与字体引用，不含密钥|
|T17|T:4155–4244四分类全队列导出|S6三模式不区分仅文字/仅打码，无分类预览|MISSING|无编辑渲染/仅打码/打码并嵌字/仅嵌字，明确全队列或所选，结果清单|
|T18|T:2014/2081/4179 NAI metadata保留策略|S5 NORMAL仅CCBStudioRecipe；不是完整NAI参数互通|PARTIAL|原源metadata与编辑Recipe分开，格式能力/签名失效明示；隐私与保留互斥|
|T19|T:4254选目录/4282 ZIP；README15格式|S6固定内部目录，PNG为主，原字节JPEG例外|PARTIAL|先选目标/命名/冲突，再预览、复制或保存；PNG/JPEG/WebP按编码器能力逐项验证|
|T20|T:3768故事书/4425 PDF|Suite不存在；Issue4最新完整继承要求覆盖旧候选标注|MISSING|完整继承：小人书动态页高/连续页面/整册PDF必须进入UX-R2c；本轮不提出或默认批准删减|

## D. 持续使用与系统界面（W5，贯穿全部）

|ID|证据/要求|Suite现状|状态|处置|
|---|---|---|---|---|
|C01|D8宽窄/预览模式；U顶栏操作|蓝logo/拖动/max/min通过，窄窗“不挡”但模式不同|PARTIAL|保留native适配；按GUI合同改工作区响应行为，不能把折叠左栏当完成|
|C02|S7 + R1/CP1历史故障测试|不可变字节、单写根、目录异常不静默、恢复/排水存在|SOURCE_OR_SYNTHETIC_ONLY|底层明确保留；SOURCE_OR_SYNTHETIC_ONLY表示无独立产品安全诊断面板，不撤销历史工程PASS|
|C03|S6:76/StudioRestore；E合成备份恢复|U只“可能有用”；恢复后要CLI重开|NOT_TESTED|产品流程未知；改为备份→选择新目录恢复→检查→打开的GUI闭环|
|C04|StateJournal/Models；E重启证据|工程有持久化；完整草稿/对话/视图不全|PARTIAL|恢复工程与草稿分版本；不可由旧工程重启通过推断新工作流通过|
|C05|SecretVault/LiveRequestBudget与E|DPAPI/引用/限额基础；UI真实凭据路径缺失|SOURCE_OR_SYNTHETIC_ONLY|保留，接档案管理；失败不回退明文/隐式换账户|
|C06|docs/05和Issue2 CP4|仅信任lab，真实安装更新链路缺失|MISSING|完整检查→验证→排水→安装→重启→数据凭据验证，不降低门槛|
|C07|U/basicWindow及E|完整键盘、DPI、IME、无障碍流程没验收|NOT_TESTED|在固定包集中验证，不把编译/窗口显示当可用|

## 引用工具也有证据边界

Desktop [86真人记录](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/docs/desktop/86_PHASE7_FINAL_ONE_SESSION_USER_ACCEPTANCE.md)原为16 PASS/1 FAIL/1 NOT TESTED；[89 APNG窄修复](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/docs/desktop/89_PHASE7_APNG_CLIPBOARD_USER_ACCEPTANCE.md)后来3/3用户通过，未覆盖全部隐私流程。[91最终Project结论](https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/5ef95428421f7c693b010ac88f51d470804ab07a/docs/desktop/91_PHASE7_FINAL_PROJECT_ACCEPTANCE.md)是有范围工程接受，保留59 SIGNED/12 USER-EVIDENCED/9 PARTIAL/28 TECH-EVIDENCE/7 OPEN，不是115项全通过。Suite继承行为合同与权威源码，不继承其他应用的用户签字。

结论：有价值的资产/事务/权威请求底层应保留；当前UI和应用编排需要按五条完整故事重组。不能把P/T/I/C各行实现为旧Inspector中的一串新按钮。完整拟议结构及依赖顺序见 [GUI合同](UX_R0_WORKFLOW_AND_LAYOUT.md) 和 [实施方案](UX_RECOVERY_IMPLEMENTATION_PLAN.md)。
