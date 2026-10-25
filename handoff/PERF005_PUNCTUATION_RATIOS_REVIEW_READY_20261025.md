# 固定压缩表负面试验：仅准备复审

PREPARED ONLY / NOT SENT；此为未采用候选，不是性能修复交付。

- 实际候选[e8378806ce950bf3d22fff1dc3e71bc7c0d95548](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/e8378806ce950bf3d22fff1dc3e71bc7c0d95548)功能3方法PASS；前值复用3d9abb0/838b087、0新基线执行，不相加。[报告及负面样本](../docs/audits/PERF005_PUNCTUATION_RATIOS_EXPERIMENT_20261025.md)、[原始逐例/输入](../docs/audits/evidence/PERF005_PUNCTUATION_RATIOS_20261025)、[采用决定](../docs/audits/evidence/PERF005_PUNCTUATION_RATIOS_DECISION_20261025.json)。RAW89873B SHA256e97050aadfc269a36594ab3dc13348eefbab146763120546236d4cbcb7130e88，25项+manifest26成员。
- 候选只3private lazy固定数组+3引用，原数值/顺序/公式/layout保持；但8192loose配对2.636→3.613ms约慢37%，分配虽约少20%，性能HOLD / NOT ADOPTED，根因UNKNOWN。无加预热/筛样/求绿重跑，不隐藏未改控制也有时间波动。
- [2c352961f2023464bbec0475722b78cdaa52d4ce](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/2c352961f2023464bbec0475722b78cdaa52d4ce)已恢复，全部modules/tools与实际3d9abb0相同；恢复0新测试，仅输入等价证明。候选新基准退出默认套件，源码及原结果在固定commit/raw保留，不动日常文件/失败现场。
- 只合成vertical layout、文档完整验证/预算在计数外，非字体/像素/UI/RSS/用户/包或独立验收。独立NOT REQUESTED，资产编辑用户未验、115/Prompt PENDING、q09/PRODUCT_HOLD及旧失败/否决/异步误写/路径UNKNOWN不改。无新包/tag/TUF或Desktop开发。

如用户批准交此具体对象，Project核候选/前值/恢复三者映射、完整原始负面值与未采用决定，独立判断即可；不为得到通过重复测量，不把负面试验收尾称作优化完成。
