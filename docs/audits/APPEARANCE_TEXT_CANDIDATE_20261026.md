# 显示主题文字修正：独立内部候选与限定人工回执

2026-10-26 Asia/Tokyo。沿用 UX-DISPLAY-THEME-001 和 UX-RECOVERY 内部候选阶段。用户22:06 UTC批准把48eed文字修正放入新的独立候选，只复测残余文字按钮。此次新增打包、隔离安装、实际入口和用户回执证据；没有新生产源码、Gradle/JUnit执行、公开Release/tag、日常安装替换、清理或新Project送审。旧阶段报告的“未打包/未人工”保留其当时事实，本报告记录随后新授权结果。

## 固定身份与入口

|对象|固定身份|
|---|---|
|实际应用编译源码|[48eed2aa35d1b2bcaa86f140ac33aac9cdc286ac](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/48eed2aa35d1b2bcaa86f140ac33aac9cdc286ac)|
|打包配置及工具来源|[b54a036fc65868836d901339f7d1e3dce24dff8a](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/b54a036fc65868836d901339f7d1e3dce24dff8a)；相对48eed的modules/tools差异为空，是文档后代，不能混写为新应用编译|
|输入与应用映像|[旧固定497输入及应用映像映射](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/b54a036fc65868836d901339f7d1e3dce24dff8a/docs/audits/evidence/APPEARANCE_TEXT_ACTIONS_20261026/SOURCE_APP_IMAGE_MAP.json)，本轮合法复用、没有重跑旧基线|
|主JAR|studio-desktop-a667c3e9a048c3d6292aa2a6ab3d8a9.jar；SHA-256 `1154b2cf6f961cba3d0d20b9846f9ada72703adb4d73bb55c2759301ce15fa19`|
|新内部安装包|0.2.2-internal / internal / version=2；168,998,452字节；SHA-256 **`ca4cafd37285a2528090247ecc53326b5e78cafa87b569d040578639d8c10ee5`**|
|实际隔离安装|550文件、252,882,226字节；清单SHA-256 `e53dce9faf914c580f371992ed412d28e7c0018e1feaf469b205d39ff4c3ebff`；2242编译类、6更新脚本、326运行时文件核对|
|报告/证据提交|本报告所属Git提交；仅文档/合成控制证据，不是应用版本或公开发布|

完整[源码/包映射](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/SOURCE_PACKAGE_MAP.json)、[构建回执](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/package/BUILD_RECEIPT.json)、[逐项隔离安装核验](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/control/ISOLATED_INSTALL_VERIFICATION.json)。通用native EXE哈希不证明代码版本，必须同时核JAR、配置和清单。

本地安装包：`H:\CCB-Studio-Suite\.local\appearance-text-actions-20261026\candidate\studio-installer.exe`。本地入口：同根 `Start-Studio-Text-Fix-48eed.lnk`；隔离安装 `isolated-install`，工作区 `entry-review/workspace`，runtime `entry-review/runtime`。仅任务内快捷方式，无桌面/开始菜单或系统注册改动；不要用裸EXE代替受控入口。

## 工程验证与保留失败

Windows x64、现有NSIS3.13、JDK17和Python更新运行时。启动前H空闲4,842,803,200、C空闲12,459,122,688字节，Available2,126,393,344、提交余量7,825,174,528、foreign heavy0；本轮估计H1.5GiB/C256MiB，无新Gradle。命令在[PACKAGE_COMMAND](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/control/PACKAGE_COMMAND.json)与[ISOLATED_INSTALL_COMMAND](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/control/ISOLATED_INSTALL_COMMAND.json)，实现可复核工具为既有Build-InternalInstaller.py；安装exit0。NSIS两项缺少FileDescription/LegalCopyright的warning保留，不称零警告。

使用安装后真实生产类给全新合成工作区预置LIGHT/FFFFFF；没有账户/provider/私人凭据。首辅助Java编译漏传Kotlin默认回调参数，**编译失败且在工作区创建/应用启动前**；失败源码、原log和补参数后的成功log各自保留。只改辅助程序，没有产品修复或JUnit失败/追加测试。成功seed exit0，随后workspace已存在，不能把该目录存在当应用正确选根的证明。

