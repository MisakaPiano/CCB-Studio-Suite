# PERF-001/007：提交后复用无损编码内容，保留Unicode与故障边界

2026-10-25，原持续性能授权内。基线[6966bc0ed80b312bea7a93efb76646fd590a645b](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/6966bc0ed80b312bea7a93efb76646fd590a645b)，实现[6dfadc02f544ce5394cf4e628f410ca695939a3d](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/6dfadc02f544ce5394cf4e628f410ca695939a3d)。未打包/发布，不改日常安装、凭据、签名或更新配置。

StateJournal提交后原本将刚生成的envelope bytes再次解码，只为取得body。现在encode返回私有运行时`EncodedState(bytes,reusableBody)`：编码body不含UTF-16代理字符才复用已有字符串；含任意代理字符（包括有效emoji）继续走**原来的envelope decode表达式**，保持UTF-8转换行为。这个类不序列化，不新增磁盘字段/版本。容量预检与实际提交**仍分别完整验证和编码**，不跨两次步骤缓存预备状态；否则可能改变可变输入及容量失败不poison的原规则，因此未采用更激进方案。

next.validate/body/envelope编码/4MiB预算表达式、整个打开链校验、safePath/reader/writer逐字保持；文档暂存、fault点、原子移动、previous hash、creationRevision、state、versionBodies及committed fault次序不变。本次复用的是本次实际编码的无损字符串，不是跳过SHA/审计或相信陈旧状态。

|合成FAILED任务detail载荷|旧线程分配中位|新线程分配中位|旧 / 新保存墙钟中位|
|---|---:|---:|---:|
|64KiB|3,078,436B|2,056,724B|28.31 / 25.20ms|
|256KiB|11,533,336B|7,561,672B|42.12 / 38.82ms|

256KiB场景线程累计分配降低约34.4%。实际调用workspace.save，各1预热/4实测，取数在保存返回后、JSON记录/摘要断言前。两次独立JVM及不同前置测试，JSON池/JIT/GC/文件force/调度未隔离；此为本机ASCII合成场景的分配记录，不称RSS/整机/端到端UI/通用提速。CPU采样粒度粗（部分0），不代表零CPU。代理字符内容使用旧解析路径，不承诺相同收益。

基线**2方法PASS**，最终**12唯一方法PASS，0失败/错误/跳过，源码稳定**，不累加重复方法、6Unicode/2crash内部case。3新方法覆盖实际保存/磁盘body/完整SHA/state/重开，ASCII/中文/转义/emoji/孤立高与低代理字符的真实持久字节和更新快照guard，源格式2正文跨仅版本3提交/重开保持、实质变化后拒绝旧快照。9原方法覆盖容量预检失败后仍能读写、未知/暂存/损坏/未来/删除链、两个自有子进程halt73故障边界、3恢复场景、2→3拷贝迁移及不透明合成加密容器/备份、设计提交中断不错误提升来源。

实测emoji及两种孤立代理字符均走旧解析分支；两种孤立字符仍保持历史UTF-8替换导致持久状态与live state不等、`UPDATE_SNAPSHOT_MISMATCH`拒绝旧快照的行为，**本轮未修复或掩盖该边界，也不宣称所有Unicode无损往返**。普通emoji保持等价。没有跨preflight缓存字符串或改变已有来源/请求算法。

[源码/字节保持断言](evidence/PERF001007_JOURNAL_COMMIT_20261025/SOURCE_MAP.json)、[逐例计数/全部样本](evidence/PERF001007_JOURNAL_COMMIT_20261025/ANALYSIS.json)、[Unicode实际分支](evidence/PERF001007_JOURNAL_COMMIT_20261025/UNICODE_CASES.json)、[命令/UTC/JUnit/退出与退役](evidence/PERF001007_JOURNAL_COMMIT_20261025/runs)、[完整账本](evidence/PERF001007_JOURNAL_COMMIT_20261025/ATTEMPTS_AND_LIMITS.md)。本段没有新失败，原计数口径失败、锁RED、q09和用户否决均保留。成功自身夹具317文件3,921,670B留证后退役，故障探针只主动终止自身的合成子进程，不结束任何外部/残留进程。

单worker/低优先级/2处理器/256MiB测试堆，离线严格依赖验证；Desktop繁忙时先等待，两个启动边界归属为空，不是连续静默证明。无用户图库/私人凭据/真实服务或暂停目录扫描，未操作Desktop/防护/权限。RAW **71,615B**，SHA256 `804d79d924b2660156bc74e6d5cb23aeb8897667061c9c32102bca544d6474c8`，26清单项+manifest共27成员，CRC/逐项SHA核对。

实现/自动验证如上，独立审查NOT REQUESTED、用户/包集成NOT TESTED，新Project只准备不外发。资产/编辑USER_NOT_TESTED、115体验/Prompt PENDING、q09 FAILED/UNKNOWN、PRODUCT_HOLD / NOT RC及整个专项不关闭。接续当前多个底层分配增量后的合成目录/受管副本集成观测，只验证新组合效果，不重跑已接受的003/启动基线或累计旧轮次，不发布。
