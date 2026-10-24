# Project下载清理P2限定关闭回执

2026-10-25 Asia/Tokyo；实际读取原Project“阶段复审结论”，thread `6adcaaef-3fac-83e8-ae57-2a524281e049`，turn `4df90e87-d453-4bc3-ab7c-36635b3a96f1`，message `44d32b47-d2fd-55d9-8838-517d63b92228`。下文为独立审查原结论，非Codex自检；固定源码f6598d3、证据54a5ba26，旧包不含修复。

## 裁定：该 P2 可以限定关闭

**`CLOSED_WITH_SCOPE`：仅接受源码 `f6598d39298c06ec5644a131e484e814f77b6f45` 的“下载引用终态分类”修复，证据锚点为 `54a5ba26e2ba8a63b40a074daace5495e10e1546`。**

本轮未发现需要继续返工的残余，**源码级关闭所需的固定证据已经足够**。该结论不覆盖旧内部 EXE、公开 115、完整升级链或全项目验收。

### 为什么可以关闭

我检查了实际差异、生产调用顺序、历史 helper、测试夹具与原始日志，并独立复现了上轮反例。

1. **核验顺序已经修正。**  
   `verified_completion` 先检查 nonce、完成标记、绝对路径、journal 所属缓存及可读性、安装状态、目标身份与 manifest 验证记录。不能证明终态的下载引用先加入保留集合，**之后才筛选程序根**，非默认来源不再绕过保护。`FAILED_RETAINED` 也必须匹配本 plan 的 nonce。[源码 109–166 行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/f6598d39298c06ec5644a131e484e814f77b6f45/tools/update-runtime/residue_cleanup.py#L109-L166)

2. **上轮反例已消失，合法终态仍可立即清理。**  
   独立合成探针直接执行冻结源码中的决策函数，安装器实字节与登记散列一致：

   | 情形 | 本轮结果 |
   |---|---|
   | 待排水、仅有 COMPLETE 标记 | 保留两个下载文件 |
   | 完成标记齐全，但缺 installed／journal | **保留** |
   | 完成标记齐全，但 journal 不存在 | **保留** |
   | FAILED_RETAINED 的 nonce 错误 | **保留** |
   | 完整合法 COMPLETE，或 nonce 正确的失败终态 | 在既有匹配成功证明下，立即清理对应两个下载文件 |

   七个情形的资料哨兵均未改变，重复执行删除数均为零。探针仅将 Windows 锁及删除原语适配到隔离临时目录，**没有冒充新的 Windows 原生执行**。

3. **补齐合法夹具字段有历史生产依据。**  
   我核对了 `ff145d0`：旧安装 helper 确实在安装验证、目录切换后写入 `INSTALLED_NOT_STARTED`。本次要求该字段明确存在，与历史协议一致；新增测试同时验证错误状态和缺状态必须保留，并非放宽断言来获得通过。[旧 helper](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/ff145d051bae286ab3e27cadd41195f8bd4c4e18/tools/update-runtime/install_ops.py#L87-L91)／[新增测试](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/f6598d39298c06ec5644a131e484e814f77b6f45/tools/update-runtime/test_residue_cleanup.py#L203-L241)

### 固定证据核对结果

- **ZIP 完整性通过：**实测 23,147 字节，SHA-256 与所给 `2e6b91f2…c85c96` 一致；18 条清单文件加 manifest，共 19 个成员。CRC、逐项大小／散列、解压字节与固定 Git 原字节全部一致，三份旧 ZIP 未变。
- **最终唯一计数成立：**权威运行 `final-20261024T173217359714Z` 为 **24 个方法 PASS，失败／错误／跳过均为 0**，24 个资料哨兵一致；五项运行输入的 Git／Windows 磁盘散列均对应固定源码。方法内子场景、重复轮次及此前 18 JVM／56 Python 均不追加计数。[原始日志](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/54a5ba26e2ba8a63b40a074daace5495e10e1546/docs/audits/evidence/DOWNLOAD_TERMINAL_20261025/runs/final-20261024T173217359714Z/tests.log)／[输入映射](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/54a5ba26e2ba8a63b40a074daace5495e10e1546/docs/audits/evidence/DOWNLOAD_TERMINAL_20261025/GIT_INPUT_MAPPING.json)
- **失败保留成立：**旧实现为 5 个方法中 4 失败、1 通过，失败夹具记录为保留、删除零。一个非阻塞措辞更正：严格按 unittest 结构，所谓“13 个失败子场景”是 **13 条失败记录＝12 个 subtest＋1 个顶层方法断言失败**；不影响复现和关闭结论，无需改包或重跑。
- **成功测试清理范围相符：**记录为本轮独立临时根的 636 文件／277,202 字节，没有新增历史目录清理。此项是源码与回执核对，未在真实 Windows 文件系统重新检查现场。[证据与产物映射](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/54a5ba26e2ba8a63b40a074daace5495e10e1546/docs/audits/evidence/DOWNLOAD_TERMINAL_20261025/SOURCE_AND_ARTIFACT_MAP.json)

### 接受范围与尚缺证据

| 层次 | 本轮裁定 |
|---|---|
| 实现 | 此 P2 已修复 |
| 自动测试 | 接受固定源码下的 24 项定向执行记录 |
| 独立审查 | **此 P2 限定关闭** |
| 用户手动验收 | 本轮没有新增，不要求重复既有人工 |
| 安装包集成／原生运行 | **本次未打包、未验证** |

**旧 `2484858` 内部 EXE，SHA-256 `2dc31cb8…701238`，不包含本次修复，不能重新标成 `f6598d3` 或“P2 已修复的安装包”。**本次没有新包，不应将新包验证记为 PASS。

若以后决定交付包含修复的安装包，再补三项固定证据即可：**新包字节与 SHA、源码／运行时输入映射、从实际新包执行相关清理路径的隔离验证**。这些是扩大到安装包接受范围时的条件，**不是现在关闭源码 P2 前必须补跑的门槛**；无需因此重做 JVM、UI、既有人工或完整原生演练。

`e4d3d5c` 经比较确实只改五份任务／状态文档，未改上述源码或证据，后续 PERF、资产管理和编辑待办不纳入本轮。此前限定关闭事项继续保留原范围；**PRODUCT_HOLD / NOT RC、q09 FAILED／根因 UNKNOWN、115 体验及 Prompt 比较各自 PENDING 均保持。**

本项不需要再发返工任务；可将以上限定关闭结论交回原 Project，当前继续遵守“先不发新版本”。本轮未写回治理记录，也未启动后续开发。

