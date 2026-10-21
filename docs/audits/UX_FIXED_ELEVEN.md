# 固定 0.2.11 安装包与审计对账

2026-10-22（Asia/Tokyo），UX_RECOVERY_CONTINUOUS，feature/windows-rc-continuous。生产源码固定于 a50f4282012bca80ea46772aa97d48bd9c0d6b3d。本记录不替代最终 Project 产品审查，状态仍为 PRODUCT_HOLD / NOT RC。

## 安装包身份与实际执行

安装包位于 `.local/windows-rc/fixed-eleven-build/studio-installer.exe`，167308030 bytes，SHA-256 `b423e3d4d1e4fa637e488452dc7b8471048dfa8b402534d865599c528465bd5d`。修正包括角色位置面板纵向排列/滚动、保存选用 NAI 档案后清除旧余额；前序恢复工作区缺失凭据更新修正也在内。没有替换 0.2.9/0.2.10 已封存安装包。

实际 createDistributable 成功，38.878 秒，源码稳定。它是构建，测试数 0，不登记测试 PASS。使用已核验 NSIS 3.13 与既有固定更新运行时打包，内部渠道，测试信任根仅该隔离环境、私钥仅内存，服务只监听 127.0.0.1:52770；元数据 24 小时过期，不是长期正式更新服务。未创建正式发行密钥、修改系统信任或公开发布。

真实静默安装三项：过长路径返回 19，目录未创建；新目录安装返回 0；再次写入已有目录返回 17，拒绝覆盖。后两次独立验证全部 546 个清单文件，251073864 bytes，清单 SHA-256 `d3e07ec4dfb29626b0e88cb0384b18a3b3eb710e15e60899fbeadf4012c1cc18`。安装目录 `.local/windows-rc/fixed-eleven-applications/11-a50f4282012b`。原始 BUILD_RECEIPT 中 installed=NOT TESTED 是打包时状态；随后实际安装结果在 receipt.json，不改写历史回执。

使用已验证的合成备份建立独立 `.local/windows-rc/fixed-eleven-data`，校验 61 个文件，凭据未复制。此准备是离线夹具复制，不登记产品恢复 PASS。原生启动器 PID 32296，窗口枚举 ID 9507724、工作台标题及安装程序路径匹配。用户提供 e1018140 截图，实际主工作台和合成样图显示。两次自动捕获分别 FrameArrived / window capture timeout；未根据过期坐标发出输入。原生中文输入/位置面板观察正在征集，不冒充自动验证。

## 测试与源码身份

|模块|最近相关实际执行|结果|当前源码比对|
|---|---|---|---|
|studio-desktop|creation-controls-desktop-full|114 PASS，0 失败/跳过|对应模块文件逐一一致|
|studio-core|glyph-export-feedback-full 中的 core 部分|129 PASS，0 失败/跳过|108 个模块文件逐一一致|
|asset-library|integrated-workflows-full 中的 asset 部分|31 PASS，0 失败/跳过|12 个模块文件逐一一致|
|image-authority|integrated-workflows-full 中的 authority 部分|99 PASS，0 失败/跳过|112 个模块文件逐一一致|

这是四个模块最近执行结果的沿用核验，不是本轮重新执行 373 项，也不把包含桌面失败的历史联合运行升级为全绿。原始失败、JUnit 和修正后的执行均保留。详细散列见 REGRESSION_IDENTITY.json。真实 API 调用 0；没有为凑绿灯调用收费服务。

## 追踪表对账

只依据实际已有记录纠正过期描述，不由源码存在直接提升状态。Q06 的预设文件导入/合并/导出/重开已在 UX_R2_SOURCE_EXCHANGE 执行；T01/T16/Q05 的新工程全局默认消费已有 TypesetPreferencesTest 执行；T17–20 的 WebP/四分类/元数据/交换后续记录分别保留。C03 安装版备份恢复和第二窗口有用户连续截图及 61 文件三方核验；C06 有 7→8 与恢复根 9→10 的后续实际更新证据。原生接收端剪贴板、IME/DPI、交互视觉与产品体验仍单列，不批量标为产品 PASS。

## 当前限制与下一动作

此包尚未取得集中真人验收，原生输入/位置交互、接收端文件粘贴及 DPI/焦点覆盖未完成。内部源过期后更新检查会失败关闭，不能作为正式长期更新渠道。旧 0.2.10 窗口可正常关闭；新 0.2.11 独立窗口用于余下实机验证。先收录实际观察并完成其余可执行门槛，再交最终固定包与完整 Project 审计，保持不合并、不发布。

证据：[UX_FIXED_ELEVEN](evidence/UX_FIXED_ELEVEN/)。截图仅含合成资料；不含私有图库、明文凭据或参考源私有附件。
