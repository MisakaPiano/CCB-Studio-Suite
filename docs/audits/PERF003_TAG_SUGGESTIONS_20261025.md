# PERF-003：标签建议排序调用内复用自然键

2026-10-25，旧API基线[f996e4ed48529af204ab6e6be9ef0ff5cf19c4bd](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/f996e4ed48529af204ab6e6be9ef0ff5cf19c4bd)，应用/实际最终执行[fa58a50c44a592df0f0c8745ba773c244d7442d9](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/fa58a50c44a592df0f0c8745ba773c244d7442d9)。无安装包或版本发布。

旧建议排序每次比较重新trim查询、为两侧字符串重新分词生成自然排序键。现一次trim，用一个**当前同步排序独有**的比较器按需记录字符串的自然键；只调用原 `naturalKey` 和 `compareNaturalKeys`，两个算法及 `galleryNaturalCompare`、Regex/Locale/数字零裁剪规则均未改。原contains(ignoreCase)、prefix优先、自然排序和稳定同值规则保留；构造计数、匹配/选择、其余生产文本相同。没有全局/字段/跨查询缓存，返回的字符串列表不捕获比较器；原Prompt标签、UI/模型/来源/原图不涉及。旧suggestions函数在测试中仅签名/private与counts参数改变。

|最终同JVM两次预热/模式、四轮交替|序列化LibraryState|旧 / 新线程分配中位|旧 / 新墙钟中位|
|---|---:|---:|---:|
|128资产，256条匹配建议|11,391B|3,173,996 / 349,352B|5.55 / 1.47ms|
|1024资产，2048条匹配建议|92,079B|37,760,560 / 2,226,600B|22.08 / 4.82ms|
|4096资产，8192条匹配建议|374,703B|184,484,600 / 8,693,520B|67.22 / 17.60ms|

每资产tagN/stageN/shared三种管理标签，种子71+规模固定打乱，查询 `" tag "`：tagN前缀组先于stageN中部匹配组，两组各自然数字排序。全部LibraryState在4MiB内。分配约少89.0%、94.1%、95.3%，仅累计线程堆分配；索引构造/序列化/输出验证不在计时中。**临时存活键表随匹配数增长，是明确的内存取舍；峰值堆、GC及时回收、RSS和返还OS未测。**空/单结果可能多一个很小的工厂/空表开销，不宣称所有输入零分配或普遍收益。基线独立JVM，表为最终同JVM交替；CPU粗粒度、0非零CPU，不代签整页/帧/物理IO/所有Windows或真实图库速度。

基线**1方法PASS**；最终**4唯一方法PASS，0失败/错误/跳过，输入稳定**，不累计尺寸/轮次/基线。三个新方法：实际建议顺序/分配及原数据字节不变；18代表字符串逐对比较与旧函数精确一致，含空串/零填充/大小写稳定同值/中文/希腊字母/阿拉伯数字/混合后缀；prefix优先、大小写/trim/Unicode、空/不匹配、重复查询与独立作用域、计数/源标签不变。一个既有方法验证唯一作用域计数、预测建议及精确/任一/全部匹配。324个内部字符串对不是324项测试。本段无新失败，旧失败/用户否决保持。

命令形态如下；实际隔离python/runpy、UTC/参数/JUnit在原始记录。Windows/JDK17/Gradle9.1，离线严格依赖校验、低优先级/单worker、测试2处理器/256MiB堆：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf003-tag-suggestions-final-20261025 -- :studio-core:test --tests *TagSuggestionsPerformanceTest --tests *LibraryInteractionTest.tagPredictionCountsUniqueScopedAssetsAndFiltersExactLabels
```

最终01:33:43Z起20.922秒含Gradle。显式一次性资源门在两个30秒间隔Desktop为空后启动；Desktop PID24008于01:33:58.725884Z创建，落在最终窗口，进程存在重叠，具体CPU/JUnit样本重叠UNKNOWN，不能称持续静默。未干预Desktop/其他进程、防护或全局Locale。新鲜所属纯元数据夹具未创建payload文件，留证退役0文件/0B；旧失败/未知/用户/备份不动。

[源码/执行映射及原比较算法不变证明](evidence/PERF003_TAG_SUGGESTIONS_20261025/SOURCE_MAP.json)、[全部样本/计数](evidence/PERF003_TAG_SUGGESTIONS_20261025/ANALYSIS.json)、[实际命令/JUnit/源输入/清理](evidence/PERF003_TAG_SUGGESTIONS_20261025/runs)、[资源记录](evidence/PERF003_TAG_SUGGESTIONS_20261025/resource-boundaries.jsonl)、[限制账本](evidence/PERF003_TAG_SUGGESTIONS_20261025/ATTEMPTS_AND_LIMITS.md)。RAW **72,215B**，SHA256 `20fe82aebc899f2af95ab8b5a1203ae37d1bec1ff50764d9528244d6f28e9228`，23清单项+manifest共24成员，CRC/逐项SHA通过。

独立NOT REQUESTED、人工/包集成NOT TESTED，新Project仅准备未外发。无真实图库/凭据/服务/付费调用、暂停扫描恢复、日常安装、更新协议/信任链/包变化或新发布。资产管理/编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、q09 FAILED/UNKNOWN与PRODUCT_HOLD / NOT RC保持。继续PERF-003组织目录子树纯元数据基线：保持DFS顺序、未知根与全部组织校验，先测重复父节点扫描，有依据才调用内索引；不重开已接受的批次图片投影/启动基线，不改归档/队列/UI。
