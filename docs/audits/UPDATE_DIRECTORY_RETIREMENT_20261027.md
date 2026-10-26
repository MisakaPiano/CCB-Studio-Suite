# 更新后旧空目录的工作目录占用修正

2026-10-27 Asia/Tokyo。沿用 UPD-CLEAN-PROTECTION-001 和既有更新清理合同。源码基点 `594c006eb762730de8649016d06cd74c27048280`；本次固定实现 [9f5a6a6be251326b2d4be27a8305cc6f74754d7b](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/9f5a6a6be251326b2d4be27a8305cc6f74754d7b)。**源码与定向验证完成；没有新安装包、版本号、tag、发布或日常安装修改。新包独立审查 NOT REQUESTED。**

## 缺口和采用的方案

已有机制会在 B 程序、资料及凭据快照确认，默认入口成功切换、交接为 COMPLETE 后，按所属收据和散列退役前序程序及下载；本轮没有重新造清理器，也没有放松保护或删除边界。

发现两个具体缺口：A 启动 helper 时将 cwd 指向旧安装；helper 启动 B 和清理进程未指定 cwd。Windows 会因此保留旧目录句柄，即使其文件已全部退役。另一方面，`prune_empty` 吞掉目录删除的 OSError，使这一情况没有进入 DEFERRED/重试。

- [StudioUpdateHandoff.kt](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/9f5a6a6be251326b2d4be27a8305cc6f74754d7b/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioUpdateHandoff.kt) 将 helper 工作目录改为本次 runtime/handoffs 子目录。[apply_update.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/9f5a6a6be251326b2d4be27a8305cc6f74754d7b/tools/update-runtime/apply_update.py) 的 `start_b`、`start_cleanup` 显式使用 B 安装目录；现有 Kotlin 启动清理器原本已使用新目录，保留。
- 兼容旧客户端：117 的 helper 已经发布，不能靠修正未来 helper 解决首次升级。新增 [UpdateLaunchDirectory.java](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/9f5a6a6be251326b2d4be27a8305cc6f74754d7b/modules/studio-desktop/src/main/java/io/github/misakapiano/ccbstudio/desktop/UpdateLaunchDirectory.java)，由 Main 最早调用。仅 Windows 安装版本带显式 runtime 内的 update-handoff 且 native cwd 不为自身安装目录时，在**打开工作区之前**以正确 cwd 再进入同一程序、保留全部参数；原宿主等既有 helper 的 COMPLETE/FAILED_RETAINED 终态后退出。不会制造 ACK、终止子进程或接管资料锁；超时仍交给原 helper 失败保留流程。普通启动及已指定正确 cwd 的新 helper 不走此分支。
- 没采用只改 `user.dir` 或只调用 SetCurrentDirectory：jpackage 还可能有等待 JVM 的原生父进程，不能靠调整 JVM 自身释放父进程句柄。原生测试实际覆盖这一点。未采用强杀、远程修改进程工作目录、提前删旧版或修改已发布包。
- [owned_files.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/9f5a6a6be251326b2d4be27a8305cc6f74754d7b/tools/update-runtime/owned_files.py) 返回每个所属目录的删除结果；[residue_cleanup.py](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/9f5a6a6be251326b2d4be27a8305cc6f74754d7b/tools/update-runtime/residue_cleanup.py) 将占用/权限失败记为 DIRECTORY_DEFERRED，并纳入原有四次短重试及后续启动重试。不改写更新终态。Windows 145 非空目录按保留处理，不递归扫描未知内容，也不因未知文件长期重试。

原成功门槛、回滚前保护、失败安装保护、ACTIVE/备份/插件/用户资料保护、同句柄验证删除和重解析点拒绝规则不变。历史受保护安装不能因为“版本旧”自动撤销；这与已获准退役的空目录是两个问题。清理日志仍在所选 runtime 的 `updates/cleanup/events.jsonl`；所属 receipts/state 保留。清理失败不改变应用的 COMPLETE，也不应把清理自身报告为已经完成。

## 实际验证和固定映射

Windows x64、本机 H 盘隔离合成根；Python 3.13.16，JDK 17.0.20.1，离线 Gradle 9.1.0、单 worker。没有调用真实 provider、读取日常图库/vault 或使用正式签名密钥。

