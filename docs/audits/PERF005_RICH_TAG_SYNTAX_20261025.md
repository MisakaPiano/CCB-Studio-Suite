# PERF-005：富文本属性标签固定表达式复用

2026-10-25。基线输入/执行[73259d51414ccd95d9a2f911c845084d00215bc2](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/73259d51414ccd95d9a2f911c845084d00215bc2)，应用/最终执行[9987d85638c38bf64641f3c19bd4836084c905a5](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/9987d85638c38bf64641f3c19bd4836084c905a5)。承接原PERF-005连续授权，不是五项登记触发的新任务。无安装包、tag、版本、签名/更新元数据或发布。

原`TypesetLayout.patch`先处理简单标签，再为每个属性或未知标签尝试重新编译同一语法Regex。现仅新增1个private lazy `richTagSyntax`、改1处引用，**原模式字节/选项/matchEntire和所有其他生产文本不变**。选择有界固定准备复用，无用户输入/结果/Matcher缓存；每次仍完整匹配/解析，原lowercase/trim、别名、引号、未知/坏标签、转义方括号、Unicode迭代、样式栈、嵌套/长度限制、数值有限/非0默认与夹取、无效颜色语法保留均不改。不涉及Prompt/请求权威、字体、测量、排版、渲染或编辑UI开发。

lazy委托在单例初始化分配，Pattern在首次属性/未知标签路径用到时常驻；首用/初始化、常驻成本、峰值堆/GC/RSS未测，不能说所有路径零新增成本。纯文本和简单标签原分支不触发表达式；不为对照路径的墙钟波动编造优化归因。

最终 **3唯一方法PASS，0失败/错误/跳过，源码稳定**；基线1方法另列，规模/路径/轮次/边界断言和线程不相加：

- 配对实际tokens方法1：纯文本、简单标签、属性标签各32/128/512组，相同Token类下旧参考tokens+patch仅限定Token类型，其他内容逐字相同。比较全部Token字段、样式、有序摘要、文档字节和调用者样式，另有硬编码字符/字号/字重向量。
- 别名/边界方法1：引号、大小写、未知和未闭合标签、escaped bracket、pop/base、plain、数值0/NaN/Infinity/负值与夹取、无效颜色保持、font截长256、混合Unicode/补充平面/孤立代理字符；999层接受/1000层拒绝，100000字符接受/100001拒绝，原错误class/message相同。直接helper坏输入测试不等于文档准入。
- 并发方法1：2个自建线程各100轮，独立结果与样式栈、后续普通文本保持原base；正常shutdown/await，不代签所有首次初始化竞争/全部环境/长期线程安全。

合成单页文档实际`TypesetDocument.validate`与序列化<=4MiB在计数外通过。属性输入2,688/10,752/43,008 UTF16字符，文档4,098/12,738/47,298B；纯文本与简单标签容量也单列于ANALYSIS。**计数仅实际tokens调用**，包括字符/Token生成，不含文档验证、断言/摘要/JSON、字体/布局/绘制/窗口或真实资产入库。TypesetLayout单例初始化在计数外，无真实用户图片或服务。

每种实现2次预热、4轮同JVM交替的最终中位数：

|组数|路径|旧/新线程累计分配B|旧/新墙钟ms|
|---|---|---:|---:|
|32|纯文本|13,040 / 13,040|0.023 / 0.014|
|32|简单标签|26,120 / 26,120|0.248 / 0.066|
|32|属性标签|324,312 / 115,368|1.383 / 0.577|
|128|纯文本|52,960 / 52,960|0.060 / 0.028|
|128|简单标签|105,208 / 105,208|0.351 / 0.223|
|128|属性标签|1,297,384 / 462,824|3.232 / 1.108|
|512|纯文本|206,888 / 206,888|0.283 / 0.160|
|512|简单标签|415,808 / 415,808|3.473 / 0.774|
|512|属性标签|5,100,464 / 1,844,144|9.509 / 3.100|

属性路径本样本分配少约64%，纯文本/简单标签分配完全不变。对照中未修改路径的墙钟亦变化较大，具体JIT/噪声因果UNKNOWN，不能把这些墙钟全部归因于Regex复用或宣称各路径稳定提速。基线独立JVM的512属性6.772ms、最终旧参考9.509ms均原样保留，没有调预热/筛样本；主要收益结论限分配。CPU时钟粗、0不是零CPU，不外推完整编辑器/UI/RSS/全Windows性能。

Windows/JDK17/Gradle9.1，离线/严格依赖、低优先级/单worker、测试2处理器/256MiB。基线14:47:56 JST起15.022秒、最终14:49:38 JST起19.915秒，含Gradle，非上表局部性能。原始参数记录UTC，命令形态：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf005-rich-tag-syntax-baseline-20261025 -- :studio-core:test --tests *RichTagSyntaxPerformanceTest
python.exe -B tools/Run-PreviewChecks.py --phase perf005-rich-tag-syntax-final-20261025 -- :studio-core:test --tests *RichTagSyntaxPerformanceTest
```

两次均经最新Desktop Java owner两次30秒间隔空边界启动；14:49:39/14:50:30 JST观察为空，非全程静默或CPU归因证明。成功所属夹具清理COMPLETE、0payload文件，无额外清理/强杀；旧失败/未知/日常资料/备份不动。未重跑竖排4、颜色5、旧接受批次9/原生/启动或其他旧性能基线。

[SOURCE_MAP](evidence/PERF005_RICH_TAG_SYNTAX_20261025/SOURCE_MAP.json)固定原始输入/源码精确差异与旧参考；[ANALYSIS](evidence/PERF005_RICH_TAG_SYNTAX_20261025/ANALYSIS.json)保留全部样本/计数；[runs](evidence/PERF005_RICH_TAG_SYNTAX_20261025/runs)含原始逐例、参数、输入/清理；[inputs](evidence/PERF005_RICH_TAG_SYNTAX_20261025/inputs)含实际生产字节。RAW **87,915B** SHA256 `3bca24af68d31a77a4a9162e5ecf28eb4884ed6fad4de1580746dd5bad0e462b`，25项+manifest共26成员，CRC/逐项SHA通过。

实现/本段自动验证完成，独立NOT REQUESTED、用户/包NOT TESTED，[新复审仅准备](../../handoff/PERF005_RICH_TAG_SYNTAX_REVIEW_READY_20261025.md)，无新外发。原16项/两段补充/日期/颜色/竖排/原Desktop报告各自固定边界不改；旧失败、collector准备失败/异步观察更正、目录候选未采用和用户否决保留。[执行者路径错误](OPERATOR_PREPARATION_PATH_ERROR_20261025.md)前态/精确UTC UNKNOWN与禁止再次访问约束保持。资产/编辑USER_NOT_TESTED、115体验/Prompt对比PENDING、q09 FAILED/root UNKNOWN、PRODUCT_HOLD / NOT RC不改。无真实服务/凭据/用户资料、暂停扫描恢复、日常安装/发布/安全/MSI或Desktop写入；不启动unified-image-tool/后续资产新需求。

持续授权继续有效，普通检查点不全停。下一独立低风险定位同PERF-005竖排标点类别`cls`按字符反复建立固定字符列表的成本：先测实际vertical layout与关闭标点优化的对照，保护全部类/advance/换行/尺寸输出，不先改分类规则或字体/渲染。Desktop资源优先，有证据才最小常量复用，不重跑本段。
