# PERF-001/007：等价摘要文本编码减少Formatter分配

2026-10-25，实际旧API基线[2559eb030bcda1d34e07802e79f704d3b8e0f9b1](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/2559eb030bcda1d34e07802e79f704d3b8e0f9b1)，应用源码[8f4597ddb8278e25a1ab90dafabae4774755e853](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/8f4597ddb8278e25a1ab90dafabae4774755e853)，实际执行文档基点[d812e8fb249e865d99438bcd869f4f2df802d9a9](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/d812e8fb249e865d99438bcd869f4f2df802d9a9)。modules/tools输入完全等价，不能把文档commit当新应用改动。未生成安装包/发布版本。

仅两个表达式：Models.sha256及FileIntegrity.fingerprintFile把32个digest字节逐个Formatter转换，改为现有Java17 `HexFormat.of().formatHex`（未变JavaAssetStore已使用同一类）。**SHA-256算法、provider选择、输入字节、完整摘要长度、验证次数、文件读取/预算/路径安全/错误分类均未改变**；两文件除这两表达式外逐字相同。无共享可变MessageDigest、缓存或省略校验，无签名/密钥/信任根/TUF/更新协议/元数据/tag/版本号/安装器改动。

|最终同JVM交替，64次SHA调用/样本|旧线程分配中位|新线程分配中位|旧 / 新墙钟中位|
|---|---:|---:|---:|
|空输入|1,508,940B|54,272B|4.76 / 0.65ms|
|1KiB输入|1,509,024B|54,272B|3.20 / 1.23ms|
|64KiB输入|1,508,892B|54,272B|12.27 / 10.10ms|

这是每64次完整工具调用（包括JCA及结果字符串）的线程累计分配，约减少96.4%；每次新调用约848B，保留2KiB/调用测试预算。各5次预热、4轮交替，旧函数从2559eb0精确复制，仅签名名/private变化。取数在JSON/断言前；CPU采样有0/粗粒度，64KiB两方CPU中位都15.625ms，不宣称CPU或端到端UI、RSS、物理IO及所有Windows速度收益。不是64个独立测试。

基线**2方法PASS**，最终**7唯一方法PASS，0失败/错误/跳过，输入稳定**，不累计重复基线/内部case。4新方法：实际配对分配与输出；全部256字节在ROOT/US/中国/日本/阿拉伯语/土耳其语显式Locale下与旧格式逐字等价（不改JVM默认Locale），空/abc固定向量及小写ASCII64位；用旧摘要函数构造的格式2/3合成envelope、完整previous/body hash/正文快照可读取且损坏仍拒绝；文件fingerprint与旧SHA一致、预算拒绝并保留原字节。3原方法覆盖删除链拒绝、未知/不完整/损坏备份不发布/覆盖、流式文件精确边界。本段没有新失败；旧失败/用户否决不改。

**旧摘要envelope是隔离构造的schema夹具，不是已发布旧安装器/原生升级或真实凭据证据。**未改请求/Prompt算法与数据模型；存储字段及字符串输出保持，未把源码等价编码写成信任链更换。

[应用/执行身份、表达式外相同及固定旧函数证明](evidence/PERF001007_HASH_ENCODING_20261025/SOURCE_MAP.json)、[所有样本/计数](evidence/PERF001007_HASH_ENCODING_20261025/ANALYSIS.json)、[命令/UTC/JUnit/输入/清理](evidence/PERF001007_HASH_ENCODING_20261025/runs)、[资源推迟记录](evidence/PERF001007_HASH_ENCODING_20261025/resource-deferrals.jsonl)、[范围账本](evidence/PERF001007_HASH_ENCODING_20261025/ATTEMPTS_AND_LIMITS.md)。一个30分钟资源门到期未执行测试，计0次；继续等待后仅实际启动一次最终回归。Desktop归属为空才开始，低优先级/单worker/2处理器/256MiB堆；最终00:41:05Z起62.583秒含Gradle。结束边界曾观察Desktop PID48948，未留创建时刻/精确重叠，不能声称持续静默。没有终止/改写其进程、数据或防护。

成功自身夹具共30文件24,821B留证后退役，旧失败/基线/备份/未知内容保留。RAW **69,569B**，SHA256 `c0971f2af670cddcab03668b2cca5224b389148e1c55dd370e2df04a4b70ce7b`，23清单项+manifest共24成员，CRC/逐项SHA核对。无私有资料/凭据/服务/付费调用或用户目录扫描、日常安装操作。独立审查NOT REQUESTED、用户/包集成NOT TESTED，新Project只准备不外发；资产/编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/UNKNOWN与PRODUCT_HOLD / NOT RC保持。

接续已有PERF-003中的**虚拟相册成员并集**纯元数据基线；这是此前未测的新函数范围，不重开已接受批次投影/启动基线，不改UI布局/入口或文件组织语义。先确认合法4MiB范围的重复成员/顺序成本，再判断最小等价优化；Desktop优先。
