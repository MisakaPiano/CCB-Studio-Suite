# Preview118 固定候选：真实117升级及旧根退役

2026-10-27 Asia/Tokyo。承接 UPD-CLEAN-PROTECTION-001 与更新清理任务。**READY_FOR_USER_CONFIRMATION_NOT_PUBLISHED**。本轮只准备候选及必要升级核验；没有创建118的tag/Release、签发正式元数据或更新日常安装。公开最新版仍为实验性邀请 Preview117；本报告不是稳定版、全项目验收或 Project 独立复审结论。

## 固定身份

| 对象 | 固定值 |
|---|---|
| 安装包/最终清理脚本源码 | [a1202e24f7a0cba8a53f43bf035b67e2134ca2aa](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/a1202e24f7a0cba8a53f43bf035b67e2134ca2aa) |
| 应用映像构建输入 | [3d0bd1f02c630d1e312d7469dbd71f29efe0bec1](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/3d0bd1f02c630d1e312d7469dbd71f29efe0bec1)，包含9f5a6a6的Java入口兼容及Kotlin helper工作目录修正 |
| 本轮空目录规则 | [862f2a8ada442c68d15d44c1f793865d96e72dcf](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/862f2a8ada442c68d15d44c1f793865d96e72dcf)；随后a1202e2补清理进程自身定位 |
| 候选版本/渠道 | `0.0.0-preview.118` / `preview`；tag **未创建** |
| 最终安装器大小 | **169,006,742 bytes** |
| 最终安装器 SHA-256 | **3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d** |
| 正式信任根 | `408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a`，保持原字节 |
| 旧117安装器 | `bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9`；原公开包，未改应用/配置/更新器源码 |

本机最终包：`H:\CCB-Studio-Suite\.local\preview-candidate-118-20261027\candidate-r3\studio-installer.exe`。保留同级 `BUILD_RECEIPT.json`、`release-inputs` 和 `PAYLOAD_RETIRED.json`。冗余解包副本已退役；后续发布脚本不能假定旧payload仍存在，应从这个固定安装器新建隔离安装。没有用旧包散列冒充新实现。

[固定源码/包输入映射](evidence/PREVIEW_118_CANDIDATE_20261027/SOURCE_AND_PACKAGE_MAPPING.json)区分应用、更新脚本、原生launcher和文档。旧[9f5a6a6薄宿主报告](UPDATE_DIRECTORY_RETIREMENT_20261027.md)保持不变，不追改它的验证边界。

## 本轮实际补齐的两处缺口

真实117→首份118已通过程序/资料/凭据确认，但旧程序根剩下 `NVIDIA Corporation/umdlogs` 两层空目录。用户本轮确认允许在获准退役的程序根内受控移除空子目录，随后实现：

- 仅程序所属收据通过既有成功、保护和退休条件后启用；下载清理及工作区通用清理不因此扩大范围。
- 先枚举、后非递归删目录；发现任何未受保护的剩余文件，保留其未知目录结构，不读取、散列或删除未知文件内容。受保护的data、配置、插件、源码、媒体、凭据和备份目录不遍历、不移除，即使为空。
- 拒绝重解析点；最多4096目录、25000目录项、64层，超限或访问失败安全延后。最终仍由同句柄目录删除判空；检查后新出现文件会令删除被拒，文件保留。

第二份候选清完这些空子目录后，旧根仍为空且共享冲突32。进一步确认：**旧117 helper启动B的清理进程也没有cwd**。它运行的是B的新脚本，但自己继承A目录；两路短重试结束后才释放该目录，错过自动收尾。

最终a1202e2在 `residue_cleanup.main` 最早调用 `anchor_packaged_worker`：同时核对脚本位于所选包的updater/lib、解释器位于同一包的updater，拒绝重解析路径，然后将**该清理进程自己**的cwd切到B。不修改别的进程，不强杀，不提前释放回滚保护；仓库源码维护入口或不匹配解释器不执行此切换。原更新COMPLETE门槛、程序/图片/凭据校验、入口CAS与正式信任链均不改。

源码入口：`tools/update-runtime/owned_files.py`、`residue_cleanup.py`；本轮新增 `test_empty_program_directories.py`、`test_worker_directory.py`。9f5a6a6的GUI启动前重新进入机制及未来helper显式cwd继续保留。

## 最终真实升级结果

环境：本机Windows x64、同一Windows账户、C盘NTFS，JDK17.0.20.1、Python3.13.16、NSIS3.13。用户明确授权的 `C:\CCB-Studio-Suite-Test-118-20261027` 内完成；独立USERPROFILE/APPDATA/TEMP、合成资料和合成凭据。没有真实provider调用或读取日常图库/vault。

**重要验证边界：**118尚未发布，正式TUF/GitHub通道没有该目标。因此隔离驱动只替换旧helper进程内的 `create_updater`、`authorize_download`，提供本机固定SHA-256的候选TargetFile；没有更改任何磁盘上的117程序、旧helper、配置或信任根，也没有创建测试签名私钥。旧helper的inspect/版本检查、真实父进程句柄/等待退出、安装器不可变句柄与散列校验、真实NSIS、550项安装核验、B的实际资料/凭据ACK、入口切换和清理均实际执行。**这证明真实安装/重启/清理，不是118公开签名发现和下载已经通过。**原117界面中的更新按钮未在本次自动点击，其安装前用户操作由隔离驱动衔接。

最终仅一条固定最终包场景计PASS：

