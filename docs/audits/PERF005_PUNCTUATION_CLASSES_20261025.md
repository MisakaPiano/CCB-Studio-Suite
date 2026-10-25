# PERF-005：竖排标点类别固定集合复用

2026-10-25。失败初轮[772a66c8cbee574cf82aa4e2688eaa4f1d13af86](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/772a66c8cbee574cf82aa4e2688eaa4f1d13af86)，修正基线输入/执行[d82f804d73557ab4e6fa633f2655835fcc3e6d03](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/d82f804d73557ab4e6fa633f2655835fcc3e6d03)，应用/最终执行[3d9abb0e10a61a230e17542a8cc22b5c1c0992e3](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/3d9abb0e10a61a230e17542a8cc22b5c1c0992e3)。无安装包、tag、版本、签名/更新元数据或发布，承接已授权PERF-005。

原private `cls`对每个投影字符最多重建五类固定codepoint字符串列表。现仅增加五个private lazy Set、将五处列表成员判断改为对应集合；原字符串字节、类别优先顺序、when/默认类别保持，**其他生产文本逐字不变**，包括tokens/verticalTokens、reduction数组/数值/公式、advance/压缩/换列/尺寸、字体/渲染。没有输入/输出缓存；选Set只复用有界固定字符成员，不变更布局算法。

五个lazy委托在单例初始化产生，固定集合/codepoint字符串在对应类别首次用到时保留。冷/首用与常驻成本、峰值堆/GC/RSS未测，不声称所有路径无成本。关闭标点优化分支不调用cls，作为控制。选择精确固定准备复用，未重开字体/编辑UI/像素行为或引入用户派生全局缓存。

## 实际执行与失败保留

|阶段|源码|唯一方法|结果|
|---|---|---:|---|
|错误测试向量基线|772a66c|1|1FAIL，取数前失败，无性能样本|
|修正测试向量基线|d82f804|1|PASS，0失败/错误/跳过|
|最终候选|3d9abb0|3|PASS，0失败/错误/跳过|

各阶段不相加，不把输入规模、边界断言、线程/轮次当方法。初轮独立10px A/逗号/中文向量把原`part(prev,false)/part(next,true)`的.12/.14侧向写反：原预期12/10.8/10.6，实际/正确为12/10.6/10.8。d82f804仅纠正测试预期，1e-9精度及产品源码不变；未放宽判定或把失败归因产品/防护。旧源码/raw/原草稿和失败现场`.local/disposable/gradle-tests-31a2a3258b1f46e3b424dfffdf4d1408`保留，runner状态RETAINED_FAILED_FIXTURES、0删除；后两次成功清理COMPLETE、0payload文件。失败不改判为通过。

最终三方法：

- 配对实际vertical layout：128/1024/8192 codepoint、标点优化关闭/开启，旧参考使用相同真实Token/Cell/Line/Layout类型。逐项比较完整结构（所有Token/样式/类别/advance/列数/列宽/整体尺寸），布局摘要与修正基线一致，文档字节不变；独立类/压缩量/尺寸向量和关闭时每字符12也通过。
- 类别/参数边界：五类符号字符串、富文本/引号/省略号/空文本/换行/混合Unicode，standard/compact/loose、宽10/48/384、letterSpacing=3、字号6/1000与lineHeight=2，旧新完整结构一致、输入不变。不是逐字形像素或全部字体结论。
- 并发局部状态：2个自建线程各50轮，独立cells/lines结果与旧参考一致，正常shutdown/await，不代签所有首次lazy竞争、所有Windows或长期稳定性。

旧参考chars/cls/reduction/layout仅限定真实数据类及tokens/verticalTokens/measure依赖，原算法逐字相同；保留的水平measure分支在这些测试中**从未执行，所有输入均vertical=true**。实际竖排布局不调用字体测量；FontRenderContext单例初始化位于计数外。因此本段包含tokens/竖排投影/类别/压缩/换列的实际layout计算，仍不是绘图、字体选择、窗口、资产准入或用户编辑体验验证。

合成文档实际`TypesetDocument.validate`、UTF16<=100000、序列化<=4MiB在计数外检查。8192 codepoint对应8,937 UTF16字符，文档关闭22,073B/开启22,072B。全部断言/摘要/JSON/文档序列化在计数外。两种实现各2预热、4轮同JVM交替，最终中位数：

|字符|标点优化|旧/新累计线程分配B|旧/新墙钟ms|列数|
|---|---|---:|---:|---:|
|128|关闭|41,992 / 41,992|0.060 / 0.066|13|
|128|开启|1,087,648 / 52,928|1.487 / 0.077|12|
|1024|关闭|330,872 / 330,872|0.314 / 0.319|103|
|1024|开启|8,735,664 / 419,916|5.579 / 1.086|94|
|8192|关闭|2,620,304 / 2,620,304|2.968 / 2.950|820|
|8192|开启|69,843,272 / 3,327,304|15.055 / 2.590|745|

