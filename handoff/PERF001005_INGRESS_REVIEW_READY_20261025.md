# 受管副本性能增量：只准备，未发送

固定源码3086895c49d4f6f41231a187aee110abe0782013，基线a6a366ee9349796238ac73cbb2c3cb54e9614748；[报告](../docs/audits/PERF001005_INGRESS_COPY_20261025.md)/[原始输入与结果](../docs/audits/evidence/PERF001005_INGRESS_20261025/SOURCE_MAP.json)以本交付证据commit固定。只改retainIngressBatch的数组复制/复核，复用既有共享helper，不改32MiB预算、状态/检查点/进度/顺序、不恢复扫描。

基线1、最终4方法实际PASS，不累加重复/24内部资产。12MiB线程分配77,441,296→6,389,648B；耗时862.8→1017.3ms变慢，非提速/RSS/GUI/全导入或整专项验收。原件、修改目标、预算拒绝、部分取消/重开保持。新回归只用所属合成目录，旧失败/用户未验保持。

63,995B RAW ZIP，SHA-256 `9e6c5b43e4cad6ef425c88211570ea8012f58ab29ce1049a1ae848341edf949d`，20清单+manifest共21项。无新Project外发批准，独立复审NOT REQUESTED；可继续已授权低风险工作，不因缺交审授权全停。无包/发布/日常资料/账户/服务/权限/其他项目操作；旧P2/003/启动接受及q09/115/Prompt/USER_NOT_TESTED/PRODUCT_HOLD保持。
