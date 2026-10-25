# UX-DISPLAY-THEME-001 / P2-SELECTED-CONTRAST：三项定向回归与资源实测

2026-10-26 Asia/Tokyo。用户2026-10-25 20:22 UTC明确答复“可以”，批准本次P2源码、三项合成回归和资源采样工程证据固定提交/push后，向既存原「阶段复审结论」提交一次限定复审。此授权仅替代此前本地执行阶段的暂不外发/push，不授权Release、tag、版本/信任修改、安装包构建、日常安装或扩范围。送达、开始和最终裁定须另读实际原页并分列，工程PASS不代签Project通过。

## 固定身份

|对象|身份/状态|
|---|---|
|P2应用及新测试源码|`c39e860252fdd96c9bec5b5d42d1abd988d3ec06`，本轮执行未再修改生产/测试源码|
|runner实际执行基点|`ed00cbb747a333999b27c072490927a5afe4958a`，modules/tools与c39差异为空|
|运行参数|临时Gradle堆384MiB/元空间256MiB，测试堆仍256MiB；单worker/单fork、CPU2、Kotlin进程内、离线严格依赖、低优先级、无常驻daemon|
|实际执行|2026-10-26 05:14:22.962 JST开始，runner95.83秒，Gradle日志89秒|
|唯一方法|**3 PASS，失败/错误/跳过0，exit0，sourceStable=true**|
|新安装包/版本/SHA|NOT BUILT；无可映射包SHA，公开Preview115不含本主题或本P2|

[SOURCE_MAP](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/SOURCE_MAP.json)分开记录29份源码/配置的Git blob与工作字节SHA。24份在runner实际source-before选择器中逐项匹配并由sourceStable证明前后不变；4份锁文件和验证元数据不在该选择器，明确列为Git/准备输入一致及事后观察，未补造运行前散列。完整原始快照及原始命令另存，不能把报告提交HEAD当编译源码。执行时三个控制文档有政策说明本地改动，未参与编译输入。

源码`gradle.properties`仍为512/384，没有修改全局或项目默认；本次以精确`-Dorg.gradle.jvmargs=-Xmx384m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2`覆盖，完整参数在[COMMAND](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/attempt-f/control/COMMAND.json)与[summary](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/attempt-f/run/summary.json)。Gradle实际复用未变模块UP-TO-DATE，本轮Studio生产/新测试均实际编译；未跳过编译复用旧错误源码。

## P2修复范围与三项结果

