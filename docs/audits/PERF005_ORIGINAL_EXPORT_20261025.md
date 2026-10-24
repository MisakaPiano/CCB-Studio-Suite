# PERF-005：图库原件导出的有界分配优化（未发布）

2026-10-25 Asia/Tokyo。按用户21:35:32 UTC后续授权持续性能专项，Desktop构建/测试优先。本小段起点f5b008e干净工作区；不重跑已接受PERF-003/启动诊断，不再尝试旧ZIP访问，也没有新交Project。优化依据是实际合成测量的整文件中转分配，结果是有界内存分配，不宣称界面或总吞吐提速。

## 问题、实现和固定源码

原[LibraryDelivery.kt@f5b008e](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/f5b008ecf55ed2f6c9248a3fd3412baca6528d54/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/LibraryDelivery.kt)调用workspace.bytes：ByteArrayOutputStream增长并复制完整原件，再写盘/计算回执。底层AssetStore本就有逐块校验导出，故保留该准入与完整性实现，只移除上层整文件数组。

- 基线测试829c2397c47fc5ab1145949669907b1a64aca7f0：测试legacy函数体除c.workspace参数引用外与固定旧函数体逐字符一致，不重写权威请求或资产库逻辑。1方法实际执行，3MiB分配约11.8MB、12MiB约46.4MB，确认随文件大小增长。
- 首版流式4339ed189cc9dbb42fb4982657bf7c2d55fcd89a：5方法PASS，分配约0.34MB，但墙钟未改善。完整原始数据保留，不筛掉较慢轮次。
- 最终[accdc59e851164ee82870297a0bfc0e626ec55c3](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/accdc59e851164ee82870297a0bfc0e626ec55c3)：只改一份生产文件，保留原Controller入口，内部转发至同一workspace导出实现。CREATE_NEW/NOFOLLOW、safePath、workspace/store串行准入及AssetStore两阶段校验保留；写入时再摘要/计数以生成实际输出回执，不盲用预读metadata。固定256KiB缓冲合并底层64KiB块；仅验证成功/上下文仍有效才flush并force(true)，所有文件成功/最后取消检查后才写COMPLETE并原子发布目录。
- 写块处检查协程取消；AssetStore既有验证读取仍按原API完成，不承诺取消响应的毫秒上界。失败保留.incomplete原件副本，不删除、不伪报完成。没有图片重新编码、移动原图、外部引用或组织语义变化。核心资产库/格式/来源与请求模块、安装更新均未修改。

## 最终配对实测

Windows10 19045 x64，JDK17，Gradle9.1离线/严格依赖校验，1worker、测试256MiB/ActiveProcessorCount2。同JVM、各一次预热、4轮交替先后次序；每次实际导出后校验回执/输出/原始散列。输入是有效16×16合成PNG附加3/12MiB自定义块，不是用户图库或像素解码场景。

|输入附加块|旧线程分配中位|最终线程分配中位|降低|旧/新墙钟中位|
|---|---:|---:|---:|---:|
|3MiB|11,836,320B|605,992B|94.9%|144.6 / 149.1ms|
|12MiB|46,438,816B|606,056B|98.7%|351.2 / 369.7ms|

最终大文件线程分配约0.58MiB，未再随3→12MiB线性增长。**墙钟样本仍略慢，不能报告导出提速**；Windows写入/force波动、缓存、JIT/GC及系统背景未作因果归因，不把差异都归防护或磁盘。收益是降低整文件缓冲带来的累计分配压力；不是已用堆、保留内存、RSS节省或实际界面卡顿改善。线程CPU中位3MiB31.25/31.25ms、12MiB125/109.375ms，粒度粗，不据此作CPU百分比收益保证。

Desktop进程按已观察CommandLine中的Desktop归属在启动及若干边界检查，无匹配时才运行；记录是工具回执的边界观察，不是连续监控/共享租约或整机静默证明。未给Desktop发任务、终止进程、清理目录或修改其工作区。每轮小构建与测量串行，未安排并行重负载。

## 验证和计数

最终accdc59下**5个唯一定向方法PASS，失败/错误/跳过0，源码输入稳定**：

1. 四轮配对的实际输出/回执散列一致及分配有界。
2. 去重/顺序/文件名与旧回执完全一致，父目录既有文件和原件不变。
3. 后续原件损坏时保留首份partial，不写COMPLETE或发布最终批次，不修复/覆盖损坏输入。
4. 新文件已打开、等待workspace准入时确定性取消，输出保持未完成且原件不变；不靠竞速/强杀。
5. 目标在workspace内或预先取消均拒绝，不改变原件/目标。

基线1方法与首版5方法分别列于[逐轮结果](evidence/PERF005_EXPORT_20261025/ANALYSIS.json)，不累加重复成11，也不把warmup/内部轮次/断言计作方法。全段没有意外测试失败；故障注入的预期拒绝是通过用例的一部分。两个只读源码检索路径错误如实保存在[尝试台账](evidence/PERF005_EXPORT_20261025/ATTEMPTS_AND_LIMITS.md)，没有保护软件归因或修改安全设置。

使用既有Run-PreviewChecks，实际命令/UTC/退出码/JUnit和源码输入散列各在[runs](evidence/PERF005_EXPORT_20261025/runs)。基线参数只选择baselineOriginalsExportAllocation，后两轮只选择另5个方法，未重复全套、旧启动或003。成功所属夹具留证后自动退役；[ANALYSIS](evidence/PERF005_EXPORT_20261025/ANALYSIS.json)合计清理规则和字节可复算，旧固定证据/失败、日常资料与安装均未动。没有单独归档基线编译class原字节，身份由固定源码、编译输入散列及实际sourceStable结果对应；不冒充安装包映射。

## 交付边界与接续

已实现/自动执行通过；独立审查**NOT REQUESTED**，新Project载荷需具体授权，当前仅准备[限定复审材料](../../handoff/PERF005_EXPORT_REVIEW_READY_20261025.md)。原件导出实际UI、原生剪贴板、正式包、真实图库、长期资源/物理IO、备份恢复整体未由此测量覆盖；资产与编辑USER_NOT_TESTED，115体验/Prompt PENDING、q09 FAILED/根因UNKNOWN、PRODUCT_HOLD / NOT RC保持。PERF-005乃至001～007整体不关闭。

原始证据[RAW_EVIDENCE.zip](evidence/PERF005_EXPORT_20261025/RAW_EVIDENCE.zip)：94,445B，SHA-256 `17ee15cfbb730155599806f66fbbeec436609cb8bd35ae5dd659526e326f28ef`，26清单项+manifest共27成员，CRC/逐项SHA检查。无新安装器、tag、元数据或发布。继续在已有PERF-005中低负载检查备份/恢复大文件路径，先判定是否已有有界实现；没有新瓶颈证据不硬改，不因本检查点反复等待普通技术批准，Desktop重负载时只准备源码/测试。