| 新增唯一用例 | 实际结果 |
|---|---|
| 子进程真实占用旧 cwd；所属文件删完后必须 DEFERRED；正常退出后再清理 | PASS，旧根最终移除，COMPLETE 回执字节不变，重复移除 0 |
| B 和清理子进程均声明新目录 | PASS（源码 AST 定向断言；实际进程验证另列下方） |
| 未知文件令目录非空 | PASS，原字节保留，所属空子目录移除，不无限重试 |
| 目录访问被拒 | PASS，记录 DEFERRED；解除故障后安全重试，合成原件保留 |
| 旧 helper 继承 cwd 的 jpackage 原生宿主 | PASS，生产 Java 兼容代码重新进入正确目录，原宿主等终态；B 继续运行时旧根已不存在 |
| 修正后的生产 start_b 原生宿主 | PASS，直接正确 cwd；B 继续运行时旧根已不存在，生产 start_cleanup 启动的原生 worker 探针 cwd 正确 |

合计 **6 个新增唯一用例**。另定向复核 4 个既有保护用例（ACTIVE/备份、清理中断与重复、路径穿越/真实 junction、恢复副本/无效保护记录），均通过，不算新增用例，不重复累计先前 7 项兼容测试或搬迁测试。

应用 `compileKotlin`、`compileJava` **COMMAND_SUCCESS，161.343 秒，sourceStable**；JUnit 执行数 **0**，不能将编译算成测试。原生探针用生产 Java 源码与保留的 jpackage launcher/runtime 构造无界面薄宿主；终态由合成驱动提供，worker 使用只报告 cwd 的 C# 探针。它们证明真实进程/父进程与目录句柄行为，**不是签名发现、真实 NSIS、应用资料/凭据校验或完整旧117→新包升级的新增证据**。

可复现入口：`tools/update-runtime/test_launch_directory.py` 的 4 个方法，以及 `tools/Test-UpdateLaunchDirectory.py --owner <新建OWNER根> --baseline <保留的隔离app-image> --jdk <JDK17> --out <新证据目录>`。嵌入式 Python 执行单元测试/Gradle runner 时需显式把 tools 或 tools/update-runtime 加入 sys.path；不要复用已退役的夹具路径。[原始清单](evidence/UPDATE_DIRECTORY_RETIREMENT_20261027/RAW_MANIFEST.json)含命令、逐例结果、日志和执行字节；[源码/编译映射](evidence/UPDATE_DIRECTORY_RETIREMENT_20261027/SOURCE_MAPPING.json)将 CRLF 执行字节、Git 固定源码及编译类散列分开记录。尚无新包，因此没有新的安装器 SHA-256，不能沿用117包散列声称包含本修复。

## 失败留存和清理

- 修前 RED 两例真实失败：空目录被占用仍报 COMPLETE；Popen 缺 cwd。原结果与小型失败夹具保留。
- 首次修后准备有 4 ERROR：测试根较长，使既有 JSON 原子临时文件触及 Windows 路径限制；没有据此宣称产品长路径问题已修。换更短的**新 OWNER 根**后执行，最终只计上述 4 个单元方法。原失败不覆盖。
- 首次直接调用 Gradle runner 因嵌入式 Python `_pth` 找不到 tools 而止于准备；未执行 Gradle。补显式 sys.path 后编译成功，原准备失败保留。
- [所属退役回执](evidence/UPDATE_DIRECTORY_RETIREMENT_20261027/FIXTURE_RETIREMENT.json)：通过夹具 520 文件、146,150,415 字节已按 OWNER/manifest 留证退役；所有探针正常退出，无强杀。源码、编译输入、原始证据和小型失败夹具保留，不动日常安装及受保护历史目录。

## 下一候选门槛及未验状态

本次没有重发117或修改正式信任/更新协议，日常117不会因源码提交自动获得修复。下一候选需纳入9f5a6a6，固定新的构建输入/包散列，并在隔离目录补一次**真实旧117 helper→完整新包**的安装、程序/合成资料与凭据确认、默认入口和“B运行时旧根移除”组合核验；本报告不预签该结果。更慢/受限 Windows、桥接超时故障和真实日常升级亦不由本次两条薄宿主通过替代。

资产/编辑完整人工验收、115体验、Prompt对比及实际任务栏固定各待验；q09 FAILED/root UNKNOWN、旧否决和历史失败保持。没有新增 Project 独立验收结论，也不扩展到性能、界面开发或 Desktop。
