# UX-R3 选择历史字段应用

基线27eb5d9112f83b0e4eed2fa60202e9a6142f705b，继续同一批准分支；产品PRODUCT_HOLD。

比较页A/B分别进入选择页。支持画风/基础/附加/负向、角色与位置组、模型、尺寸组、数量、Steps、Guidance、Rescale、采样器、实际图片Seed、角色位置开关和图像引导。未选字段保留，明确展示全部实际草稿差异及权威转换关联变化，确认后保存，可取消、撤销/重做。未知PNG字段保持只读；角色和尺寸按关联组处理，未宣称任意JSON路径可写。

适配器使用固定权威applyHistoryRecipe/withActiveSettings/validationError，不改Prompt、请求或恢复语义。缺失历史引导阻止选择该组，但不妨碍仅复用Prompt。选择Seed使用具体结果序号对应Seed。模型切换保留当前未选设置，旧模型设置留存。确认时重读并验证请求证据，草稿与预览不一致则拒绝，不覆盖并发编辑。

CreationState增加默认空recipeApplications，最多100条历史动作记录：源资产UUID/SHA-256、字段列表、应用前后草稿SHA-256和时间；不复制Prompt或凭据。撤销保留历史记录，不把该记录标为当前草稿。现有无该字段的记录可读取；旧版严格读取器不能读取新版增加字段，本次不承诺降级兼容，后续R4须按完整状态版本/备份合同处理。已验证资产存储和原图无更改。

## 实际验证

三次运行均保存：首次核心3项PASS但桌面编译因缺失UI导入FAILED；修正后GUI1项PASS（105.373秒）；增加缺失引导用例后完整core84+desktop52共136项PASS（186.002秒），0失败/错误/跳过，运行源码稳定。没有把第一次整体FAILED覆盖为PASS。

GUI实际执行B来源选择、取消不变、预览/确认、原件和Recipe字节不变、撤销/重做、预览后草稿变化拒绝覆盖、同根关闭重开及来源记录恢复。截图已查看，差异和确认操作可读。Compose离屏验证，原生Windows/DPI/IME/真人视觉NOT TESTED；没有启动或关闭用户窗口。

JDK17.0.20.1、Gradle9.1，严格依赖校验/离线/单worker串行；无新依赖或下载、真实API、用户数据、账号或Secrets操作。当前恢复阶段0次真实API。没有完整应用内A→B安装更新或RC，不提升W1–W5与产品验收。

[全部运行和失败](evidence/UX_R3_RECIPE_SELECTION/runs/) · [实际界面](evidence/UX_R3_RECIPE_SELECTION/selected-recipe-fields.png) · [逐文件大小与SHA-256](evidence/UX_R3_RECIPE_SELECTION/PACKAGE_FILES.json)。全部合成证据入Git，无独立非Git附件；Drive NOT UPLOADED。继续I01文件/拖入/剪贴板导入和图库操作，随后剩余恢复及R4。
