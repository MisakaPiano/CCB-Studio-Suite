# 组织遍历试验：候选未采用，原实现已恢复

2026-10-25，恢复提交[92f8f0e41ab16b149524554033c9160fa5217666](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/92f8f0e41ab16b149524554033c9160fa5217666)。**没有把低分配但响应收益不稳的遍历候选作为修复交付。**只撤回本轮自己在descendants方法体的改写，原完整组织校验/DFS/未知根/批次/归档/队列保持。核对恢复后modules/tools排除src/test的差异为空，生产与此前已完成应用fa58a50完全一致；没有重置/清理仓库或覆盖无关修改，没有改日常程序/资料。

|阶段|应用 / 实际执行|实际执行范围|1000节点根配对旧 / 新墙钟中位|
|---|---|---|---:|
|单有序结果、扫描中递归候选|01020b1 / 01020b1|3功能方法PASS，两预热|5.7879 / 12.4791ms|
|相同生产的预热诊断|01020b1 / 9512e88|1测量方法PASS，二十预热|8.8963 / 7.8752ms|
|先筛选再递归返工|0564cc4 / 0564cc4|3功能方法PASS，二十预热|9.0902 / 7.6238ms|
|返工版原预热条件核对|0564cc4 / a59a783|1测量方法PASS，两预热|6.0345 / 13.3995ms|

各阶段输入稳定、JUnit0失败/错误/跳过。**功能PASS不等于性能候选被接受；相同方法跨源码/诊断不累加成8项。**更多预热下的有利cohort不用于抹去较低预热下的慢样本。返工版两预热分配仍1,248,336→124,856B，却约2.22倍墙钟；32/256/1000根、小树、叶、未知根的所有数据保留，未仅挑大根或有利样本。具体JIT/调度等根因UNKNOWN，没有独立归因，不能只提高预热数就关闭响应问题。本次选择保留原行为，逐层集合分配问题因此**未作为已修复关闭**。

原[候选HOLD及84,579B包](PERF003_FOLDER_DESCENDANTS_CANDIDATE_01020B1_20261025.md)、[预热诊断及39,398B包](PERF003_FOLDER_DESCENDANTS_WARM_DIAGNOSTIC_20261025.md)原字节/SHA保持。

返工二十预热的[源码映射/不变范围](evidence/PERF003_FOLDER_DESCENDANTS_REFINED_20261025/SOURCE_MAP.json)、[全部样本](evidence/PERF003_FOLDER_DESCENDANTS_REFINED_20261025/ANALYSIS.json)、[原始命令/JUnit/输入](evidence/PERF003_FOLDER_DESCENDANTS_REFINED_20261025/runs)。RAW **86,736B**，SHA256 `3585558edb0562be82efbb339a3085b7dc4747cb1b50f1b57913f3b596948d2e`，29清单项+manifest共30成员。准备收集脚本一次PowerShell引用错误在Python前停止，0测试，错误/修正脚本保留；不是产品失败或新的通过项。

返工两预热的[应用0564cc4/执行a59a783唯一测试差异](evidence/PERF003_FOLDER_REFINED_LOWWARM_20261025/SOURCE_MAP.json)、[全部样本/不采用裁定](evidence/PERF003_FOLDER_REFINED_LOWWARM_20261025/ANALYSIS.json)、[原始命令/JUnit/输入](evidence/PERF003_FOLDER_REFINED_LOWWARM_20261025/run)。RAW **39,093B**，SHA256 `af38a6e5e9fa31ea348cd1c09ba38011a6a4544dd97c4e2e3cef0a7f2dd4369c`，12清单项+manifest共13成员。两包CRC/逐项SHA通过，后者02:19:20Z起62.999秒含Gradle，不是单次helper时间。Desktop50020创建于02:19:15（门控后、实际Gradle窗口前）且后续仍存在，具体CPU/JUnit样本重叠UNKNOWN，不能归因或称静默主机。

默认测试集移除已否决的重复对照测量；它的源码仍完整保留于所有固定候选提交和证据。保留两个小型硬编码DFS/未知/重挂/原图结构接纳契约用例，命名为GenerationFolderContractTest，下一组聚焦回归执行，**尚未把新类标为已运行**。没有删除旧失败现场/原始证据、固定基线、未知文件或备份；纯元数据新鲜夹具按已有归属机制留证退役，未生成payload文件。

下一步只针对已定位的调用次数问题：目录过滤谓词每个批次都会求同一子树，准备每次筛选只求一次。必须保留归档筛选差异、原顺序、无候选时对空范围的短路、未知范围与UI原布局；先容量内合成基线、有据后最小改动，不重开旧批次图片索引9方法或启动基线。这是后续新源码范围，不是本次试验已完成项。

独立NOT REQUESTED、人工/包NOT TESTED；本试验未加入固定fa58a50的16报告复审载荷，不新发Project材料/版本，不改信任链/更新或日常安装，不读真实图库/凭据/服务/付费调用/暂停目录。资产管理与编辑USER_NOT_TESTED，115/Prompt PENDING，q09 FAILED/UNKNOWN及PRODUCT_HOLD / NOT RC保持。
