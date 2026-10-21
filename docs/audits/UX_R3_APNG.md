# UX-R3 APNG工作流整合（I06 / I07）

起点1a9dc09ca69b82eea9977959bac9bd808c306a90，保持既有R2b动作归属，作为R3图库/交付桥接补全。PRODUCT_HOLD。

图库所选→APNG工作区→伪装/还原选择→当前处理预览→静态封面或动画播放→图库副本／选择目录批量输出／复制全部处理文件。来源使用既有固定Desktop 5ef95428421f7c693b010ac88f51d470804ab07a DesktopImageTools.disguise/restore、DesktopApngPlayback及权威ApngDisguiseCodec；没有改编解码器、Prompt或请求语义。使用CCB默认蓝色logo封面，不提供自定义封面；这不是加密或隐私清除。还原保证来源规则的像素/动画语义，不宣称恢复原压缩字节或所有元数据。原件始终保留，新副本具有父资产关联。

静态图片/GIF可伪装；既有APNG需要先还原。动态WebP明确拒绝静态化，不能把单帧当整段动画成功。进入TypeSet新工程或追加图片时，GIF/动态WebP/APNG须确认仅编辑首帧／默认图；伪装APNG提示默认图可能只是封面，须先还原内容。取消不建立工程；确认后记录原件关联。等待确认不显示工程已经建立。普通现有静态工程无需额外确认。

批次逐文件串行转换到新.incomplete目录，源ID/hash、输出大小/hash写入COMPLETE.json并落盘后，原子发布新目录才完成；不覆盖历史输出，禁止向资料根输出。失败保留原件和部分产物，不自动重试。复制通过文件列表保留动画，单APNG也不提供易静态化的image flavor；缓存独立于资产根。无自动删除缓存政策，当前保留以防接收端尚未读取，后续需明确保留/清理交互。

## 执行与边界

五次运行：首轮JUnit初始化错误（测试返回非void），整体4项计数中1FAILED，APNG用例未执行，其余图库3项PASS；修正后APNG2 PASS；增加GIF/静态确认后3 PASS；完整desktop57 PASS（118.966秒）；最后确认状态提示修正后5项定向PASS（33.952秒）。0 skipped，源码稳定，历史失败保留；最后5项不冒充再次完整57项。core/资产/权威模块未改未重跑。

实际Compose工作台执行预览、取消目录、不覆盖的新目录发布、完成清单hash核验、隔离批次文件复制、图库副本、静态确认取消/同意、还原像素、保存重开。合成GIF为之前Pillow生成的12×8红/蓝各100ms两帧样本：伪装和还原后的两帧颜色、时长、无限循环保持；动态WebP被明确拒绝。故障钩子在文件写入后中断，.incomplete保留、无成功目录、旧文件及源原件不变。

JDK17.0.20.1/Gradle9.1，离线严格依赖/单worker；无新增依赖/下载，无付费API/用户图库/凭据。Compose离屏截图已复查，不等于原生Windows操作或真人验收；系统接收端粘贴、原生路径选择器、长批次资源极限、完整应用退出期间的APNG任务交接尚未实测。完整R4安全应用内A→B仍未完成，不能称RC。

[全部原始运行含失败](evidence/UX_R3_APNG/runs/) · [处理界面](evidence/UX_R3_APNG/disguise-preview.png) · [大小与SHA-256清单](evidence/UX_R3_APNG/PACKAGE_FILES.json)。全部安全合成证据入Git，Drive NOT UPLOADED，无独立非Git附件。继续排版剩余兼容、统一工作流与R4，不自动合并或发布。
