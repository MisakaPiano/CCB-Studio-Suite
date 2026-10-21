# Preview 用户验收第1轮：未通过，需功能纠正

2026-10-21 JST。用户实际测试的包对应实现 `209b5252e06ea55b6cdf970da131b330a407e882`，交付回执 `aa0866535901f1ec5035fded1d4cf66082a3ef24`。用户已自行关闭窗口。本记录补充真人反馈，不改写旧自动化原始证据，也不意味着发现了资料损坏。

**结论：USER_REVIEW_FAILED / CORRECTION_REQUIRED。旧 PREVIEW_GATE_WITH_GAPS 是交付时的工程状态，不是本轮用户认可。当前不得推进 CP4 或继续把该包推荐为验收已通过版本。**

## 用户确认及问题清单

| ID | 用户实际反馈 | 本轮判定 |
|---|---|---|
| UR1-01 | 蓝色logo/文字、拖动、最大化/还原/最小化可用；缩窄不遮挡，但逻辑与Desktop不一致 | 基础操作USER_PASS；响应式行为PARITY_FAILED |
| UR1-02 | 样例/全选可用，缺全不选/反选；顶部＋不知创建了什么 | MISSING / UX_FAILED |
| UR1-03 | 缩略图/上下移/资料库展开收起可用，但与CCB生图历史和图库差距过大 | 基础操作USER_PASS；图库工作流PARITY_FAILED |
| UR1-04 | 矩形、画笔、圈选、撤销重做、隐藏显示移除可用；竖排勾选不立即生效、需复杂刷新；整体打码远逊于图片编辑器 | 基础动作USER_PASS；文字交互FAILED；编辑器PARITY_FAILED |
| UR1-05 | AI设计无法验证等效；继续修改像复制输入到Prompt；生成结果未进入图库 | 真实AI效果NOT_VERIFIED；结果展示FAILED |
| UR1-06 | Metadata/设计/任务记录可查看；找不到隐私伪装设置；未选择路径就直接导出；要求编辑和复制deck、批量操作/预览/复制/保存；导出总体完全失败 | 记录查看USER_PASS；导出流程USER_FAILED |
| UR1-07 | 备份“可能有用”，对无法导入/恢复提出疑问 | UNKNOWN，不记PASS；需明确导入与恢复入口并验证往返 |
| UR1-08 | 用户认为没有认真参考Desktop/安卓版，产品与预期差距显著 | 产品验收未通过；不是仅调整配色即可处理 |

用户的“编辑和复制deck”保留原词；未擅自认定它就是Recipe JSON、metadata或某种文件格式。后续须结合现有工作台操作明确对应交互，不能用技术文本弹窗替代用户要求。

截图1–3原件保存在 `docs/audits/evidence/WINDOWS_PREVIEW_USER_REVIEW_1/user-review-{1,2,3}.png`，均为用户本轮主动提供的Preview合成界面。未读取手测工作区、真实图库或凭据，未重开应用，未调用API。

## 已从Suite源码确认的原因（尚未修复）

