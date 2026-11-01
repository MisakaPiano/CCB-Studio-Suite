<!-- suite-download-entry-20261024 -->
> [!IMPORTANT]
> ## [Windows 预览版下载 — 0.0.0-preview.120](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.120) · [全部版本](https://github.com/MisakaPiano/CCB-Studio-Suite/releases)
> 在发行页 **Assets** 下载 **`CCB-Studio-Suite-0.0.0-preview.120-windows-x64.exe`**。Source code (zip / tar.gz) 是源码，不是 Windows 安装器。
>
> 实验性邀请 Preview，尚非稳定版或全产品验收版。120减少未变化目录的重复内容扫描，明确自动打码“当前图／所选N张”，简化图库操作并增加页面说明，纳入已限定验证的更新恢复与程序修复工具。原版119→120公开签名发现、下载及真实NSIS隔离更新已核验。

当前主要限制：

- **重复存储尚未解决**：图库单份原件＋目录/相册引用的改造仍为最高优先待办，已有目录副本没有清理。本版不声称消除了重复保存；源文件和现有资料保留。
- 空闲扫描的局部合成观察通过，不等于真实千图或长期CPU/RSS、各页全面性能通过。实际变更、手动同步与原件读取仍保留校验；属性稳定不等于内容完整性认证。
- 帮助按钮、图库呈现及编辑范围说明三项已获限定人工确认；自动打码识别质量仍须检查，所选指编辑队列勾选集合，每次最多100张。Prompt对比及其余原待办、历史失败和UNKNOWN保持。
- 程序修复从新可信安装包运行，保留原损坏程序和资料；不修复资料内容。当前公开更新验证只覆盖本机同Windows账户的隔离合成案例，没有自动升级日常安装。
- 内置本地AI模型使安装包约251MiB，更新需相应暂存空间。Windows Authenticode发布者签名仍未提供；TUF签名不能替代发布者认证。

[120发行证据](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/codex/preview120-gallery-workflow/docs/audits/PREVIEW_120_PUBLICATION_20261101.md) · [120固定源码](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/8fccd113aa387a7adbac6894d3327a8b1d8080e6) · [首页同步流程](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/rc/PREVIEW_UPDATE_CONTINUITY.md)

---

<!-- release-migration-20261023 -->
## 发布地址迁移记录（2026-10-23）

[Project releases](https://github.com/MisakaPiano/CCB-Studio-Suite/releases) are the canonical download location. 迁移当时的合成工作区测试包属于历史阶段；当前邀请 Preview 状态和下载入口以上方说明为准。

The former distribution repository is retained for compatibility. Historical 100–103 assets are unchanged. Long-term-channel 102/103 can update through the old address to bridge 104; installed 104 then checks the project repository. The temporary 100/101 trust channel remains historical and is not silently replaced.

Both long-term channels keep snapshot/timestamp renewal. The compatibility channel can remain pinned to bridge 104; its targets metadata still requires offline renewal before expiry. TUF signatures are not Windows Authenticode.

The following text is historical project/channel information; the migration status above takes precedence for distribution.

---

# CCB Studio Suite

Windows / Android 原生图像创作客户端与独立 Studio Server。

**Windows 已提供可运行的实验性邀请 Preview；默认分支保留项目与运维资料，当前应用源码和审计在持续开发分支。Android／Server 仍是后续范围。**

首次使用：[START_HERE_CN.md](START_HERE_CN.md)。

工程入口：[文档索引](docs/00_INDEX.md) · [当前状态](docs/CURRENT_STATE.md) · [任务登记](docs/TASK_REGISTRY.md) · [来源记录](docs/SOURCE_LOCK.json)。

目标：AI 写/改 Prompt、NovelAI 生图、多 LLM/NAI 账户档案、可靠图库、完整 Recipe 比较、非破坏图片编辑与批量打码嵌字；客户端支持本人 API 直连和服务器工作区。首个实际使用版本必须具备完整安全应用内更新。

本项目独立于官方 CCB 与 CCB Desktop；仅按明确版本复用和审查。官方 Prompt、现有 Desktop 验收与排期不因本项目自动变化。

开发工作区：`H:\CCB-Studio-Suite`。源码仓库：`MisakaPiano/CCB-Studio-Suite`。

本包没有给上游源码或参考工具新增许可证。公开发布前核对实际采用代码、素材、字体和依赖的授权，不擅自给混合来源内容统一重新授权。
