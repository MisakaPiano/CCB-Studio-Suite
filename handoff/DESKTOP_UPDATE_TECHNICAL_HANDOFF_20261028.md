> 2026-10-28 后续补充：最新公开Preview119，组装／tag d9be9d9、映像e012e2e、包SHA256 b425ae408e3613acac2a0000ead5f04cd02c9831a63d4b288682f5e2f75f1c83。新增编辑与内置模型；更新器6脚本、安装器生成器及Launcher.cs相对118的a1202e2无源码差异。119新增原版118公开下载／真实NSIS／生产Main／合成凭据／旧根自动退役证据，见[119发行回执](../docs/audits/PREVIEW_119_PUBLICATION_20261028.md)。因此下方118是机制固定实现基线，119不是另一套更新方案。Studio结果不代签Desktop验证，仍不授权修改Desktop、复制标识／端点／密钥或信任配置。

# Studio 更新安装方案：Preview118 最新交接，供 Desktop Codex 与 Project 独立判断

2026-10-28，Asia/Tokyo。本文更新[2026-10-25交接](DESKTOP_UPDATE_TECHNICAL_HANDOFF_20261025.md)中已过时的“公开115／内部未发布包”状态；旧固定报告、失败和独立结论保持历史原义。**最新公开应用为实验性 Pre-release 118，公开117→118升级及用户日常升级已留证；不是稳定版、全项目验收或Desktop可直接采用的安装方案。** 本次只发布交接文档，不重建或发布安装器、不修改日常安装或Desktop。

给 Project 的阅读顺序：第1节固定对象 → 第2至6节机制 → 第7节逐层证据与失败 → 第8节Desktop适配边界。不能只看成功摘要而跳过真实旧客户端、独立审查和Windows Installer所有权的区别。

## 1. 固定版本与本次只读公开复核

| 对象 | 固定身份、含义 |
| --- | --- |
| 最新公开Release／tag | [v0.0.0-preview.118](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.118)，Release ID 424811525，仍为Pre-release；2026-10-28重新读发行列表和tag确认 |
| 包组装、更新脚本源码 | [a1202e24f7a0cba8a53f43bf035b67e2134ca2aa](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa)，tag指向此提交 |
| JVM／应用映像实际构建输入 | [3d0bd1f02c630d1e312d7469dbd71f29efe0bec1](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/3d0bd1f02c630d1e312d7469dbd71f29efe0bec1)；之后仅打包所需Python更新脚本变更，映像输入另有逐项映射，不把文档HEAD当构建来源 |
| 安装器 | `CCB-Studio-Suite-0.0.0-preview.118-windows-x64.exe`，169006742 bytes，asset 679593008 |
| 安装器SHA-256 | `3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d`；本次回读GitHub digest一致，**本次没有再次全包下载**，原公开实字节下载证据见下 |
| 发布时元数据 | [9ddec977eab81725fa36c99e2628413118c90958](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/9ddec977eab81725fa36c99e2628413118c90958/preview/windows-x86_64/metadata)，targets17／snapshot26／timestamp26 |
| 本次观察到的续期元数据 | [c1ba1804d0ff40a7891231b5de99667260e5b98a](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/c1ba1804d0ff40a7891231b5de99667260e5b98a/preview/windows-x86_64/metadata)，targets17／snapshot27／timestamp27。续期不等于另发应用，不能仍把26说成当前值 |
| 当前固定元数据到期（UTC） | timestamp：2026-11-03T09:08:34Z；snapshot：2026-11-10T09:08:34Z；targets：2027-01-24T21:55:25Z；root：2027-10-22T10:47:53Z。以后须重新观察，非永久有效承诺 |
| 正式引导根 | root1，SHA-256 `408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a`；118未换根或密钥，本次不做签名、续签或信任配置写入 |
| 首页下载入口 | main文档提交[3db4dda450e0184e0298cd7eb01f9dc781e9cedc](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/3db4dda450e0184e0298cd7eb01f9dc781e9cedc)，只更新README；不是应用源码合并 |
| 118源码／包映射 | [SOURCE_AND_PACKAGE_MAPPING](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ee46f20bccf34fa5268cb6b39e0d180793d9ebb4/docs/audits/evidence/PREVIEW_118_CANDIDATE_20261027/SOURCE_AND_PACKAGE_MAPPING.json)，含Git字节与执行字节／换行区别、安装与运行时清单 |
| 公开链及日常升级固定报告 | [509da09e0940fcbe94ca4f72700df71e1eacac3e](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/509da09e0940fcbe94ca4f72700df71e1eacac3e/docs/audits/PREVIEW_118_PUBLICATION_20261027.md) |
| 历史111退役固定报告 | [89be2250cbf806632a4d6eed149ecdce5a3672b9](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/89be2250cbf806632a4d6eed149ecdce5a3672b9/docs/audits/RETIRE111_WORKSPACE_CLEANUP_20261027.md)，精确人工维护，不是更新器自动解除保护 |

