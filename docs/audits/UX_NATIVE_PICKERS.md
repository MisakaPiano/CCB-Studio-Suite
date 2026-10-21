# Windows 原生文件选择与分步恢复

UX_RECOVERY_CONTINUOUS；基线 893ae61fb0de39eff59351ade406b94fefa29a1a，feature/windows-rc-continuous。PRODUCT_HOLD / NOT RC。

## 用户观察与实际问题

用户在固定安装版 0.2.5-internal 的真实界面完成备份，随后把恢复存放位置选成备份来源，界面仅在确认后返回 MISSING。截图中的目标 Studio-Restored-e0ba913f-b44c-4eb0-88de-dc96df6de4da 经只读检查不存在；原工作区仍打开。该次恢复是 FAILED，不能记为成功。用户明确要求替换旧 Swing 文件夹选择框。

## 实施

只读参考 Desktop 提交 62b93e824333540657474d43dc63723d6e90ab24 的 WindowsCommonItemDialog、JnaCommonDialogBackend、DesktopPlatformPickers 中的 DesktopDialogOwner 及两份测试。逐文件 Git blob / SHA-256 / Suite 输出身份见 SOURCE_PROVENANCE.json。固定 NAI 权威提交 5ef95428421f7c693b010ac88f51d470804ab07a 不变；没有修改 Desktop 或上游。

Windows 全部产品文件选择统一使用系统 Common Item Dialog（IFileOpenDialog / IFileDialog）：导入图片、多选、Prompt、角色卡、字体、Recipe/工程/预设交换、保存原件、动画/批量导出、资产目录、备份恢复。旧合成入口也接同一服务。Swing 仅保留非 Windows 平台后备；Windows 原生调用失败明确报错，不悄悄退回旧选择框。

沿用 Desktop 单独 STA、主窗口 HWND 所有权、AWT SecondaryLoop、取消与 HRESULT 错误区分、路径内存与 COM 生命周期；Suite 扩展中文标题、指定初始目录、多选顺序和逐项资源释放。没有新增依赖。JNA 现有 Shell32 接口缺少 SHCreateItemFromParsingName 声明，补了仅此系统函数的窄绑定。调用依据 Microsoft [IFileOpenDialog](https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nn-shobjidl_core-ifileopendialog)、[IShellItemArray](https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nn-shobjidl_core-ishellitemarray)、[SetFolder](https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nf-shobjidl_core-ifiledialog-setfolder) 官方接口文档。

恢复流程先选择并检查来源目录，再由独立按钮选择新工作区的存放位置；错误来源在第一步说明原因。刚完成的备份可点“恢复这份备份”，避免重复找来源。确认框分别显示读取来源和新建目标。初步目录检查不代替已有核心的完整散列/清单验证；取消不创建目标，损坏/冲突仍由原安全核心拒绝；原件、备份、现有工作区保持原安全语义。没有修改数据安全核心。

## 实际验证

- 首次定向：编译 FAILED，未执行测试；缺少上述 JNA 函数声明，原日志保留。
- 修正后定向：16 PASS / 0 FAILED / 0 SKIPPED，71.808 秒。含真实 Windows COM 打开/保存/目录配置与初始目录、实际 Compose HWND 所有权验证；这些非交互测试不弹框，不冒充真人选择操作。
- 多选顺序、逐项失败释放、取消、同服务 STA 复用、并发拒绝及 UI 线程可响应：实际自动化 PASS。
- 新旧备份、独立资产/工程/Prompt、备份不导出凭据、取消/现有目标/损坏拒绝，以及来源/目标混淆回归：实际自动化 PASS；注入选择器并验证产品按钮，不冒充系统对话框手动选择。
- 最终 desktop 完整：108 PASS / 0 FAILED / 0 ERROR / 0 SKIPPED，250.477 秒，sourceStable=true。

JDK Temurin 17.0.20.1、Gradle 9.1.0、JNA 5.18.1，离线严格依赖、单 worker、测试 256 MiB / 2 CPU。依赖下载 0、真实 API 0。core / asset-library / image-authority 本次未改未重跑，旧 PASS 不计入本轮。

新原生选择框的安装版实际点击、中文路径选择、取消后的焦点及第二窗口恢复仍须安装版验证。证据中用户截图来自之前 0.2.5 安装版；restore-compose-scene 是真实产品 Compose 离屏场景，二者不混淆。不能凭接口配置测试宣称原生视觉或 Windows Preview 验收通过。继续固定包和剩余工程门槛；本检查点 Drive 尚未上传。
