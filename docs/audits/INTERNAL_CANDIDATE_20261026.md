# 独立Studio内部候选包与隔离验证

2026-10-26 Asia/Tokyo。用户20:59 UTC批准原阶段的独立候选包、资源预算、包完整性和隔离启动；经原协调会话01a16f0a-b53e-7062-a303-f975e672a0db附直接人类答复“同意”（Sentinel_4689eef7e13081918bbdd751cd232b70）接续。只覆盖此内部候选，不授权公开发布、替换日常115/回滚、私人资料、真实服务、信任变更、外部送审或后三项功能。没有生产代码修改。

## 可使用的本地交付

- 推荐入口：`H:\CCB-Studio-Suite\.local\candidate-review-20261026\Start-Studio-Candidate.lnk`。明确指定本轮工作区/runtime，含`--secondary-workspace --pause-collections`，不接管默认入口；快捷方式目标和参数回读一致，快捷方式点击本身未另测、未重开用户关闭的窗口。
- 说明：`.local/candidate-review-20261026/delivery/README.zh-CN.txt`。
- 备存内部安装包：`.local/candidate-review-20261026/delivery/CCB-Studio-Suite-review-80ea116-windows-x64.exe`；**168,996,001字节**，SHA-256 **`2db2f03f655aefff13103b3f63a603b346a6091c98c491af10c6def43521eaef`**。
- 实际隔离安装：`.local/candidate-review-20261026/isolated-install`。不要直接双击其中EXE或用安装包覆盖日常目录；默认EXE缺少隔离参数时会走普通启动路径。仅使用上面本地入口。

## 冻结对象与构建

源码/本次构建基点均为 **`80ea1167cdc197db30980ed116ee6d30731c994a`**；modules/tools与P2固定源码`c39e860252fdd96c9bec5b5d42d1abd988d3ec06`无差异，并包含下载终态修复`f6598d39298c06ec5644a131e484e814f77b6f45`。后续报告提交仅固定本轮证据，不当作另一份应用构建。

内部包标识 **0.2.2-internal / internal**；使用既有内部构建流程的本地标识，以完整sourceSha区分，不是新公开Preview版本。没有Release、tag、公开版本号/更新元数据或签名操作。[495项构建输入、Git/工作字节及安装包映射](evidence/INTERNAL_CANDIDATE_20261026/control/FIXED_SOURCE_PACKAGE_MAP.json)区分EXACT和CRLF_LF_ONLY，不把文字换行归一化当作原字节相同。

Windows x64、现有JDK17/Gradle9.1、单worker/in-process、低优先级、offline/strict。使用已有Run-PreviewChecks运行`:studio-desktop:createDistributable`，任务临时覆盖Gradle堆384MiB/元空间256MiB/CPU2，不改项目或系统默认。实际46.258秒、exit0/sourceStable、**COMMAND_SUCCESS / JUnit0**。15项未变任务UP-TO-DATE，映像任务实际执行；没有为了“新包”强制重编或重复旧回归，也不说本次生产源码重新编译。后续安装中的2,241个类逐一等于当前编译输出；原P2生产编译与三项通过来源仍为013aab4固定证据。

原始[构建摘要](evidence/INTERNAL_CANDIDATE_20261026/app-build/summary.json)、[完整日志](evidence/INTERNAL_CANDIDATE_20261026/app-build/gradle.log)、[执行脚本](evidence/INTERNAL_CANDIDATE_20261026/control/run-build.py)与[资源监测](evidence/INTERNAL_CANDIDATE_20261026/control/MONITOR_RESULT.json)保留。build-only的Gradle任务SKIPPED/UP-TO-DATE不转换成测试PASS。

## 资源事实与边界

启动H空闲6,633,996,288字节，C空闲12,920,848,384字节；H预算3,630,748,092来自实测既有build输出及估算的3份512MiB应用、2份220MiB压缩包、2份64MiB updater和512MiB余量，非实测未来峰值。系统提交余量约6.53GiB；该任务采用3GiB的提交余量估算准入，不沿用无实测依据的2GiB物理空闲门槛，不建立新通用最低内存要求。

构建13次样本、监测错误0，最低Available658,915,328字节，任务组私有提交采样峰683,380,736字节、工作集558,530,560字节。该构建窗口未观察到外部重型owner；Desktop活动状态另行只读核对，没有给Desktop发送指令或写其仓库。采样非连续峰值，不能证明所有时刻/整个后续打包阶段零重叠。raw PLAN继承的testForks/896MiB文字是监测模板残留，本次无JUnit/test worker，实际门槛与执行参数见脚本及EVIDENCE_BOUNDARIES，不回写原记录。没有OS分页/防护/ACL更改、强杀或腾空间清理。

## 新包完整性与隔离安装

Prepare-UpdaterRuntime只从已锁定本地包构建新runtime；Build-InternalInstaller默认internal，新目录拒绝覆盖，**未使用consume-app-image**，没有移动旧发行副本。NSIS归档工具按既有锁核验；生成脚本无WriteReg/CreateShortCut/ExecWait/ExecShell等系统入口指令。实际使用`/S /D=<本轮新isolated-install>`，退出0；未运行卸载器。

[安装核验](evidence/INTERNAL_CANDIDATE_20261026/control/ISOLATED_INSTALL_VERIFICATION.json)：550清单文件、252,879,034字节；manifest SHA-256 `153caa9044eda38e23e20640af75575bfd5ea57aa19e516f8479e5d23e0578e6`。安装身份与bootstrap散列可加载，全部runtime清单、四模块2,241个编译类及6份生产更新脚本匹配。用户正常关闭后再次完整核验相同550项。安装包与交付副本SHA相同。没有把“源码包含修复”直接当作“实际包包含修复”。

