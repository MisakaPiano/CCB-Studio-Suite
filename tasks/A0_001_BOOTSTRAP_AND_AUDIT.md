# A0-001 — 初始化核对与只读复用审计

状态：AUDIT DELIVERED / AWAITING PROJECT REVIEW（2026-10-21 JST）。目标、授权和停止点原合同保留；[实际源码审计](../docs/audits/A0_001_REUSE_AUDIT.md)、[差距/变更/验证](../docs/audits/A0_001_DEPENDENCY_AND_GAPS.md)、[实施候选](A1_001_IMPLEMENTATION_CANDIDATE.md)。本轮用户另行允许配置缺失origin，已执行；不commit/push。模型/Fast实际UI配置未取得，不推测。以下为原任务合同。

## 目标和边界

新工作目录 `H:\CCB-Studio-Suite`，不是Desktop续开发。先读AGENTS、索引、CURRENT、任务登记、来源锁及立项母本。

允许：核对本包；在新目录内必要修订控制文件、生成审计/候选任务；只读相关公开/本地源码；核算参考哈希。确认是新独立目录且没有Git时，可`git init -b main`。

禁止：生产Kotlin/Gradle实现、大量空模块、commit/push/merge/创建远程/修改可见性/全局Git身份；修改或切换Desktop工程；真实用户数据/浏览器缓存/SecretStore读取；真实付费API；签名密钥、开放端口、Tailscale/路由器设置；reset/clean/stash/终止进程/删除历史包。

## 1. 安全前置

核对实际目录、是否嵌套在其他Git仓库。目录有非本任务文件就列明并保留，不能覆盖。若已存在源码/不同AGENTS，说明差异和最小措施；仅阻塞不安全写入，能独立进行的只读工作继续。

报告新目录和只读Desktop参考的实际branch/HEAD/worktree。未知就写未核验，不凭远程SHA推测本地clean。必要时运行包内只读校验器；原始交付哈希与工作区之后的合法修改分开。

## 2. 检查现成控制文件

核对README、AGENTS、gitignore、docs/00—10、SOURCE_LOCK、CURRENT_STATE、TASK_REGISTRY、DECISIONS、CROSS_PROJECT_REGISTRY及handoff/instructions。不要重新生成同内容，不覆盖额外编辑。只修缺失、断链、解析错误和确有来源的事实差异；规格变化提议而非擅自批准。

## 3. 精确来源

- 集成Desktop：`5ef95428421f7c693b010ac88f51d470804ab07a`。
- Desktop正式upstream基线：`5e76a9cb841736bbbf3499a2e35e5789af4c5ca8` / 1.4.1。
- 立项观察官方master：`979cb505958dffb1b6d64a8de657434f198ffa96`。
- 立项观察活动feature：`c4098e08bec84372911a5a8bc38c2cfa72f98978`。

观察不是依赖批准；变化另记，不覆盖旧观察，不盲吸收。D-044/146是未实现多账户需求。未集成分支不整体引入。

优先精确对象只读读取；不要fetch/checkout到活跃Desktop工作树。独立参考副本放Suite的`.reference-repos`，记录体积与来源；本轮不做清理。不要默认分支下载冒充desktop。

## 4. TypeSet Forge

核对`.reference-assets/typeset-forge-1.1/`三文件与references/REFERENCE_ASSET_MANIFEST.json。原始README在包内规范命名README_1.1.md，字节未改。静态读取，不运行HTML，不加载外部字体/脚本，不读浏览器数据。

审多图队列、嵌字、打码、撤销/重做、metadata、分类导出、小人书/PDF。区分“原始排序重新编码导出”和真正原始字节保存。缺文件只阻塞对应证据，继续其他审计。

## 5. 定向审计

按Desktop Source Map首读和官方image Skill，从Studio draft/recipe/repository/history、Prompt Designer、模型解析/LLM transport、NAI请求/流/批次/取消/保存、metadata/引导/资源、Windows凭据/文件/生命周期、Android对应平台和图像UI、编辑边界与更新机制展开。不遍历全部聊天/Memory/RAG/Fish领域。

必须回答：
A. 可直接依赖、需窄抽取、仅参考UX和必须新实现分别有哪些？
B. 独立Studio如何摆脱聊天/角色卡容器，哪些依赖阻碍？
C. Windows/Android/Server共享部分是否真正中立？
D. 多账户怎样保留Prompt/fallback/任务归属且不泄密？须**分别**审查客户端本人直连账户与服务器管理员集中上游凭据；客户端使用 Studio 身份、无需自带 NAI Token，BYOK 仅是可选拓展，不能擅自重写为唯一方案。
E. 原图/工程/索引怎样保全与恢复？
F. 首版完整更新最低充分方案、可信链和平台差异？
G. 反哺Desktop的精确模块怎样避免循环依赖？
H. 授权/第三方素材依赖的具体缺口？
I. 未集成修复是否必要，必要则提精确候选，不整分支吸收？

## 6. 交付

创建：
- `docs/audits/A0_001_REUSE_AUDIT.md`
- `docs/audits/A0_001_DEPENDENCY_AND_GAPS.md`
- `tasks/A1_001_IMPLEMENTATION_CANDIDATE.md`

报告给精确repo/commit/path/symbol、采用方式、差异、风险与测试入口；不是泛泛“以后研究”。未读源码标缺口，不猜。首项实施候选写目标、允许路径、禁止变化、验收、测试、停止点。仅候选，不自动执行。

## 7. 验证

A0不运行Gradle/模拟器/打包。检查相对链接、JSON合法性、来源身份、未知标记、Git忽略、无秘密/真实资料、未修改外部工程。适用时`git diff --check`，不冒充产品编译或测试。

若检查到基础源文件变化，更新来源观察/影响说明，不无差别升级版本。不得修改官方Prompt或正式schema。

## 8. 停止点

控制文件确认、A0报告与实施候选完成即停；不A1、不merge、不release。回执：实际目录、Git状态、改动/新增文件、来源核验、运行验证、未覆盖、真正阻塞和下一项生产候选。更新CURRENT/TASK仅记事实。
