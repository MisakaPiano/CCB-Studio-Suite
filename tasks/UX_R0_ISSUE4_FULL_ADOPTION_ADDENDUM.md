# UX-R0 — Issue #4 最新范围快照

实际复核：updated_at=2026-10-21T02:49:22Z，评论0。来源 https://github.com/MisakaPiano/CCB-Studio-Suite/issues/4 。此范围覆盖早先快照的冲突表述；本聊天明确限定本轮只交付UX-R0，Project批准后再实施，未因Issue更新获得本轮生产代码授权。

# 2026-10-21 用户直接纠正：必须做两套成熟功能的完整整合（最高优先级）

**本节覆盖本 Issue 后面任何将功能缩减为“借鉴设计”“基本模拟”“仅初版文字/打码”“富文本/小人书/PDF 默认延后”的表述。** 本次不是让团队设计一款类似 Studio 的新 GUI。目标是：

> **CCB Desktop 真正的 NovelAI Studio 既有用户功能 + 用户现有 TypeSet Forge 1.1 图片编辑器的既有用户功能 + Studio Suite 新增的持久图库、完整 Prompt/Recipe 对比及批量导出/跨产品整合。**

用户报告当前 Preview “可以说全都没实现，完全是另外一个有 UI 的软件”。这不是逐按钮 BUG 清单，不能以蓝色 Logo、三栏布局、嵌入权威源码、合成测试通过或有限演示工具冒充功能集成。

## 本次真实用户验收与新证据
- 用户实际测试：`USER_REVIEW_FAILED / CORRECTION_REQUIRED`，冻结审核文件 [PREVIEW_USER_REVIEW_1.md](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/608be5083bb58092988fa68d24b8c8ea646f5a27/docs/rc/PREVIEW_USER_REVIEW_1.md)，含用户三张实际 Preview 截图、明确通过/失败/未知和代码定位；实现 `209b5252e06ea55b6cdf970da131b330a407e882` 未因登记而修复。
- 本 Issue 早先依照较低需求写过“基础竖排、混合字体后续”“小人书/PDF 可延期”等。**这些不是用户许可删减已成熟编辑器功能的依据**。完整继承目标先列出并由用户批准才可对 Release 范围做降级。
- CCB Desktop `desktop@5ef95428421f7c693b010ac88f51d470804ab07a` 提供 P7 功能/语义锚点；Codex 另只读对照过较新 Desktop `70b5c30c91439e79728bf224c1a44d35b25cf733` 的界面实现，不能自动把较新开发状态宣称为经过验证的稳定共享依赖。

## 第一套来源：CCB Desktop NAI Studio 的既有用户工作流
逐项列出并恢复**来源确实存在**的：真实 NovelAI 与 LLM 账户/模型设置及实际请求路径、AI 设计**多轮对话与历史/修订/应用**、画风/基础/补充/负面/角色的结构化 Prompt 与角色顺序/启停/位置、Tag 检索/补全/翻译/全文编辑、模型/尺寸/Seed/采样参数、图像引导/参考/Vibe/Inpaint/适用增强能力、生成/中间预览/停止/错误/实际结果保存、History 相册/日期检索/选择/批量/复用、导入图片的普通与隐写 Metadata 与可选择应用、复制/保存/查看/隐私/APNG 伪装与恢复，以及官方已具备的图像工具操作。准确功能与最新/正式采用版本逐条标明，不凭记忆补造；Desktop 新增强（如统一多账户）与来源既有行为单列。

**不得**把 `SyntheticDesignerTransport`、裸 JSON 角色列表、普通文本 Prompt、结果仅写盘不显示、Recipe/Metadata 字符串弹框，算作这些真实功能的等价交付。优先直接适配已成熟 Compose 用户组件和状态流程；不为了独立产品而重写已有官方 Prompt 与请求语义。

## 第二套来源：TypeSet Forge 1.1 完整小软件
按原 `TypesetForge_1.1.html` 与使用说明做**全量**逐功能盘点，并提供原生等价实现或明确缺口：多工作队列/命名/另存与工程持久化、格子缩略图和悬浮预览、多选(Ctrl/Shift)/成组拖序/置顶置底/精确插入/批量删、编号规则；文本框增删选拖改尺寸、横排/竖排/中日文竖排标点与英文数字显示转换、文字间距行距/字重/斜体/下划线/描边/底色/边框/透明度/混合字体颜色样式富文本、字体资源管理和文本预设；矩形/笔刷/圈选、马赛克/模糊/黑白条及可调参数/预设、真实画布交互与可撤销批量应用；完整的撤销重做与可重新编辑工程；NAI 原生 metadata 保留的可验证规则；四种完整序列输出（原始排序、仅打码、打码+嵌字、仅嵌字），源字节另行无损导出，批量选择目标文件夹与 ZIP/命名/冲突反馈/进度；预览上一张下一张；独立队列与预设导入导出；**小人书/连环画模式、动态页面文字高度/样式/连续页面/整册 PDF**。

