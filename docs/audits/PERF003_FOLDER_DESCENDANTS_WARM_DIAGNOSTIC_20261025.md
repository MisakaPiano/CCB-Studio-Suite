# 组织目录候选01020b1：对称20次预热诊断

2026-10-25。应用仍[01020b1f82bff95dfacdb75f6318b3d2d258c626](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/01020b1f82bff95dfacdb75f6318b3d2d258c626)，实际执行测试源码[9512e88a9f78df9e373a8a1eb182992819475706](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/9512e88a9f78df9e373a8a1eb182992819475706)。二者唯一模块/工具差异是一行：原两方预热repeat2→repeat20；生产没有变化，原校验/断言/四轮交替/形状均不变。只执行一个测量方法，实际**1方法PASS，0失败/错误/跳过，输入稳定**，不与原3项相加，未增加其他功能覆盖。

|1000节点根，原生产候选相同|旧参考墙钟中位|新墙钟中位|解释|
|---|---:|---:|---|
|原两预热cohort|5.7879ms|12.4791ms|新较慢，原负面数据与原包完整保留|
|本次两方20预热|8.8963ms|7.8752ms|方向变化，不是生产修复或首用通过|

本次大根分配1,248,336→56,648B，部分小树/叶墙钟仍略高；全部样本见ANALYSIS。不因更高预热的有利结果关闭HOLD、抹去原cohort或宣称所有输入提速。没有JIT编译层级跟踪或隔离归因证据，**根因UNKNOWN**；预热敏感是观测，不能等同已证明JIT根因。首调用/冷缓存、完整UI、RSS/峰值堆/物理IO与真实图库未测。完整StudioState校验、4MiB预算、原始元数据/DFS顺序与未知根每轮仍核对；验证和记录在计时外。

02:08:37Z起16.131秒含Gradle；Windows/JDK17/Gradle9.1、离线严格校验、低优先级/单worker，测试2处理器/256MiB堆。相同一次性Desktop资源门，不连续监测、没有静默主机或安全软件因果结论。合成自有夹具留证退役、旧失败/未知/用户/备份保留，无实际目录/图片/凭据/服务/付费调用或日常安装/包/发布。

[应用/执行与唯一测试差异](evidence/PERF003_FOLDER_WARM_DIAGNOSTIC_20261025/SOURCE_MAP.json)、[全部样本及旧负面比较](evidence/PERF003_FOLDER_WARM_DIAGNOSTIC_20261025/ANALYSIS.json)、[原始参数/日志/JUnit/输入/清理](evidence/PERF003_FOLDER_WARM_DIAGNOSTIC_20261025/run)。RAW **39,398B**，SHA256 `396b7766e84eb5b2a8a8f8fb3a1e6e90632f4e853c417c08c7af705b887eaf10`，13清单项+manifest共14成员，CRC/逐项SHA通过。原[01020b1功能通过/性能HOLD](PERF003_FOLDER_DESCENDANTS_CANDIDATE_01020B1_20261025.md)及其84,579B包/SHA不改。

后续源码0564cc4改为原来的先筛选子节点再递归结构，仍一次有序结果，需自己的一组新证据；本诊断不代签该新源码。独立NOT REQUESTED、用户/包NOT TESTED，新Project只准备、不发版、不重跑旧接受批次/启动，资产/编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/UNKNOWN及PRODUCT_HOLD / NOT RC保持。
