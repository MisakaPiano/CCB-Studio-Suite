# Tokens修复独立候选：构建、隔离安装及入口证据

## 后续用户截图：Tokens显示视觉核验通过

用户随后提供`codex-clipboard-37a58c40-ce9e-41f2-a6bd-3049cd82da58.png`。实际截图中，深色界面和黑色强调色选中态下，左下角“正向Tokens1/512”“负向Tokens152/512”均为亮色且清楚可读。本项更新为 **FIXED / AUTO_PASS / USER_SUPPLIED_SCREENSHOT_VISUAL_PASS_WITH_SCOPE**；用户本次仅发截图，没有新增“整套测试通过”的文字声明，不扩张为全主题或项目验收。

[独立保存的截图观察及哈希](evidence/TOKEN_USER_VISUAL_20261026/USER_VISUAL_RESULT.json)、[用户启动回执](evidence/TOKEN_USER_VISUAL_20261026/USER_LAUNCH_RECEIPT.json)、[4文件清单](evidence/TOKEN_USER_VISUAL_20261026/RAW_MANIFEST.json)。20:14:36 JST真实READY入口记录明确指定本候选的合成workspace/runtime、secondary/pause参数及隔离环境，主JAR散列与已交付包一致；随后任务runtime/updates存在，隔离profile默认回退及task launch-current均未创建。workspace此前已由合成seed创建，不拿其存在冒充原生启动证明。

本次只读核对任务入口记录/程序散列/目录存在性及用户已提供图像，新增构建0、JUnit0、执行者UI输入/启动/关闭/重开0；未公开图像像素、workspace或私有数据。未自动截图/操纵用户窗口，新包独立审查仍NOT REQUESTED；其他未验/旧失败与115/Prompt各PENDING保持。下文nativeAppStarts=0和用户未验是交付当时的固定历史；原50份证据未回写，本次新增4份另存。

2026-10-26 Asia/Tokyo。用户09:46 UTC批准独立候选及自动验证，沿用UX-DISPLAY-THEME-001 / THEME-TOKENS-CONTRAST；只打包既有Tokens窄修，不混入Tag库或其他需求。没有新公开版本、tag、外部送审、日常115替换、私人图库/现用配置/凭据访问、清理或安全设置变更。

## 固定对应关系

|对象|实际身份与结果|
|---|---|
|应用窄修代码|[26ef38857be510636dffdb5fcbd59a8d70c85550](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/26ef38857be510636dffdb5fcbd59a8d70c85550)：正常Tokens文字使用现有可读前景；原强调色填充、警告/超限、计数/阈值/请求保持|
|实际构建和打包HEAD|[0f11fdd6b6d281987dd7c449d623a74b8b74b8f0](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/0f11fdd6b6d281987dd7c449d623a74b8b74b8f0)，相对26ef的modules/tools差异为空，仅文档/证据后代；配置sourceSha和appBuildSourceSha都明确为0f11，不伪写成26ef新编译|
|实际内部安装包|既有0.2.2-internal / internal / version2标识；**168,998,707字节**；SHA-256 **`ff8f868a5dbadaa4bcce1970e0cc98aab73c30f8c1bd0555e19e3d3ea5f71a3d`**，无新公开tag|
|主JAR|studio-desktop-c2d9f69ad95e163c1c361e6cc59ee.jar；SHA-256 `04381d42afc15e50db1354b32736c1107a7d5df6733e2c6ca57105817653b7e0`|
|隔离安装|550文件、252,882,518字节；manifest SHA-256 `f9724173b58dc1758943c68251052a2407c4f5cb89d45d93a0ad84033a7b868a`；实际NSIS安装exit0|
|报告提交|本报告所在Git文档/证据提交；不同于应用代码、构建HEAD、安装包或发行版本|

