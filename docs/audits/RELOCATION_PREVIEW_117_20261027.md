# Preview117：实际搬迁验证与固定候选

2026-10-27，Asia/Tokyo。承接原更新清理任务／UPD-CLEAN-PROTECTION-001。用户要求纳入71bac6f，并明确同盘改名、同盘移动和跨盘移动的真实验证不能用原7项隔离回归替代。本报告不是全项目验收，也没有公开发布117或更新日常安装。

## 固定身份

| 对象 | 固定值 |
| --- | --- |
| 原清理增量 | 71bac6ff5ff7d5a204908773de1f5a46ef3caeea，已包含于下列源码 |
| 搬迁实现 | f0813bdf84bc240579a5691339d0d8dace79ea61 |
| 隔离原生测试宿主／实际应用映像构建 | c601d50a0b57de765353c6f0e600ddc2e9807738 |
| 最终组装及生产源码 | **b9b240a248d428ec158c341b8c127969fd54cb64** |
| 候选版本 | **0.0.0-preview.117**，preview，尚未创建tag／Release |
| 安装包 | `.local/relocation-117-20261027/candidate-preview-117/studio-installer.exe`，169,002,371字节 |
| SHA-256 | **bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9** |
| 原生入口编译输入SHA-256 | eaa0fcc496e2fa760f6af1c4b337ace89127db4cc65dd694e9c5d7733f1020fe |
| 正式TUF根 | 408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a，未改变 |
| 当前公开版本 | 读取GitHub发布列表确认仍为116，原附件及正式元数据未修改 |

候选、payload和发行材料保留在Suite本地上述目录。拟发布文件名为`CCB-Studio-Suite-0.0.0-preview.117-windows-x64.exe`。源码、应用映像、原生入口及最终安装包分别记录来源，不能混为一个构建。固定[输入映射](evidence/RELOCATION_PREVIEW_117_20261027/PREVIEW_INPUT_MAPPING.json)、[Windows执行字节／Git blob](evidence/RELOCATION_PREVIEW_117_20261027/SOURCE_MAPPING.json)和[正式包核验](evidence/RELOCATION_PREVIEW_117_20261027/FORMAL_CANDIDATE_VERIFICATION.json)可复核对应关系。

## 实现与边界

- [Launcher.cs固定源码](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/b9b240a248d428ec158c341b8c127969fd54cb64/tools/windows-launcher/Launcher.cs)：稳定入口在同一字节范围锁下重新读取描述符。搬迁时只接受新根中的同名、同版本程序，并核对配置、完成标记、清单身份、入口字节及可执行文件散列。内部资料和runtime必须实际存在；外部绝对资料路径保持原值，缺失时拒绝，不新建空资料冒充恢复。原子更新入口并保留最多16项旧根映射；联接／重解析、重叠根及无法证明的身份拒绝。
- [residue_cleanup.py固定源码](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/b9b240a248d428ec158c341b8c127969fd54cb64/tools/update-runtime/residue_cleanup.py)：只在当前根进行旧根路径的词法投影，不访问旧盘的对应位置；旧plan、result、journal、所属收据及活动保护记录原字节保留。完成证明、散列、文件占用保护和同句柄删除仍生效。无可验证COMPLETE不会释放旧程序；未知及自改文件保留。71bac6f处理的其他历史cache身份仍按其原保留策略处理。
- [apply_update.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/b9b240a248d428ec158c341b8c127969fd54cb64/tools/update-runtime/apply_update.py)和[DesktopEntrypoints.kt](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/b9b240a248d428ec158c341b8c127969fd54cb64/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/DesktopEntrypoints.kt)在默认入口更新时保留搬迁历史，不恢复已解决的“恢复副本夺取默认入口”行为。
- 原生快捷方式重建只允许原入口或已确认历史根中具有本应用身份的入口；其他同名文件保留。子进程不再次获取父进程已持有的入口锁。
- [Build-InternalInstaller.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/b9b240a248d428ec158c341b8c127969fd54cb64/tools/Build-InternalInstaller.py)将复用的Compose映像与本轮单独编译的原生入口分开记录。内测包与正式候选有547项精确相同；配置、根及再次编译的原生入口3项不同。原生入口源码／编译器相同不等于PE字节相同，所以正式117另做实际入口搬迁核验，没有伪造二进制等价。

