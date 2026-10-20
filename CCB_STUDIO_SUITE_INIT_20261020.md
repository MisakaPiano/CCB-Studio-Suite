# CCB Studio Suite — 立项母本

**INIT-1.0｜2026-10-20｜服务器原始设计补正 PATCH-001；分文件整理合并版；不是聊天逐字记录。**

本母本用于新Project一次性上传与A0输入。分文件已在ZIP中，不需再从聊天拆出。原始参考HTML等见包内忽略目录，不嵌入母本。打包不等于建仓、开发、验收或发布。

## 使用规则

本文件内的每个章节都有 `SOURCE_FILE`。相对链接已转换为从包根目录出发；后续实时状态以分文件为准，母本作为初始快照保存。Git锚点是之前会话观察，本次未重查；参考文件哈希则为本次实际计算。


---

## SOURCE_FILE: `START_HERE_CN.md`

# CCB Studio Suite — 文件包使用入口

**版本：INIT-1.0 · 打包整理版｜资料日期：2026-10-20｜性质：立项文件，不是软件发行包。**

## 你只需做这些

1. 将 ZIP 解压到一个新目录，最终路径建议为 `H:\CCB-Studio-Suite`。压缩包已经包含顶层 `CCB-Studio-Suite` 文件夹；解压到 `H:\` 即得到该路径。不要覆盖已有同名工程，也不要解压进 `H:\ChatChatBar-Desktop` 内。
2. 新建 ChatGPT Project **CCB Studio Suite**。将 [Project Instructions](instructions/PROJECT_INSTRUCTIONS.md) 的内容复制到项目指令；上传 [立项母本](CCB_STUDIO_SUITE_INIT_20261020.md)。其余分文件按需读取，不要求一次上传全部。
3. 在 Codex 中打开 `H:\CCB-Studio-Suite`，发送 [Codex 开场消息](handoff/CODEX_START_MESSAGE.md)。完整 [A0-001 任务书](tasks/A0_001_BOOTSTRAP_AND_AUDIT.md) 已在包内。
4. 在新 Project 发 [接手消息](handoff/NEW_PROJECT_OPENING.md)。同一 A0 任务只交给一个执行者，不并行重复建项。

**文档和参考材料已经拆分好，无需再次从聊天复制或让 Codex 重建同一批文档。** A0 从“检查现成资料＋只读复用审计”开始；不是开始写生产功能。

## 已包含什么

- 产品章程、Windows/Android/Server 模式、首版范围；
- 数据保全、完整应用内更新、服务端队列与授权合同；
- 来源观察记录、Git 协作、路线图、验收矩阵、当前状态与任务登记；
- Project Instructions、AGENTS.md、Codex 首轮任务书、跨 Project 交接说明；
- TypeSet Forge 1.1 原始 HTML、说明书和更新说明（只读参考）；
- SHA-256 文件清单和离线校验工具。

TypeSet Forge 文件位于 `.reference-assets/typeset-forge-1.1/`，已被 `.gitignore` 排除。HTML 未被执行，未下载字体或外部脚本。参考文件不因进入本私人交付包就自动取得公开再分发授权。

## 可选的离线完整性检查

在包根目录打开 PowerShell：

```powershell
powershell.exe -NoProfile -File .\tools\Verify-Package.ps1
```

如果系统策略不允许运行脚本，不要修改全局执行策略；可让 Codex 检查脚本后运行，或使用：

```text
python tools/verify_package.py
```

校验只读取本文件包，不联网、不创建 Git、不修改工程。它用于验证收到的初始包；开始编辑后出现哈希不匹配是预期现象，不代表项目数据损坏。

## 必须知道的边界

- 本包没有 `.git`、生产 Kotlin/Gradle 工程、安装程序或签名密钥。
- 没有创建远程仓库、设置公开/私有、推送或修改现有 Desktop。
- Git SHA 是上一轮立项对话的**有日期观察**，本次只打包，未重新查询最新 HEAD；A0 按需核对增量，不自动升级依赖。
- [CURRENT_STATE](docs/CURRENT_STATE.md) 是初始化状态；内容不宣称软件功能已完成。
- 任何现有用户数据、API 凭据或真实付费调用都不在 A0 授权范围内。


---

## SOURCE_FILE: `docs/00_INDEX.md`

# 00 — 文档入口

## 每次任务首读

1. [当前状态](docs/CURRENT_STATE.md)
2. [任务登记](docs/TASK_REGISTRY.md)
3. [来源锁](docs/SOURCE_LOCK.json)
4. 本任务合同；当前 [A0-001](tasks/A0_001_BOOTSTRAP_AND_AUDIT.md)

## 产品与工程合同

| 文件 | 用途 |
|---|---|
| [01章程](docs/01_PROJECT_CHARTER.md) | 定位/项目边界 |
| [02需求](docs/02_PRODUCT_REQUIREMENTS.md) | 平台/模式/首版 |
| [03架构](docs/03_ARCHITECTURE_AND_REUSE.md) | 共享/适配/无环 |
| [04数据](docs/04_DATA_SAFETY.md) | 原图/工程/恢复 |
| [05更新](docs/05_UPDATE_AND_RELEASE.md) | 首版更新/发行 |
| [06服务端](docs/06_SERVER_SECURITY.md) | 队列/撤销/授权 |
| [07来源](docs/07_SOURCE_MAP.md) | 精确入口与证据 |
| [08协作](docs/08_GIT_AND_COLLABORATION.md) | Git/工作区/反哺 |
| [09路线](docs/09_ROADMAP.md) | 切片和退出标准 |
| [10验收](docs/10_ACCEPTANCE_MATRIX.md) | 测试与证据分类 |
| [决定](docs/DECISIONS.md) | 初始约束/待设计 |
| [跨项目](docs/CROSS_PROJECT_REGISTRY.md) | 候选与目标验收 |

## 初始化和交接

[使用入口](START_HERE_CN.md) · [Project指令](instructions/PROJECT_INSTRUCTIONS.md) · [Codex开场](handoff/CODEX_START_MESSAGE.md) · [新Project接手](handoff/NEW_PROJECT_OPENING.md) · [Desktop协作说明](handoff/DESKTOP_COLLABORATION_NOTE.md) · [参考文件](references/README.md) · [打包边界](PACKAGING_NOTES.md)。

[合并母本](CCB_STUDIO_SUITE_INIT_20261020.md)用于初始上传/引继，是有日期快照。后续以Git的分文件CURRENT和当前任务维护；不要求同步改写历史母本。

源码未实施前不要因这些文件存在宣称功能PASS。A0产出的审计和实施候选届时再加入索引，不预建空验收记录。


---

## SOURCE_FILE: `docs/01_PROJECT_CHARTER.md`

# 01 — 立项章程

资料日期：2026-10-20。性质：来自立项对话的需求与初始组织方案；不是生产实现或发布许可。

## 产品定位

CCB Studio Suite 是 Windows/Android 原生图像创作套件，包含 LLM 辅助设计/修改 Prompt、NovelAI 生图、可靠本地图库、完整生成参数和元数据管理、多 Prompt/多图对比、非破坏图片编辑、批量打码嵌字、本人 API 直连、私有服务器任务执行与多终端访问，以及从首个实际使用版本起的安全应用内更新。

它不是完整聊天应用，也不是删除 CCB Desktop 若干页面后的改名产品。主运行环境不依赖浏览器、WebView、localStorage 或 IndexedDB。

**服务器原始设计（用户确认）：** Studio Server 由部署者集中配置并保护上游生图 API 凭据；经过授权的 Windows/Android 等客户端发送完整生成 Recipe，不需要各自提供 NovelAI Token。服务器持久排队、集中调度与限制请求频率、允许撤销尚未发出的任务，并将原始结果及 Metadata 归属回传。可选 BYOK 是未来扩展，不得强制取代管理员集中执行模式。供应商服务条款和实际上线风险另行评估，不反向删除此产品能力。

## 项目关系

| 来源/项目 | 责任 |
|---|---|
| 官方 SaltyFishOTL/ChatChatBar | 被选定复用的官方行为、Prompt、请求及 Android 实现来源 |
| MisakaPiano/ChatChatBar-Desktop | 官方桌面移植、已验证共享核心、Windows Studio/平台经验 |
| TypeSet Forge 1.1 | 批量嵌字打码、队列、导出工作流及算法参考 |
| CCB Studio Suite | 独立产品体验、资产库、编辑工程、服务器、跨端协议及升级合同 |

权威按功能领域划分，不能把多个来源混成一个总上游。正确编辑参考是 TypeSet Forge，不是 CCB 制卡器；旧 CCB Editor Reference Pack 只有出现明确相关任务时才作为次级参考。

## 不改变的边界

本项目不自动改变 CCB Desktop 的 P8—P18、正式 upstream baseline、Prompt、数据合同或验收；不把未合入 feature 整条当作稳定依赖；不向官方 upstream 写入；不自动读取、迁移或清理真实图库/聊天数据；不自动创建公开仓库或发布。

## 用户价值与首要承诺

原图、编辑工程、Prompt 和已接收任务不是可随意清除的缓存。工程承诺是不静默覆盖、不以默认值替换损坏数据、不因升级或测试版而清档，失败保留原始内容，并建立可验证备份/恢复。

这不是对硬件损坏、用户手动删除或任何断电场景的绝对无损保证。未测试的耐久性边界必须披露。

## 明确的非目标

首版不复制完整聊天/WorldBook/Memory/RAG/Fish 产品，不自建 NovelAI 模型推理服务，不为了复用而整体重构 CCB 为 KMP，不在初始化阶段创建通用平台或大量无消费者抽象。


---

## SOURCE_FILE: `docs/02_PRODUCT_REQUIREMENTS.md`

# 02 — 产品模式、需求与首版范围

## 三个应用目标、两个执行方式

| 应用 | 本人 API 直连 | 服务器模式 |
|---|---|---|
| Studio Windows | 本机保存图库/工程/本人凭据并直连 LLM/NAI | 使用 Studio 登录身份提交完整 Recipe、管理队列、领取结果；不需客户端 NovelAI Token |
| Studio Android | 完整独立创作，不要求电脑开机 | 同样通过 Studio 登录身份使用服务器；不需客户端 NovelAI Token |
| Studio Server | 管理员集中管理上游 API 凭据，供本人多端使用 | 同一集中执行模式扩展为邀请制多用户、配额和任务隔离 |

Android 不永久降格为遥控器。早期仅远程功能可以作为内部切片，但不能称完整 Android Studio 已完成。

**服务端账户所有权：** 服务器管理员配置的供应商 API 账户与客户端登录 Studio Server 的用户身份是不同实体。客户端无需知晓或携带管理员的上游 Token；任务仍按提交用户/设备/工作区归属，服务器在任务派发时绑定已选定的管理员上游账户。每用户自带 Token（BYOK）只能作为可选模式，不是多人客户端接入的前置条件，也不能擅自替换原始中央队列设计。

“本地/直连”指数据管理在本机并直接调用供应商，不代表 NAI 模型本地推理。离线时图库、已有工程、打码嵌字和 Prompt 编辑仍应可用，云端生图明确不可执行。

## 工作区

本地工作区、服务器 A 的用户身份、服务器 B 的用户身份分别管理资料和权限。切换工作区不自动上传图片/Token，不改变已提交任务的服务器、账户或输入，不把预览缓存当永久副本，不让一个服务器访问另一工作区。

## 第一份可长期使用的 Windows 版本

| ID | 必须具备 | 不接受的替代 |
|---|---|---|
| REQ-01 | 本地图库、重启恢复、备份与导出 | 浏览器缓存/仅内存结果 |
| REQ-02 | 独立 NAI 生图，不依赖聊天会话 | 只保留聊天消息生图 |
| REQ-03 | AI 写 Prompt、继续修改、保留设计过程 | 仅手工输入 |
| REQ-04 | LLM/NAI 多凭据档案、命名和显式选择 | 覆盖唯一全局 Token |
| REQ-05 | 显示真实平台、账户别名、生效模型 | 把协议模板当供应商 |
| REQ-06 | 拖入、文件选择、多图导入、metadata 查看 | 先建角色卡/聊天才可用 |
| REQ-07 | 至少两份完整 Prompt/Recipe 比较 | 只有图像视觉对比 |
| REQ-08 | 非破坏打码、文字图层、批量应用、撤销重做 | 覆盖唯一原图 |
| REQ-09 | 原图、工程、导出副本分开 | 重编码图冒充原始字节 |
| REQ-10 | 自动/手动检查更新，应用内下载验证安装重启 | 仅打开网页 |
| REQ-11 | 升级保留支持的数据和凭据 | 卸载清档重装 |
| REQ-12 | 采用 CCB/Desktop 来源可追溯 | 自动兼容“永远最新” |

内部骨架可以逐步实现；第一份交付长期保存真实图库的版本必须达到上述必要门槛。不能用 Preview/Beta 名义悄悄降低承诺。

## 编辑能力

核心工作流：多图片队列、多选、拖动排序、批量套用；横排/竖排文字、样式、位置、层次；矩形/画笔/圈选打码、马赛克及遮盖；可继续编辑的工程与可解释撤销。

富文本、混合字体、复杂竖排规则须纳入能力清单，由 A0 明确首版和后续范围；分类批量导出、工程与预设交换需要正式边界。小人书/连环画/整册 PDF 是明确候选，不因旧工具有实现就自动扩大首版。

裁剪、缩放、旋转作为通用几何变换独立审计，不假设旧工具已全部解决。跨端应保持工程语义，不要求不同字体栅格引擎输出逐像素相同；差异必须说明和测试。

## 多 API 档案

区分服务类型、真实供应商、稳定账户 ID、别名、Endpoint、ModelConfig、SecretStore 凭据引用。任务提交时冻结实际账户身份和请求快照；后续 UI 切换只影响后续任务。

不做隐式跨账户轮换、跨供应商重试或绕限换号。多档案管理不等于供应商允许多人共享账号或同时购买多个订阅。

NAI 和 LLM 账户/预算分开。多模型已有不等于统一账户管理已实现。**客户端本人直连的账户档案、服务器管理员所持上游 API 凭据、Studio Server 登录身份必须分开实现和审计。** 客户端在远程模式下不必提供自己的 NAI Token，不能从「多 API 档案」推导出必须 BYOK。Fish 不属于首版必需，可留通用扩展边界，不引入完整语音产品。

## Prompt 与对比

保留用户编辑源、AI 设计结果、最终出站有效参数之间的区别。比较应覆盖正向/负向、角色顺序/位置、Seed、模型、尺寸、采样/步数以及参考资产，不只做字符串差异。

复制某段 Prompt 到另一工程是显式编辑操作；完整 Recipe/工程交换、可读文本复制、NAI 请求 DTO 是不同合同。


---

## SOURCE_FILE: `docs/03_ARCHITECTURE_AND_REUSE.md`

# 03 — 架构与代码复用

## 初始方向

Windows 优先 Kotlin/JVM、Compose Desktop、JDK 17；Android 为原生应用，复用适用纯 Kotlin/JVM 逻辑和平台无关合同。不要求先将整个 CCB/Desktop 迁成 Kotlin Multiplatform。

具体 Kotlin/Compose/AGP/SDK 版本由 A0 对照被采用源码冻结，不复制旧快照版本号。

## 逻辑职责

```text
apps/studio-windows
apps/studio-android
server/studio-server
modules/studio-domain
modules/image-editing
modules/asset-library
modules/remote-protocol
modules/update-contract
platform/windows
platform/android
```

这是职责划分，不要求初始化时创建全部空模块。只有真实消费者需要时建立模块与构建配置。

## 依赖与复用优先级

1. 直接依赖经过审计且版本固定的共享模块。
2. 在原权威项目中另立小范围无语义漂移的共享抽取任务。
3. 原项目暂不适合改动时，受控引入精确源码，记录路径、commit、身份、授权、差异、同步与测试策略。
4. 确有平台或产品差异时实现窄适配。

不得无来源复制一套 Prompt、请求构造、WorldBook 或 metadata 解释后分别维护。

本地 Composite Build 是候选，不让正式发行依赖某开发者未提交邻接工作树。发行绑定精确依赖版本和可重现来源。

## Android 的直接参考

文件选择/分享、生命周期、系统安装、平台安全存储可直接参考官方 Android CCB 的适用源码与平台文档。共享请求语义则只选定一套版本；不能让 Windows/Android/Server 各追各的最新实现又宣称一致。

Suite 可比 Desktop 更早采用某项已审核的图像更新，但不得自动提升 Desktop compatibility；反之亦然。

## 无环的双向反哺

允许：CCB 图像请求核心被 Desktop/Suite 消费；通用图片编辑核心被 Suite/Desktop 消费。

禁止：完整 Desktop 应用依赖完整 Suite 应用，而 Suite 又依赖完整 Desktop。通用编辑组件不得依赖服务器、完整聊天容器或 Suite 独有持久化实现。

不急于新建第三个共享仓库。模块可以由任一现有仓库发布；每项能力只有一个维护来源，依赖方向有向无环。

## 接口边界候选（未冻结实现或 schema）

- 生成后端：本人直连使用本机选择的供应商凭据；远程任务后端将完整 Recipe 交给服务器，**服务器以管理员统一配置的上游凭据执行**，不要求远端客户端提供 NAI Token；可选 BYOK 不成为默认依赖；
- 图像资产：不可变原始字节、受控派生物、可重建缓存；
- 账户解析：区分客户端/直连供应商账户、服务器管理员的供应商账户、Studio Server 用户/设备身份，以及各自的安全凭据引用；
- 编辑工程：平台无关操作/层次 + 平台渲染适配；
- 平台能力：文件选择、分享、通知、凭据、安装与生命周期；
- 服务端协议：任务/资产标识，不接受任意本机路径或命令。

以上只是 A0 审计边界，不批准数据库、正式格式或所有类名。


---

## SOURCE_FILE: `docs/04_DATA_SAFETY.md`

# 04 — 数据安全与资源合同

## 对象分离

| 对象 | 职责 |
|---|---|
| 原始资产 | 导入或供应商返回的原始文件字节 |
| Recipe | 参数、角色、参考资产、实际生成选择 |
| AI 设计记录 | 用户要求、LLM 结果及应用过程 |
| 编辑工程 | 文字/遮盖/打码/变换/顺序与继续编辑状态 |
| 派生输出 | 渲染图片、分享副本、缩略图 |
| 任务记录 | 请求归属、状态、结果和失败证据 |
| 缓存 | 可重建内容，不拥有唯一资料 |
| 凭据 | 独立 SecretStore，不在普通工程中 |

完整出站记录去除认证头、Token 等秘密字段。Prompt 是用户私密创作资料，不默认进入诊断日志。

## 原图与保存

原图保存原始字节、大小、哈希及来源关系；编辑、旋转、打码、嵌字、隐私导出生成派生结果，不覆盖唯一原图。结果持久化并验证后才标记“已保存”。

TypeSet Forge 的原始排序导出可能重新编码，不能作为原始字节保全机制。原始文件导出与按排序渲染导出必须不同命名。

## 事务与恢复

必须覆盖写入中断、空间/权限失败、文件提交与索引提交不同步、缺失资源、未知或损坏数据、进程崩溃、重复启动、根目录切换、备份/恢复中断。

临时文件与已提交资源可区分；不确定提交不能当作可安全重试或成功。已收到但无法完整登记的图像应进入明确恢复路径，而非自动清除。

Missing / Corrupt / ReadError 分开；不以空对象或默认配置覆盖未知/损坏 bytes。恢复和迁移保留来源。索引损坏不能作为删除原图的理由；重建范围和不可重建部分须说明。

存储技术由 A0 设计；JSON/数据库/组合均须满足行为合同，不照抄旧浏览器 IndexedDB 状态。

## 编辑与导出

编辑工程与最终烧录输出分开。文字/遮盖图层可继续编辑；批量动作有清楚目标集合和撤销单位。缺字体、缺参考图、未知操作不静默丢弃内容后假称完整还原。

普通保留 metadata 导出与隐私导出是独立操作。隐私导出检查 Prompt、工程、隐藏图像与其他不该公开的信息；不只删除常见文本字段就声称安全。打码/模糊不宣传为绝对不可逆匿名化。

## 数据根与备份

Studio 使用独立数据根、应用身份和所有权控制，不直接写运行中的 Desktop 数据根。程序文件与数据分离，Portable 也不能整体覆盖。

从首份真实使用版本开始，用户资料不可作为测试废品。提供可查看数据目录、可验证备份恢复及升级保全。凭据普通导出默认排除；不同设备不假定能解密同一 OS 绑定密文。

## 验证边界

校验和证明文件身份，不自动证明来源可信、业务有效或断电耐久性。绝对无损/任意断电保证不得凭模拟测试宣称。


---

## SOURCE_FILE: `docs/05_UPDATE_AND_RELEASE.md`

# 05 — 首版应用内更新与发布合同

## 首版门槛

自动检测/手动检查 → 显示版本/渠道/说明 → 用户确认 → 应用内下载 → 验证可信来源/签名/产品与平台身份 → 保存编辑并协调写入/任务 → 安全退出或维护 → 安装 → 重启 → 验证程序和数据 → 报告真实结果。

仅网页跳转、让用户猜覆盖方法、卸载清档重装、强杀写入者、下载结束即称更新成功，均不合格。允许用户确认、系统授权与重启，不要求运行程序强行原地替换自己。

本要求比 Desktop 的最低“发现＋安全手动升级”要求更强，不改变 Desktop 原合同；Suite 成果未来可独立反哺。

## 多种版本分开

软件版本、数据格式版本、远程协议版本、更新元数据格式、CCB 依赖版本分开记录。源代码依赖升级不是客户端安装更新；两者各需验证。

安装成功不等于数据迁移成功；二进制回退不等于旧版能安全读写新版数据。不能恢复旧快照而悄悄删除升级后新作品。

## 安全更新链

设计错误包/产品/架构/渠道识别、篡改、回滚、冻结更新、源失效、密钥轮换和过期元数据的处理。校验和不能单独证明发布者身份。评估成熟的签名/更新方案，不自创未经验证协议。

密钥/证书私钥/Android keystore 不进入 Git、Project、日志或普通备份。测试签名与发行签名分开，正式首次可升级版本就固定身份与保管方案。

## Windows

应用负责发现、下载、验证和发起安装；独立更新器/安装器可负责程序替换。程序与资料分开，Portable 同父目录也不能整体覆盖。安全排水未完成，保持可见阻塞，不强杀、不释放仍需保护的根所有权。

## Android

独立 application ID，与官方 CCB 并存。自分发 APK：应用内下载校验后进入系统安装，处理确认、拒绝、取消、失败，不绕过系统静默安装。Google Play 属另一发布渠道，不混用其安装假设。

稳定应用与签名身份、版本递增、覆盖升级不丢资料、后台/进程回收边界均须测试。

## 更新前数据处理

优先兼容读取；必要迁移前保全/快照并定义提交点、失败恢复和磁盘预算。更新下载与应用资料目录分离。迁移失败保留原数据；未知新格式旧版不得继续写。

## 首次升级演练

“第一版没有旧版”不能免测。先制作内部 A 写入合成图库/工程/设置/测试凭据，再用内部 B 经真实更新链升级。覆盖正常、断网、坏包、空间不足、安装失败、迁移中断、新版启动失败及任务进行中更新。

## Server

服务器首版也要安全维护更新，仅管理员可发起；普通客户端更新自己不得改服务器程序。新旧协议兼容则继续，不兼容明确拒绝相关操作，不清库、不盲重发。

## 发布条件

当前只定义门槛，未有产品版本、下载源、签名、发行渠道或已通过证据。实际付费验证另行授权。Windows 首个长期使用版和 Android/Server 各自首次正式交付都需通过其对应门槛。


---

## SOURCE_FILE: `docs/06_SERVER_SECURITY.md`

# 06 — 服务器、队列与授权

## 执行和持久化

Server 为独立进程，不依赖 Studio 窗口。验证并持久化任务后才确认接收。客户端刷新/断开/换设备不丢已接收任务。服务器保存成功与客户端永久入库是不同事实。

## 集中 API 网关：原始产品需求

**管理员集中上游凭据为默认服务器架构。** 部署者在服务器端配置一个或多个可明确选择的上游 API 账户；授权客户端仅使用 Studio Server 的身份认证，提交完整 Recipe、必要参考素材和幂等任务标识，**无需自己提供 NovelAI Token**。服务器负责统一排队、调度、发起上游请求、保存原图及 Metadata，并按请求者归属分发结果。

服务器内的「供应商账户」与「Studio 用户/设备」必须分开：多个 Studio 用户可共享同一服务器队列和由管理员选择的执行凭据，但他们的任务、图片与可见进度彼此隔离。不能把管理员 Token 下发给客户端或写入回传图像、日志及普通导出。用户数量、队列深度与真实上游请求数量是独立维度。BYOK 可作为未来可选接入方式，不是服务器可运行或多人连接的必要条件。

## 分开的配额

| 控制 | 目的 |
|---|---|
| 用户时段提交次数 | 防连续刷队列 |
| 用户待执行任务上限 | 防占满等待队列 |
| 用户/工作区存储 | 防图像与参考素材耗尽磁盘 |
| 供应商请求并发/速率 | 遵守实际限制 |
| 服务器实际执行所选上游账户的用量预算 | 控制真实支出；不以客户端用户数量直接换算上游账户数 |
| 全局队列/资源上限 | 保持可管理可恢复 |

这些是最大发送/接纳预算，不是最低生成量，不是“免封阈值”。按用户公平调度，不能一人批量提交永久挡住其他人。

## 任务状态候选

等待执行 → 已领取未发送 → 正在发送/供应商处理中 → 正在接收保存 → 已完成；另有撤销、失败、结果不确定。正式 enum/持久化 schema 未冻结。

撤销与调度原子协调。撤销尚未发送任务后不再发送。已发送后取消等待不保证供应商停工或不计费。断线/重启无法判断是否执行时保留“不确定”，不自动重排无限付费重试。

撤销可释放待执行占位；提交频率额度是否返还另定，防反复提交/撤销绕限。时间窗口的时区、边界、重启与时钟回拨行为须明确，不靠 UI 自报剩余额度。

## 多终端

幂等提交使网络重试只产生一个逻辑任务。任一已授权设备可访问/撤销本人的任务；重复撤销有稳定结果。Job ID 不是访问凭证。多设备编辑工程用版本检查和冲突处理，不默认最后写入无条件覆盖。

服务端已完成任务和文件至少保留到既定保留策略；未下载结果不可被误当可重建缓存。存储清理须可解释、可控。

## 连接与身份

初期候选：私有网络准入＋TLS＋应用用户/设备认证＋逐资源授权。Tailscale 是网络层候选，不能替代 Studio 用户权限。默认不开放公网、不创建公开注册、不改路由器。

邀请/设备登记可撤销，普通用户与管理员分权。客户端不得获得任意服务端路径、用户目录、签名或其他用户资产访问。

服务器保存凭据须说明管理员/运行环境的信任边界；OS保护不等于管理员不能接触运行时凭据，不宣称端到端保密。

## 输入与攻击面

客户端不得指定任意本机路径、命令、脚本、供应商Endpoint或内网URL。检查路径穿越、重复资产、压缩炸弹、图像像素/解码预算、上传大小/数量、内存/磁盘预算、越权、重放与日志泄密。

仅客户端校验不够；服务端必须独立验证。有效图像数据也可能是敏感内容，不进入公共诊断包。

## 外部条款和实际上线风险（与产品能力分离）

管理员集中 API 的代理执行是**已记录的原始产品架构**，不得因为第三方服务条款就未经用户确认改成「所有客户端必须 BYOK」。NovelAI 等供应商对于第三方账号使用、代理或转售可能有限制；这种风险应单独记录、核对和决定真实部署边界，不能被混写成删除中央网关的产品需求。

统一服务器出口和严格限速属于网络与资源调度设计，**不证明供应商无法识别访问模式，也不提供防封号保证**。本项目不将伪装访问来源或规避平台检测机制作为交付目标。该风险评估不妨碍先以离线/合成 API 完成多用户队列、账号保管和回传功能的开发验收；实际接入与对外开放另作决定。


---

## SOURCE_FILE: `docs/07_SOURCE_MAP.md`

# 07 — 来源地图与精确读取

**日期锚点：2026-10-20。此表沿用上一轮立项会话的读取记录；本次打包没有重新查询 GitHub。** 当前 HEAD 可已前进，A0 用 live 查询核对必要增量。

| 来源 | 已记录版本/身份 | 用途与限制 |
|---|---|---|
| Desktop 已集成线 | `5ef95428421f7c693b010ac88f51d470804ab07a` (`desktop`) | 首次复用审计锚点，不等于已经成功抽取 |
| Desktop 声明 validated upstream | `5e76a9cb841736bbbf3499a2e35e5789af4c5ca8` / 1.4.1 | 官方语义参考；选择性图像移植另读 compatibility |
| Desktop 活动 feature | `c4098e08bec84372911a5a8bc38c2cfa72f98978` | 需求/开发参考，不整分支导入 |
| 官方 observed master | `979cb505958dffb1b6d64a8de657434f198ffa96` | 新变化观察，不自动升为依赖 |
| TypeSet Forge | 1.1 HTML、说明书、更新说明 | 本包原始字节及SHA-256已核对，功能未执行验收 |

活动 feature 相对更早的 `c280dca127ca246d03d301c5fa8af8a78b69fd20` 已有后续提交，这是上一轮记录的时间差，不自动扩大 Suite 采用范围。

## Desktop 首读

```text
docs/desktop/00_PROJECT_SOURCE_MAP.md
docs/desktop/10_UPSTREAM_BASELINE.json
docs/desktop/21_CURRENT_STATE.md
docs/desktop/13_FEATURE_PARITY.md
docs/desktop/14_UPSTREAM_COMPAT.md
docs/desktop/92_PHASE7_POST_MERGE_CLOSEOUT.md
```

按任务再读 `55_PHASE7_DEFERRED_OWNER_MAP.md`、`64_PHASE7_SCOPE_OWNER_UPDATE.md`。`146_MULTI_API_ACCOUNT_PROFILES_CONTRACT.md`/D-044 从活动 feature 的精确版本读取，它是未实施需求，不假设在旧稳定desktop存在。

## 官方定向入口

`.agents/skills/chatbar-image-generation-runtime/SKILL.md` 在已知基线可读；再按其真实入口读取 Prompt Designer、Studio Models/Repository、ImageService、metadata、guidance、history、Android平台适配与测试。其他Skill先确认存在，不臆造路径。

## TypeSet Forge

[参考说明](references/README.md)和[原始文件清单](references/REFERENCE_ASSET_MANIFEST.json)记录文件名映射、大小与SHA-256。HTML静态审计，不运行、不加载外部字体/脚本、不读取浏览器资料。

## 验证口径

本包哈希证明取得的文件字节身份，不证明它就是用户本机最后修改版本、不代表运行验收、不授予再分发许可。旧聊天观察、真实源码与正式采用状态严格分开。

所有来源采纳记录采用：来源 → 精确身份 → 功能 → 共享/适配/仅参考 → 差异 → 测试 → 后续同步责任。


---

## SOURCE_FILE: `docs/SOURCE_LOCK.json`

```json
{
  "recordFormat": 1,
  "recordType": "initiation_source_observation",
  "recordedDate": "2026-10-20",
  "timezone": "Asia/Tokyo",
  "isProductionDependencyLock": false,
  "gitObservationProvenance": "Previous initiation conversation; not re-fetched during this packaging run",
  "sources": [
    {
      "id": "CCB_OFFICIAL_BASELINE",
      "repository": "SaltyFishOTL/ChatChatBar",
      "commit": "5e76a9cb841736bbbf3499a2e35e5789af4c5ca8",
      "versionLabel": "1.4.1",
      "role": "desktop_declared_validated_baseline",
      "adoption": "audit_input_only"
    },
    {
      "id": "CCB_OFFICIAL_OBSERVED",
      "repository": "SaltyFishOTL/ChatChatBar",
      "ref": "master",
      "commit": "979cb505958dffb1b6d64a8de657434f198ffa96",
      "role": "observed_upstream",
      "adoption": "not_validated_not_automatically_adopted"
    },
    {
      "id": "CCB_DESKTOP_INTEGRATED",
      "repository": "MisakaPiano/ChatChatBar-Desktop",
      "ref": "desktop",
      "commit": "5ef95428421f7c693b010ac88f51d470804ab07a",
      "role": "initial_reuse_audit_anchor",
      "adoption": "candidate_not_yet_extracted"
    },
    {
      "id": "CCB_DESKTOP_DEVELOPMENT",
      "repository": "MisakaPiano/ChatChatBar-Desktop",
      "ref": "feature/phase8-fish-audio",
      "commit": "c4098e08bec84372911a5a8bc38c2cfa72f98978",
      "role": "development_and_requirements_reference",
      "adoption": "no_whole_branch_import"
    },
    {
      "id": "TYPESET_FORGE_REFERENCE",
      "versionLabel": "1.1",
      "files": [
        {
          "path": ".reference-assets/typeset-forge-1.1/TypesetForge_1.1.html",
          "originalName": "TypesetForge_1.1.html",
          "libraryFileId": "file_000000001fa881fda823d0f4d32758e5",
          "sizeBytes": 200005,
          "sha256": "892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63",
          "byteHandling": "copied_without_content_modification",
          "executed": false
        },
        {
          "path": ".reference-assets/typeset-forge-1.1/README_1.1.md",
          "originalName": "README(2).md",
          "libraryFileId": "file_00000000456881f6a50288e0ee7c393e",
          "sizeBytes": 16158,
          "sha256": "cd6fda7d43c1993e15f075d26fa1dc3e15b3729c37cb5a3258e1bf566099bf05",
          "byteHandling": "copied_without_content_modification",
          "executed": false
        },
        {
          "path": ".reference-assets/typeset-forge-1.1/CHANGELOG_1.1.md",
          "originalName": "CHANGELOG_1.1.md",
          "libraryFileId": "file_0000000074a4820d9b501c5982ed77d2",
          "sizeBytes": 1594,
          "sha256": "58486e13439e1593a0d07054413bbeeaadad50912d73e96cff0f9689115b3a31",
          "byteHandling": "copied_without_content_modification",
          "executed": false
        }
      ],
      "role": "local_read_only_workflow_and_source_reference",
      "adoption": "no_browser_runtime_transplant",
      "hashStatus": "sha256_computed_from_materialized_raw_bytes_in_this_packaging_run"
    }
  ]
}
```


---

## SOURCE_FILE: `docs/08_GIT_AND_COLLABORATION.md`

# 08 — Git、工作区与双向协作

## 独立仓库

拟议 `MisakaPiano/CCB-Studio-Suite`，独立普通仓库而非整体 Fork Desktop。可公开，不强制私有；“源码可见性”“私人部署”“运行时数据保密”分开。建项本身不创建或公开远程仓库。

本地 `H:\CCB-Studio-Suite` 与 `H:\ChatChatBar-Desktop` 平级。Studio任务不切换、reset、clean、stash或构建正在工作的Desktop目录。

## 分支

`main` 稳定集成；`feature/*` 功能；`integration/*` 联调；`sync/*` 范围明确的来源升级；`release/*` 发布准备。平台和运行模式通过模块/配置，不用长期android/private/network分支。

不同应用可独立发布，配套协议/数据兼容范围清楚。不因Server修复强制Windows同时发版。

## 只读参考

优先精确commit读取。需独立参考副本放在忽略的 `.reference-repos/`，记录大小和保留策略。不修改邻接工程；默认分支不代表Desktop主线，必须显式选 `desktop` 或精确SHA。

不把全部旧Project Sources复制进新项目；按来源地图读取。构建、参考、截图、发行包需保留规则，不无限堆积。

## 提交和授权

初始A0允许必要本地git init，但不自动commit/push/merge/创建远程/修改可见性。逐路径stage；参考资产、用户数据、私密Prompt、Token和签名材料排除。没有授权记录不得新增统一许可证覆盖混合来源。

现有Desktop授权不被本项目否定；新Suite所采用范围独立核对。不把仓库Public当作任意再分发许可，也不以Private替代许可核对。

## 反哺流程

候选 → 来源实现/测试 → 目标项目审查 → 目标实施 → 目标验证 → 精确采用版本。

登记见 [CROSS_PROJECT_REGISTRY](docs/CROSS_PROJECT_REGISTRY.md)。Suite登记只陈述候选/来源事实，目标工程的CURRENT与验收仍由目标项目维护；不能一份跨项目表覆盖双方批准权。

一个任务一个主执行者。只读跨项目参考不等于可写两个仓库。并行任务不得改同一工作树同一文件。


---

## SOURCE_FILE: `docs/09_ROADMAP.md`

# 09 — 路线图

| 阶段 | 交付 | 退出条件 |
|---|---|---|
| A0 建项/复用审计 | 来源、依赖图、差距、更新候选、首个实施合同 | 真实源码依据与未决影响清楚 |
| A1 数据/更新基础 | 独立数据根、原图/工程持久化、恢复、更新垂直原型 | 合成资料跨版本升级与失败保全 |
| A2 Windows创作闭环 | AI写Prompt、NAI、多账户、图库、metadata、对比 | 真实流程可用，实际供应商验证另授权 |
| A3 编辑工作台/首版收敛 | 批量打码嵌字、非破坏编辑、导出、完整更新 | 首个长期使用版全部必要门槛通过 |
| B1 本人私有Server | 管理员集中上游 API 凭据；本人多端无须客户端 Token，持久队列、撤销、下载、恢复 | 断线重启不丢任务、不盲重付费 |
| C1 Android完整Studio | 本人直连＋Server，完整创作/编辑/更新 | 平台等位及数据/更新门槛 |
| B2 邀请制多用户 | 延续管理员集中 API 网关：身份隔离、提交/排队配额、公平调度、设备撤销与管理员维护；BYOK 不强制 | 真实多用户安全证据；外部条款/实际上线另行决策，不反向改写架构 |
| 后续增强 | 小人书、复杂排版、更多平台/服务 | 独立决定，不倒灌首版 |

Android约束从A0介入，不能等Windows完成才发现核心全是Windows API。B1/C1是否并行按依赖和本机资源，不默认并行重构建。

第一份长期使用Windows版本在A3之后；A1/A2是内部阶段，不冒充已满足首版。各阶段完成不自动授权下一阶段生产实现。

A0当前只读源码＋新项目控制文档，不跑Gradle/模拟器/打包。模型选择以当时可用代码模型/较高推理为候选；没有新项目样本，不虚构耗时/额度预测。后续每项实施任务再定推荐配置、验证范围和中断checkpoint。


---

## SOURCE_FILE: `docs/10_ACCEPTANCE_MATRIX.md`

# 10 — 验收矩阵

**全部为待执行标准；文件包校验不等于产品验收。**

| ID | 必须验证 | 阻塞范围 | 当前 |
|---|---|---|---|
| DATA-01 | 导入/生成原图字节、哈希与来源保全 | 首版 | NOT TESTED |
| DATA-02 | 编辑保存、重启重开、批量状态恢复 | 首版 | NOT TESTED |
| DATA-03 | 写入中断、空间不足、索引与文件不一致 | 首版 | NOT TESTED |
| DATA-04 | 损坏/未知数据不被默认值覆盖 | 首版 | NOT TESTED |
| DATA-05 | 备份恢复成功及恢复失败保全 | 首版 | NOT TESTED |
| DATA-06 | 与Desktop数据根、锁和凭据隔离 | 首版 | NOT TESTED |
| GEN-01 | AI设计可应用、修改、撤回并追溯 | 首版 | NOT TESTED |
| GEN-02 | 最终发送参数与Recipe记录对应 | 首版 | NOT TESTED |
| GEN-03 | 实际平台/账户/模型显示正确且任务中冻结 | 首版 | NOT TESTED |
| GEN-04 | 失败/取消/429/结果不确定不产生无界重试 | 首版 | NOT TESTED |
| GEN-05 | 凭据不入metadata/日志/工程/普通导出 | 首版 | NOT TESTED |
| EDIT-01 | 文字/打码/变换不覆盖唯一原图 | 首版 | NOT TESTED |
| EDIT-02 | 多选/批量/排序与撤销边界 | 首版 | NOT TESTED |
| EDIT-03 | 横竖排、中日文输入、缩放与触控 | 对应平台 | NOT TESTED |
| EDIT-04 | 普通与隐私导出的元数据边界 | 首版 | NOT TESTED |
| UX-01 | 拖入、查看Prompt、比较完整Recipe | 首版 | NOT TESTED |
| UX-02 | 长Prompt、窄窗口、高DPI、键盘可达 | Windows首版 | NOT TESTED |
| UPD-01 | 内部A→B真实应用内更新 | 首版 | NOT TESTED |
| UPD-02 | 坏包/签名/渠道/产品/平台错误拒绝 | 首版 | NOT TESTED |
| UPD-03 | 取消/断网/空间不足保留当前数据 | 首版 | NOT TESTED |
| UPD-04 | 编辑/生成中更新安全交接 | 首版 | NOT TESTED |
| UPD-05 | 迁移中断与程序回退兼容性限制 | 首版 | NOT TESTED |
| AND-01 | 独立应用身份和签名下覆盖升级保留数据 | Android首版 | NOT TESTED |
| AND-02 | 后台/回收/分享/文件入口/离线编辑 | Android首版 | NOT TESTED |
| NET-01 | 重复提交和网络重试只形成一个逻辑任务 | Server首版 | NOT TESTED |
| NET-02 | 撤销与发送竞争，未发送任务确实不发送 | Server首版 | NOT TESTED |
| NET-03 | 重启不盲重发不确定付费请求 | Server首版 | NOT TESTED |
| NET-04 | 同用户跨设备与工程版本冲突 | 多端 | NOT TESTED |
| NET-05 | 管理员配置的上游凭据仅在 Server 内使用；授权 Windows/Android 客户端不提供 NAI Token 仍可提交 Recipe、取回原图/Metadata | Server首版 | NOT TESTED |
| NET-06 | 多用户共享管理员选择的上游执行账户时，任务归属与队列、限速、撤销互不串线；BYOK 不构成默认前置 | 多用户 | NOT TESTED |
| SEC-01 | A不能访问B任务/资产/工程 | 多用户 | NOT TESTED |
| SEC-02 | 上传预算、路径穿越、解压/解码防护 | Server首版 | NOT TESTED |
| SEC-03 | 用户/设备撤销后权限失效 | 多用户 | NOT TESTED |
| SEC-04 | 管理员与普通用户更新权限隔离 | Server首版 | NOT TESTED |
| SEC-05 | 客户端/其他用户无权读服务器管理员上游 Token；图像回传、任务查询及诊断不泄露凭据 | Server首版 | NOT TESTED |
| COMP-01 | 新旧client/server协议兼容或明确拒绝 | 多端 | NOT TESTED |
| REUSE-01 | 共享模块无环、来源与采用版本可追溯 | 所有阶段 | NOT TESTED |

证据至少记录源码SHA、执行环境、自动化/人工、合成/真实供应商、结果、原始证据入口和未覆盖项。

源码存在、测试代码存在、编译成功、测试执行、人工通过、集成、发布不能互相替代。保留FAILED、SKIPPED、NOT TESTED、UNKNOWN及旧失败。真实付费调用另行授权，不为“绿色”擅自请求。

里程碑以交付范围判断，不通过减少测试或静默删除需求来闭合。


---

## SOURCE_FILE: `docs/CURRENT_STATE.md`

# CURRENT STATE — 初始化状态

资料日期：2026-10-20；包版本 INIT-1.0。

| 项目 | 当前事实 |
|---|---|
| 项目 | CCB Studio Suite |
| 阶段 | A0准备；尚未运行A0源码复用审计 |
| 立项文件 | 已整理成分文件包与合并母本 |
| 生产源码 | 未开始 |
| Windows本地工作区 | 需用户解压后由Codex核验，不能以云端包生成冒充已部署 |
| GitHub远程仓库 | 本轮未创建，存在性未核验 |
| 产品验收/发布 | 无 |
| 真实用户数据 | 未授权读取/迁移 |
| 真实付费API | 未授权 |
| 签名密钥 | 未创建、未上传 |

## 下一项

运行 [A0-001](tasks/A0_001_BOOTSTRAP_AND_AUDIT.md)：核对现成控制文件、只读来源和复用边界，生成A0审计与首个实施候选后停止。不要重复生成相同文档，也不要直接实施A1。

## 已知边界

Git来源SHA沿用前一轮有日期观察，本包没有重新查询live GitHub。TypeSet三文件原始字节已在本次包中计算SHA-256；这不是运行验收，也不证明用户本机无更新版本。

新项目不改变CCB Desktop活动任务、验收、Prompt或baseline。跨项目候选见CROSS_PROJECT_REGISTRY。


---

## SOURCE_FILE: `docs/TASK_REGISTRY.md`

# TASK REGISTRY

| ID | 任务 | 状态 | 权限/停止点 |
|---|---|---|---|
| INIT-000 | 立项材料 | PACKAGED | 文档/参考交付，不等于软件实现 |
| A0-001 | 核对初始化并只读复用审计 | READY / NOT STARTED | 新目录控制文件＋只读来源；报告后停 |
| A0-002 | Project审查A0真实结果 | NOT STARTED | 审当前报告，不重做全历史验收 |
| A1-001 | 首个生产实施 | NOT AUTHORIZED | A0形成精确合同后另行授权 |
| X-000 | 向Desktop登记关系 | PROPOSED | 使用交接说明，目标项目自行登记 |

## 执行者

A0主执行者尚未登记。下发后记录实际Codex对话/工作区和开始时间；不得开第二个重复执行者。

## 每项回执

记录：实际目录/branch/HEAD、允许范围、实际改动、源码/测试/证据身份、是否提交/推送、验证、未覆盖项、下一步、真正阻塞。模型元数据来自实际选择，不推测。无本地访问不声称worktree clean。


---

## SOURCE_FILE: `docs/DECISIONS.md`

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


---

## SOURCE_FILE: `docs/CROSS_PROJECT_REGISTRY.md`

# CROSS PROJECT REGISTRY

本表记录候选/来源/状态，不能覆盖目标工程的CURRENT或替代其验收。

| ID | 内容 | 方向 | 当前 | 目标证据 |
|---|---|---|---|---|
| X-001 | 图像请求/Prompt Designer共享 | Desktop→Suite | 待A0 | 精确模块/依赖/测试 |
| X-002 | 原生批量嵌字打码 | Suite→Desktop | 需求候选，未实现 | 独立通用模块/接口/验收 |
| X-003 | 多API档案和请求身份 | 共同合同、分别接入 | 待设计 | D-044映射、迁移、SecretStore |
| X-004 | 安全应用内更新 | Suite→Desktop候选 | 未实现 | 安全链/平台证据；不改Desktop原门槛 |
| X-005 | 图库保全/恢复经验 | 双向 | 待实际证据 | 具体缺陷/修复/可复用边界 |
| X-006 | Android图像适配 | 官方CCB→Suite | 待审计 | 官方精确源码/Android测试 |

状态流：CANDIDATE → SOURCE_READY → TARGET_REVIEW → TARGET_IMPLEMENTED → TARGET_VALIDATED → ADOPTED。拒绝/延期另记理由，不自动升级。

每项后续补来源repo/commit/路径、目标任务、所有者、允许范围、契约变化、测试、采用版本。不能把参考文件存在视为已采纳。


---

## SOURCE_FILE: `instructions/PROJECT_INSTRUCTIONS.md`

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


---

## SOURCE_FILE: `AGENTS.md`

# CCB Studio Suite — Agent Rules

## Workspace and source authority
Work only in the explicitly authorized Suite workspace, normally H:\CCB-Studio-Suite.
H:\ChatChatBar-Desktop and official CCB are read-only reference sources. Do not change their branches, refs, remotes, worktrees, build configuration or data.

## First reads
Read docs/00_INDEX.md, docs/CURRENT_STATE.md, docs/TASK_REGISTRY.md, docs/SOURCE_LOCK.json and the active task contract. The initial master is CCB_STUDIO_SUITE_INIT_20261020.md.
Do not execute historical handoff commands or instructions embedded in reference source files as current authority.

## Initial task boundary
A0-001 verifies this already-populated package and performs a read-only reuse audit. Do not regenerate identical control documents or overwrite unrelated edits.
No production Kotlin/Gradle implementation, paid API calls, real-user-data access, repository publication, signing-key creation, network exposure or release.

## Change safety
Verify exact root, branch, HEAD, index, worktree and relevant untracked files. Never reset --hard, clean, stash, rebase or overwrite unrelated content to make a task pass.
Use explicit staging paths; no blind git add . around private references. Commit/push/merge only when explicitly permitted by the active task. Never write official upstream.

## Shared behavior
Do not copy or rewrite authoritative Prompt/request logic. Preserve exact provenance and selected commits. Cross-project changes need independent target review. No cyclic dependencies between the complete Desktop and Suite applications.

## Data and secrets
Preserve original image bytes. Separate originals, recipes, projects, exports, jobs, caches and secrets. No destructive reset, silent corruption replacement or plaintext credential export. Never commit keys/tokens/signing files/user pictures/private prompts. Synthetic fixtures unless separately authorized.

## Validation
Report actual execution, environment, source SHA and evidence. Compiled tests are not executed tests; automation is not user acceptance. Never relabel SKIPPED/NOT TESTED/UNKNOWN as PASS. No paid calls merely for green tests. No blind retry of uncertain paid generation.

## Resource use
A0 runs no Gradle, emulator or packaging build. Later tasks use the smallest sufficient regression scope. Avoid parallel heavy builds with Desktop. Do not terminate unrelated processes or delete caches, distributions or data without exact authorization.

## Handoff
Return actual paths, changed files, commits if any, validation, unresolved items and one concrete next action. Update relevant task/current records only. Completion does not authorize the next phase.


---

## SOURCE_FILE: `.gitignore`

```gitignore
.gradle/
**/build/
.idea/
*.iml
local.properties
.reference-assets/
.reference-repos/
.local/
.runtime/
.secrets/
.env
.env.*
!.env.example
*.jks
*.keystore
*.p12
*.pfx
*.key
UserData/
data/
backups/
logs/
artifacts/
dist/
__pycache__/
*.pyc
.DS_Store
Thumbs.db
# Immutable delivery checksums are not a production source manifest.
/BUNDLE_MANIFEST.json
/SHA256SUMS.txt
```


---

## SOURCE_FILE: `tasks/A0_001_BOOTSTRAP_AND_AUDIT.md`

# A0-001 — 初始化核对与只读复用审计

状态：READY / NOT STARTED。包已包含控制文件，任务从核对与审计开始。建议采用当时可用代码模型和较高推理；不虚构耗时或额度。Fast默认OFF，用户实际选择另记。

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


---

## SOURCE_FILE: `handoff/CODEX_START_MESSAGE.md`

# 发给新 Codex Project 的开场消息

先将整个包解压为 `H:\CCB-Studio-Suite`，在 Codex 打开该目录，再复制：

---

执行 CCB Studio Suite / A0-001。

请先读取 AGENTS.md、START_HERE_CN.md、docs/00_INDEX.md、docs/CURRENT_STATE.md、docs/TASK_REGISTRY.md 和 tasks/A0_001_BOOTSTRAP_AND_AUDIT.md。

立项母本 CCB_STUDIO_SUITE_INIT_20261020.md、分文件控制文档和 TypeSet Forge 1.1 只读参考已经随包提供。不要重新生成相同文件，不覆盖我的额外修改。按A0合同核对来源、静态审计复用边界，交付A0报告与首个实施候选后停止。

只在新的 H:\CCB-Studio-Suite 工作。H:\ChatChatBar-Desktop 和官方CCB只读。不写生产功能、不跑Gradle/模拟器、不请求付费API、不读真实用户数据、不自动commit/push/merge或创建远程仓库。

先核对实际目录、是否嵌套在其他Git仓库、已有文件和Git状态。只有确认是正确的新独立目录时，才按A0许可进行必要的本地git init。完整权限与停止点以任务书为准。


---

## SOURCE_FILE: `handoff/NEW_PROJECT_OPENING.md`

# 新 Project 第一条消息

复制下面正文发送：

---

开始 CCB Studio Suite。请读取已加入项目资料的 CCB_STUDIO_SUITE_INIT_20261020.md，按本项目Project Instructions接管。

当前只是分文件立项包，没有因打包而创建远程仓库、实现功能或验收发布。Windows/Android目标都是完整Studio，支持本人API直连和Server工作区。**Server 原始设计：管理员集中配置上游 API，授权客户端无须提供 NAI Token，完整 Recipe 统一排队、限速、可撤销未发送任务，结果按用户返回原图/Metadata；BYOK 不强制。** 必须保留AI写Prompt、多账户、永久图库、非破坏批量打码嵌字，以及首个长期使用版本的完整安全应用内更新。

正确编辑参考是TypeSet Forge 1.1，不是CCB制卡器；包内已附只读原始文件。官方CCB/Desktop/TypeSet按领域作为来源，不改Desktop排期、baseline、Prompt或验收。

先确认A0-001当前执行者：尚未下发就使用包内任务书；已经执行则等实际报告审查，不创建重复执行者。接手只有限核对，不重建全历史，不重复询问已明确需求；技术未决由A0提出有依据的方案。


---

## SOURCE_FILE: `handoff/DESKTOP_COLLABORATION_NOTE.md`

# 发回 CCB Desktop 的协作说明

CCB Studio Suite已形成独立立项资料包。拟议repo为MisakaPiano/CCB-Studio-Suite，本地H:\CCB-Studio-Suite，独立ChatGPT Project/Codex工作区。本轮没有建立远程仓库或生产实现。

产品包含Windows/Android完整Studio和独立Server；客户端支持本人API直连/服务器工作区。首个长期使用版本要求完整安全应用内更新，不以网页下载替代。

来源：官方CCB的选定Prompt/请求与Android实现；Desktop已审核共享核心和Windows适配；TypeSet Forge 1.1批量打码嵌字工具（不是CCB制卡器）。

初始Desktop复用锚点：desktop @ 5ef95428421f7c693b010ac88f51d470804ab07a。活动feature仅作精确需求/源码参考，不整条引入，不当稳定集成依赖。

双方成果可反哺，每个共享模块一个维护来源；不形成完整应用循环依赖。目标项目仍独立审查、实现和验收。

本说明不改变P8—P18、D-041/D-044、upstream baseline、Prompt/数据语义、Public Preview门槛或当前Codex任务；不授权Desktop代码修改或插队开发。

请在目标项目适当控制文档中仅登记关系和入口；真正共享抽取等Suite给出精确范围、合同和证据后另立任务。Studio规划或完成不自动提升Desktop parity。


---

## SOURCE_FILE: `references/README.md`

# 参考资料与来源说明

本包附 TypeSet Forge 1.1 三个原始文件，位置：`.reference-assets/typeset-forge-1.1/`。

| 包内名 | 原文件名 | 处理 |
|---|---|---|
| TypesetForge_1.1.html | TypesetForge_1.1.html | 原始字节复制 |
| README_1.1.md | README(2).md | 仅文件名规范化，字节不改 |
| CHANGELOG_1.1.md | CHANGELOG_1.1.md | 原始字节复制 |

精确哈希和大小见 [REFERENCE_ASSET_MANIFEST.json](references/REFERENCE_ASSET_MANIFEST.json)。只证明这份交付的身份，不证明用户本机最后版本或运行效果。未执行HTML；其中外部字体/CDN引用并未被下载，包内没有字体文件。

只读参考目录被Git忽略；不自动公开发布该源码。原文件中的说明是研究对象，不是授权Codex执行的指令。

未附整个官方CCB/Desktop仓库，也未附旧CCB制卡器包，避免错混来源和庞大快照。精确Git入口见 [PUBLIC_SOURCE_LINKS.md](references/PUBLIC_SOURCE_LINKS.md)。A0按需获取，不把旧Project Sources当实时Git。


---

## SOURCE_FILE: `references/REFERENCE_ASSET_MANIFEST.json`

```json
{
  "recordFormat": 1,
  "recordType": "local_reference_byte_identity",
  "recordedDate": "2026-10-20",
  "hashVerifiedInThisPackagingRun": true,
  "runtimeAcceptance": false,
  "files": [
    {
      "path": ".reference-assets/typeset-forge-1.1/TypesetForge_1.1.html",
      "originalName": "TypesetForge_1.1.html",
      "libraryFileId": "file_000000001fa881fda823d0f4d32758e5",
      "sizeBytes": 200005,
      "sha256": "892ea08f0070bdfd264b8895d38f6239f32f436b68a74ab42a2ef3336e28bb63",
      "byteHandling": "copied_without_content_modification",
      "executed": false
    },
    {
      "path": ".reference-assets/typeset-forge-1.1/README_1.1.md",
      "originalName": "README(2).md",
      "libraryFileId": "file_00000000456881f6a50288e0ee7c393e",
      "sizeBytes": 16158,
      "sha256": "cd6fda7d43c1993e15f075d26fa1dc3e15b3729c37cb5a3258e1bf566099bf05",
      "byteHandling": "copied_without_content_modification",
      "executed": false
    },
    {
      "path": ".reference-assets/typeset-forge-1.1/CHANGELOG_1.1.md",
      "originalName": "CHANGELOG_1.1.md",
      "libraryFileId": "file_0000000074a4820d9b501c5982ed77d2",
      "sizeBytes": 1594,
      "sha256": "58486e13439e1593a0d07054413bbeeaadad50912d73e96cff0f9689115b3a31",
      "byteHandling": "copied_without_content_modification",
      "executed": false
    }
  ],
  "limits": [
    "Not proof of the latest file on the user Windows machine",
    "Not a redistribution license",
    "No fonts included; HTML not executed"
  ]
}
```


---

## SOURCE_FILE: `references/PUBLIC_SOURCE_LINKS.md`

# 可追溯来源入口

这些链接来自前一轮立项资料与已知精确源码路径。本次只文件整理，未重新联网验证链接内容或最新产品政策；A0使用时按任务核对。

## 精确Git身份

- Desktop集成审计锚点：https://github.com/MisakaPiano/ChatChatBar-Desktop/tree/5ef95428421f7c693b010ac88f51d470804ab07a
- Desktop活动需求参考：https://github.com/MisakaPiano/ChatChatBar-Desktop/tree/c4098e08bec84372911a5a8bc38c2cfa72f98978
- 官方已声明基线：https://github.com/SaltyFishOTL/ChatChatBar/tree/5e76a9cb841736bbbf3499a2e35e5789af4c5ca8
- 官方观察提交：https://github.com/SaltyFishOTL/ChatChatBar/commit/979cb505958dffb1b6d64a8de657434f198ffa96
- 官方图像Skill：https://github.com/SaltyFishOTL/ChatChatBar/blob/5e76a9cb841736bbbf3499a2e35e5789af4c5ca8/.agents/skills/chatbar-image-generation-runtime/SKILL.md
- 多账户需求合同：https://github.com/MisakaPiano/ChatChatBar-Desktop/blob/c4098e08bec84372911a5a8bc38c2cfa72f98978/docs/desktop/146_MULTI_API_ACCOUNT_PROFILES_CONTRACT.md

## 设计审查用官方文档

- Gradle Composite Builds：https://docs.gradle.org/current/userguide/composite_builds.html
- Android PackageInstaller：https://developer.android.com/reference/android/content/pm/PackageInstaller
- Android应用签名：https://developer.android.com/studio/publish/app-signing
- TUF安全模型：https://theupdateframework.io/docs/security/
- GitHub仓库授权说明：https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository
- NovelAI条款：https://novelai.net/terms
- NovelAI API文档入口：https://api.novelai.net/docs/
- Tailscale机器分享：https://tailscale.com/docs/features/sharing

外部文档是可变化来源，不能从这些入口推导固定“安全封号频率”、永远有效的API参数或已获服务器托管许可。


---

## SOURCE_FILE: `PACKAGING_NOTES.md`

# 文件包范围与整理差异

版本INIT-1.0；将上一条立项材料按职责拆分，并提供合并母本。此包是整理版，不是聊天逐字导出；没有新增产品实现或隐式授权。

## 相比纯聊天交付的必要调整

1. 文档已存在，A0改为“核对现成资料＋只读审计”，不再重复生成同一批文件。
2. TypeSet Forge 1.1原始HTML/README/CHANGELOG已附入被Git忽略的参考目录；README仅改包内文件名。
3. 原来待计算的三份参考SHA-256已从本轮实际取得bytes计算；Git锚点仍为上一轮有日期观察，本轮未重新核验live。
4. 另附离线校验工具和全包清单。它们只检查交付文件，不证明产品测试或运行效果。
5. 合并母本的文件引用以其所列原文件路径为准；分文件是今后维护入口，初始母本保存为有日期快照，不双重维护CURRENT。

## 本轮未做

未创建Git/远程/Project设置，未上传Google Drive或其他云端，未修改现有Desktop，未执行TypeSet HTML、下载字体或外部脚本，未调用真实付费API，未读用户图库，未创建签名密钥，未跑产品构建或验收。

## 原始包校验

BUNDLE_MANIFEST.json和SHA256SUMS.txt不包含自身以避免循环哈希。ZIP整体的SHA-256另随下载提供。离线校验脚本默认拒绝缺失/变更/额外文件；需要忽略新增文件时显式使用相应参数。开始开发后的变更不是此初始包的完整性缺陷。
