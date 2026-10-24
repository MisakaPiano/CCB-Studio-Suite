# Project 独立复审原文（参考证据，非新增授权）

来源线程：6adcaaef-3fac-83e8-ae57-2a524281e049；原文消息：bfc44170-d0e9-5381-8f11-060bc4c06683；针对源码 da2ae810 / 证据 1d2f4ff9。后续修复的独立判断仍待复审。历史清理是否执行以用户在本线程的直接授权为准，此原文的只读限制不覆盖后续人工授权。

## 本轮结论

**六项中，第 2、5、6 项可在限定范围内关闭；第 1、3、4 项已有实质修复，但仍各有一个明确残余。新增更新清理机制存在两项必须修复的问题，暂不能整体接受；新建测试临时根的清理机制可限定接受。**

这些判断仅适用于最终源码 **`da2ae810542c79b0049b74148566a423fb3e1bc3`**。公开 Preview115 仍指向 `ff145d0`，旧复审结论保留；**PRODUCT_HOLD / NOT RC 不变。**

## 1. 固定证据核验与五种状态

本轮实际读取了差异、固定映射、原始 ZIP、逐例日志和相关实现，并进行了隔离合成的函数级复现。

- **归档完整性通过：**原始 ZIP 为 **1,049,247 字节**，SHA-256 为 `e14b0d97ec722aac9f31d903f6012625eb97c4a5e5bb215401e58de741f00e0e`；308 份证据及清单全部对应，CRC、大小、散列均吻合。
- **源码映射成立：**408 项应用输入在 `28d09f52` 与最终 `da2ae810` 间一致；两提交的实际差异仅涉及五个 Python 文件。构建和测试输入已按 Git 字节、Windows 换行差异分别核对。
- **旧身份保持：**旧 `2489844` 证据目录及 RAW ZIP 字节未变；远端 tag、Release ID、预发布标记和公开115安装包身份也未变。

依据：[固定映射](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0/docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/FIXED_SOURCE_PACKAGE_MAP.json) · [原始证据包](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0/docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/RAW_EVIDENCE.zip) · [运行索引](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0/docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/RUN_INDEX.json)

| 状态 | 本轮判断 |
|---|---|
| **实现** | 六项均有真实代码变化；其中三项仍未完整满足合同。清理机制已接线，但存在下述决策缺陷。 |
| **自动测试** | 最终 **8 份 XML、38 个唯一 JVM 用例**；另 **49 个唯一 Python 用例**，各自失败／错误／跳过均为 0。原始记录可信，但未覆盖本轮发现的遗漏场景。 |
| **用户手动** | 没有新增115人工确认。命名归档、组织树、任务栏继续 **PENDING**；既有人工确认保持原范围。 |
| **独立审查接受** | 接受上述证据完整性与输入映射；限定关闭第2、5、6项，限定接受新测试临时根清理。其余见下文。 |
| **仍未验证** | 内部 EXE 的本轮独立字节复算、原封115二进制作为 A 的完整升级、真人完整 GUI 更新、真实服务、完整灾难矩阵及长时资源稳定，仍为 **UNKNOWN／未验**。 |

内部 EXE 未随证据提供，因此 **168,923,897 字节／`8b33fd65…49deff`** 目前可追溯到构建回执及签名目标，本轮无法亲自重算其包散列；这不阻塞源码级判断。

### 原生更新证据的限定接受

我核对了真实 A 的 PID／image-bound 路径、生产维护调用、helper `COMPLETE`、相同 nonce，以及 **3 项资产、1 项合成 DPAPI 凭据指纹**的 A/B 快照匹配；B 正常关闭回执也存在。临时 TUF 四角色签名、元数据长度／散列关系及随包配置中的根散列可独立验证。

但最终状态仍须写成：

> **helper COMPLETE；最终 driver FAILED（写证据时 WinError32）；事后8项断言 PASS。**

A 是 test-only main 驱动生产路径，早期 driver PASS 的临时签名材料不完整，均不扩张成普通用户完整升级通过。[最终 driver 原始失败](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0/docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/native/ab-2f024ed824e94816943a7190b320f279/NATIVE_UPDATE_SUMMARY.json) · [事后断言](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0/docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/native/ab-2f024ed824e94816943a7190b320f279/FINAL_NATIVE_POSTCHECK.json)

## 2. 六项关闭情况

