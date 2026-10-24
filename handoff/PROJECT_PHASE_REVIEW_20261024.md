# CCB Studio Suite：邀请 Preview 阶段独立复审交接

日期：2026-10-24，Asia/Tokyo。交接者为执行 Codex；**这是给原 Project 的证据与问题清单，不是新的独立审查结论，也不宣布全项目验收完成。** 本轮只处理下载入口和文档交接，不构建、不发新版本、不改更新协议/信任或读取用户资料。

## 1. 最后可核实的独立审查基线

最后可回读的 Project 审查是 [Issue #4 / comment 6319270550](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/4#issuecomment-6319270550)，2026-10-21 15:04:26 UTC（JST 2026-10-22 00:04:26），固定源码：

`678caf1ab1e8e9611e19a35e8d7c2f27d11b074f`

结论为 **UX_CREATION_LAYOUT_CHECKPOINT: PASS_WITH_SCOPE**，仅接受创作页工程修正；总体仍为 **PRODUCT_HOLD / NOT RC**，不能把此前 USER_REVIEW_FAILED 改写为真人通过。Project 当时独立读取17份 JUnit XML，核对64项执行结果与记录，并检查部分离屏布局图；没有重新运行 Windows 构建，也没有签收该提交的新安装包、完整功能或实机交互。

同日稍早的 [6319199842](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/4#issuecomment-6319199842)审查的是同一个检查点。更早的 [UX-R0 审查](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/4#issuecomment-6307947815)接受的是恢复合同；[CP1 审查](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2#issuecomment-6298794831)只覆盖当时的资料维护/信任基础，不能累加成115验收。

本轮实际读取主库所有4个 Issue 及其全部15条评论，没有找到上述时间之后的 Project 独立裁决。这里核实的是**公开 GitHub 的 Project 审查记录**，原 Project 聊天未读取，作者归属不能由本轮自检重新认证。最后一次全项目产品/RC 独立验收通过记录：**未找到，UNKNOWN，不能记 PASS**。[原文、作者、时间及正文散列](../docs/audits/evidence/PROJECT_PHASE_20261024/PROJECT_BASELINE.json)已保留。

## 2. 本次评审冻结身份

| 对象 | 固定身份 |
|---|---|
| 应用源码及实际组装输入 | `ff145d051bae286ab3e27cadd41195f8bd4c4e18` |
| 对应公开 tag | [`v0.0.0-preview.115`](https://github.com/MisakaPiano/CCB-Studio-Suite/tree/v0.0.0-preview.115)，Git ref 直接指向上述提交 |
| 公开 release | [Preview115](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.115)，ID 422739522，非 draft，`prerelease=true` |
| Windows 安装包 | `CCB-Studio-Suite-0.0.0-preview.115-windows-x64.exe`，168890283 bytes |
| 安装包 SHA-256 | `356e749bd346af4a1d419a0d8b48b1e27d2314e6fce5763fa0933917247862dc` |
| 安装清单 | 548份随包文件；551份实际安装文件（含清单、完成标记和卸载程序） |
| 115交付审计 checkpoint | `4c0f35deaba980cbc068b8ac3968bc730a952f8d`；不是另一份应用构建 |
| 资料 reader | 3；与应用发行版本号是两回事 |

已将 GitHub Release API 返回的安装包 digest/大小，与既有安装器逐字节散列及 [115构建/安装回执](../docs/audits/evidence/ORGANIZATION_SESSION_115/BUILD_RECEIPT.json)对照一致。本轮未执行安装器或修改它。其余附件是 RELEASE_NOTES、SHA256SUMS、THIRD_PARTY_NOTICES；不是把 Source code ZIP 当作安装包。[机器冻结映射](../docs/audits/evidence/PROJECT_PHASE_20261024/FROZEN_IDENTITY.json)。

相对最后 Project 基线，是 **95 commits / 3424 changed paths，其中181条 modules 路径**；绝大部分其余路径为文档和证据。[完整路径清单](../docs/audits/evidence/PROJECT_PHASE_20261024/DIFF_SCOPE.json)与 [GitHub差异入口](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/678caf1ab1e8e9611e19a35e8d7c2f27d11b074f...ff145d051bae286ab3e27cadd41195f8bd4c4e18)。网页可能截断大 diff，独立审查应按清单选择文件，或在只读检出中使用同一对 SHA 比较。

`docs/SOURCE_LOCK.json` 是历史来源观察，不是当前全部生产依赖锁。实际图像权威边界见固定源码中的 [SOURCE_MANIFEST](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ff145d051bae286ab3e27cadd41195f8bd4c4e18/modules/image-authority/SOURCE_MANIFEST.json)，固定 Desktop `5ef95428421f7c693b010ac88f51d470804ab07a`；呈现来源另有 `37c67c7` 等记录，不能混为业务权威升级。相对678基线新增两份同源索引构建/目录更新辅助文件及清单，不能宣称整个目录零 diff。沿用 Project 的 EOL 更正：Git blob 原字节、工作树字节与 LF/CRLF 规范化文本相等须分别陈述。

## 3. 此后主要变化（按能力归并）

- **实际工作流与图库**：接入账户/模型职责、设计应用与生成、引导/角色、图像参数回用，编辑/排版/小人书/导出以及备份恢复入口；图库选择、相册、标签、回收站和工具队列逐轮修正。接线存在不等于所有源功能、所有 Provider 或真人流程都已验收。原有 [75动作映射及工作流合同](../docs/ux/UX_R0_ACTION_TRACE.md)继续有效。
- **资料与目录采集**：原件、编辑配方、工程、导出、任务、缓存和秘密分离；外部目录是明确登记后入库保留原件，变动保留新版本，来源失效不删除原件。恢复不自动激活旧目录绑定。修复整批采集锁阻塞、导入重复全库校验和合法大资产恢复上限混用。
- **公开发布和更新**：从内部 A/B 测试演进到主库实验邀请 Preview；旧地址桥接保留。新目录安装、B校验后提交入口、启动检测和持续更新提示接入；修复真实107→109未切换和旧快照编码/默认字段兼容问题。115加入固定 Windows 启动器及桌面/开始菜单入口命令。
- **批次任务与组织**：冻结分组参数、串行请求、持久随机等待/休息、429整批人工恢复、正常暂停/结束均收完已发响应、多步撤销/重做及键盘/右键入口；手动当前批次重开保留，命名归档成功后清空当前栏，自动/手动均有创建时间与上级 Project/Chapter。图库批次→分组→图片及虚拟相册只改引用，来源保留；历史自动批次以暂停副本重开。
- **资源控制**：缩略图限额/合并解码与取消排水、万项元数据计算、大图单次读取和批量像素转换；仅有指定合成规模及短时原生基线，不外推成小时级稳定、全程序倍数或整机RSS下降。

## 4. 五种状态分开记录

### 实现完成（限定已接入部分）

上述 Windows 邀请 Preview 的接线与115新增组织/会话/固定入口已有源码和安装包；细项见 [115审计](../docs/audits/UX_ORGANIZATION_SESSION_115.md)、[批次语义](../docs/audits/UX_BATCH_HISTORY_112.md)、[当前能力及边界](../docs/CURRENT_STATE.md)。这不是“原计划所有功能均完整实现”的声明。

### 自动测试通过

- 固定115源码：**46项实际 JUnit PASS，0 failure/error/skipped，sourceStable=true**；包含批次、组织、实际 Compose 内容树、图库、工作台和更新快照，createDistributable 成功。不是单纯编译，也没有重跑全仓所有历史用例。
- 另有13项安装/交接 Python 检查通过；C#固定入口检查使用合成114/115目标及隐藏的自有原生测试窗。它没有验证用户实际任务栏固定。
- 551文件安装清单/重复目标拒绝、公开107模块下载115、107 helper安装、真实115进程首帧B回执、115当前检查、固定快捷方式选择115均有回执。**A进程身份/已关闭及排水在这份公开探针中是明确替身**；不能称115整条真人应用内更新都通过。
- 历史资料、恢复、目录和性能测试分别绑定各自旧提交。它们不是本轮重新执行，也不与115的46项相加充当覆盖率。

### 用户手动确认

| 已记录的确认 | 限定身份和范围 |
|---|---|
| AI设计→应用→生图、重开图和参数保留 | [回执](../docs/audits/evidence/UX_INVITATION_PREVIEW_105/HUMAN_CREATION_RECEIPT.json)；用户当时实际包身份未精确指定，不外推全Provider/账单 |
| 日常更新重启正常、资料保留，107显示最新 | [107回执](../docs/audits/evidence/PERFORMANCE_107/HUMAN_UPDATE_ACCEPTANCE.json)；不是115签收 |
| 暂停采集后正常打开、内容可见；两张合成图单入口同步及切页正常 | [cc93735回执](../docs/audits/evidence/UX_COLLECTION_RESPONSIVENESS/HUMAN_ACCEPTANCE.json)；原大目录仍暂停 |
| 导入明显改善、阶段/用时反馈及指定队列布局正常 | [fe9e821回执](../docs/audits/evidence/UX_IMPORT_QUEUE_LAYOUT/HUMAN_ACCEPTANCE.json)；没有人工精确耗时或全局资源保证 |
| 四项磁贴、拖动预览、入口和选择显示符合要求 | [59e2fe8回执](../docs/audits/evidence/UX_GALLERY_TILES_SELECTION/HUMAN_ACCEPTANCE.json)；不代表后续组织树布局通过 |
| 115命名归档、组织树、任务栏 | **PENDING**；本轮用户说“先更新到115试一下”，没有给出结果，不当作确认 |

### 独立审查通过

只承认第1节实际可回读的各份限定 Project 结论。当前115、其间全部实现及完整产品功能**没有新的 Project 独立 PASS**。上述自动测试、发布成功、用户某组操作确认都不能替代这一步。

### 仍未验证／未关闭

- 最新115真人命名/归档、组织树、原生键鼠/DPI/输入法、桌面/开始菜单及任务栏固定效果待用户本次反馈；原生测试进程 MainWindowHandle 未观察到可读主窗，不冒充实际 Java 窗口属性人工验收。
- 完整编辑→交付→恢复连贯人工、当前APNG键盘/剪贴板接收端、多Provider和真实费用边界、跨机凭据恢复与完整数据迁移/升级故障矩阵未全签收。
- 多来源 Prompt 新比较继续 **PENDING**；既有图片比较/Prompt参照不等于新方案。长期标签分类、Android、Server仍是后续范围。
- 错误重试次数/延迟及“跳过等待继续下一张”属于早先批次管理概念诉求；115实际 `BatchPolicy` 仍只有间隔/运行/休息/抖动/预算等字段，没有这些重试策略。429不得因后续设计被绕过。不能把浏览器demo当成已接入。
- 原大目录自动采集重扫、长时间/大规模真实资料、小时级CPU/内存/磁盘表现未认证；不自动恢复暂停、不读私人库补证据。
- Windows Authenticode不是TUF签名；不宣称已完成发行者代码签名。真实供应商探针和来源 TypeSet 的全面视觉等效仍须逐项审查，不能只用自身golden证明。

## 5. 重点审计路径与证据边界

| 重点 | 已有证据入口 | 独立复审需继续核对 |
|---|---|---|
| 发布/更新链 | [115完整记录](../docs/audits/UX_ORGANIZATION_SESSION_115.md)、[公开升级回执](../docs/audits/evidence/ORGANIZATION_SESSION_115/LIVE_UPDATE.json)、[早期真实GUI A/B](../docs/audits/UX_R4_ACTUAL_AB.md)、[迁移桥接](../docs/audits/UX_RELEASE_MIGRATION.md) | 早期真实GUI A/B绑1a013737且reader同为2；115公开探针A父进程为替身。坏包/取消/断网/低空间等分项测试，不等于全安装/B失败/迁移中断矩阵已完成 |
| 资料/凭据 | [CP1执行](../docs/rc/CP1_EXECUTION.md)、[107人工回执](../docs/audits/evidence/PERFORMANCE_107/HUMAN_UPDATE_ACCEPTANCE.json)、[恢复后更新快照用例](../docs/audits/evidence/ORGANIZATION_SESSION_115/TEST_CASES.json)、[本机迁移状态记录](../docs/audits/UX_LOCAL_PROFILE_AND_CLEANUP.md) | 新目录保留旧资料，不原位覆盖；正常升级保留凭据≠备份包含凭据。DPAPI同用户可读不证明跨机器可恢复；本轮不读取真实密文/明文补证据 |
| 备份/恢复 | [入口与三项定向/90项历史回归](../docs/audits/UX_LIBRARY_BACKUP.md)、[33MiB合法大资产修复/120项历史回归](../docs/audits/UX_LARGE_ASSET_RECOVERY.md) | 常规备份排除实际凭据、本机目录授权/缓存/旧导出；新目标拒绝覆盖，旧格式复制到新格式目录。OS文件选择器、实际第二窗口、完整真人恢复及115全量恢复专项未签收 |
| 批量暂停/结束 | [112语义与慢响应替代服务测试](../docs/audits/UX_BATCH_HISTORY_112.md)、[115逐项用例](../docs/audits/evidence/ORGANIZATION_SESSION_115/TEST_CASES.json) | 暂停收当前响应后可继续；结束收当前响应后不直接继续。超时/未知结果不盲重发；429整批冷却后仍需人工。实际NAI长批次、费用和服务器失联需独立判断 |
| 目录采集/导入 | [监听与恢复绑定](../docs/audits/UX_COLLECTIONS.md)、[阻塞修复](../docs/audits/UX_COLLECTION_RESPONSIVENESS.md)、[导入对照](../docs/audits/UX_IMPORT_QUEUE_LAYOUT.md) | 单次2000文件/深度32/图片32MiB等边界；监听有周期复核，不保证全扫描严格exactly-once。历史一次20秒自动发现超时唯一根因UNKNOWN保留；原大目录仍暂停 |
| 性能/资源 | [106缩略图](../docs/audits/UX_PERFORMANCE_106.md)、[107计算和原生短测](../docs/audits/UX_PERFORMANCE_107.md)、[108大图及60轮](../docs/audits/UX_PERFORMANCE_108.md) | 合成80+24图导入39.919→8.729秒；24图解码像素72→4.5MiB；万项名称排序约407→39ms但添加时间4.1→7.1ms并未变快。107暖空闲30秒约CPU78ms/工作集350MiB；108分配481→309MB是累计分配。不能外推全局或小时级稳定 |
| 当前功能/源继承 | [动作合同](../docs/ux/UX_R0_ACTION_TRACE.md)、[来源状态](../docs/audits/UX_CREATION_LAYOUT_SOURCES.json)、[115组织与会话](../docs/audits/UX_ORGANIZATION_SESSION_115.md) | UI接近不等于全部功能parity；区分来源、实际GUI闭环、合成替代、Provider实际结果和用户体验。Project自行选择重点逐项检查 |

## 6. 失败、否决和范围变更必须保留

1. 早期 P3 被用户否决，核心Studio/编辑排版不具备真实工作流，促成 UX-R0 完整恢复合同；不能用旧控制层测试绿灯冲销。[原反馈](../docs/rc/PREVIEW_USER_REVIEW_1.md)。此前推断性版权STOP被Project纠偏，不能重新以抽象风险阻断；用户直接授权事实与独立看过原授权原件仍分开。
2. 用户对330cee/后续e469样式的否决、对113组织树与demo不符的否决保留。其后修正不自动成为最新115视觉验收。[样式沿革](../docs/audits/UX_GALLERY_DEMO_PARITY.md)、[114纠正](../docs/audits/UX_GALLERY_LAYOUT_114.md)。
3. 107→109连续三次未切换是真实用户故障：109目录已装但B验证失败，后续拒绝覆盖。110增加旧打包程序写入→新原生B回执，不能只看CLI安装退出码。[修复记录](../docs/audits/UX_UPDATE_REPAIR_110.md)。
4. 旧目录阻塞、原大目录暂停、自动发现超时，以及备份33MiB合法资产恢复失败均有原记录。修复后的有限回归不抹去此前失败或未确认的大库状态。
5. 108最初通用像素转换虽变快却增加分配，未作为最终发布方案；保留对照。捕获原生画面多次超时记ENVIRONMENT_UNAVAILABLE，不写真人视觉PASS。
6. 115开发中有0测试编译失败、160核心通过但桌面未执行的整体FAILED、更新回调兼容失败、测试误定位、旧连续开关定位失败；一次进程退出没有summary记INCOMPLETE。最后46项通过只覆盖最后冻结输入。[11次原始运行索引](../docs/audits/evidence/PROJECT_PHASE_20261024/RUN_INDEX.json)。
7. 刚发布115时CDN先返回114，等待后才验证115；旧目标不计成功。H盘约32MiB曾阻止后续打包，两次精确授权清理后才推进；不是在低空间拒绝时强制绕过保护。
8. 用户后来明确优先邀请 Preview，并把附加人工和Prompt方案后置：[范围回执](../docs/audits/evidence/UX_INVITATION_PREVIEW_105/PREVIEW_SCOPE_AUTHORIZATION.json)。这解释“公开实验版已发布”与“完整产品/RC未通过”可同时成立，不反向改写此前Project裁决。

## 7. 原始日志、交接包与资料排除

本轮未重跑产品测试；只回读旧执行证据及公开状态。115最终日志、JUnit、source-before、逐项结果以及开发失败原日志已提升为可公开追溯的 [本阶段证据目录](../docs/audits/evidence/PROJECT_PHASE_20261024/)。[RAW_EVIDENCE.zip](../docs/audits/evidence/PROJECT_PHASE_20261024/RAW_EVIDENCE.zip)封存35个白名单原始文本/XML/JSON文件，附每项大小/SHA-256，CRC及全部散列核对通过；[归档身份](../docs/audits/evidence/PROJECT_PHASE_20261024/RAW_ARCHIVE_IDENTITY.json)。网页可读文本已统一换行并去除行末空白，也可能经Git的EOL归一化，**精确原始字节以ZIP内清单为准**，不混称Git blob SHA。

其他既有主题的原始 Gradle/JUnit/合成证据继续位于上表对应审计目录；不同提交的测试次数存在重叠，不相加当成115全量通过。本机`.local`中的未打包中间件不声称已在Git/Drive交付。

独立交接包只收本交接、所需审计文本、状态/身份JSON与上述合成运行证据；不含应用安装器、整个Git工作区、用户图片/Prompt/目录内容、真实账户/凭据、恢复码、签名密钥或私密附件。历史用户否决以文本引用，不把其图片重新装包。本轮不另行上传Drive、不更改共享权限或给原Project自动发消息。

## 8. 本轮发布入口文档变更

主库和旧发行库默认分支都只修改README：主库顶部链接实际最新115发布页与全部版本，明确在Assets取windows-x64.exe而非Source code，标出邀请Preview和限制；旧库顶部指向主库发布列表，保留旧正文、版本及桥接通道。首页Releases从公开HTML可核实已开启，含16条release/tag；初始卡片为客户端加载占位，不为展示取消Pre-release。[区块证据](../docs/audits/evidence/PROJECT_PHASE_20261024/RELEASES_SECTION.json)。

以后链接随原发布流程末尾由维护者同步，必须重新核对实际已发布版本，不把115写成永久latest；不使用会排除预发布的`/releases/latest`冒充最新Preview。[手动同步步骤](../docs/rc/PREVIEW_UPDATE_CONTINUITY.md#首页下载入口同步)。本轮无新自动化、无版本/tag/包/更新元数据改动；远端文档提交与不变量回读另见本任务收尾记录。

## 9. 交给原 Project 的判断任务

请以第1节限定基线为起点，独立核对第2节固定对象及之后95次提交的关键变化；优先选择发布更新链、资料/凭据、备份恢复、批次停止、目录采集和资源边界。分别判断：可采信证据、所需补测、真实缺陷、仍属规划或人工待验的事项，以及实验邀请范围能否继续。不要把执行者自检当作独立PASS，也不要因已有公开Preview就倒推完整RC已验收。

用户正在验证115，新的反馈到达前保持PENDING。请由原Project给出自己的限定阶段结论及下一阶段条件；若材料不足，明确UNKNOWN及所需证据。本交接不自动授权实现新功能、真实服务调用、改用户资料、签名/更新链变更或新发版。
