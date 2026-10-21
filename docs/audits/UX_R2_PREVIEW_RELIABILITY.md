# UX-R2 预览字体、队列空间与回归可靠性

接续155d27df5807337531995ff767fa8a694961a7d7，当前Suite唯一执行者。PRODUCT_HOLD不变。

## 实现

画布、完整预览、导出预览共用TypesetPreviewRaster，在IO线程按页面引用重新验证并加载保存的字体资源，再度量和绘制。这样默认字体在重开后首次添加文字也能预览；缺失资源显示失败，原始文件预览不依赖文字字体。字体不安装到系统、不静默替换。测试关闭/重开工作区并清空仅该合成字体的进程缓存，验证三个消费者；此冷缓存模拟不是额外新JVM进程测试。

队列栏允许的720px宽度不再压缩画布为细缝：实际窗口容不下队列+300px属性+400px画布时使用队列/画布/属性分区。1100×900下保留画布空间，选择与720px偏好在保存重开后保留。

导出预览拆为显式State消费者，错误可见，取消正确传播；保留不含内容的内部诊断回调。没有修改权威请求、原件安全核心或Desktop。

## 失败与验证边界

独立挂载导出弹窗的700×800离屏测试在完整测试中反复超时，单独测试则通过。日志证明图片已转换且状态已赋值，画面仍未显示图片；前期帧循环/通知/局部订阅修正不足以单独解除该现象，不把这些猜测写成已证实根因。全部历史失败和失败画面保留。

最终窄窗验证经完整Workbench点击“预览与导出”，与宽窗和真实用户路径一致，保持真实700×800视口、同一控制器/工程、30秒超时、图像及全部底部动作边界断言；宽场景先关闭，未关闭用户原生窗口。测试帧循环显式发送Compose快照通知。该路径最终完整desktop52项实际PASS（97.417秒），随后同一源码--rerun强制实际复测52项PASS（92.383秒），均0失败/跳过、sourceStable=true。独立直接挂载弹窗的离屏调度根因仍UNKNOWN；不据此冒称所有原生窗口或并发窗口可靠性已测。

此前一次稳定性命令被Gradle缓存跳过，执行0项，明确COMMAND_SUCCESS / NOT TESTED。core本检查点没有修改或重新运行，不把旧结果升级为新执行。

JDK Temurin17.0.20.1、Gradle9.1，严格离线、单worker/单测试fork，依赖来自既有缓存，无新增下载。真实API0，未读真实图库/凭据。截图为实际Compose离屏；原生Windows/DPI/IME/真人质量NOT TESTED。R2其他缺口、R3完整字段流程及R4安全应用内更新继续，未达到RC。

[全部运行含失败](evidence/UX_R2_PREVIEW_RELIABILITY/runs/) · [窄窗成功](evidence/UX_R2_PREVIEW_RELIABILITY/narrow-export-deck.png) · [保留失败截图](evidence/UX_R2_PREVIEW_RELIABILITY/narrow-preview-timeout.png) · [最大队列](evidence/UX_R2_PREVIEW_RELIABILITY/maximum-queue-compact.png) · [文件身份](evidence/UX_R2_PREVIEW_RELIABILITY/PACKAGE_FILES.json)。全部合成证据纳入Git，Drive NOT UPLOADED，无新增非Git附件。
