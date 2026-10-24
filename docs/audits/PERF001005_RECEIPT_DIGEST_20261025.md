# PERF-001/005：省去未使用的新增复制摘要

2026-10-25，承接原进行中的性能修改。固定源码[ec75083a664642de3237697398eb2445c3876c43](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/ec75083a664642de3237697398eb2445c3876c43)，没有新包、发布、版本号或更新元数据。此前的仅登记请求不取消21:35/22:07 UTC持续推进授权；新Project材料只准备，不外发。

共享复制层新增的回执SHA在排版原图与受管副本两处不被使用，而调用方仍会对实际目标完整计算SHA。现在仅这两处传入`receiptDigest=false`；AssetStore原件校验、FileIntegrity、受管副本32MiB预算、目标size+SHA检查均保留。图库导出继续生成复制SHA，并在COMPLETE.json要求非空。nullable返回仅为运行时结果，不改存储格式。去掉原有校验、无验证直接Files.copy等方案未采用。

最终一次隔离执行**16个唯一方法PASS，0失败/错误/跳过，源码输入稳定**：共享/排版5、原排版回归2、受管副本3、原批量恢复1、图库5。新无回执分支覆盖8种缓冲边界长度，并独立核对实际目标全量SHA；原件、重复项/顺序、签名、未知目标拒绝、损坏/取消保留、完成标记与阻塞输出时工作区读取保持。旧基线、前轮12/4项和内部样本不累加。此前锁反例1 FAIL和q09 FAILED/UNKNOWN留在原证据，不重新标绿。

|路径|3MiB线程分配中位|12MiB线程分配中位|3 / 12MiB墙钟中位|
|---|---:|---:|---:|
|排版原图|777,432B|773,952B|225.3 / 422.8ms|
|受管副本|6,380,312B|6,389,424B|892.6 / 1354.2ms|
|图库原件（同JVM流式）|606,120B|606,184B|160.8 / 403.7ms|
|图库旧算法（同JVM对照）|11,835,712B|46,438,936B|133.8 / 381.3ms|

这是有界分配回归和额外摘要移除，**不称新提速、RSS改善或整UI验收**。排版/受管旧基线是前轮不同JVM；本轮首次为Suite检查器采用已核实支持的Gradle9.1 `--priority low`，继续单worker、测试堆256MiB、2处理器。启动前没有Desktop Java进程；BUILD SUCCESSFUL后检查观察到Desktop PID48132（创建22:48:37 UTC），具体样本/活动重叠UNKNOWN，不能宣称静默机器或因果时延对照。所有慢样本保留，后续重负载先让Desktop。

[输入身份与未改路径](evidence/PERF001005_RECEIPT_20261025/SOURCE_MAP.json)、[完整样本与分析](evidence/PERF001005_RECEIPT_20261025/ANALYSIS.json)、[实际命令/UTC/JUnit/日志/夹具退役](evidence/PERF001005_RECEIPT_20261025/run)、[尝试与限制](evidence/PERF001005_RECEIPT_20261025/ATTEMPTS_AND_LIMITS.md)。运行2026-10-24T22:45:39Z开始，193.689秒，Windows本机现有JDK17/Gradle9.1离线严格依赖验证。成功自身夹具991文件、530,866,644B留证后退役；失败现场、原证据、日常数据/安装和备份未动。准备中的只读PowerShell语法错误另记，未执行测试或改变源文件。

RAW ZIP **42,499B**，SHA-256 `833e0e754a879a6c927d8d10cae3cc5347a8fb0813ce830a970a2fabd1cd9ffe`，18清单项+manifest共19成员，CRC及逐项SHA核对。原106/107/108/115及旧审查包不变；新独立复审NOT REQUESTED、用户与包集成NOT TESTED。资产/编辑USER_NOT_TESTED、115体验/Prompt PENDING、q09 FAILED/UNKNOWN、PRODUCT_HOLD / NOT RC保持。下一动作仍为既有PERF-005编辑页原件导出的合成分配基线，不扩展编辑功能或用户验收。
