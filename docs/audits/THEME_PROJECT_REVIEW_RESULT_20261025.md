# 显示主题Project限定复审：部分接受，选中态对比度P2开放

2026-10-25 Asia/Tokyo。已实际完整读取原[阶段复审结论的本轮回复](https://chatgpt.com/c/6adcaaef-3fac-83e8-ae57-2a524281e049?messageId=d5d317bf-4708-4405-ad8a-87de63bd92f3)，非Codex自评。请求`f03257f2-62a3-4bf6-981f-7ddcb98b4a0a`，回复`d5d317bf-4708-4405-ad8a-87de63bd92f3`；[单次送达/开始/最终回执](../../handoff/THEME_PROJECT_REVIEW_DISPATCH_20261025.md)。固定源码30b065915e376d75cc6439fac0ba55925c73f735、固定证据dce6600814e91c92e1d95432faef02e8ad09c5d4均不改。

## 实际裁定

|层次|Project结论|
|---|---|
|核心主题、默认与设置格式|PASS_WITH_SCOPE|
|最新元数据修订、安全保存、合成原图保全|PASS_WITH_SCOPE|
|原4项JVM测试|PASS，4唯一方法，0失败/错误/跳过|
|配色消费端完整接线|P2 OPEN — NARROW FIX REQUIRED|
|本轮独立审查|PARTIAL_ACCEPT / LIMITED_HOLD|
|原生视觉、Windows系统主题事件、真人体验|NOT TESTED|
|新安装包、集成更新|NOT BUILT / NOT VERIFIED|

Project独立核对了11/11份主题源码的Git SHA-256、28/28清单文件大小/SHA，加清单自身29文件232,995字节一致。独立接受A原资源延期0测试、B原编译失败0测试、C修正后4PASS的分列及两词法空格修正/测试类不变，不累计循环/日志/XML副本或旧基线。这不代表全部Windows环境、所有UI或整版通过。

## 既有任务下的开放遗留

沿用 **UX-DISPLAY-THEME-001 / P2-SELECTED-CONTRAST**，作为本任务的审查发现，不新建重复任务/合同。当前 **OPEN / NOT IMPLEMENTED / NOT RETESTED**。

第一类是强调色上的前景仍直接使用Ink，未消费已计算的primaryForeground：

- [BatchWorkspace.kt:39](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/30b065915e376d75cc6439fac0ba55925c73f735/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/BatchWorkspace.kt#L39)：选中生成方式文字。
- [DisguiseCoverManager.kt:97](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/30b065915e376d75cc6439fac0ba55925c73f735/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/DisguiseCoverManager.kt#L97)：封面“✓ 使用中”标识。
- [LibraryPresentation.kt:81](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/30b065915e376d75cc6439fac0ba55925c73f735/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/LibraryPresentation.kt#L81)：选中复选框勾选符。

Project按固定源码公式独立计算：深色＋纯黑强调色的Ink-on-Gold约1.11:1，浅色＋纯白约1.05:1，而正确primaryForeground均21:1。数值调色板测试通过不能代替消费端选色正确。

第二类是Gold自身被直接用作唯一选中图标/文字/边框颜色，例如[StudioRouteButton的选中图标/文字，源码第66行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/30b065915e376d75cc6439fac0ba55925c73f735/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioTitleControls.kt#L66)。Project计算黑色对深色选中背景约1.45:1、白色对浅色约1.10:1。因此**不能仅替换前述三处Ink便关闭P2**。

上述3处及导航源码索引已由本执行者对固定30b0659只读核对，未执行额外数值/原生测试，也未修改代码。建议边界按Project原结论登记：保留用户原始RGB及现有存储格式、不禁止纯黑/纯白，只围绕实际消费者提供可辨认的前景、轮廓或状态表现及最小针对回归；不重设计主题、重做已接受的四项存储检查或自动进入安装包构建。实施及新固定源码/证据另列，本次不启动。

## 仍保留的限制及权限

损坏配置加载错误当前在打开主题设置框后可见，并非启动主动弹窗；Windows系统主题实时变化和独立窗口实际重绘缺少原生证据，按原限制保留，不将其自动变成新UI改造要求。实际主题用户验收与整版包/完整更新链仍未完成，公开115不含本主题。

本轮只按用户13:27 UTC明确批准发送一次具体工程包并读取反馈，没有新建审查对话、重复发送、源码/测试修改、构建/打包/发布/安装、真实归档/搬移/删除或权限变更。临时只读浏览器标签已关闭；不保存或外发无关侧栏/聊天。后续实现不能仅凭Project建议自动扩大范围。

UPD-CLEAN-PROTECTION-001继续仅静态登记；资产/编辑USER_NOT_TESTED、115体验及Prompt比较各PENDING、q09 FAILED/root UNKNOWN、旧失败/用户否决/固定证据及PRODUCT_HOLD / NOT RC不变。独立结果依据和格式归一化转录见[审查记录](../../handoff/evidence/THEME_PROJECT_REVIEW_20261025/FINAL_RESULT.json)及同目录转录，不声称转录为原始Markdown字节。
