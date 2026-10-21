# UX-R2 TypeSet 工程与预设交换

接续已推送 `6453a1bcd7c71298531d74e5921ce770e7bd8273`。同一Suite执行者/功能分支；PRODUCT_HOLD，继续完整导出、图库与更新，不进入集中验收。

## 实际工作流

编辑工作台预读TypeSet工程文件，显示名称/页数/大小及兼容性说明；取消不写工作区，确认创建独立工程，不覆盖原工程。每张原图按原始字节进入既有AssetStore；原交换文件单独保全、隐藏于普通图片列表、与工程关联，支持另存原文件。源metadata、未知字段和源历史保存在原文件中，不当作Studio撤销命令执行。

可导出当前可编辑TypeSet工程，继承原文件未知顶层/条目字段、源metadata和历史，更新实际文字/遮罩/旁白/原图。Studio额外的对象ID/锁/显隐/混合层序写入版本化扩展，Studio直接再导入可恢复；外部TypeSet按其原有遮罩在下/文字在上规则，不保证混合层序相同。外部TypeSet重新导出会按其自身serializer丢弃未知扩展，不能宣称跨外部编辑完整保留Studio扩展。界面明示此边界，原始导入文件仍可取回。

预设导入显示数量并要求合并或替换：合并追加文字预设、覆盖同槽打码值并导入文字/打码默认；替换移除旧预设列表。已有页面不变。适配源旧userMaskPreset迁移，source ID映射到独立native ID；全局原件与未知设置保留。未接入的源侧栏/故事书/导出全局设置目前仅保留原值，不视为已应用；这些真实消费者继续实施。

交换文件写入唯一`.incomplete`临时文件、force后同目录rename，明确不使用REPLACE_EXISTING；已有目标或并发新建目标保持原样。原文件版本未知/Studio扩展版本未知/损坏UTF-8/远程URL拒绝导入；不执行HTML、脚本、远程字体或历史命令。工程输入64MiB/1000页，预设4MiB/200个，超限明确失败，不截断。当前图像解码限制仍为PNG/JPEG；其他格式继续接入。字体文件不自动对外再分发，缺字体导出门禁保持。

## 独立来源证据

固定HTML SHA-256 `892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63`，只在隔离测试副本移动DEFAULT_PREFS到STORYBOOK_PRESETS之后，修复此前已保留的源启动TDZ；导入/导出算法未改。新建Edge131无头上下文，拦截全部外部请求，网络字体请求被阻断，无用户浏览器/本地图库。

实际源导出工作区2911bytes，SHA-256 `45a6215884ede5f7193f88fc2a219f98aae09461b60d44546ad575b7fbf9958c`；含竖排富文本、矩形/画笔/圈选遮罩及旁白。源导出预设1464bytes，SHA-256 `60c6a6248dde41c8d870190861cd6a042373cf4ad6f8e6ef6a161f6de77894bf`。均为新绘制合成图与测试文字，非参考HTML再分发。

Suite编辑后导出由源程序实际导入并重新导出，测试脚本逐项比较文字数组、遮罩数组和旁白，PASS。原生视觉未测试；真实Compose截图为离屏，系统文件选择器通过测试注入路径，不能冒充原生文件对话框验收。

## 验证范围

TypesetSourceExchangeTest覆盖真实源样本、旧预设迁移、合并/替换、未知字段、Studio扩展、原字节、备份恢复、未来版本/远程数据拒绝，以及发布前故障/已有与并发目标不覆盖。TypesetWorkflowTest实际走导入预读→取消→确认→导出、预设合并→导出→重开。

Gradle9.1/Temurin17.0.20.1、离线严格依赖校验、单worker/单fork；无新Gradle依赖，真实API0，Desktop/upstream/asset-library未改。完整结果随运行登记，旧失败证据保留。

最终完整回归 `20261021T080539378949Z-UX-R2-exchange-full-regression`：107项实际PASS（core62/desktop45），0FAILED/0SKIPPED，sourceStable=true，155.198秒。之后仅补充预设兼容性界面文字，相关GUI再运行1项PASS；核心行为未变。6次Gradle运行均无失败（首次仅编译，NOT TESTED），原检查点失败全部保留。

[来源与实际运行](evidence/UX_R2_SOURCE_EXCHANGE/) · [大小/hash身份](evidence/UX_R2_SOURCE_EXCHANGE/PACKAGE_FILES.json)。最终来源回读含冻结Suite输入，按数组逐项比较PASS。公开交付仅合成数据/截图/源码/日志；本检查点无必须非Git附件，Drive NOT UPLOADED。
