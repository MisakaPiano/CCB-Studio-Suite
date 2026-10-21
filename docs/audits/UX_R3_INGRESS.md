# UX-R3 文件、拖入与剪贴板入口

基于3726939074210a96b34b626b41bb6070c953fd06，连续恢复，PRODUCT_HOLD。

参考固定Desktop 5ef95428421f7c693b010ac88f51d470804ab07a的app/desktopApp/src/main/kotlin/com/example/chatbar/desktop/DesktopImageIngress.kt：文件列表优先于位图，普通文本粘贴不拦截，Compose DragAndDropTarget与Ctrl+V。Suite独立适配，不修改参考仓。文件不按后缀静默丢弃，逐项读取/解码验证后存入既有安全资产库；文件最多1000项，每文件32MiB/12582912像素。坏文件可跳过；资产提交错误停止后续批次，未完成项标明状态，禁止自动重试。所有原件不覆盖。

文件选择、粘贴按钮、键盘及拖放共用同一导入路径；成功聚焦最后一项，并选择全部成功项。剪贴板位图明确为新编码PNG，没有声称保全不存在的原始文件。原件Recipe记录image-ingress来源/日期，不记录源绝对路径；新日期参与图库筛选，旧无日期原件仍标日期未记录。文字框普通粘贴保持原行为。部分导入失败显示回执，状态不再笼统标全部完成。

## 实际执行

四次运行保留：首轮测试辅助函数语法编译FAILED，0项执行；修正后定向3 PASS；修正测试先关闭modal及部分完成状态/导入日期后3 PASS；最终完整desktop54 PASS，93.95秒，0失败/错误/跳过，源码稳定。core/资产/权威源码未改，本检查点没有重跑它们，不将旧136项称本次结果。

真实Compose工作台按钮→隔离Transferable文件列表→一坏两好逐项回执→关闭回执→展开结果→原字节检查→位图导入标记→保存重开，均实际执行。文本与文件双格式优先级、数量上限、异常列表均测；文件和PNG像素/来源重开一致。最终截图已查看；初始尚未关闭回执的截图保留，测试已修正，不再穿透modal操作背景。使用合成素材，测试不读取/写入系统剪贴板。

系统级拖放/键盘Ctrl+V、原生JFileChooser、存储提交失败注入和中途停止的完整GUI演练 **NOT TESTED**。这些接线/防护存在不等于系统实测；此前资产故障测试不代签本轮整批导入。APNG导入保留原文件，但静态编辑确认、伪装/还原完整交付仍继续，不因此宣称W2全通过。

JDK17.0.20.1，Gradle9.1，offline/strict依赖/单worker，0新下载/依赖、0真实API，无用户图库/凭据/系统信任修改。未开关用户窗口，未合并main或发布。

[全部运行](evidence/UX_R3_INGRESS/runs/) · [界面](evidence/UX_R3_INGRESS/partial-import.png) · [原始字节清单](evidence/UX_R3_INGRESS/PACKAGE_FILES.json)。全部合成证据Git交付，Drive NOT UPLOADED，无独立非Git附件。下一步APNG明确选择与文件交付，完整R4仍必需。
