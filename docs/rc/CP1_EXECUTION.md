# Windows RC Continuous / CP1 工程检查点 — RC BLOCKED

2026-10-21 JST。正式Gate已实际读取：[Issue #1 APPROVED_FOR_RC_RUN](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/1#issuecomment-6296885017)；完整[Issue #2](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2)及其[R1 PASS_WITH_SCOPE](https://github.com/MisakaPiano/CCB-Studio-Suite/issues/2#issuecomment-6297274746)评论均保存在[原始合同快照](../audits/evidence/WINDOWS_RC/CONTRACT_SOURCE.json)。授权连续开发不等于越过 STOP 或 RC 产品验收。

## 起点、所有权及继承

实际初始分支feature/a1-001-r1-safety，HEAD=27dbd5d578f364afb529fc33944386c0c107f0da；index/worktree/普通untracked均为空。远程main=6d3d2b2、原A1=3c17b4c9、R1=27dbd5d5。现有应用任务清单仅观察到本会话在Suite写入，一个Git工作树、无附加工作树/PR；核对分支不存在后创建feature/windows-rc-continuous。没有从main重做、reset/clean/stash或覆盖并行改动。[启动记录](../audits/evidence/WINDOWS_RC/START_BASELINE.json)。

只一个主执行者，无子代理/跨聊天派发。Suite内部记录owner claim，最终交付后标STOPPED_RC_BLOCKED而不保留活动写入者。旧A0/A1/R1报告、来源锁、历史母本及交接共59份受保护文件hash不变，旧FAILED/SKIPPED/UNKNOWN均保留，不追改为新PASS。[历史保护清单](../audits/evidence/WINDOWS_RC/HISTORICAL_EVIDENCE.json)。

## 实际实现

- 新增AssetMaintenance.inspect：逐条分类并返回状态、当前size/SHA-256及资产身份；目录整体被异常阻塞时仍可检查健康和异常项，不写入或删除任何条目。
- reconcilePending只接受显式规范暂存名和检查时fingerprint。候选必须是有效asset envelope，与已提交且原件size/hash有效的完整关系完全相同。未提交候选先调用原事务恢复；无法证明为冗余副本、未知名、正式权威文件、损坏/未来格式或已变bytes全部拒绝。
- 显式维护提交链：force intent → 原子移动冗余暂存到maintenance/reconciliations/<id>/candidate.rec → force complete。原字节留存，重复恢复验证同一intent/正式记录；中断可重入，双位置冲突不覆盖。没有通用清理/丢弃/迁移旧用户数据。
- 备份白名单增加该严格命名维护子树，原件/Recipe/工程关联及维护审计候选一起保存/恢复；既有R1排水门禁和未知目录严格拒绝保持。没有把维护归档当缓存排除而遗失证据。
- tools/Test-WindowsRc.ps1使用Suite隔离Gradle缓存/原始证据，继承已校验JUnit/Hamcrest缓存和低资源配置，不改旧工具/构建/依赖锁。源和最终Git blob需绑定实际测试hash。
- CP1更新基础采用真实python-tuf 7.0.1合成信任链脚手架；应用A→B计划和剩余门槛见[更新计划](UPDATE_A_B_PLAN.md)。这是成熟库验证，不是手写TUF/signed JSON替代。没有应用安装/重启或测试凭据迁移。

## 实际测试

| 运行 | 实际结果 | 证据身份 |
|---|---|---|
| 维护初轮 | 3 tests PASS，36.807s | cp1-maintenance-targeted，增加第4项前的快照，不代签最终测试源码 |
| 最终针对性 | 4 tests / 0 failures/errors/skipped，13.814s，PASS | cp1-maintenance-final，含3个真实子JVM halt(71)维护边界、重复恢复/备份恢复及未知/损坏/新格式检查拒绝 |
| 最终完整资产模块 | 4 suites / 31 tests / 0 failures/errors/skipped，411.419s，PASS | 原有27项重新运行+新4项，11份Java源码前后hash不变 |
| TUF信任链lab | 8 tests / 0 failures/errors/skipped，0.406s，PASS | 有效目标、签名/过期/rollback/hash/断网/身份/reader/双阈值root轮换；3份Python源码前后hash一致 |

[逐项JVM测试](../audits/evidence/WINDOWS_RC/CP1_TEST_CASES.json)、[源码身份](../audits/evidence/WINDOWS_RC/CP1_SOURCE_IDENTITY.json)、[各轮可读summary/XML/日志](../audits/evidence/WINDOWS_RC/runs)。完整运行包含原有7个真实JVM halt故障边界与新3个维护边界，以及跨进程锁、Windows junction、排水、备份恢复、1/100/1000资产和8MiB流。D06本轮374.488s（stdout elapsedMs374487），只是性能观察，不是SLA。当前新增测试没有FAILED/SKIPPED；历史失败证据保留在原文件。

## 环境及下载

Windows 10 19045 amd64；Temurin JDK17.0.20.1+1；Gradle9.1.0已安装版本，离线、单worker、Gradle heap384MiB/metaspace256MiB/2CPU、JUnit单fork192MiB，必要子JVM64MiB/1CPU。未下载Gradle/JVM依赖，未运行Desktop全量构建、模拟器、真实图库/Provider或公开服务。

TUF测试使用Suite内独立Python3.10.6 venv；该旧运行时不批准作为RC打包环境。7份固定wheel共下载5,202,673bytes，逐个与PyPI JSON发布大小/SHA-256核对，再按hash锁离线安装；元数据传输量未测。未改全局Python/pip、代理/账户/系统信任。签名器仅临时内存对象，无正式密钥或私钥文件，未进入Git/Drive/日志；无网络监听。依赖来源/锁见[provenance](../audits/evidence/WINDOWS_RC/dependencies/PYTHON_UPDATE_LAB.json)。峰值内存未测。

## STOP、未完成与交付

[依赖STOP](DEPENDENCY_ESCALATION.md)精确绑定Desktop@5ef9542的三项路径/源hash、许可文件名及README证据；不是重复全仓审计。未提供经批准的窄权威SDK制品/闭包/复用权利证据，且不能在Suite重写Prompt/request/fallback或擅自写Desktop。该问题触发正式Gate的来源/跨仓依赖STOP，CP2无法GREEN，RC整体BLOCKED。独立CP1基础已验证；没有把阻塞项当普通可跳过测试，或用Mock UI冒充Studio。

CP3/CP4产品门槛未执行；没有Windows安装包、真实应用内A→B、原生编辑/账户/Metadata/Recipe对比闭环或数据/凭据升级保全结果。REQ-01–12逐项见[RC_READINESS](RC_READINESS.md)，不降低首版范围。

NOT TESTED：任意断电/目录项耐久、真实磁盘耗尽/OS ACL、敌对hard-link/TOCTOU、产品UI/应用生命周期、真实凭据及Provider、安装更新和迁移、Android/Server、峰值内存与真人验收。未知/唯一暂存无法安全确认时仍会阻塞整库操作，用户图库完整可用并未获签。

本次checkpoint完成后按授权提交/推送当前Suite功能分支，原始安全证据另存Drive并记录ID/大小/hash/实现SHA；实际平台回执随后登记。停止点是RC BLOCKED而非CP4完成；解除依赖后从远程checkpoint恢复，不重启A0、不覆盖R1或隐式授权Desktop。main不合并、不release、不改变共享/公开性。

交付静态检查沿革：首次git diff --cached --check因新任务合同可读副本EOF多一个空行返回FAILED（tasks/WINDOWS_RC_CONTINUOUS.md:45），已仅整理该副本末尾换行；原Issue/评论JSON快照和测试源码/证据未改。不是测试失败，也不因此重跑已通过源码。后续检查结果以实际交付回执为准。
