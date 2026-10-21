# UX-R0 — Project整体审阅入口

**交付性质：产品恢复方案与GUI结构合同，待Project批准；PRODUCT HOLD / NOT ACCEPTED。** 无生产代码修复，无新安装包，无新产品测试PASS。分支 `feature/windows-rc-continuous`；本文件所在固定Git commit即UX-R0文档交付身份，产品实现仍为 `209b5252e06ea55b6cdf970da131b330a407e882`。

## 建议审阅顺序

1. [整体工作流与GUI结构合同](UX_R0_WORKFLOW_AND_LAYOUT.md)：五条端到端故事；创作、设计、图库/比较、图片编辑/小人书、导出的宽窄结构及控件归属。
2. [56项来源/现状差距矩阵](UX_R0_PARITY_MATRIX.md)与[75项用户动作继承合同](UX_R0_ACTION_TRACE.md)：固定源码→用户动作→可见反馈→保存/重开→Suite目标位置；完整TypeSet，包括富文本、小人书、整册PDF。
3. [整体实施方案](UX_RECOVERY_IMPLEMENTATION_PLAN.md)：保留/改造边界，UX-R1a/b、R2a/b/c、R3、R4工程检查点，真实测试和集中人工验收合同。
4. [来源身份](../audits/evidence/UX_R0/SOURCE_EVIDENCE.json)、[执行与保全检查](../audits/evidence/UX_R0/EXECUTION.json)、[本轮边界](../../tasks/UX_R0_PRODUCT_RECOVERY_AUDIT.md)与[Issue最新完整范围快照](../../tasks/UX_R0_ISSUE4_FULL_ADOPTION_ADDENDUM.md)。

## 产品恢复决定提案

- **两个独立核心工作台＋桥梁：** 真实NAI Studio（含完整设计对话）与完整图片队列/排版编辑器；图库、metadata/比较、批量导出连接它们。优先适配Desktop成熟Compose组件/状态流，TypeSet原生等位实现，不用WebView或合成模拟替代。
- **完整继承，不默认删功能：** 执行中Issue #4从02:39:52Z初稿更新为02:49:22Z的FULL SOURCE ADOPTION，已复读正文及0评论。最新范围覆盖旧“候选/延期”措辞；小人书/PDF完整纳入R2c。任何Release缩减必须用户明确批准。
- **保留已验证的数据基础：** A1/R1不可变原件、事务/恢复/单写排水与权威Prompt/request核心不重写；上层状态/真实接线/排版需要改造，旧schema向真实版迁移必须独立验证。
- **以可见真实闭环交付：** 每个恢复检查点从正常GUI输入、反馈、保存到重开；自动化/loopback/live传输/真人体验四类证据分开。新图不能再被工程筛选遮蔽；“原字节/无编辑重编码/隐私PNG/APNG”独立。
- **CP4完整门槛保持：** 真实应用内A→B安装更新与数据/测试凭据保全必须完成；信任lab、文件故障测试、窗口命令不能代替。

Project可一次审查这套范围/结构/实施次序，给出批准或集中修改意见；本轮不自动执行后续阶段。若批准，下一任务先核验最新固定checkpoint与所有权，再连续实现，不逐Slice要求用户签字。没有产品接受或main合并提案。

## 视觉证据及边界

以下是用户实际操作P3后提交、已保全的截图；不是本轮生成的效果图。来源/size/hash见 [USER_EVIDENCE.json](../audits/evidence/WINDOWS_PREVIEW_USER_REVIEW_1/USER_EVIDENCE.json)。实现归属基于用户会话与原交付记录，截图本身不是运行二进制hash证明。

|证据|可证明的观察|不能推断|
|---|---|---|
|[生成页](../audits/evidence/WINDOWS_PREVIEW_USER_REVIEW_1/user-review-1.png)|合成模式、窄列表/右长面板、工程与选择混合|真实provider路径或效果质量通过|
|[编辑页](../audits/evidence/WINDOWS_PREVIEW_USER_REVIEW_1/user-review-2.png)|文字输入/竖排控件与画面不同步；应用文字按钮在下方|完整TypeSet排版等效|
|[记录页](../audits/evidence/WINDOWS_PREVIEW_USER_REVIEW_1/user-review-3.png)|图库计数与当前工程列表、Recipe/metadata弹框入口|新增结果可见、完整双图比较可用|

Desktop实际P7用户操作证据读自固定5ef内86/89/91及79；保留有限范围/未测项。58截图索引明示含Android/旧Desktop/第三次反馈，不能当成P7最终实现截图。本轮未新启动Desktop或Suite取屏，**新运行P3截图验证为UNVERIFIED**；TypeSet仅静态审查，未运行原HTML，**TypeSet运行视觉为NOT TESTED**。GUI合同中的文字框图/Mermaid均为提案，不能标“已对齐”。

后续真实取屏路径：先核验[既有P3包身份与Drive回执](../audits/evidence/WINDOWS_PREVIEW/DELIVERY.json)，或批准后新包的manifest；使用新建隔离合成工作区，从正常启动入口逐条走W1–W5，截图记录source SHA/package SHA/窗口尺寸/DPI/步骤。若自动取屏受阻，保持UNVERIFIED并在集中真人验收由用户确认，不借旧P2 `user-first-launch.png` 冒充新版本，不擅自关闭用户窗口。

## 本轮交付与验证边界

实际工作：读取Issue两版/评论、固定Desktop对象、TypeSet三文件、Suite相关源码、用户反馈与历史验收；形成合同与来源证据。仅文档与控制记录变化；生产/测试/构建文件及历史证据校验见EXECUTION。本轮未执行Gradle/JDK构建、产品测试、GUI自动化、真实API、签名或安装更新；所以没有新增运行环境/依赖下载/用例PASS可报告。

所有本轮交付均为小型Git文档/来源摘要；**没有新的Drive上传**，也没有未交付的大型原始运行日志。TypeSet原件及本地提取的参考源码保持ignored，不公开再分发；旧P3 Drive包继续只是被拒收的历史交付，不是本轮修复包。精确文件清单与远程回执见最终固定提交及EXECUTION；不创建release、不改变共享权限。

**下一步：Project审查UX-R0整体合同，批准后再交Codex连续实现。当前停止等待，不后台续跑。**
