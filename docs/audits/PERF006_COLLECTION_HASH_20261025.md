# PERF-006：目录采集复用同一输入的完整摘要

2026-10-25，承接持续性能授权，**未恢复用户暂停的大目录**。测试定义07d838c，实际基线[225318f7284599840874397383225b75fe45b6db](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/225318f7284599840874397383225b75fe45b6db)仅多任务文档；实现[cb819b4d81183ddda7e374f77abbfc141f57e663](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/cb819b4d81183ddda7e374f77abbfc141f57e663)。只有LibraryRepository.scan内6增/4删：首次完整SHA随同已读ByteArray返回，后续观察记录/版本/来源复用这一结果，消除相同输入第二次摘要计算。

安全路径、读取前后size/mtime/fileKey、32MiB图片/1MiB文本预算、静态图片与UTF-8校验、原件Store校验、来源/parent/标签、Prompt中断幂等、持久检查点/finally/取消、文件数量/递归上限均保留。没有采用只看mtime、跳过原件检查或自动恢复目录的方案；共享复制、AssetStore、Desktop、请求/生成/更新逻辑未变，不启动Prompt对比功能。

|每文件合成PNG附加块（每次扫2文件）|旧 / 新墙钟中位|旧 / 新线程CPU中位|旧 / 新线程分配中位|
|---|---:|---:|---:|
|3MiB|62.9 / 50.3ms|62.5 / 46.9ms|14,108,272 / 14,061,168B|
|12MiB|182.3 / 138.6ms|187.5 / 140.6ms|52,055,024 / 52,008,544B|

仅展示本机两次独立JVM各3轮的原始中位，不外推整库/真实目录/UI/RSS或所有机器提速。每尺寸2张不同16×16有效PNG、私有合成附加块，先导入、1次UNCHANGED预热、3次实测；测量包含原有catalog验证及有界字节读取，不是隔离SHA微基准。可确定的源码收益是同一输入SHA **2→1次**；读取数组分配仍随输入大小增长，未宣称目录内存优化已完成。

基线**1方法PASS**；最终**5唯一方法PASS，0失败/错误/跳过，源码稳定**，不累加基线重跑、内部2文件/轮次或两个中断点。覆盖实际UNCHANGED API及观察/资产数/原文件摘要/暂停设置不变，取消检查点与进度时工作区读可用，旧已提交图像无观察时复用，修改/丢失来源及图像/Prompt标签/备份恢复，Prompt在两种提交中断后重开幂等恢复。自动采集始终暂停，无watcher或用户路径；现有恢复回归也只用runner所属合成目录。本段无产品/测试失败，旧锁RED、q09及用户否决留在旧证据。

Desktop繁忙时先推迟基线、继续只读准备；两次实际启动前归属检查为空，单worker/低优先级/2处理器/256MiB测试堆。最终区间23:00:03Z起80.603秒，事后23:01:25Z观察Desktop PID51720创建23:00:16.670315Z，具体样本/活动重叠UNKNOWN，不声称静默或因果性能对照。未中断、清理或改变Desktop/系统防护。

[固定输入身份](evidence/PERF006_COLLECTION_HASH_20261025/SOURCE_MAP.json)、[完整样本与计数](evidence/PERF006_COLLECTION_HASH_20261025/ANALYSIS.json)、[实际命令/UTC/JUnit/退出/退役日志](evidence/PERF006_COLLECTION_HASH_20261025/runs)、[尝试/限制](evidence/PERF006_COLLECTION_HASH_20261025/ATTEMPTS_AND_LIMITS.md)。两个成功自身夹具共565文件126,078,663B留证后退役，未删旧证据、备份或未知内容。RAW **67,191B**，SHA256 `4be0c0986e4a49f3fd10e3dbb473fdb4c340f5d7fb998083a8b4d3451e397ee7`，22清单项+manifest共23成员，CRC/逐项SHA核对。

实现/自动检查如上，独立审查NOT REQUESTED、用户/包集成NOT TESTED。新Project材料只准备，不发新包/版本/更新日常安装，不访问真实图库/凭据/服务或启动Desktop开发。资产/编辑USER_NOT_TESTED、115体验/Prompt PENDING、q09 FAILED/UNKNOWN、PRODUCT_HOLD / NOT RC及整个性能专项未关闭。继续低风险性能定位，不把普通检查点当停止；共享读取器若要减少稳定文件的双数组，必须另有实际边界/增长/缩短/故障证据，不能用文件大小提示替代动态上限。