## 实际执行与结果

环境为本机Windows、同一用户、C/H两个不同NTFS卷。C盘隔离根由用户准确批准；没有用SUBST或联接伪装跨盘。所有测试数据与凭据均为合成，没有真实生成服务调用，没有读取日常图库／私人凭据。进程只经正常关闭退出，无强杀。

| 层次 | 实际结果与限制 |
| --- | --- |
| 原71bac6f的7项回归 | 原结果复用，不重跑累计，也不作为下面真实搬迁的替代 |
| 新搬迁清理保护回归 | 5个唯一方法PASS；覆盖完成／未完成、保护映射、未知／改动文件及无效历史。实际命令和逐例日志保留，不是原生应用搬迁 |
| 原生入口受控检查 | f0813bd阶段入口的路径投影、外部资料、缺失资料、改动入口与联接5种检查；另有b9b240a最终入口锁内重建及外来快捷方式拒绝2种检查。前5种不冒充最终PE重测；最终PE的真实Studio结果在下面分列 |
| 三种完整真实场景 | **同盘改名、同盘换父目录、H→C真实跨盘，3种全部PASS**。每种搬迁前后均打开真实窗口并核对实际资料／runtime及工作区锁；随后真实原生A排水退出、可信下载、NSIS安装、B确认结果／资产／合成凭据、旧所属程序及下载清理、重复清理0删除、正常退出 |
| 正式117包 | **550项真实NSIS隔离安装PASS**；使用生产Main、未带测试驱动。首次启动及连续同盘改名、同盘换父目录、C→H跨盘后的启动共4次PASS，原图哨兵和资料根保留。该连续包核验不累计成另一组三种完整更新场景 |
| 公开117更新链／日常升级 | 尚未发布，因此NOT TESTED；116日常安装没有修改 |
| 用户／独立验收 | 本轮是Codex工程验证，不是用户完整验收或Project独立审查；独立审查NOT REQUESTED |

完整更新链中的A是明确标记的隔离应用映像：通过独立测试JAR驱动原生进程，其普通启动委托生产`launchStudio`，更新流程复用既有`ReviewUpdateNativeA`。**不是未经修改的公开116旧二进制**。每例包含3个合成资产、2项合成凭据，不把重复数据累计成更大覆盖。正式117的生产入口单独实际验证，没有测试JAR。

补足资料验证深度：上面的首轮正式包搬迁使用初始化文档、资料锁及exports哨兵。随后保持**同一个bec5f787…fb0089c9安装包**，在搬迁前经生产`StudioWorkspace`／`SecretVault` API存入两张合成PNG及两项合成DPAPI凭据，再执行相同三种连续搬迁。每次实际生产GUI正常关闭后，按预先固定的资产ID／凭据引用及SHA-256，经生产API读回并解密校验，4个入口阶段均通过；明文只在内存，不导出vault或图像字节。[有内容资料的固定结果](evidence/RELOCATION_PREVIEW_117_20261027/postcheck/populated/POPULATED_FORMAL_VERIFICATION.json)与Java测试源／逐次material日志另列，不将相同三类再累计为六类。该补验首次改名遇到临时WinError5，保留现场并待占用释放后接续；这一首次改名没有保留整目录前快照，不补写“全目录逐字节比较”，实际核对了已固定的程序清单、两张原图及两项凭据；之后两次移动有整目录逐字节比较。

复核入口：

