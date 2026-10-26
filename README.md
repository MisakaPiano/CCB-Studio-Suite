<!-- suite-download-entry-20261024 -->
> [!IMPORTANT]
> ## [Windows 预览版下载 — 0.0.0-preview.116](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.116) · [全部版本](https://github.com/MisakaPiano/CCB-Studio-Suite/releases)
> 打开发行页，在 **Assets** 下载 **`CCB-Studio-Suite-0.0.0-preview.116-windows-x64.exe`**。**Source code (zip / tar.gz) 是源码，不是 Windows 安装器。**
>
> 这是**实验性邀请 Preview，尚非稳定版或完整 RC 验收版**，以 Windows 生图和图库管理体验为重点。116包含更新/关闭排水、所属清理和显示主题修复；公开签名发现、下载及隔离安装已验证，用户验收范围仍单独保留。

当前主要限制：

- 116对安装目录曾改名/移动后留下的旧缓存路径记录，自动清理可能延后；安装及资料校验可成功。针对性源码修复和7项隔离回归已完成，但尚未随116交付；不要手工删除历史记录来解除保护。完整目录搬迁和真实跨盘迁移仍待验证。

- 资产管理/编辑完整人工验收、115体验反馈和Prompt对比分别保留待验；自动与局部人工检查不能代替完整功能验收。日常安装状态单独留证。
- q09编辑历史像素断言失败根因仍UNKNOWN；跨机器恢复、更多Windows环境、长时间负载及各供应商实际调用没有本轮新验收。
- 清理只处理具有可信归属的废弃程序和缓存，受保护111、未知/自改文件、用户资料与备份保留。Windows Authenticode发布者签名尚未提供，不能把TUF更新签名视为Windows发布者认证。
- Android 与 BYOK Server 尚未实施；当前可下载交付为 Windows x64。

[当前实现与验证记录](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/CURRENT_STATE.md) · [阶段独立复审交接](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/handoff/PROJECT_PHASE_REVIEW_20261024.md) · [以后首页链接同步流程](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/rc/PREVIEW_UPDATE_CONTINUITY.md#首页下载入口同步)

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