[完整498输入→源码/包映射](evidence/TOKEN_CANDIDATE_20261026/SOURCE_PACKAGE_MAP.json)、[实际构建摘要](evidence/TOKEN_CANDIDATE_20261026/run/summary.json)、[安装包回执](evidence/TOKEN_CANDIDATE_20261026/package/BUILD_RECEIPT.json)、[实际安装核验](evidence/TOKEN_CANDIDATE_20261026/control/ISOLATED_INSTALL_VERIFICATION.json)。输入逐项保存Git和工作字节SHA；只允许UTF-8文本的CRLF/LF差异。

## 自动执行与未重复的旧回归

Windows x64、现有JDK17/Gradle9.1，offline/strict、低优先级、单worker、Kotlin进程内。任务临时384MiB堆/256MiB元空间，未改项目或系统设置。只调用`:studio-desktop:createDistributable`，42.605秒、exit0、sourceStable。已有26ef编译/JAR合法UP-TO-DATE复用；实际重新生成应用映像。Gradle日志的配置检查SKIPPED不是JUnit测试跳过。本次**新增JUnit0**，不能把构建成功写成执行了新的测试。

固定26ef的[旧定向回归1方法PASS](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/0f11fdd6b6d281987dd7c449d623a74b8b74b8f0/docs/audits/evidence/TOKEN_TEXT_CONTRAST_20261026/run/test-cases.json)及24个实际生产底栏离屏场景仍按原来源保留，没有重跑或叠加计数。该方法曾核验文字像素≥4.5、精确计数/警告、进度语义及原RGB填充；此处不代签新包Windows窗口。

沿用Build-InternalInstaller.py生成新目录NSIS包，使用原已验证runtime和既有合成公开root。NSIS exit0；原有缺少FileDescription/LegalCopyright的2警告保留，非零警告。隔离安装使用`/S /D=<新任务目录/isolated-install>`，不写系统快捷方式/注册表/默认入口；对安装脚本确认无相关动作。

核验全部550安装项、326运行时文件、6个更新脚本，以及4模块全部2242生产class。每个项目JAR的class集合与实际main编译目录一致，无额外类或新测试类。Tokens消费者DesktopStudioResultControlsKt.class与当前编译字节相同，SHA `4a78ddeee24d43473e6b299c58e3ef1014651232cb99068bdcacbe302c4c3690`，不同于旧48eed的`1697285c82d37d0cf68638d7b8a2c3bf9029a1b8b23758c89f007fc26d3fedf5`，证实装入修复。

[实际构建命令](evidence/TOKEN_CANDIDATE_20261026/control/COMMAND.json)、[打包命令](evidence/TOKEN_CANDIDATE_20261026/control/PACKAGE_COMMAND.json)、[安装命令](evidence/TOKEN_CANDIDATE_20261026/control/ISOLATED_INSTALL_COMMAND.json)、原始[Gradle](evidence/TOKEN_CANDIDATE_20261026/run/gradle.log)/[NSIS](evidence/TOKEN_CANDIDATE_20261026/package/compile.log)输出和重现脚本均保留。

## 重型时段与资源

最初只读核对/轻量准备，不凭重型进程空列表抢跑。协调确认Desktop实际NOT_READY / WAIT_ACK、其入口语法失败发生在jpackage前，接受等待后，[明确授予Studio时段](evidence/TOKEN_CANDIDATE_20261026/control/SLOT_GRANTED.json)。没有结束/中断Desktop程序，也没有重启另一个构建回合。

预算按实际app215,143,868字节、updater37,620,624字节、旧安装包及main类规模计算：两个app缓冲＋完整payload和隔离安装＋旧包×1.2＋main类双份＋256MiB余量，H预算1,447,291,365、C预算805,306,368字节；不复制与本次无关的整个历史test/build树，不依赖删除旧候选。不是实测峰值保证，也没有未经实测的2GiB空闲内存硬门。

启动前H3,491,663,872、C12,397,899,776、Available1,128,656,896字节，提交余量约7.31GB、foreign0。应用映像阶段11次采样，监测异常0、foreign均空；最低Available466,898,944，本组采样private峰值737,554,432字节，未见OOM/预设连续严重压力。打包阶段仅单次资源观察，其时编译器已不在进程列表；原记录的“during NSIS”是阶段标签，不能当活跃编译峰值。

