# PERF-003：批次组织投影的定向性能优化

2026-10-25 Asia/Tokyo。原协调会话`01a16f0a-b53e-7062-a303-f975e672a0db`转交用户继续性能专项的授权：承接现有PERF-001～007，测基线、定位瓶颈，再做证据支持的最小优化；不发布、不启动日常应用或暂停目录、不扩展资产管理/编辑功能。开始于`feature/windows-rc-continuous@e4d3d5c554bc73d8f653b788a3122f833469fdee`，index/worktree干净。本轮只推进PERF-003的批次列表及组织树元数据查询，不关闭整个性能专项。

此前下载清理P2已获[Project独立限定关闭](PROJECT_DOWNLOAD_TERMINAL_CLOSURE_20261025.md)，不重跑其24项或以前18 JVM/56 Python。旧内部包和公开115均不含该源码修复，也不含本次优化；本轮没有新安装器或应用映像。

## 固定源码与测量路径

- 治理/关闭回执：`cc745d17d7ced54d867655c72bd189eeb953dab1`，与性能源码分开。
- 基线测试：[`69f53d314abe0aa58730ac5ed4aadffd1ff7b940`](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/69f53d314abe0aa58730ac5ed4aadffd1ff7b940)。只加合成测量方法，生产仍与e4d3d5c一致。
- 实现与最终执行：[`98666fba90e0ebc10eb1b800a788b803ac211e1e`](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/98666fba90e0ebc10eb1b800a788b803ac211e1e)。实际运行输入逐项映射在[SOURCE_MAP.json](evidence/PERF_ORGANIZATION_20261025/SOURCE_MAP.json)，最终完整源码磁盘散列在`runs/final/source-before.json`，执行结束稳定。
- 差异：[69f53d3 → 98666fb](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/69f53d314abe0aa58730ac5ed4aadffd1ff7b940...98666fba90e0ebc10eb1b800a788b803ac211e1e)。三份生产文件、两份测试文件，无core/asset-library/image-authority、Gradle依赖、打包、签名或更新协议修改。

`OrganizationPerformanceTest`建立100/1000/10000张纯合成内存图片来源，每10张一批；不读用户图库或原件。先执行生产`organizationFolders`旧路径，基线1个方法通过，1万张/1000批次三轮111.3～155.9ms。期间Desktop第二次启动构建，墙钟时间仅作规模定位；原始轮次和环境限制保留，不外推静默系统速度。

最终同一个测试JVM内运行固定旧算法和新生产函数：旧函数从69f53d3精确提取到**仅测试**的`OrganizationPerformanceLegacy.kt`，除函数名外逐字核对。每规模两次预热、四轮交替先后顺序，新建索引成本包含在新路径中；复用快照另列。每轮核对组织种类/ID/名称、批次及图片顺序、图片集合完全一致；不因时间不理想筛掉轮次。最终启动前及完成后未观察到Desktop JVM，不宣称连续监控或所有后台任务静默。线程CPU采样粒度约15.625ms，零读数不表示没有CPU开销。

## 瓶颈及改法

旧批次卡片和展开组织树逐批执行`org.batches.find`、对整个`batchOrigins`做`filterValues`，列表规模成本含图片数×批次数的扫描；换选择/重绘还会再次请求这些集合。

新增[GalleryBatchIndex.kt](../../modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/GalleryBatchIndex.kt)一次按批次索引**已通过原有来源核验的投影**，按需建立该批次“组织记录图片＋回执图片”的有序去重集合。查询未知批次返回空，不将任意查询ID加入缓存。批次卡片、组织树及当前范围筛选共用该视图索引。`GalleryWorkspace`以组织记录和历史投影作为`remember`输入；重命名、归档、删除或来源变化后重建，离开组合后没有全局静态索引继续持有它。

保留回执先于仅组织批次的顺序、组织分组优先、已移除批次回退为自动、旧名称与日期显示语义、虚拟相册/Project/Chapter层级及未知图片处理；原件/来源/任务不写入。索引是有界于当前快照的元数据引用，内存规模随来源数和已查询组成员增长；没有大图字节缓存，但本轮**没有测其保留堆或整机RSS**。

