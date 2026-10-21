# TypeSet 队列操作补齐

基线 `4a6dd1ef5e4dd7080abd5dde7c03ba6dfb55d920`，分支 `feature/windows-rc-continuous`；继续现有连续授权，不是产品/RC最终验收。

只读对照 TypeSet Forge 1.1 的 `renderImageList`、`moveSelectedEntriesBefore`、`moveEntryByPrompt`、`reorderEntry`、悬停预览及侧栏拖动函数。参考文件身份见 [SOURCE.json](evidence/UX_TYPESET_QUEUE/SOURCE.json)，未改动或上传参考原件。

实现按显示序号（含自定义开始编号）定位、四舍五入、插入前/后；批量移动保持原队列相对顺序，目标在所选页内时不移动。对话框显示范围及目标，队列/编号改变后旧目标失效。右键“此页”仅移动点击页；批次入口另列。复用既有工程保存/撤销，不改资产安全核心。

长队列由列表持有拖动捕获，靠近边缘自动滚动，释放时重新计算目标，一次提交排序。缩略图及悬停均通过既有排版渲染器显示编辑后图片或故事页，过期异步结果不冒充当前页；队列渲染串行且仅保留适合显示的位图。悬停不改变选中页或工程；弹层随鼠标并限制在窗口范围内。分隔线拖动只在松开时保存宽度，并为画布保留空间；旧窄窗口切换入口保留。

保留失败：首次遗漏共享组件 import、测试局部函数语法、分隔线作用域引用三次编译失败，未执行的桌面测试仍为 NOT TESTED。长队列测试两次过早在部分可见末页前放下：裁切后的语义边界不能证明已滚到底，现改用实际滚动范围；同时补上释放时重新计算插入点。分隔线测试初次将新工程尚未保存的视图状态误计作第二次编辑，改为先保存基线，再验证一次编辑提交。另一次测试关闭 Compose 场景时出现 SlotWriter 越界，补充失败诊断和释放指针/等待画面完成的清理。其后首次完整运行180项中179 PASS、1超时 FAILED，系统错误栈明确显示离屏 ImageComposeScene 默认协程在 DefaultExecutor 上调用滚动布局，与测试 AWT 绘制并发，触发 performMeasureAndLayout called during measure layout。定向改为使用与画面相同的 AWT 协程上下文；6项定向实际 PASS。早先 SlotWriter 异常可能相关，但不把这一推断写成已证明的单一根因；原生窗口验收仍未执行。

验证环境：JDK 17.0.20.1 / Gradle 9.1.0，离线严格依赖校验，单 worker 串行，未下载新依赖、未调用真实 API。测试使用合成图片及实际 Workbench Compose 操作树，包含指针拖动、右键、无效/过期输入、预览不改选择、撤销、持久化重开和原件字节校验。它们是离屏 GUI 自动化，不能替代原生 Windows 或真人验收。

最终完整重新执行 core104 + desktop76，共180项实际 PASS，0失败/错误/跳过，301.980秒，运行期间源码未变；没有用 UP-TO-DATE 或旧结果充数。定向与所有失败运行见 [RUNS.json](evidence/UX_TYPESET_QUEUE/RUNS.json)，逐项 JUnit 与日志保留。实际检查了编辑后悬停和侧栏调整截图；文件身份见 PACKAGE_FILES.json，测试源码与提交 index 对应见 SOURCE_DELIVERY_IDENTITY.json。仍为 PRODUCT_HOLD / NOT RC。当前证据可全部进入 Git，没有新 Drive 附件回执。后续继续设计结果与实际请求关联、剩余 TypeSet/资产操作及完整安全更新故障门槛，不在此处停止连续实施。
