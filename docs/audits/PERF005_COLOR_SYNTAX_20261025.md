# PERF-005：功能色解析复用固定表达式

2026-10-25。基线输入/执行[1a007ab11f7fa50520d57387d2000e3d2f1cebf9](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/1a007ab11f7fa50520d57387d2000e3d2f1cebf9)，应用/最终执行[b8b225d6951617b35b8f5e3d5b82950511681e3c](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/b8b225d6951617b35b8f5e3d5b82950511681e3c)。无新安装包、版本/tag、签名/更新元数据或发布。

`TypesetColors.parse` 原来每次功能色解析重新编译函数语法表达式、每次现代空格语法重新编译分隔表达式、每个数值分量重新编译数值表达式。现只新增3个private lazy固定Regex并替换3处引用，**表达式字节、选项、matchEntire/split/matches用法不变**。不缓存输入、颜色结果或Matcher；仍逐次完整解析/验证。原长度256限制、trim/Locale.ROOT、色名字典、HEX/透明分支、RGB/HSL/HWB规则、数值有限性/单位/夹取/转换、异常类型/文字完全保持，未扩大支持格式或实现编辑新功能。

3个lazy委托在单例初始化时产生，固定Pattern在对应分支首次用到时保留；这是有界常量复用。首用/冷启动、委托/Pattern常驻成本、峰值堆/GC/RSS未测，不声称所有路径零新增成本。选择惰性方式是避免HEX/色名调用主动编译未用到的功能色表达式；没有用户派生的全局缓存。旧完整解析器仅改private对象名作为测试参考；所有其他生产和原Golden资源/容差原样，精确文本替换及原字节/固定Git校验见SOURCE_MAP。

最终 **5唯一方法PASS，0失败/错误/跳过，源码稳定**：

- 新配对解析/文档快照方法1：HEX/色名/透明控制及功能色，旧新ARGB有序摘要与基线一致。
- 新精确结果方法1：162个固定来源值、额外有效样本、13种无效输入，逐个比较ARGB或异常class/message，另有5个硬编码ARGB断言；包括长度/未知空间/坏分量/非有限值/换行等。
- 新并发方法1：2个自建线程×100轮×7值，复用表达式仍得到各自正确结果，正常shutdown/await；不代签所有初始化竞争或长期/全平台线程安全。
- 既有 `TypesetColorTest` 2：162个独立Source Canvas样本与富文本值/1×1 raster结果，以及错误色不能静默用上次样式。保留原premultiplied像素最多1单位容差，未放宽；不是162个JUnit方法或完整编辑器渲染验证。

基线1方法另列，不相加；未重跑旧批次9/启动、日期/标签/导出性能基线。合成单页文档32/128/500文字层、每层4颜色，实际 `TypesetDocument.validate` 和序列化<=4MiB在计数外通过：HEX/色名文档18,798/73,223/284,365B，功能色20,562/80,283/311,965B。没有资产payload/真实图片/字体载入或存储项目准入结论。**计数范围只是相同map结果列表包装下的实际parse调用，非整个document.validate、字体排版、绘制、UI或冷启动。**断言、ARGB摘要、文档字节/JSON均在计数外。

各模式2次预热、4轮同JVM交替，最终配对中位数：

|文字/颜色调用|颜色组|旧/新累计线程分配B|旧/新墙钟ms|
|---|---|---:|---:|
|32/128|HEX/色名/透明|56,624 / 56,624|0.335 / 0.146|
|32/128|功能色|2,331,224 / 424,888|4.554 / 1.065|
|128/512|HEX/色名/透明|225,496 / 225,496|0.658 / 0.649|
|128/512|功能色|9,229,952 / 1,698,624|9.827 / 3.207|
|500/2000|HEX/色名/透明|881,672 / 881,672|2.241 / 2.215|
|500/2000|功能色|36,052,956 / 6,635,372|17.129 / 6.046|

功能色线程累计分配约少81.6%～81.8%，本样本局部墙钟下降。HEX控制分配不变，未修改分支的小样本墙钟差异不归因于Regex复用，JIT/噪声等具体原因未核定；不宣传HEX或所有Windows提速。CPU计时粗，0不是无CPU；没有完整UI/峰值堆/RSS/物理IO/长期结论。

Windows/JDK17/Gradle9.1，离线严格依赖校验、低优先级/单worker、测试2处理器/256MiB；完整runtime/runpy/参数/UTC在raw，命令形态：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf005-color-syntax-baseline-20261025 -- :studio-core:test --tests *TypesetColorSyntaxPerformanceTest.measuredActualColorSyntaxKeepsAdmittedDocumentValuesAndSnapshots
python.exe -B tools/Run-PreviewChecks.py --phase perf005-color-syntax-final-20261025 -- :studio-core:test --tests *TypesetColorSyntaxPerformanceTest --tests *TypesetColorTest
```

基线04:43:00Z起20.808秒，最终04:52:18Z起23.745秒含Gradle，非上述性能样本。两轮均按最新Desktop owner门的两次30秒间隔空边界启动，后边界05:00:12Z为空，不是全程静默/CPU归因证明，不按旧PID退出判断空闲。成功所属夹具清理均COMPLETE、0payload文件；旧失败/未知/备份/日常资料不动，未强杀或改Desktop。

[源码/实际输入/精确差异/资源身份](evidence/PERF005_COLOR_SYNTAX_20261025/SOURCE_MAP.json)、[全部样本/计数](evidence/PERF005_COLOR_SYNTAX_20261025/ANALYSIS.json)、[原始逐例/参数/输入/清理](evidence/PERF005_COLOR_SYNTAX_20261025/runs)、[原字节与未变Golden/字典](evidence/PERF005_COLOR_SYNTAX_20261025/inputs)、[限制及历史准备记录](evidence/PERF005_COLOR_SYNTAX_20261025/ATTEMPTS_AND_LIMITS.md)。RAW **91,403B** SHA256 `5956ce94b63d548fd2def374d0293cb29f7ccbb0da688f6062577db4021d7fd1`，31清单项+manifest共32成员，CRC/逐项SHA核对通过。

## 异步状态登记措辞更正

3d1251f文档提交于04:43:39UTC，写“尚未执行”时执行者尚未读取后台结果；原始summary证明基线04:43:00.224508UTC开始、约04:43:21.032508UTC已经结束。因此当时准确状态应为**PENDING_OBSERVATION（结果尚未读取）**，不能据无观察断言0执行。当前记录更正、旧提交与raw保留，此更正不产生新测试/重跑，不混淆作业日期前两门真正0执行。实际方法计数仍基线1、最终5分列。

实现/本段自动检查完成；独立NOT REQUESTED、用户/包NOT TESTED，[复审仅准备](../../handoff/PERF005_COLOR_SYNTAX_REVIEW_READY_20261025.md)，不新外发。原16项fa58a50、两段补充b5e84da及原Desktop报告各自边界不扩大。旧失败/否决/遍历试验未采用、[执行者路径错误前态/精确UTC UNKNOWN及约束](OPERATOR_PREPARATION_PATH_ERROR_20261025.md)、q09 FAILED/root UNKNOWN、资产/编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、PRODUCT_HOLD / NOT RC保持。不再访问误写路径或扫描无关文件，后续目标根/归属/冲突/链接预检保持。没有真实图片/私人凭据/服务、暂停扫描恢复、日常安装/安全/信任链或发行变化；本段不是unified-image-tool整合或编辑用户验收。继续既有性能范围的低负载源码定位，有证据才最小改动、已有机制不重复造轮子，Desktop资源优先。
