# CCB Studio Suite Project Instructions

本项目开发并长期维护 CCB Studio Suite：Windows/Android原生图像创作客户端与独立Studio Server。拟议仓库CCB-Studio-Suite，本地默认H:\CCB-Studio-Suite，与H:\ChatChatBar-Desktop平级，禁止嵌套。

## 产品
Windows和Android最终都须完整Studio：AI写/改Prompt、NAI生图、多LLM/NAI账户、图库、metadata、完整Recipe对比、非破坏编辑和批量打码嵌字。Android不得永久降格为遥控器。同一客户端支持本人API直连与Server工作区；不按个人/联网版长期分叉。Server先本人多端，再邀请多用户。
Server 原始设计是**管理员在服务器集中配置上游 API 凭据，授权多终端客户端无需提供自己的 NovelAI Token，提交完整 Recipe 由服务器排队执行并回传原图/Metadata**；多人服务器可按用户管理提交额度和排队上限。BYOK 只是可选拓展，不强制替代中央服务器方案。
首个长期实际使用版本必须有自动/手动检测更新、应用内下载、可信验证、安装、安全重启和数据保全；不能仅跳网页或清档重装，各平台首次交付均适用。

## 来源权威
按领域：选定官方CCB精确源码/协议决定复用的Prompt/请求/数据行为；已审核Desktop精确源码决定共享核心和Windows适配；Suite已批准合同/源码决定独有行为；TypeSet Forge 1.1是批量打码嵌字参考，不是制卡器。旧聊天/资料只参考。最新观察、正式采用、已验证分开，schema/字段/日期/SHA不能永久硬编码。

## 读取入口
先读docs/00_INDEX.md、CURRENT_STATE.md、TASK_REGISTRY.md、SOURCE_LOCK.json，再读当前任务及相关来源。初始化可读CCB_STUDIO_SUITE_INIT_20261020.md。只定向读取，不重审所有历史。未实时取得的来源标未核验。

## 分工权限
Project负责产品/架构、来源、任务规格、真实commit/PR审查和验收。Codex执行获准本地修改、构建测试和提交。
任务写明目标、精确起点、允许修改、禁止事项、验收、测试和停止点。同一任务一个主执行者，并行任务不能改同一工作树同一文件。
立项或读到文件不自动授权后续实现、merge、release、公共仓库创建、账号/网络设置或付费API。不向官方upstream写入，不默认修改Desktop工程。

## 复用
优先权威共享，不复制第二套Prompt/请求。双向成果流动但编译依赖无环；Desktop不依赖完整Suite，通用图片组件不依赖Server/聊天容器。Android可直接参考官方平台机制，共享请求语义只维护一套。无需先整体KMP重构。

## 数据
原图、Recipe、设计记录、工程、派生物、任务、缓存、Secrets分开。原图保留字节；编辑导出不覆盖唯一原图；缓存不拥有唯一资料。持久化验证后才显示已保存。Missing/Corrupt/ReadError分开，未知/损坏bytes不默认覆盖。迁移前保全，升级/回退不清档、不丢新作品。
独立数据根，不写运行中的Desktop目录。任务固定工作区/执行端/账户/输入快照，UI切换不改已排队任务。

## Secrets与合规
API Key/Token/签名私钥/证书不得进入Git、Project、日志、metadata、普通工程或备份。Windows OS保护，Android适用安全存储；跨设备不可假定同密文可解密。
服务器管理员上游凭据只留服务器，Studio 登录身份与供应商账户分离；不把管理员 Token 下发客户端，不宣传端到端保密。不做隐式轮换或跨供应商额外付费重试。供应商服务条款/真实部署风险独立记录，不以条款为由自动改写成强制 BYOK；限速不保证供应商不可识别，也不把规避检测当作产品功能。

## Server
持久化后确认接收。幂等、有限队列、提交配额、执行预算、公平调度分别管理。未发送撤销与调度原子协调，已发送取消不保证停工/退费；不确定付费请求不可自动重排。网络准入/应用认证/资源授权分开。禁止访问他人任务、任意本机路径或任意内网URL。

## 验收与连续性
源码/测试存在、执行PASS、真人验收、集成、发布分别记。保留FAILED/SKIPPED/NOT TESTED/UNKNOWN和旧失败。每轮记录当前任务、来源SHA、实际修改、证据、缺口和下一步，仅维护相关控制文件。跨项目写CROSS_PROJECT_REGISTRY但目标独立验收。常规引继只核对当前任务和增量。
重任务默认串行低资源，不擅自结束已有Java/Codex，不删历史包或浏览器缓存。首轮A0不跑Gradle/模拟器/打包。
