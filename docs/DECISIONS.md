# DECISIONS — 初始约束与待设计事项

以下是立项对话中的明确目标和采用的初始化方案；不把文件生成当成对所有实现细节的授权。

| ID | 事项 | 状态/含义 |
|---|---|---|
| S-001 | 独立Suite，名称CCB-Studio-Suite | INIT BASELINE；独立Project/本地目录/拟议仓库 |
| S-002 | Windows/Android完整Studio＋独立Server | USER REQUIREMENT；不永久缩水Android |
| S-003 | 同客户端直连/服务器工作区 | INIT DESIGN；不按模式长期分叉 |
| S-004 | AI写Prompt、多LLM/NAI档案与真实身份 | USER REQUIREMENT；不做隐式轮换 |
| S-005 | 原图/工程/Recipe保全 | USER REQUIREMENT；缓存非唯一权威 |
| S-006 | 正确编辑参考TypeSet Forge 1.1 | USER CLARIFICATION；不是制卡器 |
| S-007 | 首个实际使用版完整安全更新 | USER REQUIREMENT；不能仅网页跳转 |
| S-008 | 分域权威与无环双向反哺 | INIT DESIGN；目标项目独立验收 |
| S-009 | Kotlin/JVM与原生平台优先 | INITIAL DIRECTION；不整体强制KMP |
| S-010 | 不强制Private，不自动公开远程 | INIT ORGANIZATION；可见性与授权分开 |
| S-011 | A0只读审计＋新项目控制文件 | READY；不自动批准生产实现 |
| S-012 | Server 管理员集中上游 API 网关 | USER CLARIFICATION / ORIGINAL DESIGN RESTORED；客户端无须自带 NAI Token，多人共享服务器队列；BYOK 仅可选，不将供应商条款当成擅自变更设计的依据 |

## S-012 的执行边界

服务器集中配置上游凭据、客户端提供 Studio 登录身份和完整 Recipe、服务端统一排队及安全回传属于**明确产品要求**。没有取得用户新的产品决策时，A0 或后续开发不得自动替换成「每用户自带上游 Token 才能接入」的方案。供应商实际使用限制和公开部署风险另行核对；不宣称不可检测或无封号风险，不将检测规避设计列入功能。

## A0需决定而非让用户重复描述的问题

| 事项 | 应产出 |
|---|---|
| 共享抽取/直接依赖 | 最小依赖图、精确路径、Desktop是否需单独任务 |
| 图库索引和持久化 | 规模、恢复、迁移比较及推荐，不先定schema |
| 更新器 | 可信链、平台安装、授权、维护成本与推荐 |
| Android最低版本 | 与真实源码/目标设备匹配的依据 |
| 应用ID与渠道 | 冲突检查和一套候选值，未发布前冻结 |
| 字体/图像渲染 | 授权、包体、跨端差异 |
| Studio工程格式 | 保全/可读性/版本兼容合同 |
| 公开复用授权 | 核对原记录，缺口只阻塞相应公开采用 |

上述技术待决不阻塞文档初始化和已有来源的审计；不能用猜测填空，也不能擅自把候选schema写成用户批准。
