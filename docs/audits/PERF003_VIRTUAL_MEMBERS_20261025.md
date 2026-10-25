# PERF-003：虚拟相册成员并集减少重复引用分配

2026-10-25，实际旧API基线[462790ad2d00c5b3e2e10d7e7ea0ab0bbf08faf5](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/462790ad2d00c5b3e2e10d7e7ea0ab0bbf08faf5)，应用及最终执行源码[1553d8bdc1e9d63503feae3c90cb4444ac8645ce](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/1553d8bdc1e9d63503feae3c90cb4444ac8645ce)。未生成安装包/发布版本；这是此前未测的成员函数范围，不重开已独立接受的批次投影或启动基线。

只有 `VirtualAlbums.members` 函数体改变：原算法把所有成员引用展开为大列表、再按重复引用总数预分配集合，且为每个节点线性查找相册；现保留同一次 `descendants` 调用和全量校验，建立调用内ID表、按原节点和成员顺序直接加入有序集合。**全量元数据校验、子树遍历、首次出现顺序、去重、未知节点拒绝和32层上限保持**。未增加缓存、数据格式或UI/入口变化，不移动/重写原图或来源。旧函数精确保留于测试，仅private/名称及descendants限定名变化；其余生产文件文本和全部其他modules/tools不变，见SOURCE_MAP证明。

|最终同JVM四轮交替，两个预热/模式|序列化LibraryState|旧 / 新线程累计分配中位|旧 / 新墙钟中位|
|---|---:|---:|---:|
|32相册×128引用，128唯一成员|164,047B|489,464 / 392,336B|1.04 / 0.87ms|
|128相册×512引用，512唯一成员|2,573,003B|7,723,688 / 5,892,672B|8.74 / 5.94ms|
|500相册×128引用，128唯一成员|2,562,947B|7,638,292 / 5,996,112B|6.38 / 4.87ms|

所有样本均低于现有4MiB持久化预算；没有用无法保存的巨大相册冒充实际规模。分配分别约减少19.8%、23.7%、21.5%，完整校验中的UUID解析等成本仍在，不能宣称常量内存、免校验、整页/RSS或所有Windows提速。CPU计时粗粒度、0值不等于零CPU，旧/新部分样本中位出现0与7.8125ms，不能据此推断CPU改善。原API基线单独JVM；表中只用最终同JVM交替样本。取数紧跟实际调用、在JSON/断言/重新序列化之前；内容/顺序和调用前后全部序列化字节每次均核对。

基线**1方法PASS**；最终**5唯一方法PASS，0失败/错误/跳过，输入稳定**，不累计基线/内部轮次。三个新方法：实际配对分配/结果/字节不变；非拓扑排列分支的节点及成员首次出现顺序、无关节点排除、空成员及未知UUID/字符串/空树的NoSuchElementException；重复ID/成员、无效图片UUID、缺父节点、循环、无关节点非法名称、33层拒绝与32层接受。两个既有方法：虚拟相册关联/移除/重开保留原图；任务移除后仍使用不可变生成回执投影来源。后者只构造合成账户/回执，不发送网络请求。Set接口、内容、迭代顺序保持，不承诺具体集合实现类。

实际命令（Gradle9.1/JDK17，Windows，单worker/低优先级、测试2处理器/256MiB堆、离线严格依赖校验）：

```
python.exe -B tools/Run-PreviewChecks.py --phase perf003-virtual-members-final-20261025 -- :studio-core:test --tests *VirtualMembersPerformanceTest --tests *VirtualAlbumsTest
```

这里的python为隔离更新运行时；实际执行通过 `runpy` 将仓库tools加入sys.path，完整Gradle参数、UTC、源输入与JUnit见原始summary/gradle.log。最终00:59:07Z起39.479秒含Gradle。Desktop归属进程连续两个30秒间隔边界为空后才开始，结束边界也为空，但未连续监测，安静主机状态仍UNKNOWN；此前等待时有Desktop worker，未强杀/修改其进程或抢占构建。本段只一次最终执行，无新失败，旧失败/用户否决均保留。

[源码/执行输入与旧函数映射](evidence/PERF003_VIRTUAL_MEMBERS_20261025/SOURCE_MAP.json)、[所有样本及计数](evidence/PERF003_VIRTUAL_MEMBERS_20261025/ANALYSIS.json)、[原始命令/日志/JUnit/源清单/夹具清理](evidence/PERF003_VIRTUAL_MEMBERS_20261025/runs)、[资源边界](evidence/PERF003_VIRTUAL_MEMBERS_20261025/resource-boundaries.jsonl)、[限制账本](evidence/PERF003_VIRTUAL_MEMBERS_20261025/ATTEMPTS_AND_LIMITS.md)。成功自身合成夹具41文件30,019B留证后退役，旧失败/未知目录/用户资料/备份不动。RAW **68,880B**，SHA256 `4de46dd958ee2788c410ce1420b9496be943ab4003dbd121ce5ae7c65fa2d0d7`，21清单项+manifest共22成员，CRC/逐项SHA通过。

独立审查**NOT REQUESTED**、用户/包集成**NOT TESTED**；新Project材料仅准备未发送。没有真实图库/私密凭据/付费调用/日常安装操作、恢复暂停采集、签名/信任链/更新协议变化或新发布。资产管理/编辑USER_NOT_TESTED、115体验与Prompt比较PENDING、q09 FAILED/UNKNOWN及PRODUCT_HOLD / NOT RC保持。接续既有PERF-003中的管理标签索引纯元数据定位：先容量内合成基线，有依据才最小等价优化，不改变标签、Prompt或资产管理功能。
