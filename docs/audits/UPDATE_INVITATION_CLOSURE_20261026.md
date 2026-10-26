# 已完成课题确认及更新链包级补证

2026-10-26 Asia/Tokyo。用户本次直接要求确认已完成课题，优先完成更新相关问题，再准备 Pre-release 邀请测试。承接原 UX-RECOVERY 第3步及 UPD-CLEAN-PROTECTION-001，不重开主题、既有人工或后三组新功能。本次补证实际完成；没有新的生产代码修复，没有发布或替换日常安装。

## 已完成与接受范围

|课题|固定依据|准确状态|
|---|---|---|
|本轮性能工程段|[阶段收尾](../../handoff/PERFORMANCE_STAGE_CLOSURE_20261025.md)|本工程段完成；长期 PERF-001～007、大规模 UI/RSS 与用户体验不是全验收|
|主题与齿轮|[22aa 原主题人工及命中修复](APPEARANCE_GEAR_AND_USER_ACCEPTANCE_20261026.md)、[48eed 三按钮人工](APPEARANCE_TEXT_CANDIDATE_20261026.md)|按各自包和用户回复限定完成，不再索要同组重测|
|深色黑强调色 Tokens|[26ef 自动及用户截图](TOKEN_CANDIDATE_20261026.md)|FIXED / AUTO_PASS / USER_SUPPLIED_SCREENSHOT_VISUAL_PASS_WITH_SCOPE|
|六项更新及任务安全修复|[2484858 原始返工与原生证据](../../handoff/PROJECT_REVIEW_RESIDUALS_20261025.md)|既有源码/自动/独立裁定分别保留；不重跑18 JVM/56 Python来增加计数|
|下载引用终态 P2|[f6598d3 / 54a5ba26 原 Project 裁定](PROJECT_DOWNLOAD_TERMINAL_CLOSURE_20261025.md)|CLOSED_WITH_SCOPE；本轮补当前包脚本身份和原生清理执行，不重新宣称全项目通过|
|程序及工作区清理机制|[原保留规则](../rc/OWNED_ARTIFACT_LIFECYCLE.md)|机制已存在；本轮补保护冲突组合及成功后立即退役证据。受保护或未知历史文件仍保留|
|发布入口与 Desktop 技术参考|[原发布连续性](../rc/PREVIEW_UPDATE_CONTINUITY.md)、[原报告](../../handoff/DESKTOP_UPDATE_TECHNICAL_HANDOFF_20261025.md)|原交付保留；本轮公开入口只读复核通过，Desktop 不启动开发|

## 固定源码、包和真实执行

- 产品源码固定 `9839bce20ccdf4460e0aa42befcb070c1a6a235f`，应用映像实际构建 `0f11fdd6b6d281987dd7c449d623a74b8b74b8f0`；`modules/tools` 对 Tokens 代码 `26ef38857be510636dffdb5fcbd59a8d70c85550` 差异为空。本轮生产源码差异为空；新增回归文件另列，不将未提交文件冒充固定生产源码。
- 本轮新建 **内部演练包** `0.2.2-internal`，168,998,701 字节，SHA-256 `0bd761c36ef35cba4e12dc469eb08656d9963123da7ad8936d20fde94fcf321f`。NSIS 实际成功，复用已有真实应用映像，未执行 Gradle、没有新的 JUnit。这个包没有公开 release/tag。
- 550 个安装清单成员逐文件匹配，6 个包内更新脚本与源码一致。主 JAR SHA-256 `04381d42afc15e50db1354b32736c1107a7d5df6733e2c6ca57105817653b7e0`，与用户已看过的 Tokens 候选相同。旧用户候选安装包 SHA `ff8f868a…a5f71a3d` 未变。
- [完整源码／构建／包映射](evidence/UPDATE_INVITATION_CLOSURE_20261026/SOURCE_PACKAGE_MAPPING.json)。新包仅使用既有回环测试工具的内存临时签名身份；正式根、密钥、元数据、版本和渠道没有修改。原过期离线测试根不能验证新在线升级，因此没有拿旧离线候选冒充本次成功。
- Windows 原生 A 由固定 test-only `ReviewUpdateNativeA` 调用实际生产 Workbench、维护排水、handoff 与 close，真实 NSIS 安装及正常生产 Main 的 B 完成首次帧／资料／凭据确认。测试 probe JAR SHA `d81a414a…bb3e563` 与原2484858一致，probe 源码未变，不需要重编译它。

## 本轮新增自动结果（不累加旧测试）

|层次|实际执行及结果|证据|
|---|---|---|
|当前已安装包的保护组合|3个唯一 Python 方法，7.707秒，失败/错误/跳过均0；实际包内决策函数及 Windows 文件句柄/目录联接|[逐例日志](evidence/UPDATE_INVITATION_CLOSURE_20261026/protection/tests.log)、[结果和6脚本散列](evidence/UPDATE_INVITATION_CLOSURE_20261026/protection/RESULT.json)|
|公开通道只读检查|当前包客户端配合只读115基线的正式固定根，真实 TUF 校验返回 UP_TO_DATE /115，3.601秒；未下载安装器|[通道结果](evidence/UPDATE_INVITATION_CLOSURE_20261026/public/PUBLIC_CHANNEL_CHECK.json)|
|最新生产代码原生 A→B|1个新包集成情形 PASS；实际慢响应排水、只发送1条、A退出0、B验证3项合成资产与2项合成DPAPI凭据、完成后立即清理、B正常关闭|[原始总结](evidence/UPDATE_INVITATION_CLOSURE_20261026/native/NATIVE_UPDATE_SUMMARY.json)、[A输入](evidence/UPDATE_INVITATION_CLOSURE_20261026/native/A_INPUT_MAP.json)、[确认及快照](evidence/UPDATE_INVITATION_CLOSURE_20261026/native/A_TO_B_RESULT.json)|
|公开下载入口复核|实际最新公开版本115，仍 Pre-release；主库默认分支README链接实际115发布页、全部版本和 Assets/windows-x64.exe，警示Source code；旧库迁移链接存在|[只读公开回执](evidence/UPDATE_INVITATION_CLOSURE_20261026/postcheck/RELEASE_ENTRY_CHECK.json)|

