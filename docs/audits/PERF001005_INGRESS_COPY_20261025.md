# PERF-001/005：受管目录副本的有界复制与复核

2026-10-25，按持续性能授权接续，不恢复用户暂停扫描。固定基线a6a366ee9349796238ac73cbb2c3cb54e9614748；最终源码[3086895c49d4f6f41231a187aee110abe0782013](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/3086895c49d4f6f41231a187aee110abe0782013)。只改LibraryRepository.retainIngressBatch的文件复制/复核；metadata计划、COPY_PENDING/PRESENT、两次持久检查点、finally/progress/返回顺序不变。复用5bfa51d已验证的共享流式复制；FileIntegrity/AssetStore/格式及请求/更新逻辑均未改。

原路径先workspace.bytes整数组，再readBounded整文件复核；已有目标的SHA检查和最终size+SHA检查继续保留，只将读取改为fingerprintFile。两个调用都明确32MiB预算，未改成默认256MiB上限；新目标仍CREATE_NEW/NOFOLLOW，未知或修改过的目标不替换。传输中取消仍保留partial/COPY_PENDING，原件先前已提交不受影响，未新增采集/自动重试行为。

|合成附加块|旧线程分配中位|新线程分配中位|旧/新墙钟中位|
|---|---:|---:|---:|
|3MiB|23,856,792B|6,381,152B|601.6 / 700.5ms|
|12MiB|77,441,296B|6,389,648B|862.8 / 1017.3ms|

12MiB当前线程累计分配降低约91.7%；3→12MiB未再增加与原件大小成比例的缓冲，剩余主要包含元数据历史读取/写入。轮次中元数据增长仍有成本，未称固定总预算或完成大库优化。墙钟/CPU样本变慢，**不报告提速、UI或RSS改善**；两个独立JVM前后各预热一次/3实测轮，非同JVM配对，不隔离IO/force/JIT/GC/系统调度影响。有效16×16合成PNG附加块仅用于原始字节复制，非用户图库/真实图像多样性。

基线1方法PASS；最终**4个唯一方法PASS，0失败/错误/跳过、输入稳定**，重复基线方法和内部24张/轮次不累加。覆盖实际副本/回执/PRESENT及重放、被修改的目标保持COPY_PENDING且原件不变、32MiB+1拒绝CORRUPT但保留副本和源、原24项部分取消/两检查点/重开继续回归。性能与新故障用例显式暂停采集；原有纯batch回归没有启动任何扫描。未重跑其他共享helper的12项（其源字节未改），不把旧通过计作本次执行。

[输入身份](evidence/PERF001005_INGRESS_20261025/SOURCE_MAP.json)、[所有原始轮次与计数](evidence/PERF001005_INGRESS_20261025/ANALYSIS.json)、[实际命令/UTC/JUnit/退出/散列](evidence/PERF001005_INGRESS_20261025/runs)、[范围与尝试](evidence/PERF001005_INGRESS_20261025/ATTEMPTS_AND_LIMITS.md)。两轮均用既有隔离OWNER，成功自身夹具留证后退役，无新意外失败，旧共享helper RED/q09保持。Desktop启动/边界归属检查不等于持续静默；单worker/256MiB/2处理器，不中断对方进程/目录。

RAW ZIP63,995B，SHA-256 `9e6c5b43e4cad6ef425c88211570ea8012f58ab29ce1049a1ae848341edf949d`，20清单项+manifest共21成员，CRC/逐项SHA核对。仅合成工程证据，无真实图片/凭据、日常安装/账户/付费调用/权限防护/大扫描/新包或发布；新独立复审NOT REQUESTED，人工/包集成NOT TESTED。资产/编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/UNKNOWN、PRODUCT_HOLD / NOT RC及整个PERF未关闭。

下一独立低风险检查点：共享复制在排版/受管副本的返回SHA未被使用，而调用方保留实际目标全量SHA复核；核对只去掉新引入的额外摘要生成、保持原有校验，避免资源优化添加不必要CPU。新Project材料只准备不外发；普通检查点继续推进，Desktop优先。
