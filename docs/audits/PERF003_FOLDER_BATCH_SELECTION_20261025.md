# PERF-003：文件夹筛选每次只求一次子树

2026-10-25。基线源码/执行[974e1007da207a47a27153cb34aec7380c98ff69](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/974e1007da207a47a27153cb34aec7380c98ff69)，应用源码/最终执行[1256c64ddd4291f585be4ff8d086f98a404a972b](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/1256c64ddd4291f585be4ff8d086f98a404a972b)。没有新安装包、版本/tag、签名/更新元数据或发布产物。

此前[子树遍历算法试验](PERF003_FOLDER_TRAVERSAL_EXPERIMENT_20261025.md)因低预热响应收益不稳未采用，原方法已恢复，全部负面样本保留。本次不修改该方法，而是处理调用处：原 `batches.filter` 对每个候选批次重新计算同一子树。新增 `GenerationOrganization.batchesInSubtree`，非空候选集只计算一次子树，再按原批次顺序筛选；没有全局/跨快照缓存或父索引。

生产仅三个文件：`GenerationOrganization.kt` 插入 helper；`GalleryOrganization.kt` 的组织树、文件夹卡片、面包屑三处表达式等价替换；`GalleryWorkspace.kt` 的组织范围筛选一处替换。原 descendants/完整校验、组织格式/容量、批次图片索引、路径/名称/身份、来源、队列/归档存储及所有布局/remember 键不变。原归档策略差异刻意保留：树/卡片/面包屑默认仍含当前及归档批次元数据，页面文件夹筛选仍仅已归档；组织总根仍直接筛已归档批次。空候选或没有已归档候选时不解引用 null ID；非空有效候选时 null 仍按旧谓词抛 NullPointerException，不提前 `!!` 或改变异常类型。

最终 **4 个唯一方法 PASS，0失败/错误/跳过，源码输入稳定**：核心配对测量1、稳定顺序/归档/未放置/未知根1、空候选/null短路/独立重挂快照1，以及桌面纯文件夹卡片适配器1（层级顺序、当前/归档图片ID、重挂后范围）。桌面方法无原生窗口/controller/剪贴板/provider/目录读取；并非组织树/面包屑/整页的原生交互验证。基线另有3方法：原谓词测量1和保留的 `GenerationFolderContractTest` 2，后两项在本次基线实际通过；不把基线3、最终4、样本或旧方法相加。已接受的旧批次索引9方法和启动基线未重跑。

全新合成纯元数据：32/128/256文件夹，64/256/512批次，32层内、批次/节点乱序、未放置批次和两类归档状态，每批次一个组/合成图片UUID；实际完整 `StudioState(version=3).validate`，序列化25,345/98,411/196,005B，均在4MiB内。没有真实图片或资产存储准入结论。根/叶×归档开关，每模式2次对称预热、4轮同JVM交替；期望ID独立按夹具规则计算，每次验证状态字节不变，计数在JSON/断言之前结束。完整根/叶原始样本见证据，下面仅最终配对根中位数：

|文件夹/批次|仅归档|旧/新累计线程分配B|旧/新墙钟ms|
|---|---|---:|---:|
|32/64|否|2,634,704 / 42,128|2.956 / 0.067|
|32/64|是|1,317,312 / 42,048|1.620 / 0.064|
|128/256|否|39,851,032 / 158,752|34.570 / 0.202|
|128/256|是|19,925,400 / 159,192|14.816 / 0.151|
|256/512|否|161,761,928 / 322,720|201.039 / 0.486|
|256/512|是|80,880,664 / 323,600|101.568 / 0.532|

这些是局部计算的墙钟和累计线程分配，非峰值堆/GC/RSS/整页帧率、首开或所有Windows提速结论。原单次递归扫描复杂度仍在；归档筛选有临时eligible列表。零CPU计时来自分辨率，不表示无CPU。原遍历候选没有因此改判为通过。

实际隔离命令形态（完整runtime/runpy、参数、UTC与JUnit在raw）：Windows/JDK17/Gradle9.1，离线严格依赖、低优先级/单worker，测试2处理器/256MiB：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf003-folder-batches-baseline-20261025 -- :studio-core:test --tests *FolderBatchProjectionPerformanceTest.measuredRecordedFolderPredicatesKeepOrderArchivePolicyAndAdmittedState --tests *GenerationFolderContractTest
python.exe -B tools/Run-PreviewChecks.py --phase perf003-folder-batches-final-20261025 -- :studio-core:test --tests *FolderBatchProjectionPerformanceTest :studio-desktop:test --tests *FolderSubtreeCardProjectionTest
```

基线02:40:29Z起39.189秒；最终02:54:55Z起84.092秒含编译。Desktop优先，资源门两次间隔30秒的空owner检查后启动，等待时只做低负载准备。02:53边界存在Desktop PID53256；最终启动后02:55:10及完成后02:56:34边界为空，不能外推整段无活动或CPU归因。没有终止/修改Desktop或安全设置。成功夹具清理记录均COMPLETE、0 payload文件；旧失败/未知/备份/日常目录未清理。

[源码/原字节与差异证明](evidence/PERF003_FOLDER_BATCH_SELECTION_20261025/SOURCE_MAP.json)、[全部中位数/计数](evidence/PERF003_FOLDER_BATCH_SELECTION_20261025/ANALYSIS.json)、[逐例日志/参数/源输入/清理](evidence/PERF003_FOLDER_BATCH_SELECTION_20261025/runs)、[基线与最终源码原字节](evidence/PERF003_FOLDER_BATCH_SELECTION_20261025/inputs)、[范围/未验/失败保留](evidence/PERF003_FOLDER_BATCH_SELECTION_20261025/ATTEMPTS_AND_LIMITS.md)。三个生产原文件严格匹配source-before哈希，分别仅CRLF规范化后等于固定Git blob，helper插入及四处精确替换证明其余文本不变；参考旧谓词文本未变。RAW **116,842B**，SHA256 `a5a089a2e5e1059320aac57b10f3ab8e9e88d063bfe1af20c96c1a1d83b118f7`，30清单项+manifest共31成员，CRC和逐项SHA核验通过。

实现/本段自动检查完成；独立NOT REQUESTED、人工和包集成NOT TESTED，新Project只准备未外发。[16历史增量索引](../../handoff/PERFORMANCE_INCREMENT_REVIEW_READY_20261025.md)仍固定fa58a50，不悄悄扩大其载荷或原Desktop报告范围。资产/编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、q09 FAILED/root UNKNOWN、PRODUCT_HOLD / NOT RC、原失败和用户否决保持。没有真实服务/私人资料/恢复扫描/日常安装或发布。继续既有性能项的低负载定位，有据再最小改动，不重复已接受基线。
