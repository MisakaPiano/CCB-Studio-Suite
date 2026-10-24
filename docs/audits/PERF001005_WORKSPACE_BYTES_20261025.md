# PERF-001/005：验证后完整字节返回减少数组扩容

2026-10-25，沿原持续性能授权。实际API基线[b6abdc92d5b47f256e84e4c3da0695b5be3a6e03](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/b6abdc92d5b47f256e84e4c3da0695b5be3a6e03)，实现[66ed23c44357b10de198eb2a488475b472b6ac81](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/66ed23c44357b10de198eb2a488475b472b6ac81)。只改`StudioWorkspace.bytes`使用的内存收集器及移除无用import，仍在相同同步工作区operation内调用同一个`assets.export`；原Store源验证/准入/排水、256MiB存储预算没有变化，不新读元数据或去掉校验。整个StudioWorkspace其他代码精确相同，在collector中断言。

原ByteArrayOutputStream随容量倍增，再复制完整输出，12MiB输入约46MB线程分配。新增模块内部AssetBytesBuffer复制Store复用的输入块、保留私有不超过64KiB块，最终合并；单一恰好完整的小块可直接交给调用方。输出仍是完整ByteArray，不是新流式调用协议，不设全局缓存/共享返回数组；异常不返回partial，每次API调用拥有独立数据。未采用借用Store可变输入块、跳过验证、额外整图校验读或按陈旧元数据直接信任原件长度的方案。

|合成不透明输入|旧线程分配中位|新线程分配中位|旧 / 新墙钟中位|
|---|---:|---:|---:|
|3MiB|11,649,484B|6,469,608B|20.81 / 17.55ms|
|12MiB|46,252,400B|25,348,712B|70.27 / 62.33ms|

12MiB线程累计分配降低约45.2%；仍约两份编码源大小加常量开销，不宣称零复制或完整解码/编辑内存改善。两个独立JVM各尺寸1预热/4轮，包含原有完整源验证；从一开始就在API返回后、JSON记录前取数。仅本机合成调用，不外推UI、真实图库、RSS、物理IO或普遍提速；CPU12MiB两轮中位均62.5ms、粒度粗，所有慢样本保留。不是图像多样性测试，输入为原始二进制，不解码或生成图片。

基线1方法PASS；最终**7个唯一方法PASS，0失败/错误/跳过，输入稳定**，不累加重复基线或内部case。4新方法覆盖实际分配/原字节，10种长度及返回数组可独立修改而原Store/状态/资产数不变，损坏源不返回partial/不替换源，生产者缓冲复用/混合scalar/bulk/offset/空写/非法范围的所有权。3原集成方法覆盖工程/undo/重开、来源修改/丢失/标签/图像与Prompt/备份恢复、两种Prompt提交中断后幂等恢复。现有其他读取17方法的源码未改、未重新执行或累计；旧计数口径失败/锁RED/q09继续保留，本小段无新产品或测试失败。

[输入身份及未改路径](evidence/PERF001005_WORKSPACE_BYTES_20261025/SOURCE_MAP.json)、[所有样本/计数](evidence/PERF001005_WORKSPACE_BYTES_20261025/ANALYSIS.json)、[实际命令/UTC/JUnit/退出/退役](evidence/PERF001005_WORKSPACE_BYTES_20261025/runs)、[尝试/限制](evidence/PERF001005_WORKSPACE_BYTES_20261025/ATTEMPTS_AND_LIMITS.md)。单worker/低优先级/2处理器/256MiB测试堆，离线严格依赖验证，启动前Desktop归属为空但不是连续静默证明；未操作其进程或目录。成功自身夹具共476文件64,300,767B留证后退役，旧失败/备份/未知内容不动。

RAW **67,759B**，SHA256 `0cde4d611754e1ff8ebf5572af66faa51eb1704a2050221672606d670f63ec5b`，22清单项+manifest共23成员，CRC/逐项SHA核对。无新包/发布/日常资料或安装/真实服务、账户或用户暂停目录操作，新Project只准备不外发。独立审查NOT REQUESTED、用户/包集成NOT TESTED；资产/编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/UNKNOWN、PRODUCT_HOLD / NOT RC与全性能专项未关闭。继续低负载核对元数据提交中的冗余解析，若触及编码/UTF-8/失败与poison边界必须保留原语义、先有基线，不移除完整审计或复跑已接受旧基线。
