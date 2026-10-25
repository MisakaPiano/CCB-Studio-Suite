# PERF-003：作业结果日期汇总减少临时集合

2026-10-25。基线输入[3d7369963ec942fed58b1a1963d2c5d9ccc397e4](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/3d7369963ec942fed58b1a1963d2c5d9ccc397e4)、实际基线执行b5e84daf63ceb3139277e6e3400d1116f354dd3b；应用源码[41e0111194b407f329a222b2adf4dfcb7631a93b](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/41e0111194b407f329a222b2adf4dfcb7631a93b)、最终实际执行c32045c0dfb7249d7c7490c8e87a873466d6a308。各执行与对应应用输入的modules/tools完全相同，差别仅文档提交。无新安装包/版本/tag、签名/更新元数据或发布。

`libraryRecordedTimes` 原先在完整导入收据投影后，对每个任务拼接普通/合成结果列表、建立Pair列表、展平、toMap，再与导入映射相加。现只将导入映射复制到局部可变Map，按原任务顺序、每任务普通结果再合成结果写入，返回原声明的Map接口。第一次插入决定键顺序，后来的输入任务覆盖时间，**不按时间值大小排序，不过滤失败任务已有结果**。没有全局缓存或引用原输入的可变映射；不承诺具体集合实现/强制转型后的可变性。

生产仅 `LibraryCatalogProjection.kt` 的上述返回段改变。导入收据的单次防御副本、16KiB上限、完整JSON/kind/version/createdAt解析、错误捕获、未知/0/负日期及其他函数完全不变；标签相册canonical默认参数复用仍原样。权威Prompt/请求、队列/账户/归档/容量/来源、数据格式与UI调用/remember键均未改。源文件实际原字节严格匹配各source-before，CRLF规范化后等于各固定Git blob，精确段替换证明其他生产文本保持；私有测试参考是整个旧函数，仅改名称。

最终 **3唯一方法PASS，0失败/错误/跳过，输入稳定**：

- `RecordedJobTimesPerformanceTest.measuredActualResultDatesKeepFirstKeyOrderAndLastJobValue`：实际新旧API配对；期望顺序独立由首次ID集合、时间独立按最后含该ID的任务查找，任务乱序且时间不单调，避免错误改成最大时间/排序。
- `importsOverlapsEmptyResultsAndIndependentSnapshotsKeepOrderAndDates`：导入重复ID、普通/合成重复、跨任务覆盖、空输入/空结果、逆序输入、负值和独立后续快照；原管理状态及防御recipe字节不变。
- 既有 `LibraryRecordedTimesPerformanceTest.malformedOversizedUnknownReceiptsAndJobOverridesMatchLegacyWithoutPayloadAccess`：坏JSON/未知kind/version/缺时间、16,384/16,385B边界、0/负值、导入覆盖及普通/合成结果顺序保持。

基线实际1方法另列，不相加，不重复旧导入收据防御副本性能基线或已接受批次9/启动。合成128/512/2048个JobRecord、每有普通结果任务4条/每任务2条合成结果、共享ID且每7项普通结果为空、COMPLETED/FAILED均保留；固定179+count乱序。实际 `StudioState(version=3).validate`、序列化89,718/355,177/1,417,168B<=4MiB和输入字节检查通过。**这些是元数据记录，synthetic-unused配方不是实际请求准入；没有资产payload/入库、队列/provider/真实账户或凭据执行结论。**

最终每模式2预热、4轮同JVM交替；计数在断言、状态序列化和JSON前结束。以下为最终配对中位数，非基线与最终跨进程拼接：

|任务/结果引用/日期键|旧/新累计线程分配B|旧/新墙钟ms|
|---|---:|---:|
|128/692/250|100,960 / 38,432|0.237 / 0.094|
|512/2776/1000|396,824 / 153,584|1.399 / 0.530|
|2048/11116/3950|1,617,968 / 612,224|2.919 / 1.071|

本样本线程累计分配约少61%～62%，局部墙钟也下降；不代签整页帧率、峰值堆/GC/RSS、长期/物理IO或所有Windows。CPU分辨率粗，0不代表无CPU。完整原始样本保留，未调预热/阈值求绿。

Windows/JDK17/Gradle9.1，离线严格依赖校验、低优先级/单worker，测试2处理器/256MiB。完整runtime/runpy/UTC/参数在raw，命令形态：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf003-job-times-baseline-20261025 -- :studio-core:test --tests *RecordedJobTimesPerformanceTest.measuredActualResultDatesKeepFirstKeyOrderAndLastJobValue
python.exe -B tools/Run-PreviewChecks.py --phase perf003-job-times-final-20261025 -- :studio-core:test --tests *RecordedJobTimesPerformanceTest --tests *LibraryRecordedTimesPerformanceTest.malformedOversizedUnknownReceiptsAndJobOverridesMatchLegacyWithoutPayloadAccess
```

前两次30分钟资源门到期均**0测试执行**，记录保留；第三次按最新owner的两次间隔30秒空边界启动，不将旧PID退出当全空闲，不强杀。基线04:28:41Z起33.218秒、最终04:33:57Z起22.664秒含Gradle，非性能样本耗时。最终后边界出现的Desktop PID39104创建于04:35:56Z，晚于最终结束约04:34:20Z；不能据此证明整个窗口无其他活动/CPU持续静默。成功所属夹具清理均COMPLETE、0payload文件，旧失败/未知/备份和日常资料不动。

[源码/实际输入及精确差异](evidence/PERF003_JOB_RECORDED_TIMES_20261025/SOURCE_MAP.json)、[全部样本/计数/限制](evidence/PERF003_JOB_RECORDED_TIMES_20261025/ANALYSIS.json)、[原始逐例/命令/源输入/清理](evidence/PERF003_JOB_RECORDED_TIMES_20261025/runs)、[执行原字节](evidence/PERF003_JOB_RECORDED_TIMES_20261025/inputs)、[准备/资源推迟及限制](evidence/PERF003_JOB_RECORDED_TIMES_20261025/ATTEMPTS_AND_LIMITS.md)。RAW **76,429B** SHA256 `8ead31f82da88a387371a7d40734b7cb7c08b567e186e0f40c5a351ae869d34e`，26清单项+manifest共27成员，CRC/逐项SHA通过；PLAN等保留初始准备时点，实际结果以summary/逐例为准。

实现/本段自动检查完成；独立NOT REQUESTED、用户/包集成NOT TESTED，[复审仅准备](../../handoff/PERF003_JOB_RECORDED_TIMES_REVIEW_READY_20261025.md)，不发送新Project载荷。原16项fa58a50索引、两段补充b5e84da的52e0bf0边界和原Desktop报告均不扩大到本源码。旧失败/否决/遍历试验未采用、[执行者路径错误UNKNOWN及后续约束](OPERATOR_PREPARATION_PATH_ERROR_20261025.md)、q09 FAILED/root UNKNOWN、资产/编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、PRODUCT_HOLD / NOT RC保持。不再访问误写路径或扫描无关文件；后续临时路径/归属/同名/链接预检保持。无真实图片/凭据/服务、暂停扫描恢复、日常安装/安全/信任链/发行操作。继续既有低风险性能范围；颜色解析仍仅本地草稿，有资源/实际基线依据后才最小改动，不启动编辑新功能或unified-image-tool整合。
