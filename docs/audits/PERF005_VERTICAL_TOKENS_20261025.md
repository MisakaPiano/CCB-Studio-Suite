# PERF-005：竖排字符投影固定准备复用

2026-10-25。基线输入/执行[c6efb5e36827311da9e919521723a0dd7fce3f97](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/c6efb5e36827311da9e919521723a0dd7fce3f97)，应用/最终执行[e9ae9f32498786326ae41552748db346604d97bd](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/e9ae9f32498786326ae41552748db346604d97bd)。无新安装包、tag、版本、签名/更新元数据或发布。本段承接已经进行中的PERF-005；用户五项待办登记没有启动新的性能范围或功能。

`TypesetLayout.verticalTokens` 原来每个字符创建两个固定引号集合，进入后备分支时重新编译Unicode标点表达式、重建固定符号的codepoint字符串列表。现在仅两处改为精确的双成员直接比较，后备条件复用private lazy `Regex("\\p{P}")` 和原符号字符串的Set。生产差异严格限于2个lazy常量和3条条件，具体模式/选项/符号字节、短路顺序、输出与错误保持。原force映射、presentation判断、uprightFallback、每次调用自己的引号开合/省略号状态不变；没有输入/输出缓存，未改变富文本解析、字体、测量、标点压缩、排版、绘制或编辑UI。

选择有界常量复用，没有缓存用户输入；lazy委托在单例初始化分配，Regex/Set在后备分支首次用到时保留。首用/初始化与常驻成本、峰值堆/GC/RSS未测，不能声称所有路径零新增成本。基线实际显示映射控制路径也有重复准备成本，因此在同一helper内同时去掉引号集合创建；不是新功能或新任务。未采用此前收益不稳的目录遍历候选，仍保留其负面结果。

最终 **4唯一方法PASS，0失败/错误/跳过，源码稳定**；基线2方法、3参考文件、输入规模、轮次与线程数另列不累加：

- 实际投影配对方法1：128/1024/8192 codepoint，映射控制与CJK/Hangul/Greek/emoji/符号后备路径。同一实际Token类，旧实现参考只复制chars/force/uprightFallback/presentation/verticalTokens且仅限定Token类型；原算法逐字相同。实际与旧输出的所有Token字段、样式、输入快照和文档字节一致；另有硬编码ASCII/逗号/三点/引号/中文向量。
- 既有固定来源元数据方法1原样保留：TS-V01、TS-V02、TS-R01-vertical三个公开合成JSON，读取时先验证原始SHA，按段比较已接受的displayCh/rotate/fontSize和数量。未新增advance、换行位置、像素、字体或全编辑器保真结论。
- Unicode/样式/边界方法1：混合全半角引号、词内撇号、2～5点、presentation/upright例外/符号、补充平面/组合字符、富文本样式、自定义Token、重复调用引号重置；空helper Token的异常class/message与原实现保持。空Token是直接helper非法输入测试，不声称其可被文档准入。
- 并发方法1：2个自建线程各100轮，结果与旧参考一致且为独立列表，输入不变；正常shutdown/await。不代签所有首次初始化竞争、长时间或全部Windows环境。

合成文档使用实际`TypesetDocument.validate`、序列化<=4MiB及UTF16长度<100000检查，均在计数外。文档字节为控制1,388/2,580/12,138B、后备1,544/3,823/22,069B。输入由实际tokens预先生成，**计数范围仅verticalTokens调用**，验证、摘要和JSON记录在计数外；TypesetLayout初始化亦在计数外，不是字体/布局/渲染/冷启动测试，也不是资产入库、真实图片/服务或整个文档验证耗时。

每种实现2预热、4轮同JVM交替的最终配对中位数：

