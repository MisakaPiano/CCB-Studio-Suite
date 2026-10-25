# Tokens文字对比窄修与定向自动验证

2026-10-26 Asia/Tokyo。用户在上一候选三项按钮人工通过后，明确要求修复深色＋黑色强调色下Tokens文字，自动验证先做，暂不发新版本。本项沿用UX-DISPLAY-THEME-001 / THEME-TOKENS-CONTRAST，不创建后续功能任务。

## 源码与结果

固定源码：[26ef38857be510636dffdb5fcbd59a8d70c85550](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/26ef38857be510636dffdb5fcbd59a8d70c85550)，父提交37231f6。只改一个生产消费者DesktopStudioResultControls.kt，新增一个StudioTokenContrastTest.kt；报告/证据另列文档提交，不能作为新应用版本或发行包。

正常Tokens文字和进度条原先共用primary。自定义强调色000000使文字成为黑色，与深色底栏失去对比。现在正常文字使用现有selectionInk，其按现有主题表面计算可读前景；进度条继续使用原primary。接近上限/超限仍沿用原warning/destructive色。未改原RGB、颜色存储、阈值、计数、文案、进度比例、tokenizer、Prompt或请求逻辑，也没有全局替换primary、禁用黑色或修改Desktop。

**状态：IMPLEMENTED / TARGETED AUTOMATION PASS。新源码原生运行及用户人工仍NOT TESTED，独立审查NOT REQUESTED。**用户此前48eed弹窗三按钮通过原样保留，不扩签到26ef新Tokens文字。

## 实际自动验证

Windows x64、现有JDK17/Gradle9.1、offline/strict、单worker/单fork、低优先级、Kotlin进程内编译；仅任务临时Gradle384MiB堆/256MiB元空间、测试256MiB，未改项目/系统默认。通过既有Run-PreviewChecks.py、--keep-fixtures运行：

```text
:studio-desktop:test --tests *StudioTokenContrastTest
-Dorg.gradle.jvmargs=-Xmx384m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2
```

[完整命令](evidence/TOKEN_TEXT_CONTRAST_20261026/control/COMMAND.json)、[Gradle实际参数与执行摘要](evidence/TOKEN_TEXT_CONTRAST_20261026/run/summary.json)、[原始逐例](evidence/TOKEN_TEXT_CONTRAST_20261026/run/test-cases.json)、[原始Gradle输出](evidence/TOKEN_TEXT_CONTRAST_20261026/run/gradle.log)。实际编译新生产/测试代码，**1唯一方法PASS，0失败/错误/跳过，sourceStable**。方法7.037秒，runner77.149秒；没有重跑旧测试、基线或因矩阵样本重复累计方法。

方法`actualFooterTokenTextIsReadableWithoutChangingCountsWarningsOrAccentFills`实际渲染生产DesktopStudioFooter及其正/负StudioTokenBar，合成计数，不创建tokenizer/provider/workspace。两模式×000000/FFFFFF/默认7DB5FF×300/660宽度×正常1/152与警告461/超限513，共24个离屏场景仍只计1方法。核对实际TextLayoutResult前景及文字区域像素对比≥4.5、两行确切计数/警告文案、progressBarRangeInfo和原强调色/超限填充像素，覆盖窄布局换行。不是只检查一个颜色辅助函数，也不将离屏像素称为Windows实机人工验收。

[源码/实际构建输入映射](evidence/TOKEN_TEXT_CONTRAST_20261026/SOURCE_EXECUTION_MAP.json)逐项区分Git原始字节与CRLF工作字节；只允许UTF-8文本的CRLF/LF差异，保留双方SHA。旧固定包、证据和用户否决不回写。

## 资源与准备记录

启动前H空闲4,162,404,352、C空闲12,431,806,464字节；H预算1,586,993,010、C预算805,306,368。Available1,156,521,984、提交余量约6.65GB，启动前foreign0。18次采样、监测异常0；最低Available365,629,440、本组采样private峰值1,323,749,376字节，无OOM或预设连续严重压力。采样可能漏峰值，不作为通用内存保证。

原日志有一次“foreign/INTERVENTION_REQUIRED”：在先遍历父子树、后列Java进程的顺序采样中，新生PID10752一度未知，下一采样纳入本次自有树。完整父ID/创建时间保留在[采样解释](evidence/TOKEN_TEXT_CONTRAST_20261026/MONITOR_OBSERVATION.json)和原memory-samples，不删除警告或谎称每个快照foreign均为0；无持续外部owner冲突，无强杀进程/系统改动。

证据收集辅助脚本的两次准备失败另列保留：先漏.json、后漏.md的CRLF后缀白名单，发生在已PASS回归之后，证据新目录保持空。随后改成UTF-8解码＋仅换行差异核对，保留两版失败脚本及记录；未改产品或重跑测试求绿。默认空白检查对保留CRLF的CR报警，使用`git -c core.whitespace=cr-at-eol … --check`接受CRLF。原始Gradle行尾空格及runner结尾空行仍触发格式检查；只将本证据组`control/runner.log`、`run/gradle.log`两份原log精确排除空白格式检查，raw不格式化，其他路径仍检查；全部23份的SHA和Git原字节核对没有排除。

## 交付边界

[新证据清单](evidence/TOKEN_TEXT_CONTRAST_20261026/RAW_MANIFEST.json)只含本轮合成回归/控制记录，没有截图像素、用户资料、凭据、二进制或安装包。fixture显式保留，无清理。未创建应用映像/安装包、Release/tag或更新元数据；现有48eed候选和日常115均不含26ef增量，未替换/启动/操作任何用户窗口或日常资料。

当前工程窄修自动通过，不自行宣布全主题或项目验收。原Project限定结论不扩签，q09 FAILED/root UNKNOWN、资产/编辑功能未验、115体验与Prompt对比各PENDING、PRODUCT_HOLD / NOT RC继续保持。本轮停止于源码/自动结果/证据交付，不自动打包、送Project、发布、更新日常安装、清理或启动其他需求。