开启路径本样本分配少约95.1%～95.2%，关闭控制的配对分配不变。两种标点模式本来就有不同换列结果，不能把开启/关闭间差异当作回归；要求的是同模式旧新完全一致。基线独立JVM8192开启69,187,992B/17.142ms与最终旧参考69,843,272B/15.055ms有所波动，保留所有行，不调整预热/选择有利轮次；关闭路径的小时间波动不归因修改，JIT/噪声具体因果UNKNOWN。CPU时钟粗、0不等于零CPU；不外推完整UI/RSS/字体/像素/全部Windows响应收益。

Windows/JDK17/Gradle9.1、离线/严格依赖、低优先级/单worker，测试2处理器/256MiB。初轮14:55:05 JST起15.522秒、修正14:57:01起15.402秒、最终15:00:31起20.127秒，包含Gradle，非表内局部计数。命令形态（完整UTC/参数在raw）：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf005-punctuation-classes-baseline-20261025 -- :studio-core:test --tests *VerticalPunctuationClassPerformanceTest
python.exe -B tools/Run-PreviewChecks.py --phase perf005-punctuation-classes-corrected-baseline-20261025 -- :studio-core:test --tests *VerticalPunctuationClassPerformanceTest
python.exe -B tools/Run-PreviewChecks.py --phase perf005-punctuation-classes-final-20261025 -- :studio-core:test --tests *VerticalPunctuationClassPerformanceTest
```

最终门控曾让行Desktop新Java owner22472/48516，不按旧PID退出判断。两次30秒空边界后启动，15:01:38 JST后边界为空，不证明全程静默/CPU归因；没有终止Desktop进程。竖排4/富文本3/颜色5/旧接受批次9/原生/启动均未重跑。

[SOURCE_MAP](evidence/PERF005_PUNCTUATION_CLASSES_20261025/SOURCE_MAP.json)固定三阶段来源、实际原字节/精确局部差异与旧参考；[ANALYSIS](evidence/PERF005_PUNCTUATION_CLASSES_20261025/ANALYSIS.json)包含所有样本/方法计数/失败保留；[runs](evidence/PERF005_PUNCTUATION_CLASSES_20261025/runs)保留三阶段原始逐例、参数、输入和清理；[inputs](evidence/PERF005_PUNCTUATION_CLASSES_20261025/inputs)保留原生产字节。RAW **125,974B** SHA256 `d026c15e213282c99b4455492d2b6550cf1a7a1208eb1e2f89eb7e0b6efc64a9`，34项+manifest共35成员，CRC/逐项SHA通过。

## 异步状态再次误写及纠正

[81420ac55ab6045a99671f62c70186835ab67c67](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/81420ac55ab6045a99671f62c70186835ab67c67)在15:00:55 JST提交“尚未执行”，执行者当时尚未读取后台最终结果；raw证明15:00:31.057506开始、20.127秒后约15:00:51.184506已结束。此时准确说法是**PENDING_OBSERVATION（结果尚未观察）**，不是0执行。这是3d1251f之后同类记录错误再次发生，保留两次旧提交与raw，不以新测试洗掉错误；此更正0追加执行。当前记录更正，后续异步轮询尚未读到时仅记观察状态，只有明确门控0执行回执才能说未执行。旧81420ac输入模块/工具与3d9abb0等价，但不是最终测试执行来源；以raw的3d9abb0为准。RAW封存于此次措辞核对前，纠正事实在本报告及当前任务记录中另列，不改封存归档或旧commit。

实现/本段自动检查完成，用户/包NOT TESTED、独立NOT REQUESTED，[新复审只准备](../../handoff/PERF005_PUNCTUATION_CLASSES_REVIEW_READY_20261025.md)，未外发。原16项/两段补充/日期/颜色/竖排/富文本/原Desktop报告固定边界保持，旧失败、用户否决、目录候选未采用及[路径错误前态/精确UTC UNKNOWN和禁止再访问](OPERATOR_PREPARATION_PATH_ERROR_20261025.md)不改。资产编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、q09 FAILED/root UNKNOWN、PRODUCT_HOLD / NOT RC保持。无日常安装/私人资料/凭据/真实服务/暂停扫描恢复/发布/安全/MSI或Desktop写入。

持续授权继续，普通检查点不停止。下一步仅低负载核对本函数仍逐字创建的reduction固定数值表，先承接本段实际前值，不重复跑基线；如采用则必须保留同一公式/数值/顺序/全部布局输出、无数组外泄或写入，新的固定输入/定向回归另列，Desktop优先，不扩展需求。
