# 大 Recipe 往返与小人书批量操作验证

UX_RECOVERY_CONTINUOUS；feature/windows-rc-continuous，基线 f2b64dee9bda07dcf7b6c154b267292a80d511ea。PRODUCT_HOLD / NOT RC。

## 真实问题及修复

合法的编辑工程上限为 16 MiB，原图的来源 Recipe 允许 1 MiB；附带 base64 来源后，编辑 sidecar 可能超过原来的 16 MiB 导入门禁。合成回归以 158 个隐藏文本对象和 1 MiB 合成来源，实际完成导出后在导入处 FAILED。现在 sidecar 编解码与文件选择读取共用 20 MiB 上限，导出也进行大小检查；不截断文字或来源、不改变原资产核心上限。新版本可读取既有 sidecar；旧软件仍会拒绝超过其旧上限的附件，这不代表允许旧程序读写新根。

直接对照 TypeSet 1.1 的 applyStoryStyleToEntries：复制当前样式到选定页，但保留每页旁白。新增实际 Workbench Compose 操作覆盖页面宽度、边距、间距、文字 padding、最小高、字号/行距/字体/对齐、颜色、边框、缩进、页码；每个控件都断言状态。测试实际暴露队列复选框把当前页跳走，改变了待应用样式来源。修复为复选框仅改变选择集合，点击缩略图仍负责跳页；展示当前样式来源和所选/全册页数。

三页各有不同旁白；勾选第三页时第一页面保持当前，所选页与全册样式应用分别验证，第二页未选择时保持原样；每次批次可一次撤销/重做，旁白和原图不变；关闭重开工程内容完全相同。既有队列拖动、边缘滚动、按编号移动与窄窗弹层回归继续通过。实际查看 story-custom-batch 离屏图又发现：测试选择系统字体列表首项，其缺少中文字形，预览显示方框；控件状态断言没有覆盖字形可读性。进一步源码定位为小人书测量/缺字校验使用回退字体，绘制却直接使用原字体，导出也可能受影响；不能声称导出会自动拒绝这个组合。该视觉缺口保留并继续修复，不能把此图称为中文排版视觉 PASS。

## 实际执行

- 大 sidecar 首次复现：1 FAILED，明确失败于原 16 MiB inspect 门禁。
- 修正后联合目标：5 个 core PASS / 1 个 desktop FAILED，后者明确抓到复选框换页。
- 复选框修正后：5 个 desktop 实际 PASS；core 当次 UP-TO-DATE，不重复计数。
- 四模块串行完整执行：351 PASS，0 FAILED/ERROR/SKIPPED，799.618 秒，sourceStable=true。分模块计数见 EXECUTED_MODULE_COUNTS.json，全部是新 XML 中的实际测试。

JDK Temurin 17.0.20.1 / Gradle 9.1.0，离线严格依赖、单 worker，asset 测试 192 MiB、其余 256 MiB，各限制 2 CPU。真实 API 0、依赖下载 0；资产安全原有 1/100/1000 规模和进程故障未缩减。所有 FAILED 与最终源码身份保留。截图为产品 Compose 离屏场景，不冒充原生 Windows 截图或真人认可。

将此前 18 项尚无映射的动作关联到实际实现及执行用例，75 项均可追踪；不表示每项产品体验已通过。尤其 C07 原生 IME/高 DPI/焦点、I05 接收端剪贴板、F07 原生快捷键仍单列未测；下一步固定安装包的界面与备份恢复、批量导出进度和安全更新最终检查继续。未修改源 Desktop、TypeSet 母本、官方 Prompt/request 或资产安全核心。

Desktop 工作树观察 HEAD 已前进至 62b93e824333540657474d43dc63723d6e90ab24；只读比较确认自此前标题栏参考 37c67c724fe21107d7eaff0306439481c6f2bf07 起，标题栏与 NAI Studio 文件未变，没有随参考工作树自动切换权威来源。111 个 image-authority 文件逐字节与固定 5ef95428421f7c693b010ac88f51d470804ab07a Git 对象及来源清单一致。TypeSet 原文件散列保持一致。

精简安全证据交付 Git；Drive 本检查点 NOT UPLOADED。没有 RC/发布/合并结论。