**19:08:53 JST重型时段已明确释放**：[退出码/产物/资源回执](evidence/TOKEN_CANDIDATE_20261026/control/SLOT_RELEASED.json)。当时所有构建/打包/安装退出均0、无重型进程，H2,812,837,888、C12,395,261,952、Available1,154,195,456、提交余量7,329,890,304字节；后续仅轻量文档/哈希整理，Desktop可自行复检继续。本次没有构建失败或再次构建；旧阶段失败、否决、UNKNOWN保持原样。

## 精确隔离入口与人工残余

本地交付根：`H:\CCB-Studio-Suite\.local\token-candidate-20261026`。

- 入口：`Start-Studio-Tokens-Fix-26ef.lnk`；程序在`isolated-install`。
- 合成工作区：`entry-review/workspace`；runtime：`entry-review/runtime`；profile：`entry-review/profile`。
- 已用安装后真实生产类给全新工作区保存并读回DARK / CUSTOM_ACCENT / 000000；断言账户为空，无provider或凭据访问。

入口沿用已证明的`.lnk → PowerShell包装 → 子进程`机制。实际执行同一快捷方式的**无应用探针**，全部6参数、显式studio/runtime根、secondary/pause标记，以及清空继承后的LOCALAPPDATA/APPDATA/HOME/USERPROFILE/TEMP/TMP白名单均核对；即使app忽略参数，默认回退仍只落合成profile。路径拒绝任务根外/重解析点，并固定主JAR/native EXE哈希，没有ExecutionPolicy或安全设置绕过。未创建回退workspace/default launch-current；[探针回执](evidence/TOKEN_CANDIDATE_20261026/entry/PROBE_VERIFIED.json)。READY只表示包文件/真实入口探针通过，**不是APP_VERIFIED**。

**本轮没有启动应用窗口，nativeAppStarts=0，原生启动和用户人工NOT TESTED。**用户只需通过上述新入口打开，查看创作页左下角“正向Tokens”“负向Tokens”在预置深色＋黑色强调色下是否清楚。无需重做48eed已确认的三个弹窗按钮或整套主题清单；旧确认不扩签本新包。

## 保护与停止点

旧48eed安装包SHA仍`ca4cafd37285a2528090247ecc53326b5e78cafa87b569d040578639d8c10ee5`、主JAR仍`1154b2cf6f961cba3d0d20b9846f9ada72703adb4d73bb55c2759301ce15fa19`，未覆盖旧payload/安装和证据。日常115、私人图库/现用配置/凭据、备份没有访问或修改，未清理文件、未添加Tag库。

离线合成更新源继续127.0.0.1:1及既有过期公开测试root（SHA `efc8fdda742b6cc5f7fe4edcb38f75eba9bfcbe0210e07eb888d25f76c3eb892`），没有签名/密钥/TUF/协议变更；不拿它验证正式更新，SlowRetrievalError旧限制保持。

50份新[原始证据清单](evidence/TOKEN_CANDIDATE_20261026/RAW_MANIFEST.json)与旧证据分开；不公开二进制、快捷方式二进制、用户图片像素、workspace/profile/vault、真实凭据或私人资料。原日志保留其空白：格式检查使用`core.whitespace=cr-at-eol`接纳原始CRLF，仅精确排除本组`control/runner.log`、`run/gradle.log`的原始Gradle行尾空格/结尾空行。其余文件格式检查保持，全部50份的SHA/Git字节核验不排除这两份；不格式化raw来求绿。

无新Project送审，独立审查NOT REQUESTED；q09 FAILED/root UNKNOWN、资产/编辑功能USER_NOT_TESTED、115体验/Prompt对比各PENDING、PRODUCT_HOLD / NOT RC不变。本次交付独立候选及自动证据后停止，不自动启动用户窗口、发布、更新日常安装或继续其他工程。
