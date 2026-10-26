# Preview116发布、日常升级与主动维护

2026-10-26 Asia/Tokyo。用户确认发布固定候选，且仅在公开更新链核验通过后更新日常安装。随后用户手动完成程序内升级并供图确认“已更新并显示116”。**已发布并完成本机日常升级；不代表全项目或完整功能验收。**

## 不可变发行身份

- 发布：[v0.0.0-preview.116](https://github.com/MisakaPiano/CCB-Studio-Suite/releases/tag/v0.0.0-preview.116)，release ID `424305004`，保持Pre-release；公开时间2026-10-26 13:47:44 UTC（22:47:44 JST）。
- 组装／tag源码 `9839bce20ccdf4460e0aa42befcb070c1a6a235f`；实际应用映像构建 `0f11fdd6b6d281987dd7c449d623a74b8b74b8f0`。不是后续清理窄修的源码。
- 安装器 `CCB-Studio-Suite-0.0.0-preview.116-windows-x64.exe`，168,998,814字节，SHA-256 **`8fee16c386f1da7a247b0f8a739b067687ab6712f9ad0e749fff106e0648966e`**，附件ID `678306334`。四份附件通过GitHub digest／大小核对，安装器还实际从公开地址完整流式校验；没有更换原附件或重新打包。
- 正式元数据提交 [59412d5ab296563ccc556e7cfb1523bcf9b0cb3e](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/59412d5ab296563ccc556e7cfb1523bcf9b0cb3e)，父 `9d1e0472eea272a4e877804258cadf1090fcf017`；targets15、snapshot24、timestamp24。仅修改现有preview元数据五条路径，普通前进提交，无强推。
- 根SHA-256仍为`408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a`。只通过既有适配器调用targets／snapshot／timestamp签名，未创建密钥、未读取root私钥、未导出私钥，旧桥接通道未改。
- 主库main下载入口提交 [20e8696](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/20e869658bfcc5c44e406cd355f2d5c3561103b9)，随后已知问题披露 [9c4540d](https://github.com/MisakaPiano/CCB-Studio-Suite/commit/9c4540db57d5b3ea064d1d6d0b9d154247078df2)，两份提交各只改README，未合并应用。旧发行库迁移提示、历史版本和桥接保持，Releases区块观察为开启，未取消Pre-release。

## 实际执行结果

|层次|实际结果与边界|
|---|---|
|固定候选发行前|[候选报告](PREVIEW_116_CANDIDATE_20261026.md)：真实NSIS／隔离安装，550项／2242类／326运行时／6脚本匹配|
|公开完整字节|PASS；真实公开安装器168,998,814字节及SHA与用户确认一致；没有重新制作候选|
|实际旧客户端|PASS；保留的115自带Python和客户端从正式根发现并下载116。真实NSIS隔离安装及116当前检查通过；这是CLI＋合成父目录锚点，不冒充原生旧A界面|
|日常115→116|用户手动程序内保存／排水／安装重启完成；截图及“已更新并显示116”确认，入口指向116，550程序项再核，workspace／runtime仍沿原根。应用自己的COMPLETE回执为`B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH`；执行者未读取用户vault、凭据快照或图库|
|自动化UI|尝试启动后取得日常115窗口；截图分别FrameArrived／window capture超时，可访问树仅窗格，未盲点。用户完成界面步骤，不冒充执行者自动点击成功|
|本机残留|116原清理曾因旧路径记录全局延后；经[独立窄修及主动维护](LEGACY_CACHE_CLEANUP_FIX_20261026.md)，旧115目录及对应下载缓存已清，553已证实归属文件／421,899,352字节。111保护、旧plan／result记录、资料及备份未改；运行中116未热补丁|

公开核验脚本第一次误把旧helper的三元组返回值当dict，真实下载安装已成功，驱动随后TypeError。保留原脚本／日志，读取已生成`INSTALLED_NOT_STARTED`journal并核对字节后续接当前检查，**没有重新下载或安装，没有伪造COMPLETE**。[原错误与续接](evidence/PREVIEW_116_PUBLICATION_20261026/PUBLIC_DRIVER_FAILURE.json)。这与真实产品更新失败分开。

公开核验临时产物570文件／422,056,397字节在外部留证后，通过既有OWNER机制退役；旧115卸载器归属验证的临时重放产物也封存退役。保留固定安装器、原始失败及审查证据，不清整盘、不清回收站、不删除固定基线。

## 发布后发现的限制及后续版本

116仍带有旧路径记录阻塞清理的原引擎。曾改名／移动目录的用户可能遇到延后；本机已用**另行测试的未发布源码**执行一次性受控维护，不表示116安装器已被修复或替换。公开发行页和README已追加该限制，原附件、SHA清单及签名目标说明保持原字节，不对同版本重签不同目标。后续窄修7项合成回归已通过，单独提交；没有再建版本或发布。

**资产管理／编辑完整人工验收、115体验反馈、Prompt对比分别保留待验；新包独立审查NOT REQUESTED，q09历史FAILED／root UNKNOWN、旧失败／用户否决保留。**真实完整跨盘搬迁、迁移后启动／资料定位／旧所属收据重绑定均未凭本次书面记录验收。仍是实验邀请Preview，Authenticode尚无，不宣称稳定版／完整RC。

## 证据与隐私边界

[公开发行、元数据、实际旧客户端及隔离安装证据](evidence/PREVIEW_116_PUBLICATION_20261026/RAW_MANIFEST.json)；[脱敏日常结果](evidence/PREVIEW_116_PUBLICATION_20261026/DAILY_UPDATE_SANITIZED_SUMMARY.json)。用户截图仅保存散列和观察，未将像素放进仓库；日常原始更新／维护记录只留Suite `.local`，不复制用户workspace、vault或真实快照。Git认证仅在标准凭据助手和发布进程内存中使用，未输出认证材料。

下一动作是承接原更新清理任务的后续候选交付，不再自动发布；整安装根迁移能力另按安装身份、启动、资料、保护与所属记录逐层补验证，不能只替换路径字符串。
