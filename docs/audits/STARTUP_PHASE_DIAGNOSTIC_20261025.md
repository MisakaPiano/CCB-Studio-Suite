# PERF-002/007 启动阶段隔离诊断（未改产品，未发布）

2026-10-25 Asia/Tokyo。本段承接已授权的启动定位，不重跑已获Project限定接受的4ec1979/3cd2dab原生基线，不新发交审。**已完成两次独立新JVM的启动测量；主要可见区间是窗口创建及首次绘制，但尚不能确定某个生产方法的可优化根因。**没有产品逻辑、数据模型、UI或生产接线改动。

## 固定身份与执行

- 起点6add1274e5cbfbfbe48391b677330464b6dfee16；测试探针/诊断工具80125d4601355a6f7f22feea98924c6e0c2031be；仅工具的CRLF身份核对补正后固定为[8a757a6121c219dd942358d25c7a3707bc1fa8db](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/8a757a6121c219dd942358d25c7a3707bc1fa8db)。生产目录与更新运行时相对起点零差异。
- Windows 10 19045 x64、Adoptium Java 17.0.20.1、测试堆上限512MiB、ActiveProcessorCount=2、1320×880原生Compose Window。48个合成资产ID，**仅1个唯一PNG SHA**，832×1216；空账户，提供者发送拒绝，目录采集保持暂停，默认入口关闭；不访问私人资料。
- 一次`:studio-desktop:nativePerformanceInputs`编译/类路径导出，**COMMAND_SUCCESS，0 JUnit**。构建在6add127未提交工作区完成：保留完整source-before散列与实际sourceStable=true摘要；编译的探针字节与后来固定源码完全一致。之后仅Python工具的文本身份检查变化，没有重编译不变Kotlin。运行输入1728项逐文件散列已在运行后复核；详见[SOURCE_MAP](evidence/STARTUP_DIAG_20261025/SOURCE_MAP.json)和[构建摘要](evidence/STARTUP_DIAG_20261025/build/summary.json)。运行时清单是本机二进制身份记录，不代表第三方已逐文件核验。
- 唯一成功测量为`run-b4de1224`：timeline-only PID41388、minimum-jfr PID23356；各启动前及结束后未发现Desktop Java启动器，均正常关闭并退出0，后来再次只读确认PID不存在。另一个prepare JVM仅准备数据，不计为第三次测量。每轮验证48原件散列、提供者调用0、关闭后缩略图待办/像素0。
- JFR仅白名单ExecutionSample、NativeMethodSample、ClassLoad、Compilation、GarbageCollection；使用合成HOME/TEMP及子进程环境白名单。环境/系统属性/安全属性/JVM信息及参数/系统进程五类敏感事件计数均0，才保留原始JFR及无损JSON gzip。配置见[固定JFC](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/8a757a6121c219dd942358d25c7a3707bc1fa8db/tools/startup-minimal.jfc)。没有使用默认profile配置或捕获其他进程。

执行命令（Suite根目录，顺序运行；先确认Desktop不在重负载）：

```powershell
& .local/windows-rc/updater-runtime-115/python.exe -B -c "import sys,runpy;sys.path.insert(0,'H:/CCB-Studio-Suite/tools');sys.argv=['tools/Run-PreviewChecks.py','--phase','startup-diag-inputs-20261025','--',':studio-desktop:nativePerformanceInputs'];runpy.run_path(sys.argv[0],run_name='__main__')"
& .local/windows-rc/updater-runtime-115/python.exe -B -c "import sys,runpy;sys.path.insert(0,'H:/CCB-Studio-Suite/tools');runpy.run_path('tools/Run-StartupDiagnostic.py',run_name='__main__')"
```

这是已执行命令的记录，不要求重跑。准确Java命令数组、UTC、PID、退出码、阶段JSON和JFR导出命令均在[成功运行目录](evidence/STARTUP_DIAG_20261025/runs/run-b4de1224)。父进程计时从发起Popen前开始，不是内核精确进程出生时间；首条main标记还包含测试类/管理接口初始化，不能称为纯JVM启动耗时。

## 实测结果与判断

|阶段|仅时间线（ms）|最小JFR（ms）|
|---|---:|---:|
|父启动请求→首条main记录|224|498|
|品牌初始化（含Swing隐藏根窗口/AWT及PNG）|562|438|
|工作区打开|122|148|
|工作区已开→application首次组合|173|173|
|HTTP客户端构造（无请求）|142|139|
|控制器构造|171|255|
|Window调用前→窗口内容首次组合|1626|765|
|工作台首次组合→提交|189|200|
|组合提交→首次完成内容绘制|611|610|
|完成绘制→窗口显示条件确认|454|294|
|父启动请求→窗口就绪（以上整段）|4347|3588|
|整个进程存活时间（含等待/检查/关闭/退出尾段）|10695|7625|

