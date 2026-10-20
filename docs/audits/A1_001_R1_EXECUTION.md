# A1-001-R1 — 资产目录完整性与维护排水补强

2026-10-21 JST。任务来源：本轮用户明确指令；Project 对 A1-001 固定提交的结论 `PASS_WITH_SCOPE` 由用户提供，未独立读取外部 Project 会话。只修复 R1-01/R1-02，允许指定功能分支 commit/push 和指定 Drive 文件夹内的安全附件上传，不授权 merge/release/A1-002/A2。

## 精确起点与范围

工作区 `H:\CCB-Studio-Suite`；初始分支 `feature/a1-001-asset-durability`，本地 HEAD 与 GitHub 分支均为 `3c17b4c9038d821efef726b1dc84c90c87855c39`，index/worktree/普通 untracked 均为空。远程 main 仍为 `6d3d2b22651f9744807925f74c6941f681bbef02`。从该审查 SHA 创建 `feature/a1-001-r1-safety`；没有覆盖已有工作。仅单执行者，无委派。

只改资产模块相关源码/测试/说明、任务局部测试工具、当前/任务/索引和新 R1 审计材料。旧 A0/A1 报告、原始测试证据、任务母本、SOURCE_LOCK、验收矩阵及原 Verify 工具不改。没有访问 Desktop 数据、真实图库、Prompt、Secrets 或付费 API。

## R1-01：目录完整性

旧 `catalogInternal()` 只枚举 `*.rec`，例如 `records/orphan.bin` 和真实写入中断留下的 `.pending-UUID` 会被漏掉；`rebuildIndex()` 继承这个不完整目录结果。

修复后枚举 `records` 全部直接子项，在任何索引写入前完成严格扫描；只返回完整成功，发现第一个异常则显式失败，不返回部分库：

| 条目 | 处理 |
|---|---|
| 规范小写 `<uuid>.rec` | 验证 envelope、关系/文件名身份及原件 size/hash；只有全部有效才进入目录 |
| 规范 `.pending-<uuid>` 且有效 asset envelope | `RECOVERY_REQUIRED`；这是已知写入器暂存命名格式的候选，不代表已证明归属或已提交 |
| 暂存/正式文件损坏、未知 envelope 版本或无法读取 | 分别 `CORRUPT` / `UNSUPPORTED_VERSION` / `READ_ERROR`，不改原 bytes |
| 其他名称、畸形/非规范 UUID、额外后缀或未知目录 | `CORRUPT` 并保留，不默认为可忽略缓存 |
| 扫描后正式记录消失 | 现有 `MISSING` 失败语义保留，不返回完整目录 |

健康资产仍可按 ID 单独读取/导出；未知项、临时项、原件、索引及观察记录不被此次目录校验删除或改写。已封存事务恢复仍可完成正式记录提交，但不会删除旧 `.pending-UUID`，目录仍要求显式检查。暂存处置/自动清理没有在本轮实现。未知版本即使使用当前可识别文件名也不会被自动解释为旧格式。

## R1-02：备份与排水

旧备份已通过外层 `operation()` 登记并持有串行锁，但内部再次调用公开 `catalog()`。在外层登记后、内层调用前开始 close，会使已接纳的备份在内层被 `CLOSING` 拒绝。Java monitor 可重入不代表生命周期门禁可重入。

最小修复：备份/恢复在已有登记内调用 `catalogInternal()`，内部入口检查当前线程持有 serial monitor；不重新通过 public admission。`operation()` 原登记/计数与 `close(Duration)` 的行为不放宽：新外部操作仍被拒，已登记/排队 IO 的整个 body 与 finally 都在 active 生命周期内，active 清零后才能释放 FileLock。未增加全应用生命周期或通用可重入门禁。

新增 `BACKUP_REGISTERED` 合成故障点，在外层登记、进入串行 body 后及目录校验前触发；不改变磁盘格式。两项并发测试用有界 CountDownLatch 固定顺序：登记→close(0)进入排水→检查锁及拒新操作→放行目录→首个文件复制后再次超时关闭/检查锁→完成或注入 IOException→关闭排水结束。没有随机 sleep。

成功分支验证 report、有效 `backup-manifest.rec`、全量 manifest 校验/恢复以及恢复后的原始 bytes；中断分支必须抛出指定异常，不产生完成标记，restore 返回 MISSING 且不创建目标。两者都验证源根只能在工作结束后重新获得所有权。已有“最终 manifest + 全量校验后才返回成功”的提交点不变。

## 测试沿革与实际状态

