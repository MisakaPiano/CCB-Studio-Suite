# UX-DISPLAY-THEME-001：选中态P2源码准备，资源未准入

## 2026-10-26 04:49 JST续接：磁盘已满足，内存未准入

沿用14:19 UTC已批准的原三项及一次原Project送审，不新增任务或重做四项接受基线。c39e860源码与86ad298准备材料保持，modules/tools无新改动。收到外部已完成精确清理/idle的通知后重新有界统计四模块输出，并于19:48:11、19:49:01 UTC观察：H要求仍2,195,497,728字节，空闲4,184,600,576→4,538,703,872；C也满足原805,306,368下限。可用内存1,005,854,720→1,057,619,968字节，低于原2,147,483,648门槛，最近不足1,089,863,680字节（约1.02GiB）；Java/打包owner均0。两次间隔约50秒，但因内存不足不构成安静准入，不降低预算或操作用户进程。

[D原始观察/结果/清单](evidence/THEME_SELECTED_CONTRAST_RESUME_20261026_D/RESULT.json)独立保留6文件9,043字节，只有资源观察，runner0调用、JUnit0项，NOT COMPILED / NOT TESTED；无Gradle日志或新fixture。已承接续接；跑测尚未启动；Project新提交0次/未落地；无新最终裁定，P2仍OPEN。无后台门/构建或定时唤醒，没有清理/搬移、打包/安装/发版。下一步仅在内存实际达原2GiB后重新核资源、相隔至少30秒的两次安静观察及启动前检查，再跑原`StudioSelectedContrastTest`三项并读取真实结果，固定新证据后用原一次额度送审。下文为此前H盘不足的固定历史记录。

2026-10-25，Asia/Tokyo。用户14:19 UTC明确批准继续原P2及一次原Project复审（原协调会话附人类答复）；不扩新任务、换肤功能或包阶段。原[Project最终裁定](THEME_PROJECT_REVIEW_RESULT_20261025.md)和30b0659/dce6600保持固定，本文件是其后的新候选与零执行记录，不覆盖原失败或独立结论。

## 固定对象与当前状态

- 新应用/测试源码：[c39e860252fdd96c9bec5b5d42d1abd988d3ec06](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/c39e860252fdd96c9bec5b5d42d1abd988d3ec06)。相对原30b0659，16份生产展示文件和1个新测试类；权威Prompt/请求处理、设置编解码/存储/原图逻辑均未改。
- [准备输入映射](evidence/THEME_SELECTED_CONTRAST_P2_20261025/SOURCE_MAP.json)：Git与工作副本字节/SHA分别记录；12份配置/依赖锁输入与原30b一致。**不是已执行构建输入**；编译/runner均0调用，无安装包版本或SHA可映射。公开Preview115不含本主题或本P2。
- **NOT COMPILED / NOT TESTED；实际新JUnit 0项；P2 OPEN。**源码实现候选已准备，不称验证完成或独立关闭。原4项PASS保留原范围，不重跑或计入此P2。
- **REVIEW NOT SUBMITTED，发送次数0。**本次一次送审授权保留，待针对性回归实际完成、固定新证据及核远端/目标/去重后使用，不把资源推迟材料抢先当成修复送审。

## 原因与最小修改

原实现的`primaryForeground`数学正确，但部分控件仍用页面背景Ink绘制在原始强调色Gold上；另一些仅用Gold本身表示选中/焦点，合法纯黑/纯白在相近表面失去可辨认状态。

`StudioAppearance.kt`保持`primary`为用户原RGB填充、原格式1和全部设置持久化代码不动。新增仅展示用的`selectionInk`：一次创建palette时，检查原primary在本palette的background/surface/muted/input/accent/libraryBackground/libraryPanel上最小对比；达到4.5则原色不变，否则选黑/白中最小对比更高者。不会回写、限制或替换用户RGB，未增加偏好字段/数据迁移。默认深蓝主题的蓝色仍可原样用于标识；独立数学准备值最小约5.1638，但该数值不是Kotlin/JUnit执行结果。

按实际消费端分开接线：

|消费类别|新接线|静态核对范围|
|---|---|---|
|强调色上的文字/勾选符|`primaryForeground`|BatchCreationEntry生成方式、ActiveCoverBadge（从原内联同款标签提取）、LibrarySelectionCheckbox及LibraryToggle；另修同类导出按钮图标Ink误用|
|导航/状态图标文字|`SelectionInk`|StudioRouteButton、更新待办点、旧Studio页签及项目/模型/工具/账户服务选择文字|
|图库/批次选择或拖放轮廓|`SelectionInk`|LibraryAction/ToolTile、批次模式/组/槽、图库/归档相册/动画队列/共享选择器/结果预览边框、框选轮廓、菜单选中RadioButton|
|Suite内既有源展示适配|`DesktopBootstrapColors.selectionInk`|本仓库DesktopStudioControls焦点轮廓、DesktopStudioPromptField焦点边框、DesktopStudioResultControls两种结果缩略条；仅颜色表达式，不动其请求/文本/选择行为|

精确[源码差异](evidence/THEME_SELECTED_CONTRAST_P2_20261025/SOURCE_DIFF.patch)固定于c39e860。未改真实Desktop仓库、控件点击处理、队列/归档或文件操作。未全局替换Gold：填充保留原色，其它装饰/进度/提示不因本P2扩成全面可访问性重做；任意原图上的画布编辑叠加颜色不属于标准UI表面对比保证，本轮没有为它改变画布或图像内容。上述范围是源码核对，除下述待跑控件外不冒充逐个像素验证。

