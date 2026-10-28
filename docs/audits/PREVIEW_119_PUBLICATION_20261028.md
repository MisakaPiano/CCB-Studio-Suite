# Preview119：编辑改善阶段收尾、公开更新链与材料交接

2026-10-28 Asia/Tokyo。状态 **PUBLISHED_PUBLIC_CHAIN_VERIFIED_EDITOR_STAGE_CLOSED_WITH_SCOPE**。用户明确确认本轮三项通过，并要求收尾发布；不冒充稳定版、全项目验收或Project独立复审通过。日常安装仍未由执行者更新。

## 固定对应关系

| 对象 | 固定身份 |
| --- | --- |
| Pre-release | [v0.0.0-preview.119](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.119)，Release ID 426328218，发布时间 2026-10-28T06:29:48Z |
| 应用映像输入 | [e012e2e098e20d40de97688cae376978840c3575](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/e012e2e098e20d40de97688cae376978840c3575) |
| 来源记录／最终组装与tag | [d9be9d9f90cd305c71749933a0b2c4f2929b06a3](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/d9be9d9f90cd305c71749933a0b2c4f2929b06a3)；模型与编辑早期本地快照03f589c／9e3b182随祖先正常推送，不把旧“未验收”历史改写 |
| 安装包 | CCB-Studio-Suite-0.0.0-preview.119-windows-x64.exe，264,116,085 bytes |
| SHA-256 | **b425ae408e3613acac2a0000ead5f04cd02c9831a63d4b288682f5e2f75f1c83** |
| 更新元数据 | [9b86ac3f89c43dc9a5f3aa819ee2480e4bc6a5ac](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/9b86ac3f89c43dc9a5f3aa819ee2480e4bc6a5ac)，targets18／snapshot28／timestamp28；原root1与旧桥接不改 |
| 根SHA-256 | 408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a |
| main首页 | [c156170413ccee19398a5bae024ddb06ea96962c](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/c156170413ccee19398a5bae024ddb06ea96962c)，仅README.md，旧正文分界线后字节保留 |

四个发行附件为安装器、RELEASE_NOTES.md、THIRD_PARTY_NOTICES.zip、SHA256SUMS.txt；ID／大小／散列见[RELEASE_STATE](evidence/PREVIEW_119_PUBLICATION_20261028/RELEASE_STATE.json)。完整包公开流式下载核对通过后才签发现有三角色元数据；无根密钥创建／轮换／导出，不替换旧附件或force推送。Windows Authenticode发布者签名仍未提供。

## 实际纳入与限定人工结果

- 本轮编辑操作、三栏与队列／图层、连续工具设置、竖排、命名预设、快捷键和多种成品导出沿已批准方案收尾；性能只保留既有局部改进，不声称全机流畅验收。
- 工程栏去重，标题占中间剩余宽度，废弃完整文案及默认红色垃圾桶在最右导出左侧。收起保存并清空；归档沿用同一工程与原图、保留历史；废弃必须确认并回收可证明独占进度，生成文件另选确认，依赖原图／共享内容／已有备份保留。
- 新完整包导入不再把整包额外存成资产，只保存原图及支持的当前编辑内容；外来历史／未知字段不保留，确认摘要明确提示。外部包及已有导入资料不清理；旧源的读取／另存兼容保留。完整导出包含原图与当前内容，不含本机撤销历史、字体文件、模型、凭据和组织树，仍限64MiB。
- 默认本地AI权重实际内置，47,600,269 bytes，SHA a12ac5532e93be9dfeb96a77fc3f3647791335c9df0de9a18fcd503f7877a828。固定作者模型卡MIT声明与许可／来源随包保存，不把另一上游应用的许可UNKNOWN变成已授权。工作区不按工程复制权重；自选模型保留。
- 编辑预览和共用图像浏览宿主改独立原生窗口，最大化／还原／关闭隔离，标题栏跟随主题。

用户本次明确通过：**收起／本机列表重开保留进度；归档／历史重开；完整包导出再导入继续编辑**。前次已单独确认预览最大化／还原／关闭。其他此前确认按原记录保留，不外推资产管理、全部编辑能力、115整体体验、Prompt或独立审查。[本次原话与固定候选](evidence/PREVIEW_119_PUBLICATION_20261028/USER_EDITOR_ACCEPTANCE.json)。

## 自动与包级验证，分层不累加

