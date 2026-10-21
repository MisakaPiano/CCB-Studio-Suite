# UX-R3 完整 PNG 只读字段

基于5a0037ae16f49b187cf60c6426dd4a1f1c79526f继续，PRODUCT_HOLD。

原图的tEXt/zTXt/iTXt及Alpha隐藏信息进入只读结构字段，重复文本块按顺序保留，普通/Alpha冲突分别展示，未知JSON字段可搜索、展开完整值、复制单字段或完整投影。解压总量8MiB、JSON深度/字段数受限。原始元数据块继续沿用既有CRC核验；不改变Desktop参数优先级、不从只读字段构造新请求。未知压缩方式/坏JSON逐项标明，部分可读为PARTIAL；缺失、非PNG、CRC损坏及解压超限分别报告。Alpha读取失败不丢已读文本。原字节始终保持原样。

单图“参数与来源→查看完整PNG字段”与两图比较均有消费者。比较使用独立/pngMetadata路径，Studio Recipe缺失时仍可比较原图PNG字段。返回参数选择保留此前勾选，确认应用继续调用既有权威语义；当前仍不是任意Recipe字段应用功能。

## 执行

七次运行均保留：首轮测试辅助函数编译失败（0测试）、第二轮3项执行中1失败（未知格式异常越界），后续修复；3项核心读取回归PASS、5项GUI相关回归PASS、4项选择保留回归PASS、core80+desktop52完整132项PASS（169.958秒），最后Surface颜色修正后4项定向PASS（22.971秒）。均无跳过，源码运行期间稳定；不把最终4项写成再次完整132项运行。

原先单独挂载字段页未继承正确文字颜色，功能测试绿但截图不可读；问题截图原样保存，补页面Surface后重新查看截图文字正常。GUI测试覆盖未知字段搜索/复制、返回勾选保留、权威确认应用/撤销/重做/重开、两图未知字段复制及原件字节不变。剪贴板消费者隔离，不改用户系统剪贴板。

JDK17.0.20.1/Gradle9.1，离线严格依赖校验、串行单worker/单fork；无新增依赖/下载/真实API/真实图库/凭据。截图为Compose离屏，原生Windows/DPI/IME及真人验收NOT TESTED。R3逐字段草稿应用、图库交互及完整安全应用内更新仍待完成，不是RC。

[原始运行及失败](evidence/UX_R3_PNG_FIELDS/runs/) · [字段页](evidence/UX_R3_PNG_FIELDS/unknown-field.png) · [两图比较](evidence/UX_R3_PNG_FIELDS/png-metadata-comparison.png) · [问题截图](evidence/UX_R3_PNG_FIELDS/before-surface.png) · [大小/SHA清单](evidence/UX_R3_PNG_FIELDS/PACKAGE_FILES.json)。全部合成证据入Git，Drive NOT UPLOADED，无独立非Git附件。
