# UX-R1a 执行记录（持续更新）

状态：**IN PROGRESS / PRODUCT HOLD**。R0批准基线e85367845e82494fcdd9701009ad101b321a12d9；定向补正checkpoint25f121f16bc4ff143eae29755b6c879f62c28ab7已推送。当前只完成创作运行时基础，尚未把新实现宣称为R1a完整交付或Windows RC。

## 已实现基础

- 独立v2根允许受保护真实账户引用与权威StudioDraft/会话；v1合成根仍按原合同识别。复制迁移先验证备份，在隔离暂存完成v2提交/核验后发布新目录，失败保留源与暂存，不原地升级。
- 正常创作运行时使用固定来源的PromptDesigner、辅助消息封装、请求序列化、NAI请求/响应处理。凭据仅DPAPI引用；每个实际请求（含planner/repair）先经过持久预算；不自动轮换账号或盲重发。
- 候选与当前草稿分离，显式应用并可撤销；多轮会话/编辑分支保留历史。进程中断任务结果未知，不自动重发。收到的部分原件立即入资产库，不因批次失败丢弃。
- 异步创作任务持有工作区租约，关闭需先取消并等待任务；活动期间关闭不释放数据根所有权。未改asset-library数据核心。

## 真实运行沿革

|阶段|结果|说明|
|---|---|---|
|version-targeted|4 PASS|v2/旧根、端点、复制迁移、备份恢复和故障发布边界|
|runtime-compile|COMMAND_SUCCESS / 0 tests|仅编译，不能称执行测试通过|
|runtime-targeted|10 executed / 9 PASS / 1 FAILED|同进程重复打开抛OverlappingFileLockException，清理异常遮蔽原断言；原失败XML/日志保留|
|runtime-retest|10 PASS / 0 failed/error/skipped|已将同进程锁冲突统一为IN_USE；复测关闭保锁、取消零请求、HTTP429不泄露响应、不重发、流式不完整拒绝、设计分支、部分生成保留与重启|
|foundation-regression|56 PASS / 0 failed/error/skipped；438.417s|新执行asset-library31 + studio-core25；image-authority UP-TO-DATE，未重新执行，不计入此次PASS；源码稳定|

测试使用本机loopback HTTP与合成输入/凭据，实际调用真实付费API为0。所有执行均通过Run-PreviewChecks.py保留source-before/test-cases/summary/原始日志；源码稳定性与实际test数记录在每轮summary。前置一次argparse失败未启动Gradle，属命令解析错误，不计测试。读源码时PowerShell花括号/rg参数错误未修改参考源码。

## 环境与差异

Windows；JDK17.0.20.1、Gradle9.1.0本机缓存；离线strict依赖验证、单worker/单测试fork。当前没有新依赖下载。精确源码身份随运行证据记录，未把旧21/31 PASS冒称为此次执行。异步租约新增于Suite工作区；资产库的R1备份排水和未知文件保全实现保持原样。

## 尚未通过

GUI账户/设计/生成/入库/重开完整串联仍在实现，真实provider NOT TESTED；窗口捕获/鼠标行为尚未取证，真人NOT TESTED。R1b、R2a/b/c、R3、R4未完成；TypeSet源视觉仍SOURCE_VISUAL_NOT_TESTED，完整安全A→B应用更新未完成。继续连续授权内的GUI接入与全部工程门槛，不进入提前验收。

证据：[保留各轮日志、XML、源码hash和用例](evidence/UX_R1A_FOUNDATION)。这是可恢复的运行时基础checkpoint，不是R1a全流程通过；继续GUI接入。Drive无非Git大附件需上传，本checkpoint以Git证据为准，未声称Drive上传。
