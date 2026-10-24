# 六项定向修复、更新残留及工作区清理交接

状态：**实现及定向自动验证完成；待Project独立复审；PRODUCT_HOLD / NOT RC保持**。无新公开版本，没有修改日常安装/资料、签名信任链或真实凭据。115体验反馈、Prompt对比各自PENDING。本轮同时处理用户要求的六项缺陷与清理机制，未重开UI开发或重复已确认人工验收。

## 固定对象

| 对象 | 固定身份 |
|---|---|
| 原应用/公开发布 | `ff145d051bae286ab3e27cadd41195f8bd4c4e18` / `v0.0.0-preview.115`，Pre-release保持 |
| 原公开安装器 | 168890283 bytes；`356e749bd346af4a1d419a0d8b48b1e27d2314e6fce5763fa0933917247862dc` |
| 原交接 | `2489844984e2ad13f2f0c9d614702d37a7e1d615`；原ZIP散列与Git blob未变 |
| 本轮最终源码 | `da2ae810542c79b0049b74148566a423fb3e1bc3` |
| JVM应用构建 | `28d09f52ea8954f7f713744af892fc50f53f3887`；至最终源码的全部modules及原生启动器输入散列相同，未重复跑38项充数 |
| 最终内部安装器 | 168923897 bytes；`8b33fd65aa0d4a08d0d2d1e6b6a1614ae1e35b166eba4d8e1224abf43d49deff`；internal 2，仅隔离演练，**未发布** |

最后可核实独立PASS基线仍为`678caf1`创作页限定PASS_WITH_SCOPE。原Project“阶段复审结论”随后对115提出3个P1和3个P2，不能称已独立关闭。本轮材料提交给同一Project重新判断。

