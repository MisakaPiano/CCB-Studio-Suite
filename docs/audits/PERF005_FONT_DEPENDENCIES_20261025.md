# PERF-005：字体依赖清单减少中间列表

2026-10-25。基线输入[b049e4c7bdc292a26e5623313c1c1d91933ca9b9](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/b049e4c7bdc292a26e5623313c1c1d91933ca9b9)，实际基线执行[639d4ada0630e006df9414c64264993c7ab9d395](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/639d4ada0630e006df9414c64264993c7ab9d395)，两者modules/tools输入等价；应用/最终执行[db46098445d1c1064ee77104fe320268cc6d0f93](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/db46098445d1c1064ee77104fe320268cc6d0f93)。无安装包、tag、版本、签名/TUF/更新协议或发布，日常安装不变。

原`TypesetFonts.documentFonts`用多层flatMap/map/listOf/plus拼接字体名称，反复复制中间列表。现仅将这一函数体改为调用内ArrayList依次收集，**顺序与重复项不变**：每页story字体→每个文本base字体→该文本全部实际tokens的字体→最后全部预设base字体。保留每条文本原tokens解析、未知/坏标签、错误及长度/嵌套限制；没有去重、过滤、跳过空文本/隐藏文本、添加输入/结果缓存或跨调用状态。预设仍只取base字体，不擅自解析预设文本。其他生产文本逐字保持，包括已有字体缓存/激活/校验/加载/渲染和权威Prompt/请求代码。

候选在执行前源码复核时选普通局部ArrayList，避免额外引入builder的只读实现行为；此前仅.local未执行草稿有buildList，已在PLAN注明调整，不是失败后调参或改测量输入。公开List的值/顺序契约保持；具体容器/强制可变转换、最终backing容量/常驻成本、峰值堆/GC/RSS未测，不能把累计分配下降说成所有保留内存都下降。

## 自动验证与数据边界

最终 **3唯一方法PASS，0失败/错误/跳过、源码稳定**。基线1方法另列，前四轮资源门各0执行，页面/引用/断言/轮次/线程不相加；没有重跑旧已接受批次9/原生/启动、颜色/竖排/富文本/分类或数值表试验。

- 配对实际documentFonts方法1：1/4/16页×每页8文本，每文本512 codepoint，纯文本/内联字体标签两组及8预设；完整有序字体列表/重复/摘要、文档字节与基线相同，另有独立小型story/base/inline/preset顺序向量。参考只复制原单行函数，仍调用同一个实际tokens，没有复制字体缓存或加载器。
- 边界方法1：空文档、空文本、隐藏/锁定文本、重复假别名、未知/escaped标签、预设只取base；可变副本清空不影响后续结果；1000层/100001字符文本的异常class/message与旧实现相同。无效预设长文本的直接helper行为保持，不代表其通过文档准入。
- 并发方法1：2个自建线程各50轮，完整列表、独立结果、调用者文档字节保持，正常shutdown/await。不代签全部平台、首次初始化竞争或长期稳定性。

文档实际`TypesetDocument.validate`、序列化<=4MiB及全部有序列表/摘要/输入快照检查均在计数外。16页/128文本/65,688引用，纯文本233,418B、内联标签1,167,306B；其他规模在ANALYSIS。**计数是完整documentFonts调用，包含原tokens解析和清单收集，不是单独append或整个document.validate。**假字体别名只有字符串，没有注册/导入/激活/解析实际字体资源、调用Font.createFont/GraphicsEnvironment/measure/render、打开窗口或访问工作区/原图/私人资料。TypeSet对象初始化在计数外，不是冷启动或字体可用性验证。

源码调用边界见[CALLER_BOUNDARY](evidence/PERF005_FONT_DEPENDENCIES_20261025/CALLER_BOUNDARY.json)：当前两个仓库调用方用于工程打开和预览前的activate(Collection<String>)；没有观察到对具体列表/可变cast的依赖。预览显式需要空文本base字体供第一字输入，工程打开仍保留隐藏文本依赖。**已有activate即使命中loaded缓存仍重新验证持久字体资产TAG和SHA**，本次完整保留；这只是调用方源码未改证据，没有新增实际字体加载、激活、渲染或工程UI回归，不能用它代替运行验收。

两种实现各2次预热、4轮同JVM交替的最终中位数：

|页/文本/字体引用|内容|旧/新累计线程分配B|旧/新墙钟ms|
|---|---|---:|---:|
|1/8/4,113|纯文本|752,888 / 580,768|0.881 / 0.838|
|1/8/4,113|内联字体|1,457,592 / 1,285,472|3.120 / 2.319|
|4/32/16,428|纯文本|3,117,352 / 2,375,224|2.192 / 1.912|
|4/32/16,428|内联字体|5,936,204 / 5,193,684|7.406 / 6.368|
|16/128/65,688|纯文本|12,848,584 / 9,338,512|3.137 / 2.647|
|16/128/65,688|内联字体|24,119,176 / 20,609,104|12.200 / 11.078|

