# 原Project定向返工复审交接

本次承接Project对`da2ae81 / 1d2f4ff`的限定裁决，原文保留在`docs/audits/evidence/REVIEW_RESIDUALS_20261025/PROJECT_FINDINGS.md`。原第2/5/6项已限定关闭，不借此重开整套UI或重做已确认人工。

最终源码 **248485824e1d6d2201ceedf7d14ff66e251d344d**，实际JVM构建59283a0315d182cd745c9c03c6fe3b802bf8114c，408项应用输入相同。内部包`0.2.2-internal`，168926729字节，SHA-256 `2dc31cb8badfe4ed5cb02c8a99523d81dc05f65a874aeb49ac3299048f701238`，仅本地交接包，没有新公开release/tag或日常升级。

请独立核对以下返工，而非仅复述Codex总结：

1. 当前B首次接管同前驱接管一样使用旧journal固定manifest，未知81字节Uninstall替换在B→C后保留。
2. 下载退役绑定同一目标身份/安装器散列的COMPLETE；全部未验证为终态的交接引用优先保留，含非默认来源及不完整COMPLETE。较新失败下载/解包等待后续成功，历史成功不能追溯释放。
3. Python helper共享锁内重新比较期望旧入口、版本及installed/data/runtime，在修改launcher/shortcut/descriptor之前拒绝旧/外国根写入，只允许已确认迁移。
4. 每条手动VIBE编码的admission及出站接受点纳入维护门。双未缓存参考中，第一条慢响应完成缓存后第二条零发送；维护可见标记在关闭发送门之后发布。
5. 同账户仅保存间隔/休息策略保留冻结快照；明确换账户ID才改绑。更新快照包含当前档案及冻结账户ref并集，两个合成DPAPI凭据均验证，缺旧ref拒绝。

最终定向原始结果为 **18唯一JVM、56唯一Python，分别0失败/错误/跳过**，中间重复不累加。实际原生A(test-only main调用生产维护/handoff/close)→真实NSIS/B、3项合成资产与2项DPAPI凭据匹配、COMPLETE后完整清理、未知哨兵保留、B窗口正常退出，最终driver PASS。同一包/回环目标的最终补验使用全新data/runtime/program/cache；不是原封公开115或真人GUI完整升级。

原生首轮helper COMPLETE但驱动WinError32失败原样保留：它在清理器仍保护祖先目录时写证据，重试未解决。最终驱动改为外部留证并等完整PASS_COMPLETE，没有改产品信任链或伪造原成功。上一轮driver FAILED/事后8项PASS及更早失败也保持。首轮JVM20项3失败中维护窗口/夹具已修，额外画布q09像素断言仍失败，未扩大UI范围；最终18项不代表整个UI套件绿色。

Prompt故障注入表述已收窄为第二版本两个提交窗口，没有为了增加计数重跑首次导入。完整实现说明、选择与限制见[Desktop技术报告](DESKTOP_UPDATE_TECHNICAL_HANDOFF_20261025.md)和[运行索引](../docs/audits/evidence/REVIEW_RESIDUALS_20261025/RUN_INDEX.json)，固定包映射、原始ZIP、manifest及本地签名/包散列核验均在同证据目录。二进制只在本地，云端独立包重算仍未验。

历史清理获得用户后续直接授权，**不再是前次交接的只读阶段**。本次仅处理20个确认根，236530文件/8506611341字节（7.92GiB）；1086项备份/证据等保留项仍在，115程序与入口锚点复核，受活动记录保护的111、固定107/114/115、复审证据和私人资料/备份保留。没有调用新自动清理器升级日常应用。准确路径/逐文件计划和删除日志见[人工清理回执](../docs/audits/HISTORICAL_CLEANUP_EXECUTED_20261025.md)。其余69项旧调查中未取得确定归属者及早期失败现场未被一键清空。

请分别裁定实现、自动测试、用户手动、独立审查接受与仍未验证。新源码独立审查PENDING，115体验和Prompt比较分别PENDING；PRODUCT_HOLD / NOT RC不变，无真实服务、私人凭据读取、信任链变更或自动发布。Desktop报告只是参考交接，不授权或自动启动Desktop开发。
