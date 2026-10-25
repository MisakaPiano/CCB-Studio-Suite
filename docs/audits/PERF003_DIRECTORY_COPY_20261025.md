# PERF-003：目录相册两处防御副本复用

2026-10-25，前置源码[3bda776657a2908a2044d81f2c19c7ce542414f5](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/3bda776657a2908a2044d81f2c19c7ce542414f5)，应用/实际最终执行[605f89e8f8fd7d8cc9f93ade44135123b471c918](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/605f89e8f8fd7d8cc9f93ade44135123b471c918)。无新安装包/发布。

沿用[记录日期增量](PERF003_RECORDED_TIMES_20261025.md)已经核实的同一机制：Java Request.recipe每次getter会防御clone。目录相册 `galleryDirectoryAlbums` 与目录排序身份 `galleryAlbumIdentity` 的正常完整解析路径也各取两次，现每次调用只取一个局部副本，用于长度与decode。**不重复建立复制机制基线，不把此前分配百分比或4方法挪作本消费者结果。**本次没有新的分配/速度/帧/RSS数字。

仅两处取值段改变。分组第一次getter仍在runCatching外，与原takeIf相同；原<=16384判断改为等价>16384返回null，超限仍不解析。身份第一次getter仍在runCatching内，同一require<=16384保持。完整JSON/kind/version/路径解析、记录来源、移除目录名称、未知回退、身份键及哈希、折叠/Prompt规则、其余生产代码完全不变；Java构造/getter防御复制不变，无内部数组暴露/缓存/模型/UI/文件组织变化。修改前原字节在编辑前复制保留，最终原字节匹配实际source-before；各自仅CRLF→LF后等于固定Git blob，精确块替换证明其余文本未改。

最终**2唯一方法PASS，0失败/错误/跳过，输入稳定**，没有新测试源码。这是新源码的两个实际消费者回归，不是重开已接受的批次投影9方法或启动基线：

- `GalleryHistoryTest.directoryGroupingUsesOnlyRecordedProvenanceAndPreferencesSurviveReopen`：相同显示名的不同目录ID不混组，图片顺序、移除目录的来源保留、折叠偏好保存/重开、原图字节保持。
- `GallerySortingTest.naturalOrderIndependentAlbumsAndNewArrivalsKeepPreferencesAcrossReopen`：目录身份在新到图片/倒序输入后保持，各目录排序偏好独立、自然顺序、重开与原图字节保持。

本轮未新执行穷举的超限/坏JSON/未知分支；预算/解析/回退以固定源码对照保留，不用先前日期函数的分支测试代签本消费者。没有新性能基线或整页/UI/人工验收结论；本段无失败，旧失败/用户否决保留。

实际命令形态如下；完整隔离运行时/runpy、UTC和Gradle参数在原始记录。Windows/JDK17/Gradle9.1，离线严格依赖校验、低优先级/单worker，测试2处理器/256MiB堆：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf003-directory-copy-final-20261025 -- :studio-core:test --tests *GalleryHistoryTest.directoryGroupingUsesOnlyRecordedProvenanceAndPreferencesSurviveReopen --tests *GallerySortingTest.naturalOrderIndependentAlbumsAndNewArrivalsKeepPreferencesAcrossReopen
```

01:24:13Z起47.691秒含Gradle，这是检查时长，不是性能样本。显式一次性资源门在两个30秒间隔Desktop归属为空后启动；较晚边界记录不构成连续静默证明。未终止/改写Desktop或安全设置。新鲜所属合成夹具106文件36,200B留证后退役，旧失败/未知/日常资料/备份不动。

[源码/执行及两段之外不变证明](evidence/PERF003_DIRECTORY_COPY_20261025/SOURCE_MAP.json)、[范围/计数](evidence/PERF003_DIRECTORY_COPY_20261025/ANALYSIS.json)、[原始参数/JUnit/源输入/清理](evidence/PERF003_DIRECTORY_COPY_20261025/run)、[前后源原字节](evidence/PERF003_DIRECTORY_COPY_20261025/inputs)、[限制账本](evidence/PERF003_DIRECTORY_COPY_20261025/ATTEMPTS_AND_LIMITS.md)。RAW **49,530B**，SHA256 `b8f8cef62dd3a56365d26d5fa8723a1a435cc822e0525179a8f2e3a61c1c3541`，18清单项+manifest共19成员，CRC/逐项SHA通过。

独立NOT REQUESTED、用户/包集成NOT TESTED，新Project仅准备未外发。没有真实目录扫描、原图移动/删除、私人凭据/服务/付费调用、暂停采集恢复、日常安装、信任链/更新协议/包或发布变化。资产管理/编辑USER_NOT_TESTED、115体验/Prompt比较PENDING、q09 FAILED/UNKNOWN及PRODUCT_HOLD / NOT RC保持。继续已有PERF-003标签建议纯元数据定位：先测自然排序比较器重复生成键的成本，有据才调用内复用，保持prefix优先/自然排序/稳定同值、大小写/trim和作用域规则，不加全局缓存/改UI。
