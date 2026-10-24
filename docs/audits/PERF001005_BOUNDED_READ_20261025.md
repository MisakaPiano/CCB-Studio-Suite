# PERF-001/005：稳定文件单数组读取与动态容量边界

2026-10-25，持续性能授权内的公共读取路径增量，无包或发布。最终源码[27e562186b0980de5fa5cbb8e975d9153a1ba13d](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/27e562186b0980de5fa5cbb8e975d9153a1ba13d)；最初生产候选ad2b9c0、实际旧实现基线8dc3133及各失败输入在[固定映射](evidence/PERF001005_BOUNDED_READ_20261025/SOURCE_MAP.json)分列。原StateJournal的记录、完整链校验、格式/提交及writer在reader之外逐字保持，不改签名/凭据/更新信任配置或请求逻辑。

`readBounded`原先`readNBytes(max+1)`先积累中间块再合并数组。现在从同一NOFOLLOW打开句柄取得大小作**分配提示**，稳定文件只分配返回数组，实际EOF和max+1哨兵仍决定返回/超限；缩短返回实际读到的字节，增长走有界补读，IO失败不返回partial，MISSING/READ_ERROR/CORRUPT及旧负预算/溢出行为保持。未采用相信文件大小后直接无上限读、取消校验或全局缓存/OS参数方案。

另将单次底层读取限制64KiB，避免FileChannel堆缓冲适配器因一次整图请求而申请/缓存原件大小的临时native buffer；测试核对请求上限和zero-read恢复，不宣称所有native内存/RSS已量化。没有修改全局JVM缓存或Desktop设置。文件安全路径/NOFOLLOW保持，没有新增提权或junction/symlink创建。

|最终同JVM交替对照|旧线程分配中位|新线程分配中位|旧 / 新墙钟中位|
|---|---:|---:|---:|
|3MiB稳定文件|6,345,184B|3,156,920B|3.24 / 1.45ms|
|12MiB稳定文件|25,318,184B|12,594,104B|15.63 / 5.87ms|

每尺寸各1预热、4轮交替；旧函数从8dc3133精确复制，仅函数签名名/private不同并逐字核对。分配计数在读取返回后、组装任何JSON前取得。收益为这一读取阶段约减半的线程分配，非端到端UI、RSS/物理磁盘或所有机器提速；CPU计时粒度会给短操作0样本，不代表零CPU。之前实际旧API基线的计数夹带记录JSON成本，保留但不用于此因果对照。

**最终17个唯一方法PASS，0失败/错误/跳过，源码输入稳定**。5 reader方法覆盖实际对照/原字节，8种真实文件长度、精确及超限/空/缺失/目录/错误，stale size hint及分片/增长/缩短与旧EOF行为、实际消费不超过max+1，初段与增长段IO故障，以及64KiB请求上限/zero-read。其余12方法覆盖文档链/原件与工程重开、合成DPAPI凭据不进入文档和备份、完整/损坏/未知/中断恢复、33MiB archive，合成目录UNCHANGED、Prompt提交中断幂等、纯Transferable的静态/APNG/损坏/多文件格式及原字节。没有系统剪贴板、用户窗口或真实服务调用。动态大小场景采用确定性InputStream，**未认证所有Windows/文件系统并发竞态**。

失败账本不删除、不累计为通过或更多缺陷：

- a07edb4首轮实际15方法，1分配断言FAIL、14 PASS；Desktop任务未执行，不能记为其通过/跳过。
- 69e15e7单方法诊断仍FAIL，3MiB首样本4,081,288B；保留准确数值与栈。
- 4395656仅修正测试计数位置/增加固定旧实现对照，阈值仍为一份源大小+256KiB，生产candidate未改，单方法PASS。证明记录成本混入口径；JSON初始化/记录各子项的精确分摊UNKNOWN，未归因Desktop或防护，也不是已修复用户数据故障。
- 27e5621再单独补底层读取请求上限，最终17方法如上；旧轮次、内部组合/样本不累加。锁RED、q09与用户否决继续在旧证据中保持。

[所有逐例计数/完整样本/清理记录](evidence/PERF001005_BOUNDED_READ_20261025/ANALYSIS.json)、[失败原记录](evidence/PERF001005_BOUNDED_READ_20261025/FAILURES_RETAINED.json)、[实际命令/UTC/JUnit/输入/日志](evidence/PERF001005_BOUNDED_READ_20261025/runs)、[完整尝试和限制](evidence/PERF001005_BOUNDED_READ_20261025/ATTEMPTS_AND_LIMITS.md)。Desktop繁忙时同一单次任务等待资源窗口，启动前归属为空后才运行，无新调度自动化；最终23:26:14Z起67.694秒，单worker/低优先级/2处理器/256MiB测试堆，离线严格依赖验证。初RED期间曾观察Desktop PID312，重叠/精确活动未知，不声称任何轮次连续静默。成功自身夹具360文件286,909,265B留证后退役；两个失败根66eb77…/61c109…保留，旧证据/备份/未知内容未动。

RAW **164,910B**，SHA256 `919cae1c04846362cae863e8afd0546fad8d5e37d8f263775cf672b589f80529`，49清单项+manifest共50成员，CRC/逐项SHA核对。实现/自动验证如上，独立审查NOT REQUESTED、用户/包集成NOT TESTED。新Project只准备不外发；没有日常资料/安装/私人凭据/付费API或暂停用户目录操作。资产/编辑USER_NOT_TESTED、115体验/Prompt PENDING、q09 FAILED/UNKNOWN、PRODUCT_HOLD / NOT RC保持。接续现有`workspace.bytes`验证后整数组返回的分配基线，先测再最小优化，原Store验证/准入/排水不减。