源工具基于浏览器 Canvas/IndexedDB 的机制必须用 Windows 原生持久化与渲染等位替代；不用复制 WebView。**全量目标不等于要求一次写完一个巨型 Slice**，允许内部分阶段，但功能不得无记录从产品承诺中消失。是否另将小人书/PDF 等某能力排除首个长期使用 Release，必须取得用户明确范围批准；先前助手单方面“后期候选”的标注不构成批准。

## Studio Suite 新增的整合能力
- 共享一个明确持久化图库与编辑工程库，但必须区分生成历史、导入素材、工程页面、导出结果；新生成图在正确图库可见，是否加入工程要显式操作，不可再因工程筛选导致不可见。
- 原始 PNG 字节/Metadata/Recipe/AI Design 历史/图层与输出互不覆盖；图片处理保持非破坏编辑和可靠恢复；错误/损坏不能默默清理。
- 直观双图与分字段完整 Prompt/Recipe Diff、应用指定差异/回用参数、跨产品复制/导入、分类批量输出。
- 多 API 档案是 Suite 扩展；Server 管理员集中上游凭据/客户端多端排队的原始方案保持不变，但 Server/Android 不混入本轮 Windows 产品修复。
- 首个长期使用版完整安全应用内更新、迁移和保全合同保持阻塞门槛。

## 改为「功能继承矩阵」驱动实施与验收
1. **先产生完全覆盖两个实际来源的 source-to-user-action-to-result-to-persistence 追踪矩阵**：每项有源文件/符号/用户动作、期望的视觉反馈、实际保存/重新打开结果、目标 Suite 实现路径、严格状态 `USABLE_REAL` / `SOURCE_OR_SYNTHETIC_ONLY` / `PARTIAL` / `MISSING` / `NOT_TESTED`、证据。不能只对照有无同名按钮。
2. **建议产品结构优先保留源工作流**：以真正 NAI Studio（完整 AI 设计对话/Prompt/生图）为独立核心工作台；以真正图片队列/排版打码编辑器为独立核心工作台；图库/比较/批量导出作为两套成熟流程的桥梁。允许视觉协调，但不允许用一个空三栏 Inspector 打散并简化所有操作。
3. **先把现有成熟来源功能按顺序接到可见真实 UI，再考虑统一外观。** 每个恢复切片必须在真实可启动的程序中完成从输入→效果反馈→保存→重开验证，不以 mock-only/自检 draw count/窗口命令测试当用户功能验收。
4. 仍可沿旧长跑计划按检查点持续开发、代码和数据自动化照跑、用户人工验收集中；但 **Preview 审核失败在功能恢复/真实配置/正确排版前不解除，CP4 安全更新也不得被删。**
5. 旧验收事实继续保留；下一份验收标准依据完整来源继承矩阵与用户反馈，不以 Code PASS 代替产品实现。

---

## 以下为较早 UX-R0 技术排查与候选（与本次用户直接纠正冲突时以上文为准）

# Windows Preview 产品验收 HOLD：恢复真正的 CCB Studio 工作流，而非继续堆砌功能

**问题性质：产品级 UX/功能缺失；不是单一视觉缺陷或补几个按钮。**
用户已实际使用 Windows Preview，反馈：与 CCB Desktop Studio 差距过大；核心 Studio 功能从真实使用角度基本未实现；附加图片处理不能完成有用的排版/编辑工作流；各功能仅堆放在现有界面。**真人产品验收：HOLD / NOT ACCEPTED（用户反馈）**。不得把既有工程自动化 PASS、窗口绘制/重启和两次 provider 探针称为人用 Studio PASS。

