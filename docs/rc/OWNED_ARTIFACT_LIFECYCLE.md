## 2026-10-26 UPD-CLEAN-PROTECTION-001新增包内合成证据

[内部候选与3逐例](../audits/INTERNAL_CANDIDATE_20261026.md)直接调用80ea116安装包实际updater字节：默认115/保护111保持；成功115→116只退役有所属证明的115，未知文件/111/备份保留、重复0；nonce不符和损坏保护记录拒绝或保留。3PASS，Windows原语/全合成JSON和假程序字节，非实际App111记录迁移。原静态登记作为历史保留；当前仅SYNTHETIC_3_PASS / REAL_RECORD_PENDING，完整重解析点/占用等矩阵本轮未重跑，不宣布所有保护或实际历史清理完成。未访问、修改、迁移或删除真实App/ACTIVE/备份。

# 安装与测试产物生命周期（下一版实现，未发布）

最新已记录结论：Project对f6598d3/54a5ba26下载终态P2作CLOSED_WITH_SCOPE，24项定向证据获限定接受；[原回执](../audits/PROJECT_DOWNLOAD_TERMINAL_CLOSURE_20261025.md)不覆盖旧包或完整升级链。未重打包，2484858内部EXE不含该补充。完整COMPLETE/journal身份验证和FAILED nonce绑定必须先于程序根筛选；不能证明终态的引用保留。实现见`docs/audits/DOWNLOAD_TERMINAL_FIX_20261025.md`。

范围合同：`tasks/REVIEW_FIXES_AND_UPDATE_CLEANUP_20261024.md`。用户已否决最初的7天/两次启动保留方案：**既有流程确认COMPLETE后立即退役所属程序和缓存**。不是清理整个H盘；不改115公开包或日常安装。源码限定审查不代签新包集成、原生完整运行或全项目验收。

## 更新后

现有更新采用并排新目录安装、严格安装清单、真实B程序/资料/凭据回执，成功后才切换固定入口。NSIS原有逐清单卸载及Windows临时下载自动关闭删除继续保留。

新增`tools/update-runtime/residue_cleanup.py`：安装时保存所属收据；成功交接后由B运行时启动清理，B启动/之后主入口启动可补重试。不得把清理失败写成更新失败，也不得凭下载完成宣布更新成功。失败/未完成交接保留旧程序和回退条件；完成后的旧程序不再作为长期回滚副本。资料迁移原目录、人工备份和活动保护记录继续保留，不自动降级资料。

清理只使用已验证安装器的明确输出、受限下载文件名及匹配的哈希/大小。初次升级进入新机制，仅可凭旧COMPLETE安装记录固定的manifest哈希，接管本次刚被替代的旧程序；不枚举接管全部历史目录。未知、改过的文件及插件/源码/用户资料等目录保留。活动/备份保护优先。按同一Windows句柄核验并删除文件，锁定父目录拒绝路径穿越、符号链接和目录联接；目录只在空且属于清单时移除。

被占用或无权限时，有限重试（1、2、4秒），随后留待下次启动；无7天等待。清理日志：`<runtime>/updates/cleanup/events.jsonl`，状态/所属收据同目录，进程输出`post-update-process.log`；应用启动结果另有`last-result.json`。交接与失败日志仍保留，旧程序大文件不长期堆积。不明历史文件不能安全归属时必须另列待人工确认，不能保证删除用户自行放入的所有未知内容。

## UPD-CLEAN-PROTECTION-001：历史保护与当前入口冲突（待整版合成验收）

登记在既有REVIEW_FIXES_AND_UPDATE_CLEANUP_20261024之下，不新开清理任务。**REGISTERED / STATIC ANALYSIS ONLY / NOT EXECUTED**，执行留到主题验证后的正常内部包验收及单独资源门；不因登记启动测试、修改生产逻辑或处理本机残留。

现场前提由原协调线程只读核实并转述：`launch-current.json`指向115-ff145d051bae且exe SHA匹配；`ACTIVE_INSTALLATION.json`仍引用111-1c9385afc38c并设置`doNotCleanReferencedDirectories=true`。本执行线程未访问真实App目录，不读取/复制真实data或凭据。原20根回执已[明确保留受保护111](../audits/HISTORICAL_CLEANUP_EXECUTED_20261025.md)，属于按保护规则保留，非漏删；最新源码未部署到公开115，现场不能作为新机制执行失败的证据。

静态固定于源码[42146d3705f5cbc9ed392735f9c4727f8a8f67d7](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/42146d3705f5cbc9ed392735f9c4727f8a8f67d7)：

