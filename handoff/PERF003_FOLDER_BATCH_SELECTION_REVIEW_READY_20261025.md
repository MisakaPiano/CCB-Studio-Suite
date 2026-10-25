# 文件夹批次筛选性能增量：仅准备复审

PREPARED ONLY / NOT SENT；无具体新载荷外发授权，不发送Project，不启动Desktop开发或发布。

- 应用及最终实际执行：[1256c64ddd4291f585be4ff8d086f98a404a972b](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/1256c64ddd4291f585be4ff8d086f98a404a972b)。基线974e1007da207a47a27153cb34aec7380c98ff69；最终4唯一方法PASS，基线3另列，不累加。
- [完整实现/实测/未验报告](../docs/audits/PERF003_FOLDER_BATCH_SELECTION_20261025.md)、[SOURCE_MAP](../docs/audits/evidence/PERF003_FOLDER_BATCH_SELECTION_20261025/SOURCE_MAP.json)、[原始逐例](../docs/audits/evidence/PERF003_FOLDER_BATCH_SELECTION_20261025/runs)、[源码差异](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/974e1007da207a47a27153cb34aec7380c98ff69...1256c64ddd4291f585be4ff8d086f98a404a972b)。RAW116842B SHA256 a5a089a2e5e1059320aac57b10f3ab8e9e88d063bfe1af20c96c1a1d83b118f7，30项+manifest31成员，CRC/逐项SHA通过。
- 仅每筛选一次求子树，原递归/验证、批次顺序、归档策略差异、null短路、原布局/图片索引/来源/队列/容量不变。核心3+纯桌面卡片适配器1；原生树/面包屑/整页、UI/RSS/峰值堆/长期资源、包/人工未验。256/512根配对201.039→0.486ms、161761928→322720B只作局部样本，不保证整机收益。
- [此前遍历试验未采用及全部负面样本](../docs/audits/PERF003_FOLDER_TRAVERSAL_EXPERIMENT_20261025.md)仍保留，不改判；旧接受批次9/启动未重跑，16报告索引原fa58a50边界不变。本段无新失败；原失败/用户否决、q09 FAILED/UNKNOWN、资产/编辑USER_NOT_TESTED、115/Prompt PENDING和PRODUCT_HOLD / NOT RC保持。
- 没有包/version/tag/SHA256或更新元数据新映射，因没有构建发行包；公开115和原更新修复包的对应关系仍由各自旧固定报告定义。独立NOT REQUESTED、人工/包NOT TESTED；不自行宣称Project通过。

若后续用户明确授权交审，请Project先核对固定源码/实际输入与raw逐例、四处调用的空候选/归档/顺序等价以及本段测量边界，独立裁定本段；不外推整个性能专项、日常安装、全部Windows或Desktop验收。
