# 显示主题：齿轮修复、隔离入口和本轮人工验收

2026-10-26 Asia/Tokyo。沿用UX-DISPLAY-THEME-001与既有内部候选阶段；仅修用户报告的齿轮入口及截图中实际可见的文字对比问题，保留旧失败/否决和独立审查范围。没有公开Release/tag、日常115替换、信任链/元数据更新、私人资料/凭据读取或真实provider调用；没有新增功能任务。

## 固定版本对应关系

|对象|固定身份及实际状态|
|---|---|
|旧候选|源码80ea116、证据2eb4de1；其构建/安装记录保留。用户后来发现齿轮不可点击和非隔离路径，候选人工验收FAILED/HOLD，不以早先默认页启动记录覆盖|
|齿轮修复源码|`22aa8c55ec4b05db29acb443d85717e80eb9d24a`；StudioApplication.kt与新StudioAppearanceTitleHitTest.kt两文件|
|用户实际测试的内部包|0.2.2-internal，sourceSha=22aa8c5；168,998,350字节；SHA-256 **`ee6f86a3ef347d88a38ea0a16eba1f8cfe75e448ed8960a5df41371404ebf06f`**|
|额外文字修正源码|`48eed2aa35d1b2bcaa86f140ac33aac9cdc286ac`；仅StudioAppearance.kt与新StudioAppearanceTextActionTest.kt两文件；源码/应用映像自动验证通过，**该增量没有新NSIS安装包、原生运行或人工复测**|
|公开/日常115|仍为ff145d051bae286ab3e27cadd41195f8bd4c4e18对应既有安装；未启动或更新日常安装|

两个内部包都使用既有0.2.2-internal本地标识，以完整sourceSha和EXE哈希区分，不冒充公开Preview版本递增。通用jpackage EXE散列可与115相同，不能单看它判定应用代码新旧；实际主JAR、配置、清单和源码映射分别核验。

齿轮包位置：`H:\CCB-Studio-Suite\.local\appearance-gear-20261026\candidate\studio-installer.exe`；实际隔离安装在同根`isolated-install`。本地入口`Start-Studio-Gear-Fix.lnk`通过包装脚本指定`entry-review/workspace`和`entry-review/runtime`，不应直接双击裸EXE。原样本/失败包/基线未覆盖或删除。

[齿轮源码→496输入→实际安装包映射](evidence/APPEARANCE_GEAR_20261026/FIXED_SOURCE_PACKAGE_MAP.json)；[额外文字源码→497输入→应用映像映射](evidence/APPEARANCE_TEXT_ACTIONS_20261026/SOURCE_APP_IMAGE_MAP.json)。后者明确没有把22aa安装包哈希配给48eed源码。

## 齿轮打不开：根因与修复

实际齿轮LibraryAction原来位于整个标题栏区域，但没有像导航按钮那样向WorkbenchChrome.buttons登记测量矩形。原生DesktopChromeHitTest对未登记的标题栏区域返回CAPTION，鼠标被按拖动标题栏处理，无法到达Compose点击回调。

22aa8c5新增真实生产组件StudioAppearanceTitleButton：登记其onGloballyPositioned矩形，移除组件时注销，Workbench使用该组件；原`c.appearance.shown=true`、对话框、设置存储、颜色和请求逻辑不改。WorkbenchChrome只改为internal供同模块真实布局回归使用，没有修改共享Desktop原生桥。

新两项使用生产齿轮、实际测量结果、原生命中决策与Compose指针点击；涵盖760/1320宽度、100%/150%/200%缩放及最大化命中分支，检查拖动空白、关闭按钮和控件移除后的旧矩形。离屏命中/点击不冒充Windows系统实际鼠标或截图验收。

## 隔离入口：已证明和未知分别保留

