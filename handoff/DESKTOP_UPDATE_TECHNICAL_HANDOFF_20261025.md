# Studio 发布与更新技术交接：供 ChatChatBar-Desktop 独立适配

2026-10-25，Windows 10 Pro 19045 / x64。范围是本轮既有更新、发布入口、六项复审修复及所属残留清理的收尾，不启动 Desktop 开发或发布。

**定向实现及隔离验证已完成，尚未发布。公开应用仍是 Preview115。新源码的 Project 独立复审待定，PRODUCT_HOLD / NOT RC 不变。** 115 命名归档、组织树、任务栏体验反馈及 Prompt 对比继续分别 PENDING。另保留一次额外画布用例失败，不把这份报告写成全项目验收。

## 1. 固定身份与差异入口

| 对象 | 固定身份与实际含义 |
|---|---|
| 本轮最终实现源码、更新器及打包输入 | [248485824e1d6d2201ceedf7d14ff66e251d344d](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/248485824e1d6d2201ceedf7d14ff66e251d344d) |
| 实际 JVM 应用构建及最终 JVM 回归 | `59283a0315d182cd745c9c03c6fe3b802bf8114c`。之后只改两个 Python 文件，408 项应用/原生启动器输入与最终源码相符；逐项记录 Git 原字节、构建输入字节和仅 CRLF/LF 的差异 |
| 内部隔离安装器 | `0.2.2-internal`，168926729 字节，SHA-256 **`2dc31cb8badfe4ed5cb02c8a99523d81dc05f65a874aeb49ac3299048f701238`**；未上传、未新增 tag、不是 Preview116 |
| 内部包实际保留位置 | `H:\CCB-Studio-Suite\.local\review-fixes-cleanup-20261024\delivery\CCB-Studio-Suite-review-2484858-windows-x64.exe`。Desktop 本机可独立复算；云端 Project 尚未取得该 EXE，不能声称其已独立重算二进制 |
| 实际公开 release/tag/源码 | [v0.0.0-preview.115](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.115)，源码 `ff145d051bae286ab3e27cadd41195f8bd4c4e18`，Pre-release 保持 |
| 公开115安装器 | `CCB-Studio-Suite-0.0.0-preview.115-windows-x64.exe`，168890283 字节，SHA-256 **`356e749bd346af4a1d419a0d8b48b1e27d2314e6fce5763fa0933917247862dc`**；本轮未更换 |
| 公开更新元数据 | 主库 `long-term-metadata@60c527f55ced1f8a8e897ce957b49a66c9f83f53`，路径 `preview/windows-x86_64/metadata/`；本轮只读验证四角色签名、长度/散列关联，目标仍为115 |
| 随115固定的公开根 | `1.root.json` SHA-256 `408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a`；本轮没有换根、换密钥或写公开元数据 |
| 主库默认分支下载文档提交 | [810775488b9ecc298b5295dde4e7d04e8fa20b22](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/810775488b9ecc298b5295dde4e7d04e8fa20b22)，仅 README；不是应用源码合并 |
| 旧发行库迁移文档提交 | [e47d47abf5e77a94f9c0c7f71695cc0ad6b35eeb](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/commit/e47d47abf5e77a94f9c0c7f71695cc0ad6b35eeb)，仅 README；旧包、桥接更新和信任链保留 |
| 本报告/新证据的提交 | 与应用源码分开提交，见同目录 `DESKTOP_UPDATE_HANDOFF_RECEIPT_20261025.json` 的 `documentEvidenceCommit`；不把文档提交当成构建来源 |

完整关联见 [FIXED_SOURCE_PACKAGE_MAP.json](../docs/audits/evidence/REVIEW_RESIDUALS_20261025/FIXED_SOURCE_PACKAGE_MAP.json)。[上一轮复审至本轮源码差异](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/da2ae810542c79b0049b74148566a423fb3e1bc3...248485824e1d6d2201ceedf7d14ff66e251d344d)，[115至本轮累计差异](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/ff145d051bae286ab3e27cadd41195f8bd4c4e18...248485824e1d6d2201ceedf7d14ff66e251d344d)。构建不是位级可复现声明，NSIS/JDK/临时根及时间信息仍可能改变包字节。

