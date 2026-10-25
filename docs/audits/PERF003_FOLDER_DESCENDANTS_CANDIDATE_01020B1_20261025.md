# 组织目录遍历候选01020b1：功能通过，性能HOLD

2026-10-25，基线测试源码[e3989bc1f5e5f4a99caffee7419e4bb95d8f8b83](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/e3989bc1f5e5f4a99caffee7419e4bb95d8f8b83)，基线实际执行文档提交[d30e94de2119f636b6517a0da81e51dbd7dbdfb8](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/d30e94de2119f636b6517a0da81e51dbd7dbdfb8)（modules/tools完全相同）；候选应用/实际执行[01020b1f82bff95dfacdb75f6318b3d2d258c626](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/01020b1f82bff95dfacdb75f6318b3d2d258c626)。**这不是已接受的性能优化，不发布/集成到日常安装。**无新包或独立验收。

原方法逐节点filter、flatMap并合并新Set。候选只有descendants函数体改为一次有序结果、同样扫描原列表并DFS递归；没有父索引或缓存，没有移除/移动任何组织校验。整个其余生产文件、path/批次/归档/手动容量/队列逻辑及其他生产模块不变。旧函数在测试中为private扩展，只有接收者签名/递归名称变化；Set接口、内容/首次出现DFS顺序保持，不承诺具体集合类。

最初f6d680c只根节点的门控在测试启动前取消，**0执行**，记录工具退出1和取消原因；非产品FAIL/PASS。扩充根、两节点小树、叶、未知根四形状，每种32/256/1000节点，完整StudioState(version3).validate在计时外实际先行，序列化5,478/36,546/139,962B均<4MiB。没有遍历真实目录。基线实际**1方法PASS**；候选**3唯一方法PASS，0失败/错误/跳过，输入稳定**，不累计基线/内部尺寸/轮次。

除配对值/成员/字节及顺序SHA外，两项方法覆盖非拓扑输入DFS硬编码顺序、子树/无关/未知/空图、重挂父节点的新旧快照；原validator拒绝重复ID、无效UUID/名称/类型、缺父/循环、33层/1001节点，32层接受。公开helper仍按原约定接收已接纳图，未新增“遇坏图静默去重”的接受路径。没有新图像/原件/服务操作。

**两次预热/模式、四轮同JVM交替的配对样本显示需要HOLD的取舍：**

|形状|旧 / 新线程分配中位|旧 / 新墙钟中位|
|---|---:|---:|
|32节点根|41,152 / 3,000B|0.2246 / 0.0241ms|
|256节点根|335,616 / 22,760B|1.2932 / 0.9707ms|
|1000节点根|1,248,336 / 71,880B|5.7879 / 12.4791ms|
|1000节点两节点小树|648 / 232B|0.0129 / 0.0145ms|
|1000节点叶|240 / 160B|0.00825 / 0.00950ms|
|1000节点未知根|240 / 160B|0.00810 / 0.00875ms|

大根分配虽少约94.2%，**该cohort新墙钟约为旧2.16倍**，不能只因分配下降就宣布性能改善。大根两方CPU中位均15.625ms、计时粗粒度；根因UNKNOWN，JIT/预热/调度等尚无充分归因证据。小形状绝对差异很小，也不据此普遍提速。完整数据包含所有形状，未只挑有利样本。保持每节点扫描的O(folders×visited)，调用者每批次反复求子树仍未处理。

后续仅测试源码9512e88把两方预热次数由2增为20，生产01020b1不变，单测量方法做稳态诊断；**原两预热结果/包不改、不删、不重分类为通过优化**。更充分预热即使改善，也不能抹去原cohort或代签首用/完整UI。若需要生产返工，另列源码与证据，不重跑已接受批次9方法/启动或无关全套。

基线01:56:59Z起30.257秒；候选02:02:59Z起27.456秒，均含Gradle、不是单次helper时间。使用既有隔离运行时/runpy及Run-PreviewChecks，Windows/JDK17/Gradle9.1、离线严格校验、低优先级/单worker，测试2处理器/256MiB堆。命令分别只选测量方法/本新类三个方法；完整参数/UTC/JUnit/源散列在原始runs。Desktop资源让行、等待/取消与较晚边界记录保留，边界为空不证明持续静默。未干预Desktop/安全设置。

[源码/执行与方法体外不变证明](evidence/PERF003_FOLDER_DESCENDANTS_20261025/SOURCE_MAP.json)、[全部配对/基线样本](evidence/PERF003_FOLDER_DESCENDANTS_20261025/ANALYSIS.json)、[命令/JUnit/输入/清理](evidence/PERF003_FOLDER_DESCENDANTS_20261025/runs)、[匹配测试散列与Git内容的原字节](evidence/PERF003_FOLDER_DESCENDANTS_20261025/inputs)、[取消/候选准备记录](evidence/PERF003_FOLDER_DESCENDANTS_20261025/PROTOTYPE_AND_CONSUMERS.md)。RAW **84,579B**，SHA256 `4ed5cae62fc29993d7191f29b508c6a68ae86f0c5e9574ed85a62a1694a5a7e8`，27清单项+manifest共28成员，CRC/逐项SHA通过。纯元数据新鲜夹具无payload文件，留证退役0文件/0B；旧失败/未知/用户/备份保留。

状态：**功能自动通过 / 性能HOLD / 独立NOT REQUESTED / 人工与包NOT TESTED**。本候选未加入冻结在fa58a50的16报告复审索引。无真实资料/凭据/付费或供应商调用、暂停扫描、日常安装、UI/版本/更新/信任链/包变化。资产管理/编辑USER_NOT_TESTED，115/Prompt PENDING，q09 FAILED/UNKNOWN与PRODUCT_HOLD / NOT RC保持。
