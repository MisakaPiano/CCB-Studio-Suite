# 导入性能、队列与页面组织

2026-10-23 JST，起点aaf98b42775e7ce03a3e9aed4751b41e931a9cb9。用户确认上一包两个限定测试组pass，同时指出3图约一分钟、24图约十分钟的严重导入迟缓；原反馈从本聊天本地旧消息找回，用户确认后授权继续修复，并允许一并优化其他页面相同的组织问题。

要求：优先量测导入耗时；APNG队列多选/移除、参考外部工具按对象和功能分区，保留复制/全部复制等常用直达操作；可展开入口有统一展开标记；返回固定左上；图库搜索右上紧凑，可展开长检索，日期紧凑并提供日历。用户截图不入公开测试证据，不访问用户工作区/真实图片/外部图库，不改Desktop或权威请求/编解码。

初步源码诊断：LibraryRepository.records使用完整workspace.catalog，校验所有原件；load/update/retainIngress和逐图library.refresh反复触发，导致随已有资产与管理版本放大。完整catalog原件校验仍须保留给完整性检查/备份，不以省略校验冒充通过。合成基线采用已有80图再导入24图，修复后同用例对照，不能据合成耗时断言已复现用户机器的十分钟。

上一包人工回执见evidence/UX_GALLERY_SORT_COMPACT_APNG/HUMAN_ACCEPTANCE.json；指定功能通过不代表性能/全部页面组织已签收。原大目录继续暂停，多Prompt重点专项仍独立待验，PRODUCT_HOLD / NOT RC。

## 导入修复与校验边界

- 旧管理读取调用完整catalog，后者每次遍历所有关系记录并SHA校验所有图片blob；旧单图副本链中load/update/refresh多次触发。旧24图还新增约49个管理版本，每次再遍历更长历史。
- 新增明确的catalogProject：验证所有关系信封和规范身份，仅对选中project的payload做完整验证。此API不是整库payload完整审计；完整catalog/backup/rebuild契约保持。管理记录查自身project，Prompt列表查Prompt project；未知/未来/损坏关系记录不能因过滤隐藏，选中payload损坏仍拒绝。没有将index当权威，也未做不可失效的全库“已验证”缓存。
- update在同一原子操作中复用已读取管理链，省去内部重复load。显式导入先逐项保留原件，然后批次创建COPY_PENDING副本记录、复制/校验各文件、最后将完成项提交PRESENT；新库24图只需3个管理版本（入口注册、pending、完成）。取消/失败保留原件、pending和已完成项，已有不同内容目的文件不覆盖；不会清空旧管理链。
- 每批只完整刷新一次，原件自身提交、目录副本及最后完整catalog仍校验。显示读取入库/保存副本阶段与真实计数、停止入口及用时；分阶段计时仅是本机诊断数据，不暴露文件路径/Prompt/凭据。

## 队列及组织

- 独立的当前查看与队列选择，支持勾选、Ctrl/Shift图片选择、全选/清空、删除所选和仅复制/导出所选；移除仅影响当前工具输入，不删除图库原件。输出仍固定source/mode/cover身份，复制无原图回退。
- 常用当前复制、全部复制、所选复制保持直达；添加/当前结果/整批按对象和功能收在带展开标记的有边框分区，展开后显示操作按钮。队列卡片显示文件名、勾选与实际预览/导出状态；未处理封面明确为示意，不冒充处理完成。清空队列同时清旧预览/进度，批量子集计数按实际导出数。
- 伪装队列复用同一封面显示；缩略图静态解码到128px，并发最多2，离页取消；不为每张卡片启动动画播放，不缓存无界全尺寸结果。主预览主动播放保留。
- 图库左上统一返回、当前相册简名与导入，右上紧凑单行搜索/长搜索独立展开；筛选内容压成紧凑行，日期32dp输入+日历，可选日/月/年。保留原日期语法和相册独立排序。其他可见同类入口（导入回执、共用凭据、创作设置/封面入口）统一展开标记；原有StudioDisclosure和CompactChoice继续沿用，没有把直接导航或执行操作都伪装成折叠框。
- Workbench右键菜单使用深色表示，原件/结果的操作职责保持。

## 逐次验证（后续最终包回填）

