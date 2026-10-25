# PERF-003：记录日期投影复用单次防御副本

2026-10-25，旧API基线[e66aeaeecbca6a6ccbc9b4ec5e70a97a4dde8945](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/e66aeaeecbca6a6ccbc9b4ec5e70a97a4dde8945)，应用/实际最终执行[492f8cc17d260a826a5a35019976dd90e450b7c4](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/492f8cc17d260a826a5a35019976dd90e450b7c4)。没有构建安装包或发布新版本。

`Request.recipe()` 在构造和getter均防御复制。旧 `libraryRecordedTimes` 为长度和decode分别取一次，现只取一个局部副本用于这两步；**防御API、完整JSON解析、16KiB预算、runCatching/未知拒绝、kind/version/必需时间戳、导入键顺序和作业last-wins覆盖全部保持**。不暴露/借用Java内部数组、不缓存、不改日期默认值/格式或来源。除这两个取值表达式外，整个生产文件及全部其他modules/tools相同；旧完整函数在测试中仅private/名称变化。

|最终同JVM四轮交替、两次预热/模式|每条recipe|旧 / 新线程分配中位|旧 / 新墙钟中位|
|---|---:|---:|---:|
|128条目录元数据|414B|473,392 / 418,096B|1.31 / 1.26ms|
|1024条目录元数据|1,182B|6,929,352 / 5,700,600B|6.21 / 6.08ms|
|1024条目录元数据|12,446B|53,066,932 / 40,303,560B|14.74 / 11.61ms|

分配约少11.7%、17.7%、24.1%，仍做全量JSON解析并分配其结构。数据为**直接构造的Asset/Request目录记录**，未写入真实资产库：payload不存在，长度1与全零指纹只是合成字段，不是原图/完整性/资产接纳验证。每条recipe在16KiB内，聚合目录元数据不是一个4MiB LibraryState文档；填充字段刻意覆盖允许大小，不代表通常receipt分布。不宣称整图库/UI/RSS/物理IO/所有Windows提速，CPU粗粒度且大样本两方同为15.625ms。基线单独JVM；表中为最终配对样本，取数在验证/JSON记录和源副本核对之前。

基线**1方法PASS**；最终**4唯一方法PASS，0失败/错误/跳过，输入稳定**，不累计尺寸/轮次/基线。三个新方法覆盖实际投影分配/值/顺序/recipe字节；错误kind/version、缺失/坏时间戳、数组/null/损坏/空JSON、16384B接受与16385B拒绝、零/负值、重复目录ID与作业/合成结果最后覆盖及键顺序与旧函数一致；修改调用方原数组/getter返回数组后防御API仍保全、重建映射无缓存污染。一个既有方法验证未知日期无推测默认、非法日历拒绝及多标签相册标识保持。作业只用SYNTHETIC账户/无效合成endpoint的记录，不构造客户端或发送HTTP。

证据收集首次检查失败：脚本仅考虑全LF或全CRLF，而该生产文件含一个CRLF、其余LF。**未修改/归一化实际源码，未重跑测试。**保留原失败脚本/错误和本地部分收集；最终原字节必须匹配最终source-before，反向替换精确实现段必须重建基线source-before散列，再分别仅CRLF→LF与固定Git blob逐字核对。两个测试输入原字节一并存入证据，不是去掉hash断言或接受不匹配输入。收集失败计0追加Gradle/JUnit，本段无产品测试失败，旧失败/用户否决不变。

实际命令形态如下；使用隔离python运行时/runpy加入tools，Gradle9.1/JDK17、Windows、离线严格依赖校验、低优先级/单worker、测试2处理器/256MiB堆，完整参数与UTC在原始summary：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf003-recorded-times-final-20261025 -- :studio-core:test --tests *LibraryRecordedTimesPerformanceTest --tests *LibraryInteractionTest.multiTagAlbumsHaveUniqueKeysAndUnknownDatesStayUnknown
```

最终01:17:05Z起20.18秒含Gradle。门控在两个30秒间隔Desktop归属为空后启动；基线期间Desktop PID43340于01:15:14.690271Z创建，落在基线01:15:13.707946Z起17.046秒窗口，进程存在重叠，具体CPU/方法采样重叠UNKNOWN；不能称无干扰主机。最终较晚观察与原始时间分列，不连续监控或作速度因果归因。没有强杀/干预Desktop或安全设置。

[源/执行映射及不变范围](evidence/PERF003_RECORDED_TIMES_20261025/SOURCE_MAP.json)、[全部样本/计数](evidence/PERF003_RECORDED_TIMES_20261025/ANALYSIS.json)、[命令/JUnit/输入/清理](evidence/PERF003_RECORDED_TIMES_20261025/runs)、[两份实际源原字节](evidence/PERF003_RECORDED_TIMES_20261025/inputs)、[收集失败与修正依据](evidence/PERF003_RECORDED_TIMES_20261025/COLLECTOR_FAILURE.md)、[资源边界](evidence/PERF003_RECORDED_TIMES_20261025/resource-boundaries.jsonl)、[限制账本](evidence/PERF003_RECORDED_TIMES_20261025/ATTEMPTS_AND_LIMITS.md)。新鲜所属夹具仍先封存再退役，未生成payload文件，0文件/0B；旧失败/未知/用户/备份不动。RAW **81,684B**，SHA256 `9d84529c6309484eb49f66d7b5e21cfed2d8364d0a4fc3210e5a7d0c65d710ba`，27清单项+manifest共28成员，CRC/逐项SHA通过。

独立NOT REQUESTED、用户/包集成NOT TESTED，新Project材料仅准备未外发。无原图读写、真实图库/凭据/服务/付费调用/暂停采集恢复、日常安装、签名/信任链/更新协议/包或新发布。资产管理/编辑USER_NOT_TESTED，115体验/Prompt比较PENDING，q09 FAILED/UNKNOWN及PRODUCT_HOLD / NOT RC保持。继续低负载核对目录相册/身份的同类元数据取值；完整解析、来源/折叠/标识与原图语义不改变，有基线证据才最小复用副本，不扩展功能。