1. `WorkspaceUi.kt` 的 `Library` 与 `PreviewPane` 在已有工程时都只枚举 `project.current.pages`；全库计数却来自 `c.assets`。`Main.kt generate()` 更新 current/selected，但未改变这个筛选。截图3显示4份原件、列表仍3张，与此问题吻合；**不能据此认定图片未持久化，也不能把展示失败说成通过。** 正确区分“全部图库”“生成历史”“当前工程页”，新结果在图库可见，是否加入工程由明确操作决定。
2. 竖排复选框只更新 `c.vertical`；已有图层直到下方“应用文字设置”才写回。没有被选中文字图层的直接属性绑定。自检只测试新建时带vertical参数，因此漏测用户实际交互。修复应包含选中图层、即时预览/提交、撤销与重启验证，而非再要求用户刷新。
3. `Generation.kt` 的 `SyntheticDesignerTransport` 按模板拼接 `instruction`，UI从未接真实transport。虽然权威Designer/serializer函数确实执行过，合成输出和两次独立真实探针均不足以证明实际UI的AI效果。后续真实入口必须沿用唯一权威核心、SecretRef与全局限额，明确合成/真实模式；每次实际底层请求（含规划/修复/重试）计入预算。
4. `EditorRenderer.encode(privacy=true)` 只使用 `PrivacyPngEncoder`；没有APNG伪装/还原流程。`Main.kt export()` 直接写工作区exports并弹出路径，没有选择目标、结果操作区、复制或批量预览。隐私清理与APNG伪装是不同功能，不能拿前者的测试代替后者。
5. 顶部＋调用 `newProject`，保存所选图片的顺序、编辑图层与撤销历史，原件独立保存；没有名称/目标/用途说明。应使用明确的“创建编辑工程／加入现有工程”等操作，不让用户猜图标。
6. 当前BRUSH宽度在控制器硬编码18f；缺少参考编辑器的笔刷大小操作、涂抹反馈、完整结果预览/复制/另存流程。基础矩形/圈选成功不是完整编辑器等效。

## 已实际读取的参考实现

只读固定 Desktop 当前提交 `70b5c30c91439e79728bf224c1a44d35b25cf733` 的22份相关文件，路径/原始字节hash见 [SOURCE_OBSERVATION](../audits/evidence/WINDOWS_PREVIEW_USER_REVIEW_1/SOURCE_OBSERVATION.json)。不改Desktop，不替换既有图像请求权威5ef95428。这里的Android代码是该Desktop仓库携带的Android实现；**尚未独立核对官方Android最新分支，不能声称已完成两个平台全面等效审计。**

- `DesktopStudioWorkspace.kt`：900dp宽度判定、可拖动分隔、展开预览/仅缩略图/预览专注、紧凑展开与收起、导航和窗口控制区布局。Suite当前760dp切换三单面板不是同一逻辑。
- `DesktopStudioHistory.kt`：自适应网格、折叠/筛选与历史选择；`DesktopNovelAiStudioPanel.kt` 分开维护当前导入图、生成结果、设计历史与生图历史。Suite把当前工程页面充当图库不等效。
- `DesktopImageToolsDialog.kt`：独立图像工具、涂抹反馈、APNG伪装、复制结果/动画文件、选择另存位置、显示已保存副本；`DesktopImageClipboardFiles.kt` 与 `DesktopSafeClipboard.kt` 给出文件/像素复制边界。
- Android `ImageMosaicEditor.kt`：笔刷类型/尺寸、撤销/重置/旋转、独立去元数据、APNG伪装/逆向还原、结果预览与保存；不是现有四个简化按钮的同义实现。
- Android `NovelAiHistoryScreen.kt`：相册/折叠/筛选、批量保存/删除及冲突处理。`ImagePrivacyExporter.kt` 的去元数据副本也与APNG伪装分开。

## 后续纠正的验证入口

1. 先按上述真实实现完成图库/历史/工程和响应式工作台对应表；不继续凭印象画一套不同的工作流。
2. 修正全选/全不选/反选、新结果可见与工程用途；测试“当前工程打开时生成新图”这一真实漏测路径。
3. 文字属性直接作用于选中图层；补实时预览、撤销、保存重开的一致性测试。编辑器补参考实现的笔刷与结果操作区。
4. 接通真实设计/生图UI与命名安全账户；独立验证真实模式/限额/取消/不确定结果保全，不能用合成模式代签。
5. 分离普通、去元数据、APNG伪装与原始导出；提供结果预览、目标选择、复制/保存及明确批量目标，验证原件不变和动画语义。先确保真实功能，再整理UI。
6. 完成针对性可靠回归与新包自检后再进行下一次集中真人验收；当前不存在修复完成/新版验收PASS结论。

本轮实际动作仅为反馈保全、源代码原因定位和当前状态纠正；没有更换用户正在评审的二进制，没有新实现或新测试执行，也没有创建新Preview包。保留上次工程测试PASS，但它们不能覆盖本轮真人FAILED。
