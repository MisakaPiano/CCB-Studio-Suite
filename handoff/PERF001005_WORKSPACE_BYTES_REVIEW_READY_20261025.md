# 完整资产字节返回分配增量：只准备，未发送

固定66ed23c44357b10de198eb2a488475b472b6ac81，基线b6abdc92d5b47f256e84e4c3da0695b5be3a6e03；[报告](../docs/audits/PERF001005_WORKSPACE_BYTES_20261025.md)/[原始映射](../docs/audits/evidence/PERF001005_WORKSPACE_BYTES_20261025/SOURCE_MAP.json)以本次证据提交固定。仅原bytes API内存收集器，原工作区锁、assets.export源验证/准入/排水及预算不变，不改其他StudioWorkspace代码；返回数组每次独立，无全局缓存/新格式。

基线1、最终7唯一方法PASS分列，计数从一开始排除JSON记录，旧失败保留。12MiB线程分配46,252,400→25,348,712B（约45.2%），仍需完整返回和分块合并；耗时70.27→62.33ms是独立JVM本机样本，非UI/RSS/普遍提速或用户/包验。大小/输入复用/数组独立/损坏/来源版本及Prompt恢复回归通过。

RAW67,759B，SHA256 `0cde4d611754e1ff8ebf5572af66faa51eb1704a2050221672606d670f63ec5b`，23成员。独立NOT REQUESTED、人工/包NOT TESTED，新Project只准备；无发布/日常资料/真实服务/Desktop操作。旧q09/用户未验/115/Prompt/PRODUCT_HOLD保持，继续原低风险性能定位，Desktop优先。
