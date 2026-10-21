# UX-R4 实际应用内A→B已执行，整体产品仍HOLD

用户释放空间后从1a013737d08579bc413d2dbf4da20e18cb523c99恢复，初始H盘6360670208 bytes可用，工作树/index干净，远程一致且Suite只有本执行者。没有执行此前提出的三个目录清理。旧空间不足FAILED证据保留。

## 实际路径与结果

固定源码1a013737d08579bc413d2dbf4da20e18cb523c99，Temurin17.0.20.1/Gradle9.1 offline strict、单worker。真实app image构建A 74.393秒、B 20.717秒，均COMMAND_SUCCESS/0测试，不记测试PASS。NSIS3.13完整归档文件身份校验后生成两个真实安装器：

|版本|字节|SHA-256|
|---|---:|---|
|A 0.2.1 internal|166534777|ccba1173b8ba9744b32e71dff9cab1c76d03e7f3b1fd0417decaed1e5d72278e|
|B 0.2.2 internal|166534740|ea24a432dd67c0dd14cbe77474476c7c356f83bdf58338cb6cf93a256bb98bd4|

A实际安装至Suite .local/windows-rc/ab-applications-2/1-1a013737d085，546文件验证通过。独立ab-data-2是上轮合成资料复制，45文件逐字节核对；包括GUI回环创建的设计/生图历史、两个DPAPI测试账户及明确注明为fixture seed的竖排/打码工程。

应用内路径确实运行：实际A窗口经computer-use键盘进入更新→检查AVAILABLE→确认下载DOWNLOADED_VERIFIED→安装确认→维护排水→A自行退出→独立进程重新TUF验证→真实NSIS安装B到新目录→546文件验证→B真实进程/窗口启动→B_VERIFIED→COMPLETE→生成快捷方式。不是外部复制包代替安装，也不是只调用TUF库。

交接前后3条资产记录（含原件及排版快照资产）、2个测试凭据指纹、全工作区state hash及关联metadata完全一致。凭据由B通过DPAPI实际解密后取指纹，未输出明文或上传容器；未以真实服务账号可用性冒充。安装日志INSTALLED_NOT_STARTED与最终交接COMPLETE分开保留。B窗口592022存在；A PID10424已退出。正常关闭B后，生成的Launch-CCB-Studio.lnk再次实际打开B，PID12320/窗口133314，随后正常关闭。内部127.0.0.1服务通过STOP正常退出；临时签名私钥只在内存并随退出销毁。

## 更新后继续工作与证据边界

实际更新完成的ab-data-2用于新增回归：Compose GUI输入更新后中文草稿→保存→重开，原有assets和credentials指纹不变，1项实际PASS（43.476秒，无跳过）。此回归使用相同生产源码的测试classpath，明确是OFFSCREEN_COMPOSE，不冒充在原生B输入框中操作。随后上述真实B快捷方式重开证明仍可启动。此次只新增测试及测试参数，无生产源码变化；既有62项完整回归不重新记本轮PASS。

原生截图首次FrameArrived timed out、刷新重选后window capture timed out；原生视觉仍UNVERIFIED，可访问性仅窗口/窗格。实际动作以app-owned下载/安装/交接回执及新版本窗口身份确认，未用离屏截图替代原生截图。首次临时JSON校验脚本漏指定UTF-8而遇GBK解码失败，修正后验证通过，备注保留；不是应用故障。无付费API调用，无Desktop/upstream改动，无正式发行签名或系统信任变更。

此次通过的是正常实际A→B路径；安装失败、B失败、迁移中断等完整故障门槛仍需补齐，原生视觉、真实提供者探针及剩余UX功能仍未整体签收。相同数据reader=2无需迁移，本次没有发生迁移，不称迁移PASS。整体PRODUCT_HOLD/NOT RC，旧磁盘阻塞已解除，继续连续实施。

## 双通道回执

[Git原始证据身份清单](evidence/UX_R4_ACTUAL_AB/PACKAGE_FILES.json)。非Git证据压缩包已上传并通过Drive元数据回读确认大小131561 bytes、父目录1DcysN49RtUDnY9jx4wZ4z41F_j4ttUT2：

[UX_R4_ACTUAL_AB_1a013737_EVIDENCE_NOT_RC.zip](https://drive.google.com/file/d/1-KRRWLtKAEzL1x7WLJsGI6X2R1p7CWXq/view?usp=drivesdk)，Drive ID 1-KRRWLtKAEzL1x7WLJsGI6X2R1p7CWXq；本地SHA-256 1322b32eda26abe3c4cf4a7cbecfabc9f558ba2995406c77d9ae584e7d7b3350。连接器未返回远端SHA字段，不声称远端hash已比对。未改变共享权限；安装器仍本地内部测试产物，不是RC交付包。