公开元数据原字节及 release 资产 digest 在 [public/](../docs/audits/evidence/REVIEW_RESIDUALS_20261025/public/PUBLIC_IDENTITY.json)。观察时 root/targets/snapshot/timestamp 分别到期于 2027-10-22、2027-01-22、2026-11-07、2026-10-31（UTC）；这些是该固定元数据的到期时间，不是以后永远有效。内部 TUF 是仅回环测试的内存临时签名者，公钥和四角色元数据保留，私钥不落盘，不能当成公开115的签名链或可部署配置。

## 2. 问题、根因、方案及选择

### 发布入口与 Preview 标识（已公开的文档修正）

原主库首页不足以让用户直接找到 Windows 安装器，旧发行库入口也容易继续被当成新版本入口。两个默认分支 README 顶部现有“Windows 预览版下载”和“全部版本”，明确到 Assets 取 `windows-x64.exe`，Source code ZIP/TAR 不是安装器；标注实验性邀请 Preview、非稳定版以及未完成项。最新 Preview 通过实际发行列表核实，本轮再次回读仍为115。

Releases 区块开启的嵌入页面证据已留存，客户端占位卡片不冒充最终视觉检查。保留 GitHub 对预发布卡片的原行为，没有为显示首页卡片取消 Pre-release。旧仓库 README 指向主库发行列表，保留原正文、旧版本、104桥接和续签通道。后续沿现有流程，在实际发布、签名元数据及公开回读完成后，人工同步主库下载链接并核对最新实际 Preview；本轮没有增加自动化。

依据：[实际文档回执](../docs/audits/PUBLISH_ENTRY_PROJECT_HANDOFF_20261024.md)、[既有发布流程](../docs/rc/PREVIEW_UPDATE_CONTINUITY.md)。这里的成功是入口文档与公开链接验证，不是全应用人工验收。

### 构建与打包（沿用现有实现，本轮补边界）

Compose/JDK17 先产生实际应用 image，再用 hash-lock 的本地 Python/TUF 依赖及 NSIS3.13 组成 Windows 安装器。记录应用构建 commit、完整组装源码 commit、逐输入散列、运行时清单、安装清单、包字节数和 SHA。更新器 Python 单独修正时，可以保留相同应用 image，但必须证明应用输入没有变化；不能只把新 HEAD 写进旧包就宣称重新构建。

安装目标必须是全新目录，程序与数据不能重叠；NSIS 在解包前检查路径长度，包含随机 staging 后缀及最长目录/文件。清理的 Win32 IO 支持扩展长路径，但不据此放宽安装器仍受限的路径。保留构建日志中的两条 NSIS version-info 警告，不将其隐去或写成发布者认证。

固定代码：[Build-InternalInstaller.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/Build-InternalInstaller.py)、[Prepare-UpdaterRuntime.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/Prepare-UpdaterRuntime.py)、`tools/update-runtime/artifacts.json`、`modules/studio-desktop/build.gradle.kts`。放弃覆盖旧目录、重复留下完整应用 image 和不检查输入的“借旧构建”；本轮成功演练副本在外部留证后按所属清单退役，唯一交接 EXE另存。

### 签名、发现及下载（既有信任链保持，本轮只读复核）

已安装程序中的 `update-config.json` 与固定 SHA 的 `update-root.json` 是引导依据；可写下载缓存不能替换根。python-tuf 负责角色签名、版本、到期及元数据关联；适配层再检查 product/platform/channel、严格递增版本、reader范围、输出数据格式、源码SHA、目标长度/散列和已认证的 GitHub tag/asset。

应用更新选择以 TUF 已认证目标为准，不用 GitHub `/latest` 来授权安装器。GitHub 传输只允许固定元数据仓库路径，安装器 CDN 重定向仅有限次数和指定 HTTPS 域；拒绝任意重定向。下载在独立 incomplete 目录，验证后才发布 VERIFIED；安装前再次持有文件句柄核对目标长度和散列，防止检查后被更换。缺空间、下载中断或散列不符不能记成功。

