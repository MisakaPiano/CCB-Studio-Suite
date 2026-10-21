# UX-R2 四分类、元数据、复制与编辑Recipe

接续 `63863dfb2f69bdaf897109294d0e3246dd4c391c`，唯一Suite功能分支执行者。PRODUCT_HOLD，连续恢复未到RC，不合并、不发布。

## 实际工作流

导出预览可同时勾选四分类，每类包含所有目标页（无文字/遮罩页也输出），按队列顺序编号，分类目录与ZIP路径一致。全部/所选/当前页、PNG/JPEG、Recipe附件、ZIP/PDF维持同一完成回执。新receipt version2列出分类、每文件hash及元数据状态；验证器兼容旧version1，拒绝不完整目录或hash不一致。

PNG保留模式按TypeSet源算法保留tEXt/zTXt/iTXt原块和alpha隐写原始位流；新增CRC、长度、容量界限，不解压执行opaque数据。JPEG显示/记录源数据丢失；小人书记录composite。保留的元数据来自原图，不宣称原签名对编辑图仍有效。隐私清除调用固定权威PrivacyPngEncoder公开接口，清除文本与像素LSB；JPEG先经过同一公开清除路径再编码，不更改权威源码。原始文件分类永远保全字节，Recipe附件也不属于隐私清除范围，界面明确告知。

“复制所选输出文件”生成完整、校验通过的独立文件集再交给剪贴板。多文件用Windows文件列表格式；单张静态图兼容image flavor，动画不提供静态image flavor。复制缓存保留以支持后续粘贴，未清理用户文件或系统剪贴板。自动化仅使用独立AWT Clipboard对象，实际系统剪贴板/资源管理器粘贴NOT TESTED。

编辑Recipe包含原图hash、完整页面编辑、源Recipe的Base64来源记录。导入预览→确认→原图hash匹配→单次可撤销应用；不匹配拒绝，原图及权威源请求元数据不改。来源记录与编辑层分开，不把侧车文件冒充新的NAI原始响应。

## 实际验证

TypesetMetadataExportTest：保留原PNG文本块及alpha位流，另用固定权威StealthAlphaMetadata独立解码得到预期合成内容；隐私结果不含源文本块/alpha头；JPEG状态；坏CRC拒绝；2页×4分类、Recipe、ZIP共17文件；无编辑页不省略；Recipe匹配、拒绝、撤销。

实际Compose测试：勾选四分类/Recipe/ZIP→预览→复制17文件到隔离剪贴板→文件列表/单图flavor核验→导入刚导出的Recipe→确认恢复→撤销。小人书导出回归继续通过。截图为真实Compose离屏，非原生窗口/真人验收。

首次编译FAILED：调用权威internal sanitizePixel不可跨模块访问，0测试执行；改用现有公开编码接口解决，没有放宽权威可见性或复制其内部实现。失败日志保留。

最终完整回归 `20261021T082444371678Z-UX-R2-export-complete-regression`：**112项实际PASS（core66/desktop46），0FAILED/0SKIPPED，sourceStable=true，135.056秒**。Temurin17.0.20.1、Gradle9.1，离线严格依赖hash、单worker/单fork。无新增依赖/真实API/真实图库/系统剪贴板读写，AssetStore与官方Prompt/request未改。

## 后续缺口

WebP/GIF/BMP及EXIF坐标兼容、完整全局导出/侧栏/故事书偏好接入、原生视觉/文件选择器/粘贴、APNG完整产品接线验证、图库与安全更新仍需继续。当前预览页按所选页/分类切换，不等于已经完成所有图片deck的人机体验。长批次取消/应用排水属于后续整体生命周期验证，不以本次文件故障注入代替。完整安全A→B安装更新门槛保持。

[5次运行及失败原件](evidence/UX_R2_EXPORT_METADATA/runs/) · [文件身份清单](evidence/UX_R2_EXPORT_METADATA/PACKAGE_FILES.json)。本检查点均为可安全存Git的合成证据；Drive NOT UPLOADED，无虚构上传回执。
