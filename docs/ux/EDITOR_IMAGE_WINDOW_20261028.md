> 2026-10-28 发行补充：本轮相应实现已纳入Preview119（d9be9d9／SHA256 b425ae40…75f1c83），用户确认按原限定范围记录；公开118→119链通过。完整映射及所有未完成见docs/audits/PREVIEW_119_PUBLICATION_20261028.md，以下历史候选／未发布状态不改写。

# 图片预览窗口修复：本地候选与验证边界

2026-10-28 Asia/Tokyo，沿 UX-RECOVERY/W4／UX-R2／UX-EDITOR-GAP-001。不是新Preview发行，也不是编辑整体验收。

## 用户问题与本次范围

用户报告编辑预览首次最大化仅填满应用、第二次才最大化、图标未还原、关闭预览导致整个应用退出。旧实现确实是Compose Dialog内改变Surface尺寸，没有独立原生窗口。展开后的按钮与主窗口原生标题栏命中区域可能重叠，是关闭／二次最大化的机制解释；没有重开旧私人工程复现，不能把这项推断写成已采到的旧进程调用栈。

本次以独立Compose Window替代应用内最大化弹层，复用主应用已有DesktopWindowChrome、WindowsWindowChrome及StudioCaptionButton，每个窗口持有自己的HWND、布局记录、原生状态和关闭回调。按钮由实际窗口最大化状态切换还原标识，关闭不调用工作台退出。标题栏直接使用同一主题Panel／前景色，并同步DWM深浅设置。非Windows保留原生系统装饰，不宣称已跨平台核验。

统一入口：编辑完整预览／导出预览、创作与图库共用的StudioAssetViewer、DesktopImageToolWindow及图像裁切窗口。既有图片缩放、平移、翻页、右键、导出参数与工程操作未重写。没有顺便更改所有设置弹窗、AI／批次独立页等其他窗口。

生产工作台及其独立页为真实窗口宿主；离屏组件显式使用投影宿主。投影大小切换仅用于组件流程回归，不能证明原生最大化正确。

## 固定来源与产物

- 基点：`03f589c313b1aef324b39e4eb697bbbe747fb305`，这是此前获准的模型本地快照；本次窗口修复为其后**未提交**增量，没有新提交／push。
- 8项窗口相关源输入逐文件SHA／可还原源在`.local/windows-preview/evidence/20261027T174327165162Z-image-native-window-retained-trace-20261028/FIXED_SOURCE_MAP.json`及`fixed-source/`。
- 当前候选固定59项运行输入及JAR散列：`.local/image-window-20261028/CANDIDATE_INPUT_MAP.json`。正常生产`MainKt`，全新12张合成图、0账户、secondary workspace／暂停目录采集；不用测试宿主替代正常应用窗口。准备数据所用测试JAR不在运行类路径或生产JAR内。
- 本地启动入口：`.local/image-window-20261028/Studio-Image-Window-Review.lnk`；启动日志、PID与准确独立根见`LATEST_LAUNCH.json`。旧人工资料未检查、重置或复制，新试用根已登记保护，后续用户导入不能自动清理。
- 本候选含内置模型。此前`03f589c`内部NSIS包及其安装验证独立见`BUNDLED_AUTOMASK_MODEL_20261028.md`；**那个固定安装包不含随后窗口修复**，不能混用其安装器SHA作为本候选身份。

## 实际执行与失败保留

Windows本机、JDK17、离线严格依赖、单worker、低优先级。沿`.local/editor-native-20261027/Run-EditorChecks-C.py`，768MiB堆／512MiB元空间／2 CPU调度，实际命令在每份`summary.json`。

1. `20261027T173152473144Z-image-native-window-first-20261028`：8例执行，7 PASS／1 FAILED，sourceStable=true。既有6个标题栏逻辑与编辑预览文档保全组件流程通过；新原生测试在获取辅助功能按钮前超时。没有把失败抹掉；原XML、源、日志、fixture保留。
2. `20261027T173911234370Z-image-native-window-targeted-20261028`：测试驱动编译失败，0执行；两个新增测试中的Kotlin语法及JNA返回类型错误，原源和日志保留。不是应用已执行失败或PASS。
3. `20261027T174108329047Z-image-native-window-fixed-driver-20261028`：2新增用例PASS，sourceStable=true。真实HWND测试逐一验证编辑预览、通用图库浏览宿主、既有图像工具：标题按钮区域WM_NCHITTEST归属正确、第一次原生最大化、实际IsZoomed状态、还原尺寸、最大化后关闭，仅该窗口释放，主窗口仍显示且边界不变。另一组件用例核对深／浅标题栏实际像素、最大化／还原语义标识互斥及关闭回调。其336字节逐步临时文本随成功fixture退役，JUnit原始结果仍保留；没有伪造该文本。
4. `20261027T174327165162Z-image-native-window-retained-trace-20261028`：只复核同一原生用例，将9个逐窗口检查点同步保存在JUnit stdout并显式保留fixture，PASS／sourceStable=true。不计为新增用例。原生使用WM_NCHITTEST及系统命令，不冒充实际鼠标注入。详细逐步文本已复制到证据根后按owner退役成功自有fixture。

合计是**2新增＋7相关既有，9个唯一JUnit用例分阶段具有PASS证据**，不是一次全套，也不累计重复执行或把9个检查点计成9个测试。原始失败仍是FAILED。

候选准备首次遇到Windows长类文件路径导致测试种子JAR打包失败，尚未打开程序。旧脚本／参数／部分包散列在`.local/image-window-20261028/prepare-failed-long-path/`；恢复只复用已核对的生产JAR，使用长路径读取及精确测试种子类，不覆盖失败证据。第二次准备成功。

电脑操作技能对新候选两次读取画面分别报`FrameArrived timed out: timed out waiting on channel`和`window capture timed out: timed out waiting on channel`，已停止输入。当时实际鼠标点击和原生观感仍待用户确认，没有把命令测试或进程存在替代用户验收。

随后用户明确回复“最大化、还原和关闭这三步已经通过”。据此仅将**本地候选编辑图片预览的首次最大化、还原与关闭保留主应用**记为USER_CONFIRMED_PASS，不再要求重测这三项。图库／创作其他浏览器的人工观感、工程操作栏、编辑完整人工与独立审查并未随之通过；原电脑控制失败仍保留。

## 后续与不变边界

上述编辑预览三步已获用户确认，不重测。图库／创作浏览器共享宿主的自动结果保留，不冒充它们均获人工确认；已通过的工具效果不用重测。

工程操作栏尚未重排。用户明确纠正：重点是功能划分过细、重复入口过多，先合并重复、区分主次，再按真实操作关系收起低频项；不是原样复制“文件／编辑／视图”，也不是把全部按钮机械搬进菜单。当前仍USER_REVIEW_ISSUES_REPORTED，不把窗口局部修复写成编辑整体通过。

无新版本发布、签名、信任链改动或日常118更新。资产／编辑完整人工、115体验、Prompt对比、模型精度及旧独立UNKNOWN／失败／否决继续保留。编辑改善签收并固定交接后才迁移新对话。