固定代码：[studio_update.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/studio_update.py)、[github_transport.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/github_transport.py)、[install_ops.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/install_ops.py)。发布侧已有 `release_repository.py`、`prepare_release.py`、`maintain_metadata.py`、`renewal-workflow.yml`；本轮没有执行生产签名或续签，没有读取真实密钥托管。TUF 包真实性验证不等于 Windows Authenticode/SmartScreen 发布者认证，后者本轮未解决。

### 安装切换、默认入口及恢复副本（本轮修正）

程序采用并排全新版本目录，不覆盖 A。进程间共享更新缓存锁；helper 绑定 A 的实际 PID/image，等 A 正常排水退出。NSIS退出和文件清单通过仍只是 `INSTALLED_NOT_STARTED`。真正 B 启动后，对程序身份、nonce、已提交状态、资产原字节/引用、凭据快照及资料根/格式作回执比较，才提交固定 launcher 指针并记 COMPLETE。

恢复副本原先可能注册为默认资料入口；修复把恢复根标为独立工作区、使用独立 runtime，禁止它接管默认启动/更新。Project 随后指出 Python helper 虽加了锁却仍无条件写指针，本轮补锁内条件提交：重新读取期望旧描述符、版本、installed/data/runtime；拒绝延迟旧写入、同版本不同根及未确认的资料重定位；只有既有流程已确认的格式迁移可换资料根。检查在改 launcher、shortcut、descriptor 前完成。

固定代码：[apply_update.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/apply_update.py)、[DesktopEntrypoints.kt](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/DesktopEntrypoints.kt)、`tools/windows-launcher/Launcher.cs`、`StudioUpdateHandoff.kt`。放弃“只加锁但不重新检查”、同版本最后写入者获胜，以及把恢复/备份行为隐式等同默认入口变更。原 `.local/ACTIVE_STUDIO_INSTALLATION.json` 是保护记录，不能当成当前 launch 指针或为了清理而擅自改掉。

### 暂停、结束及关闭/更新排水（本轮修正）

原先队列状态检查与实际派发分离，暂停/结束可能输给已经排队的发送；关闭/更新也可能取消在途结果。现在线性化本地发送许可：状态检查与任务预留在 workspace 锁内，出站接受点再次核对；许可关闭前尚未接受的任务是 NOT_SENT，退回预留。已许可的请求收完响应并落库，不因点暂停或维护而丢弃。

暂停停止下一次派发，当前响应保存后可继续；结束也收完当前响应，但进入 ENDED，不能直接继续该批次。关闭/更新先关闭新发送门，再对生成任务 join、保存和释放资料锁。首轮回归还发现维护标记先于关门暴露的小窗口，已把关门移到标记前。VIBE准备曾绕过门，本轮在 Suite 适配器给每次编码 admission 和 HTTP接受点接入同一门，第一条慢响应落入编码缓存后第二条零发送。没有改权威 Prompt/请求构造，也没有真实调用 NovelAI。

固定代码：[GenerationSendGate.kt](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationSendGate.kt)、`BatchGeneration.kt`、`CreationSession.kt`、`StudioApplication.kt`。这是本地接受点定义，不声称服务端提供可撤回事务。服务端已经接受却结果未知时仍禁止盲目重发；网络超时、进程崩溃等全部灾难组合没有在本轮穷举。

### 冻结账户、Prompt恢复与容量（本轮同一范围）

排队批次保存完整账户配置及 secretRef，而非派发时按同ID读取已改过的档案。仅保存间隔/休息策略时保留原快照；明确换账户ID才重新绑定，旧格式null快照只登记一次。更新快照枚举当前账户与排队冻结账户的凭据引用并集，避免改档案后旧ref不再被验证；两个合成DPAPI凭据均可读时必须都匹配，目标缺旧ref时拒绝。

