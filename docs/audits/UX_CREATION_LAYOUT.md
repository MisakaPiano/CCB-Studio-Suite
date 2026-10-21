# 创作页源码继承修正 · 2026-10-21

状态：工程回归 PASS；完整产品恢复继续，PRODUCT_HOLD / NOT RC。

基线 1f2b5fea605772443108367ed1556efcbfad89a3，feature/windows-rc-continuous。
只读参考 Desktop 37c67c724fe21107d7eaff0306439481c6f2bf07，逐文件来源及 SHA-256 见 UX_CREATION_LAYOUT_SOURCES.json。

之前虽复用了权威请求与部分组件，Suite 创作页自行重组为互斥 Prompt/角色/参数页、大生成栏和简单窄窗按钮，未完整继承源产品流程。此次直接继承 DesktopStudioFooter、DesktopWorkspaceControls，选取 DesktopStudioOutput 的 token 进度及横竖缩略条、DesktopViewerPresentation 的单击/双击入口；参照 DesktopNovelAiStudioPanel 接回连续 Prompt/角色/生成设置、图标操作、账户信息与固定生成栏。Suite 独立存储/controller 保留。没有修改 Desktop、官方 Prompt/request 或资产核心。

窄窗提供 88dp 当前图片摘要，宽窗保留分栏；缩略条跟随选中项并支持滚轮和打开预览。撤销/重做使用源 48dp 目标与悬停说明。适配仍有差异：Suite 的账户和图库是独立工作区；窄窗摘要的图像操作进入 Suite 引导页；尚未宣称全部源交互等效。

## 实际验证

- 首轮 FAILED：编译发现漏带 desktopViewerEntry；0 测试执行，原日志保留。
- 补齐源交互辅助函数后，5 项定向实际 PASS。
- 新增真实 Workbench Compose 树的尺寸/状态回归，1 项实际 PASS：1240→900→640→480→1240，固定栏 48–125px，编辑区保留，选图与草稿不丢失，撤销/重做及保存重开。
- 最终 desktop 完整 64 项实际 PASS，0 FAILED/ERROR/SKIPPED，100.289 秒。精确源码清单、JUnit、运行参数和原日志见 evidence/UX_CREATION_LAYOUT。
- JDK Temurin 17.0.20.1，Gradle 9.1.0，offline/strict dependency verification，单 worker；本轮无依赖下载、无真实 API 调用。
- 检查了 640/1240 离屏截图；不能替代原生窗口视觉或真人验收。悬停/键盘焦点行为沿用源实现，本轮未独立实际验证，不称 PASS。

下一步：继续源工作流逐项核对及其余 UX/更新故障门槛；不凭组件复用或自动测试宣称 RC。
