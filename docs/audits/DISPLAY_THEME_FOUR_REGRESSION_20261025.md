# UX-DISPLAY-THEME-001：原四项隔离回归通过，当前范围收尾

2026-10-25 Asia/Tokyo。依据用户12:46 UTC（21:46 JST）明确同意，经原协调线程附对话转述：重新核资源后只接续原4项主题测试，Desktop优先；不打包、发布、安装、真实归档/搬移/删除、自动清理或新增功能。本轮新增内容仅修正实际编译阻碍并完成同一4项验证/留证；**不进入原队列的内部包阶段，不签收UI或全项目**。

## 固定源码及实际结果

最终应用/执行源码：[30b065915e376d75cc6439fac0ba55925c73f735](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/30b065915e376d75cc6439fac0ba55925c73f735)。相对初始主题源码42146d3705f5cbc9ed392735f9c4727f8a8f67d7，只在`BatchWorkspace.kt:237`的`warning else`与`TypesetQueueWorkspace.kt:96`的`accent else`各补一个空格；此前常量替换成属性时漏分隔，使Kotlin把`warningelse`/`accentelse`当标识符。没有颜色取值、控制逻辑、断言、测试类或范围变化，见[精确修复差异](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/LEXICAL_FIX.patch)和[源码/构建输入映射](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/SOURCE_MAP.json)。

|独立记录|源码/实际执行commit|真实结果|计数边界|
|---|---|---|---|
|原A资源窗口|42146d3；固定证据4bce9ad|30分钟未准入，runner未调用|0方法；旧回执原字节保留，不改为PASS|
|本轮B首编译|42146d3输入，4bce9ad实际执行|资源准入后编译FAILED/exit1，sourceStable=true；两处词法错误，未到JUnit|0方法；保留失败日志及隔离根，不是功能断言通过|
|本轮C修正后|30b0659输入及实际执行|编译与原4个唯一JUnit方法PASS/exit0，sourceStable=true；失败0、错误0、跳过0|最终只报告4方法，不加B/A、内部模式/颜色循环或日志/XML副本|

最终唯一方法及[原始逐例](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/attempt-c/run/test-cases.json)：

|方法|已实际验证的范围|结果|
|---|---|---|
|modesAndColorStylesHaveReadableForegroundsWithoutChangingTheExistingDefault|深浅/系统模式解析、配色、强调色前景对比及深色蓝色默认的纯palette值|PASS|
|strictSettingsRejectFutureMalformedAndOutOfRangeValuesInsteadOfReplacingThem|设置格式/类型/越界/未来字段拒绝，合法值往返|PASS|
|saveUsesLatestMetadataAndReopensWithoutChangingOriginalImageBytes|最新元数据合并、重复保存不添修订、重开配置/合成原图字节与其他偏好保留|PASS|
|damagedLatestAppearanceBlocksWritesAndRetainsItsRevisionAndOriginal|后续损坏记录阻止覆盖、原修订与合成原图保留|PASS|

这些是隔离JVM/合成资料验证，**没有启动真实应用窗口、验证所有页面/菜单/独立窗口实际显示或系统深浅模式事件**。前景对比的数值通过不代签像素/UI可读性或用户体验；未调用真实服务、读取私人凭据或访问日常App/图库。

## 命令、环境、时间及资源

实际分别执行（通过现有串行runner，不添加其他测试）：

```text
python tools/Run-PreviewChecks.py --phase display-theme-resume-b-20261025 --keep-fixtures -- :studio-desktop:test --tests *StudioAppearanceTest
python tools/Run-PreviewChecks.py --phase display-theme-resume-c-20261025 --keep-fixtures -- :studio-desktop:test --tests *StudioAppearanceTest
```

Windows/PowerShell，Gradle9.1.0现有wrapper，离线、严格依赖验证、低优先级、单worker、in-process Kotlin编译；Gradle配置512MiB堆/384MiB metaspace/2CPU，测试256MiB堆/2CPU。JDK17是现有构建目标，精确运行时版本本轮未额外启动JVM采集，不把配置冒充独立环境测量。

B实际runner开始21:52:40.284 JST，54.708秒（含编译）后失败；C实际开始21:55:49.551 JST，96.211秒（含编译）后4方法通过。门的START早于runner真正开始，两者分别保留，不把准备/等待/编译时长当主题切换延迟或性能测量。

B/C均重新有限统计四个模块build目录，H盘预算维持至少2,194,033,190字节，C盘维持805,306,368字节；不下调旧预算。额外观察可用内存并设2GiB规划下限：Gradle堆512＋metaspace384＋测试堆256＋launcher128＋native/测试metaspace256＋OS/Desktop余量512MiB；这是规划估计非实测峰值。两次相隔30秒的安静观测及启动前再次检查均过，Java/javaw和打包工具未知owner/不可读CIM也会阻塞；最初系统快照CPU2%、H盘队列0只作当时观察，不证明未来或全程Desktop闲置。资源控制与原始读数分别在[控制B](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/attempt-b/control/preflight.json)及[控制C](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/attempt-c/control/preflight.json)，没有操作Desktop进程或抢占其验证。

## 原始证据及保留

[RUN_MATRIX](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/RUN_MATRIX.json)固定两轮源码、阶段、真实计数与路径。[MANIFEST](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/MANIFEST.json)逐文件大小/SHA；29文件共232,995字节，含两轮原始summary/Gradle日志/输入散列、最终JUnit XML/逐例、资源控制及实际收集脚本。原始source-before是工作副本字节SHA，Git blob SHA另列；换行可能不同，sourceStable来自runner前后比较，没有补造source-after。收集终端`rawFiles`把6个目录也计入35项；文件数以MANIFEST的28项加清单自身=29为准，该终端字段未用于测试计数或文件证明。没有复制fixture payload、用户图片/Prompt或凭据到仓库。

本轮显式`--keep-fixtures`，runner未自动清理；以下两个新建合成根仍保留，失败根不删除、不把历史目录补造归属：

- B：`.local/disposable/gradle-tests-b9013ac3cf35474d9b8c2d7b6d54d014`，首编译失败现场；[保留回执](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/attempt-b/run/fixture-cleanup.json)。
- C：`.local/disposable/gradle-tests-2f92d111e598483eacbaaf559f8d34ba`，最终合成资料；[保留回执](evidence/DISPLAY_THEME_FOUR_REGRESSION_20261025/attempt-c/run/fixture-cleanup.json)。

没有真实归档/搬移/删除、自动清理任务、新I盘空间使用、安装、签名/信任/版本/tag/更新元数据修改或新安装包；旧115/2484858包不代表本轮源码。f6598d3更新清理修复的包内集成仍未验证，Git祖先关系不代签实际包字节。

## 尚未验证及下一边界

本次四项自动回归范围完成；主题真实UI/用户、内部包与完整更新链仍NOT TESTED / NOT BUILT，未向Project新交审，也没有独立审查通过。当前授权明确止于原四项测试及其必要修正/留证，**不自动进入内部打包或后三组需求**；原队列保持，待后续指令/资源门推进对应阶段。

UPD-CLEAN-PROTECTION-001继续仅登记/静态分析，未执行或修改真实App/111；旧20根清理与保护结论不变。资产管理/编辑USER_NOT_TESTED，115体验及Prompt比较各PENDING，q09 FAILED/root UNKNOWN、旧失败/用户否决、路径错误前态及UTC UNKNOWN、所有固定旧证据与PRODUCT_HOLD / NOT RC均保留，不用本次4PASS替代它们。
