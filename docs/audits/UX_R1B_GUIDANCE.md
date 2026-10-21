# UX-R1b 图像引导与 Q09 工程进度

2026-10-21，继承已推送/远程核验8691443。R1b仍 **IN_PROGRESS / PRODUCT_HOLD**；反推、后处理、全部历史回用与完整参数回归继续实施，R2–R4未开始。此为安全可恢复中间检查点，非完整R1b或RC交付。

保留 Desktop 固定5ef9542图像编辑/工具/蒙版/聚焦/参考算法，Suite接资产ID、受保护凭据与每次请求预算。Q09唯一负责仍R1b：来源Canvas在实际Compose场景中执行四次旋转、区域打码、undo/redo/reset、画蒙版/清除/撤销、应用、保存副本及关闭重开。旋转适配补充焦点框随图旋转并清除旧蒙版；undo同时保留图、蒙版、框。完成回调等待持久化，不能提前关闭并误报成功。

图生图/聚焦重绘/精确参考/Vibe原始导入不改，按Desktop来源归一化为单独派生PNG。聚焦服务原始响应先作为原件提交；合成图另建有父关系的派生记录。合成失败仍能读取唯一原始响应。Vibe仅cache miss计一次请求预算，cache hit无网络。

发现大图片Base64会超过旧1MiB资产描述/4MiB日志限额，已修复实际消费者：GenerationReceipt v3将二进制请求字段存入既有AssetStore，不新造数据核心；每段≤8MiB、带整体size/hash，日志只保留引用。可恢复完全相同的实际请求JSON并核验wire hash；v2旧回执仍读。Vibe编码单独引用，回用时恢复。请求载荷标明角色，不伪装成图库图片；备份/恢复随现有资产事务携带。文档超限在写入前拒绝，保持原工作区可读，未放宽校验/静默删历史。

最新串行严格回归 **155实际PASS、0FAILED、0SKIPPED**：authority99、core30、desktop26。包括大于1MiB参考图真实本地HTTP请求、备份恢复后精确还原；9MiB分块校验、错误descriptor拒绝；合成失败保留原件；源Canvas操作、Vibe缓存预算。此处均为合成输入/loopback，不是提供者实际结果或真人认可。

保留此前缺少两个来源符号的compile失败、JUnit返回类型导致的initializationError及后续通过证据，见 [RUNS](evidence/UX_R1B_GUIDANCE/RUNS.json)。JDK17.0.20.1/Gradle9.1.0，offline、单worker，无新下载；本阶段实际API调用0。未重跑未改动的asset-library31项，历史PASS不重贴为本次执行。

原生截图工具仍未获得可审计画面；真实鼠标/高DPI/真人排版观感NOT TESTED，最终统一验收。来源manifest现在105文件；新增源码完全固定来源或逐项记录适配。Drive本中间检查点NOT UPLOADED，安全原始日志及小图已入Git；非Git最终包另行交付。