Prompt采集按来源、内容散列和父版本接管已提交资产，补齐标签/观察，不确定时 RECOVERY_REQUIRED，避免中断后重复入库。**故障注入实际是第二版本的两个提交窗口，不是首次导入和新版本各跑了一组。** 手动当前批次在引导/发送前统一检查累计1000项；遗留1001项可拆1000+1归档并保留引用，仍受40组和文档体积边界约束。

固定代码：`BatchGeneration.kt`、[StudioUpdateHandoff.kt](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioUpdateHandoff.kt)、`LibraryRepository.kt`、`GenerationOrganization.kt`。没有借此重开图库UI、Prompt比较或其他模型逻辑。

## 3. 兼容、失败恢复与回滚边界

旧102/103的长期根通过104桥接进入主库渠道，旧发行库和桥接资产继续保留；100/101使用已弃临时测试根，不承诺直接自动升级，不能手工替换已安装信任根来伪装兼容。历史103→104实际旧包演练仅证明当时合成资料保持；当时凭据基线是 MISSING，不能当成可解密凭据保全证明。见 [旧渠道迁移记录](../docs/audits/UX_RELEASE_MIGRATION.md)。本轮没有重复部署旧桥接，也没有修改旧包。

reader2→3只在新兄弟资料目录复制迁移，原目录、人工备份和exports保留。加密DPAPI文件在同机复制，普通备份则排除凭据，二者不能混为一谈；旧版本不能自动打开被新程序写过的新格式，也没有自动数据降级。依据 [StudioFormatMigration.kt](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioFormatMigration.kt)。

安装、B校验或入口条件提交失败时保留旧入口/程序、源资料及失败现场，结果为 FAILED_RETAINED；中断交接不直接重放旧计划。没有强杀资料写入者、覆盖旧资料或用“启动了进程”宣告成功。成功COMPLETE前保护旧程序；成功后按用户明确要求立即退役可信所属旧文件，不再保留七天或要求额外两次启动。**成功后的旧程序不再是长期回滚副本**，资料备份/格式迁移原目录仍受保护；需要回退时必须先验证版本兼容和原资料副本，不能默认拿新格式给旧程序。

## 4. 数据与清理边界

| 内容 | Studio边界及清理规则 |
|---|---|
| 程序、随包运行时、安装清单 | 新目录安装；只有认证安装器输出收据或旧COMPLETE记录固定manifest证明能赋予清理归属；校验同一句柄的大小/散列后删除 |
| 原图/衍生媒体、recipes、projects、jobs、组织/来源关系 | 由workspace/资产库持有，程序清理不进入；调整组织不移动原图，移出任务/归档不抹批次/分组来源 |
| 配置与runtime缓存 | dataRoot/runtimeRoot是显式根；只清理更新下载收据列出的两个明确文件，不把整个runtime当缓存清空 |
| 凭据 | Studio在`<workspace>/secrets`保存OS用户绑定DPAPI密文；读取回调后清零明文，普通备份排除。更新本地快照验证当前+冻结ref；公开证据只有合成凭据的散列，未读取私人凭据 |
| 插件、源码、人工备份、迁移原目录 | 明确保留，即便路径位于安装父目录；活动保护记录优先，不能为了回收空间重写保护记录 |
| 未知/被修改文件、重解析点、占用、ACL拒绝 | 未知或身份变化保留；拒绝穿越、符号链接、目录联接；占用/权限记录并有限重试，之后留待启动重试。清理失败不改更新COMPLETE/FAILED结论 |

自动机制见 [residue_cleanup.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/residue_cleanup.py)、[owned_files.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/update-runtime/owned_files.py)。当前B初次接管与前驱接管都只用旧journal固定manifest，**不会追认后来被换成未知81字节的Uninstall.exe**；没有安装时生成文件散列证明就保留它。