阶段之间还有少量标记与内容准备间隙，表内子项不能当作无遗漏加总。两轮的模式、缓存/加载/JIT状态不同，**不是改前改后对照**；不将JFR轮更快解释为优化收益。OS缓存未清空，准备夹具已暖缓存；没有统计显著性或所有机器启动承诺。

JFR记录9649个ClassLoad、43个≥10ms Compilation、12个GC事件、20个Java执行采样和234个原生方法采样。窗口创建区间出现Direct3DRedrawer.createDirectXDevice/chooseAdapter、原生库加载及AWT窗口创建，品牌区间出现WToolkit/D3D初始化，与阶段定位相符。Window区间有2276个类加载事件，提交到内容绘制有2498个，提示首次框架/图形加载占有明显区间；类加载和JIT时间可重叠/嵌套，不能相加成独占CPU或归因证明。

**不能把大量WToolkit.eventLoop原生样本解释为CPU热点**，它们可在等待。20个Java执行样本不足以把根因定在某个应用方法。控制器区间有5个SHA2样本，但它包含完整性/构造工作，不能据此删校验。JIT出现DataInputStream/类加载/压缩流等编译事件，其编译线程耗时也不是应用方法自身耗时。详见[可复算阶段及采样分析](evidence/STARTUP_DIAG_20261025/runs/run-b4de1224/ANALYSIS.json)、[分析脚本](evidence/STARTUP_DIAG_20261025/analyze.py)和原始JFR。

源码只读对照：StudioBrand的`configureDefaultWindows()`会触发Swing根窗口，`painter`惰性转换到Compose位图；Window标记区间包含icon/state参数求值、Compose窗口/图形创建，不能当成单个Window函数自耗时。没有为提速删除品牌、窗口功能、数据完整性或token计数。当前不做生产优化；最小下一步是在现有测试宿主内进一步区分图标转换、AWT窗口/Skiko初始化和首轮布局/栅格化的观察边界。该类隔离定位仍属已有诊断范围；若需要改生产接线、图形后端配置、功能时序或新增权限，应先具体说明取舍。

## 失败、保留和未验证

- 首次Python调用漏设工具搜索路径，导入失败，未建夹具/启动JVM；原命令、退出1与错误已存。**该初次调用没有捕获准确UTC**，不补造时间。
- `run-42ad01c3`在启动前因现有Gradle文本CRLF/Git LF身份检查失败；原始散列与失败保留。8a757a6只允许该已知文本文件用额外LF散列核对，未改Gradle/探针检查或放宽二进制身份。该空失败夹具保留。
- `run-52ae8690`检测到Desktop两进程，启动前推迟，没有夹具/JVM；不计通过。未中断Desktop或强杀进程。
- 最终只有2次测量通过、1次编译命令成功；不把上述失败/推迟、setup JVM、阶段数、JFR事件或旧基线重复累加为测试通过。见[逐次台账及限制](evidence/STARTUP_DIAG_20261025/FAILURES_AND_LIMITS.md)。
- 成功本轮所属payload留证后按既有机制清理320文件、70,384,907字节；[回执](evidence/STARTUP_DIAG_20261025/runs/run-b4de1224/FIXTURE_CLEANUP.json)。失败夹具、旧基线、复审证据及日常安装/备份未动。
- **未验证**：完整生产Main/打包启动、OS冷缓存、DWM实际呈现、物理磁盘IO、全系统/GPU资源与长期使用、用户真实图库、资产管理/编辑人工体验。2秒settle及正常退出不代表稳定空闲/长期无泄漏。此轮没有测量全部CPU/RSS资源，沿用原基线的原范围，不伪造新资源改善。
- Project对旧4ec1979/3cd2dab为[PASS_WITH_SCOPE](NATIVE_BASELINE_PROJECT_ACCEPTANCE_20261025.md)；本段新诊断**独立审查NOT REQUESTED，未发送**。q09 FAILED/根因UNKNOWN、用户否决、资产/编辑USER_NOT_TESTED、115体验/Prompt对比各自PENDING、PRODUCT_HOLD / NOT RC均保持。

原始证据[RAW_EVIDENCE.zip](evidence/STARTUP_DIAG_20261025/RAW_EVIDENCE.zip)：1,233,904字节，SHA-256 `a5eb28eb597763a306e558e006270ab9e8b58c3e6aa66929af1bbdba4a7fb99f`；[清单](evidence/STARTUP_DIAG_20261025/RAW_MANIFEST.json)55项，ZIP含manifest共56成员，CRC/大小/逐项SHA核验。旧原生基线ZIP仍为`02f92fa93d385eb29d760816d7540b38f048354edb714d8fc1217d2d98cd5714`，原字节未改。无安装器/包散列新增、无版本号/tag/签名/信任链或发布变化；公开115不变。