本次公开只读观察原文和角色摘要见[PUBLIC_OBSERVATION](DESKTOP_UPDATE_118_PUBLIC_OBSERVATION_20261028.json)。这是发行列表、tag、当前元数据回读，**没有重新执行TUF签名验证、真实升级或私人资料核验**；那些结论引用其原固定证据。正在开发的编辑／AI／工程管理为本地未发布工作树，不属于118，也不作为本交接的已发布能力。

## 2. 实际布局、构建与目标授权

程序是**并排新目录安装**，不覆盖旧版。概念布局如下，名称仅用于解释，Desktop不可照搬：

```text
<安装父目录>/
  CCBStudioLauncher.exe           稳定入口
  launch-current.json            当前程序、data、runtime及身份
  <version>-<source前12位>/       单个不可变程序目录及随包JRE／更新运行时
  data/workspace/                原图、配方、工程、任务、索引、OS保护凭据
  data/runtime/                  应用运行状态、更新记录和下载缓存
```

data／runtime可有既有显式位置，不能从布局示例推断所有用户均便携式。安装器只输出程序目录，不覆盖资料。Compose/JDK应用映像、原生稳定launcher、受锁定的Python/TUF运行时和NSIS安装器分别有输入／散列；每个安装产生`INSTALL_MANIFEST.json`、`INSTALL_COMPLETE`及卸载器。需要保留包、源码和真实生成文件的对应，而非只记录“构建成功”。[构建入口](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/Build-InternalInstaller.py)、[运行时准备](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/Prepare-UpdaterRuntime.py)。这不宣称位级可复现。

首页“Windows预览版下载”指向实际Preview发布页，用户在Assets下载`windows-x64.exe`，Source code不是安装器。“全部版本”可找旧包；旧Releases库只作迁移指引，旧包／桥接通道保留。更新客户端的授权依据则是**固定根的TUF目标**，不是GitHub `/latest`、README或版本字符串。TUF检查签名、版本、过期和角色散列关联，适配层再检查产品／平台／渠道、递增版本、reader范围、资料格式、sourceSha、认证tag和附件长度／SHA。HTTPS重定向受限；无真实性证明的下载不会进入可安装状态。[发现校验](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/update-runtime/studio_update.py)、[GitHub传输](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/update-runtime/github_transport.py)。

下载写独立incomplete目录，完成校验后才VERIFIED；安装前重新持有安装器句柄、检查同一目标字节，避免校验后替换。空间门槛也在真实下载前检查：原117检查该118包需`169006742×3 + 64MiB = 574129090 bytes`。**没有通过降低门槛、删锁文件或删除资料来强行通过。** 固定TUF更新根不等于Windows Authenticode／SmartScreen发布者签名，后者尚未提供。

## 3. A→B交接和真正的成功确认点

