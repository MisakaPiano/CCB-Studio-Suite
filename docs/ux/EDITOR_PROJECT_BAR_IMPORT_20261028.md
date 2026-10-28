> 2026-10-28 用户最新确认：本轮收起／重开、归档／历史重开、完整包导出再导入三项均通过；并授权收尾发布。下方“待试用”是保留的候选时点，完整编辑／资产／115／Prompt及独立范围不随之签收。

# 工程栏去重与完整包精简导入（本地候选，待用户试用）

2026-10-28 JST；沿 UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001。仅本地实施与合成验证，无新提交、推送、发行或日常118变更。

## 实现与边界

- 移除所有“移出工程列表”UI入口及控制器动作；保留旧REMOVED记录、序列化及核心读取兼容，不改写旧资料。收起保存并清空，归档仍沿用同一工程与原图，废弃必须确认并按既有独占记录回收规则执行。
- 单一工程菜单收纳本机工程、导入、当前设置、收起及仅待保存时可用的保存；常用新建、归档、废弃、导出常驻。追加图片只在队列头，底部只留画布／历史／预览操作。
- 用户指出标题太窄并要求导出最右、废弃其左；已采用宽窗口剩余空间填充标题、长名悬停查看，完整显示“废弃当前工程”，垃圾桶启用时使用主题错误红色，导出靠右。窄窗口仍可水平浏览，不将浏览器demo的320px检查冒充原生验证。
- 本机列表分进行中／已收起与归档历史；复用原归档读取及重开、删除能力。嵌入列表时隐藏重复返回按钮；重开／删除固定在内容滚动区域之外，避免封面挤掉入口。
- 名称和多层组织设置复用新建／归档的暂存树，保存前统一校验版本、父节点及名称，再原子发布；取消不落盘，不复制原图或配方。
- 新完整包导入仅写入原图字节和Studio支持的当前文字／打码／排版／队列等内容，sourceExchangeId=null、sourceEntryIndex=null；不再把整包作为资产保存，也不保存外来历史／未知字段。确认摘要明确说明；外部包不改动。
- 已有导入工程的源整包、未知字段及原文件另存能力保留；本轮不迁移、不清理任何已有导入资料。主动完整导出仍嵌入原图和当前可编辑内容，另有成品图片／PDF等导出；不包含模型、凭据、字体文件、本机撤销历史及组织树。64MiB上限保持。

## 固定输入与入口

- 基点 `03f589c313b1aef324b39e4eb697bbbe747fb305`，另有未提交模块差异；分支 feature/windows-rc-continuous，index未改。
- 固定源码映射 `H:\CCB-Studio-Suite\.local\windows-preview\evidence\20261028T060806825846Z-project-bar-import-reviewed-20261028\FIXED_SOURCE_MAP.json`（SHA-256 `5867d1f738713f177d40cebd7a0f93bffd9aa174b9e4acdd8cb430d736fc1fd0`），共23个变更/新增模块文件。保留现有窗口修复，并非仅凭HEAD构建。
- 候选与59项运行输入：`H:\CCB-Studio-Suite\.local\project-bar-import-20261028-final/CANDIDATE_INPUT_MAP.json`；种子驱动仅用于合成准备，不加入生产运行类路径。
- `studio-core.jar` SHA-256 `7b4633031129dbb4606ce972b05394c639c27e94be40e40657f56080d3d987c8`，70199438 bytes。
- `studio-desktop.jar` SHA-256 `865803081bfdd623b3a37371996ae5275053a28900c7ef6b4dda649b98115428`，49421656 bytes。
- `asset-library.jar` SHA-256 `f55abfcfa68293ca1b58e31c5aa1574c86f7d141bfc491c4ba949147dc75ebe5`，58442 bytes。
- 启动链接 `H:\CCB-Studio-Suite\.local\project-bar-import-20261028-final\Studio-Project-Bar-Review.lnk`；正常MainKt，独立合成资料根 `H:\CCB-Studio-Suite\.local\disposable\editor-native-review-13a7a95e7ed842e881f23f5290ed89f1\payload\workspace`，12合成图／0账户。初始页面为创作，点击顶部编辑即可继续样例工程。
- 当前启动 PID 28540，日志 `H:\CCB-Studio-Suite\.local\project-bar-import-20261028-final\launch-20261028T060951815Z`；前一候选PID6700正常关闭，无强杀。两份试用根按人工资料保护，不自动清理。

## 自动结果与失败保留

- `H:\CCB-Studio-Suite\.local\project-bar-import-20261028-final/REVIEW_VALIDATION_INDEX.json`保留逐例来源与全部7次流程状态；13个唯一JUnit用例分阶段有PASS证据，重复运行不累计。
- 核心9例：精简导入、外部及已有源不变、原图／配方、重导出及备份恢复、未知版本／恶意内容拒绝、旧源兼容与合成退役、名称组织原子保存等。该9例所处流程仍因桌面编译失败保持FAILED，不冒充整次通过。
- 桌面4例：单行入口去重与位置；真实菜单收起→列表重开→归档→历史重开、废弃取消保留；导入预检取消／确认→导出→重开；相关底部预览与导出入口回归。最终标题宽度>500px、废弃在导出左侧、导出贴右、历史重开按钮在可见范围有断言；截图已人工目视检查。
- 首轮新增测试错误比较重新导入生成的对象ID，实际内容一致；修正仅标准化ID后继续比较所有文本属性，原失败XML与failed-source保留。随后新增placement参数造成旧归档调用编译失败，修正具名参数；原日志/source-before保留。
- 截图发现嵌入历史的操作被封面挤掉、用户否决标题过窄均留痕，后修复并定向复核。测试编译不等于执行；原生进程可响应不等于用户验收。
- 环境 Windows / JDK17 / Python3.13；现有串行离线Gradle runner、strict依赖验证、1 worker、ActiveProcessorCount=2、synthetic fixture，无真实服务调用。每阶段summary保留完整参数、时间与sourceStable=true。
- 最终命令：`.local/editor-native-20261027/Run-EditorChecks-C.py --phase project-bar-import-reviewed-20261028 --keep-fixtures -- -Dorg.gradle.jvmargs=... :studio-desktop:test --tests *TypesetToolbarWorkflowTest :studio-desktop:jar`，完整准确转义以summary.arguments为准。
- 成功测试临时根先把截图/日志留在证据，再由既有owned manifest机制清理5份自身合成fixture；失败根、旧证据、用户试用根及先前导入保留。清理回执`OWNED_TEST_FIXTURE_CLEANUP.json`。

## 验收状态与下一步

- 实现：本轮工程栏／精简新导入已接入。自动：上述13唯一例有PASS证据。用户：两张新候选截图是布局反馈，不等同收起／归档／导入导出已验收。独立审查：本轮未进行。
- 既有编辑预览最大化／还原／关闭三步已获用户确认，不要求重测。资产／编辑完整人工、115体验、Prompt对比、AI精度及旧独立UNKNOWN／失败／否决继续保留。
- 用户只需试：标题／按钮位置与菜单；收起后本机列表重开、归档后历史重开；导出完整包再导入继续编辑。废弃若测试只用合成样例，确认前说明原图保留。
- 仍不发布、不更新日常安装；签收并收尾编辑改善、固定交接后才迁移新对话。