已验证待安装下载只能由相同目标身份及安装器散列的COMPLETE释放。所有未验证为终态的交接引用都保护准确安装器目录，包含非默认来源记录和不完整COMPLETE。失败下载/解包输出只能在其后一次成功确认后退役，不能被较早的历史成功误删。目录只移除空且明确所属者，重复执行安全。日志在`<runtime>/updates/cleanup/events.jsonl`、`state.json`、`owned/*.json`、`last-result.json`及`post-update-process.log`；不把日志本身当应一并清掉的安装器垃圾。

**历史人工清理与上述下一版机制分开：**用户批准准确清单后，本轮对20个根逐文件执行人工核对清理，实际移除236530文件、8506611341字节（7.92GiB），1086个备份/证据等保留项存在，当前115程序清单和12个保护锚点检查通过，活动记录保护的111保留。没有在日常安装中部署/调用新自动更新清理器，也没有清空回收站。详见 [人工清理回执](../docs/audits/HISTORICAL_CLEANUP_EXECUTED_20261025.md)。资料/私人凭据没有读取；排除保护范围不能写成全量私人资料的逐字节验证。

仓库本地生命周期由 [workspace_artifacts.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/248485824e1d6d2201ceedf7d14ff66e251d344d/tools/workspace_artifacts.py)管理新建`.local/disposable`所属根：先OWNER、后写产物，外部证据完整且任务成功后封存并清自己的payload；旧目录不补造OWNER。失败现场、固定基线、依赖、日常资料和不明归属保留，未自动打扫整个仓库。成功演练的生命周期结果另附证据；保留失败目录不代表授权无差别删除。

## 5. 证据、测试及未关闭状态

入口：[RUN_INDEX.json](../docs/audits/evidence/REVIEW_RESIDUALS_20261025/RUN_INDEX.json)、[原始证据包](../docs/audits/evidence/REVIEW_RESIDUALS_20261025/RAW_EVIDENCE.zip)、`RAW_MANIFEST.json`、`VERIFICATION.json`。逐例JUnit/XML、Python测试名/耗时/结果、完整命令、源码散列和stdout都保留。不要累加重复轮次。

| 层次 | 实际执行与限制 |
|---|---|
| 最终JVM定向回归 | **18唯一PASS，0失败/错误/跳过**；source59283a0，至最终源码408应用输入一致。含暂停/结束前零发送、主生成慢响应排水、双VIBE维护、同账户策略保存、双ref快照及缺失拒绝、恢复根、Prompt第二版本恢复、手动容量 |
| 最终Python定向回归 | **56唯一PASS，0失败/错误/跳过**；source2484858。含客户端校验、合成安装执行器、入口条件提交、未知卸载器、待安装/交接引用保留、较新失败输出、文件占用/拒绝/重解析点/中断/重复清理、工作区所属根 |
| 实际隔离NSIS/原生A→B | 同一168926729字节内部安装器，真实Windows A PID/image、生产Workbench维护/handoff/close、实际回环TUF与NSIS、真实B回执；3项合成资产、2个可解密DPAPI凭据快照匹配，主生成仅发送1次并保存慢响应；COMPLETE后完整清理PASS，未知哨兵保留，B窗口正常关闭，最终driver PASS |
| 原生A的性质 | test-only main驱动当前生产维护路径，非原封公开115二进制；最终补验使用同一不可变包/回环目标但全新data/runtime/programs/cache。A改造及probe JAR散列在A_INPUT_MAP，临时公开签名元数据完整保留 |
| 真实旧版本与公开渠道 | 历史103→104、107→115有各自旧证据和父进程替身等限制，本轮没有重跑，更不能冒充本轮新清理机制的真实115升级 |
| 用户日常环境 | 本轮只做已授权的程序残留人工清理及程序/指针检查，未升级日常115、未读私人凭据或调用真实服务；115新交互和真人完整GUI更新仍等用户反馈 |
| 独立审查 | 上次Project针对da2ae81/证据1d2f4ff限定关闭原第2/5/6项、限定接受新临时根清理，否决自动清理两项及其余三项残余；本轮修复已补，**新源码独立复审PENDING**。原文在PROJECT_FINDINGS.md，不把Codex测试当独立接受 |