原[独立回复d5d317bf](https://chatgpt.com/c/6adcaaef-3fac-83e8-ae57-2a524281e049?messageId=d5d317bf-4708-4405-ad8a-87de63bd92f3)对30b0659/dce6600判定PARTIAL_ACCEPT / LIMITED_HOLD，核心/保存和原四项接受，选中态P2开放。此旧结论及问题ID保持，不重开整项目。

c39最小修改分开“原RGB填充”和“可辨认标识”：Batch生成方式、封面使用中标签、图库复选框等在强调色上用primaryForeground；新增只展示的selectionInk，在标准主题表面原强调色对比足够时原色不变，否则选黑/白中最小对比更高者，供导航、选择/焦点轮廓及既有Suite展示适配消费。不禁止黑白、不改RGB存储/格式/设置读写、原图、权威Prompt/请求、队列或按钮行为；不是全局改Gold或重设计换肤。具体16份生产/1新测试差异沿用[固定源码patch](evidence/THEME_SELECTED_CONTRAST_P2_20261025/SOURCE_DIFF.patch)。任意原图上的画布叠加不在标准表面对比保证内。

|本次唯一JUnit方法|实际秒数|范围|结果|
|---|---:|---|---|
|actualRouteAndLibrarySelectionBordersStayVisibleAndClickable|1.995|实际离屏导航及选中LibraryAction的标识/边框像素、点击回调，含默认蓝色|PASS|
|filledBatchCoverAndLibraryControlsRenderReadableForegrounds|2.942|实际Batch标签、封面标签、两种图库勾选控件的黑深/白浅前景像素|PASS|
|indicatorUsesOriginalAccentWhenReadableAndPreservesEveryStoredRgb|0.037|标准表面对比、模式/风格/系统输入、极端RGB、原值/格式和可读时原色保留|PASS|

原始[逐例JSON](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/attempt-f/run/test-cases.json)及同目录JUnit XML、Gradle日志完整保留。原四项StudioAppearanceTest源码仍同30b，未重跑或与本次合并计数；RGB循环、重复帧、像素数、XML副本不增加方法数。XML stdout勾号编码记为`?`的原字节不改，控件原“✓ 使用中”语义匹配/前景像素断言实际通过；这不是字体视觉或真人验收。

## E/F与旧记录分列

- E：执行者第一次调用嵌入Python遗漏tools路径引导，workspace_artifacts导入失败，发生于Gradle/fixture创建前，exit1、JUnit0。原始[失败日志](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/attempt-e/control/runner.log)和命令/监测保留，不能算产品断言FAIL、OOM或PASS。
- F：只恢复既有sys.path＋runpy引导，源码、三项、断言和资源参数不变；实际编译及3方法PASS。[RUN_MATRIX](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/RUN_MATRIX.json)分列，不把E的0、旧四项或日志副本累加。
- 原A资源延期0、B词法编译失败0、C原四项PASS，86ad298及ed00cbb的H/旧2GiB资源推迟均保留固定字节，不追改成已执行或通过。

## 资源测量及边界

旧2GiB空闲线是执行者未实测的保守估算，不是用户指定或Windows/Gradle硬要求。本次以当时Available/Free、约6.7GiB提交余量、初始分页0、原H/C预算和无其他重负载owner作一次受监测尝试。Gradle堆/元空间配置上限合计少256MiB，测试堆未缩；原生/JIT/线程栈等不受堆上限完全约束，不能把896MiB相加当实际进程总量。

|25次采样指标|实际结果|
|---|---:|
|初始Available|1,014,902,784字节（约967.89MiB）|
|最低Available|356,499,456字节（约339.98MiB）|
|本任务进程组私有提交采样峰值|1,305,767,936字节（约1245.28MiB）|
|本任务进程组工作集采样峰值|1,206,693,888字节（约1150.79MiB）|
|系统提交采样最大值/相对起点增加|23,295,008,768 / 859,889,664字节|
|最小观察提交余量|6,370,398,208字节|
|瞬时分页读取/输入峰值|1579次/秒、4396页/秒|
|监测失败/其他重负载owner冲突/组合严重压力采样|0 / 0 / 0|

[MONITOR_RESULT](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/attempt-f/control/MONITOR_RESULT.json)及同目录原始jsonl提供每次计数、派生私有提交/工作集、被观察进程生命周期峰值及本任务命令行片段。只跟踪本次runner后代，不导出其他私人进程内容。有短暂分页读入，未见OOM或达到预先记录的持续组合压力条件，未取消/强杀；末尾本任务JVM已退出。H最低观察5,413,830,656字节高于原2,195,497,728预算，C满足原805,306,368；无删除/搬移、OS分页/安全/ACL调整或无关进程操作。

采样可能漏掉短峰；工作集共享页不可当独占RAM，系统提交变化有其他进程贡献，监测辅助开销仅进入系统计数。jvmFlags是原始命令行正则片段，可能包含转发值，不能把启动器/Python/cmd的片段当生效参数；实际Gradle JVM19128记录Xmx384/MaxMetaspace256，测试JVM22732记录Xmx256。没有原512/384同源码对照或普遍最低内存测量，不主张性能改善/全Windows可靠或设新的1GiB硬线。

## 证据保全、发布授权与未验

[PUBLICATION_MANIFEST](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/PUBLICATION_MANIFEST.json)覆盖29份文件：26份原本地包原字节（其MANIFEST SHA-256为`2ea2c02995e00e20ca1c5bcff6cd93386acc0b2a8b8e5752bea956dd83ca0c1b`），另加字节保全属性和本次授权/发布清单。旧raw中“LOCAL_ONLY/禁止push”等字段记录的是原执行阶段，保留原样；仅由[20:22 UTC授权](evidence/THEME_SELECTED_CONTRAST_EXECUTION_20261026/PUBLICATION_AUTHORIZATION.json)替代当前交付限制，不能回写成执行时已有发布许可。内容仅任务工程文本/合成结果，source-before路径全部核为Git受控工程文件；无fixture payload、私人图库/Prompt、真实凭据/密钥、用户文档或无关聊天。

提交准备的通用diff空白检查曾拒绝两份原始Gradle日志的既有行尾空格/末尾空行，未修改日志求绿；只在格式检查精确排除`attempt-f/control/runner.log`和`attempt-f/run/gradle.log`，其原始字节、清单SHA及Git提交字节仍逐一核对。此准备检查不属于JUnit失败或新增测试。

新合成根gradle-tests-defe092dca4d4c93b1bbe2662abc0dde为EXPLICITLY_RETAINED，未自动清理。旧失败/否决/0执行与固定基线保持，未发新版本/安装或擅自打包。真实Windows视觉/系统主题事件/独立窗口实时重绘和用户体验NOT TESTED；公开115未含本主题，完整包/更新未验。UPD-CLEAN-PROTECTION-001仅静态、资产/编辑USER_NOT_TESTED、115体验及Prompt比较PENDING、q09 FAILED/root UNKNOWN及PRODUCT_HOLD / NOT RC不变。

本材料准备和公开工程字节可核验不等于原Project收到或通过；实际原页去重、单次请求/完整SHA/消息ID、回读、开始与最终裁定另留回执。当前不追加测试或重开其他专项。
