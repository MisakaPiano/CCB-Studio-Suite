# CCBdesktop 参考：Windows 安装与 GitHub 应用内升级

验证日期：2026-10-22。结论：**真实公开 GitHub 下载 → 安装 → 应用内升级 → 保存退出 → 新版启动 → 资料核验 → 再次检查最新版本已跑通**。对象是隔离合成工作区的 `0.0.0-preview.100 → 0.0.0-preview.101`，不是日常正式版本验收。

## 可核对的来源

- [固定实现源码 271eef55](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/271eef55fcd7d3421f3cd93eb227f8d54889e78d)，应用镜像来源为 `3bea16b7405d1c1890d840e76ff3dee759faad5c`；源码仓库访问权限保持原状。
- [A 测试安装包](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/releases/tag/v0.0.0-preview.100)、[B 测试安装包](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/releases/tag/v0.0.0-preview.101)。附测试说明与第三方声明，不用于真实资料。
- [固定签名元数据](https://github.com/MisakaPiano/CCB-Studio-Suite-Releases/tree/f2ab4c5a5cecaf458e875d565062bfdf99b6dea4/preview/windows-x86_64/metadata)。
- [实际验证回执](../audits/evidence/UX_GITHUB_CHANNEL/real-github-100-101/ACTUAL_RESULT.json)、同目录的 A 计划、B 确认、安装日志、最终结果及用户截图。

## 实现方式

1. **独立安装目录与资料目录。** NSIS 将固定清单文件装入新版本目录；拒绝覆盖已有目录。所谓升级保留数据，并非把新文件强行覆盖旧程序，而是保留旧版、安装新版，验证后切换启动入口。卸载只处理清单中的程序文件。
2. **签名元数据与固定下载来源。** 客户端用 TUF 验证 root/targets/snapshot/timestamp，校验渠道、版本、资料读取范围、长度及 SHA-256。签名目标绑定指定 GitHub 仓库、tag、asset，限制 HTTPS 重定向范围；不把网页的 latest 或任意 URL 当作可信更新。
3. **下载不等于安装成功。** 下载到暂存目录，长度/散列通过后才记录完成；安装前重新验证元数据和用户批准的目标身份。
4. **保存、退出、安装、确认。** A 停止新任务、保存并等待写入结束，生成资料/资产/凭据状态快照；辅助进程等 A 正常退出后安装 B。B 启动同一工作区并核验快照，只有返回匹配的 nonce、版本、源码身份及资料确认后才提交启动入口。失败保留旧程序、资料和日志，不强杀写入进程、不自动重发生成请求。
5. **资料格式变化单独处理。** 需要迁移时建立独立副本，旧目录保留。本次是格式 3 → 3，未重新验证格式迁移；此前迁移证据不能混称本次执行。

对应代码：固定源码中的 `tools/Build-InternalInstaller.py`、`tools/update-runtime/{studio_update,github_transport,apply_update,install_ops,release_repository}.py`，以及桌面模块的 `StudioUpdateHandoff.kt`。Desktop 应独立审查适配，不应复制 Suite 工作区标记、签名根或直接依赖整个 Suite 应用。

## 实际结果与边界

|项目|结果|
|---|---|
|A 从公开 GitHub 下载并安装|通过，167322647 bytes；SHA-256 `947bb931851671292e4a414d42cdfdf6da4aed5e20e8e7dcd804334a9c2564c1`|
|B 在应用内从公开 GitHub 下载|通过，167322654 bytes；SHA-256 `94f041c85aed935c11e2bc2eb1df5879d45e460f289380e01559df5861017a28`|
|实际安装与重启|交接 `915cb53c-21ab-413e-9472-794413599900` 为 COMPLETE；用户截图显示 B 启动成功|
|安装文件|A/B 各 547 文件，251091625 bytes，完整回读通过|
|资料|10 条资产字节、Recipe/工程相关散列及整体状态快照一致；原合成备份未变|
|凭据|备份原本排除的一项凭据在升级前后均为 MISSING_BEFORE_UPDATE；本次不证明可读凭据保全|
|再次检查|用户截图显示当前 101 与可信更新源一致，无需再次安装|
|自动化|已有 39 项更新/安装边界测试实际通过；本轮未重复执行，不把编译或截图当自动化测试|

首次人工启动因我将备份目录直接复制为工作区而被 FOREIGN_ROOT 拒绝。保留失败目录后，改用安装包内正式 StudioRestore 恢复到新目录，再完成上述升级。这是测试准备错误，未修改产品防护或已发布包。截图与记录保留，不能省略此失败。

**尚不具备长期发行条件：**本批签名私钥仅存在于准备进程内存，退出后丢弃；timestamp 于 2026-10-29 09:52:41 UTC（日本时间 18:52:41）到期，不能续签。长期根/在线密钥保管、备份恢复和续签部署仍待建立。安装器没有 Windows 发布者代码签名；第三方 native transitive 归属审核仍有未完成项；整体产品维持 PRODUCT_HOLD / NOT RC。

下一步是建立独立的长期信任与续签方案并完成日常包验收，不应把本测试渠道直接作为长期版本。此报告可用于 Desktop 设计和实现参考，不能作为 Desktop 自身已经通过测试的证明。
