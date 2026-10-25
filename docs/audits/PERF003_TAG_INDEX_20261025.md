# PERF-003：管理标签索引减少展开分配

2026-10-25，实际旧API基线[c82b463ba06f00f0251458f724fcc924622b1b35](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/c82b463ba06f00f0251458f724fcc924622b1b35)，应用源码[490c59686870d6acdfce03b7bad7b1034160c97c](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/490c59686870d6acdfce03b7bad7b1034160c97c)，实际执行文档基点[ca70ab219baa7eabbefafbdd751148e9a0bc850f](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/ca70ab219baa7eabbefafbdd751148e9a0bc850f)。模块/工具输入完全相同，不能把文档commit当新应用改动；无安装包/版本发布。

仅 `LibraryTagIndex` 构造初始化改变。旧实现两次作用域ID去重、逐资产标签distinct并展开总列表后统计；现一次ID去重、构造内临时逐资产seen集合和有序计数表，直接累计。seen每个资产前清空，ID和seen集合均不保留为字段，没有全局缓存/保留图谱。**作用域唯一资产数、每资产每标签最多一次、大小写敏感标签身份、首次键顺序保持**；无标签ID仍算作用域资产。建议排序、精确/任一/全部标签匹配、范围选择源码逐字相同，原Prompt标签不参与。最终测试中的旧类逐字复制，仅private/类名改变；除构造段外该文件及全部其他生产modules/tools不变，SOURCE_MAP保留证明。

|最终同JVM四轮交替、每模式两次预热|序列化LibraryState|旧 / 新线程分配中位|旧 / 新墙钟中位|
|---|---:|---:|---:|
|128资产，161作用域引用|23,963B|198,184 / 79,936B|1.06 / 0.88ms|
|1024资产，1281作用域引用|191,067B|1,536,096 / 742,464B|2.66 / 1.90ms|
|8192资产，10241作用域引用|1,527,899B|12,076,200 / 6,820,912B|14.71 / 7.71ms|

每资产16个管理标签、全域64种，作用域含重复四分之一ID和一个无标签ID；LibraryState均在现有4MiB预算内。表内分配约少59.7%、51.7%、43.5%，仅构造函数线程累计堆分配；临时节点、计数装箱、ID与输出集合仍分配，不是零分配/整图库RSS/物理IO/所有Windows速度结论。CPU粗粒度、0不等于零CPU。基线单独JVM，表中用最终同JVM交替旧/新；取数紧跟构造、在类型读取/断言/JSON和输入重新序列化之前。每轮计数、键顺序和原输入序列化字节均核对。

基线**1方法PASS**；最终**4唯一方法PASS，0失败/错误/跳过，输入稳定**，不累计基线/尺寸/轮次。三项新方法覆盖实际API配对分配/作用域/计数/字节；重复ID、重复标签、大小写独立、首键顺序、范围外标签不进入、缺标签、中文、自然排序/trim/不区分大小写搜索与旧结果一致；空/未知作用域、输入可变集合修改后新索引重新取数且旧快照不受影响。一个既有方法验证唯一范围计数、建议及精确/任一/全部匹配。本组全部纯元数据，不读取/生成原图或使用服务。

准备脚本首次用Windows默认GBK读取含合成中文标签的UTF-8草稿，在写入测试文件前失败；显式UTF-8修正后重新准备。该失败**0次Gradle/JUnit执行**，原始错误及修正脚本单独保留，不冒充产品PASS/FAIL，不删除/放宽断言。本段没有Gradle/JUnit失败，旧失败/用户否决不变。

命令形态如下；实际隔离运行时通过runpy加入tools，完整参数/UTC/输入/JUnit在原始记录。Gradle9.1/JDK17，Windows，离线严格依赖校验、低优先级/单worker、测试2处理器/256MiB堆：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf003-tag-index-final-20261025 -- :studio-core:test --tests *LibraryTagIndexPerformanceTest --tests *LibraryInteractionTest.tagPredictionCountsUniqueScopedAssetsAndFiltersExactLabels
```

最终01:10:32Z起22.66秒含Gradle。Desktop为空的两个30秒间隔归属检查后启动，较晚结束观察也为空；没有持续监测，静默主机仍UNKNOWN。资源等待不算测试，不强杀或改写Desktop/防护。新鲜所属夹具先留证退役，本组未创建payload文件，清理0文件/0B；旧失败/未知目录/备份/用户资料不动。

[源码/执行映射及构造外不变证明](evidence/PERF003_TAG_INDEX_20261025/SOURCE_MAP.json)、[全部样本和计数](evidence/PERF003_TAG_INDEX_20261025/ANALYSIS.json)、[命令/JUnit/输入/清理](evidence/PERF003_TAG_INDEX_20261025/runs)、[准备失败](evidence/PERF003_TAG_INDEX_20261025/PREPARATION_FAILURE.md)、[资源边界](evidence/PERF003_TAG_INDEX_20261025/resource-boundaries.jsonl)、[限制账本](evidence/PERF003_TAG_INDEX_20261025/ATTEMPTS_AND_LIMITS.md)。RAW **70,672B**，SHA256 `f3b5e7ccfd946a0652c6ccb1987f84a9b250b07ac75ac342ca7fc79e2de651a7`，23清单项+manifest共24成员，CRC/逐项SHA通过。

独立审查NOT REQUESTED、用户/包集成NOT TESTED，新Project只准备未外发。无UI/模型/文件组织/原图/请求/Prompt逻辑/更新协议/信任链/包变化，无真实图库/凭据/服务/付费调用、恢复暂停扫描、日常安装操作或新发布。资产管理/编辑USER_NOT_TESTED，115体验与Prompt比较PENDING，q09 FAILED/UNKNOWN与PRODUCT_HOLD / NOT RC保持。继续既有PERF-003记录日期纯元数据路径定位：Request.recipe防御副本仍保持，先测重复取值成本，有依据才调用内复用同一副本，不改JSON解析/预算/来源或日期语义。
