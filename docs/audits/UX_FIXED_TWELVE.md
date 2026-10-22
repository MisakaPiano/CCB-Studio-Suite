# 最新图库/编辑改动：固定 0.2.12 内部包

2026-10-22（Asia/Tokyo）。生产源码固定为 `3bea16b7405d1c1890d840e76ff3dee759faad5c`，分支 `feature/windows-rc-continuous`；启动时本地、远程一致，index/worktree/untracked 干净。本轮不改生产代码，只完成上轮界面改动的完整桌面回归、实际安装验证和长期使用版条件对账。**PRODUCT_HOLD / NOT RC** 不变。

## 当前固定包

- 内部版本：`0.2.12-internal`，包含最新图库/编辑默认折叠、连续嵌字样式和命名预设入口。
- 安装器：`H:\CCB-Studio-Suite\.local\windows-rc\fixed-twelve-3bea16b-build\studio-installer.exe`。
- 大小：167,320,552 bytes；SHA-256：`9ed09d169d926820c2ebd74ea831bcb7c742c721cab380845f55f748183514b6`。
- 自动验证安装目录：`H:\CCB-Studio-Suite\.local\windows-rc\fixed-twelve-applications\12-3bea16b7405d`。
- 本轮没有启动该应用，没有替换用户旧窗口/工作区，没有向更新源发布此包，也未外部上传安装器。此前基于 9e6c586 的旧 0.2.12 构建记录保留，不与本包混用。

## 实际执行结果

完整桌面回归 `20261022T083522084367Z-editor-clarity-desktop-full`：**115 PASS，0 FAILED / ERROR / SKIPPED**，354.616 秒，源码稳定。覆盖最新样式沿用、图库/编辑入口、APNG 合成流程、保存重开、任务/恢复/更新等已有桌面用例。实际提供者、原生接收端和用户体验不是这 115 项的验证结论。

打包 `20261022T090552192998Z-fixed-twelve-current-package-retry`：实际 createDistributable 成功，29.509 秒，源码稳定，参数 `studioPackageVersion=0.2.12`。构建测试数为 0，记录 COMMAND_SUCCESS，不冒充测试通过。使用 JDK 17.0.20、Gradle 9.1.0、离线严格依赖校验、单工作线程；安装器使用已核验 NSIS 3.13，更新运行时 323 个文件及三个更新源文件逐一核验。NSIS 的 FileDescription/LegalCopyright 两条元信息警告保留在 compile.log。

实际静默安装检查：

| 场景 | 实际结果 | 独立核验 |
|---|---|---|
| 过长路径 | 返回 19 | 在创建目标父目录前拒绝 |
| 全新独立目录安装 | 返回 0 | 全部 546 个清单文件核验，251,085,821 bytes；完成标记匹配版本和源码 |
| 再次安装到已有目录 | 返回 17 | 拒绝覆盖；546 个清单文件复核，包含安装器辅助文件在内的 549 个文件均未变化 |

安装清单 SHA-256：`a308520556979cdcd24edf45a47250a501f73d69a9fea9b8e3a1af4a068a1575`。BUILD_RECEIPT 的 installed=NOT TESTED 是打包时的历史状态，后续安装结果单列 receipt.json，不改写历史。

保留了三个未成功尝试：首次命令参数解析失败（尚未开始 Gradle）；发现另一工作区正在重构建后，仅停止身份核实的本次 Suite 打包进程，原始中断记录为 FAILED/0 tests，确认另一构建结束后重跑；首次安装核验脚本因 Python 赋值求值顺序在安装调用之前检查清单，修正后使用新证据目录重跑。最后一项不是产品安装失败，也没有把未执行的首次安装标为 PASS。

## 源码与历史回归核对

365 个构建/测试输入与固定提交逐一核对（只允许已记录的文本 CRLF/LF 差异），本轮测试快照与构建快照完全一致。core 108 文件、asset-library 12 文件、image-authority 112 文件与各自最近执行快照完全一致，分别沿用既有 129、31、99 项执行结果；**本轮没有重新执行这 259 项**。原始失败历史不因此升级为成功。

证据：[原始日志、JUnit、安装回执和散列清单](evidence/UX_FIXED_TWELVE/)。未修改官方 Desktop/权威 Prompt 与请求语义；未访问真实用户资料或凭据，本轮真实 API 调用 0。

## 发布边界与下一步

安装包仍使用隔离内部信任根，expires=`2026-10-22T23:18:42Z`，更新地址只指向 `127.0.0.1:53387`。没有为本包发布更新元数据，没有创建正式发行密钥。这不是长期更新渠道，不能作为首个长期使用版对外发布。

当前固定包的启动/手动体验、实际 A→B 更新及 APNG/多文件接收端均 **NOT TESTED**；较早包的人工截图和更新回执仅保留原有适用范围。用户要求暂缓新人工测试继续有效，本轮未索取截图或要求保留窗口。

下一动作：按 [首个长期使用测试版条件](../rc/FIRST_DAILY_USE_READINESS.md)准备可审阅的长期更新渠道实施方案；集中 W1–W5 人工验收仍暂缓。此检查点不授权 merge、发布、真实资料迁移或正式签名密钥创建。
