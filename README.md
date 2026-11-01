<!-- suite-download-entry-20261024 -->
> [!IMPORTANT]
> ## [Windows 预览版下载 — 0.0.0-preview.121](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.121) · [全部版本](https://github.com/MisakaPiano/CCB-Studio-Suite/releases)
> 在发行页 **Assets** 下载 **`CCB-Studio-Suite-0.0.0-preview.121-windows-x64.exe`**。Source code (zip / tar.gz) 是源码，不是 Windows 安装器。
>
> 实验性邀请 Preview，尚非稳定版或全产品验收。121减少新导入的额外存储，目录／相册使用资产引用，并整理文件夹导入和自动收集流程。原版120→121公开签名下载、真实NSIS安装与生产程序资料核验已限定通过。

当前主要限制：

- **真实图库仍有严重切页和预览加载卡顿反馈，尚未解决或完成性能验收。** 不能将局部正确性、安装成功或合成小样本通过当作千图／长期资源／全项目通过。
- 库内原件按内容散列保存，目录和相册引用资产ID，新导入不再额外另存imports图片。外部源文件保留，源文件与库内原件仍各有一份；历史冗余副本需显式逐项核清并迁移引用，不自动批量删除。
- 一次文件夹导入不自动开启持续收集。自动收集可按文件夹暂停／继续／停止；停止保留已入库图片、相册和源文件。Prompt对比、创作复用／拖放／右键、查看器顺序等原待办与历史失败／UNKNOWN保持。
- 公开121使用原正式签名更新源；本机内部维护包与公开版有不同维护方式，不会静默改成公开渠道。程序修复使用新的可信安装包，保留原程序和资料；资料内容损坏不属于程序修复能力。
- 公开更新链只覆盖同一Windows账户的隔离合成案例。更新完成提示仍打开时，旧启动器可能占用而延后清理；正常关闭该提示后，本例的已知旧文件正常退役。未知或受保护内容不为目录消失而强删。
- 安装包约251MiB。Windows Authenticode发布者签名仍未提供；TUF更新签名不等同于Windows发布者认证。

[121发行证据](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/codex/preview121-single-original/docs/audits/PREVIEW_121_PUBLICATION_20261101.md) · [121固定源码](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/e6cbe7ef6313c1389d48501d948c2d99763f1e56) · [首页同步流程](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/rc/PREVIEW_UPDATE_CONTINUITY.md)

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
