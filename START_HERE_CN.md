# CCB Studio Suite — 文件包使用入口

**版本：INIT-1.0 · 打包整理版｜资料日期：2026-10-20｜性质：立项文件，不是软件发行包。**

## 你只需做这些

1. 将 ZIP 解压到一个新目录，最终路径建议为 `H:\CCB-Studio-Suite`。压缩包已经包含顶层 `CCB-Studio-Suite` 文件夹；解压到 `H:\` 即得到该路径。不要覆盖已有同名工程，也不要解压进 `H:\ChatChatBar-Desktop` 内。
2. 新建 ChatGPT Project **CCB Studio Suite**。将 [Project Instructions](instructions/PROJECT_INSTRUCTIONS.md) 的内容复制到项目指令；上传 [立项母本](CCB_STUDIO_SUITE_INIT_20261020.md)。其余分文件按需读取，不要求一次上传全部。
3. 在 Codex 中打开 `H:\CCB-Studio-Suite`，发送 [Codex 开场消息](handoff/CODEX_START_MESSAGE.md)。完整 [A0-001 任务书](tasks/A0_001_BOOTSTRAP_AND_AUDIT.md) 已在包内。
4. 在新 Project 发 [接手消息](handoff/NEW_PROJECT_OPENING.md)。同一 A0 任务只交给一个执行者，不并行重复建项。

**文档和参考材料已经拆分好，无需再次从聊天复制或让 Codex 重建同一批文档。** A0 从“检查现成资料＋只读复用审计”开始；不是开始写生产功能。

## 已包含什么

- 产品章程、Windows/Android/Server 模式、首版范围；
- 数据保全、完整应用内更新、服务端队列与授权合同；
- 来源观察记录、Git 协作、路线图、验收矩阵、当前状态与任务登记；
- Project Instructions、AGENTS.md、Codex 首轮任务书、跨 Project 交接说明；
- TypeSet Forge 1.1 原始 HTML、说明书和更新说明（只读参考）；
- SHA-256 文件清单和离线校验工具。

TypeSet Forge 文件位于 `.reference-assets/typeset-forge-1.1/`，已被 `.gitignore` 排除。HTML 未被执行，未下载字体或外部脚本。参考文件不因进入本私人交付包就自动取得公开再分发授权。

## 可选的离线完整性检查

在包根目录打开 PowerShell：

```powershell
powershell.exe -NoProfile -File .\tools\Verify-Package.ps1
```

如果系统策略不允许运行脚本，不要修改全局执行策略；可让 Codex 检查脚本后运行，或使用：

```text
python tools/verify_package.py
```

校验只读取本文件包，不联网、不创建 Git、不修改工程。它用于验证收到的初始包；开始编辑后出现哈希不匹配是预期现象，不代表项目数据损坏。

## 必须知道的边界

- 本包没有 `.git`、生产 Kotlin/Gradle 工程、安装程序或签名密钥。
- 没有创建远程仓库、设置公开/私有、推送或修改现有 Desktop。
- Git SHA 是上一轮立项对话的**有日期观察**，本次只打包，未重新查询最新 HEAD；A0 按需核对增量，不自动升级依赖。
- [CURRENT_STATE](docs/CURRENT_STATE.md) 是初始化状态；内容不宣称软件功能已完成。
- 任何现有用户数据、API 凭据或真实付费调用都不在 A0 授权范围内。
