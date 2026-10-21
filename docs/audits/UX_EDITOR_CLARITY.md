# 图库与编辑操作整理、连续嵌字修复

起点 9e6c586a89805d599e83c094bf0be3639ad2dcec，feature/windows-rc-continuous。用户要求降低图库/编辑的信息密度，并核对原图片编辑器的连续设置和命名预设行为。本轮最终以自动回归后告知为止，用户明确暂不开展新的人工测试。

默认编辑页收起工程管理/导入交换和页面批量操作；图库保留导入、搜索、选图/编辑主路径，来源/日期/折叠归入“筛选与分组”，伪装/引导/参照等归入“图片工具”。文件散列和详细格式说明按需展开；实际错误和进行中的操作反馈保持可见。原功能入口均保留。

已按固定 TypeSet 1.1 源码核对：修改文字样式时自动保存 textDefaults，下一张新建文字应用样式并限制到图像尺寸，命名预设仅存样式。原实现经实际 GUI 复现 42→27 字号回退；修正后即使尚未落盘也可连续切图继承，并保存重开。文字内容、位置和绘制区域不随默认值复制。新增“存为预设”及快捷样式选用；完整覆盖/删除/导入导出仍在预设管理中。打码当前工具参数随编辑保存，已有单页/显式连续默认优先级保留。

APNG 并未遗漏实际编解码接线：本轮只读核验 Suite 的 DesktopImageTools 和 ApngDisguiseCodec 与固定 Desktop 5ef95428421f7c693b010ac88f51d470804ab07a 在 LF 规范化后完全一致；GalleryAnimation 实际调用其 disguise/restore。其原生接收端仍未测，不能把普通 PNG 的粘贴验证当成 APNG 验收。

较大范围运行 `20261021T232830337912Z-editor-gallery-clarity` 实际 35 项 PASS、302.588 秒。随后收尾修正保存失败状态/来源入口和说明呈现，最后运行 `20261021T233356214623Z-editor-gallery-clarity-final`：实际 7 项 PASS，0 FAILED / 0 SKIPPED，sourceStable=true，114.726 秒。这不是再次完整 35 项运行。全部尝试及失败保留；图片为实际 Compose 离屏渲染，非 Windows 实机截图。未改 core/权威请求算法/官方 Desktop；0 真实 API 调用。

当前用户旧窗口无需保留。此变更尚未安装到该窗口；此前 0.2.12 仅构建旧快照，不冒充本轮新界面。按用户要求不启动新人工测试、不索取截图，后续实机画面优先使用用户主动提供的截图。PRODUCT_HOLD / NOT RC；集中真人验收、APNG/多文件接收及其他已列门槛继续保持未验证。

[精确源行为与边界](evidence/UX_EDITOR_CLARITY/SOURCE_BEHAVIOR.json) · [回归原始记录](evidence/UX_EDITOR_CLARITY/runs/) · [单张 PNG 实机粘贴](UX_NATIVE_CLIPBOARD_ELEVEN.md)。下一动作：告知本轮修改和自动验证完成，等待用户后续安排，不自动开始人工测试。
