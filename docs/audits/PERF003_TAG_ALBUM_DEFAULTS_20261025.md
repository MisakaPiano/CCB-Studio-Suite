# PERF-003：标签相册默认参数的调用内复用

2026-10-25。基线源码/执行[45068dfbf2213f8d8fc2041aa1b4a30e81024efb](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/45068dfbf2213f8d8fc2041aa1b4a30e81024efb)，应用/最终执行[52e0bf05a5844f85c59d3bb79a71b7f2c3182858](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/52e0bf05a5844f85c59d3bb79a71b7f2c3182858)。无新包、版本/tag、发布或更新元数据。

`galleryTagAlbums` 原来为每个“图片×标签”条目构造一套默认 `NovelAiGenerationRecipe`，包括默认设置/图像引导/精确参考对象，以及权威默认负面文本函数的constant.trim结果。默认配方只有不可变val/空列表/null值，不含默认图像资产/随机ID/时钟。现只插入一个调用内nullable变量，在第一条目通过**原权威构造函数**求值并复用；空输入不构造默认对象，无全局或跨投影缓存。没有复制默认Prompt文字或改写权威逻辑，没有实际生成请求。图片对象和历史entry仍逐条新建；entry原默认createdAt时钟在原构造处保留，ID、每标签hash/key、成员顺序/缺标签回退均不变。

生产仅 `LibraryCatalogProjection.kt` 一变量/一构造参数替换。该文件其他日期/作业/选择函数和其他modules/tools生产代码不变；image-authority的模型/图像引导/默认Prompt源文件未改，源输入哈希与固定Git一并列出。原函数完整放入测试私有参考，仅改函数名，字节规范化后严格核对一致。原文件混合换行保留在raw输入中，不对源码做归一化来满足断言。

最终 **4唯一方法PASS，0失败/错误/跳过，源码稳定**：新配对实测1、空/缺失/重复输入及配方值1、复制修改不污染兄弟/后续投影1，以及既有 `LibraryInteractionTest.multiTagAlbumsHaveUniqueKeysAndUnknownDatesStayUnknown` 1。每条entry验证canonical配方全值相等、图像path/seed、images/index、ID/key，创建时间在实际调用前后范围内；这不证明不同entry的时间值各不相同，也不计数时钟调用，时钟求值位置保持以源码证明。直接重复标签测试保留helper原行为，不把不合标签准入的数据称为已入库。基线1方法另列，不累加内部轮次、历史方法或两个阶段数量。

128/512/2048个合成UUID资产标识，每个有标签项8标签、每13项缺管理标签、每11项空标签、输入每17项追加重复ID；固定163+count乱序。实际标签UUID/数量/长度/非空/唯一等限制检查，序列化 `LibraryState` 为14,013/55,850/223,365B、均<=4MiB。**没有调用私有Repository.validate、没有真实资产或存储准入结论**。两模式各2预热、4轮同JVM交替，全部值/时间/输入字节断言与序列化在计数外。最终配对中位数：

|资产/条目|旧/新累计线程分配B|旧/新墙钟ms|
|---|---:|---:|
|128/877|987,508 / 314,664|1.148 / 0.875|
|512/3515|3,755,396 / 1,056,644|2.847 / 1.528|
|2048/14074|14,853,936 / 4,045,872|4.590 / 4.845|

收益仅累计线程分配约少68%～73%。最大样本墙钟略慢，原结果保留，不调预热或复跑求绿，不称普遍提速。线程CPU分辨率粗，0不是无CPU；没有峰值堆/GC/RSS/整页/长期或全部Windows结论。

实际Windows/JDK17/Gradle9.1，离线严格依赖、低优先级/单worker、测试2处理器/256MiB。完整runtime/runpy与UTC/参数/JUnit保留，命令形态：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf003-tag-album-defaults-baseline-20261025 -- :studio-core:test --tests *TagAlbumDefaultsPerformanceTest.measuredActualTagAlbumsPreserveLabelsKeysDefaultsAndInputSnapshots
python.exe -B tools/Run-PreviewChecks.py --phase perf003-tag-album-defaults-final-20261025 -- :studio-core:test --tests *TagAlbumDefaultsPerformanceTest --tests *LibraryInteractionTest.multiTagAlbumsHaveUniqueKeysAndUnknownDatesStayUnknown
```

基线03:02:42Z起15.890秒，最终03:04:26Z起19.236秒含Gradle；不是上述性能样本时间。Desktop优先，门控两次间隔30秒空owner检查后才启动；完成后03:05:02Z边界为空，不外推连续无占用或CPU归因。成功夹具COMPLETE/0payload文件，旧失败/未知/备份/日常内容未清理。

[固定源/精确差异/权威未改](evidence/PERF003_TAG_ALBUM_DEFAULTS_20261025/SOURCE_MAP.json)、[全部样本/范围](evidence/PERF003_TAG_ALBUM_DEFAULTS_20261025/ANALYSIS.json)、[原始逐例/命令/源输入/清理](evidence/PERF003_TAG_ALBUM_DEFAULTS_20261025/runs)、[执行源码原字节](evidence/PERF003_TAG_ALBUM_DEFAULTS_20261025/inputs)、[限制与保留](evidence/PERF003_TAG_ALBUM_DEFAULTS_20261025/ATTEMPTS_AND_LIMITS.md)。RAW **76,259B** SHA256 `c9ac75067185b9112282b3ddf75ae00720d49d6f248607eae7bd153c27628385`，25清单项+manifest共26成员，CRC及逐项SHA核对通过。

实现/本段自动检查完成，独立NOT REQUESTED、用户/包集成NOT TESTED，新[复审只准备](../../handoff/PERF003_TAG_ALBUM_DEFAULTS_REVIEW_READY_20261025.md)。16历史索引仍固定fa58a50，不扩大原外发或Desktop报告范围。旧接受批次9/启动未重跑，旧失败/用户否决及遍历候选未采用保持。资产/编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、q09 FAILED/root UNKNOWN、PRODUCT_HOLD / NOT RC保持。无原生窗口、真实图片/凭据/服务、付费API、恢复扫描、日常安装/安全/信任链或发布操作。继续已有性能低负载定位，有据才最小改动。
