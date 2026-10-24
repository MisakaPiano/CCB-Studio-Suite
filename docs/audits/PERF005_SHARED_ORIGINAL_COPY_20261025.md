# PERF-005：共享有界原件复制及排版原图导出

2026-10-25 Asia/Tokyo。承接21:35/22:07用户持续推进授权，Desktop资源优先；本段不是新UI/编辑功能或独立验收。上一[图库导出增量](PERF005_ORIGINAL_EXPORT_20261025.md)固定accdc59/c2ae633不改，本段继续排版“原图”同类内存路径，并解决共享实现的工作区锁持有问题。

## 固定版本与方案

- 排版实际基线源码`d8eb0cb5570c2265f7e81230bb1cec534acd019f`，1方法PASS。3MiB/12MiB合成PNG附加块，真实调用TypesetExport.create(original)，包含recipe和原始assetId回执；当前线程分配约11.98/46.59MB，确认整文件数组问题。
- 首版共享实现`72b1c2987583efa2797813b9dad8bd02e8793c30`，11方法PASS。Core新增[OriginalFileCopy.kt](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/5bfa51df9fdc632902cd329212d0b2fdaf9ba379/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt)，图库导出复用，排版original只保留12字节格式头，实际原件逐块写到私有pending，再不覆盖地移到原扩展名。其他变体仍走原渲染/字体/隐私/元数据流程；原有force、recipe、sourceId、文件身份和批次发布检查保留。
- 新锁回归`3d935c349c09a114f6a57c0753b7f5779fdfe955`实际**1失败**：合成写入检查点阻塞时，helper的workspace.operation覆盖整个输出，独立workspace.state读取不能完成。finally释放/等待自有线程，无强杀；失败夹具/日志不删，不改成通过。
- 最终修复源码[5bfa51df9fdc632902cd329212d0b2fdaf9ba379](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/5bfa51df9fdc632902cd329212d0b2fdaf9ba379)：只短暂检查workspace准入并取store引用，实际AssetStore.export继续持有其原串行准入/排水注册；helper不额外在目标写入期间占有workspace监视锁。TypesetExport既有外层operation未拆，不宣称所有排版/GUI读写均可并发。

共有实现固定256KiB缓冲，64KiB写块处可检查取消，SHA/实际字节数用于图库回执；成功验证后flush/force，失败保留未发布partial。AssetStore原前置验证/导出校验不移除，不承诺其验证读取能立即取消。12字节足以满足原ImageFormats.format的既有PNG/JPEG/GIF/BMP/WebP签名判断，原件体不重新编码；没有新增持久化格式、版本迁移、原件移动或来源逻辑。排版内层文件移动不带替换选项，另测注入同名未知文件时必须保留。

## 最终自动结果与指标限制

5bfa51d下最终**12个唯一定向方法PASS，失败/错误/跳过0，源码稳定**。7个Core方法（含2个既有渲染/字体/原件回归）、5个图库原件方法；基线1、首版11和新反例1不累计成25，也不把内部格式/边界/采样轮次算方法。主要覆盖：原件和recipe/sourceId/工程状态、5格式头分发、0/1/8/12/13及缓冲边界尾部、未知同名文件、原件损坏、取消/partial不发布、阻塞输出期间helper层的状态读取，以及原渲染/缺字体门禁。

|路径/12MiB|旧线程分配中位|最终线程分配中位|说明|
|---|---:|---:|---|
|排版original|46,585,240B|795,184B（降98.3%）|独立前后JVM，各预热一次/3实测轮；非同进程配对|
|图库original|46,438,672B|606,040B（降98.7%）|最终同JVM、4轮旧/新交替配对|

3MiB排版11,980,112→795,080B，图库11,835,568→605,976B。是测量线程累计分配，不是保留堆/RSS或实际界面内存验收。输入有效16×16合成PNG附加3/12MiB块，不覆盖真实编码/像素多样性；格式头专用用例是opaque资产分发等价，不冒充完整解码认证。

**不报告提速。**最终12MiB排版墙钟中位623.8ms（旧基线542.0ms）、图库306.9/371.8ms；较慢/波动轮次全部保留。排版前后不是配对JVM；额外摘要与IO/force/JIT/GC/缓存/系统背景的具体贡献未隔离。没有去掉校验换时间，不把零CPU或堆上限当实际消耗。2秒状态读取检查只是合成锁阻塞的确定性回归限时，不是产品GUI延迟承诺。

资源策略：单worker，测试256MiB、2处理器，启动前核对Desktop Java归属，无匹配才运行；串行构建。某次后续边界观察见Desktop PID53124/父11548，但未保留创建时刻，是否与测量重叠UNKNOWN；没有连续机器静默证据。没有中断/写入/清理Desktop，也没有据未知背景归因安全软件。墙钟不作优化证明；后续重负载时仅源码/测试准备。

## 原始证据和当前边界

[输入映射](evidence/PERF005_SHARED_COPY_20261025/SOURCE_MAP.json)、[计数/全部样本](evidence/PERF005_SHARED_COPY_20261025/ANALYSIS.json)、[尝试和未验](evidence/PERF005_SHARED_COPY_20261025/ATTEMPTS_AND_LIMITS.md)、[各轮原始命令/UTC/退出/JUnit/源码散列](evidence/PERF005_SHARED_COPY_20261025/runs)。成功所属夹具先留证后自动退役，失败root仍RETAINED_FAILED_FIXTURES、removed0；不删旧失败/基线/复审/日常安装。所需Gradle命令以runs/*/summary.json的arguments为准，已有执行不要求重跑；没有源码不稳定轮或编译失败被算通过。

RAW ZIP130,087B，SHA-256 `114fa07386bf7d6917faa23eb678476607376d064aedcffe8abe9265de19250c`；38清单项+manifest共39成员，CRC/逐项SHA检查。编译身份是固定源码与source-before+sourceStable实际记录，未单独归档全部class/安装包；没有新包散列。存储/权威请求/备份恢复/更新运行时未改，旧Project接受范围和归档TOOL_ACCESS_LIMIT不重开。

实现及本段自动检查完成；用户人工、实际UI/原生剪贴板、正式包与长期资源NOT TESTED；新独立审查NOT REQUESTED，只有[待授权交接材料](../../handoff/PERF005_SHARED_COPY_REVIEW_READY_20261025.md)，未外发。资产/编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/UNKNOWN、PRODUCT_HOLD / NOT RC保持，性能整体未关闭。

持续工作接续：沿PERF-001/005检查导入后的受管目录副本，其原件数组和完整校验读取仍可能叠加分配；只准备并测隔离合成基线，目录采集保持暂停，不扫描用户目录。普通技术选择不停等；缺新交审授权不阻塞独立工作，Desktop优先，不发布/动日常安装/真实资料/账户/付费服务/权限防护或其他项目。