- 20261023T012553857812Z-import-24-baseline：1项实际通过，101.244秒、sourceStable；已有80张480×640合成图，单图52889字节，24图导入端到端39919ms。不是用户真实图库十分钟的复现。
- 20261023T012930459058Z-import-24-optimized：42项存储/核心实际通过，452.985秒；包含进程中断、千项规模、备份/恢复和目录收集。桌面编译因在非挂起计时lambda内取得coroutineContext失败，桌面未执行，整体FAILED；原日志保留，后续把context快照移到计时段外。
- 20261023T013749979253Z-import-24-optimized-measurement：11项桌面实际通过，101.661秒；相同用例10610ms，约为基线26.6%。导入/剪贴板/目录收集、取消排水已有回归，自动通过不代签真实库性能。
- 20261023T014250345982Z-queue-layout-and-import-regression：新project catalog测试2项/1失败，11.825秒；测试直接比较含byte[]的Java record对象，clone后的数组引用不相等。改为验证稳定ID及原字节/hash；未修改实现以迎合断言。
- 20261023T014346381464Z-queue-layout-and-import-regression-second：19项/1失败，162.767秒；project catalog2及batch checkpoint1通过，桌面15通过/1失败。队列/日历/子集复制已执行，失败为离屏场景无法读取独立native Window的语义。将长检索内容提取为原生窗口实际使用的同一组件，分别离屏验证输入/完成，保留原生窗口人工待验。该轮性能8466ms，具体阶段以原始measurement为准，不只挑最快值报告。

另建tasks/PERFORMANCE_RESOURCE_PROGRAM.md按PERF-001–007持续记录性能/资源，单项改善不关闭整个专项。

## 最终验证与固定本地包

20261023T014856925977Z-import-queue-compact-final实际桌面20项PASS，0失败/错误/跳过，141.705秒，sourceStable=true，createDistributable成功。实际执行队列勾选/范围选择、子集复制收据对象、移除/清空不删原件、空批不留预览忙碌/旧进度、日历今天/清除、紧凑图库、长搜索实际内容组件输入/完成；另含原有APNG、相册排序、导入/剪贴板与模型档案回归。原生长搜索Window适配需人工，不以离屏内容语义验证代签native Window。

最终同规模导入8729ms，分阶段记录在measurements/final.json；主要耗时为不可变原件提交6759ms与目录副本1549ms。read=0是每次小读取按ms取整累加的分辨率限制，不是没有读盘；端到端直接测量完整c.active生命周期。首次优化10610ms、后续8466/8729ms均保留，不只挑最快值。基线39919ms相比最终约降低78%，不外推成真实库或全应用统一倍数。

最终core/asset生产文件与42项完整存储/核心用例执行快照逐项一致，新project过滤/副本中断边界3项在后续单列通过；相关失败轮的整体状态仍FAILED。Temurin17.0.20.1+1、Gradle9.1.0、离线/严格依赖校验/单worker。113项固定共享权威保持原字节，未改Prompt/request/编解码。全部原始日志、JUnit、source-before及失败记录保留于evidence/UX_IMPORT_QUEUE_LAYOUT，不把UP-TO-DATE算作新执行。

已实际检查合成离屏Compose截图，队列多选标记/文件名/状态清楚，主预览与封面分开；图库搜索右上、日期单行，展开筛选后图片仍有足够区域。右键深色及独立窗口的实际Windows体验仍待新包人工回执。

新包`.local/windows-rc/import-queue-layout-review/application`220个文件逐项大小/SHA与最终构建相同，modules/tools完全匹配最后source-before。快捷方式`Start-import-queue-layout-review.lnk`目标/工作目录/旧测试资料参数/`--pause-collections`已回读；旧包保留，没有启动第二实例、读取/复制用户workspace/runtime或重扫原大目录。inputs复用已知合成album-A/album-B、24图apng-batch及两张封面；另有空paste-receiver/exports。

下一动作：正常关闭旧窗口后用新快捷方式，仅导入合成apng-batch一次，记录总用时/阶段反馈/切页响应；随后检验勾选多项、子集复制、移除不删图库、常用复制直达/分区、图库日历与长搜索窗口。此前两组基础PASS保留，不重做真实生成或原大目录。新功能与PERF-001真实库速度仍HUMAN_PENDING，PRODUCT_HOLD / NOT RC。