1. A只接受已认证目标，更新缓存独占锁串行化发现／下载／安装／清理。用户确认后固定目标、计划、nonce、当前程序／资料／runtime身份及只用于本地比较的快照。
2. **先禁止新发送，再等待已发送结果回来并保存**，然后关闭写入者和资料锁。暂停／结束也在真正发送接受点原子检查；不丢弃已在途结果。界面关闭或更新不得一边排水一边继续接受新请求。[维护与排水](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioApplication.kt)、[交接](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioUpdateHandoff.kt)。
3. helper绑定真实A进程身份，等待正常退出；不凭PID字符串、不强杀、不通过删锁抢占。旧程序在这时仍受回滚保护。
4. NSIS安装到全新暂存／最终版本目录，拒绝不安全或过长路径，核验安装清单。安装器退出0和文件齐全仅是`INSTALLED_NOT_STARTED`，**尚非更新完成**。[安装实现](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/update-runtime/install_ops.py)。
5. 真正的B打开指定资料根，核验程序身份、nonce、格式、已提交状态、原图／配方引用及当前和排队冻结凭据状态。失败不以空工作区顶替、不跳过错误凭据。恢复副本只能独立打开，不能夺取默认入口。
6. B成功回执必须为`B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH`。在入口锁内重新读取预期旧描述符，以条件提交切换稳定入口，拒绝旧helper延迟写入、同版不同根和未授权根变更；随后交接才为`COMPLETE`。[默认入口](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/DesktopEntrypoints.kt)、[helper](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/update-runtime/apply_update.py)。
7. 只有上述确认完成、当前默认入口吻合、相关引用终态都可证明后，才运行下节的所属清理；记录清理完成或延后。**清理结果不反向改写更新COMPLETE／FAILED_RETAINED。**

失败或中断保留旧入口、旧程序和源资料；不盲目重放旧计划。需要格式转换时沿现有“新目录副本＋保留原目录”规则，禁止自动数据降级。[格式迁移](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioFormatMigration.kt)。用户明确选择成功后立即清理，`ROLLBACK_SECONDS=0`：**成功前保护A；成功后所属A可退役，不保留七天，也不把旧程序当长期回滚副本。** 既有备份和迁移源目录不因此删除；回退须重新判断程序与资料兼容。

## 4. 118补齐的旧helper／工作目录兼容

实际缺陷不是“删除清单少了一个文件”：A、旧helper、jpackage原生父进程或清理解释器继承A目录作为cwd，导致Windows即使已删完文件仍持有旧根。单改未来helper不解决已经发布117的第一次升级。

