# 功能清单入口与覆盖范围

## 2026-10-23新增：三类清单底表已建立

- [CCB本体图片能力](inventories/CCB_IMAGE_CAPABILITIES.md)：固定官方来源的图片相关候选207个源码单元；全部384个生产Kotlin单元及未匹配项也保留在JSON，逐入口继续核实。
- [CCBdesktop全产品能力](inventories/CCBDESKTOP_CAPABILITIES.md)：固定集成来源337个单元，以及开发快照新增/变更104个单元；全快照与删除差异另列。聊天、角色、世界书、模型、图片、平台与语音等都纳入来源目录，未映射项不悄悄省略。
- [Suite新增与扩展能力](inventories/SUITE_ADDED_CAPABILITIES.md)：25项，注明实现文件、限定证据、人工待测与Android/Server未实现；与既有75项继承动作分别追踪。

[覆盖与状态说明](inventories/README.md)及[COVERAGE.json](inventories/COVERAGE.json)给出固定SHA、计数和边界。它们是完整来源入口枚举与首轮实现映射，不代表每个来源用户动作均已完成语义审计或人工签收；源码单元数不能用作功能完成百分比。以下保留清单建立前的现状核对。

2026-10-23 JST，按用户提出的三类清单核对。结论：已有需求、差距、动作实现和验证清单；**尚没有三份独立、全量且持续同步到当前版本的实现总表**。本文件是入口与完整性说明，不冒充一次新的全源码审计。

|用户要查的范围|已有记录|目前缺口|
|---|---|---|
|CCB本体图片相关全部能力|[来源锁](SOURCE_LOCK.json)、[初始复用审计](audits/A0_001_REUSE_AUDIT.md)、[差距矩阵](ux/UX_R0_PARITY_MATRIX.md)及[动作合同](ux/UX_R0_ACTION_TRACE.md)|主要通过固定Desktop图像工作室与官方特定来源核对；不能据此保证官方Android全部图片入口及版本新增能力已全量枚举|
|CCBdesktop全部能力|同上，另有模型和视觉专项审计|现有覆盖重点是图像工作室、图像工具和相关模型设置，不是整个Desktop产品的聊天、角色、语音等全功能实现清单。列出Desktop能力不等于授权把整个聊天产品移植到Suite|
|Suite新增能力|[产品需求](02_PRODUCT_REQUIREMENTS.md)、[验收矩阵](10_ACCEPTANCE_MATRIX.md)、[动作实现映射](ux/ACTION_IMPLEMENTATION.json)、[最新状态](CURRENT_STATE.md)、各专项审计|新增图库/工程/比较/导入等与继承项混列；安装更新/签名/恢复另有专项记录。尚缺统一的新增能力逐项状态表；Android/Server规划不能当成已实现|

## 哪份文件回答什么

- `UX_R0_PARITY_MATRIX.md`：2026-10-21固定起点的差距审查，原56项；是历史基线，不可当作今天缺失项的实时统计。
- `UX_R0_ACTION_TRACE.md`：把来源展开成用户动作、反馈、持久化和目标位置的继承合同；期望行为不等于已经实现。
- `ACTION_IMPLEMENTATION.json`：本次实际数得75条、75个唯一动作ID，列出实现文件、测试和证据，是现有最接近逐项实现清单的文件。覆盖的是获批工作流集合；不能外推为CCB/CCBdesktop全量功能。后续专项验收仍需回填，旧状态不能覆盖更新后的明确证据。
- `10_ACCEPTANCE_MATRIX.md`：验收标准，文件明确全部为待执行标准；不能把这一文件当最终测试成绩。
- `CURRENT_STATE.md`和`audits/`：按时间记录实际变更、自动结果和人工结果，更新较新，但分散，需要按功能归并。
- [模型接口审计](audits/UX_MODEL_ACCOUNTS.md)、[视觉配置审计](audits/UX_VISION_CREDENTIALS.md)：本轮补齐与未覆盖范围。基础模型真人通过不等于视觉或付费生成通过。
- [邀请测试版差距](rc/INVITED_TEST_READINESS.md)：发布剩余条件，不能替代全量功能目录。

## 全量实现清单还应补哪些字段

后续应按三类来源建立稳定能力ID，至少记录：固定来源版本与入口、Suite对应入口/文件、实现状态（已实现/部分/未实现/不适用）、自动证据、人工验证、实际提供者验证、最近核对提交和遗漏说明。源码存在与用户能用必须分开，不把未测记为通过。全量比较需枚举来源后再定分母，不能先用现有75项当作100%。

当前不宣称“CCB图片功能全部迁入”或“CCBdesktop全部功能已实现”。这次只核对清单现状，不擅自扩大Suite的产品范围。