|输入Token|路径|旧/新累计线程分配B|旧/新墙钟ms|
|---|---|---:|---:|
|128|映射控制|54,040 / 9,112|0.116 / 0.052|
|128|后备|410,504 / 27,368|1.895 / 0.044|
|1024|映射控制|425,200 / 69,936|1.358 / 0.141|
|1024|后备|3,211,200 / 215,392|4.161 / 0.218|
|8192|映射控制|3,389,848 / 549,792|6.760 / 1.221|
|8192|后备|25,465,236 / 1,715,680|12.595 / 1.557|

本样本线程累计分配少约83%～93.3%，局部墙钟下降。基线独立JVM的8192后备25,678,984B/13.971ms与最终旧参考有所差异，原始行全保留，不选择有利轮次、不作JIT因果断言。CPU时钟粗，0不是无CPU。没有UI/RSS/全局响应或普遍Windows性能结论。

Windows/JDK17/Gradle9.1，严格依赖校验/离线、低优先级、单worker，测试2处理器/256MiB。基线05:24:42.684386UTC起16.132秒，最终05:36:03.023694UTC起20.225秒，包含Gradle，不是上述局部性能样本。完整参数/执行来源见raw，命令形态：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf005-vertical-tokens-baseline-20261025 -- :studio-core:test --tests *VerticalTokenProjectionPerformanceTest
python.exe -B tools/Run-PreviewChecks.py --phase perf005-vertical-tokens-final-20261025 -- :studio-core:test --tests *VerticalTokenProjectionPerformanceTest
```

两次均按最新Desktop Java owner的两次30秒间隔空边界启动，最终后边界05:36:25UTC为空，非全程静默/CPU归因证明；不按旧PID退出认定空闲。成功所属夹具清理COMPLETE、0payload文件；无额外删除/强杀，旧失败、未知归属、备份、日常安装不动。未重跑旧接受批次9/原生启动/颜色回归。

[SOURCE_MAP](evidence/PERF005_VERTICAL_TOKENS_20261025/SOURCE_MAP.json)固定精确差异/输入/生产与参考等价；[ANALYSIS](evidence/PERF005_VERTICAL_TOKENS_20261025/ANALYSIS.json)含全部规模中位数和方法计数；[runs](evidence/PERF005_VERTICAL_TOKENS_20261025/runs)含原始逐例、参数、源码/清理记录；[inputs](evidence/PERF005_VERTICAL_TOKENS_20261025/inputs)保留实际生产原字节和3个未变来源JSON。RAW **99,994B**，SHA256 `de9d28a668688cd38672624267c01f27330dd4dafcdaa7df1fa07bd881841290`，30项+manifest共31成员，CRC/逐项SHA通过。

首次证据收集脚本误认为runner的source-before包含docs夹具键，实际只覆盖模块/工具，故发生KeyError；发生在成功测试之后，0新增测试。首次脚本、错误记录及当时部分文件SHA保留，修正脚本先核全部所属部分文件的原字节，复用同目录且不覆盖不同内容。来源JSON身份改由实际JUnit读取并打印的SHA+两次固定Git字节核对，不能声称runner启动前快照覆盖这些docs文件。未重跑测试来消除准备失败。

实现与本段自动检查完成；用户/包NOT TESTED，独立NOT REQUESTED，[新复审只准备](../../handoff/PERF005_VERTICAL_TOKENS_REVIEW_READY_20261025.md)。原16项fa58a50、两段补充b5e84da、日期/颜色报告和原Desktop报告保持各自固定边界，不扩大或重发。旧失败/用户否决、目录试验未采用、[执行者路径错误前态/精确UTC UNKNOWN和禁止再访问约束](OPERATOR_PREPARATION_PATH_ERROR_20261025.md)、q09 FAILED/root UNKNOWN、资产/编辑USER_NOT_TESTED、115体验/Prompt对比PENDING、PRODUCT_HOLD / NOT RC保持。无真实服务/私人凭据/用户资料、暂停扫描恢复、日常安装/发布或Desktop写入；unified-image-tool等待用户测试后另定范围。继续原已授权范围，资源和安全边界不变。
