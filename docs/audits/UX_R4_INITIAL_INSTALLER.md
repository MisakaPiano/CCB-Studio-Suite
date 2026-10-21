# UX-R4 实际内部安装器基线（仍非A→B）

应用源码固定bc3c4d863fdd80ca6f7d2690fdbbe50e6ef794ec。真实Compose createDistributable执行成功（20.695秒、0项测试，COMMAND_SUCCESS不能当测试PASS）。由新Build-InternalInstaller.py将实际应用映像、已核验隔离Python/TUF和只含公钥的测试bootstrap编入NSIS3.13安装器。无正式签名密钥、无系统信任变更、无系统Python安装；测试私钥只在内存且已随生成进程退出。

安装器只接受新目录，拒绝覆盖已存在目录；程序与资料分离，卸载清单只列明确的程序文件，不递归删除目录或用户资料。实际安装到Suite .local/windows-rc/installer-smoke/version-1：退出0，544个程序文件大小/hash全部匹配；重复同目标安装退出17，重新检查全部文件不变。这是真NSIS安装执行，**不是应用A触发B，也未验证卸载**。

安装包166479920 bytes，SHA-256 fab595cd57b0f4b972c6875d80e6b760185554e9960ea7b363425820372182d2。本地保留installer-initial-1/studio-installer.exe；这是中间测试包，不作为RC交付。NSIS编译器身份/许可见前一可信客户端报告；首次下载HTML的BadZipFile失败留存本地，不当可执行包使用。

首次用受限隐藏方式启动PID27928，工作区已创建但无可控窗口，记NO_TARGETABLE_WINDOW。核对进程路径与合成根后终止该测试实例，保留目录；普通用户交互桌面重开同一根后获得唯一工作台窗口PID972，无stderr。Computer-use截图两次失败：FrameArrived timed out / window capture timed out；仅可访问性返回窗口和窗格，无法证明界面可读或按钮可用。随后向唯一测试窗口发送Alt+F4，进程退出；不将此观察提升为GUI验收。未操作其他窗口或真实资料。原生视觉保持UNVERIFIED。

为后续真实内部HTTP演练添加严格限制：只有随包internalTest=true、internal渠道且主机字面量127.0.0.1时允许HTTP；其他渠道/主机仍要求固定HTTPS。不改系统网络开放状态，没有新监听。新8项Python客户端回归全部实际PASS（含该限制），原有7项/桌面60项证据保留，不冒充本轮重跑JVM测试。此检查点无JVM源码修改。

[安装、编译、启动及新回归原始记录](evidence/UX_R4_INITIAL_INSTALLER/PACKAGE_FILES.json)。Git存精简证据；大中间安装包保留本地，Drive NOT UPLOADED，尚非最终交付附件。下一步：将应用内确认下载与维护排水、独立安装、B启动/资料验证串成真实A→B，并处理剩余UX门槛；PRODUCT_HOLD，不发布或合并。
