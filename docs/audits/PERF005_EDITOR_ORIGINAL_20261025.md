# PERF-005：编辑工程原始文件导出有界分配

2026-10-25，沿用持续性能授权。基线[a04d452f86c4fb568aabcad6826e62b02608ae90](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/a04d452f86c4fb568aabcad6826e62b02608ae90)，实现[5a91e999adab7d458dd7d1c6e7e2e4f444d0c892](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/5a91e999adab7d458dd7d1c6e7e2e4f444d0c892)。这是已有编辑页“原始文件 · 保留字节”入口的性能修改，不新建UI、增加格式或代签编辑页用户验收；无包或发布。

原`StudioWorkspace.export(...,"ORIGINAL")`经`workspace.bytes`用整文件ByteArray，再写出并计算回执。改用ec75083固定共享复制，保留AssetStore源校验和原有传输后`assets.read`复核，实际写出count/SHA用于原COMPLETE.txt。只在ORIGINAL分支提前返回；PRIVACY/NORMAL仍走原解码/渲染/编码、派生资产保留和隐私策略。新目标目录、每文件pending/force/原子移动、COMPLETE最后写及原件不变规则保持。旧命名仅根据首字节判jpeg/png，本次保留这一历史兼容行为；格式扩展和异步取消没有混入。

|合成PNG附加块|旧线程分配中位|新线程分配中位|旧 / 新墙钟中位|
|---|---:|---:|---:|
|3MiB|11,829,528B|603,544B|137.1 / 204.0ms|
|12MiB|46,432,216B|603,560B|403.0 / 405.9ms|

12MiB当前线程累计分配降低约98.7%；**不称提速、RSS或完整UI响应改善**。两次独立JVM各尺寸预热1次/实测3次，未控制JIT/GC/IO/force/其他进程。输入为16×16有效PNG加私有合成附加块，不代表真实格式/大像素多样性。两次均低优先级、单worker、2处理器、256MiB测试堆，离线严格依赖验证；启动前Desktop Java为空，不是连续静默证明。事后观察到Desktop PID51048创建22:53:35 UTC，晚于最终测试结束约22:53:02 UTC，未操作对方进程。

基线**1方法PASS**，最终**5个唯一方法PASS，0失败/错误/跳过，输入稳定**，不将重复基线/内部样本/先前16项计入本次。最终覆盖：分配边界与实际API、精确原字节/COMPLETE回执；历史命名/顺序/重复选择、不渲染编辑层/缺字库/不导出recipe、重开状态与资产数；第二原件损坏保留此前输出且无完成标记、不替换损坏源；非法/空/外来选择不碰未知现有内容；原批量undo及PRIVACY/NORMAL/ORIGINAL分类和备份回归。opaque头样本只核对旧原件字节行为，不认证图片解码。

[输入身份与未改路径](evidence/PERF005_EDITOR_ORIGINAL_20261025/SOURCE_MAP.json)、[逐轮原始数据与计数](evidence/PERF005_EDITOR_ORIGINAL_20261025/ANALYSIS.json)、[实际命令/UTC/日志/JUnit/退役记录](evidence/PERF005_EDITOR_ORIGINAL_20261025/runs)、[尝试和限制](evidence/PERF005_EDITOR_ORIGINAL_20261025/ATTEMPTS_AND_LIMITS.md)。基线27.265秒，最终33.522秒（均含Gradle，不作产品操作耗时）；成功自身合成夹具共230文件188,957,584B留证后退役。一次准备脚本引用错误未执行编辑，已在冻结测试前纠正；本段无产品/测试失败，旧锁RED/q09与用户否决原样保留。

RAW ZIP **65,233B**，SHA-256 `162b397510e245ad5bf0efa602a3f5a47acfcea32453d9518d72a3541ca2de5f`，20清单项+manifest共21成员，CRC/逐项SHA核对。共享helper、AssetStore、Desktop、请求与更新逻辑不变；没有重跑旧已接受基线或旧复制16方法。新独立审查NOT REQUESTED、用户/包集成NOT TESTED。编辑导出依旧在工作区串行锁内且未新增取消API，不能声称所有UI阻塞已解。资产/编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/UNKNOWN、PRODUCT_HOLD / NOT RC及整个性能专项未关闭。继续其他既有路径的低负载检查，未经具体授权不交新Project载荷或发布。