| 原问题 | 本轮裁决 |
|---|---|
| **1．恢复副本抢默认入口** | **原恢复路径已修，入口提交链暂不全关。** secondary 标记、独立 runtime、桌面端归属检查有效；Python 更新 helper 的锁内提交仍缺版本和归属复核。 |
| **2．发送前暂停／结束** | **限定关闭。** 状态检查与预留在 workspace 锁内完成；HTTP 发送许可拒绝停止后的请求，未发送任务记录 `NOT_SENT` 并退回预留。原始测试明确验证暂停、结束时生成传输计数为 **0**。[实现与发送前检查](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/BatchGeneration.kt#L130-L149) |
| **3．关闭／更新收完在途结果** | **主生成排水可接受，暂不全关。** 批次及手动主生成已改为先关门、再等待并保存慢响应；但手动 VIBE 前置编码未接入发送门。 |
| **4．排队账户及 secretRef 冻结** | **普通编辑路径已修，暂不关闭。** 快照能持久化、重开并用于派发；仅保存节奏设置仍会刷新该快照。 |
| **5．Prompt 中断重复入库** | **限定关闭已识别的提交窗口。** 按来源、内容散列、父版本接管已提交资产，修复标签和观察记录；歧义明确报 `RECOVERY_REQUIRED`，不盲目再建。[恢复逻辑](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryRepository.kt#L193-L240) |
| **6．手动累计／归档容量** | **限定关闭。** 新请求在引导准备前检查1000上限；旧1001项状态能分成1000＋1归档并保留引用。仍受已有40组和文档体积上限约束。[容量与归档实现](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationOrganization.kt#L29-L44) |

## 3. 尚需修复的问题

以下按**本轮残余影响**定级。函数级复现使用最终源码原函数；Windows 锁、句柄删除或快捷方式边界使用隔离适配，不能计作 Windows 端到端重跑，也不加入38／49的计数。

### P1｜首次接管当前程序时，未知卸载器被错误登记，后续升级会删除它

`residue_cleanup.py:164–170` 接管没有所属收据的当前 B 时，仅核验旧 journal 固定的 manifest，却调用未带 `pinned_manifest` 的登记路径；于是 `66–76` 将当前 `Uninstall.exe` 的任意内容登记成刚由可信安装器产生的文件。

**独立合成复现：**B 的卸载器换成未知81字节文件，旧 manifest／journal 不变；首次清理将其登记，随后 B→C COMPLETE 后，该未知文件被实际选中删除。数据哨兵和当前 C 保留。

前驱接管路径已有正确保护，但**“当前 B 首次接管→下一轮成为前驱”**漏掉了同样保护。这直接违反未知／修改文件保留要求，阻塞新增更新清理机制通过。

位置：[首次接管路径](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/tools/update-runtime/residue_cleanup.py#L164-L180)、[卸载器登记条件](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/tools/update-runtime/residue_cleanup.py#L66-L76)。

### P2｜历史 COMPLETE 会释放新的待安装下载，甚至已被交接引用的安装器

找不到 C 对应的 COMPLETE 时，清理代码只检查值为0的 `FAILED_SECONDS`，没有区分已验证待安装下载与失败输出；`blocked` 也只保护程序目录。

**独立合成复现：**已有 A→B COMPLETE，C 已验证下载但尚未安装，清理删除 C 的安装器及 `VERIFIED.json`；增加 B→C `READY_TO_DRAIN` 后，仍删除这两个文件。

影响是新更新失败、需要重新下载，未证明原图或凭据丢失。应按**该下载所属目标及交接状态**决定退役，并保护所有 pending／active handoff 引用的安装器；无需恢复用户已否决的七天或额外冷启动等待。

位置：[residue_cleanup.py:200–211](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/tools/update-runtime/residue_cleanup.py#L200-L211)。

### P2｜第1项残余：更新 helper 加锁后仍无条件覆盖默认入口

`create_launcher()` 加了共享锁，但锁内 `_create_launcher_locked()` 没有读取、比较已有版本及资料根，直接覆盖描述符。

**原函数合成复现：**已有版本3被候选版本2覆盖；同版本下，原资料根也能被另一个根覆盖。这里只证明提交函数缺少防线，**没有声称发生真实 TUF 降级**。

应在锁内对交接所期望的旧入口、版本和归属作条件提交，同时允许经过确认的资料迁移目标。当前跨进程探针只覆盖 Kotlin 写入端。

位置：[apply_update.py:24–48](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/tools/update-runtime/apply_update.py#L24-L48)。

### P2｜第3项残余：手动 VIBE 准备仍可在维护开始后发送新请求

静态调用链确认：手动生成先执行 `guidance.prepare()`，之后才检查 `generationSends.open`；VIBE 准备逐项执行原始 admission 和 `/ai/encode-vibe` 请求，没有维护发送许可。

因此，两条未缓存参考中，第一条编码在途时开始维护，收到第一条后仍可能发送第二条。批次首版禁止参考引导，这个残余**限定于手动 VIBE 前置请求**，不否认已验证的主生成排水。

应让每次编码出站也经过维护许可；已许可的请求收完，后续请求停止。补一个“两条未缓存 VIBE、首条慢响应期间维护”的合成用例即可。

位置：[StudioApplication.kt:310–318](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioApplication.kt#L310-L318)、[DesktopNovelAiGuidance.kt:89–98](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-desktop/src/main/kotlin/com/example/chatbar/desktop/DesktopNovelAiGuidance.kt#L89-L98)。

### P2｜第4项残余：仅保存节奏也会重新绑定账户快照

`configure()` 每次都从当前账户表重写 `accountSnapshot`；“节奏、账户与额度”的保存按钮无论是否更换账户都调用它。

可达流程是：队列冻结旧 ref A→同 ID 档案改成 ref B→暂停后仅修改间隔或原样保存设置→队列改用 B。已有测试只覆盖编辑档案后直接派发／重开，未覆盖这一步。

同账户的节奏保存应保留原快照；明确更换账户版本应走独立、可识别的动作。

位置：[BatchGeneration.kt:84–88](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/BatchGeneration.kt#L84-L88)、[BatchWorkspace.kt:285–297](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/BatchWorkspace.kt#L285-L297)。

## 4. 清理范围、补证与下一步

**新测试临时根清理可限定接受。** 新建归属根、外部证据封存、大小／散列复核、失败现场及未知内容保留的规则已接入。新清理回执与历史69个位置没有交集；69项仍是只读待处理清单，部分容量统计不完整，不能称已回收。原活动备份、固定107／114／115及旧复审包继续保留。[工作区生命周期实现](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/tools/workspace_artifacts.py#L48-L128) · [历史清单](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0/docs/audits/HISTORICAL_CLEANUP_REVIEW_20261025.md)

需要补证或校正文案的两点：

- **冻结旧凭据的升级覆盖：**快照目前只遍历 `state.accounts`，没有覆盖仅由 `batch.accountSnapshot` 引用的旧 `secretRef`。用两个合成凭据验证“队列冻结A、当前档案改B”，并确认目标缺失A时会拒绝验证。现有单凭据演练不能覆盖这个形态；尚未证明实际发生丢钥。[快照枚举位置](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/StudioUpdateHandoff.kt#L33-L49)
- **Prompt 测试表述收窄：**新增故障注入实际发生在第二版本，不能写成首次导入与新版本两组都已执行。可以修正文案；不必为增加计数重跑历史测试。[实际用例](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/da2ae810542c79b0049b74148566a423fb3e1bc3/modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/ReviewBoundaryTest.kt#L36-L50)

driver 写证据的 sharing violation 可以后续修好并取得完整成功回执；**原 FAILED 永久保留，现有源码问题不能归咎于这次 driver 失败。**

下一步应是上述五个残余的定向修复和最小合成回归。已有107更新、采集、导入、图库及设计生成等限定人工确认无需重做；115体验等待用户反馈，Prompt对比继续 PENDING。历史69处是否删除另待用户对准确清单决定。

### 可直接交回 Codex

> 以 da2ae810／证据1d2f4ff9继续小范围返工：①当前程序首次接管采用旧安装的固定 manifest 证明，禁止追认无来源哈希的 Uninstall.exe；②下载清理绑定同一目标及交接状态，保护全部未完成交接引用，保留 COMPLETE 后立即退役政策；③Python helper 在共享锁内复核期望旧入口、版本与归属；④手动 VIBE 编码接入维护发送许可，已许可响应收完、后续零发送；⑤同账户仅保存节奏时保留冻结快照，并补冻结旧 secretRef 的升级验证。分别补遗漏场景的隔离回归，提交新固定输入与原始逐例结果；不重开 UI、不累加重复测试、不重做已有人工、不删除历史69处、不自动发版。保留原 driver FAILED、用户否决及所有 PENDING／UNKNOWN。

本轮未修改项目源码、日常资料、治理记录或信任链，未调用真实服务或付费 API，未发布新版本。

