# 目录收集阻塞与恢复

2026-10-23，基线 `44c10abadd4c53437525a9d430e45a7061f10939`，`feature/windows-rc-continuous`。用户说明卡住的是旧测试窗口；未把该反馈误记为新APNG包验收失败或通过。

## 实际诊断与旧窗口处理

只读取了已确认Suite进程的线程栈与程序身份，未读取堆、用户目录内容、图片、Prompt或凭据，未将截图/private目录名写入公开证据。默认沙箱拒绝进程诊断后，以限定进程的只读提权成功检查。旧程序为 `.local/windows-rc/studio-usability-review/application/CCBStudioSuitePreview.exe`，PID 23500。

栈中 `AWT-EventQueue-0` 在 `SharedDocumentAssets → SharedAssets.documents → StudioWorkspace.catalog` 等待workspace monitor；后台 `DefaultDispatcher-worker-2` 在 `LibraryController.synchronize → LibraryRepository.scan → importImage → AssetStore.finish → StoreIO.cache → FileChannel.force`，持有同一monitor。可确定整批同步和界面同步读取相互阻塞；单次线程快照不证明所有IO均正常或没有其他故障，也未确认用户资料完全无未完成项。

已向用户说明强制结束可能留下未完成导入记录/丢失未保存修改；用户明确选择“允许结束这个旧窗口进程”。再次核对完整程序路径后，仅结束PID 23500，命令成功。未删除目录、资料、锁文件，未启动第二实例，未触碰Desktop。

## 修改范围

- 扫描不再占有整批workspace monitor。外部枚举/读取/校验及进度回调在锁外，各资产写入仍使用既有事务与串行保护；整个扫描持有任务登记，关闭须取消并等待，未放宽单写任务门槛。
- 每文件间检查取消；进度以合并通道发布，显示总数/完成数、可换页，并提供任务本身的“停止目录收集”。停止暂停本次自动调度，已完成写入保留。等待当前文件安全结束，不强行中断持久化。
- 扫描观察记录分批保存，正常取消时保存已完成部分；未完成扫描不把未访问项标为来源丢失。管理记录合并变更，保留并发偏好/标签；持久化出错停止，不再用自动重试覆盖不确定状态。
- 旧扫描可能先提交图片、后保存观察记录。后续扫描以文件hash及完整目录/相对路径/前版本来源识别可复用的已提交图片，不重新创建同一入库项；现有原件均保留，不删除历史重复项。不是对用户真实库的修复结果声明。
- 图库整册资产使用后台更新的内存投影，回收站和Prompt参照读盘转后台；创作输入使用已载入状态，前台操作收尾读盘也转后台，避免目录写入时堵住事件线程。
- 增加 `--pause-collections` 本次启动开关，不改持久目录授权/偏好。恢复快捷方式默认暂停；用户检查入口后可明确恢复，或仅手动同步一个入口。普通新包保留原自动收集行为。

## 验证与交付

合成回归执行中，原始结果与固定包待回填。新增测试包括持有workspace锁时实际Compose渲染/创作输入响应、暂停启动不扫描且单次手动同步有效、取消时只保存完成项、并发偏好不丢失，以及旧提交未观察图片重开后不重复入库。旧窗口被终止不等于新包真人验收；APNG/多Prompt尚待项独立保留。无真实服务请求、无新公开Release，PRODUCT_HOLD / NOT RC。

首轮 `20261022T222050579028Z-collection-responsiveness-recovery` 实际20项/1失败，204.114秒，sourceStable=true。core10项通过，desktop10项中1项失败：旧测试把首个非空提示当作完成，新进度提示出现即可提前继续下一轮。改为等待collectionBusy结束；不把这轮整体标为通过。

第二轮 `20261022T222446764360Z-collection-drain-and-recovery-final` 实际desktop15项/1失败，78.932秒，sourceStable=true，core为UP-TO-DATE。追加取消/收尾防重入测试抓到真问题：IO排水虽处于NonCancellable，返回已取消上下文时会在清除busy状态前抛取消。改为整个finally在NonCancellable内排水并保证清状态；以UNDISPATCHED进入任务登记，防止启动前取消漏掉finally。两次失败的原始JUnit/日志保留，修复后的结果另列。

## 最终回归与恢复入口

`20261022T222631749095Z-collection-drain-and-recovery-final` 实际desktop15项PASS，0失败/错误/跳过，85.596秒，sourceStable=true，createDistributable成功。其中目录6项（含实际Compose渲染/输入/编辑器初始化/参照页不等待后台锁、暂停启动、取消排水）、备份恢复4项、Prompt比较4项、查看器1项。core本轮UP-TO-DATE，不能重计；首轮core10项全部通过，核心源码此后未变。所有失败/成功的原始source-before、JUnit和日志保存在evidence/UX_COLLECTION_RESPONSIVENESS，不以最终绿灯抹去前轮失败。JDK Temurin17.0.20.1+1、Gradle9.1.0，离线/严格依赖校验/单worker。

新本地固定包 `.local/windows-rc/library-recovery-review/application`，220文件逐项SHA-256与最终构建相同，modules/tools源码和最终构建快照相同。113项固定来源编解码/请求权威及asset-library源码保持原字节，见SOURCE_VERIFICATION.json与PACKAGE_FILES.json。

恢复快捷方式为 `.local/windows-rc/library-recovery-review/Start-library-recovery-review.lnk`，已回读核对程序/工作目录及原测试工作区/runtime参数，新增 `--pause-collections`。只创建快捷方式，未启动第二实例、未读取/复制其私人数据。之前APNG/查看器包保留原快照，当前应先用此恢复入口；本包包含前轮封面及自动预览变更。提供inputs/collection-smoke的两张纯色合成图片，供后续主动验证单目录同步，非用户图。

已异步请用户先保持暂停，仅核对重开/切页、配置、目录和已入库图片可见；此时回执PENDING。真实旧工作区恢复结果未由代理确认，尚未恢复自动扫描或真实生成。完整目录大批次体验、新APNG交互及重点多Prompt人工组仍独立待验，不发布邀请版。

## 人工回执：暂停状态恢复通过

2026-10-23用户明确答复“正常打开，配置、目录和已入库图片可见”。限定签收cc93735恢复包在暂停自动收集下打开、账户/图库/目录切页与保存内容可见；见evidence/UX_COLLECTION_RESPONSIVENESS/HUMAN_ACCEPTANCE.json。未以此代签原大目录重扫、收集进度/停止/排水实机操作、全部字节完整性或APNG/多Prompt组；代理未自行恢复监听或读取私人内容。下一步先使用随包两张纯色合成图片所在小目录验证单入口同步，不直接恢复大目录收集。仅记录回执，未改生产代码或重建包。

随后用户完成随包inputs/collection-smoke两张纯色图片小目录的显式单入口同步，答复“同步正常，切页正常，两张纯色图片可见”。此项人工通过，原目录继续暂停；没有代签原大目录重扫或长任务进度/停止/排水体验。下一组回到原先待验的APNG自动预览/缩略图及多套封面，沿用同一固定恢复包与合成图片，无须重设账户或调用服务。此次仍仅记录人工反馈，不改应用、不重跑无关测试。
