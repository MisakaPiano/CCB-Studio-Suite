# 公共读取分配增量：只准备，未发送

固定源码27e562186b0980de5fa5cbb8e975d9153a1ba13d；[报告](../docs/audits/PERF001005_BOUNDED_READ_20261025.md)/[原始输入映射](../docs/audits/evidence/PERF001005_BOUNDED_READ_20261025/SOURCE_MAP.json)以本次证据提交固定。大小只作预分配提示，动态EOF/max+1边界、NOFOLLOW/错误分类、原件/文档链/备份/凭据验证保持，单次底层请求64KiB防原图大小native临时缓冲。

最终17唯一方法PASS，与旧基线1、失败15、诊断失败1、纠正测试后探针1分列，绝不累计。两次分配断言失败及现场保留；测试取数排除JSON记录，阈值未放宽，生产候选在纠正取数时未改；最终另有64KiB资源边界修正。固定旧函数同JVM交替：12MiB读取25,318,184→12,594,104B线程分配，非RSS/UI/包/用户或全平台竞态验收。原生缓存没有全局设置变化。

RAW164,910B，SHA256 `919cae1c04846362cae863e8afd0546fad8d5e37d8f263775cf672b589f80529`，50成员。新Project载荷仅准备，独立NOT REQUESTED，人工/包NOT TESTED；不发布，不操作日常数据/安装/服务或Desktop。旧失败、用户否决、q09/资产编辑用户未验、115/Prompt及PRODUCT_HOLD保持；持续既有低风险性能任务，Desktop优先。