没有改成全局缓存、持久化第二套来源或取消完整性校验。组织文件夹的后代遍历、未托管来源批次树标题的旧回退查询、全资产折叠投影仍有潜在扫描成本；本轮没有为消除所有热点而扩大改动。

## 实测结果与验证

四轮中位数，单位ms；含新建索引与复用的列分开：

|图片/批次|固定旧算法|新算法含构建|复用当前快照|
|---|---:|---:|---:|
|100 / 10|0.473|0.284|0.210|
|1000 / 100|4.659|0.707|0.462|
|10000 / 1000|125.630|7.900|1.867|

1万项场景线程**累计分配**中位数3,750,764 → 2,937,804字节（含构建，约降21.7%），复用为1,471,440字节。这不是保留内存、峰值或RSS，也不是整页速度。所有原始值见[paired-comparison.json](evidence/PERF_ORGANIZATION_20261025/paired-comparison.json)及[MEDIANS.json](evidence/PERF_ORGANIZATION_20261025/MEDIANS.json)。

最终**9个唯一方法PASS，失败/错误/跳过0**：5个性能/语义方法＋图库组织、布局各1个＋命名归档/独立历史2个。验证覆盖来源单遍遍历及重复查询、重命名/新增/移除后的新快照和旧快照不变、13种范围与成员顺序对照、批次新旧计算、原有离屏组织/菜单/分割线/归档接线及合成原件保全。离屏Compose不是原生窗口或人工测试。前面1个基线方法在最终重跑，**不累加为10**；内部多轮、多规模、13种范围也不单计测试数。

Windows 10 Pro 19045 x64，JDK17.0.20.1，测试堆256MiB、ActiveProcessorCount=2、Gradle单worker、离线且严格依赖核验。通过现有`tools/Run-PreviewChecks.py`运行，Python嵌入环境用`sys.path.insert(0,'H:/CCB-Studio-Suite/tools')`＋runpy包装调用；原脚本副本保留。最终Gradle参数为：

```text
--no-daemon --max-workers=1 --console=plain
-Pkotlin.compiler.execution.strategy=in-process --offline --dependency-verification=strict
-PpreviewTestRoot=<本轮新登记payload>
:studio-desktop:test
--tests *OrganizationPerformanceTest
--tests *GalleryOrganizationWorkflowTest
--tests *GalleryLayoutParityTest
--tests *GenerationManagementWorkflowTest
```

准确隔离路径、命令、时间和逐方法状态见[runs/final/summary.json](evidence/PERF_ORGANIZATION_20261025/runs/final/summary.json)与同目录JUnit XML/原始gradle.log。首轮因Desktop启动构建而在测试前中断，明确NOT TESTED；失败的脚本启动、UTF-8来源提取和证据收集准备也在[ATTEMPTS.md](evidence/PERF_ORGANIZATION_20261025/ATTEMPTS.md)分列，不改作产品通过。没有强杀其他项目进程。

本轮新夹具沿用已有OWNER/清单生命周期：成功后外部留证再自动退役，回执分别保存；中断夹具保留，无历史目录/私人文件清理。未启动原生应用，无付费调用、暂停大目录恢复、信任链变更、Desktop开发、日常安装写入或发版。

原始ZIP：96,661字节，SHA-256 `44d81e0143e09ca5f5aaa6bc978b0612ec4b10bb49a47acb72fcee5941b9b030`；29条清单文件＋manifest＝30成员，CRC及逐项散列本地核验通过。见[RAW_EVIDENCE.zip](evidence/PERF_ORGANIZATION_20261025/RAW_EVIDENCE.zip)。旧固定更新审查证据不覆盖、不改写。

## 状态与下一动作

实现及上述自动检查完成；**性能独立复审PENDING，用户手动验收未增加，安装包集成未测**。资产管理/编辑仍USER_NOT_TESTED；q09旧FAILED/根因UNKNOWN、115命名归档/组织树/任务栏体验、Prompt比较PENDING及PRODUCT_HOLD / NOT RC保持。不能用本轮离屏测试签收这两个界面，不能借本轮优化宣布PERF-002/007长期资源或整个专项完成。

下一动作：以此固定源码和原始证据定向复核PERF-003；后续再按原待办确认PERF-002/007的整页持续资源测量范围。没有自动发版或要求用户重复已确认人工。