同一个真实`.lnk → PowerShell包装 → 探针/应用`入口，先PROBE后APP；探针核6参数及清空继承后白名单环境，显式根、secondary、pause-collections和整个profile均落任务根；路径拒绝越界/重解析点，无ExecutionPolicy绕过。随后主/子进程5916/24940实际命令行均含6参数，runtime新出现，隔离profile默认回退和任务launch-current均未创建。[实际进程观察](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/entry/APP_IDENTITY_OBSERVATION.json)保留原时间点及当时“尚未人工”字段；[后续入口门](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/entry/APP_VERIFIED.json)仅代表入口隔离通过，不是整个产品验收。REVIEW模式开放给用户后续重开，未再次自动启动。

本次native截图仍 **FrameArrived timed out / FAILED / 根因UNKNOWN**，没有送出自动齿轮或按钮点击；用户截图不是工具自动截图。旧80ea非隔离启动原因UNKNOWN、旧22aa候选、所有旧失败/否决及原证据均保留。

## 用户限定通过与新的可见残余

[人工回执与截图哈希](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/control/MANUAL_TEXT_ACTION_RESULT.json)：用户先答“稍后检查”，随后明确纠正误点并继续检查；没有因等待自动推定通过。用户提供两模式×黑白强调色的8张图，再对以下问题明确答 **“都正常”**，后补默认蓝色弹窗及关闭后的2张图：

- 四种组合“应用颜色”清楚可读且生效。
- “关闭”可用，关闭后齿轮能重新打开。
- “恢复 Studio 默认”生效。

据此将48eed新候选的这三项登记为 **USER_REPORTED_PASS_WITH_SCOPE**。这不同于之前22aa的四项主题人工通过，不重跑/累计整套验收。错误状态专用“重新读取”仅由[原48eed单个自动方法](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/b54a036fc65868836d901339f7d1e3dce24dff8a/docs/audits/evidence/APPEARANCE_TEXT_ACTIONS_20261026/run/test-cases.json)覆盖其生产组件/像素/回调，native错误分支NOT TESTED，不要求用户制造损坏。这个方法曾实际PASS，0失败/错误/跳过，本打包阶段新增JUnit **0**。

**THEME-TOKENS-CONTRAST — OPEN / NOT FIXED：**截图82ac显示深色＋000000强调色下，创作页正向/负向Tokens文字非常暗。它在弹窗之外，48eed只改四个弹窗文字按钮，没有修复该消费者；单列在原UX-DISPLAY-THEME-001，不新建开发任务，也不把用户三按钮通过扩成无残余。后续窄修范围尚未开始，无新源码/测试/包。

## 安全与停止点

离线候选继续复用既有过期合成公开root（SHA `efc8fdda742b6cc5f7fe4edcb38f75eba9bfcbe0210e07eb888d25f76c3eb892`）与127.0.0.1:1地址；没有签名/密钥/信任/更新协议变动，不冒充正式Preview更新。SlowRetrievalError不属于此次已通过范围。日常115指针观察SHA保持 `783eb43528a486ff97a08a7581108fc2bd7add8cc2d1464f3e8effdcd84b5df2`，未更新日常115、旧保护111或备份。

34份新[原始证据清单](evidence/APPEARANCE_TEXT_CANDIDATE_20261026/RAW_MANIFEST.json)与旧45/18份独立分开；旧raw不回写。仅复制命名的合成控制记录，不公开二进制、用户图片像素、workspace/vault、真实凭据或私有资料；原log字节保持。准备时默认空白检查将原始CRLF的CR判作行尾空白；没有改写raw来求绿，随后单次使用`git -c core.whitespace=cr-at-eol diff --cached --check`明确允许原始CRLF，0问题，其余空白规则保持。所有34份仍逐项哈希/字节核对，旧63份与固定Git逐字节一致；这不是产品测试或额外JUnit。

原Project c39/013aab4的P2 CLOSED_WITH_SCOPE不扩签本候选，独立审查 **NOT REQUESTED**。q09 FAILED/root UNKNOWN、资产/编辑功能USER_NOT_TESTED、115体验及Prompt对比各PENDING，**PRODUCT_HOLD / NOT RC**。不自动送审、发版、更新日常安装、清理、修改Desktop或启动后续功能。
