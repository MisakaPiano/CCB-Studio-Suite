# UX-R4 实际A→B：空间门槛阻塞

固定应用源码b8a91533e583c16230d5c2801dc5309c64dbc29f。A 0.2.1与B 0.2.2均由真实Compose app image和NSIS3.13构建；离线Gradle构建成功不记测试PASS。首次未引用版本参数被PowerShell拆分，构建FAILED保留；正确引用后A/B构建成功。NSIS完整工具链与固定归档逐文件一致。NSIS仅有FileDescription/LegalCopyright版本字段缺失警告，不隐瞒。

A包166534728 bytes / SHA-256 8ab841dbd07f6565e45d23f5785da83b540de9b43c4cbee1af63b2b6019f6328；B包166534747 bytes / SHA-256 991273d6bcbbcf2b8a301b48fd5002f7cdbe392b250cb86a21bf36204b160016。A实际安装到Suite独立测试目录，546个程序文件大小/哈希验证通过。资料为此前GUI回环测试加明确标记的排版合成工程，44文件复制后逐字节一致；无真实图库/付费调用。

内部服务仅127.0.0.1:63747、临时内存测试签名，无正式私钥文件或系统信任修改。通过computer-use在实际A窗口发送快捷键，A自身启动随包更新客户端：检查返回AVAILABLE，签名、渠道、版本及源码验证通过。确认下载返回FAILED / Insufficient update staging space。B包166534747 bytes，暂存检查要求566713105 bytes；H盘仅约153804800 bytes可用。未下载完成、未安装B、未重启B。旧程序和资料保留，不能将此结果称为完整A→B通过。

实际A窗口截图两次均返回window capture timed out: timed out waiting on channel；可访问性仅窗口/窗格。键盘触发由应用实际回执确认，原生视觉UNVERIFIED。演练结束向唯一A测试窗口发送Alt+F4，随后PID668不存在；内部服务通过自己的STOP文件正常结束，内存测试私钥随进程销毁。下一次需新建测试信任根并重新打包，不能复用已经不存在的签名私钥。

状态：RC BLOCKED / PRODUCT_HOLD。当前空间不足不能安全继续下载/安装，不降低余量门槛。AGENTS.md要求未经精确授权不得删除缓存、发行产物或数据，因此以下仅提出清理、不执行：installer-initial-1/payload、ab-build-a-1/payload、ab-build-b-1/payload三个构建展开副本，合计751003334 bytes。每个副本的完整文件清单/哈希和保留的对应安装器路径见PROPOSED_CLEANUP.json；不包含测试资料根，不删除安装器、已安装程序、测试日志、源码或凭据。

[完整原始回执与清理候选身份](evidence/UX_R4_AB_SPACE_BLOCK/PACKAGE_FILES.json)。Git保存精简证据；中间安装器仍本地保留，Drive NOT UPLOADED。下一动作：取得上述精确清理授权或用户自行释放H盘空间后，接续内部A→B与剩余产品恢复；不合并/发布，不宣布RC。
