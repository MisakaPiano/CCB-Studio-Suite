# PERF-001/006：底层分配修改后的目录/受管副本集成观测

2026-10-25，本段**没有新增生产或测试代码**。执行基点[1b6e3b62838e69f4e811d57066c36a26d209f308](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/1b6e3b62838e69f4e811d57066c36a26d209f308)的modules/tools输入与应用源码[6dfadc02f544ce5394cf4e628f410ca695939a3d](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/6dfadc02f544ce5394cf4e628f410ca695939a3d)完全一致；只补实际IO/完整资产字节/提交增量组合后的集成数据，不重做已接受PERF-003或启动基线。

一次最终**3个唯一方法PASS，0失败/错误/跳过，输入稳定**：合成目录UNCHANGED实际扫描、受管副本复制/重放，以及进度回调不占工作区锁、并行读可用/取消检查点仅保留完成项。内部文件/轮次、前轮方法不累加。性能夹具关闭自动采集，进度用例仅新建所属源，没有watcher/用户目录或真实图片/凭据/服务。

|当前组合的实际API场景|3MiB输入线程分配中位|12MiB输入线程分配中位|3 / 12MiB墙钟中位|
|---|---:|---:|---:|
|UNCHANGED扫描（每次2文件）|7,682,856B|26,558,272B|47.69 / 123.73ms|
|受管副本（每次1文件）|6,377,080B|6,375,312B|674.67 / 792.14ms|

历史参考：cb819b4扫描12MiB×2为52,008,544B，当前约减半；3086895受管副本为6,389,648B，当前**基本不变，不报告额外显著内存收益**。历史代码组合/独立JVM/调度不同，**不认定因果墙钟/CPU、真实大库、RSS、物理IO或端到端UI提速**。保留全部慢样本。沿用的两项旧样本记录器包含少量JSON记录开销，与较新的隔离计数器精度不同，不作跨夹具精确比较。

[源码与输入相同证明](evidence/PERF001006_INTEGRATION_20261025/SOURCE_MAP.json)、[实际样本/范围](evidence/PERF001006_INTEGRATION_20261025/ANALYSIS.json)、[原始命令/UTC/JUnit/源散列/退役](evidence/PERF001006_INTEGRATION_20261025/run)、[账本](evidence/PERF001006_INTEGRATION_20261025/ATTEMPTS_AND_LIMITS.md)。Desktop繁忙时推迟，单次任务资源门检查为空后才启动；低优先级/单worker/2处理器/256MiB测试堆，不是持续静默证明。31.152秒含Gradle，不是用户操作耗时。成功自身夹具341文件204,604,712B留证后退役，旧失败/基线/备份/未知内容未动，无新失败。

RAW **37,631B**，SHA256 `1a292695e8c46f7fa6d7b19d08d1e89485f53f30d168481400f96b3bafc82ff9`，15清单项+manifest共16成员，CRC/逐项SHA核对。独立审查NOT REQUESTED、人工/包集成NOT TESTED；新Project仅准备，未发布/更新日常安装，未操作Desktop或防护。资产/编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/UNKNOWN及PRODUCT_HOLD / NOT RC保持。继续既有低风险分配定位，任何校验摘要编码优化必须输出逐字等价、保持旧文档/完整性及Locale边界，不改信任链或算法。