- [三种真实场景最终结果](evidence/RELOCATION_PREVIEW_117_20261027/NATIVE_CASES_RESULT_FINAL.json)，各例`native-r3/<场景>/REAL_MOVE.json`、`RESULT.json`、`RAW_UPDATE_RESULT.json`及完整前后文件散列。
- [正式候选安装和连续搬迁](evidence/RELOCATION_PREVIEW_117_20261027/FORMAL_CANDIDATE_VERIFICATION.json)，`formal-package/*-RESULT.json`及实际进程／入口参数、资料锁和正常退出记录。
- [新增5项逐例](evidence/RELOCATION_PREVIEW_117_20261027/NEW_SAFETY_RESULT.json)与`new-safety-tests.log`；[锁内快捷方式检查](evidence/RELOCATION_PREVIEW_117_20261027/SHORTCUT_LOCK_RESULT.json)。原7项仍在原[71bac6f报告](LEGACY_CACHE_CLEANUP_FIX_20261026.md)。
- `build/*/summary.json`和`gradle.log`为实际应用映像／测试宿主编译，JUnit执行数0；`PREVIEW_PACKAGE_COMMAND.json`和包内`BUILD_RECEIPT.json`为实际打包命令。原生逐例驱动、准备命令及故障续接脚本原文均保留在证据目录，历史脚本是证据，不是可盲目重执行的当前指令。

源码回归复现需新指定隔离根，显式选取`test_relocation.RelocationCleanupTests`中5个`test_relocat*`方法；不要使用继承类无过滤发现而重跑旧回归。真实原生实验需要新根／新loopback仓库和测试包，不能把保留脚本中的已退役绝对路径直接重用。

## 保留的失败与限制

没有把中途失败改记PASS，也没有为失败而删除保护记录或放宽安全检查：

1. 首次夹具在未初始化资料根放入哨兵，实际触发FOREIGN_ROOT；用户关闭提示。保留原日志／失败事实；新夹具经生产初始化后才在允许的exports内放入合成哨兵，未改应用保护逻辑。
2. 较长测试路径触发NSIS退出19、FAILED_RETAINED，资料未由安装器修改。入口／搬迁步骤当时已经通过，但完整更新失败仍保留。后用新短路径运行，未禁用路径检查。当前包最长成员84个UTF-16单位，带随机暂存名的更新根预算约115单位；不是支持任意长路径。
3. 跨盘测试工具`shutil.move`已复制目标，但删除只读jpackage入口时被拒。源和目标均保留并逐文件核验；后用既有Windows同句柄所属删除安全完成源退役，再接续尚未执行的更新，没有重跑已通过的前两例。
4. 空目录退役断言、续接脚本漏变量、包复用时未先分离updater、初始包映射误以为原生PE字节确定，以及首次工具参数错误均保留原记录。驱动错误与生产应用错误分列；通过项不为这些收尾错误重新累计。

有内容补验的首次改名短暂被占用，未更改权限或强杀进程；原路径安全保留，后续正常改名成功。不能将正常关闭解释为Windows任何第三方占用都已立即释放。

搬迁后应先从新位置的`CCBStudioLauncher.exe`进入；外部快捷方式可通过现有入口重建。**原桌面／开始菜单／任务栏快捷方式自动发现新位置没有通过验证；实际用户任务栏重新固定也未验**，不能宣传为自动修复Windows固定项。没有测试换账户／跨机器、网络盘或全部Windows环境。外置资料不由本机制搬运。超过16项历史或根身份不明确时安全拒绝，不删除未知内容。

资产／编辑完整人工验收、115体验、Prompt对比继续分别PENDING；q09旧失败及UNKNOWN根因、旧用户否决和其他未完成状态不变。主题原人工确认不重做。

## 产物清理和下一动作

本轮已退役成功原生场景与正式包搬迁夹具；原失败的程序副本、两份内部安装器和合成资料也在完整保留失败记录／散列后，按用户对新C盘测试根的授权逐文件安全退役。未导出任何vault或图像原件字节。[最终范围](evidence/RELOCATION_PREVIEW_117_20261027/FINAL_SCOPE.json)及`retirement/*-MANIFEST.json`／`*-RESULT.json`可核对；C盘批准根已移除，固定117候选及所有证据保留。原日常安装、111保护、备份、旧固定基线和旧审查包不动。

**状态：READY_FOR_USER_CONFIRMATION_NOT_PUBLISHED。**沿用户此前“候选就绪后确认再发布”的顺序，下一步确认该固定117候选后才创建Pre-release及正式签名元数据，再做公开发现／下载链验证；本轮没有替换116附件、改变信任链或更新日常安装。