本轮实际命令（Suite隔离测试说明，不是对Desktop或历史清理的执行授权）：

```powershell
& $PrimaryPython -B tools/Run-PreviewChecks.py --phase review-residual-final -- '-Pkotlin.incremental=false' :studio-core:test --rerun --tests '*ReviewBoundaryTest' :studio-desktop:test --rerun --tests '*ReviewDispatchTest' --tests '*RestoredUpdateSnapshotTest' --tests '*GuidanceSafetyTest.vibeMissUsesBudgetOnceAndCachedEncodingMakesNoRequest' :studio-desktop:reviewUpdateProbeJar :studio-desktop:createDistributable
& $VerifiedUpdaterPython -B tools/Run-UpdateChecks.py
Get-FileHash -Algorithm SHA256 '<准确内部交接EXE路径>'
```

运行环境：Windows 10 Pro 10.0.19045/x64，Temurin17.0.20.1，Gradle9.1.0，单worker、offline/strict依赖验证；Python/TUF依赖版本和SHA见`artifacts.json`与原始运行时清单。测试根由runner新建，Java临时文件也隔离。原生辅助驱动与完整原始参数在`reproduction-drivers/`和build receipt；临时服务只监听127.0.0.1，不接触生产信任链。内部包和真实服务调用均没有发布授权。

失败及未验必须保留：

- 首轮JVM20项中3失败：维护可见标记窗口已修；新VIBE夹具runtime嵌入资料根已纠正；**额外的`q09CanvasRotateUndoMaskResetApplyAndReopen`像素断言仍失败，未在本轮扩大UI修复，也未将其删掉/跳过为PASS**。最终18项是明确的定向子集，不代表整个GuidanceSafety/UI套件绿色。
- 上轮最终driver因WinError32失败、事后8项PASS原样保留。本轮第一原生driver也在清理未结束时向被清理器保护的祖先目录原子写证据，重试仍失败；原tmp JSON保留为失败证据，helper COMPLETE不改成driver PASS。最终驱动把自身证据放到外部，等待整轮PASS_COMPLETE后判断清理成功，再在全新根补验，最终无证据写入重试。
- 七天/额外冷启动政策曾被用户否决，已改为COMPLETE后立即退役；113布局被否决及早期更新/空间/长路径/只读占用失败仍在旧固定证据，不被新成功覆盖。
- 原封115作为A的完整新机制升级、真人完整GUI更新、真实服务、全Windows/ACL/杀软/断电/磁盘故障矩阵、跨账户/跨机DPAPI、长期RSS/CPU稳定、完整性能与灾难恢复均未由本轮证明。内部二进制的云端独立复算仍未验。

原复审包`2489844`与`1d2f4ff`、RAW ZIP及原失败文件保持原字节。旧zip SHA分别`dd2da2bcc76c47b6f0b9d4688e30cb2ea99314dd536e271f709a8223b1a9136a`和`e14b0d97ec722aac9f31d903f6012625eb97c4a5e5bb215401e58de741f00e0e`，见FROZEN_PRESERVATION。原先更早的678caf1也只是创作工程限定审查，不是全项目独立验收基线。

## 6. Desktop的实际差异与迁移建议

只读参考固定对象 **`H:\ChatChatBar-Desktop@8d1f46af89a201e5918e79cd0b31a69485f19287`**。其工作树由其他执行者持续修改，报告没有将未提交内容当固定实现，也未改其分支、ref、index、源码或数据。13项参考文件散列及观察状态在 [DESKTOP_REFERENCE.json](../docs/audits/evidence/REVIEW_RESIDUALS_20261025/DESKTOP_REFERENCE.json)。Desktop接手时必须重读自己的AGENTS、当前状态及活动合同。