- [`protected_paths` 88–102行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/42146d3705f5cbc9ed392735f9c4727f8a8f67d7/tools/update-runtime/residue_cleanup.py#L88-L102)对历史活动记录实行独立保护，保护标志不是显式false时，program/workspace/runtime/两种备份引用均纳入；未知/损坏路径不推断失效。默认入口更新不会自动撤销这层保护。
- [`run` 174–208行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/42146d3705f5cbc9ed392735f9c4727f8a8f67d7/tools/update-runtime/residue_cleanup.py#L174-L208)先校对当前入口并要求可验证COMPLETE，只允许按本次更新的直接前任及其固定manifest证明登记；202行阻止受保护前任接管，228行及265行还会阻止受保护所属文件退役。不枚举接管全部历史版本。
- [`create_launcher` 24–74行](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/42146d3705f5cbc9ed392735f9c4727f8a8f67d7/tools/update-runtime/apply_update.py#L24-L74)条件提交的是`launch-current.json`；当前modules/tools对`ACTIVE_INSTALLATION.json`的引用仅保护读取与合成fixture，未发现自动协调/迁移旧保护记录的生产写入分支。
- 既有[`test_registered_program_and_backup_protection_override_retirement`](https://github.com/MisakaPiano/CCB-Studio-Suite/blob/42146d3705f5cbc9ed392735f9c4727f8a8f67d7/tools/update-runtime/test_residue_cleanup.py#L90-L92)准备/历史覆盖的是受保护旧程序保持，不能代签“历史111＋当前115＋后续候选”冲突已做整版包验；本次未重跑该方法或追加通过计数。

**静态结论：可安全自动做的是保留受保护111，并对其他具备完整可信证明、未受保护的本次直接前任按既有规则判断；没有自动解除旧保护/归并两种记录的机制。若要释放111，需一次性人工审定的记录迁移及独立文件归属核验，并取得具体操作授权。**不能仅因默认入口是115、记录较旧或文件不在新包就删除111；不能一概移除活动记录或将保护标志改为false来绕过workspace/runtime/备份引用。即使完成记录迁移，没有可信所属收据/固定旧安装证明的历史文件仍保留。本次只报告该前提缺口，不执行或实现迁移。

后续仅在新建、明确所属的合成验收根构造相同JSON字段/层级（路径全部替换为该根内的假路径、exe/manifest内容使用合成字节；不复制真实记录内容/data/凭据），覆盖：

1. 合成默认入口115与保护记录111并存；初始化清理后111、保护记录及合成备份/资料均保持原字节，未知文件保持，不自动改写记录或补造历史所属证明。
2. 构造115→候选的完整成功交接及匹配的所属收据/先前固定manifest；仍保留受保护111，只有符合本次前任条件的115所属文件可按既有规则退役。没有这些证明时保留115，不用目录名推断。
3. 缺失/不匹配身份、损坏保护记录、未完成交接或重解析点等条件下保留或拒绝，不能误报成功退役；重复运行不得扩大范围。每种结果留原始日志及前后元数据/合成字节散列，不把保护性跳过写成全部历史残留清理完成。

真实App的所有指针/设置/安装文件/111和备份均不在本次操作范围，10份MSI副本的精确删除许可与本用例无关。主题→内部包验收→后三组顺序及旧未验/失败/PENDING保持。

## 本地工作区

`tools/workspace_artifacts.py`只接管本轮新建的`.local/disposable/<标签>-<随机身份>/payload`，创建OWNER后才允许写入，结束后保存大小/哈希清单并封存。日志、逐例结果和源码/构建输入散列先保存在该目录之外；只有证据完整且成功的任务才删除本轮已登记且仍匹配的文件。失败现场、未知或改过的文件保留；占用可再次执行`--cleanup <准确目录>`。OWNER、封存清单和清理摘要保留以便追溯，体积很小。禁止给旧测试根补造OWNER后批量删除。

入口：

- `tools/Run-PreviewChecks.py`给定向JVM测试创建隔离根，Java临时文件也落在本轮根；日志和JUnit保留后自动清理；`--keep-fixtures`显式保留。真实服务探针不自动登记。
- `tools/Run-UpdateChecks.py`执行本轮定向更新/残留/生命周期合成回归，保留逐例结果后清理。
- 原`tools/update-runtime/run_tests.py`也接入新根；临时目录不再持续追加在旧固定测试根。
- 安装包/原生更新等大产物须在新任务开始时创建独立根，复制唯一需要保留的包及原始证据到外部并核验后再封存清理；不能自动删除固定复审包、依赖缓存或失败现场。

历史目录初查阶段仅只读，后续用户已直接授权准确清单的清理；实际20根回执见`docs/audits/HISTORICAL_CLEANUP_EXECUTED_20261025.md`，不把该授权推广到归属不明目录。特别保护`.local/ACTIVE_STUDIO_INSTALLATION.json`引用的`model-accounts-review/workspace`和`runtime`、107/114/115基线、已冻结Project复审包及原始失败证据。测试和自检通过均不等于Project验收或115人工体验确认；Prompt对比继续PENDING。

## Project返工后的补充（2484858，未发布）

当前B首次接管也必须使用旧安装记录固定manifest；无安装时字节证明的生成卸载器保留。已验证待安装下载只由同目标/包散列的COMPLETE释放，未验证为终态的交接引用始终优先保护；较新失败下载和解包等待后续成功，不能用历史成功追溯释放。入口helper在共享锁内做期望旧描述符、版本、根归属的条件提交。18 JVM/56 Python及双DPAPI合成凭据原生演练通过，新源码独立复审待定；详见`handoff/PROJECT_REVIEW_RESIDUALS_20261025.md`，旧失败不覆盖。