1. 从原公开117安装器真实安装，启动生产Main；两张合成原图、两个实际账户引用的DPAPI合成凭据保存在独立工作区。
2. 等117启动检查释放缓存锁后，新交接绑定真实117窗口进程，旧helper进入READY_TO_DRAIN；正常关闭117，未强制解锁或终止。
3. 旧helper安装最终118，生产Main完成资料格式、原图/配方、账户及凭据状态确认，交接 **COMPLETE**；安装器未修改资料。
4. **118窗口与进程仍运行时，整个旧117根已自动消失**，不是只删除文件或手动补删。553所属旧程序文件＋2下载文件自动退役；下载目录和`.installing`目录均为0。
5. 使用B自带清理器重复执行：`COMPLETE, removed=0, deferred=0`，交接回执字节不变；图库原件、两个合成凭据、备份哨兵保留。
6. B正常关闭后从新默认入口重开118，资料锁和相同资料根正确，再正常关闭。桌面/开始菜单重建及任务栏人工固定不在本次签收范围。

[最终逐例结果](evidence/PREVIEW_118_CANDIDATE_20261027/integration-r3/RESULT.json)、[B运行时观察](evidence/PREVIEW_118_CANDIDATE_20261027/integration-r3/LIVE_B_RESULT.json)、[原始交接](evidence/PREVIEW_118_CANDIDATE_20261027/integration-r3/RAW_UPDATE_RESULT.json)、[清理日志](evidence/PREVIEW_118_CANDIDATE_20261027/integration-r3/cleanup-events.jsonl)、[重复清理](evidence/PREVIEW_118_CANDIDATE_20261027/integration-r3/repeat-cleanup.log)可逐项核对。日志仍位于应用所选runtime的 `updates/cleanup/events.jsonl`，失败清理不会改写更新成功/失败。

## 自动测试、构建和失败分列

- 新增 **9个唯一单元方法PASS**：空目录6项（未知文件、保护路径、真实junction、创建竞态、界限等）＋worker定位3项（身份不符不切换、junction拒绝、进程存活时释放旧cwd）。worker单位例身份字符串有合成替身，实际打包行为由上述完整原生升级补验。
- 另4个既有目录收尾方法复核PASS，不算新增；先前的7项兼容、3类搬迁、6项9f5a6a6验证及用户主题人工不重测、不累计。
- 应用映像离线构建47.905秒，sourceStable；**JUnit执行数0，编译不算测试**。两个后续改动均只涉及Python，复用逐字节核对的相同Compose映像，分别记录appBuildSource与包组装source。最终真实NSIS核验550文件；生产JAR内含入口兼容类，没有测试driver/seed类。更新运行时按既有锁定的第三方manifest逐项校验后重组，未下载新依赖。
- 本轮完整原生升级结果为 **2次旧根要求失败＋1次最终包通过**，不能写成3次通过。第一份118 `7016b314…2533ccb`遗留未登记空目录；第二份 `0d0dd864…3bac4210`遗留worker占用的空根。原固定来源、包散列、原始失败、补充诊断和源码均保留。
- 两次准备失败另列：本地TargetFile适配参数顺序错误，尚未进入安装；另一次117启动检查占缓存锁，旧helper写FAILED_RETAINED，未安装。等待锁后新建nonce/交接，原失败不重写。最初失败驱动结束后其B已不在，退出原因UNKNOWN，没有追认为正常退出；后续检查及最终场景的正常关闭分别留证。执行者没有发出强杀。

原始命令、测试输出、源码执行字节、编译输入/包清单和全部失败见[原始清单](evidence/PREVIEW_118_CANDIDATE_20261027/RAW_MANIFEST.json)。`drivers/integrate-r3.py.txt`等是**本轮审计复现材料，不是日常维护命令**；先新建明确授权的隔离根并核对当前状态，不能直接重放已经退役的路径。

## 产物清理与剩余边界

[退役回执](evidence/PREVIEW_118_CANDIDATE_20261027/FIXTURE_RETIREMENT.json)：合计 **3174文件／1,411,745,656字节** 的本轮测试/重复产物已留证退役，C盘授权根已不存在。两个失败候选的临时二进制和合成资料随授权测试根退役，失败源码/散列/日志仍保留；最终固定安装器和release-inputs保留。没有动历史复审包、116兼容基线、日常117、受保护111或真实备份。

C测试配置产生的3个Windows INetCache junction在留证时被安全拒绝；改为只记录链接本身，核对目标仍在授权根内后精确非递归解除，没有遍历目标。此为本轮明确授权的自有夹具清理，**不放宽更新器对未知junction的拒绝规则**。一次构建副本清理只剩未登记的空app/resources目录，另行列明并完成非递归退役，未掩盖为首次即完成。

未知/自改文件、受保护内容、持续占用/权限失败仍可能保留旧根，这是保护边界；不会为“目录必须消失”删除这些内容。未验证所有Windows版本、网络盘、跨用户/机器凭据迁移或当前包全部异常组合；沿用原路径长度限制和安全延后策略，不宣称所有环境可靠。

## 等待确认

下一动作：用户确认**上述3540be44…1e8bd60d固定候选**后，才沿原流程创建预发布并核验公开117→118发现/签名/下载链。当前正式元数据仍为ad7ed68，公开117 tag不变，118 tag不存在；本轮不自动发布或更新日常安装。

资产管理/编辑完整人工验收、115体验、Prompt对比及任务栏人工固定继续各待验；q09 FAILED/root UNKNOWN、旧否决和旧失败不变。新包独立审查 **NOT REQUESTED**，不把Codex自检当作Project验收。不启动性能、界面重做、Tag或Desktop开发。
