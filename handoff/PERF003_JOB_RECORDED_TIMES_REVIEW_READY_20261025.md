# 作业日期Map汇总：仅准备复审

PREPARED ONLY / NOT SENT；无新外发/发布或Desktop开发权限。

- 应用[41e0111194b407f329a222b2adf4dfcb7631a93b](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/41e0111194b407f329a222b2adf4dfcb7631a93b)，最终执行c32045c0dfb7249d7c7490c8e87a873466d6a308，modules/tools一致；基线3d7369963ec942fed58b1a1963d2c5d9ccc397e4在b5e84da执行，1方法另列。
- [完整报告](../docs/audits/PERF003_JOB_RECORDED_TIMES_20261025.md)、[SOURCE_MAP](../docs/audits/evidence/PERF003_JOB_RECORDED_TIMES_20261025/SOURCE_MAP.json)、[逐例/输入/推迟](../docs/audits/evidence/PERF003_JOB_RECORDED_TIMES_20261025/runs)、[精确差异](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/3d7369963ec942fed58b1a1963d2c5d9ccc397e4...41e0111194b407f329a222b2adf4dfcb7631a93b)。RAW76429B SHA2568ead31f82da88a387371a7d40734b7cb7c08b567e186e0f40c5a351ae869d34e，26项+manifest27成员。
- 最终3唯一方法PASS（新2+既有坏收据/覆盖1），不加基线/内部轮次/历史计数。仅局部Map顺序赋值替代临时拼接/Pair/双Map，导入完整解析/16KiB/未知/0负值/首键顺序/最后输入覆盖/普通后合成及快照保持。Map接口/内容/顺序保持，不保证具体实现；没有缓存、验证削减或请求/队列/凭据改变。
- 合成任务元数据/full StudioState.validate/<=4MiB，不是实际配方/资产/账户/执行准入。线程分配约少61%～62%，2048任务配对2.919→1.071ms仅局部；UI/峰值堆/GC/RSS/长期/包/人工未测。两次资源到期各0执行保留，Desktop优先，不按旧PID退出认定空闲。
- 原16项及两段补充b5e84da保持各自固定边界，不并入本对象或重发；旧失败/用户否决/遍历不采用、路径错误前态/精确UTC UNKNOWN保留且不再访问该路径。独立NOT REQUESTED、用户/包NOT TESTED、资产/编辑USER_NOT_TESTED、115/Prompt PENDING、q09 FAILED/UNKNOWN与PRODUCT_HOLD / NOT RC保持。没有本源码的包/version/tag/安装器SHA/TUF映射，因未打包发布。

如用户后续批准此具体载荷交审，请Project独立核对固定输入/原字节/逐例、首键插入及最后输入覆盖（不是最大时间）、普通与合成结果/导入覆盖和坏收据规则、元数据测量范围及未验项；不代签整专项、日常环境或Desktop。