bootstrap只复用已有历史**合成公开**root，SHA `efc8fdda742b6cc5f7fe4edcb38f75eba9bfcbe0210e07eb888d25f76c3eb892`，已过期；端点为`http://127.0.0.1:1/metadata/`和`targets/`。没有密钥读取、创建、续签或正式信任变化。此候选不提供在线更新；启动检查可能显示离线错误，不冒充完整公开升级链已测。原正式/日常115不因该内部包改变。

## UPD-CLEAN-PROTECTION-001：原待执行用例的包内合成验证

本轮直接导入实际隔离安装中的`updater/lib/residue_cleanup.py`及依赖，用新建假程序字节和JSON模拟协议；Windows原生锁/删除原语实际执行，**没有真实115→候选升级、实际ACTIVE记录迁移或历史清理**。原静态登记保留历史；现新增3个唯一合成情形，重复清理不累加测试数：

|新情形|实际结果|
|---|---|
|默认115、成功111→115且ACTIVE仍保护111|111、原保护记录、数据/备份字节保持，两次删除均0|
|完整115→116成功记录并有所属清单，111仍受保护|仅115已登记程序文件退役；未知note保留，111/新程序/资料/备份保持；重复删除0|
|nonce不匹配；再构造相对路径损坏保护记录|前者删除0保留115，后者明确拒绝，旧程序未损|

[逐例原始结果](evidence/INTERNAL_CANDIDATE_20261026/control/PROTECTION_CASES.json)、[摘要](evidence/INTERNAL_CANDIDATE_20261026/control/PROTECTION_SUMMARY.json)、[可复现脚本](evidence/INTERNAL_CANDIDATE_20261026/control/protection-integration.py)。3PASS/0失败，不计为新JUnit或沿用旧24/56等总数。独立审查NOT REQUESTED；真实111是否解除保护仍需独立人工审定，不是发现即删或自动迁移默认记录。本轮没有重跑重解析点、文件占用等完整故障矩阵，不以此关闭整个保护任务。

## 实际启动、用户截图与关闭

用新工作区/runtime及子进程环境白名单启动**包内生产EXE**，普通Workbench路径，`--secondary-workspace --pause-collections`；不是旧`--synthetic-root`演示界面。没有账户/凭据、私人图库导入或真实provider调用；目录采集暂停。窗口实际返回“CCB Studio Suite · 工作台”。

自动截图首次FrameArrived超时，按技能重新定位并激活后仍window capture超时；文字读取仅窗口/窗格。**自动画面验证失败/不可得，根因UNKNOWN**，不说自动显示PASS。用户随后直接提供截图，能看到创作工具、当前手动批次空栏、预览和设置齿轮；并明确“我给你截图了”“然后我已经正常关闭了”。只确认本次默认页面实际显示和正常关闭，不把它扩大为全部主题、选中态、窗口交互或编辑/资产人工验收。用户图片未复制进公开证据。

两个自身进程23044/18308均观察到exit0，之后本路径进程0。stderr/stdout为空；没有强制结束。收到“别”后没有再关或重开。**没有发送Alt+F4或点击关闭**；[原始退出观察](evidence/INTERNAL_CANDIDATE_20261026/startup/EXIT_OBSERVATION.json)中的normalCloseRequestedBy是提前写在watcher里的意图文本，不能证明执行；[明确更正及用户确认](evidence/INTERNAL_CANDIDATE_20261026/startup/USER_CONFIRMATION.json)说明实际由用户关闭，原始错误字段保留，不回写消除。

## 当前状态

|层次|状态|
|---|---|
|独立内部包构建/隔离安装完整性|实际通过|
|包内旧保护用例3合成情形|实际通过；非真实安装记录迁移|
|本次生产默认页面显示/正常关闭|用户截图/反馈；两个进程exit0|
|自动截图|两次失败；根因UNKNOWN保留|
|主题源码P2独立复审|原c39/013aab4 CLOSED_WITH_SCOPE，不能代签本新包|
|新包Project独立审查|NOT REQUESTED；未外发请求|
|全套主题/系统事件/独立窗口及资产、编辑界面用户验收|仍未完成|
|真实公开A→本包更新、日常安装/回滚|本轮NOT TESTED / NOT TOUCHED|
|q09|历史FAILED / root UNKNOWN；不重跑或抹去|
|115体验、Prompt比较|分别PENDING|
|全项目|PRODUCT_HOLD / NOT RC|

新任务仅既有内部候选阶段，没有开启后三组或后续性能测量。未知/失败/备份/日常资料和旧固定基线保持。候选、安装包、隔离安装及新合成场景为这次可审阅交付保留；本轮没有删除历史目录或用户数据。

原始证据[RAW_MANIFEST](evidence/INTERNAL_CANDIDATE_20261026/RAW_MANIFEST.json)固定47份文件（46清单成员加清单本身）；不包含EXE/JAR、用户图片、workspace/vault或无关进程内容。安装产物本地保留，上述完整散列可复核。

提交准备的格式检查对原始app-build/gradle.log及control/runner.log报告既有行尾空白（runner另有结尾空行）。原字节保留，只将这两份原始日志排除空白格式检查；SHA-256、manifest与Git原字节验证不排除它们。这是准备检查拒绝，不是新增产品测试失败，也没有为通过格式检查修改原始日志。