## 精确来源（不要以浮动 HEAD 替代）
- Suite 实现：[`209b5252e06ea55b6cdf970da131b330a407e882`](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/209b5252e06ea55b6cdf970da131b330a407e882)；交付记录：[`aa0866535901f1ec5035fded1d4cf66082a3ef24`](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/aa0866535901f1ec5035fded1d4cf66082a3ef24)。
- 当前交付审核：[PREVIEW_PROJECT_REVIEW.md](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/aa0866535901f1ec5035fded1d4cf66082a3ef24/docs/rc/PREVIEW_PROJECT_REVIEW.md)、[集中验收](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/aa0866535901f1ec5035fded1d4cf66082a3ef24/docs/rc/PREVIEW_REVIEW_ITEMS.md)。
- CCB Desktop 已验收 P7 Studio 产品/源码参考：[`5ef95428421f7c693b010ac88f51d470804ab07a`](https://github.com/MisakaPiano/ChatChatBar-Desktop/commit/5ef95428421f7c693b010ac88f51d470804ab07a)。定向参考 `app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopNovelAiStudioPanel.kt`、`DesktopNovelAiStudioController.kt`、`DesktopDesignConversation.kt`、`DesktopStudioPromptField.kt`、`DesktopStudioCharacters.kt`、`DesktopStudioHistory.kt`、`DesktopNovelAiGuidance.kt` 与对应真实视觉/操作验收记录。不要因平台可分离而强迫直接复制完整应用或改动权威 Prompt。
- 图像编辑参考：用户提供的 TypeSet Forge 1.1，A0 静态来源报告；与 CCB Desktop 图像工具分域审查。原始参考 HTML 为本地 ignored 文件，不得无授权公开上传。
- 现有 Suite 窗口：`modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/Main.kt`、`WorkspaceUi.kt`；合成引擎 `modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/Generation.kt`；渲染器 `EditorRenderer.kt`。
- 仓库当前 Git main、feature、工作树须在下一任务开场重新核对，不能暗中移动基线。

## 可以独立从源码证明的问题（不等同真人截图完整视觉测试）
1. **真实生成尚未接线**：`Main.kt` 初始化 `SYNTHETIC` 账户，`StudioController.design/generate` 使用 `GenerationEngine`；`Generation.kt` 内 `SyntheticDesignerTransport` / 合成 NAI HTTP 响应。独立 live probe 位于测试 classpath，未进入分发 UI。用户在主界面无法正常录入真实 LLM/NAI 凭据并完成真实 AI 设计→生图→保存流程。
2. **现有交互不是 Desktop Studio 的等价移植**：`WorkspaceUi.kt` 的右侧生成/编辑/记录三态 Inspector 把大量功能塞入一条垂直滚动列，角色仅以 JSON 文本输入；AI 设计没有 Desktop 设计对话的历史/分支/逐条应用 UI；Prompt 未接 Desktop 的 Tag 补全/注释/全文编辑；NovelAI 模型/角色位置/图像引导、Vibe/参考、历史结果复用等核心工作流没有等位表面。不能因为共享了85份底层图像源码，就宣称共享了 P7 Studio UX。
3. **编辑器是基础画笔 Demo，不是 TypeSet Forge 级可用排版**：`EditorRenderer.kt` 的竖排是依次 drawString(codepoint) 加简易列偏移，未实现可靠的中日文标点、组合、旋转/直立、换列、文字框排版/溢出/样式编辑。UI 主要是四个工具选择、少量文本输入和“示例图层”；缺失可实用的画布操控、上下文编辑与系统化图层/批量流程，需完整比对而不是凭标签判断实现。
4. **历史/比较是字符串弹框，不是研究用工作台**：`WorkspaceUi.kt` 直接弹出整个 Recipe JSON / PNG metadata / `compareRecipes` 字符串列表；无多图并排对照、分字段差异与轻松将参数/Prompt 取回当前编辑的自然流程。
5. **发布基础仍缺失**：工程交付明确 CP4 应用内检查—下载—验证—安装—重启—数据保全未实现；绝不可将现有 Preview 作为用户可长期存储唯一作品的正式版本。

## 建议的纠偏：先合同和结构，后实施；不要逐项修补旧面板

### UX-R0：产品级反向差距/信息架构审计（本 Issue 第一步，非全仓审计）
- 以 CCB Desktop 的实际 P7 Studio 为行为基准，做逐功能/逐工作流映射。至少：账户/成本；AI 设计对话＋多轮修改/历史；结构化 Prompt（画风/基础/补充/负向/角色与位置）；Tag 助手/全文编辑；图像引导/参考/Vibe；完整请求参数；生成/取消/结果/History/复用；Metadata/导入；原图保存/导出。
- 以 TypeSet Forge 1.1 为编辑工作流参考，审查：图片队列、画布选择/缩放/定位、实时操作反馈、文字框横竖排及字体/样式/换行、打码工具、图层/批量、撤销/重做、预设、分类导出。对每项分别标记 `SOURCE_ONLY` / `SYNTHETIC_ONLY` / `UI_ENTRY_ONLY` / `USABLE_REAL_FLOW` / `MISSING` / `NOT_TESTED`，附具体代码和证据。
- 形成 3–5 个真实用户故事的屏幕流程：**真实 LLM 账户→AI 写/改 Prompt→确认完整分段/角色→真实 NAI 配置/生成→图库保存；导入PNG→查看/应用 metadata→复用；两图及完整Recipe对比；多图文字排版＋打码→批量导出与再编辑；退出重开＋安全更新**。
- 输出工作台 IA/布局图（宽窗/窄窗）与单项控件归属。**拒绝继续用一个右侧超长 Inspector 堆放所有功能；拒绝裸 JSON 文本框作为普通角色编辑器；拒绝多行报文对话框当完整比较器。**
- 给出可运行 P3 真实截图的取得途径，若受制于环境不可截图，则标 `UNVERIFIED`，不能伪造“视觉已对齐”。Git 内 `user-first-launch.png` 是较早 P2 图，不能冒充 P3 最终样貌。
- 本步骤让 **Project 审查整体 IA 与真正 P7 能力覆盖** 后，再进入整体实现。无需用户给出几十个逐项 bug；只有真实不可推断的审美取舍才征求一次集中意见。

### UX-R1：真实 Studio 创作闭环
- 独立 Studio 必须接真实凭据录入/多档案及 LLM/NAI provider、实际 effective model/identity；保留已验证官方 Prompt/NAI request authority，不改原 Prompt 文本。
- AI Designer 是多轮对话和修订流程，不是调用一次 SyntheticDesignerTransport 的两个按钮。AI 输出/用户要求/已应用分段/真实最终请求分别追踪。
- 提供 Desktop Studio 必要的结构化 Prompt/角色/位置/图像引导/Tag/设置/History 回用；由 Controller/视图状态的可验证用户动作组成完整闭环。
- 缺乏真实付费授权时先用可注入 loopback/mock 测全程，真实请求独立批准；**不能再把合成模式作为唯一可交付界面**。

### UX-R2：真正可用的图片编辑与排版
- 建立独立编辑工作台：大画布、图层/批次选择、上下文操作/属性、真实拖拽编辑、可观察的文字换行/溢出、固定易用的保存/导出与撤销，缩放/高DPI/CJK IME/键盘操作。
- 文字排版以可复查文本框/位置/断行/样式/中文日文竖排标点验收样例为目标；不得用代码点一字一列 Demo 代替。
- 打码/批量图层独立验证区域准确性、操作顺序、原图字节保全；对非破坏编辑工程建立重启还原/缺字体告警与普通/隐私/原始字节导出的分离证据。

### UX-R3：历史、导入和比较
- 可视化历史相册/检索/日期与复用；两张图并排（可缩放）＋结构化完整 Prompt/Recipe 差异，导入 metadata 可选择应用到当前草稿。
- 记录实际生成/Design history/导入源/导出身份，不把弹框 JSON 视为用户产品功能。

### UX-R4：完整发行门槛不降级
- 保留 CP4 完整应用内安全更新（检测/下载/校验/安装/重启/数据迁移与恢复）的原始任务；失败保全不降级。
- RC 前对真实 GUI 进行一次覆盖上述用户路径的集中 Windows 人工验收；测试必须附实际源SHA/包SHA及用户结果。合成工程测试保留，但不能替代真实功能和体验验收。

## 安全与实施边界
- **优先保留**已核验不可变原图/资产持久化与恢复、权威图像共享核心、测试与证据、原生窗口适配；审计后允许重构/替换 `Main.kt`、`WorkspaceUi.kt` 与薄 UI adapter，不先大规模重写低层已通过的核心。
- 新的组织边界可为真正 Studio 生成、设计对话、图库历史、编辑器各有明确 Controller/Screen，避免单 359 行 UI + 单 231 行 Main 耦合所有用户流程。技术模块数不是验收标准。
- 禁止向官方 upstream/CCB Desktop 写入；需要 Desktop 侧窄抽取由独立任务授权。禁止清除真实图像/数据根、凭据、历史，禁止隐式跨账户轮换或无许可付费调用。
- 先记录 PRODUCT_HOLD，不偷偷改旧自动化 PASS；用户集中验收未通过前，**不要将其标作 Preview Accepted 或 RC Ready**。
- 单主执行者、远程 feature checkpoint、精确 SHA、保留旧失败/NOT TESTED，按工作流验收而不是按钮/代码行数。
- Issue 本身是**用户反馈的产品整改需求和 Project 审查入口**；不代表 Codex 已运行或直接授权任意代码修改。Project 先冻结最小充分 UX 合同，再安排续跑。

## 最低交付
1. `UX_R0_PARITY_MATRIX.md`：真实 Desktop / TypeSet 来源功能 vs Suite 现状、证据和明确遗漏；
2. `UX_R0_WORKFLOW_AND_LAYOUT.md`：整体 IA、宽窄布局、5个用户故事和每步显示/操作预期；
3. `UX_RECOVERY_IMPLEMENTATION_PLAN.md`：按完整用户流程而不是零碎按钮的修复序列、测试与停止点、预计保留/替换源码边界；
4. 后续每阶段真实可执行界面+证据：不以 Mock Only 或 Window Commands PASS 交付“Studio 产品验收”。

**最终性质：恢复 CCB Studio 核心用户体验的产品修复，非纯 UI 主题换皮。**