旧快捷方式实际字符串有4个正常引号，无字面反斜杠引号，目标/参数SHA与交付一致；旧Main字节码为`main(String[])`并把原args交给launchStudio，后者优先解析`--studio-root`、`--runtime-root`。最初有环境白名单的单次启动确实创建任务目录工作区，任务home里的默认路径未创建。

然而后来在**任务自己的程序父目录**发现了新的launch-current.json，明确将内部包指向标准`%LOCALAPPDATA%/CCBStudioSuite/workspace-v3`和runtime，独立印证用户报告的非隔离路径。更新页显示来自实际workspace.root，不是固定文案。**那次用户操作的具体入口、参数链或旧进程来源没有留痕，仍UNKNOWN**；不能仅凭存储的快捷方式正确就认定实际调用正确，更不归责用户。未读取、清理或迁移该默认工作区内容。

新交付入口实际走完整`.lnk → PowerShell包装 → 子进程`链：包装同时固定显式根/secondary/pause参数和子进程LOCALAPPDATA、APPDATA、HOME、USERPROFILE、TEMP/TMP；路径拒绝任务根之外或重解析点。即使参数被忽略，默认回退仍落任务profile，而不会落私人默认目录。没有ExecutionPolicy绕过或系统环境修改。

先通过同一个真实快捷方式启动无Studio探针，核对6个参数及隔离环境，再通过相同入口启动实际包。真实主/子进程命令行均含全部参数，新指定工作区/runtime出现，隔离profile默认回退路径未创建，任务默认入口文件未创建；日常115的launch-current SHA仍`783eb43528a486ff97a08a7581108fc2bd7add8cc2d1464f3e8effdcd84b5df2`。用户更新页截图也直接显示`H:\CCB-Studio-Suite\.local\appearance-gear-20261026\entry-review\workspace`。

入口PROBE、APP校验后开放REVIEW模式，每次用户重开保留独立调用记录。当前用户操作的窗口不再由执行者关闭、重开或改变设置；仅在用户明确说“我关闭了，你可以再打开一次了”后重开过一次，截图确认此前浅色/红色显示效果保留。旧Computer Use物理Escape中断遵守停止输入，旧失败和退出观察更正按原证据保留。

## 用户验收与截图中的额外发现

用户先后提供齿轮打开、浅色/绿色、红色、关闭重开后的截图；随后按四项清单回复**“全部通过了”**并提供14张截图。按用户声明，22aa候选的以下主题人工检查登记通过：

1. 跟随Windows系统明暗变化。
2. 自定义黑白强调色的操作与选中态辨认。
3. 创作、设计、图库、编辑、资产、账户、更新页的主题外观。
4. 独立批次窗口随主窗口主题变化。

系统变化事件主要依用户实际操作后的明确声明；截图13/14另显示独立批次窗口浅色/深色。静态截图不是自动事件追踪。齿轮、保存与重开效果的先前确认也保留，不要求用户重做已确认项目。

**另列THEME-TEXT-ACTION-CONTRAST：**用户第4张图`codex-clipboard-b8079402-db48-4547-b17c-13d65022f1ed.png`实际显示，FFFFFF强调色在浅色弹窗中使“应用颜色”“恢复Studio默认”“关闭”成为白字白底。完整保留用户四项PASS声明，同时单列这一可见残余，不把“全部通过”扩成每个像素或整项目验收。

48eed2a仅将该对话框的重新读取/应用/恢复/关闭四个TextButton统一使用现有selectionInk作为前景，仍保持原始primary/RGB填充、序列化和点击回调。没有全局替换Material primary，也没有扩改其他TextButton消费者。新1个方法渲染实际生产文字按钮，核对两模式×黑白强调色下的可读前景像素、4个原点击回调和原RGB不变；矩阵/像素数不另算测试。**该额外增量只自动验证，未将用户22aa验收冒签到新48eed包。**

## 实际执行和证据