不采用禁止黑白、自行改用户RGB或持久化格式的方案；也不只修原三处Ink便宣布关闭。独立前景/标识令牌避免把原始强调色填充一起改掉。源适配文件已有受保护的原始CRLF，保留字节风格，差异检查以`cr-at-eol`正确识别，未整文件归一化。

## 三项定向回归：已写，尚未执行

新类`StudioSelectedContrastTest`，沿用已有ImageComposeScene、JUnit和隔离runner，无新依赖、原生窗口或真实服务调用。计划方法：

|唯一方法|拟验证内容|实际状态|
|---|---|---|
|indicatorUsesOriginalAccentWhenReadableAndPreservesEveryStoredRgb|模式/风格/系统输入和极端RGB在标准表面上的标识对比；原RGB/格式和可读时原色/default blue保留|NOT COMPILED / NOT TESTED|
|filledBatchCoverAndLibraryControlsRenderReadableForegrounds|真正渲染BatchCreationEntry、ActiveCoverBadge、LibrarySelectionCheckbox和LibraryToggle；黑深/白浅读像素，不以同一palette数值代替消费端接线；toggle只读左侧勾选区，防标签文字假阳性|NOT COMPILED / NOT TESTED|
|actualRouteAndLibrarySelectionBordersStayVisibleAndClickable|真正渲染StudioRouteButton与选中LibraryAction，读导航与边框像素，调用原点击语义确认回调次数，含默认主题|NOT COMPILED / NOT TESTED|

模式/RGB循环、像素数和重复帧都不增加JUnit方法数。两项离屏方法只使用新runner合成工作区/阻止网络准入，不读取用户图片/配置/凭据，不生成用户验收截图。原`StudioAppearanceTest`类与30b字节一致，已接受四项不重跑。将来离屏PASS也不能代签Windows实时主题事件、真实独立窗口、用户视觉或整版验收。

待资源门通过后的计划命令（**本轮未执行**）：

```text
python tools/Run-PreviewChecks.py --phase display-theme-selected-p2-20261025 --keep-fixtures -- :studio-desktop:test --tests *StudioSelectedContrastTest
```

保持现有离线/严格依赖、低优先级、单worker/in-process配置及原堆限制。显式保留新测试fixture，不自动清理；目前runner未调用，尚无本P2 fixture或JUnit XML/Gradle结果日志。

## 两次资源观察与未启动依据

原保守H下限2,194,033,190字节、C下限805,306,368字节、可用内存2,147,483,648字节保持；四个模块build有界完整枚举后，按当前输出两倍＋1248MiB规划余量将H要求**上调**为2,195,497,728字节（约2.045GiB），不降低预算，也不冒充实测峰值。

|UTC观察时间|H空闲字节|H不足字节|可用内存字节|观察到的Java/打包owner|准入|
|---|---|---|---|---|---|
|14:34:20|1,756,524,544|438,973,184|2,099,212,288|0|NO|
|14:35:36|1,756,520,448|438,977,280（约419MiB）|2,025,144,320|0|NO|

C分别9,536,839,680和9,529,102,336字节满足门槛；两次内存也低于2GiB下限。没有owner不是全程空闲证明，更不替代磁盘/内存准入。原始[第一观察](evidence/THEME_SELECTED_CONTRAST_P2_20261025/preflight-143420285513.json)及[第二观察](evidence/THEME_SELECTED_CONTRAST_P2_20261025/preflight-143537131884.json)含枚举字节/数量、预算和快照；采集器不具备启动测试、构建或清理的分支。

初次采集因Python3.10普通路径处理遇到既有长类名而在lstat报WinError3，未生成完整观察/启动runner；初版脚本及[准备失败说明](evidence/THEME_SELECTED_CONTRAST_P2_20261025/PREPARATION_NOTES.json)保留。仅为同一build绝对路径使用Windows扩展路径后完成枚举，没有把该错误归成产品缺文件或JUnit失败，也未伪造原始stderr文件。其它准备期路径/匹配检查拒绝分开记录，不冒充产品通过/失败。

## 证据与接续

[MANIFEST](evidence/THEME_SELECTED_CONTRAST_P2_20261025/MANIFEST.json)覆盖12份文件（含清单本身），原字节、大小、SHA逐一固定；包含输入映射、差异、两观察、初/终采集器、采集脚本、计划矩阵、授权和准备说明。不含fixture payload、用户资料/原图/Prompt、真实凭据、私钥或无关聊天。此目录是新材料，不覆盖旧A资源0执行/B编译失败/C四项PASS或旧Project回执。

留证时全路径`diff --check`曾将原始patch的空行上下文前缀空格判作行尾空白；该检查拒绝已保留，不改写原始patch求绿。源码本身以`cr-at-eol`检查通过，文档检查仅排除这个原始patch，同时逐字节比对`git diff 30b0659 c39e860 -- modules tools`及清单SHA；这不是JUnit失败或额外执行。

下一步：在资源改善后重新有限统计实际输出及空间/内存，取得相隔30秒的两次安静owner观察并启动前再检，才按同一固定源码执行这三项；任何新失败原样留存，不放宽断言或重跑已接受存储基线求绿。新源码、实际执行输入/原始逐例及证据远端核对后，再使用这次已批准的一次原Project送审，区分收到/开始/最终结论。

本轮未清理/搬移、打包、发布、安装或修改日常数据/主题；没有向Project发送新请求。UPD-CLEAN-PROTECTION-001仅静态、资产/编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、q09 FAILED/root UNKNOWN、旧失败/用户否决及PRODUCT_HOLD / NOT RC保持。当前停的是资源依赖，不是用户撤回本P2或送审授权。
