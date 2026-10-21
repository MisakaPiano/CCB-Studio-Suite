# UX-R1b 创作、反推、后处理与回用检查点

2026-10-21；继承已推送74c30d85。产品仍PRODUCT_HOLD，未完成R2–R4，不是RC。本阶段真实API调用0；所有请求测试仅loopback，未使用真实凭据/用户图库。

## 实际实现

- Enhance/Upscale采用固定Desktop原有参数、成本、限制及比较面板；Suite安全凭据/逐请求预算与任务门禁接入。请求身份在发送前保存，原始返回先入库再验证尺寸，验证失败不会丢弃唯一返回文件。
- 图片反推使用原有ImageUnderstandingService/EffectiveModelResolver与权威designer：直接多模态或关联视觉模型，缺配置在请求前失败。候选独立持久化，明确应用；新反推清除旧活动候选，避免错误应用上一次结果。关联模型快照只含无凭据配置，DPAPI仅在内存解密。
- 元数据选择正负词、角色关/覆盖/新增、参数、Seed、引导；先看现值与候选，确认更新可撤销，原件不重编码。嵌入引导原bytes保留，再建立归一化派生引用。原解析优先级不变；诊断分READABLE/MISSING/CORRUPT/UNSUPPORTED/READ_ERROR。未知原字段仍在原件。
- 历史完整复现／新种子／仅Seed采用来源规则，核验请求证据hash后才复用；缺原引导时完整复现禁用，其他复用需确认警告。历史原件/Recipe不覆盖。
- CCB包JSON/PNG及原生卡JSON可选择图像字段，持久化画风、负向、角色图像参考及来源hash，不保存聊天/系统Prompt或附件。跟随角色卡／全局模型优先级采用来源函数。额外设计要求、V5自然语言设计、Tag注释偏好、Token计数、结构化粘贴预览/清空接入。
- 用来源缩放、平移、双击及动画读取器替换临时静态缩略读取器；全图窗口支持前后、键盘、复制和原字节另存。辅助窗口与主界面明确内容前景色，修复渲染检查中发现的暗背景黑文字。

## 实际验证和失败保全

[全部原始运行回执](evidence/UX_R1B_CREATION/RUNS.json)，各次保留log、JUnit XML、输入hash与test-cases。最后desktop完整 **39 PASS / 0 FAILED / 0 SKIPPED**，分发目录构建成功；authority99/core30在前次完整运行中实际PASS，该次整体仍FAILED（desktop Q09定位失败），未改记为整体成功。随后仅影响desktop，针对4项与完整39项复测通过。未改动的asset-library历史31项没有重贴为本次执行。

后处理3项、反推3项、metadata4项、卡片2项新增执行；metadata实际Compose界面确认→撤销/重做→相同根重开已验证。source缩放真实pointer事件、Q09画布操作/保全重开复测。后处理/反推目前controller+真实本地HTTP与持久化验证，未签原生鼠标窗口/真实提供者效果。源组件存在不等于产品等效。

保留失败：后处理测试缺必填参数；Metadata switch回调/测试辅助函数编译错误；fixture缺sampler/action造成3项失败；Viewer函数片段提取换行差异导致接口未切换；新增源测试残留无关import；Q09旧图像语义标签失效，改为明确画布语义后重跑相同拖绘断言。未删除关键用例或放宽像素/数据断言。

渲染图为实际Compose场景，见[截图目录](evidence/UX_R1B_CREATION/compose-scenes)；它们不是Windows原生截屏或真人视觉认可。打包清单见[包身份](evidence/UX_R1B_CREATION/PACKAGE_FILES.json)，尚未做此包原生手测、安装或更新。

JDK Temurin17.0.20.1，Gradle9.1.0，离线严格校验，单worker/单测试fork/256MiB测试heap。本阶段Gradle无下载；新增111份权威源闭包（上次105），固定5ef9542，新增CCB角色卡包解析消费者；没有整包sharedCore或Desktop构建。测试样本仅合成。

## 后续门槛

R1b功能已接入但完整原生交互/视觉与提供者效果仍NOT TESTED；R3继续同一图库完善相册、日期检索、并排完整Recipe比较、拖入/剪贴板入口。R2先取得TypeSet源样本，再完成全部富文本/打码批次/小人书PDF；最终W1–W5与真实A→B安全应用内更新齐备后集中验收。4MiB应用状态文档仍有长历史容量上限，超限安全拒绝而不清历史；R2工程容量演进必须覆盖恢复/备份，不可默删历史或放宽未知格式检查。

Git交付包含本阶段安全源码、日志、hash、截图。Drive本中间检查点NOT_UPLOADED：尚无新的非Git安装包交付，最终大附件按原授权同步。TypeSet原始HTML只读且ignored；后续源样本获取使用隔离浏览器，本报告不冒称已完成来源排版对照。
