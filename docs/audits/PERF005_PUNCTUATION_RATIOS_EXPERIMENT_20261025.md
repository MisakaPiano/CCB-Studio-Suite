# PERF-005：压缩数值表候选未采用，原实现已恢复

2026-10-25。前值应用[3d9abb0e10a61a230e17542a8cc22b5c1c0992e3](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/3d9abb0e10a61a230e17542a8cc22b5c1c0992e3)/证据838b087；数值表候选/实际执行[e8378806ce950bf3d22fff1dc3e71bc7c0d95548](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/e8378806ce950bf3d22fff1dc3e71bc7c0d95548)，恢复[2c352961f2023464bbec0475722b78cdaa52d4ce](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/2c352961f2023464bbec0475722b78cdaa52d4ce)。**功能3方法PASS，性能HOLD / NOT ADOPTED**。无新包、tag、版本、签名/更新元数据或发布，不把候选写成已交付优化。

候选仅将`reduction`的compact/loose/standard三个固定8项DoubleArray改为private lazy数组、替换三处when引用。原8项数值/顺序/默认分支/part索引/公式及所有其他生产文本逐字相同；数组在正常实现中只被局部part读取，没有写入、返回或传出外部API，无用户输入/输出缓存。这是源码只读所有权，不声称DoubleArray普遍不可变或具备反射安全。lazy委托/首次与常驻三数组成本、峰值堆/GC/RSS未测。

直接复用838b087固定的3d9abb0实际current行作为前值，**没有新增基线执行**。新标准/关闭场景的完整文档字节数、UTF16长度、列数与布局SHA均与前值吻合；compact/loose新增配对。前次3方法仍属于前次，不加进本次3。原始前值逐例副本有固定Git字节核对，复制证据不是新执行。

本次仅一轮新定向执行，**3唯一方法PASS、0失败/错误/跳过、源码稳定**：

- 实际vertical layout配对：128/1024/8192字符、关闭/standard/compact/loose，每种旧新2次预热、4轮同JVM交替；相同实际Token/Cell/Line/Layout类，完整结构/样式/类别/advance/列/尺寸/文档字节保持。计数包括tokens/投影/类别/压缩/换列，不是只测数组或整个编辑器。
- 数值/边界：原类别/富文本/空文本/换行/Unicode，宽10/48/384、三种tightness、字号6/1000、lineHeight2/spacing3；独立10px向量standard12/10.6/10.8、compact12/10/10.2、loose12/11.2/11.4和未知helper回退原standard，1e-9不变。未知helper值不等于有效文档准入。
- 并发读取：2个自建线程各50轮轮换三表，独立行状态和完整旧结果一致，正常shutdown/await；不代签全部首次初始化竞争、长期或全部平台。

参考chars/已缓存类别集合/cls/reduction/layout从3d9abb0逐字复制，仅限定真实类型/依赖；水平measure分支保留，但所有输入vertical=true、从未调用水平字体测量。合成文档实际validate、UTF16<=100000、序列化<=4MiB及全部断言/摘要/JSON在计数外。不是字体加载/渲染/像素/窗口/真实图片或资产入库测试。

最终配对中位数（全部行见ANALYSIS）：

|字符|场景|旧/新累计线程分配B|旧/新墙钟ms|
|---|---|---:|---:|
|128|关闭|41,992 / 41,992|0.148 / 0.150|
|128|standard|52,928 / 42,768|0.204 / 0.123|
|128|compact|52,928 / 42,768|0.102 / 0.074|
|128|loose|52,152 / 41,992|0.201 / 0.076|
|1024|关闭|330,872 / 330,872|0.988 / 0.496|
|1024|standard|419,216 / 337,376|1.424 / 0.540|
|1024|compact|419,216 / 337,376|1.478 / 0.600|
|1024|loose|412,712 / 330,872|0.917 / 0.670|
|8192|关闭|2,620,304 / 2,620,304|4.524 / 4.462|
|8192|standard|3,327,304 / 2,672,024|3.289 / 2.912|
|8192|compact|3,327,304 / 2,672,024|2.664 / 2.601|
|8192|loose|3,275,584 / 2,620,304|2.636 / 3.613|

开启时分配少约19%～20%，8192少655,280B；关闭分配不变。最大loose墙钟约慢37%，具体因果UNKNOWN；小样本/未改控制也有波动，不能证明lazy间接访问、JIT或环境中的任一原因。没有追加预热、挑选轮次或重跑求通过。当前证据不足以将此候选当作响应改善，故保留原数值表实现；不能把功能通过或低分配等同于性能验收。

2c35296恢复原表表达式，移除本候选新引入的默认基准类。全部modules/tools输入与已实际通过的3d9abb0相同，应用源码并未保留这次三数组改动；原5类字符Set/富文本/竖排投影等已完成增量仍保持。候选源码/新基准保留在e837880与raw来源记录，未改历史。恢复本身**0新测试执行**，只作固定Git输入等价核对，不冒称恢复commit跑过本次3方法。没有删除日常文件、缓存、数据、备份或旧失败夹具。

Windows/JDK17/Gradle9.1、离线/严格依赖、低优先级/单worker、测试2处理器/256MiB。本次15:09:03.480655 JST起20.648秒含Gradle，命令：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf005-punctuation-ratios-final-20261025 -- :studio-core:test --tests *VerticalPunctuationRatioPerformanceTest
```

最新Desktop owner两次30秒空边界后启动；后边界15:09:34 JST发现新owner3316，创建15:09:26、在测试预计结束15:09:24之后；不能据此宣称全程静默或CPU归因。CPU计时粗、0不是零CPU。本次成功所属夹具清理COMPLETE/0payload文件；前值清理只是前次记录，不再累计。未重跑前次分类/竖排/富文本/颜色/旧接受批次9/原生/启动。

[SOURCE_MAP](evidence/PERF005_PUNCTUATION_RATIOS_20261025/SOURCE_MAP.json)固定实际候选输入、参考、私有数组边界与前值复用；[ANALYSIS](evidence/PERF005_PUNCTUATION_RATIOS_20261025/ANALYSIS.json)、[原始逐例](evidence/PERF005_PUNCTUATION_RATIOS_20261025/runs)、[原字节](evidence/PERF005_PUNCTUATION_RATIOS_20261025/inputs)完整保留负面样本。RAW **89,873B** SHA256 `e97050aadfc269a36594ab3dc13348eefbab146763120546236d4cbcb7130e88`，25项+manifest共26成员，CRC/逐项SHA通过。封存raw仅对应候选执行；[采用决定/恢复映射](evidence/PERF005_PUNCTUATION_RATIOS_DECISION_20261025.json)另列于归档之外，不改raw包。

试验收尾/恢复完成，性能HOLD / NOT ADOPTED；独立NOT REQUESTED、用户/包NOT TESTED，[负面试验复审只准备](../../handoff/PERF005_PUNCTUATION_RATIOS_REVIEW_READY_20261025.md)，未外发。旧固定报告、失败、否决、两次异步误写及路径错误UNKNOWN/禁止再访问保持。资产编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/root UNKNOWN、PRODUCT_HOLD / NOT RC不变。无真实资料/凭据/服务、暂停扫描、日常安装/发布/安全/MSI或Desktop写入。

连续授权继续，下一低负载定位已有`TypesetFonts.documentFonts`的字体依赖清单聚合：现有字体加载缓存已经存在，不重复造缓存。先只测纯元数据清单/完整原解析的临时列表成本，不加载字体或改字形/渲染/用户功能；保留所有重复项、顺序、预设与每条文本解析、错误/预算。Desktop资源优先，有证据再决定最小聚合修改，不重跑本试验。