- 新helper启动目录改为runtime/handoffs；启动B和清理进程显式指定B cwd。
- B的最早Java入口在打开资料根前识别安装版、明确handoff／runtime、实际原生cwd。需要时用正确cwd重新进入同一程序，保留参数；原宿主等原helper终态后退出。没有伪造ACK或远程改变其他进程。[UpdateLaunchDirectory.java](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/modules/studio-desktop/src/main/java/io/github/misakapiano/ccbstudio/desktop/UpdateLaunchDirectory.java)、[Main.kt](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/Main.kt)。
- 旧helper也会无cwd启动**B自带的清理器**。最终a1202e2在`anchor_packaged_worker`核对解释器和脚本确属B包、拒绝reparse后，将清理器自己定位到B；不改变源码维护调用或其他进程。[residue_cleanup.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/update-runtime/residue_cleanup.py#L355)。
- `prune_empty`不再吞掉删除失败；占用／权限进入`DIRECTORY_DEFERRED`和安全重试。没有采用强杀、只改Java `user.dir`、提前删A或替换旧117附件。

完整原因、两个失败包与最终包分别见[118候选报告](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ee46f20bccf34fa5268cb6b39e0d180793d9ebb4/docs/audits/PREVIEW_118_CANDIDATE_20261027.md)。Desktop若同样采用jpackage父进程，需要核实自身进程树；**这不是MSI操作所有权问题的通用修复**。

## 5. 自动退役：只删有归属和成功证明的程序产物

核心：[residue_cleanup.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/update-runtime/residue_cleanup.py)、[owned_files.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/update-runtime/owned_files.py)。这是受限文件退役器，**不是“新包不存在就删除”的目录同步器或全盘清扫器**。

| 对象／判断 | 已实现规则 |
| --- | --- |
| 当前B、data、runtime、插件、源码、配置、媒体、凭据、备份 | 当前包与这些根保护；ACTIVE_INSTALLATION的program／workspace／runtime／保留备份引用继续生效，不关闭整份保护记录 |
| 废弃程序文件 | 受信任安装时输出收据，或旧合法COMPLETE记录固定的manifest；再次核对产品／版本／source／包散列、准确父目录和每个文件长度／SHA。仅路径名或现存manifest不能追认所有权 |
| 下载／解包／失败输出 | 仅准确登记的安装器、VERIFIED和安装输出；旧下载无收据时也只能从完整合法成功交接接管精确字节。不完整终态、未完成引用、错误nonce／缺journal先保留，之后才筛选是否当前来源；失败产物不能被更早的历史成功误清 |
| 实际删除 | 拒绝绝对／点段／反斜线／盘符／保留设备名等非法相对路径；固定根并拒绝reparse，持有父目录和文件句柄，核验与删除同一句柄，避免路径替换竞态 |
| 空目录 | 用户另行确认的规则：仅获准退役程序根内，在没有未知文件的前提下，受限发现空子目录；保护名称即使为空也不遍历删除。最多4096目录／25000项／64层；最终同句柄判空删除，新出现文件令删除失败 |
| 未知／改动文件 | 保留，不因“不在新包中”而删除；不递归移除包含未知文件的旧目录。根可能因此合理保留，不承诺任何情况下都只剩一个目录 |
| 占用／权限／忙锁 | 写明原因，4次短重试，之后由后续启动重试；不删锁文件、不强杀、不阻止B启动。清理返回0不表示所有文件已删，须看COMPLETE／DEFERRED及逐项日志 |
| 重复执行 | 以收据／状态／终态证明为准；已删除项不重复产生副作用。真实最终118场景重复清理为removed=0／deferred=0 |

日志与记录在**所选runtime**的`updates/cleanup/events.jsonl`、`state.json`、`owned/*.json`，以及相关handoff计划／结果、安装journal、进程日志。它们是后续判定依据，不能为“清缓存”一起抹掉；下载目录是`updates/downloads`。清理器的成功条件不能只看进程退出码。

**111历史残留是单独人工处理。** 它当时被活动记录保护，且NSIS动态生成的卸载器缺独立归属证明，自动机制保留是预期。之后用**原111安装器**隔离重建补证，用户明确授权，在锁内仅把两份保护记录的program字段迁至118，workspace／runtime／备份字段不变，再核对550所属文件退役，111根消失。不能把这次人工维护写成更新器会自动解除所有旧保护；更不能复制“先改保护记录再删目录”的做法到Desktop。[精确回执](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/89be2250cbf806632a4d6eed149ecdce5a3672b9/docs/audits/RETIRE111_WORKSPACE_CLEANUP_20261027.md)。

源码工作区另用[workspace_artifacts.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/89be2250cbf806632a4d6eed149ecdce5a3672b9/tools/workspace_artifacts.py)：先登记OWNER的新合成根，成功后外部封存证据再退役自身payload；失败、固定基线、人工试用资料、备份和不明归属保持。它不由普通用户更新器扫描源码仓库；新机制不能给旧目录补造OWNER来追认删除权。

## 6. 改名／搬迁、资料和凭据边界

117加入的稳定launcher在正常关闭、完整目录改名／移动后，可从**新位置launcher**核对当前程序、资料根及身份，再重定位描述符和受限`previousRoots`历史；清理投影旧路径时不访问旧根，不凭路径历史产生文件所有权。资料缺失、外部资料未搬、身份不符或目录联接时安全拒绝，不创建空库顶替。[原生launcher](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa/tools/windows-launcher/Launcher.cs)、[117固定报告](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/3d4a97f7c6303bf7c81798abbabfdc7057312a36/docs/audits/RELOCATION_PREVIEW_117_20261027.md)。

验证覆盖本机同Windows账户的C／H NTFS卷：同盘改名、换父目录、真实跨盘。包含带合成原图及DPAPI合成凭据的正式包搬迁补验；**不是跨机器／换账户DPAPI迁移**。系统桌面／开始菜单／任务栏快捷方式不会因跨盘移动保证自动追踪，需新位置启动后重建并必要时重新固定；任务栏自动跟随仍未人工验收。NSIS长路径上限仍在，测试中过长安全拒绝被保留。

Studio的普通资料备份排除凭据；更新本机一致性快照与普通备份不是同一功能。资料、媒体、配方、工程和任务不归程序清理器处理；既有备份／迁移源保持。不读取或公开真实凭据、恢复码、密钥和用户图片。本次交接没有再次检查日常私人资料，日常成功结论以原应用ACK及用户确认为限。

## 7. 证据矩阵：实现、测试、用户和独立审查分开

以下只复用冻结证据，**本次没有重跑这些升级或把重复检查累计成新测试**。

| 层次 | 固定证据与边界 |
| --- | --- |
| 早期独立源码审查 | [Project限定关闭回执](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/89be2250cbf806632a4d6eed149ecdce5a3672b9/docs/audits/PROJECT_DOWNLOAD_TERMINAL_CLOSURE_20261025.md)：f6598d3下载引用终态P2为CLOSED_WITH_SCOPE，证据54a5ba26；不能提升为118新包或全项目独立验收 |
| 搬迁合成回归／完整原生场景 | [117报告与原始索引](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/3d4a97f7c6303bf7c81798abbabfdc7057312a36/docs/audits/RELOCATION_PREVIEW_117_20261027.md)：旧7回归不代替3类真实搬迁；完整更新实验A有明确测试驱动，**不是未修改公开116**；正式117生产包的连续搬迁另列 |
| 118单元与构建 | [118候选](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ee46f20bccf34fa5268cb6b39e0d180793d9ebb4/docs/audits/PREVIEW_118_CANDIDATE_20261027.md)：新增9唯一方法（空目录6、worker3），另4受影响既有复核；映像构建JUnit执行0不能称测试。550文件真实NSIS安装清单核验 |
| 真实117→候选118 | [integration-r3原始结果](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ee46f20bccf34fa5268cb6b39e0d180793d9ebb4/docs/audits/evidence/PREVIEW_118_CANDIDATE_20261027/integration-r3/RESULT.json)：原117程序／helper，真实NSIS及B生产Main，2合成原图／2DPAPI合成凭据，B运行时旧117根及下载／installing清空，重复清理0删除；**候选发现／下载在内存适配，不是当时公开TUF已验** |
| 正式公开117→118 | [公开链固定报告](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/509da09e0940fcbe94ca4f72700df71e1eacac3e/docs/audits/PREVIEW_118_PUBLICATION_20261027.md)、[RAW_MANIFEST](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/509da09e0940fcbe94ca4f72700df71e1eacac3e/docs/audits/evidence/PREVIEW_118_PUBLICATION_20261027/RAW_MANIFEST.json)：原117客户端真实签名发现／全包下载／旧helper／NSIS／B；**无候选发现适配**。安装前GUI点击由隔离驱动衔接，不冒充用户点击 |
| 日常用户升级 | 同上报告的DAILY_UPDATE_VERIFICATION／USER_UPDATE_CONFIRMATION：用户GUI117→118并确认118，随后“已是最新”；550项、COMPLETE、资料与runtime定位、118运行时旧117及下载清空。不是执行者读取真实图像或解密私人凭据来独立证明全部内容 |
| 新包独立验收 | 118为NOT REQUESTED；本次不是Project批准。Desktop还必须独立评估并实际验证自己的包和保护方案 |
| 待验／未关闭 | 资产管理完整人工、编辑未覆盖及最新UI问题、115体验、Prompt对比、任务栏人工、跨机器／账户／网络盘、更多Windows环境、模型许可／准确率；q09旧失败与UNKNOWN根因不改 |

**失败不抹去：**118的两份先行候选分别因未知空目录、清理worker旧cwd未满足“旧根消失”；保留对应失败包散列和日志。公开链初次过早断言下载清空而失败，随后对**同一已完成升级**等待收尾、重开、重复清理及核对，未重算新升级；最初B退出原因NOT_INDEPENDENTLY_DIAGNOSED。搬迁的FOREIGN_ROOT夹具、NSIS长路径拒绝、暂时WinError5和首次改名未保留全目录前快照均按原报告限制保留。

日常又出现缓存忙和暂存空间不足：真实门槛生效，未降低检查；缓存占用在清理退出后消失，用户最后确认最新。**当前仍直接显示英文错误，缺少友好的忙状态／所需与剩余空间说明**；没有宣称这些UI已修复，也不因报告需求发新包。[详细失败及续验边界](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/509da09e0940fcbe94ca4f72700df71e1eacac3e/docs/audits/PREVIEW_118_PUBLICATION_20261027.md)。

复核命令应取对应phase的`summary.json`／构建命令和驱动源，但必须改为**新授权隔离根**。典型单元入口是`tools/update-runtime/test_empty_program_directories.py`、`test_worker_directory.py`、`test_relocation.py`的指定新增方法；全套发现会重复继承旧测试，不能据此累加。原始驱动中绝对路径常已退役，不是可以对日常安装执行的维护脚本。完整复现实测还需要原117和固定118包、独立profile/runtime和合成账户，不能以Python单元通过代替真实Windows进程／文件占用／NSIS链。

## 8. 给Desktop的迁移结论和最小顺序

上份报告的Desktop参考基点8d1f46a已经过时。本次只读观察本机Desktop HEAD为`7eb766fec77b70155d81840cb9e8c6d0f7494476`，其Codex正在处理普通安装包／MSI保护与1115冻结包相关工作；**观察不是对其当前未提交改动的固定审计，更不是指示其切换方案**。接手须读Desktop自己的当前授权、源码、安装包和Project结论，不拿本报告代替。

| 类别 | 借鉴内容与前提 |
| --- | --- |
| 可复用设计 | 目标认证和字节固定；下载／安装／真正B确认／入口切换分阶段；先禁新任务再收完在途；恢复副本无默认权威；成功才退役；有归属才删；同句柄核验删除、未知保留、可见延后、原始失败与证据外部封存 |
| 必须按Desktop改造 | **MSI／EXE／portable实际渠道、安装上下文／权限、ProductCode／UpgradeCode、Component／KeyPath／Shortcut／修复／卸载／回滚资源图**。Studio的NSIS并排文件清理不能越过Windows Installer所有权，尤其不可把“字节相同”当作可以删共享MSI组件的许可 |
| 必须按Desktop改造 | Desktop资料根authority／portable／bootstrap、独立凭据根、backup／SaveSlot及reader语义、所有异步服务排水；窗口原生父进程／安装helper／清理worker cwd；稳定入口及系统快捷方式资源关系。必须先核清实际架构，再取最小适用部分 |
| 不可直接复制 | Studio应用ID、版本计数、路径、渠道／仓库端点、TUF根、key ID／私钥、DPAPI配置、凭据引用、桥接历史和reader2/3；不要形成完整应用循环依赖 |
| 不适用／未验证 | Studio PASS不等于Desktop PASS。未证明Desktop的MSI repair/uninstall、跨账户安装、服务／提权、网络盘或多用户共享根可靠；本报告不解决这些差异，不授予任何新权限 |

最小顺序：①读本报告及Desktop当前合同，冻结实际包／安装模式／操作资源图 → ②只列适用设计与缺口，保持现有入口可用 → ③先建立隔离包身份、范围和失败保留证据 → ④按Desktop已授权顺序接入排水、B确认、条件切换 → ⑤最后验证成功后所属退役及移动／修复／卸载／重复执行。真实资料、新信任权、发布、迁移或破坏性操作须核对Desktop自身授权；本消息只共享资料，**不要求立即修改、替换冻结包或发布**。

可给Desktop Project的开场指令：

> 请独立阅读这份2026-10-28的Studio更新安装交接及其固定源码／原始证据，先核对118包身份、最新续期元数据与发布时元数据的区别，再评估哪些设计适用于Desktop当前安装架构。重点检查真实旧客户端升级与合成替身、成功确认与清理终态、MSI操作资源所有权、资料／凭据／备份、失败保留与搬迁边界。不要将Studio或Codex自测视为Desktop验收；保留旧失败与UNKNOWN。请给出适配性／差距、按风险排序的最小实施顺序和Desktop自己的独立验证方案；不要仅凭此报告授予发布或真实资料操作权限。

本次任务顺序已调整为：**先完成此技术共享 → 下一步模型内置及默认资产定位 → 再恢复UI问题整理与修改**。当前编辑候选用户反馈“仍有很多问题”，状态为USER_REVIEW_ISSUES_REPORTED，不能写成编辑验收结束或提前迁移新对话。