Windows x64、现有JDK17/Gradle9.1，offline/strict、低优先级、单worker/in-process；任务临时384MiB堆/256MiB元空间、测试256MiB，未改项目/OS默认。两个独立执行都经过H/C预算、提交余量和实际owner检查，监测错误0、未观察到外部重型owner，不重复旧四项/三项或全量基线。

|执行|实际结果|源码/范围|
|---|---|---|
|appearance-gear-fix-20261026|2唯一方法PASS，0失败/错误/跳过，sourceStable；runner78.426秒|22aa8c5；生产/测试实际编译并生成应用映像|
|隔离NSIS安装|exit0；550文件252,882,082字节，2242编译类和6更新脚本匹配|22aa实际包；manifest SHA `6bafd7460ea04f36e239bf38018ee5cf85e69da173ca1fd8e575daac2086f0ee`|
|appearance-text-actions-20261026|1唯一方法PASS，0失败/错误/跳过，sourceStable；runner72.422秒|48eed2a；新生产/测试编译与应用映像；无新NSIS/原生运行|

三方法分属两个源码/运行，不相加成最新源码一次“3项端到端通过”。22aa齿轮代码与48eed未变；原回归结果按固定来源保留。[22aa逐例](evidence/APPEARANCE_GEAR_20261026/run/test-cases.json)、[原始日志](evidence/APPEARANCE_GEAR_20261026/run/gradle.log)、[48eed逐例](evidence/APPEARANCE_TEXT_ACTIONS_20261026/run/test-cases.json)、[原始日志](evidence/APPEARANCE_TEXT_ACTIONS_20261026/run/gradle.log)。各control/run脚本和COMMAND记录完整命令；run/control中保留资源样本和监测回执。

本次原生自动截图仍FrameArrived超时，**保留FAILED/根因UNKNOWN**，没有把用户截图当成自动捕获。原始退出watcher中误写的“计划Alt+F4”已由原回执明确更正，实际旧窗口由用户关闭，不回写原字节。只读路径helper曾因PSIsContainer用于.NET DirectoryInfo而循环，仅中断自己的命令会话；未强杀应用或写日常目录。准备阶段的路径拼写/脚本字符串替换问题均在运行前核对后修正，不伪造产品失败或新测试。

原始[22aa证据清单](evidence/APPEARANCE_GEAR_20261026/RAW_MANIFEST.json)45份、[48eed证据清单](evidence/APPEARANCE_TEXT_ACTIONS_20261026/RAW_MANIFEST.json)18份均包含清单自身。来源映射分开Git原字节与CRLF工作字节。用户图片像素、workspace/vault/真实凭据未入公开仓库；包/原始图片仅本地。原始日志空白即使触发格式检查也保留，SHA/Git验证不排除。

本次提交准备实际有4份原始日志触发空白检查：两证据组各自`control/runner.log`和`run/gradle.log`的Gradle行尾空格，runner另有结尾空行。只精确排除这4份的空白格式检查，原日志不改；全部63份证据文件仍核对manifest/Git字节。该准备拒绝不记作JUnit失败或追加测试。

## 保持的停止点

- 用户本轮主题四项人工PASS仅对应22aa候选；48eed额外文字修正为源码/应用映像自动PASS，用户/新安装包/Project独立审查均未进行。
- 原Project c39/013aab4的P2 CLOSED_WITH_SCOPE仍是其固定范围，不自动扩签新入口/22aa/48eed或整项目。没有新外部送审。
- 原q09 FAILED/root UNKNOWN、资产管理与编辑的**功能**人工未验、115体验及Prompt比较分别PENDING。主题外观检查不等于资产/编辑功能验收。
- 在线更新故障SlowRetrievalError属于该离线internal候选的既有loopback/过期合成公开root配置；本轮不修改它或冒充正式升级验收。日常115和受保护111/备份未动。
- PRODUCT_HOLD / NOT RC；本轮停止于固定修复/自动结果/用户声明及证据。没有因用户“全部通过”自动发布、安装替换、清理、启动Desktop或后续性能/功能开发。
