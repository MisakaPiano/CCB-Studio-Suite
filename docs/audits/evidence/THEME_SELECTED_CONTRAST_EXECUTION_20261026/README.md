# Studio主题对比度P2：仅本地三项回归通过

2026-10-26 Asia/Tokyo。本轮仅本地：没有向Project或任何外部对话发送，没有push、打包、安装或发布；此前自动送审接续已暂停。原独立裁定PARTIAL_ACCEPT / LIMITED_HOLD和P2 OPEN仍是最近独立结果，本地PASS不替代独立关闭或用户验收。

## 源码、执行输入与实际结果

- 应用/新测试源码：`c39e860252fdd96c9bec5b5d42d1abd988d3ec06`，本轮没有追加生产或测试源码变更。
- runner实际基点：`ed00cbb747a333999b27c072490927a5afe4958a`；与c39的modules/tools差异为空，运行前后sourceStable=true。执行时三个控制文档有本地政策说明改动，不属于编译输入；它们尚未推送。
- 参数覆盖和实际输入见`SOURCE_MAP.json`、`attempt-f/control/COMMAND.json`、`attempt-f/run/source-before.json`。源码中的Gradle512MiB堆/384MiB元空间没改，本次仅用Gradle命令行`-Dorg.gradle.jvmargs`覆盖为384MiB/256MiB；测试堆仍256MiB，单worker、单fork、CPU2、Kotlin进程内、离线严格依赖、低优先级、无常驻daemon。实际存在启动器、一次性Gradle JVM及测试JVM。
- F实际启动：2026-10-26 05:14:22.962 JST。Gradle报告89秒；runner95.83秒，含部分输入/结果校验，不是主题切换耗时。监测包围区间约112.6秒，包含引导/哈希/采集开销。
- **3个唯一JUnit方法PASS，失败/错误/跳过均0，exit0。**原四项测试类未修改、未执行；编译测试源码不计作执行旧四项。

|实际方法|秒|结果|
|---|---:|---|
|actualRouteAndLibrarySelectionBordersStayVisibleAndClickable|1.995|PASS|
|filledBatchCoverAndLibraryControlsRenderReadableForegrounds|2.942|PASS|
|indicatorUsesOriginalAccentWhenReadableAndPreservesEveryStoredRgb|0.037|PASS|

原始JUnit XML、逐例JSON、完整Gradle日志、源码前快照和保留回执在`attempt-f/run/`。像素数、RGB/模式循环、重复渲染帧和日志副本不增加方法数。XML stdout中的勾号被输出编码记为`?`，原字节保留；控件语义匹配原“✓ 使用中”及其前景像素的断言实际通过，不修改输出求整齐或把stdout当成字体视觉验收。

## 首次工具失败保留

E第一次调用嵌入式Python时遗漏现有的tools路径引导，`workspace_artifacts`导入失败，exit1，发生在Gradle/fixture创建之前，JUnit0；不是产品断言FAIL或OOM。原始`attempt-e/control/runner.log`与监测/命令保留。F只恢复既有`sys.path.insert + runpy`引导方式，资源参数、源码、断言和三项范围完全不变。没有放宽断言或重跑已接受四项求绿。

此前A资源0执行、B词法编译失败、C原四项PASS，以及86ad298的H/内存推迟、ed00cbb的D旧2GiB推迟都保留原固定字节，本次不覆盖或累加。

## 本次内存评估和观测

2GiB空闲是执行者原估算，未有最低需求实测。本次不以该数一律等待；保留H/C原空间预算和真实owner保护，基于当时实际物理可用、提交余量、分页及任务上限作一次有界尝试。Gradle堆/元空间上限合计下调256MiB，测试堆不变，避免同时更改离屏渲染测试的容量。原生内存、JIT、线程栈等不受堆上限完全约束，不能把896MiB配置相加当作实际总内存。

|指标|本次采样|
|---|---:|
|启动前Available|1,014,902,784字节，约967.89MiB|
|最低Available|356,499,456字节，约339.98MiB|
|本任务进程组私有提交采样峰值|1,305,767,936字节，约1245.28MiB|
|本任务进程组工作集采样峰值|1,206,693,888字节，约1150.79MiB|
|系统提交采样最大值|23,295,008,768字节|
|系统提交相对起点增加|859,889,664字节，约820.05MiB|
|最小观察提交余量|6,370,398,208字节|
|分页读取瞬时最大|1579次/秒；输入4396页/秒|
|监测次数/监测失败|25 / 0|
|其他任务进程冲突/组合严重压力采样|0 / 0|

发生过短暂分页读入，不能称完全无分页；未见OOM或达到预先记录的持续组合压力条件，未触发取消/强杀。观测末尾本任务JVM已退出。H最低观察仍5,413,830,656字节，高于原预算2,195,497,728，C也足够；没有为测试清理或搬移文件、调整分页/安全/ACL或结束无关进程。

采样可能遗漏瞬时峰值；各进程工作集可能共享页面，相加不是独占物理RAM。系统提交变化包含其他进程，不全归因于本任务。监测辅助进程未加入任务进程组，但已反映在系统计数中。`jvmFlags`是原始命令行正则片段，可能含转发参数，不能将Python/cmd/启动器片段当作生效JVM选项；实际Gradle PID19128观测到384/256、测试PID22732观测到Xmx256。

这证明这一次原三项可在低于旧2GiB初始空闲估算时完成，不建立新的全局1GiB门槛，也不声称其它规模、冷缓存或Windows环境同样可靠。本轮没有进一步缩堆、扩展测量或重做无关全套。

## 保留与停止点

新合成根`H:\CCB-Studio-Suite\.local\disposable\gradle-tests-defe092dca4d4c93b1bbe2662abc0dde`为`EXPLICITLY_RETAINED`，未自动清理。此包只包含工程控制/原始合成结果，不复制fixture payload、用户图片/Prompt、真实凭据或私钥。

原生真实视觉、Windows系统主题事件、独立窗口实时重绘及用户验收仍NOT TESTED；安装包NOT BUILT，公开115不含本主题。UPD-CLEAN-PROTECTION-001、资产/编辑用户未验、115体验/Prompt比较PENDING、q09及旧失败/否决保持。

本轮本地验证完成；没有后台编译、测试、资源门或新开应用窗口。不得按旧接续自动送审或push；外部交付留待后续明确授权。