3个新增方法覆盖：保护111＋115占用时整体保留、释放后重试；保护111＋注入权限拒绝/删除中断/重入幂等；损坏保护记录/路径穿越/真实NTFS联接拒绝。资料、备份、当前116、受保护111和未知文件前后散列一致。权限拒绝为边界注入，不宣称真实ACL改动或所有杀毒软件场景已测。

原生演练证明生产发送门先关闭，慢请求返回的图片完整入库，后续请求没有发送；更新器在 A 正常退出后安装 B，快照匹配后才提交成功。`COMPLETE` 后旧程序所属文件和下载立即移除，未知哨兵保留，不等待7天、不要求额外冷启动。[清理断言](evidence/UPDATE_INVITATION_CLOSURE_20261026/native/CLEANUP_ASSERTIONS.json)、[清理事件](evidence/UPDATE_INVITATION_CLOSURE_20261026/cleanup/events.jsonl)、[B正常退出](evidence/UPDATE_INVITATION_CLOSURE_20261026/native/B_EXIT_OBSERVATION.json)。本次是新内部 A/B，不是原封公开115应用的人机点击升级；也没有在真实服务发送请求。

## 失败、资源及临时产物

首次准备因显式隔离 PATH 缺 Git，在构建输入读取阶段失败，包未生成、原生应用启动0；[失败原日志](evidence/UPDATE_INVITATION_CLOSURE_20261026/preparation-failure/package-process.log)保留。只把 Git/Windows PowerShell 的已知程序目录补入隔离 PATH，在新夹具中执行最终演练；不继承用户私有配置，不覆盖首次失败或计算为额外通过。

最终总耗时122.863秒，122次资源样本，最低物理可用471,060,480字节，H盘最低1,285,644,288字节，测试server正常退出0。无Gradle，与当时已有固定应用/运行时复用；磁盘预算1,787,564,193字节，物理估算896MiB，不是实测峰值或以后通用阈值。[准入](evidence/UPDATE_INVITATION_CLOSURE_20261026/native-control/RESOURCE_ADMISSION.json)、[实际监测](evidence/UPDATE_INVITATION_CLOSURE_20261026/native-control/RUN_RESULT.json)。未动Desktop进程或配置。

成功后先外部冻结证据并保留一份精确内部安装器供复核，再通过既有 OWNER／封存清单机制退役**本次新建**的隔离大产物：1211文件、676,146,465字节。日志、清单及固定安装器保留；[退役回执](evidence/UPDATE_INVITATION_CLOSURE_20261026/postcheck/OWNED_LAB_RETIREMENT.json)。没有顺手清旧失败目录、历史基线、111、日常安装、资料或备份。首次准备失败现场保留，不追补OWNER接管旧目录。

重现入口：

```powershell
# 仅全新隔离输出目录；lib必须是明确的隔离安装包，不能指向日常资料。
& H:/CCB-Studio-Suite/.local/windows-rc/updater-runtime-115/python.exe -B `
  H:/CCB-Studio-Suite/tools/update-runtime/test_protection_combinations.py `
  --lib H:/CCB-Studio-Suite/.local/token-candidate-20261026/isolated-install/updater/lib `
  --out H:/CCB-Studio-Suite/.local/<全新合成输出目录>
```

原生复现脚手架和实际命令位于证据`native-control/`；其中固定路径仅是当次输入，不执行历史命令去覆盖已封存目录。需创建新 OWNER 根、检查资源、配置只回环测试仓库并改到新输出根。测试新建合成凭据，不读取私人凭据；不导出DPAPI密文、私钥或用户图片到此证据包。

## 还没有完成的事，以及邀请测试边界

1. **正式邀请候选的发行交付尚未执行。**公开最新仍是`v0.0.0-preview.115`（安装器SHA `356e749b…47862dc`），没有包含本轮后续修复。不能将当前内部0.2.2包、历史115或本轮自测称为“新Preview已发布”。后续沿现有流程固定实际新Preview包身份，核对正式渠道发现/下载、发布材料和首页链接；不复用115 tag或覆盖旧附件。本轮没有创建新公开版本、变更正式更新元数据或更新日常安装。
2. 历史111仍受活动安装记录保护；新版程序与未知/受保护历史目录的处理分开。自动清理不能撤销该保护，真实记录迁移及精确历史清理尚未执行，也不作为“目录旧了就删”的例外。
3. 新内部包的独立复审 **NOT REQUESTED**；本轮自动证据不是 Project 接受。原限定关闭结论不扩大。若需要新增Project裁定，交付此固定映射和raw，由其独立判断。
4. 用户115体验、Prompt对比各 **PENDING**；资产管理和编辑完整功能 **USER_NOT_TESTED**；跨机器恢复、真实付费生成、更多Windows环境没有本轮新验证。q09历史FAILED/root UNKNOWN、用户否决的旧布局/性能方案、原生WinError32和下载P2旧失败均保留。
5. 邀请测试按已有实验性Preview范围，明确生图/图库核心、编辑等未验及无Authenticode，不取消Pre-release、不称稳定版或全项目验收。后三组新功能、Tag库及Desktop开发不混入更新收尾。

这份补证完成更新机制当前包的工程验证，不新增产品实现，不等于已完成正式发布或整项目RC验收。下一具体动作是按上述边界准备正式邀请候选的发行核验。
