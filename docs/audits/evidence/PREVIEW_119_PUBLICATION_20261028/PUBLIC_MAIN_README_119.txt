<!-- suite-download-entry-20261024 -->
> [!IMPORTANT]
> ## [Windows 预览版下载 — 0.0.0-preview.119](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.119) · [全部版本](https://github.com/MisakaPiano/CCB-Studio-Suite/releases)
> 在发行页 **Assets** 下载 **`CCB-Studio-Suite-0.0.0-preview.119-windows-x64.exe`**。**Source code (zip / tar.gz) 是源码，不是 Windows 安装器。**
>
> 这是**实验性邀请 Preview，尚非稳定版或全项目验收版**。119纳入编辑改善、工程收起／归档／完整包、独立预览窗口及内置本地AI识别。原版118→119公开签名发现、下载及真实NSIS隔离升级已核验。

当前主要限制：

- 本轮收起／重开、归档／历史重开、完整包导出再导入，以及此前预览窗口三步已获限定人工确认；资产管理和编辑剩余覆盖、115整体体验／任务栏固定、Prompt对比仍待验。旧q09像素断言失败根因仍UNKNOWN，不宣称Project独立验收通过。
- AI当前为本地检测框打码，非像素分割或智能擦除；准确率需用户检查。内置模型使安装包约252MiB，更新需要相应暂存空间。完整工程交换包仍限64MiB，字体文件／模型／凭据与本机撤销历史不包含在内。
- 组织和归档不复制原图；新导入只保留继续编辑所需内容，不额外留整包，已有导入资料不自动清理。文件管理与真正删除原文件的更广能力仍待后续实施。
- 更新／关闭排水、搬迁及所属清理沿用原机制；未知、自行修改和受保护内容保留，占用或权限失败安全重试。外部入口搬迁后可能需要重建，路径长度限制仍存在。
- 当前验证是本机同Windows账户的隔离案例；更多机器、网络盘、跨账户／跨机器凭据、长期CPU／RSS与大规模队列并未由本轮签收。Windows Authenticode发布者签名尚未提供；TUF签名不能替代发布者认证。Android及独立Server尚未实施。

[当前状态](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/CURRENT_STATE.md) · [本轮发行证据](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/audits/PREVIEW_119_PUBLICATION_20261028.md) · [首页同步流程](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/feature/windows-rc-continuous/docs/rc/PREVIEW_UPDATE_CONTINUITY.md)

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