完整固定输入与包对应关系：[FIXED_SOURCE_PACKAGE_MAP.json](evidence/REVIEW_FIXES_CLEANUP_20261025/FIXED_SOURCE_PACKAGE_MAP.json)。差异：[2489844 → da2ae81](https://github.com/MisakaPiano/CCB-Studio-Suite/compare/2489844984e2ad13f2f0c9d614702d37a7e1d615...da2ae810542c79b0049b74148566a423fb3e1bc3)。逐文件入口见[实际变更](evidence/REVIEW_FIXES_CLEANUP_20261025/CHANGED_FILES.json)。

## 六项实现与对应验证

| 缺陷 | 最终变化与证据 |
|---|---|
| 恢复副本夺取默认入口 | 恢复启动使用secondary标记及独立runtime；默认data/runtime归属检查，加跨进程字节锁并在锁内复核版本。恢复副本不能创建默认快捷方式或执行默认更新交接。ReviewDispatchTest及跨进程探针覆盖默认描述符/快捷方式原字节、较旧写者与恢复根拒绝。 |
| 暂停/结束后仍可发送 | 持久队列begin/submitted在workspace锁下复核暂停/结束；HTTP应用拦截器入口与关闭门作原子发送许可，未发送任务NOT_SENT并退回预留。覆盖等待准入时暂停和结束，生成传输计数均0。许可先赢的请求视为在途并接收，不承诺取消服务器已受理工作。 |
| 关闭/更新取消在途 | 维护入口先关发送门、阻止新任务，批次runner与手动生成join排水；保留并保存慢响应，不再cancel已发送请求。实际批次runner、手动关闭/维护用例，以及原生A慢响应到B验证链均覆盖。 |
| 排队账户可变 | 持久保存AccountProfile及secretRef快照，派发用冻结版本；新空批次在首次加入时取当前账户，新建批次清除旧快照。覆盖修改同ID档案与重开；未读取真实凭据。 |
| Prompt采集中断重复 | 按来源目录、UTF-8内容哈希和父版本接管已提交Prompt；修复别名/标签/观察记录。多匹配歧义保留RECOVERY_REQUIRED，拒绝盲目再入库。覆盖首次与新版本在资产提交、标签/观察提交之间中断及重开重扫。 |
| 手动累计/归档容量不一致 | 当前手动批次上限1000项，包含合成结果；在生成或引导准备之前拒绝超额请求；大回执分离保存。旧超额状态按1000项分组归档，保留原图引用；仍受既有40分组和文档体积上限约束，非无限容量保证。999/1001及重开、归档回归覆盖。 |

核心文件：`GenerationSendGate.kt`、`CreationSession.kt`、`BatchGeneration.kt`、`GenerationOrganization.kt`、`LibraryRepository.kt`；桌面接线：`StudioApplication.kt`、`BatchController.kt`、`DesktopEntrypoints.kt`、`LibraryBackupWorkspace.kt`、`StudioUpdateHandoff.kt`。未复制或改写权威Prompt/NAI请求序列化逻辑。

## 更新和工作区清理

原机制已有并排安装、严格manifest、新B资料/凭据回执、逐清单NSIS卸载，以及普通临时下载关闭删除；缺口是成功后的旧程序与持久下载/失败所属输出退役。新增所属收据及成功后的安全删除，详见[生命周期规则](../rc/OWNED_ARTIFACT_LIFECYCLE.md)。

用户否决7天/两次启动保留，最终为**COMPLETE后立即清理**。此前保留旧程序与失败现场，之后不维持长期二进制回滚副本；资料迁移原目录及备份另受保护，不自动降级或删除。遇占用/权限问题有限重试并留到下次启动，不影响启动，也不改变更新COMPLETE结论。

新安装时登记可信安装器输出；首次接入旧版本时仅接管本次刚被替代且旧安装记录固定manifest哈希的目录。**旧记录未固定散列的生成卸载器不能只凭文件名删除**，这类历史文件另列人工清单。原件、数据、配置、凭据、插件、源码、备份和未知/被修改的文件保留。父目录与被核验文件持有Windows句柄，拒绝重解析点；只移除已知空目录。jpackage正常只读启动器使用同一句柄的DispositionInfoEx处理，不按路径清除未知文件属性。旧Windows不支持时安全保留重试。

日志位于`<runtime>/updates/cleanup/`：`events.jsonl`、所属收据、状态、`post-update-process.log`及`last-result.json`。无归属证明的旧目录、未形成清单的中断解包不能盲删，不能承诺替用户删除任意未知文件。

工作区新测试统一登记独立`.local/disposable`根，先保存日志、逐例结果、源码/包映射，再清理该轮匹配文件。失败/未封存现场与未知变更不自动清除；长路径、占用、原子中断、重复执行有覆盖。Gradle与更新测试入口已接入，Java临时目录也归该轮。成功的原生演练在证据保存后退役重复安装副本，仅保留最终内部安装包。历史共享测试根不追溯补造所属权，另交[只读清单](HISTORICAL_CLEANUP_REVIEW_20261025.md)，**本轮未删除历史目录**。

## 实际验证及限制

最终计数分开：**38个JVM用例**，**49个Python用例**，失败/错误/跳过均0；另有跨进程入口3场景和原生A→B演练，不加进用例总数。仅最终集合计一次，旧37/47/48轮次及重复执行不累加。[运行索引](evidence/REVIEW_FIXES_CLEANUP_20261025/RUN_INDEX.json)保留各次真实结果。

原生演练使用真实Windows native exe/JVM PID及image-bound父进程句柄，测试专用main调用真实生产Workbench维护/排水、prepareUpdateHandoff和正常关闭；一条慢响应收完，未发第二条，A退出0。真实TUF回环下载、NSIS安装和B首帧核验完成；3项合成资产与1项实际DPAPI可解密测试凭据快照一致。COMPLETE后所属文件立即删除，重复清理0，未知哨兵保留。B仅对可见主窗口发正常关闭，实际进程退出。**最终补证驱动在写CLEANUP_ASSERTIONS.json时遇Windows共享占用，原始driver状态为FAILED，不改成PASS**；只读事后8项断言确认上述落盘结果，并验证完整临时TUF四角色签名、元数据散列关系和安装包字节。状态为POSTCHECK_PASS_DRIVER_FAILURE_PRESERVED，不宣称整条驱动无异常通过。此前同源码演练有完整driver PASS，但早期临时签名元数据未完整保留，适用范围更窄。

**不是原封115二进制A，不是用户点击完整GUI更新，不是任何真实服务调用**；A测试JAR与改过的cfg/manifest均有输入映射，测试main不随B发行。

保留发现的问题：第一原生探针混淆jpackage外层PID与真实JVM PID；受限环境快捷方式替换失败；早期脚本错误关闭隐藏工具窗口造成测试进程挂起；工作区长路径封存失败；中间原生演练只读launcher未清除；早期成功实验退役副本时未完整留存临时签名元数据。最后一项在同一源码补证演练中纠正，最终canonical证据包含完整公开签名元数据，旧实验只作有限历史记录，不能独立验证其临时签名链。它们有原始失败/观察记录及相应修正，不能用后来PASS冲销。用户精确授权后仅结束两条已核对image路径的挂起测试进程；最终探针在失败时也自动正常关闭自己打开的可见测试窗口。

| 状态 | 本轮范围 |
|---|---|
| 实现完成 | 六项修复与两类清理已接线，尚未公开发布 |
| 自动测试通过 | 上述38/49；原生链实际COMPLETE，最终driver写证据FAILED、8项只读事后断言PASS分列，非完整产品验收 |
| 用户手动确认 | 保留原107、磁贴/拖动/导入/选择四项等既有范围；无新增115体验确认 |
| 独立审查通过 | 新增改动**PENDING**，本交接不替Project签字 |
| 仍未验证 | 115命名/组织树/任务栏反馈、Prompt对比、真实服务、完整灾难矩阵、长时资源稳定、全日常环境原生升级与二进制可复现构建 |

没有重跑旧性能/资源测试，不能据定向回归声称无泄漏或更低整机占用。备份/恢复沿用原限定通过，本轮只测与默认入口和更新快照有关的变化。未清空回收站，未访问真实凭据/签名私钥/恢复码。

原始日志、JUnit/XML、逐例结果、manifest、安装日志和失败记录见[evidence目录](evidence/REVIEW_FIXES_CLEANUP_20261025)及`RAW_EVIDENCE.zip`；其逐文件大小/散列在`RAW_MANIFEST.json`。只有合成证据和公开身份信息，不包含用户资料、凭据密文或签名私钥。