| 维度 | 已核实Desktop基点 | 对适配的影响 |
|---|---|---|
| 安装 | `app/desktopApp/build.gradle.kts`声明Compose `TargetFormat.Exe, Msi`，产品`ChatChatBarDesktop`，desktopVersion独立，数值packageVersion去prerelease；既有Build-Package脚本实际调用createDistributable | 声明格式不证明现有EXE/MSI真正采用Studio的NSIS并排目录。先核对实际安装器、安装范围/权限、UpgradeCode及修复/卸载语义，再选策略；不要另造与MSI所有权冲突的文件清理器 |
| 资料与根权限 | `DesktopDataDirectory.kt`默认LocalAppData/ChatChatBarDesktop，bootstrap位于资料根之外；支持显式override、custom与portable authority。`DesktopPortableRoot.kt`识别applicationHome直接子项portable.flag和UserData并做独立验证 | 不能照搬Studio默认root或用恢复副本自动改bootstrap。沿Desktop已有authority、ownership和FreshRoot协议接入条件切换，不吞掉损坏root并回退到空库 |
| 凭据 | `WindowsSecretRootResolver.kt`使用LocalAppData/ChatChatBarDesktopCredentials，与所选资料根禁止重叠；`WindowsSecretStore.kt`是有版本封装的OS保护存储 | 不同于Studio workspace/secrets。需要按Desktop credential provider与引用/版本枚举验证，不复制Studio ref、entropy、目录，也不能在复制资料时顺手复制/清理Desktop凭据根 |
| 更新发现 | `DesktopUpdateChecker.kt`对独立`desktop-v`语义版本、Preview标记一致性及准确GitHub release页检查；限1MiB/100项、单显式只读GET、无重定向/重试，打开发布页 | 这已经是可用的发现策略；**不是TUF/安装器下载/切换器**。不能把Studio整套更新协议宣称成Desktop已有能力。若增加签名安装，单独确定其端点/渠道/根/轮换和维护职责 |
| 资料格式/备份 | `backup/DesktopBackupPolicy.kt`有自己的支持实体、媒体映射、localRoots、Secret字段排除和不支持即拒绝；还有SaveSlot/.cbbackup、跨设备合并等语义 | Studio的reader2/3、资产catalog/state快照、普通备份格式不可直接移植。先列Desktop每个持久写入者及格式迁移/回滚矩阵，保持其备份/凭据分离策略 |
| 运行与关闭 | `DesktopDataRootOwnership.kt`、`DesktopDataRootSwitchController.kt`已有根锁、容器关闭/排水后释放所有权等规则 | 复用“先禁新任务、再收在途”的原则，逐一适配聊天/语音/自动图片/手动图片/前置编码/目录写入任务。未执行Desktop回归，不能据Studio问题反推Desktop有同一缺陷或已修复 |

**可以复用的设计**：下载/安装/应用确认分阶段；认证元数据授权目标而非只信发布页；实际B读取数据和凭据后才提交固定入口；默认入口锁内条件提交；恢复副本与默认根权威分离；发送接受点与排水；按确切安装时归属和COMPLETE清理，未知/备份保护；外部原始证据封存后清合成临时根。

**需要按Desktop架构改造**：实际EXE/MSI/portable安装与Windows快捷方式/任务栏身份、Desktop根选择协议、外部凭据provider、自己的数据reader和备份兼容、跨设备SaveSlot/媒体引用、所有异步服务的排水、发布仓库/版本语义/多渠道与角色维护。先保持现有可用发现入口，按缺口增加最小部分，不把整个Studio当依赖或循环引用完整应用。

**不适用或尚未验证**：Studio应用标识、H盘目录、UUID/ref/DPAPI entropy、更新端点、签名根/私钥/密钥ID、104桥接、reader2/3和Studio测试计数；不能复制为Desktop配置。Studio的Windows10单机合成PASS不证明Desktop安装/升级/回滚、所有Windows或真实资料可靠。尚未验证Desktop安装器所有权时，不建议直接套文件退役实现。

最小实施顺序建议：先只读冻结Desktop实际源码/安装形式/根与凭据边界→列现有能力和缺口、选择一个受支持安装渠道→隔离验证包身份及兼容→补关闭排水和B确认→补条件入口切换及失败保留→最后加入成功后所属清理和工作区生命周期。每步独立Desktop测试，涉及新签名权限、真实资料/服务或发布的动作按Desktop已有授权核对，缺授权再确认；本报告不授予这些动作。