1. 本轮工程栏／新导入有13唯一JUnit分阶段PASS证据；重复不累计。[逐例索引](evidence/PREVIEW_119_PUBLICATION_20261028/editor-review/REVIEW_VALIDATION_INDEX.json)保留全部7次流程状态。核心9例所处流程因桌面旧调用编译失败仍是FAILED；后修复的桌面结果分列，不篡改原失败。首轮比较重新生成对象ID的测试错误及原失败源、封面挤掉入口的截图与用户标题否决均保留。
2. 内置模型历史6唯一例、窗口原生／相关回归沿固定证据复用，不计成本轮新增13例。模型真实权重处理合成图只证明管线，非准确率验收；upstream观察器另有3个合成用例，不计编辑功能用例。
3. 实际createDistributable：应用源码e012e2e，离线严格依赖／单worker／低优先级／JDK17，sourceStable=true；这是构建成功，JUnit0，不称测试通过。
4. 119真实NSIS隔离安装返回0；551项大小／SHA逐项通过。安装后生产JAR类路径加载模型，resourceProtocol=jar、权重长度／散列及7类别通过；没有从外部模型路径偷取。三份应用JAR与用户刚试用候选完全相同，[等价映射](evidence/PREVIEW_119_PUBLICATION_20261028/APPLICATION_EQUIVALENCE.json)。
5. 公开118→119原生链如下，不用之前117→118成功替代，也没有再次要求已确认编辑人工项目。

## 公开118→119：新增一条真实隔离链

环境：本机Windows x64、同Windows账户、H盘NTFS，专用隔离HOME／USERPROFILE／APPDATA／TEMP；全为两张合成原图与两个账户引用的DPAPI合成凭据。旧118安装器仍为3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d。

- 原118生产Main从默认入口启动、正常关闭，取合成资料／凭据快照。
- **原版118包内客户端**从公开long-term-metadata正式签名源发现并实际下载119，返回DOWNLOADED_VERIFIED；原root、版本／身份及安装器实际字节核对，无候选发现适配。
- 原118 helper绑定真实父进程、READY_TO_DRAIN、正常关闭旧窗口、真实NSIS安装119，119生产Main完成程序／资料／凭据ACK，回执COMPLETE。
- 119仍运行时旧118根消失、downloads与installing为0；备份哨兵、两张原图与两项合成凭据保持。实际清理一轮removed555／deferred0；随后重复清理removed0，成功交接回执字节未变。
- 新版正常关闭后核对合成原图／凭据，默认入口重开119并正常关闭；119包内公开检查UP_TO_DATE。没有强杀或修改日常安装。

[原始结果](evidence/PREVIEW_119_PUBLICATION_20261028/public-native/RESULT.json)、[原118公开命令](evidence/PREVIEW_119_PUBLICATION_20261028/public-native/OLD118_PUBLIC_COMMAND.json)、[交接回执](evidence/PREVIEW_119_PUBLICATION_20261028/public-native/RAW_UPDATE_RESULT.json)、[清理事件](evidence/PREVIEW_119_PUBLICATION_20261028/public-native/cleanup-events.jsonl)。旧GUI交接由隔离驱动衔接，未自动点击GUI更新按钮，不称日常GUI已升级。

## 清理、来源与保留

本轮完成后，3份自行创建的包构建／隔离安装／升级fixture先留证、验证固定119安装包另行保留，再按原owner＋逐文件manifest退役：2128项／1372828468 bytes（约1.28GiB）。未知删除0；未碰旧失败、固定基线、用户本地候选试用根、已有导入、日常安装、真实资料／凭据／插件／备份。[清理回执](evidence/PREVIEW_119_PUBLICATION_20261028/OWNED_FIXTURE_RETIREMENT.json)。固定119包留在仓库.local/preview-publication-119-20261028/delivery，仅一份保留物，不把安装包当临时垃圾。

更新器6脚本、安装器生成器与Launcher.cs相对118源码a1202e2未变化；新公开链验证其在更大模型包上的实际使用，不是又重写了清理或信任协议。原111历史人工退役及Desktop报告仍按原事实保留。本次源码冻结准备脚本参数错误、探针生成引号错误和无状态影响的占位命令拼写错误分列PREPARATION_CORRECTIONS；均未改产品结果或旧失败。

## 未完成与下一步

本轮编辑改善阶段收尾；整体编辑覆盖／资产管理完整人工、115体验／任务栏固定、Prompt对比、q09旧像素断言根因UNKNOWN、更多环境／网络盘／跨账户／跨机器凭据及长期CPU/RSS不随之完成。本轮独立审查NOT REQUESTED，旧Project结论只按其原范围保留。

下一步先迁移新对话，承接既有PERF-001～007基线确认新的性能范围；资产文件管理与真正删除、更多图像能力、Tag库按既有任务排队，Prompt继续PENDING。详见[新对话交接](../../handoff/STUDIO_NEXT_CONVERSATION_20261028.md)。日常118可由用户正常更新页升级；本次没有自动更新或读取其资料。