- 首轮 `red-targeted`：6 个测试，5 失败。并发断言失败可复现；3 个目录测试先在辅助快照读取 Windows `.owner.lock` 时失败，不能用这些结果证明目录漏洞。PS5 将 Gradle stderr 变成 NativeCommandError，脚本提前退出；事后从实际 XML 保留结果，未补造运行前源码 hash。该运行原始日志、XML、运行后源码快照与局限全部保留。
- 修正测试辅助快照只排除 live owner lock，脚本保留失败 exit/XML/summary，并在执行前写 hash；`red-confirmed` 实际 6 tests / 5 failures / 0 errors / 0 skipped，32.277s。目录遗漏和两项竞争测试在旧逻辑上失败；正常空/正式目录通过。修复前源码快照和运行前后 hash 保留。脚本在 Java version stderr 上还有一个回执阶段异常，测试及 summary 已落盘，随后仅修正回执收集。
- 修复后 `green-targeted`：实际 6 tests / 0 failures / 0 errors / 0 skipped，15.905s，PASS；9 份 Java 源码测试前后 hash 一致。
- 模块完整回归：实际 **3 suites / 27 tests / 0 failures / 0 errors / 0 skipped，PASS**，403.517s；重新编译并执行既有 21 项与新增 6 项，9 份 Java 源码运行前后 hash 一致。完整结果见[最终 summary](evidence/A1_001_R1/20261020T172241273Z-full-module/summary.json)与[逐项测试](evidence/A1_001_R1/FINAL_TEST_CASES.json)，没有沿用旧版 21 PASS。

## 环境、依赖与资源

Windows 10 amd64；Temurin JDK/javac `17.0.20.1+1`；复用已安装 Gradle 9.1.0，不下载 distribution；生产外部依赖仍为零。JUnit 4.13.2 / Hamcrest-core 1.3 从现有缓存只读取用并校验固定 SHA-256，复制到 Suite 隔离临时仓库；全部 Gradle 测试使用 `--offline --dependency-verification=strict --max-workers=1 --no-daemon`。原校验清单、依赖锁及 Gradle 构建配置不变，依赖下载字节为 0。

Gradle heap 384MiB/metaspace 256MiB/2 processors；JUnit 单 fork 192MiB；原进程级子 JVM 64MiB/1 processor。串行运行各轮，仅并发正确性用例建立两个受控工作线程。峰值内存未测量，不声称资源 SLA。

任务工具 `tools/Test-AssetSafety.ps1` 使用 `.local/a1-001-r1/` 独立缓存/原始证据；原 `Test-AssetDurability.ps1` 及 A1 证据不改。目录/写入测试仍使用既有 `.local/a1-001/tests` 下每次新 UUID 的合成根。工具按本次启动时间筛选 XML，失败编译不能把上次 XML 当本轮测试结果；输出运行前源码 hash 并在运行后核对。原始 PS5 日志可含 NativeCommandError 的 PowerShell 格式包装，不将其一概当成 Java/测试失败；以对应 exit、XML、summary 分开解释。

## 未覆盖及停止点

NOT TESTED：真实磁盘耗尽/OS ACL修改、任意断电/目录项耐久、敌对 hard-link/TOCTOU、完整应用生命周期、真实用户图库、真实 API、Android/Server、签名/安装/真实 A→B 更新、峰值内存及产品人工验收。空间/权限和中断仍使用合成异常。没有新增应用/协议/schema，也未降低更新安全门槛。

本轮仅修复 `records` 直接子项枚举，不新增全根/objects 完整性扫描或孤儿自动处置；已有 catalog 验证每个正式记录引用的原件。FileLock 仍只协调合作进程，无法证明所有外部写入者遵守。多个异常按排序首次失败报告，不提供批量修复 UI。严格拒绝目录意味着旧版能导出的暂存候选库现在会拒绝创建新备份，直到显式维护；原件和旧备份保持原样。

完成本轮验证和双通道实际交付后停止等待 Project review。审阅入口：[A1-001-R1 Project](A1_001_R1_PROJECT_REVIEW.md)。原始附件的大小/hash、固定实现 SHA、Drive 文件 ID 与 GitHub 回执由后续交付记录登记，未成功上传不得写成功。

完整回归包含原有 7 个真实子 JVM halt(71) 故障点、跨进程所有权/Windows junction、1/100/1000 资产及 8MiB 有界流式用例。D06 本轮实测 359.844s；不是性能 SLA。新增 6 项及原有 21 项均实际执行。构建 NO-SOURCE 任务不等于跳过测试。