## 7. 脱敏配置示例与关键文件索引

下面只表达字段职责，故意不是有效签名配置；Desktop必须使用自己的产品、版本、reader、端点和受信任根。不要替换已安装客户端的信任根来绕过迁移。

```json
{
  "format": 1,
  "product": "<desktop独立产品标识>",
  "platform": "<desktop支持的平台>",
  "version": "<desktop实际版本策略>",
  "reader": "<desktop资料兼容版本>",
  "metadataUrl": "https://updates.example.invalid/<channel>/metadata/",
  "rootSha256": "<经独立核验的Desktop引导根SHA-256>",
  "internalTest": false
}
```

| 职责 | Suite固定源码内文件 |
|---|---|
| 打包与运行时固定 | `tools/Build-InternalInstaller.py`、`Prepare-UpdaterRuntime.py`、`update-runtime/artifacts.json` |
| 签名准备/维护（本轮未执行生产操作） | `tools/update-runtime/release_repository.py`、`prepare_release.py`、`maintain_metadata.py`、`renewal-workflow.yml` |
| 发现、下载、安装、交接 | `tools/update-runtime/studio_update.py`、`github_transport.py`、`install_ops.py`、`apply_update.py` |
| 清理/所属根生命周期 | `tools/update-runtime/residue_cleanup.py`、`owned_files.py`、`tools/workspace_artifacts.py` |
| 应用入口/维护/快照 | `modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/{DesktopEntrypoints,StudioApplication,StudioUpdateHandoff,LibraryBackupWorkspace}.kt`；固定入口原生适配`tools/windows-launcher/Launcher.cs` |
| 队列、请求、恢复/来源 | `modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/{GenerationSendGate,BatchGeneration,CreationSession,LibraryRepository,GenerationOrganization,StudioRestore,StudioFormatMigration,SecretVault}.kt` |
| 主要本轮测试 | core `ReviewBoundaryTest.kt`；desktop `ReviewDispatchTest.kt`、`RestoredUpdateSnapshotTest.kt`、test-only `ReviewUpdateNativeA.kt`；Python `test_handoff.py`、`test_residue_cleanup.py`、`test_workspace_artifacts.py`；runner `Run-PreviewChecks.py`、`Run-UpdateChecks.py` |

以上相对文件名均以第1节2484858固定源码为准；公共代码链接示例已列在各问题段。本报告证据不包含私钥、令牌、恢复码、用户图片/私有Prompt或实际凭据密文。

## 8. 可直接发给Desktop Codex的接续指令

> 请先读取Studio的`handoff/DESKTOP_UPDATE_TECHNICAL_HANDOFF_20261025.md`及其文档提交回执、固定源码248485824e1d6d2201ceedf7d14ff66e251d344d和原始证据索引，再读取Desktop自己的AGENTS、当前状态、活动任务和实际未提交工作。先只读冻结Desktop当前源码、安装方式、版本/渠道、资料根与bootstrap、外部凭据存储、备份/回滚及异步写入者；报告已有能力、适配缺口和最小实施顺序，给出独立的合成安装/升级/失败/占用/回滚/清理验证方案，再在Desktop已有授权范围内推进。Studio的新包仅为未发布内部隔离产物，18 JVM/56 Python及原生合成PASS不是Desktop验收证据；保留原失败、q09未关闭、Project待复审、115体验和Prompt对比PENDING。可以借鉴设计或适配代码，不能复制Studio标识、路径、端点、密钥/根信任、DPAPI配置或把完整Studio应用接成循环依赖。不要执行历史清理命令，不读取私人凭据，不凭“新包没有”删除未知文件，不擅自覆盖未提交修改。涉及新权限、真实服务/资料、签名托管、发布或更新日常安装，核对已有明确授权，缺少时另行确认。本报告只授权参考分析，不自动授予Desktop开发、数据操作或发布权限。
