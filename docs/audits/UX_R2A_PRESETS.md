# UX-R2a 预设与默认样式工程检查点

接续已推送 `afe3e8105f2cb3e661466d33dd8c1ffc66f52211`，同一功能分支/执行者。**ENGINEERING_PARTIAL / PRODUCT_HOLD**，继续交换、导出、图库及更新，不进入真人验收。

## 实际实现

- 文字预设跨工程存储，只保存样式，不存本页文字/位置/锁定状态。支持新建、覆盖确认、应用、删除确认及记住新建文字默认；应用保留目标内容/位置/ID/显隐/锁定。
- 打码四个槽精确采用TypeSet内置参数：轻/重马赛克与细/粗条纹。覆盖/恢复内置均有明确确认；恢复槽不改已有遮罩。条纹槽保留当前白色，否则用黑色。
- 启动默认可选择槽或已保存自定义参数。已有页面优先自己的工具参数；用户可显式保存当前自定义参数，界面说明优先关系，避免“当前控件”和“下次默认”混淆。
- 新建文字遵循TypeSet defaultText：按图宽四舍五入基础字号/描边；记忆框宽上限95%图宽、字号上限25%图宽、下限6，清空正文并重新定位。已保存页面不随默认值变动。
- 全局预设进入现有StudioState/journal和备份恢复；没有新数据根、账号或秘密导出。

## 实际验证

TypesetPreferencesTest逐项核对来源四槽、样式内容分离、目标内容/位置/锁保留、覆盖ID、删除、默认尺寸限制、完整备份恢复。实际Compose测试走保存文字预设→改字号→应用恢复样式；覆盖打码槽→恢复内置→已有遮罩保持黑色→设置启动优先→重开保留。

早期完整回归95项实际PASS，随后新增源默认尺寸回归及界面明确默认按钮，定向6项实际PASS；最终完整运行结果随后登记，不把旧95项代签新版。

Temurin17.0.20.1 / Gradle9.1，离线严格依赖hash、单worker/单fork；真实API0、真实图库0、没有新Gradle依赖。补齐Microsoft字体测试fixture上游Fonts/LICENSE.txt的SIL OFL1.1完整声明（blob `3a2e691c5915b6eb87d879fe2f0994b5d3996f31`），与已有MIT代码声明并存；未修改打包字体字节、内部名称或复制Windows字体。该补充纠正旧回执只描述MIT代码声明的不完整性，旧证据保留。

## 未完成范围

预设交换文件、源队列交换、缺字体/缺字导出门禁、完整四类导出及隐私/剪贴板、整册PDF/ZIP、图库全流程与真实A→B应用内更新继续实施。原生窗口/真人排版质量NOT TESTED。无新RC包、无合并/发布；本检查点非Git附件未形成，Drive NOT UPLOADED。

最终完整回归 `20261021T073248273760Z-UX-R2a-preset-final-regression`：**96项新执行PASS（core53、desktop43），0 FAILED / 0 SKIPPED，sourceStable=true，108.347秒**。[五次原始运行](evidence/UX_R2A_PRESETS/runs/)与[文件身份清单](evidence/UX_R2A_PRESETS/PACKAGE_FILES.json)保留；本检查点五次运行没有失败，之前检查点的失败不删除。未改asset-library或authority，不将其UP-TO-DATE编译列为新测试。