本样本纯文本分配少约23%～27%、内联约12%～15%，最大少3,510,072B。各配对墙钟下降；独立基线JVM中4页纯文本1.267ms、16页纯文本9.196ms与最终旧参考不同，所有原始行保留，不挑有利阶段或调整预热，也不将局部墙钟外推为整个编辑器/备份/所有Windows提速。CPU时钟粗、0不代表零CPU，峰值堆/GC/RSS/物理IO/长期未测。

## 资源等待与实际执行来源

Windows/JDK17/Gradle9.1、离线/严格依赖、低优先级/单worker，测试2处理器/256MiB。实际基线17:40:23.538858 JST开始57.751秒、最终17:43:39.440315开始22.591秒，含Gradle，非表中局部计数。门控START回执17:39:47与runner实际开始17:40:23是不同边界，不把间隔当应用启动耗时。

```
python.exe -B tools/Run-PreviewChecks.py --phase perf005-font-dependencies-baseline-20261025 -- :studio-core:test --tests *DocumentFontDependenciesPerformanceTest
python.exe -B tools/Run-PreviewChecks.py --phase perf005-font-dependencies-final-20261025 -- :studio-core:test --tests *DocumentFontDependenciesPerformanceTest
```

前四轮各30分钟资源门因Desktop owner存在，分别15:47:13.789、16:18:44.063、16:52:09.849、17:27:05.440 JST明确回执RESOURCE_GATE_TIMEOUT_NO_TEST_STARTED/0执行，均在旧会话正常结束后以相同冻结输入排队；不是四次实际基线或测试失败。原始JSONL和期间文档commit保留，等待期间仅准备候选/调用边界/六行旧记录索引。第五次资源窗口才实际跑1方法，没有重复已成功基线或以未读结果断言0。

两次实际运行按最新Desktop owner两次30秒空边界启动；17:44:16 JST后边界为空，不是全程静默/CPU归因/另项目迁移成功证明。此前进度消息“正常退出”措辞过强，Studio侧实际只观察到owner已消失且可读查询为空，没有捕获其退出码；不能据此替Desktop验收。没有终止其他进程或降低门槛，Desktop大容量操作的独立结果以其自身证据为准。本次成功所属夹具清理均COMPLETE/0payload文件，旧失败/未知/备份/日常资料不动。

[SOURCE_MAP](evidence/PERF005_FONT_DEPENDENCIES_20261025/SOURCE_MAP.json)固定应用/实际执行/原字节、唯一函数体变化和完整解析器未变；[ANALYSIS](evidence/PERF005_FONT_DEPENDENCIES_20261025/ANALYSIS.json)包含所有样本/计数；[runs](evidence/PERF005_FONT_DEPENDENCIES_20261025/runs)含原始逐例、参数、输入/清理；[inputs](evidence/PERF005_FONT_DEPENDENCIES_20261025/inputs)保留实际生产原字节。RAW **90,353B** SHA256 `193b1af7467e2a807bc20996a842b4708f6c995461f965790a41c442d96117ae`，28项+manifest共29成员，CRC/逐项SHA通过。

实现及本段自动检查完成，用户/包NOT TESTED、独立NOT REQUESTED，[新复审只准备](../../handoff/PERF005_FONT_DEPENDENCIES_REVIEW_READY_20261025.md)，不新外发。原16项/两段补充/六行后续索引与原Desktop报告固定边界不扩大；六行索引d360487明确排除当时待观察的字体基线，不回写该固定索引。数值表候选e837880仍HOLD/NOT ADOPTED、2c35296恢复与负面样本保留。旧失败/用户否决/异步误写与路径错误前态/精确UTC UNKNOWN及禁止再访问约束保持。资产/编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、q09 FAILED/root UNKNOWN、PRODUCT_HOLD / NOT RC不变。

新主题需求UX-DISPLAY-THEME-001只按8fc577e登记排队、未开始，没有中断本次验证或修改主题设置/读取Desktop主题实现。按用户17:49 JST新顺序先收尾本轮性能记录，再排队主题色；不以长期PERF-001～007整体未关闭无限扩展本轮测量，也不把本段通过写成整个专项完结。内部包与验收仅在主题后按新授权准备，必须纳入f6598d3及精确身份映射；不等于公开发布或日常安装授权。Desktop资源优先与其他边界保持。
